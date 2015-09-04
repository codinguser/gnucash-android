/*
 * Copyright (c) 2012 - 2014 Ngewi Fet <ngewif@gmail.com>
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

package org.gnucash.android.ui.transaction;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.doomonafireball.betterpickers.calendardatepicker.CalendarDatePickerDialog;
import com.doomonafireball.betterpickers.radialtimepicker.RadialTimePickerDialog;
import com.doomonafireball.betterpickers.recurrencepicker.EventRecurrence;
import com.doomonafireball.betterpickers.recurrencepicker.EventRecurrenceFormatter;
import com.doomonafireball.betterpickers.recurrencepicker.RecurrencePickerDialog;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import org.gnucash.android.R;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.db.ScheduledActionDbAdapter;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.model.AccountType;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.ScheduledAction;
import org.gnucash.android.model.Split;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.model.TransactionType;
import org.gnucash.android.ui.UxArgument;
import org.gnucash.android.ui.transaction.dialog.SplitEditorDialogFragment;
import org.gnucash.android.ui.util.AmountInputFormatter;
import org.gnucash.android.ui.util.RecurrenceParser;
import org.gnucash.android.ui.util.TransactionTypeSwitch;
import org.gnucash.android.ui.util.CustomKeyboard;
import org.gnucash.android.ui.widget.WidgetConfigurationActivity;
import org.gnucash.android.util.QualifiedAccountNameCursorAdapter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

/**
 * Fragment for creating or editing transactions
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class TransactionFormFragment extends Fragment implements
        CalendarDatePickerDialog.OnDateSetListener, RadialTimePickerDialog.OnTimeSetListener,
        RecurrencePickerDialog.OnRecurrenceSetListener {

    public static final String FRAGMENT_TAG_SPLITS_EDITOR       = "splits_editor";
    private static final String FRAGMENT_TAG_RECURRENCE_PICKER  = "recurrence_picker";

    /**
	 * Transactions database adapter
	 */
	private TransactionsDbAdapter mTransactionsDbAdapter;

	/**
	 * Accounts database adapter
	 */
	private AccountsDbAdapter mAccountsDbAdapter;

	/**
	 * Adapter for transfer account spinner
	 */
	private SimpleCursorAdapter mCursorAdapter;

	/**
	 * Cursor for transfer account spinner
	 */
	private Cursor mCursor;

    /**
	 * Transaction to be created/updated
	 */
	private Transaction mTransaction;

    /**
	 * Formats a {@link Date} object into a date string of the format dd MMM yyyy e.g. 18 July 2012
	 */
	public final static DateFormat DATE_FORMATTER = DateFormat.getDateInstance();

	/**
	 * Formats a {@link Date} object to time string of format HH:mm e.g. 15:25
	 */
	public final static DateFormat TIME_FORMATTER = DateFormat.getTimeInstance();

	/**
	 * Button for setting the transaction type, either credit or debit
	 */
	private TransactionTypeSwitch mTransactionTypeButton;

	/**
	 * Input field for the transaction name (description)
	 */
	private AutoCompleteTextView mDescriptionEditText;

	/**
	 * Input field for the transaction amount
	 */
	private EditText mAmountEditText;

	/**
	 * Field for the transaction currency.
	 * The transaction uses the currency of the account
	 */
	private TextView mCurrencyTextView;

	/**
	 * Input field for the transaction description (note)
	 */
	private EditText mNotesEditText;

	/**
	 * Input field for the transaction date
	 */
	private TextView mDateTextView;

	/**
	 * Input field for the transaction time
	 */
	private TextView mTimeTextView;

	/**
	 * {@link Calendar} for holding the set date
	 */
	private Calendar mDate;

	/**
	 * {@link Calendar} object holding the set time
	 */
	private Calendar mTime;

	/**
	 * Spinner for selecting the transfer account
	 */
	private Spinner mDoubleAccountSpinner;

    /**
     * Checkbox indicating if this transaction should be saved as a template or not
     */
    private CheckBox mSaveTemplateCheckbox;

    /**
     * Flag to note if double entry accounting is in use or not
     */
	private boolean mUseDoubleEntry;

    /**
     * Flag to not if the transaction involves multiple currency
     */
    private boolean mMultiCurrency;

    /**
     * The AccountType of the account to which this transaction belongs.
     * Used for determining the accounting rules for credits and debits
     */
    AccountType mAccountType;

    TextView mRecurrenceTextView;

    private String mRecurrenceRule;
    private EventRecurrence mEventRecurrence = new EventRecurrence();

    private AmountInputFormatter mAmountInputFormatter;

    private String mAccountUID;

    private List<Split> mSplitsList = new ArrayList<Split>();

    private boolean mEditMode = false;
    private CustomKeyboard mCalculatorKeyboard;

    /**
	 * Create the view and retrieve references to the UI elements
	 */
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_new_transaction, container, false);

		mDescriptionEditText = (AutoCompleteTextView) v.findViewById(R.id.input_transaction_name);
		mNotesEditText = (EditText) v.findViewById(R.id.input_description);
		mDateTextView           = (TextView) v.findViewById(R.id.input_date);
		mTimeTextView           = (TextView) v.findViewById(R.id.input_time);
		mAmountEditText         = (EditText) v.findViewById(R.id.input_transaction_amount);
		mCurrencyTextView       = (TextView) v.findViewById(R.id.currency_symbol);
		mTransactionTypeButton  = (TransactionTypeSwitch) v.findViewById(R.id.input_transaction_type);
		mDoubleAccountSpinner   = (Spinner) v.findViewById(R.id.input_double_entry_accounts_spinner);
        mRecurrenceTextView     = (TextView) v.findViewById(R.id.input_recurrence);
        mSaveTemplateCheckbox = (CheckBox) v.findViewById(R.id.checkbox_save_template);
        return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setHasOptionsMenu(true);
		ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);

		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
		mUseDoubleEntry = sharedPrefs.getBoolean(getString(R.string.key_use_double_entry), false);
		if (!mUseDoubleEntry){
			getView().findViewById(R.id.layout_double_entry).setVisibility(View.GONE);
            mAmountEditText.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
		}

        mAccountUID = getArguments().getString(UxArgument.SELECTED_ACCOUNT_UID);
		mAccountsDbAdapter = AccountsDbAdapter.getInstance();
        mAccountType = mAccountsDbAdapter.getAccountType(mAccountUID);

        String transactionUID = getArguments().getString(UxArgument.SELECTED_TRANSACTION_UID);
		mTransactionsDbAdapter = TransactionsDbAdapter.getInstance();
		if (transactionUID != null)
            mTransaction = mTransactionsDbAdapter.getTransaction(transactionUID);
        if (mTransaction != null) {
            mMultiCurrency = mTransactionsDbAdapter.getNumCurrencies(mTransaction.getUID()) > 1;
        }

        //updateTransferAccountsList must only be called after initializing mAccountsDbAdapter
        // it needs mMultiCurrency to be properly initialized
        updateTransferAccountsList();

        mDoubleAccountSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                if (mSplitsList.size() == 2){ //when handling simple transfer to one account
                    for (Split split : mSplitsList) {
                        if (!split.getAccountUID().equals(mAccountUID)){
                            split.setAccountUID(mAccountsDbAdapter.getUID(id));
                        }
                        // else case is handled when saving the transactions
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                //nothing to see here, move along
            }
        });

        setListeners();
		if (mTransaction == null) {
            initalizeViews();
            initTransactionNameAutocomplete();
        } else {
			initializeViewsWithTransaction();
            mEditMode = true;
		}

        mCalculatorKeyboard = new CustomKeyboard(getActivity(), R.id.calculator_keyboard, R.xml.calculator_keyboard);
        mCalculatorKeyboard.registerEditText(R.id.input_transaction_amount);
	}

    /**
     * Extension of SimpleCursorAdapter which is used to populate the fields for the list items
     * in the transactions suggestions (auto-complete transaction description).
     */
    private class DropDownCursorAdapter extends SimpleCursorAdapter{

        public DropDownCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
            super(context, layout, c, from, to, 0);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            super.bindView(view, context, cursor);
            String transactionUID = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.TransactionEntry.COLUMN_UID));
            Money balance = TransactionsDbAdapter.getInstance().getBalance(transactionUID, mAccountUID);

            long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseSchema.TransactionEntry.COLUMN_TIMESTAMP));
            String dateString = DateUtils.formatDateTime(getActivity(), timestamp,
                    DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR);

            TextView secondaryTextView = (TextView) view.findViewById(R.id.secondary_text);
            secondaryTextView.setText(balance.formattedString() + " on " + dateString); //TODO: Extract string
        }
    }

    /**
     * Initializes the transaction name field for autocompletion with existing transaction names in the database
     */
    private void initTransactionNameAutocomplete() {
        final int[] to = new int[]{R.id.primary_text};
        final String[] from = new String[]{DatabaseSchema.TransactionEntry.COLUMN_DESCRIPTION};

        SimpleCursorAdapter adapter = new DropDownCursorAdapter(
                getActivity(), R.layout.dropdown_item_2lines, null, from, to);

        adapter.setCursorToStringConverter(new SimpleCursorAdapter.CursorToStringConverter() {
            @Override
            public CharSequence convertToString(Cursor cursor) {
                final int colIndex = cursor.getColumnIndexOrThrow(DatabaseSchema.TransactionEntry.COLUMN_DESCRIPTION);
                return cursor.getString(colIndex);
            }
        });

        adapter.setFilterQueryProvider(new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence name) {
                return mTransactionsDbAdapter.fetchTransactionSuggestions(name == null ? "" : name.toString(), mAccountUID);
            }
        });

        mDescriptionEditText.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                mTransaction = new Transaction(mTransactionsDbAdapter.getTransaction(id), true);
                mTransaction.setTime(System.currentTimeMillis());
                //we check here because next method will modify it and we want to catch user-modification
                boolean amountEntered = mAmountInputFormatter.isInputModified();
                initializeViewsWithTransaction();
                List<Split> splitList = mTransaction.getSplits();
                boolean isSplitPair = splitList.size() == 2 && splitList.get(0).isPairOf(splitList.get(1));
                if (isSplitPair){
                    mSplitsList.clear();
                    if (!amountEntered) //if user already entered an amount
                        mAmountEditText.setText(splitList.get(0).getAmount().toPlainString());
                } else {
                    if (amountEntered){ //if user entered own amount, clear loaded splits and use the user value
                        mSplitsList.clear();
                        setAmountEditViewVisible(View.VISIBLE);
                    } else {
                        if (mUseDoubleEntry) { //don't hide the view in single entry mode
                            setAmountEditViewVisible(View.GONE);
                        }
                    }
                }
                mTransaction = null; //we are creating a new transaction after all
            }
        });

        mDescriptionEditText.setAdapter(adapter);
    }

    /**
	 * Initialize views in the fragment with information from a transaction.
	 * This method is called if the fragment is used for editing a transaction
	 */
	private void initializeViewsWithTransaction(){
		mDescriptionEditText.setText(mTransaction.getDescription());

        mTransactionTypeButton.setAccountType(mAccountType);
        mTransactionTypeButton.setChecked(mTransaction.getBalance(mAccountUID).isNegative());

		if (!mAmountInputFormatter.isInputModified()){
            //when autocompleting, only change the amount if the user has not manually changed it already
            mAmountEditText.setText(mTransaction.getBalance(mAccountUID).toPlainString());
        }
		mCurrencyTextView.setText(mTransaction.getCurrency().getSymbol(Locale.getDefault()));
		mNotesEditText.setText(mTransaction.getNote());
		mDateTextView.setText(DATE_FORMATTER.format(mTransaction.getTimeMillis()));
		mTimeTextView.setText(TIME_FORMATTER.format(mTransaction.getTimeMillis()));
		Calendar cal = GregorianCalendar.getInstance();
		cal.setTimeInMillis(mTransaction.getTimeMillis());
		mDate = mTime = cal;

        //TODO: deep copy the split list. We need a copy so we can modify with impunity
        mSplitsList = new ArrayList<>(mTransaction.getSplits());
        mAmountEditText.setEnabled(mSplitsList.size() <= 2);

        //if there are more than two splits (which is the default for one entry), then
        //disable editing of the transfer account. User should open editor
        if (mSplitsList.size() == 2 && mSplitsList.get(0).isPairOf(mSplitsList.get(1))) {
            for (Split split : mTransaction.getSplits()) {
                //two splits, one belongs to this account and the other to another account
                if (mUseDoubleEntry && !split.getAccountUID().equals(mAccountUID)) {
                    setSelectedTransferAccount(mAccountsDbAdapter.getID(split.getAccountUID()));
                }
            }
        } else {
            if (mUseDoubleEntry) {
                setAmountEditViewVisible(View.GONE);
            }
        }

		String currencyCode = mTransactionsDbAdapter.getAccountCurrencyCode(mAccountUID);
		Currency accountCurrency = Currency.getInstance(currencyCode);
		mCurrencyTextView.setText(accountCurrency.getSymbol());
        if (mMultiCurrency) {
            enableControls(false);
        }

        mSaveTemplateCheckbox.setChecked(mTransaction.isTemplate());
        String scheduledActionUID = getArguments().getString(UxArgument.SCHEDULED_ACTION_UID);
        if (scheduledActionUID != null && !scheduledActionUID.isEmpty()) {
            ScheduledAction scheduledAction = ScheduledActionDbAdapter.getInstance().getScheduledAction(scheduledActionUID);
            mRecurrenceRule = scheduledAction.getRuleString();
            mEventRecurrence.parse(mRecurrenceRule);
            mRecurrenceTextView.setText(scheduledAction.getRepeatString());
        }
    }

    private void enableControls(boolean b) {
        mDescriptionEditText.setEnabled(b);
        mNotesEditText.setEnabled(b);
        mDateTextView.setEnabled(b);
        mTimeTextView.setEnabled(b);
        mAmountEditText.setEnabled(b);
        mCurrencyTextView.setEnabled(b);
        mTransactionTypeButton.setEnabled(b);
        mDoubleAccountSpinner.setEnabled(b);
        // the next is always enabled, so the user can check the detailed info of splits
        // mOpenSplitsButton;
    }

    private void setAmountEditViewVisible(int visibility) {
        getView().findViewById(R.id.layout_double_entry).setVisibility(visibility);
        mTransactionTypeButton.setVisibility(visibility);
    }

    /**
	 * Initialize views with default data for new transactions
	 */
	private void initalizeViews() {
		Date time = new Date(System.currentTimeMillis());
		mDateTextView.setText(DATE_FORMATTER.format(time));
		mTimeTextView.setText(TIME_FORMATTER.format(time));
		mTime = mDate = Calendar.getInstance();

        mTransactionTypeButton.setAccountType(mAccountType);
		String typePref = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(getString(R.string.key_default_transaction_type), "DEBIT");
        mTransactionTypeButton.setChecked(TransactionType.valueOf(typePref));

		String code = Money.DEFAULT_CURRENCY_CODE;
		if (mAccountUID != null){
			code = mTransactionsDbAdapter.getAccountCurrencyCode(mAccountUID);
		}
		Currency accountCurrency = Currency.getInstance(code);
		mCurrencyTextView.setText(accountCurrency.getSymbol());

        if (mUseDoubleEntry){
            String currentAccountUID = mAccountUID;
            long defaultTransferAccountID = 0;
            String rootAccountUID = mAccountsDbAdapter.getOrCreateGnuCashRootAccountUID();
            do {
                defaultTransferAccountID = mAccountsDbAdapter.getDefaultTransferAccountID(mAccountsDbAdapter.getID(currentAccountUID));
                if (defaultTransferAccountID > 0) {
                    setSelectedTransferAccount(defaultTransferAccountID);
                    break; //we found a parent with default transfer setting
                }
                currentAccountUID = mAccountsDbAdapter.getParentAccountUID(currentAccountUID);
            } while (!currentAccountUID.equals(rootAccountUID));
        }
	}

    /**
     * Updates the list of possible transfer accounts.
     * Only accounts with the same currency can be transferred to
     */
	private void updateTransferAccountsList(){
		String conditions = "(" + DatabaseSchema.AccountEntry.COLUMN_UID + " != ?"
                            + " AND " + (mMultiCurrency ? "" : (DatabaseSchema.AccountEntry.COLUMN_CURRENCY + " = '" + mAccountsDbAdapter.getCurrencyCode(mAccountUID) + "'"
                            + " AND ")) + DatabaseSchema.AccountEntry.COLUMN_TYPE + " != ?"
                            + " AND " + DatabaseSchema.AccountEntry.COLUMN_PLACEHOLDER + " = 0"
                            + ")";

        if (mCursor != null) {
            mCursor.close();
        }
		mCursor = mAccountsDbAdapter.fetchAccountsOrderedByFullName(conditions, new String[]{mAccountUID, AccountType.ROOT.name()});

        mCursorAdapter = new QualifiedAccountNameCursorAdapter(getActivity(),
                android.R.layout.simple_spinner_item, mCursor);
		mCursorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mDoubleAccountSpinner.setAdapter(mCursorAdapter);
	}

    /**
     * Opens the split editor dialog
     */
    private void openSplitEditor(){
        if (mAmountEditText.getText().toString().length() == 0){
            Toast.makeText(getActivity(), "Please enter an amount to split", Toast.LENGTH_SHORT).show();
            return;
        }
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        String baseAmountString;

        if (mTransaction == null){ //if we are creating a new transaction (not editing an existing one)
            BigDecimal enteredAmount = parseInputToDecimal(mAmountEditText.getText().toString());
            baseAmountString = enteredAmount.toPlainString();
        } else {
            Money biggestAmount = Money.createZeroInstance(mTransaction.getCurrencyCode());
            for (Split split : mTransaction.getSplits()) {
                if (split.getAmount().asBigDecimal().compareTo(biggestAmount.asBigDecimal()) > 0)
                    biggestAmount = split.getAmount();
            }
            baseAmountString = biggestAmount.toPlainString();
        }

        SplitEditorDialogFragment splitEditorDialogFragment =
                SplitEditorDialogFragment.newInstance(baseAmountString);
        splitEditorDialogFragment.setTargetFragment(TransactionFormFragment.this, 0);
        splitEditorDialogFragment.show(fragmentManager, FRAGMENT_TAG_SPLITS_EDITOR);
    }
	/**
	 * Sets click listeners for the dialog buttons
	 */
	private void setListeners() {
        mAmountInputFormatter = new AmountTextWatcher(mAmountEditText); //new AmountInputFormatter(mAmountEditText);
        //mAmountEditText.addTextChangedListener(mAmountInputFormatter);
        mAmountEditText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int DRAWABLE_LEFT = 0;
                final int DRAWABLE_TOP = 1;
                final int DRAWABLE_RIGHT = 2;
                final int DRAWABLE_BOTTOM = 3;

                if(event.getAction() == MotionEvent.ACTION_UP) {
                    if(event.getRawX() >= (mAmountEditText.getRight() - mAmountEditText.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                        openSplitEditor();
                        return true;
                    }
                }
                return false;
            }
        });

        mAmountEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    String amountText = mAmountEditText.getText().toString();

                    if (amountText.trim().isEmpty())
                        return;

                    // FIXME: replace the decimal separator of the current locale with '.'
                    ExpressionBuilder expressionBuilder = new ExpressionBuilder(amountText);
                    Expression expression;

                    try {
                        expression = expressionBuilder.build();
                    } catch (RuntimeException e) {
                        setEditTextError();
                        return;
                    }

                    if (expression != null && expression.validate().isValid())
                        // FIXME: limit the decimal places
                        // FIXME: use the locale decimal separator
                        mAmountEditText.setText(Double.toString(expression.evaluate()));
                    else
                        setEditTextError();
                }
            }

            private void setEditTextError() {
                // FIXME: i18n
                mAmountEditText.setError("Invalid expression.");
                // TODO: log error
            }
        });

		mTransactionTypeButton.setAmountFormattingListener(mAmountEditText, mCurrencyTextView);

		mDateTextView.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				long dateMillis = 0;
				try {
					Date date = DATE_FORMATTER.parse(mDateTextView.getText().toString());
					dateMillis = date.getTime();
				} catch (ParseException e) {
					Log.e(getTag(), "Error converting input time to Date object");
				}
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(dateMillis);

                int year = calendar.get(Calendar.YEAR);
                int monthOfYear = calendar.get(Calendar.MONTH);
                int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
                CalendarDatePickerDialog datePickerDialog = CalendarDatePickerDialog.newInstance(TransactionFormFragment.this,
                        year, monthOfYear, dayOfMonth);
                datePickerDialog.show(getFragmentManager(), "date_picker_fragment");
			}
		});

		mTimeTextView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                long timeMillis = 0;
                try {
                    Date date = TIME_FORMATTER.parse(mTimeTextView.getText().toString());
                    timeMillis = date.getTime();
                } catch (ParseException e) {
                    Log.e(getTag(), "Error converting input time to Date object");
                }

                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(timeMillis);

                RadialTimePickerDialog timePickerDialog = RadialTimePickerDialog.newInstance(
                        TransactionFormFragment.this, calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE), true);
                timePickerDialog.show(getFragmentManager(), "time_picker_dialog_fragment");
            }
        });

        mRecurrenceTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentManager fm = getActivity().getSupportFragmentManager();
                Bundle b = new Bundle();
                Time t = new Time();
                t.setToNow();
                b.putLong(RecurrencePickerDialog.BUNDLE_START_TIME_MILLIS, t.toMillis(false));
                b.putString(RecurrencePickerDialog.BUNDLE_TIME_ZONE, t.timezone);

                // may be more efficient to serialize and pass in EventRecurrence
                b.putString(RecurrencePickerDialog.BUNDLE_RRULE, mRecurrenceRule);

                RecurrencePickerDialog rpd = (RecurrencePickerDialog) fm.findFragmentByTag(
                        FRAGMENT_TAG_RECURRENCE_PICKER);
                if (rpd != null) {
                    rpd.dismiss();
                }
                rpd = new RecurrencePickerDialog();
                rpd.setArguments(b);
                rpd.setOnRecurrenceSetListener(TransactionFormFragment.this);
                rpd.show(fm, FRAGMENT_TAG_RECURRENCE_PICKER);
            }
        });
	}

    /**
     * Updates the spinner to the selected transfer account
     * @param accountId Database ID of the transfer account
     */
	private void setSelectedTransferAccount(long accountId){
		for (int pos = 0; pos < mCursorAdapter.getCount(); pos++) {
			if (mCursorAdapter.getItemId(pos) == accountId){
                final int position = pos;
                mDoubleAccountSpinner.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mDoubleAccountSpinner.setSelection(position);
                    }
                }, 200);
				break;
			}
		}
	}

    /**
     * Callback when the account in the navigation bar is changed by the user
     * @param newAccountUID GUID of the newly selected account
     */
    public void onAccountChanged(String newAccountUID) {
        if (mMultiCurrency) {
            Toast.makeText(getActivity(), R.string.toast_error_edit_multi_currency_transaction, Toast.LENGTH_LONG).show();
            return;
        }
        AccountsDbAdapter accountsDbAdapter = AccountsDbAdapter.getInstance();
        String currencyCode = accountsDbAdapter.getCurrencyCode(newAccountUID);
        Currency currency = Currency.getInstance(currencyCode);
        mCurrencyTextView.setText(currency.getSymbol(Locale.getDefault()));

        mAccountType = accountsDbAdapter.getAccountType(newAccountUID);
        mTransactionTypeButton.setAccountType(mAccountType);
        mAccountUID = newAccountUID;
        updateTransferAccountsList();
    }

	/**
	 * Collects information from the fragment views and uses it to create
	 * and save a transaction
	 */
	private void saveNewTransaction() {
		Calendar cal = new GregorianCalendar(
				mDate.get(Calendar.YEAR),
				mDate.get(Calendar.MONTH),
				mDate.get(Calendar.DAY_OF_MONTH),
				mTime.get(Calendar.HOUR_OF_DAY),
				mTime.get(Calendar.MINUTE),
				mTime.get(Calendar.SECOND));
		String description = mDescriptionEditText.getText().toString();
		String notes = mNotesEditText.getText().toString();
		BigDecimal amountBigd = parseInputToDecimal(mAmountEditText.getText().toString());

		Currency currency = Currency.getInstance(mTransactionsDbAdapter.getAccountCurrencyCode(mAccountUID));
		Money amount 	= new Money(amountBigd, currency).absolute();

        //capture any edits which were done directly (not using split editor)
        if (mSplitsList.size() == 2 && mSplitsList.get(0).isPairOf(mSplitsList.get(1))) {
            //if it is a simple transfer where the editor was not used, then respect the button
            for (Split split : mSplitsList) {
                if (split.getAccountUID().equals(mAccountUID)){
                    split.setType(mTransactionTypeButton.getTransactionType());
                    split.setAmount(amount);
                } else {
                    split.setType(mTransactionTypeButton.getTransactionType().invert());
                    split.setAmount(amount);
                }
            }
        }

        Money splitSum = Money.createZeroInstance(currency.getCurrencyCode());
        for (Split split : mSplitsList) {
            Money amt = split.getAmount().absolute();
            if (split.getType() == TransactionType.DEBIT)
                splitSum = splitSum.subtract(amt);
            else
                splitSum = splitSum.add(amt);
        }
        mAccountsDbAdapter.beginTransaction();
        try {
            if (!splitSum.isAmountZero()) {
                Split imbSplit = new Split(splitSum.negate(), mAccountsDbAdapter.getOrCreateImbalanceAccountUID(currency));
                mSplitsList.add(imbSplit);
            }
            if (mTransaction != null) { //if editing an existing transaction
                mTransaction.setSplits(mSplitsList);
                mTransaction.setDescription(description);
            } else {
                mTransaction = new Transaction(description);

                if (mSplitsList.isEmpty()) { //amount entered in the simple interface (not using splits Editor)
                    Split split = new Split(amount, mAccountUID);
                    split.setType(mTransactionTypeButton.getTransactionType());
                    mTransaction.addSplit(split);

                    String transferAcctUID;
                    if (mUseDoubleEntry) {
                        long transferAcctId = mDoubleAccountSpinner.getSelectedItemId();
                        transferAcctUID = mAccountsDbAdapter.getUID(transferAcctId);
                    } else {
                        transferAcctUID = mAccountsDbAdapter.getOrCreateImbalanceAccountUID(currency);
                    }
                    mTransaction.addSplit(split.createPair(transferAcctUID));
                } else { //split editor was used to enter splits
                    mTransaction.setSplits(mSplitsList);
                }
            }

            mTransaction.setCurrencyCode(mAccountsDbAdapter.getAccountCurrencyCode(mAccountUID));
            mTransaction.setTime(cal.getTimeInMillis());
            mTransaction.setNote(notes);

            // set as not exported because we have just edited it
            mTransaction.setExported(false);
            mTransactionsDbAdapter.addTransaction(mTransaction);

            if (mSaveTemplateCheckbox.isChecked()) {//template is automatically checked when a transaction is scheduled
                if (!mEditMode) { //means it was new transaction, so a new template
                    Transaction templateTransaction = new Transaction(mTransaction, true);
                    templateTransaction.setTemplate(true);
                    mTransactionsDbAdapter.addTransaction(templateTransaction);
                    scheduleRecurringTransaction(templateTransaction.getUID());
                } else
                    scheduleRecurringTransaction(mTransaction.getUID());
            } else {
                String scheduledActionUID = getArguments().getString(UxArgument.SCHEDULED_ACTION_UID);
                if (scheduledActionUID != null){ //we were editing a schedule and it was turned off
                    ScheduledActionDbAdapter.getInstance().deleteRecord(scheduledActionUID);
                }
            }

            mAccountsDbAdapter.setTransactionSuccessful();
        }
        finally {
            mAccountsDbAdapter.endTransaction();
        }

        //update widgets, if any
		WidgetConfigurationActivity.updateAllWidgets(getActivity().getApplicationContext());

		finish();
	}

    /**
     * Schedules a recurring transaction (if necessary) after the transaction has been saved
     * @see #saveNewTransaction()
     */
    private void scheduleRecurringTransaction(String transactionUID) {
        ScheduledActionDbAdapter scheduledActionDbAdapter = ScheduledActionDbAdapter.getInstance();

        List<ScheduledAction> events = RecurrenceParser.parse(mEventRecurrence,
                ScheduledAction.ActionType.TRANSACTION);

        String scheduledActionUID = getArguments().getString(UxArgument.SCHEDULED_ACTION_UID);

        if (scheduledActionUID != null) { //if we are editing an existing schedule
            if ( events.size() == 1) {
                ScheduledAction scheduledAction = events.get(0);
                scheduledAction.setUID(scheduledActionUID);
                scheduledActionDbAdapter.updateRecurrenceAttributes(scheduledAction);
                Toast.makeText(getActivity(), "Updated transaction schedule", Toast.LENGTH_SHORT).show();
                return;
            } else {
                //if user changed scheduled action so that more than one new schedule would be saved,
                // then remove the old one
                ScheduledActionDbAdapter.getInstance().deleteRecord(scheduledActionUID);
            }
        }

        for (ScheduledAction event : events) {
            event.setActionUID(transactionUID);
            scheduledActionDbAdapter.addScheduledAction(event);

            Log.i("TransactionFormFragment", event.toString());
        }
        Toast.makeText(getActivity(), "Scheduled recurring transaction", Toast.LENGTH_SHORT).show();

        //TODO: localize this toast string for all supported locales

    }


    @Override
	public void onDestroyView() {
		super.onDestroyView();
		if (mCursor != null)
			mCursor.close();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.default_save_actions, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		//hide the keyboard if it is visible
		InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(mDescriptionEditText.getApplicationWindowToken(), 0);

		switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;

		case R.id.menu_save:
            if (mMultiCurrency) {
                Toast.makeText(getActivity(), R.string.toast_error_edit_multi_currency_transaction, Toast.LENGTH_LONG).show();
                finish();
            }
            else if (mAmountEditText.getText().length() == 0) {
                Toast.makeText(getActivity(), R.string.toast_transanction_amount_required, Toast.LENGTH_SHORT).show();
            } else if (mUseDoubleEntry && mDoubleAccountSpinner.getCount() == 0){
                Toast.makeText(getActivity(),
                        R.string.toast_disable_double_entry_to_save_transaction,
                        Toast.LENGTH_LONG).show();
            } else {
                saveNewTransaction();
            }
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

    /**
     * Called by the split editor fragment to notify of finished editing
     * @param splitList List of splits produced in the fragment
     */
    public void setSplitList(List<Split> splitList, List<String> removedSplitUIDs){
        mSplitsList = splitList;
        Money balance = Transaction.computeBalance(mAccountUID, mSplitsList);

        mAmountEditText.setText(balance.toPlainString());
        mTransactionTypeButton.setChecked(balance.isNegative());
        //once we set the split list, do not allow direct editing of the total
        if (mSplitsList.size() > 1){
            mAmountEditText.setEnabled(false);
            setAmountEditViewVisible(View.GONE);
        }
    }

    /**
     * Returns the list of splits currently in editing
     * @return List of splits
     */
    public List<Split> getSplitList(){
        return mSplitsList;
    }

	/**
	 * Finishes the fragment appropriately.
	 * Depends on how the fragment was loaded, it might have a backstack or not
	 */
	private void finish() {
		if (getActivity().getSupportFragmentManager().getBackStackEntryCount() == 0){
			//means we got here directly from the accounts list activity, need to finish
			getActivity().finish();
		} else {
			//go back to transactions list
			getActivity().getSupportFragmentManager().popBackStack();
		}
	}

    @Override
    public void onDateSet(CalendarDatePickerDialog calendarDatePickerDialog, int year, int monthOfYear, int dayOfMonth) {
        Calendar cal = new GregorianCalendar(year, monthOfYear, dayOfMonth);
        mDateTextView.setText(DATE_FORMATTER.format(cal.getTime()));
        mDate.set(Calendar.YEAR, year);
        mDate.set(Calendar.MONTH, monthOfYear);
        mDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
    }

    @Override
    public void onTimeSet(RadialTimePickerDialog radialTimePickerDialog, int hourOfDay, int minute) {
        Calendar cal = new GregorianCalendar(0, 0, 0, hourOfDay, minute);
        mTimeTextView.setText(TIME_FORMATTER.format(cal.getTime()));
        mTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
        mTime.set(Calendar.MINUTE, minute);
    }

	/**
	 * Strips formatting from a currency string.
	 * All non-digit information is removed, but the sign is preserved.
	 * @param s String to be stripped
	 * @return Stripped string with all non-digits removed
	 */
	public static String stripCurrencyFormatting(String s){
        if (s.length() == 0)
            return s;
		//remove all currency formatting and anything else which is not a number
        String sign = s.trim().substring(0,1);
        String stripped = s.trim().replaceAll("\\D*", "");
        if (stripped.length() == 0)
            return "";
        if (sign.equals("+") || sign.equals("-")){
            stripped = sign + stripped;
        }
		return stripped;
	}

	/**
	 * Parse an input string into a {@link BigDecimal}
	 * This method expects the amount including the decimal part
	 * @param amountString String with amount information
	 * @return BigDecimal with the amount parsed from <code>amountString</code>
	 */
	public static BigDecimal parseInputToDecimal(String amountString){
		String clean = stripCurrencyFormatting(amountString);
        if (clean.length() == 0) //empty string
                return BigDecimal.ZERO;
		//all amounts are input to 2 decimal places, so after removing decimal separator, divide by 100
        //TODO: Handle currencies with different kinds of decimal places
		return new BigDecimal(clean).setScale(2,
				RoundingMode.HALF_EVEN).divide(new BigDecimal(100), 2,
				RoundingMode.HALF_EVEN);
	}


    @Override
    public void onRecurrenceSet(String rrule) {
        mRecurrenceRule = rrule;
        String repeatString = getString(R.string.label_tap_to_create_schedule);
        if (mRecurrenceRule != null){
            mEventRecurrence.parse(mRecurrenceRule);
            repeatString = EventRecurrenceFormatter.getRepeatString(getActivity(), getResources(), mEventRecurrence, true);

            //when recurrence is set, we will definitely be saving a template
            mSaveTemplateCheckbox.setChecked(true);
            mSaveTemplateCheckbox.setEnabled(false);
        } else {
            mSaveTemplateCheckbox.setEnabled(true);
            mSaveTemplateCheckbox.setChecked(false);
        }

        mRecurrenceTextView.setText(repeatString);
    }
    private class AmountTextWatcher extends AmountInputFormatter {

        public AmountTextWatcher(EditText amountInput) {
            super(amountInput);
        }

        @Override
        public void afterTextChanged(Editable s) {
            String value = s.toString();
            if (value.length() > 0 && mTransactionTypeButton.isChecked()){
                if (s.charAt(0) != '-'){
                    s = Editable.Factory.getInstance().newEditable("-" + value);
                }
            }
            super.afterTextChanged(s);
        }
    }
}
