package au.com.mysites.camps.util;


import android.util.Log;

/**
 * General short methods
 */
public class OperationsGeneral {

    private final static String TAG = OperationsGeneral.class.getSimpleName();

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
}
