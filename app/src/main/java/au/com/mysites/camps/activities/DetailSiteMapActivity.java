package au.com.mysites.camps.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import au.com.mysites.camps.R;
import au.com.mysites.camps.util.Debug;

import static au.com.mysites.camps.util.UtilMap.covertDegMinSecToDegrees;


/**
 * Displays the location of the activities on a map and adds a marker
 */
public class DetailSiteMapActivity extends FragmentActivity implements OnMapReadyCallback {
    private final static String TAG = DetailSiteMapActivity.class.getSimpleName();

    private String mLatitude;
    private String mLongitude;
    private String mName;

    float zoomLevel = 16.0F;

    /**
     * Gets the activities, latitude, longitude and name from the intent,
     * loads the view with the map and requests the map.
     * The results are returned in {@link #onMapReady(GoogleMap)}.
     *
     * @param savedInstanceState  saved state
     */
    @Override
    @CallSuper
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "onCreate()");

        //check we have a latitude and longitude
        Intent myIntent = getIntent();
        if (myIntent == null) {
            Toast.makeText(this, getString(R.string.ERROR_Map_invalid_lat_long),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        mLatitude = myIntent.getStringExtra(getString(R.string.pref_key_lat));
        mLongitude = myIntent.getStringExtra(getString(R.string.pref_key_long));
        mName = myIntent.getStringExtra(getString(R.string.pref_key_name));

        setContentView(R.layout.activity_site_detail_map);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * provides the map when ready
     *
     * @param googleMap     map from API
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "onMapReady()");

        // Get latitude and longitude, convert from DD:MM:SS to DD.DDDDDD as a double
        LatLng location = new LatLng(covertDegMinSecToDegrees(mLatitude),
                covertDegMinSecToDegrees(mLongitude));

        // Add a marker to the map
        googleMap.addMarker(new MarkerOptions()
                .position(location)
                .title(mName));     //name of the activities
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, zoomLevel));
    }
}
