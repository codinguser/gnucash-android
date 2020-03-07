package org.gnucash.android.ui.transaction;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TextView;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.db.adapter.ScheduledActionDbAdapter;
import org.gnucash.android.db.adapter.TransactionsDbAdapter;
import org.gnucash.android.model.AccountType;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.ScheduledAction;
import org.gnucash.android.model.Split;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.ui.common.FormActivity;
import org.gnucash.android.ui.common.UxArgument;
import org.gnucash.android.ui.passcode.PasscodeLockActivity;

import java.text.DateFormat;
import java.util.Date;
import java.util.MissingFormatArgumentException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Activity for displaying transaction information
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class TransactionDetailActivity extends PasscodeLockActivity {

    class SplitAmountViewHolder {
        @BindView(R.id.split_account_name) TextView accountName;
        @BindView(R.id.split_debit) TextView        splitDebitView;
        @BindView(R.id.split_credit) TextView       splitCreditView;

        View itemView;

        public SplitAmountViewHolder(View view,
                                     Split split) {

            itemView = view;

            ButterKnife.bind(this,
                             view);

            AccountsDbAdapter accountsDbAdapter = AccountsDbAdapter.getInstance();

            final String accountUID = split.getAccountUID();

            accountName.setText(accountsDbAdapter.getAccountFullName(accountUID));

            // splitSignedAmount (positive or negative number)
            Money splitSignedAmount = split.getValueWithSignum();

            // Define debit or credit view
            TextView balanceView = splitSignedAmount.isNegative()
                                   ? splitCreditView
                                   : splitDebitView;

            final AccountType accountType = AccountsDbAdapter.getInstance()
                                                             .getAccountType(split.getAccountUID());

            // Display absolute value because it is displayed either in debit or credit column
            accountType.displayBalance(balanceView,
                                       splitSignedAmount,
                                       // TODO TW C 2020-03-07 : Mettre une préférence pour le signe
                                       true);
        }

    } // Class SplitAmountViewHolder

    @BindView(R.id.toolbar) Toolbar mToolBar;
    @BindView(R.id.trn_description) TextView mTransactionDescription;
    @BindView(R.id.transaction_account) TextView mTransactionAccount;
    @BindView(R.id.balance_debit) TextView mDebitBalance;
    @BindView(R.id.balance_credit) TextView mCreditBalance;
    @BindView(R.id.trn_time_and_date) TextView mTimeAndDate;
    @BindView(R.id.trn_recurrence) TextView mRecurrence;
    @BindView(R.id.trn_notes) TextView mNotes;

    @BindView(R.id.fragment_transaction_details)
    TableLayout mDetailTableLayout;

    private String mTransactionUID;
    private String mAccountUID;
    private int mDetailTableRows;

    public static final int REQUEST_EDIT_TRANSACTION = 0x10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_transaction_detail);

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
    private void bindViews() {

        TransactionsDbAdapter transactionsDbAdapter = TransactionsDbAdapter.getInstance();
        Transaction transaction = transactionsDbAdapter.getRecord(mTransactionUID);

        // Transaction description
        mTransactionDescription.setText(transaction.getDescription());

        // Account Full Name
        mTransactionAccount.setText(getString(R.string.label_inside_account_with_name, AccountsDbAdapter.getInstance().getAccountFullName(mAccountUID)));

        //
        // Add Debit/Credit Labels
        //

        mDetailTableRows = mDetailTableLayout.getChildCount();
        int index = 0;

        LayoutInflater inflater = LayoutInflater.from(this);
        View           view     = inflater.inflate(R.layout.item_split_amount_info,
                                                   mDetailTableLayout,
                                                   false);
        ((TextView) view.findViewById(R.id.split_debit)).setText(getString(R.string.label_debit));
        ((TextView) view.findViewById(R.id.split_credit)).setText(getString(R.string.label_credit));
        mDetailTableLayout.addView(view,
                                   index++);

        //
        // Détails
        //

        AccountsDbAdapter accountsDbAdapter = AccountsDbAdapter.getInstance();

        boolean useDoubleEntry = GnuCashApplication.isDoubleEntryEnabled();

        for (Split split : transaction.getSplits()) {

            if (!useDoubleEntry && split.getAccountUID()
                                        .equals(accountsDbAdapter.getImbalanceAccountUID(split.getValue()
                                                                                              .getCommodity()))) {
                //do not show imbalance accounts for single entry use case

            } else {

                view = inflater.inflate(R.layout.item_split_amount_info,
                                        mDetailTableLayout,
                                        false);
                SplitAmountViewHolder viewHolder = new SplitAmountViewHolder(view,
                                                                             split);
                mDetailTableLayout.addView(viewHolder.itemView,
                                           index++);
            }
        } // for

        //
        // Account balance at Transaction time
        //

        // Compute balance at Transaction time
        Money accountBalance = accountsDbAdapter.getAccountBalance(mAccountUID,
                                                                   -1,
                                                                   transaction.getTimeMillis());

        // #8xx
        // Define in which field (Debit or Credit) the balance shall be displayed
        TextView balanceTextView = accountBalance.isNegative()
                                   ? mCreditBalance
                                   : mDebitBalance;

        final AccountType accountType = accountsDbAdapter.getAccountType(mAccountUID);

        accountType.displayBalance(balanceTextView,
                                   accountBalance);

        //
        // Date
        //

        Date trnDate = new Date(transaction.getTimeMillis());
        String timeAndDate = DateFormat.getDateInstance(DateFormat.FULL).format(trnDate);
        mTimeAndDate.setText(timeAndDate);

        //
        //
        //

        if (transaction.getScheduledActionUID() != null){
            ScheduledAction scheduledAction = ScheduledActionDbAdapter.getInstance().getRecord(transaction.getScheduledActionUID());
            mRecurrence.setText(scheduledAction.getRepeatString());
            findViewById(R.id.row_trn_recurrence).setVisibility(View.VISIBLE);

        } else {
            findViewById(R.id.row_trn_recurrence).setVisibility(View.GONE);
        }

        if (transaction.getNote() != null && !transaction.getNote().isEmpty()){
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
        removeSplitItemViews();
        bindViews();
    }

    /**
     * Remove the split item views from the transaction detail prior to refreshing them
     */
    private void removeSplitItemViews(){
        // Remove all rows that are not special.
        mDetailTableLayout.removeViews(0, mDetailTableLayout.getChildCount() - mDetailTableRows);
        mDebitBalance.setText("");
        mCreditBalance.setText("");
    }


    @OnClick(R.id.fab_edit_transaction)
    public void editTransaction(){
        Intent createTransactionIntent = new Intent(this.getApplicationContext(), FormActivity.class);
        createTransactionIntent.setAction(Intent.ACTION_INSERT_OR_EDIT);
        createTransactionIntent.putExtra(UxArgument.SELECTED_ACCOUNT_UID, mAccountUID);
        createTransactionIntent.putExtra(UxArgument.SELECTED_TRANSACTION_UID, mTransactionUID);
        createTransactionIntent.putExtra(UxArgument.FORM_TYPE, FormActivity.FormType.TRANSACTION.name());
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
