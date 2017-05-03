package org.gnucash.android.app;

import android.app.Application;
import android.os.Build;

import com.facebook.stetho.Stetho;

import org.gnucash.android.BuildConfig;

/**
 * Utility class for initializing Stetho in debug builds
 */

public class StethoUtils {

    /**
     * Sets up Stetho to enable remote debugging from Chrome developer tools.
     *
     * <p>Among other things, allows access to the database and preferences.
     * See http://facebook.github.io/stetho/#features</p>
     */
    public static void install(Application application){
        //don't initialize stetho during tests
        if (!BuildConfig.DEBUG || isRoboUnitTest())
            return;

        Stetho.initialize(Stetho.newInitializerBuilder(application)
                        .enableWebKitInspector(Stetho.defaultInspectorModulesProvider(application))
                        .build());
    }

    /**
     * Returns {@code true} if the app is being run by robolectric
     * @return {@code true} if in unit testing, {@code false} otherwise
     */
    private static boolean isRoboUnitTest(){
        return "robolectric".equals(Build.FINGERPRINT);
    }
}
