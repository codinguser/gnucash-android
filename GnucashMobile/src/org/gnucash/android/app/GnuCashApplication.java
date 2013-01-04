package org.gnucash.android.app;

import android.app.Application;
import android.content.Context;

/**
 * An {@link Application} subclass for retrieving static context
 * @author Ngewi Fet <ngewif@gmail.com>
 *
 */
public class GnuCashApplication extends Application{

    private static Context context;

    public void onCreate(){
        super.onCreate();
        GnuCashApplication.context = getApplicationContext();
    }

    /**
     * Returns the application context
     * @return Application {@link Context} object
     */
    public static Context getAppContext() {
        return GnuCashApplication.context;
    }
}