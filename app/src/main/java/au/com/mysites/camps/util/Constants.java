package au.com.mysites.camps.util;

public class Constants {

    //Storage Permissions request identifier
    public static final int REQUEST_EXTERNAL_STORAGE = 10;
    //Storage Permissions request identifier
    public static final int REQUEST_CONTACTS_ACCESS = 20;

    //Image request codes
    public static final int REQUEST_THUMBNAIL_IMAGE_CAPTURE = 30;
    public static final int REQUEST_IMAGE_CAPTURE = 40;

    public static final int RC_SIGN_IN = 9001;

    public static final int LIMIT = 50;

    // used to set the time for toasts
    public static int TOASTTIMEFACILITIES = 500;
    public static int TOASTTIMEDATABASE = 1000;

    //format of dates
    //Note there is also a Resources string version
    public static final String DATEFORMAT = "dd/MM/yyyy";

    //Permission requests
    public final static int PERMISSIONS_REQUEST_CAMERA = 10;
    public final static int PERMISSIONS_REQUEST_EXTERNAL_STORAGE_CAMERA = 20;
    public final static int PERMISSIONS_REQUEST_EXTERNAL_STORAGE_GETPHOTO = 30;
    public final static int PERMISSIONS_REQUEST_LOCATION = 40;

    // Size of photos to store in Firebase storage
    public final static int SIZEPHOTOWIDTH = 1080;
    public final static int SIZEPHOTOHEIGHT = 1080;

}
