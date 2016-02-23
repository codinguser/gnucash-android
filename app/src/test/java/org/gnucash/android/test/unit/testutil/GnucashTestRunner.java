package org.gnucash.android.test.unit.testutil;

import com.crashlytics.android.Crashlytics;
import com.uservoice.uservoicesdk.UserVoice;

import org.junit.runners.model.InitializationError;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.internal.bytecode.InstrumentationConfiguration;

/**
 * Test runner for application
 */
public class GnucashTestRunner extends RobolectricGradleTestRunner {

    public GnucashTestRunner(Class<?> klass) throws InitializationError {
        super(klass);
    }

    @Override
    public InstrumentationConfiguration createClassLoaderConfig() {
        InstrumentationConfiguration.Builder builder = InstrumentationConfiguration.newBuilder()
                .addInstrumentedClass(Crashlytics.class.getName())
                .addInstrumentedClass(UserVoice.class.getName());

        return builder.build();
    }
}
