package digipodium.locationserviceapp;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.chip.Chip;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;

import java.util.List;

import static digipodium.locationserviceapp.AddressFinderWorker.LATITUDE;
import static digipodium.locationserviceapp.AddressFinderWorker.LONGITUDE;
import static digipodium.locationserviceapp.AddressFinderWorker.RESULT;

public class FirstFragment extends Fragment {

    FusedLocationProviderClient locationClient;
    private TextView tvLocUpdates;
    private TextView tvLoc, tvAddress;
    private Chip chipUpdate;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    public static final String API_KEY = "pk.eyJ1IjoiemFpZGthbWlsIiwiYSI6ImNrNXh2Z2xiYjBnazkzbHBkNG03enQ4NTYifQ.8sAJfK4lDkZ8hysdCxF-Ag";
    private MapView mapView;
    private MapboxMap mapboxMap;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        Mapbox.getInstance(getActivity(), API_KEY);
        return inflater.inflate(R.layout.fragment_first, container, false);
    }


    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvLoc = view.findViewById(R.id.tvLoc);
        tvLocUpdates = view.findViewById(R.id.tvLocUpdate);
        tvAddress = view.findViewById(R.id.tvAddress);
        chipUpdate = view.findViewById(R.id.chipUpdate);

        mapView = view.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull MapboxMap mapboxMap) {
                FirstFragment.this.mapboxMap = mapboxMap;
                mapboxMap.setStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {
                        Toast.makeText(getActivity(), "map loaded", Toast.LENGTH_SHORT).show();
                    }
                });

            }
        });
        locationClient = LocationServices.getFusedLocationProviderClient(getActivity());
        chipUpdate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    getLocationContinuously();
                } else {
                    if (locationCallback != null) {
                        locationClient.removeLocationUpdates(locationCallback);
                        tvLocUpdates.setText("stopped location updates");
                    }
                }
            }
        });
        getLocationOnce(tvLoc);
    }

    @SuppressLint("MissingPermission")
    private void getLocationContinuously() {
        locationRequest = new LocationRequest()
                .setInterval(15000)
                .setFastestInterval(10000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                List<Location> locations = locationResult.getLocations();
                for (Location location : locations) {
                    tvLocUpdates.setText("");
                    if (location != null) {
                        tvLocUpdates.append("LOC " + location.getLatitude() + " : " + location.getLongitude());
                        LatLng latLng= new LatLng(location.getLatitude(),location.getLongitude());
                        CameraPosition camPos = new CameraPosition.Builder().target(latLng).zoom(12).build();
                        MarkerOptions options= new MarkerOptions().position(latLng).setTitle("your location");
                        mapboxMap.addMarker(options);
                        mapboxMap.setCameraPosition(camPos);
                    }
                }

            }

            @Override
            public void onLocationAvailability(LocationAvailability locationAvailability) {
                super.onLocationAvailability(locationAvailability);
            }
        };
        locationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }


    private void getLocationOnce(TextView tvLoc) {
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                if (task.isSuccessful()) {
                    Location location = task.getResult();
                    double lat = location.getLatitude();
                    double lng = location.getLongitude();
                    String output = "LOCATION:" + lat + ":" + lng;
                    tvLoc.setText(output);

                    // address fetching
                    Data locationData = new Data.Builder()
                            .putDouble(LATITUDE, lat)
                            .putDouble(LONGITUDE, lng)
                            .build();

                    Constraints constraint = new Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build();
                    OneTimeWorkRequest lastKnowLocRequest = new OneTimeWorkRequest.Builder(AddressFinderWorker.class)
                            .setInputData(locationData)
                            .setConstraints(constraint)
                            .build();

                    WorkManager.getInstance(getActivity()).enqueue(lastKnowLocRequest);
                    WorkManager.getInstance(getActivity()).getWorkInfoByIdLiveData(lastKnowLocRequest.getId())
                            .observe(getViewLifecycleOwner(),workInfo -> {
                                if (workInfo!=null && workInfo.getState().isFinished()) {
                                    String addresses = workInfo.getOutputData().getString(RESULT);
                                    tvAddress.setText(addresses);
                                }
                            });
                }
            }
        });
    }
}
