package au.com.mysites.camps.util;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.StringTokenizer;

import au.com.mysites.camps.R;
import au.com.mysites.camps.model.Site;
import au.com.mysites.camps.site.DetailSiteMapActivity;

/**
 * Methods used to assist with maps
 */
public class OperationsMap {
    private final static String TAG = OperationsMap.class.getSimpleName();

    /**
     * Checks a longitude String is in the format
     * FORMAT_DEGREES:MINUTES:SECONDS ie dd:mm:ss.s
     * and then checks each field's value is in a valid range
     * <p>
     * returns true if valid format.
     */
    public static boolean mapCheckLongitudeCoordinate(String coordinate) {
        if (Debug.DEBUG_METHOD_ENTRY_SITE) Log.d(TAG, "mapCheckLongitudeCoordinate()");

        if (coordinate == null) return false;

        if (coordinate.charAt(0) == '-') {
            coordinate = coordinate.substring(1);
        }
        StringTokenizer st = new StringTokenizer(coordinate, ":");
        int tokens = st.countTokens();
        if (tokens != 3) {
            return false;
        }
        String degrees = st.nextToken();
        String minutes = st.nextToken();
        String seconds = st.nextToken();

        int deg = Integer.parseInt(degrees);
        int min = Integer.parseInt(minutes);

        //Double as seconds can have decimal points
        double sec = Double.parseDouble(seconds);

        //check for condition where degrees is 180, mins is 0 and secs is 0
        if ((deg == 180) && (min == 0) && (sec == 0))
            return true;
        // deg must be in [0, 179]
        if (deg < 0.0 || deg > 179) return false;
        // min must be in [0, 59 etc]
        if ((min < 0 || min > 59)) return false;
        // sec must be in [0.0, 59.99 etc)
        return sec >= 0 && sec < 60;
    }

    /**
     * Checks a latitude String is in the format
     * FORMAT_DEGREES:MINUTES:SECONDS ie dd:mm:ss.s
     * and then checks each field's value is in a valid range
     * <p>
     * returns true if valid format.
     */
    public static boolean mapCheckLatitudeCoordinate(String coordinate) {
        if (Debug.DEBUG_METHOD_ENTRY_SITE) Log.d(TAG, "mapCheckLatitudeCoordinate()");

        if (coordinate == null) return false;

        if (coordinate.charAt(0) == '-') {
            coordinate = coordinate.substring(1);
        }
        StringTokenizer st = new StringTokenizer(coordinate, ":");
        int tokens = st.countTokens();
        if (tokens != 3) {
            return false;
        }
        String degrees = st.nextToken();
        String minutes = st.nextToken();
        String seconds = st.nextToken();

        int deg = Integer.parseInt(degrees);
        int min = Integer.parseInt(minutes);

        //Use double as seconds can have decimal points
        double sec = Double.parseDouble(seconds);

        //check for condition where degrees is 180, mins is 0 and secs is 0
        if ((deg == 90) && (min == 0) && (sec == 0)) return true;
        // deg must be in [0, 90]
        if (deg < 0.0 || deg > 90) return false;
        // min must be in [0, 59 etc]
        if ((min < 0 || min > 59)) return false;
        // sec must be in [0.0, 59.99 etc)
        return sec >= 0 && sec < 60;
    }

    /**
     * Converts a string of degrees minutes seconds in the format DD:MM:SS or -DD:MM:SS
     * to degrees in a double.
     * <p>
     * Does do not any format or range checking.
     *
     * @param coordinate String of DD:MM:SS
     * @return Degrees as double
     */
    public static double mapCovertDegMinSecToDegrees(String coordinate) {
        if (Debug.DEBUG_METHOD_ENTRY_SITE) Log.d(TAG, "mapConvertDegMinSecToDegrees()");

        //assume it is positive
        double sign = +1D;

        if (coordinate.charAt(0) == '-') {
            coordinate = coordinate.substring(1);
            sign = -1D;
        }

        StringTokenizer st = new StringTokenizer(coordinate, ":");

        String degrees = st.nextToken();
        String minutes = st.nextToken();
        String seconds = st.nextToken();

        double deg = Double.parseDouble(degrees);
        double min = Double.parseDouble(minutes);
        double sec = Double.parseDouble(seconds);
        double decimal = ((min * 60) + sec) / (60 * 60);

        return sign * (deg + decimal);
    }

    /**
     * Checks have a valid site, and latitude and longitude are set.
     * Format checking of latitude and longitude is not done here
     * as this is done at entry of latitude and longitude.
     *
     * @param site    site containing latitude and longitude
     * @return false if null site or latitude or longitude not set
     */
    public static boolean mapCheckLatLongSet(Site site) {
        if (Debug.DEBUG_METHOD_ENTRY_MAP) Log.d(TAG, "mapCheckLatLongSet()");

        if (site == null) return false;
        if (site.getLatitude() == null || site.getLatitude().isEmpty()) return false;
        if (site.getLongitude() == null || site.getLongitude().isEmpty()) return false;

        return true;
}

    /**
     * Shows a map using an intent
     *
     * @param site    site containing latitude and longitude
     * @param context context of calling method
     */
    public static void mapShow(Site site, Context context) {
        if (Debug.DEBUG_METHOD_ENTRY_MAP) Log.d(TAG, "mapShow()");

        Intent intent = new Intent(context, DetailSiteMapActivity.class);

        intent.putExtra(context.getString(R.string.pref_key_lat), site.getLatitude());
        intent.putExtra(context.getString(R.string.pref_key_long), site.getLongitude());
        intent.putExtra(context.getString(R.string.pref_key_name), site.getName());

        context.startActivity(intent);
    }
}
