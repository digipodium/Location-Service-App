package digipodium.locationserviceapp;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AddressFinderWorker extends Worker {

    public static final String LATITUDE = "latitude";
    public static final String LONGITUDE = "longitude";
    public static final String RESULT = "result";


    private final Context context;

    public AddressFinderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {

        double lat = getInputData().getDouble(LATITUDE, 0);
        double lng = getInputData().getDouble(LONGITUDE, 0);
        String resultMsg = "";
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocation(lat, lng, 5);
        } catch (IOException e) {
            resultMsg = e.getMessage();
        }

        if (addresses == null || addresses.size() == 0) {
            if (resultMsg.isEmpty()) {
                resultMsg = "No address found";
            }
        } else {
            StringBuilder builder = new StringBuilder();
            for (Address address : addresses) {
                ArrayList<String> addressParts = new ArrayList<>();
                for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                    addressParts.add(address.getAddressLine(i));
                }
                String locAddress = TextUtils.join("\n", addressParts);
                builder.append(locAddress);
                builder.append("\n");
            }
            resultMsg = builder.toString();
        }
        Data data = new Data.Builder().putString(RESULT,resultMsg).build();
        return Result.success(data);
    }
}
