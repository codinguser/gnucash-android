package org.gnucash.android.test.unit.testutil;

import org.junit.runners.model.InitializationError;
import org.robolectric.RobolectricGradleTestRunner;

/**
 * Test runner for application
 */
public class GnucashTestRunner extends RobolectricGradleTestRunner {

    public GnucashTestRunner(Class<?> klass) throws InitializationError {
        super(klass);
    }

}
