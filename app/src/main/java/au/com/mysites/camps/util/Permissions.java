package au.com.mysites.camps.util;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

public class Permissions {
    private final static String TAG = Permissions.class.getSimpleName();

    /**
     * Checks we have permissions
     * If we have permission return true.
     * If permission not granted call requestPermissions(String[], int)}, and
     * return false.
     *
     * @param permissions Permissions being checked
     * @param requestId   Used to identify calling method
     */
    public boolean checkPermissions(Activity activity, String[] permissions, int requestId) {
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "checkPermissions()");
        //Check permission
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            //no access, request permissions
            ActivityCompat.requestPermissions(activity, permissions, requestId);
            return false;
        } else {
            //permission already granted
            return true;
        }
    }


}
