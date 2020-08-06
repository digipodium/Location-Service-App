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

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.chip.Chip;

import java.util.List;

public class FirstFragment extends Fragment {

    FusedLocationProviderClient locationClient;
    private TextView tvLocUpdates;
    private TextView tvLoc;
    private Chip chipUpdate;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false);
    }


    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvLoc = view.findViewById(R.id.tvLoc);
        tvLocUpdates = view.findViewById(R.id.tvLocUpdate);
        chipUpdate = view.findViewById(R.id.chipUpdate);

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
                }
            }
        });
    }
}
