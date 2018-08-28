package au.com.mysites.camps.util;

import android.app.Application;
import android.content.Context;

public class AppContextProvider extends Application {

    /**
     * Keeps a reference of the application context
     */
    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();

        mContext = getApplicationContext();

    }

    /**
     * Returns the application context
     *
     * @return application context
     */
    public static Context getContext() {
        return mContext;
    }
}
