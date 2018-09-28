package au.com.mysites.camps.util;

public final class Constants {

    private static final String PACKAGE_NAME = "au.com.mysites.camps";

    // Intent request codes
    public static final int RC_IMAGE_CAPTURE = 20;
    public static final int RC_PICK_IMAGE = 30;
    public static final int RC_SIGN_IN = 40;

    // Database query limit
    public static final int LIMIT = 50;

    // Used to set the display time for toasts
    public static final int TOASTTIMEFACILITIES = 500;
    public static final int TOASTTIMEDATABASE = 1000;

    public static final long QUERYTIMEOUT = 10000L;
    public static final long ASYNCTIMEOUT = 7000L;

    // SaveInstanceState Key
    public static final String M_SITE_HAS_CHANGED = "site_changed";

    // Date format
    // Note there is also a Resources string version
    public static final String DATEFORMAT = "dd/MM/yyyy";

    // Storage Permissions request code
    public final static int PERMISSIONS_REQUEST_CAMERA = 10;
    public final static int PERMISSIONS_REQUEST_EXTERNAL_STORAGE = 20;
    public final static int PERMISSIONS_REQUEST_EXTERNAL_STORAGE_CAMERA = 30;
    public final static int PERMISSIONS_REQUEST_EXTERNAL_STORAGE_GETPHOTO = 40;
    public final static int PERMISSIONS_REQUEST_LOCATION = 50;

    // Used in FetchAddressService
    public static final int SUCCESS_RESULT = 0;
    public static final int FAILURE_RESULT = 1;

    public static final String RECEIVER = PACKAGE_NAME + ".RECEIVER";
    public static final String RESULT_DATA_KEY = PACKAGE_NAME + ".RESULT_DATA_KEY";
    public static final String LOCATION_DATA_EXTRA = PACKAGE_NAME + ".LOCATION_DATA_EXTRA";

    // Used in filter dialog and filter code in summary activity
    public static final String FIELD_NAME = "name";
    public static final String FIELD_RATING = "rating";
    public static final String FIELD_STATE = "state";
}
