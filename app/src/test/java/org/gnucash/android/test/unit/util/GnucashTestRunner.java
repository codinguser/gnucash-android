package org.gnucash.android.test.unit.util;

import org.junit.runners.model.InitializationError;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.internal.SdkConfig;
import org.robolectric.internal.bytecode.ClassHandler;
import org.robolectric.internal.bytecode.ShadowMap;

/**
 * Test runner for application
 */
public class GnucashTestRunner extends RobolectricGradleTestRunner {

    public GnucashTestRunner(Class<?> klass) throws InitializationError {
        super(klass);
    }

    @Override
    protected ShadowMap createShadowMap() {
        return super.createShadowMap()
                .newBuilder().addShadowClass(ShadowCrashlytics.class).build();
    }

    @Override
    protected ClassHandler createClassHandler(ShadowMap shadowMap, SdkConfig sdkConfig) {
        ShadowMap map = shadowMap.newBuilder().addShadowClass(ShadowCrashlytics.class).build();
        return super.createClassHandler(map, sdkConfig);
    }


}
