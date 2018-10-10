package au.com.mysites.camps.services;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.os.ResultReceiver;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import au.com.mysites.camps.util.Constants;
import au.com.mysites.camps.util.Debug;

@SuppressWarnings("unused")
public class FetchAddressService extends IntentService {
    public FetchAddressService() {
        super("FetchAddressIntentService");
    }

    private final static String TAG = FetchAddressService.class.getSimpleName();
    private ResultReceiver mReceiver;

    private String street;
    private String locality;
    private String state;
    private String postCode;
    @SuppressWarnings("unused")
    private String country;

    /**
     * Checks for errors invokes Geocoder
     * gets address, checks for errors
     * logs debug information which is normally suppressed
     * calls method to send results back to main activity
     *
     * @param intent intent used to start service
     */
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (Debug.DEBUG_METHOD_ENTRY_SERVICE) Log.d(TAG, "onHandleIntent");

        if (intent == null) {
            if (Debug.DEBUG_SERVICE_ADDRESS) Log.d(TAG, "Intent is null");
            return;
        }
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        String errorMessage;
        ArrayList<String> addressFragments = new ArrayList<>();

        // Get the location passed to this service through an extra.
        Location location = intent.getParcelableExtra(Constants.LOCATION_DATA_EXTRA);
        List<Address> addresses;
        mReceiver = intent.getParcelableExtra(Constants.RECEIVER);

        try {
            //Just a single address.
            addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
        } catch (IOException | IllegalArgumentException e) {
            // Catch network or other I/O problems.
            errorMessage = "Service_not_available";
            Log.e (TAG, "Error: " + e);
            deliverResultToReceiver(Constants.FAILURE_RESULT, errorMessage);
            return;
        }
        // Handle case where no address was found.
        if (addresses == null || addresses.size() == 0) {
            errorMessage = "Address is null";
            deliverResultToReceiver(Constants.FAILURE_RESULT, errorMessage);
        } else {
            if (Debug.DEBUG_SERVICE_ADDRESS) Log.d(TAG, "Address_found");
            Address address = addresses.get(0);

            // Collect them send to the calling activity
            addressFragments.add(address.getThoroughfare());
            addressFragments.add(address.getLocality());
            addressFragments.add(address.getAdminArea());
            addressFragments.add(address.getPostalCode());
            addressFragments.add(address.getCountryCode());

            deliverResultToReceiver(Constants.SUCCESS_RESULT,
                    TextUtils.join(System.getProperty("line.separator"),
                            addressFragments));
        }
    }

    /**
     * send address back to calling activity
     *
     * @param resultCode results code
     * @param message    address
     */
    @SuppressLint("RestrictedApi")
    private void deliverResultToReceiver(int resultCode, String message) {
        if (Debug.DEBUG_METHOD_ENTRY_SERVICE) Log.d(TAG, "deliverResultToReceiver");

        Bundle bundle = new Bundle();
        bundle.putString(Constants.RESULT_DATA_KEY, message);
        mReceiver.send(resultCode, bundle);
    }
}
