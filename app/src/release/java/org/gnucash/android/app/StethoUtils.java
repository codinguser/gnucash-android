package org.gnucash.android.app;

import android.app.Application;

/**
 * Dummy utility class for overriding Stetho initializing in release build variants
 */

public class StethoUtils {

    public static void install(Application application) {
        //nothing to see here, move along
        //check the debug version of this class to see Stetho init code
    }
}
