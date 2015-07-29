

package org.gnucash.android.test.ui;

import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;

import org.gnucash.android.ui.chart.PieChartActivity;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class PieChartActivityTest extends ActivityInstrumentationTestCase2<PieChartActivity> {
	private PieChartActivity mPieChartActivity;

	public PieChartActivityTest() {
		super(PieChartActivity.class);
	}
	
	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		injectInstrumentation(InstrumentationRegistry.getInstrumentation());

        setActivityIntent(new Intent(Intent.ACTION_VIEW));
		mPieChartActivity = getActivity();
	}

	@Override
	@After
	public void tearDown() throws Exception {
		mPieChartActivity.finish();
		super.tearDown();
	}
	
}
