package org.gnucash.android.test.unit.db;

import android.database.sqlite.SQLiteConstraintException;

import org.gnucash.android.BuildConfig;
import org.gnucash.android.db.adapter.ScheduledActionDbAdapter;
import org.gnucash.android.model.BaseModel;
import org.gnucash.android.model.PeriodType;
import org.gnucash.android.model.Recurrence;
import org.gnucash.android.model.ScheduledAction;
import org.gnucash.android.test.unit.util.GnucashTestRunner;
import org.gnucash.android.test.unit.util.ShadowCrashlytics;
import org.gnucash.android.test.unit.util.ShadowUserVoice;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test the scheduled actions database adapter
 */
@RunWith(GnucashTestRunner.class) //package is required so that resources can be found in dev mode
@Config(constants = BuildConfig.class, sdk = 21, packageName = "org.gnucash.android", shadows = {ShadowCrashlytics.class, ShadowUserVoice.class})
public class ScheduledActionDbAdapterTest {

    ScheduledActionDbAdapter mScheduledActionDbAdapter;

    @Before
    public void setUp(){
        mScheduledActionDbAdapter = ScheduledActionDbAdapter.getInstance();
    }

    public void shouldFetchOnlyEnabledScheduledActions(){
        ScheduledAction scheduledAction = new ScheduledAction(ScheduledAction.ActionType.TRANSACTION);
        scheduledAction.setRecurrence(new Recurrence(PeriodType.MONTH));
        scheduledAction.setEnabled(false);

        mScheduledActionDbAdapter.addRecord(scheduledAction);

        scheduledAction = new ScheduledAction(ScheduledAction.ActionType.TRANSACTION);
        scheduledAction.setRecurrence(new Recurrence(PeriodType.WEEK));
        mScheduledActionDbAdapter.addRecord(scheduledAction);

        assertThat(mScheduledActionDbAdapter.getAllRecords()).hasSize(2);

        List<ScheduledAction> enabledActions = mScheduledActionDbAdapter.getAllEnabledScheduledActions();
        assertThat(enabledActions).hasSize(1);
        assertThat(enabledActions.get(0).getRecurrence().getPeriodType()).isEqualTo(PeriodType.WEEK);
    }

    @Test(expected = NullPointerException.class) //no recurrence is set
    public void everyScheduledActionShouldHaveRecurrence(){
        ScheduledAction scheduledAction = new ScheduledAction(ScheduledAction.ActionType.TRANSACTION);
        scheduledAction.setActionUID(BaseModel.generateUID());
        mScheduledActionDbAdapter.addRecord(scheduledAction);
    }


    @Test
    public void testGenerateRepeatString(){
        ScheduledAction scheduledAction = new ScheduledAction(ScheduledAction.ActionType.TRANSACTION);
        PeriodType periodType = PeriodType.MONTH;
        periodType.setMultiplier(2);
        scheduledAction.setRecurrence(new Recurrence(periodType));
        scheduledAction.setTotalFrequency(4);

        String repeatString = "Every 2 months,  for 4 times";
        assertThat(scheduledAction.getRepeatString().trim()).isEqualTo(repeatString);

    }
}
