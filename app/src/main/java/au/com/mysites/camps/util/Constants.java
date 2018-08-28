package au.com.mysites.camps.util;

public final class Constants {

    // Storage Permissions request code
    public static final int RC_EXTERNAL_STORAGE = 10;

    // Intent request codes
    public static final int RC_IMAGE_CAPTURE = 20;
    public static final int RC_PICK_IMAGE = 30;
    public static final int RC_SIGN_IN = 9001;

    public static final int LIMIT = 50;

    // used to set the display time for toasts
    public static int TOASTTIMEFACILITIES = 500;
    public static int TOASTTIMEDATABASE = 1000;

    // Date format
    // Note there is also a Resources string version
    public static final String DATEFORMAT = "dd/MM/yyyy";

    // Permission requests
    public final static int PERMISSIONS_REQUEST_CAMERA = 10;
    public final static int PERMISSIONS_REQUEST_EXTERNAL_STORAGE_CAMERA = 20;
    public final static int PERMISSIONS_REQUEST_EXTERNAL_STORAGE_GETPHOTO = 30;
    public final static int PERMISSIONS_REQUEST_LOCATION = 40;
}
