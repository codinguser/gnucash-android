package org.gnucash.android.ui.transaction;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.ScheduledActionDbAdapter;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.ScheduledAction;
import org.gnucash.android.model.Split;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.ui.FormActivity;
import org.gnucash.android.ui.UxArgument;

import java.text.DateFormat;
import java.util.Date;
import java.util.MissingFormatArgumentException;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Activity for displaying transaction information
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class TransactionInfoActivity extends AppCompatActivity{

    @Bind(R.id.trn_description) TextView mTransactionDescription;
    @Bind(R.id.transaction_amount)TextView mTransactionAmount;
    @Bind(R.id.trn_transfer_account) TextView mTransferAccount;
    @Bind(R.id.trn_time_and_date) TextView mTimeAndDate;
    @Bind(R.id.trn_recurrence) TextView mRecurrence;
    @Bind(R.id.trn_notes) TextView mNotes;
    @Bind(R.id.toolbar) Toolbar mToolBar;
    private String mTransactionUID;
    private String mAccountUID;

    public static final int REQUEST_EDIT_TRANSACTION = 0x10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_transaction_info);

        mTransactionUID = getIntent().getStringExtra(UxArgument.SELECTED_TRANSACTION_UID);
        mAccountUID     = getIntent().getStringExtra(UxArgument.SELECTED_ACCOUNT_UID);

        if (mTransactionUID == null || mAccountUID == null){
            throw new MissingFormatArgumentException("You must specify both the transaction and account GUID");
        }

        ButterKnife.bind(this);
        setSupportActionBar(mToolBar);

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setElevation(0);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);
        actionBar.setDisplayShowTitleEnabled(false);


        bindViews();

        int themeColor = AccountsDbAdapter.getActiveAccountColorResource(mAccountUID);
        actionBar.setBackgroundDrawable(new ColorDrawable(themeColor));
        mToolBar.setBackgroundColor(themeColor);
        if (Build.VERSION.SDK_INT > 20)
            getWindow().setStatusBarColor(GnuCashApplication.darken(themeColor));

    }

    /**
     * Reads the transaction information from the database and binds it to the views
     */
    private void bindViews(){
        TransactionsDbAdapter transactionsDbAdapter = TransactionsDbAdapter.getInstance();
        Transaction transaction = transactionsDbAdapter.getRecord(mTransactionUID);

        mTransactionDescription.setText(transaction.getDescription());
        Money balance = transaction.getBalance(mAccountUID);
        mTransactionAmount.setText(balance.formattedString());
        int color = balance.isNegative() ? R.color.debit_red : R.color.credit_green;
        mTransactionAmount.setTextColor(getResources().getColor(color));

        if (!GnuCashApplication.isDoubleEntryEnabled()){
            findViewById(R.id.row_transfer_account).setVisibility(View.GONE);
        } else {
            findViewById(R.id.row_transfer_account).setVisibility(View.VISIBLE);
            if (transaction.getSplits().size() == 2) {
                if (transaction.getSplits().get(0).isPairOf(transaction.getSplits().get(1))) {
                    for (Split split : transaction.getSplits()) {
                        if (!split.getAccountUID().equals(mAccountUID)) {
                            mTransferAccount.setText(
                                    AccountsDbAdapter.getInstance()
                                            .getFullyQualifiedAccountName(split.getAccountUID()));
                            break;
                        }
                    }
                }
            } else {
                mTransferAccount.setText(transaction.getSplits().size() + " splits");
            }
        }

        Date trnDate = new Date(transaction.getTimeMillis());
        String timeAndDate = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT).format(trnDate);
        mTimeAndDate.setText(timeAndDate);

        if (transaction.getScheduledActionUID() != null){
            ScheduledAction scheduledAction = ScheduledActionDbAdapter.getInstance().getRecord(transaction.getScheduledActionUID());
            mRecurrence.setText(scheduledAction.getRepeatString());
            findViewById(R.id.row_trn_recurrence).setVisibility(View.VISIBLE);

        } else {
            findViewById(R.id.row_trn_recurrence).setVisibility(View.GONE);
        }

        if (transaction.getNote() != null){
            mNotes.setText(transaction.getNote());
            findViewById(R.id.row_trn_notes).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.row_trn_notes).setVisibility(View.GONE);
        }

    }

    /**
     * Refreshes the transaction information
     */
    private void refresh(){
        bindViews();
    }

    @OnClick(R.id.fab_edit_transaction)
    public void editTransaction(){
        Intent createTransactionIntent = new Intent(this.getApplicationContext(), FormActivity.class);
        createTransactionIntent.setAction(Intent.ACTION_INSERT_OR_EDIT);
        createTransactionIntent.putExtra(UxArgument.SELECTED_ACCOUNT_UID, mAccountUID);
        createTransactionIntent.putExtra(UxArgument.SELECTED_TRANSACTION_UID, mTransactionUID);
        createTransactionIntent.putExtra(UxArgument.FORM_TYPE, FormActivity.FormType.TRANSACTION_FORM.name());
        startActivityForResult(createTransactionIntent, REQUEST_EDIT_TRANSACTION);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK){
            refresh();
        }
    }
}
