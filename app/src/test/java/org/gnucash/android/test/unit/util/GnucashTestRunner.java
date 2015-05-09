package org.gnucash.android.test.unit.util;

import org.junit.runners.model.InitializationError;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.internal.bytecode.ClassInfo;
import org.robolectric.internal.bytecode.InstrumentingClassLoaderConfig;
import org.robolectric.internal.bytecode.ShadowMap;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Test runner for application
 */
public class GnucashTestRunner extends RobolectricGradleTestRunner {

    private static final List<String> CUSTOM_SHADOW_TARGETS =
            Collections.unmodifiableList(Arrays.asList(
                    "com.crashlytics.android.Crashlytics"
            ));
    public GnucashTestRunner(Class<?> klass) throws InitializationError {
        super(klass);
    }

    @Override
    protected ShadowMap createShadowMap() {
        return super.createShadowMap()
                .newBuilder().addShadowClass(ShadowCrashlytics.class).build();
    }

    @Override
    public InstrumentingClassLoaderConfig createSetup() {
        return new InstrumenterConfig();
    }

    private class InstrumenterConfig extends InstrumentingClassLoaderConfig {

        @Override
        public boolean shouldInstrument(ClassInfo classInfo) {
            return CUSTOM_SHADOW_TARGETS.contains(classInfo.getName())
                    || super.shouldInstrument(classInfo);
        }

    }

}
