package au.com.mysites.camps.util;


import android.util.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * General short methods
 */
public class UtilGeneral {

    private final static String TAG = UtilGeneral.class.getSimpleName();

    /**
     * Checks if string is empty,
     * also regards the string empty if just has spaces.
     *
     * @param string string to be tested
     * @return true if empty
     */
    public static boolean stringEmpty(String string) {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "stringEmpty()");

        return (string == null || string.trim().length() == 0);
    }

    public static String getTodaysDate(String format) {

    DateFormat dateFormat = new SimpleDateFormat(format);
    Date date = new Date();
	return dateFormat.format(date);
}
}
