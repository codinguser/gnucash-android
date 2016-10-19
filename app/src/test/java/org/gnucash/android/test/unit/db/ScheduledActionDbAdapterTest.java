package org.gnucash.android.test.unit.db;

import android.content.res.Resources;

import org.gnucash.android.BuildConfig;
import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.adapter.ScheduledActionDbAdapter;
import org.gnucash.android.model.BaseModel;
import org.gnucash.android.model.PeriodType;
import org.gnucash.android.model.Recurrence;
import org.gnucash.android.model.ScheduledAction;
import org.gnucash.android.test.unit.testutil.GnucashTestRunner;
import org.gnucash.android.test.unit.testutil.ShadowCrashlytics;
import org.gnucash.android.test.unit.testutil.ShadowUserVoice;
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
        Recurrence recurrence = new Recurrence(periodType);
        recurrence.setMultiplier(2);
        scheduledAction.setRecurrence(recurrence);
        scheduledAction.setTotalPlannedExecutionCount(4);
        Resources res = GnuCashApplication.getAppContext().getResources();
        String repeatString = res.getQuantityString(R.plurals.label_every_x_months, 2, 2) + ", " +
                res.getString(R.string.repeat_x_times, 4);

        assertThat(scheduledAction.getRepeatString().trim()).isEqualTo(repeatString);

    }

    @Test
    public void testAddGetRecord() {
        ScheduledAction scheduledAction = new ScheduledAction(ScheduledAction.ActionType.BACKUP);
        scheduledAction.setActionUID("Some UID");
        scheduledAction.setAdvanceCreateDays(1);
        scheduledAction.setAdvanceNotifyDays(2);
        scheduledAction.setAutoCreate(true);
        scheduledAction.setAutoNotify(true);
        scheduledAction.setEnabled(true);
        scheduledAction.setStartTime(11111);
        scheduledAction.setEndTime(33333);
        scheduledAction.setLastRun(22222);
        scheduledAction.setExecutionCount(3);
        scheduledAction.setRecurrence(new Recurrence(PeriodType.MONTH));
        scheduledAction.setTag("QIF;SD_CARD;2016-06-25 12:56:07.175;false");
        mScheduledActionDbAdapter.addRecord(scheduledAction);

        ScheduledAction scheduledActionFromDb =
                mScheduledActionDbAdapter.getRecord(scheduledAction.getUID());
        assertThat(scheduledActionFromDb.getUID()).isEqualTo(
                scheduledAction.getUID());
        assertThat(scheduledActionFromDb.getActionUID()).isEqualTo(
                scheduledAction.getActionUID());
        assertThat(scheduledActionFromDb.getAdvanceCreateDays()).isEqualTo(
                scheduledAction.getAdvanceCreateDays());
        assertThat(scheduledActionFromDb.getAdvanceNotifyDays()).isEqualTo(
                scheduledAction.getAdvanceNotifyDays());
        assertThat(scheduledActionFromDb.shouldAutoCreate()).isEqualTo(
                scheduledAction.shouldAutoCreate());
        assertThat(scheduledActionFromDb.shouldAutoNotify()).isEqualTo(
                scheduledAction.shouldAutoNotify());
        assertThat(scheduledActionFromDb.isEnabled()).isEqualTo(
                scheduledAction.isEnabled());
        assertThat(scheduledActionFromDb.getStartTime()).isEqualTo(
                scheduledAction.getStartTime());
        assertThat(scheduledActionFromDb.getEndTime()).isEqualTo(
                scheduledAction.getEndTime());
        assertThat(scheduledActionFromDb.getLastRunTime()).isEqualTo(
                scheduledAction.getLastRunTime());
        assertThat(scheduledActionFromDb.getExecutionCount()).isEqualTo(
                scheduledAction.getExecutionCount());
        assertThat(scheduledActionFromDb.getRecurrence()).isEqualTo(
                scheduledAction.getRecurrence());
        assertThat(scheduledActionFromDb.getTag()).isEqualTo(
                scheduledAction.getTag());
    }
}
