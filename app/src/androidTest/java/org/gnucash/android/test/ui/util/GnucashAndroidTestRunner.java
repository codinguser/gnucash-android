/*
 * Copyright (c) 2015 Ngewi Fet <ngewif@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gnucash.android.test.ui.util;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.multidex.MultiDex;
import android.support.test.runner.AndroidJUnitRunner;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Custom test runner
 */
public class GnucashAndroidTestRunner extends AndroidJUnitRunner{
    private static final String TAG = "GncAndroidTestRunner";
    private static final String ANIMATION_PERMISSION = "android.permission.SET_ANIMATION_SCALE";
    private static final float DISABLED = 0.0f;
    private static final float DEFAULT = 1.0f;

    @Override
    public void onCreate(Bundle args) {
        super.onCreate(args);
        // as time goes on we may actually need to process our arguments.
        disableAnimation();

    }

    @Override
    public void onDestroy() {
        enableAnimation();
        super.onDestroy();
    }

    private void disableAnimation() {
        int permStatus = getContext().checkCallingOrSelfPermission(ANIMATION_PERMISSION);
        if (permStatus == PackageManager.PERMISSION_GRANTED) {
            if (reflectivelyDisableAnimation(DISABLED)) {
                Log.i(TAG, "All animations disabled.");
            } else {
                Log.i(TAG, "Could not disable animations.");
            }
        } else {
            Log.i(TAG, "Cannot disable animations due to lack of permission.");
        }
    }

    private void enableAnimation(){
        int permStatus = getContext().checkCallingOrSelfPermission(ANIMATION_PERMISSION);
        if (permStatus == PackageManager.PERMISSION_GRANTED) {
            if (reflectivelyDisableAnimation(DEFAULT)) {
                Log.i(TAG, "All animations enabled.");
            } else {
                Log.i(TAG, "Could not enable animations.");
            }
        } else {
            Log.i(TAG, "Cannot disable animations due to lack of permission.");
        }
    }

    private boolean reflectivelyDisableAnimation(float animationScale) {
        try {
            Class<?> windowManagerStubClazz = Class.forName("android.view.IWindowManager$Stub");
            Method asInterface = windowManagerStubClazz.getDeclaredMethod("asInterface", IBinder.class);
            Class<?> serviceManagerClazz = Class.forName("android.os.ServiceManager");
            Method getService = serviceManagerClazz.getDeclaredMethod("getService", String.class);
            Class<?> windowManagerClazz = Class.forName("android.view.IWindowManager");
            Method setAnimationScales = windowManagerClazz.getDeclaredMethod("setAnimationScales",
                    float[].class);
            Method getAnimationScales = windowManagerClazz.getDeclaredMethod("getAnimationScales");

            IBinder windowManagerBinder = (IBinder) getService.invoke(null, "window");
            Object windowManagerObj = asInterface.invoke(null, windowManagerBinder);
            float[] currentScales = (float[]) getAnimationScales.invoke(windowManagerObj);
            for (int i = 0; i < currentScales.length; i++) {
                currentScales[i] = animationScale;
            }
            setAnimationScales.invoke(windowManagerObj, currentScales);
            return true;
        } catch (ClassNotFoundException cnfe) {
            Log.w(TAG, "Cannot disable animations reflectively.", cnfe);
        } catch (NoSuchMethodException mnfe) {
            Log.w(TAG, "Cannot disable animations reflectively.", mnfe);
        } catch (SecurityException se) {
            Log.w(TAG, "Cannot disable animations reflectively.", se);
        } catch (InvocationTargetException ite) {
            Log.w(TAG, "Cannot disable animations reflectively.", ite);
        } catch (IllegalAccessException iae) {
            Log.w(TAG, "Cannot disable animations reflectively.", iae);
        } catch (RuntimeException re) {
            Log.w(TAG, "Cannot disable animations reflectively.", re);
        }
        return false;
    }
}
