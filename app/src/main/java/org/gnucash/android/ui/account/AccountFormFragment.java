/*
 * Copyright (c) 2012 - 2014 Ngewi Fet <ngewif@gmail.com>
 * Copyright (c) 2014 Yongxin Wang <fefe.wyx@gmail.com>
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

package org.gnucash.android.ui.account;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.widget.CompoundButton.OnCheckedChangeListener;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import org.gnucash.android.R;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.AccountType;
import org.gnucash.android.model.Money;
import org.gnucash.android.ui.UxArgument;
import org.gnucash.android.ui.colorpicker.ColorPickerDialog;
import org.gnucash.android.ui.colorpicker.ColorPickerSwatch;
import org.gnucash.android.ui.colorpicker.ColorSquare;
import org.gnucash.android.util.QualifiedAccountNameCursorAdapter;

import java.util.*;

/**
 * Fragment used for creating and editing accounts
 * @author Ngewi Fet <ngewif@gmail.com>
 * @author Yongxin Wang <fefe.wyx@gmail.com>
 */
public class AccountFormFragment extends SherlockFragment {

    /**
     * Tag for the color picker dialog fragment
     */
    public static final String COLOR_PICKER_DIALOG_TAG = "color_picker_dialog";

    /**
	 * EditText for the name of the account to be created/edited
	 */
	private EditText mNameEditText;
	
	/**
	 * Spinner for selecting the currency of the account
	 * Currencies listed are those specified by ISO 4217
	 */
	private Spinner mCurrencySpinner;
	
	/**
	 * Accounts database adapter
	 */
	private AccountsDbAdapter mAccountsDbAdapter;

    /**
     * Whether the AccountsDbAdapter is created inside this class.
     * If so, it should be also closed by this class
     */
    private boolean mReleaseDbAdapter = false;
	
	/**
	 * List of all currency codes (ISO 4217) supported by the app
	 */
	private List<String> mCurrencyCodes;

    /**
     * GUID of the parent account
     * This value is set to the parent account of the transaction being edited or
     * the account in which a new sub-account is being created
     */
    private String mParentAccountUID = null;

    /**
     * Account ID of the root account
     */
    private long mRootAccountId = -1;

    /**
     * Account UID of the root account
     */
    private String mRootAccountUID = null;

	/**
	 * Reference to account object which will be created at end of dialog
	 */
	private Account mAccount = null;

    /**
     * Unique ID string of account being edited
     */
    private String mAccountUID = null;

    /**
     * Cursor which will hold set of eligible parent accounts
     */
	private Cursor mParentAccountCursor;

    /**
     * List of all descendant Account UIDs, if we are modifying an account
     * null if creating a new account
     */
    private List<String> mDescendantAccountUIDs;

    /**
     * SimpleCursorAdapter for the parent account spinner
     * @see QualifiedAccountNameCursorAdapter
     */
	private SimpleCursorAdapter mParentAccountCursorAdapter;

    /**
     * Spinner for parent account list
     */
	private Spinner mParentAccountSpinner;

    /**
     * Checkbox which activates the parent account spinner when selected
     * Leaving this unchecked means it is a top-level root account
     */
	private CheckBox mParentCheckBox;

    /**
     * Spinner for the account type
     * @see org.gnucash.android.model.AccountType
     */
    private Spinner mAccountTypeSpinner;

    /**
     * Checkbox for activating the default transfer account spinner
     */
    private CheckBox mDefaultTransferAccountCheckBox;

    /**
     * Spinner for selecting the default transfer account
     */
    private Spinner mDefaulTransferAccountSpinner;

    /**
     * Checkbox indicating if account is a placeholder account
     */
    private CheckBox mPlaceholderCheckBox;

    /**
     * Cursor adapter which binds to the spinner for default transfer account
     */
    private SimpleCursorAdapter mDefaultTransferAccountCursorAdapter;

    /**
     * Flag indicating if double entry transactions are enabled
     */
    private boolean mUseDoubleEntry;

    /**
     * Default to transparent
     */
    private String mSelectedColor = null;

    /**
     * Trigger for color picker dialog
     */
    private ColorSquare mColorSquare;

    private ColorPickerSwatch.OnColorSelectedListener mColorSelectedListener = new ColorPickerSwatch.OnColorSelectedListener() {
        @Override
        public void onColorSelected(int color) {
            mColorSquare.setBackgroundColor(color);
            mSelectedColor = String.format("#%06X", (0xFFFFFF & color));
        }
    };


    /**
	 * Default constructor
	 * Required, else the app crashes on screen rotation
	 */
	public AccountFormFragment() {
		//nothing to see here, move along
	}
	
	/**
	 * Construct a new instance of the dialog
	 * @param dbAdapter {@link AccountsDbAdapter} for saving the account
	 * @return New instance of the dialog fragment
	 */
	static public AccountFormFragment newInstance(AccountsDbAdapter dbAdapter){
		AccountFormFragment f = new AccountFormFragment();
		f.mAccountsDbAdapter = dbAdapter;
        f.mReleaseDbAdapter = false;
		return f;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
        if (mAccountsDbAdapter == null){
            mReleaseDbAdapter = true;
            mAccountsDbAdapter = new AccountsDbAdapter(getSherlockActivity());
        }

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mUseDoubleEntry = sharedPrefs.getBoolean(getString(R.string.key_use_double_entry), true);
	}
	
	/**
	 * Inflates the dialog view and retrieves references to the dialog elements
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_new_account, container, false);
		getSherlockActivity().getSupportActionBar().setTitle(R.string.title_add_account);
		mCurrencySpinner = (Spinner) view.findViewById(R.id.input_currency_spinner);
		mNameEditText = (EditText) view.findViewById(R.id.edit_text_account_name);
		//mNameEditText.requestFocus();

        mAccountTypeSpinner = (Spinner) view.findViewById(R.id.input_account_type_spinner);
        mAccountTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                loadParentAccountList(getSelectedAccountType());
                setParentAccountSelection(mAccountsDbAdapter.getID(mParentAccountUID));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                //nothing to see here, move along
            }
        });

        mPlaceholderCheckBox = (CheckBox) view.findViewById(R.id.checkbox_placeholder_account);

		mParentAccountSpinner = (Spinner) view.findViewById(R.id.input_parent_account);
		mParentAccountSpinner.setEnabled(false);

		mParentCheckBox = (CheckBox) view.findViewById(R.id.checkbox_parent_account);
		mParentCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mParentAccountSpinner.setEnabled(isChecked);
			}
		});

        mDefaulTransferAccountSpinner = (Spinner) view.findViewById(R.id.input_default_transfer_account);
        mDefaulTransferAccountSpinner.setEnabled(false);

        mDefaultTransferAccountCheckBox = (CheckBox) view.findViewById(R.id.checkbox_default_transfer_account);
        mDefaultTransferAccountCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                mDefaulTransferAccountSpinner.setEnabled(isChecked);
            }
        });

        mColorSquare = (ColorSquare) view.findViewById(R.id.input_color_picker);
        mColorSquare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showColorPickerDialog();
            }
        });

		return view;
	}
	

	/**
	 * Initializes the values of the views in the dialog
	 */
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		ArrayAdapter<String> currencyArrayAdapter = new ArrayAdapter<String>(
				getActivity(), 
				android.R.layout.simple_spinner_item, 
				getResources().getStringArray(R.array.currency_names));		
		currencyArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mCurrencySpinner.setAdapter(currencyArrayAdapter);

        mAccountUID = getArguments().getString(UxArgument.SELECTED_ACCOUNT_UID);

        if (mAccountUID != null) {
            mAccount = mAccountsDbAdapter.getAccount(mAccountUID);
            getSherlockActivity().getSupportActionBar().setTitle(R.string.title_edit_account);
        }
        mRootAccountUID = mAccountsDbAdapter.getGnuCashRootAccountUID();
        mRootAccountId = mAccountsDbAdapter.getAccountID(mRootAccountUID);

        //need to load the cursor adapters for the spinners before initializing the views
        loadAccountTypesList();
        loadDefaultTransferAccountList();
        setDefaultTransferAccountInputsVisible(mUseDoubleEntry);

        if (mAccount != null){
            initializeViewsWithAccount(mAccount);
        } else {
            initializeViews();
        }


	}

    /**
     * Initialize view with the properties of <code>account</code>.
     * This is applicable when editing an account
     * @param account Account whose fields are used to populate the form
     */
    private void initializeViewsWithAccount(Account account){
        if (account == null)
            throw new IllegalArgumentException("Account cannot be null");

        loadParentAccountList(account.getAccountType());
        mParentAccountUID = account.getParentUID();
        if (mParentAccountUID == null) {
            // null parent, set Parent as root
            mParentAccountUID = mRootAccountUID;
        }
        setParentAccountSelection(mAccountsDbAdapter.getID(mParentAccountUID));

        String currencyCode = account.getCurrency().getCurrencyCode();
        setSelectedCurrency(currencyCode);

        if (mAccountsDbAdapter.getTransactionMaxSplitNum(mAccount.getUID()) > 1)
        {
            mCurrencySpinner.setEnabled(false);
        }

        mNameEditText.setText(account.getName());

        if (mUseDoubleEntry) {
            long doubleDefaultAccountId = mAccountsDbAdapter.getAccountID(account.getDefaultTransferAccountUID());
            setDefaultTransferAccountSelection(doubleDefaultAccountId);
        }

        mPlaceholderCheckBox.setChecked(account.isPlaceholderAccount());
        initializeColorSquarePreview(account.getColorHexCode());

        setAccountTypeSelection(account.getAccountType());
    }

    /**
     * Initialize views with defaults for new account
     */
    private void initializeViews(){
        setSelectedCurrency(Money.DEFAULT_CURRENCY_CODE);
        mColorSquare.setBackgroundColor(Color.LTGRAY);
        mParentAccountUID = getArguments().getString(UxArgument.PARENT_ACCOUNT_UID);


        if (mParentAccountUID != null) {
            AccountType parentAccountType = mAccountsDbAdapter.getAccountType(mParentAccountUID);
            setAccountTypeSelection(parentAccountType);
            loadParentAccountList(parentAccountType);
            setParentAccountSelection(mAccountsDbAdapter.getID(mParentAccountUID));
        }

    }

    /**
     * Initializes the preview of the color picker (color square) to the specified color
     * @param colorHex Color of the format #rgb or #rrggbb
     */
    private void initializeColorSquarePreview(String colorHex){
        if (colorHex != null)
            mColorSquare.setBackgroundColor(Color.parseColor(colorHex));
        else
            mColorSquare.setBackgroundColor(Color.LTGRAY);
    }

    /**
     * Selects the corresponding account type in the spinner
     * @param accountType AccountType to be set
     */
    private void setAccountTypeSelection(AccountType accountType){
        String[] accountTypeEntries = getResources().getStringArray(R.array.key_account_type_entries);
        int accountTypeIndex = Arrays.asList(accountTypeEntries).indexOf(accountType.name());
        mAccountTypeSpinner.setSelection(accountTypeIndex);
    }

    /**
     * Toggles the visibility of the default transfer account input fields.
     * This field is irrelevant for users who do not use double accounting
     */
    private void setDefaultTransferAccountInputsVisible(boolean visible) {
        final int visibility = visible ? View.VISIBLE : View.GONE;
        final View view = getView();
        view.findViewById(R.id.layout_default_transfer_account).setVisibility(visibility);
        view.findViewById(R.id.label_default_transfer_account).setVisibility(visibility);
    }

    /**
     * Selects the currency with code <code>currencyCode</code> in the spinner
     * @param currencyCode ISO 4217 currency code to be selected
     */
    private void setSelectedCurrency(String currencyCode){
        mCurrencyCodes = Arrays.asList(getResources().getStringArray(R.array.key_currency_codes));
        if (mCurrencyCodes.contains(currencyCode)){
            mCurrencySpinner.setSelection(mCurrencyCodes.indexOf(currencyCode));
        }
    }

    /**
     * Selects the account with ID <code>parentAccountId</code> in the parent accounts spinner
     * @param parentAccountId Record ID of parent account to be selected
     */
    private void setParentAccountSelection(long parentAccountId){
        if (parentAccountId > 0 && parentAccountId != mRootAccountId){
            mParentCheckBox.setChecked(true);
            mParentAccountSpinner.setEnabled(true);
        } else
            return;

        for (int pos = 0; pos < mParentAccountCursorAdapter.getCount(); pos++) {
            if (mParentAccountCursorAdapter.getItemId(pos) == parentAccountId){
                mParentAccountSpinner.setSelection(pos, true);
                break;
            }
        }
    }

    /**
     * Selects the account with ID <code>parentAccountId</code> in the default transfer account spinner
     * @param defaultTransferAccountId Record ID of parent account to be selected
     */
    private void setDefaultTransferAccountSelection(long defaultTransferAccountId){
        if (defaultTransferAccountId > 0){
            mDefaultTransferAccountCheckBox.setChecked(true);
            mDefaulTransferAccountSpinner.setEnabled(true);
        } else
            return;

        for (int pos = 0; pos < mDefaultTransferAccountCursorAdapter.getCount(); pos++) {
            if (mDefaultTransferAccountCursorAdapter.getItemId(pos) == defaultTransferAccountId){
                mDefaulTransferAccountSpinner.setSelection(pos);
                break;
            }
        }
    }

    /**
     * Returns an array of colors used for accounts.
     * The array returned has the actual color values and not the resource ID.
     * @return Integer array of colors used for accounts
     */
    private int[] getAccountColorOptions(){
        Resources res = getResources();
        TypedArray colorTypedArray = res.obtainTypedArray(R.array.account_colors);
        int[] colorOptions = new int[colorTypedArray.length()];
        for (int i = 0; i < colorTypedArray.length(); i++) {
             int color = colorTypedArray.getColor(i, R.color.title_green);
             colorOptions[i] = color;
        }
        return colorOptions;
    }
    /**
     * Shows the color picker dialog
     */
    private void showColorPickerDialog(){
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        int currentColor = Color.LTGRAY;
        if (mAccount != null){
            String accountColor = mAccount.getColorHexCode();
            if (accountColor != null){
                currentColor = Color.parseColor(accountColor);
            }
        }

        ColorPickerDialog colorPickerDialogFragment = ColorPickerDialog.newInstance(
                R.string.color_picker_default_title,
                getAccountColorOptions(),
                currentColor, 4, 12);
        colorPickerDialogFragment.setOnColorSelectedListener(mColorSelectedListener);
        colorPickerDialogFragment.show(fragmentManager, COLOR_PICKER_DIALOG_TAG);
    }

    @Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {		
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.default_save_actions, menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_save:
			saveAccount();
			return true;

		case R.id.menu_cancel:
			finishFragment();
			return true;
		}
		
		return false;
	}

    /**
     * Initializes the default transfer account spinner with eligible accounts
     */
    private void loadDefaultTransferAccountList(){
        String condition = DatabaseSchema.AccountEntry.COLUMN_UID + " != '" + mAccountUID + "' "
                + " AND " + DatabaseSchema.AccountEntry.COLUMN_PLACEHOLDER + "=0"
                + " AND " + DatabaseSchema.AccountEntry.COLUMN_UID + " != '" + mAccountsDbAdapter.getGnuCashRootAccountUID() + "'";
        /*
      Cursor holding data set of eligible transfer accounts
     */
        Cursor defaultTransferAccountCursor = mAccountsDbAdapter.fetchAccountsOrderedByFullName(condition);

        if (defaultTransferAccountCursor == null || mDefaulTransferAccountSpinner.getCount() <= 0){
            setDefaultTransferAccountInputsVisible(false);
        }

        mDefaultTransferAccountCursorAdapter = new QualifiedAccountNameCursorAdapter(getActivity(),
                android.R.layout.simple_spinner_item,
                defaultTransferAccountCursor);
        mDefaultTransferAccountCursorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mDefaulTransferAccountSpinner.setAdapter(mDefaultTransferAccountCursorAdapter);
    }

    /**
     * Loads the list of possible accounts which can be set as a parent account and initializes the spinner.
     * The allowed parent accounts depends on the account type
     * @param accountType AccountType of account whose allowed parent list is to be loaded
     */
	private void loadParentAccountList(AccountType accountType){
        String condition = DatabaseSchema.SplitEntry.COLUMN_TYPE + " IN ("
                + getAllowedParentAccountTypes(accountType) + ") ";

        if (mAccount != null){  //if editing an account
            mDescendantAccountUIDs = mAccountsDbAdapter.getDescendantAccountUIDs(mAccount.getUID(), null, null);
            String rootAccountUID = mAccountsDbAdapter.getGnuCashRootAccountUID();
            List<String> descendantAccountUIDs = new ArrayList<String>(mDescendantAccountUIDs);
            if (rootAccountUID != null)
                descendantAccountUIDs.add(rootAccountUID);
            // limit cyclic account hierarchies.
            condition += " AND (" + DatabaseSchema.AccountEntry.COLUMN_UID + " NOT IN ( '"
                    + TextUtils.join("','", descendantAccountUIDs) + "','" + mAccountUID + "' ) )";
        }

        //if we are reloading the list, close the previous cursor first
        if (mParentAccountCursor != null)
            mParentAccountCursor.close();

		mParentAccountCursor = mAccountsDbAdapter.fetchAccountsOrderedByFullName(condition);
		if (mParentAccountCursor == null || mParentAccountCursor.getCount() <= 0){
            final View view = getView();
            view.findViewById(R.id.layout_parent_account).setVisibility(View.GONE);
            view.findViewById(R.id.label_parent_account).setVisibility(View.GONE);
        }

		mParentAccountCursorAdapter = new QualifiedAccountNameCursorAdapter(
				getActivity(), 
				android.R.layout.simple_spinner_item,
                mParentAccountCursor);
		mParentAccountCursorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mParentAccountSpinner.setAdapter(mParentAccountCursorAdapter);
	}

    /**
     * Returns a comma separated list of account types which can be parent accounts for the specified <code>type</code>.
     * The strings in the list are the {@link org.gnucash.android.model.AccountType#name()}s of the different types.
     * @param type {@link org.gnucash.android.model.AccountType}
     * @return String comma separated list of account types
     */
    private String getAllowedParentAccountTypes(AccountType type) {

        switch (type) {
            case EQUITY:
                return "'" + AccountType.EQUITY.name() + "'";

            case INCOME:
            case EXPENSE:
                return "'" + AccountType.EXPENSE.name() + "', '" + AccountType.INCOME.name() + "'";

            case CASH:
            case BANK:
            case CREDIT:
            case ASSET:
            case LIABILITY:
            case PAYABLE:
            case RECEIVABLE:
            case CURRENCY:
            case STOCK:
            case MUTUAL: {
                List<String> accountTypeStrings = getAccountTypeStringList();
                accountTypeStrings.remove(AccountType.EQUITY.name());
                accountTypeStrings.remove(AccountType.EXPENSE.name());
                accountTypeStrings.remove(AccountType.INCOME.name());
                accountTypeStrings.remove(AccountType.ROOT.name());
                return "'" + TextUtils.join("','", accountTypeStrings) + "'";
            }

            case TRADING:
                return "'" + AccountType.TRADING.name() + "'";

            case ROOT:
            default:
                return Arrays.toString(AccountType.values()).replaceAll("\\[|]", "");
        }
    }

    /**
     * Returns a list of all the available {@link org.gnucash.android.model.AccountType}s as strings
     * @return String list of all account types
     */
    private List<String> getAccountTypeStringList(){
        String[] accountTypes = Arrays.toString(AccountType.values()).replaceAll("\\[|]", "").split(",");
        List<String> accountTypesList = new ArrayList<String>();
        for (String accountType : accountTypes) {
            accountTypesList.add(accountType.trim());
        }

        return accountTypesList;
    }
    /**
     * Loads the list of account types into the account type selector spinner
     */
    private void loadAccountTypesList(){
        String[] accountTypes = getResources().getStringArray(R.array.account_type_entry_values);
        ArrayAdapter<String> accountTypesAdapter = new ArrayAdapter<String>(
                getActivity(), android.R.layout.simple_list_item_1, accountTypes);

        accountTypesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mAccountTypeSpinner.setAdapter(accountTypesAdapter);

    }

	/**
	 * Finishes the fragment appropriately.
	 * Depends on how the fragment was loaded, it might have a backstack or not
	 */
	private void finishFragment() {
		InputMethodManager imm = (InputMethodManager) getSherlockActivity().getSystemService(
			      Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(mNameEditText.getWindowToken(), 0);

        final String action = getActivity().getIntent().getAction();
        if (action != null && action.equals(Intent.ACTION_INSERT_OR_EDIT)){
            getActivity().setResult(Activity.RESULT_OK);
            getActivity().finish();
        } else {
		    getSherlockActivity().getSupportFragmentManager().popBackStack();
        }
	}
	
	@Override
	public void onDestroy() {
		super.onDestroyView();
		if (mParentAccountCursor != null)
			mParentAccountCursor.close();
        // The mAccountsDbAdapter should only be closed when it is not passed in
        // by other Activities.
		if (mReleaseDbAdapter && mAccountsDbAdapter != null) {
            mAccountsDbAdapter.close();
        }
        if (mDefaultTransferAccountCursorAdapter != null) {
            mDefaultTransferAccountCursorAdapter.getCursor().close();
        }
	}

    /**
     * Reads the fields from the account form and saves as a new account
     */
	private void saveAccount() {
        // accounts to update, in case we're updating full names of a sub account tree
        ArrayList<Account> accountsToUpdate = new ArrayList<Account>();
        boolean nameChanged = false;
		if (mAccount == null){
			String name = getEnteredName();
			if (name == null || name.length() == 0){
				Toast.makeText(getSherlockActivity(), 
						R.string.toast_no_account_name_entered, 
						Toast.LENGTH_LONG).show();
				return;				
			}
			mAccount = new Account(getEnteredName());
		}
		else {
            nameChanged = !mAccount.getName().equals(getEnteredName());
            mAccount.setName(getEnteredName());
        }
			
		String curCode = mCurrencyCodes.get(mCurrencySpinner
				.getSelectedItemPosition());
		mAccount.setCurrency(Currency.getInstance(curCode));

        AccountType selectedAccountType = getSelectedAccountType();
        mAccount.setAccountType(selectedAccountType);

        mAccount.setPlaceHolderFlag(mPlaceholderCheckBox.isChecked());
        mAccount.setColorCode(mSelectedColor);

        long newParentAccountId;
        String newParentAccountUID;
		if (mParentCheckBox.isChecked()) {
            newParentAccountId = mParentAccountSpinner.getSelectedItemId();
            newParentAccountUID = mAccountsDbAdapter.getAccountUID(newParentAccountId);
            mAccount.setParentUID(newParentAccountUID);
        } else {
            //need to do this explicitly in case user removes parent account
            newParentAccountUID = mRootAccountUID;
            newParentAccountId = mRootAccountId;
		}
        mAccount.setParentUID(newParentAccountUID);

        if (mDefaultTransferAccountCheckBox.isChecked()){
            long id = mDefaulTransferAccountSpinner.getSelectedItemId();
            mAccount.setDefaultTransferAccountUID(mAccountsDbAdapter.getAccountUID(id));
        } else {
            //explicitly set in case of removal of default account
            mAccount.setDefaultTransferAccountUID(null);
        }

        long parentAccountId = mAccountsDbAdapter.getID(mParentAccountUID);
        // update full names
        if (nameChanged || mDescendantAccountUIDs == null || newParentAccountId != parentAccountId) {
            // current account name changed or new Account or parent account changed
            String newAccountFullName;
            if (newParentAccountId == mRootAccountId){
                newAccountFullName = mAccount.getName();
            }
            else {
                newAccountFullName = mAccountsDbAdapter.getAccountFullName(newParentAccountUID) +
                    AccountsDbAdapter.ACCOUNT_NAME_SEPARATOR + mAccount.getName();
            }
            mAccount.setFullName(newAccountFullName);
            if (mDescendantAccountUIDs != null) {
                // modifying existing account, e.t. name changed and/or parent changed
                if ((nameChanged || parentAccountId != newParentAccountId) && mDescendantAccountUIDs.size() > 0) {
                    // parent change, update all full names of descent accounts
                    accountsToUpdate.addAll(mAccountsDbAdapter.getSimpleAccountList(
                            DatabaseSchema.AccountEntry.COLUMN_UID + " IN ('" +
                                    TextUtils.join("','", mDescendantAccountUIDs) + "')",
                            null,
                            null
                    ));
                }
                HashMap<String, Account> mapAccount = new HashMap<String, Account>();
                for (Account acct : accountsToUpdate) mapAccount.put(acct.getUID(), acct);
                for (String uid: mDescendantAccountUIDs) {
                    // mAccountsDbAdapter.getDescendantAccountUIDs() will ensure a parent-child order
                    Account acct = mapAccount.get(uid);
                    // mAccount cannot be root, so acct here cannot be top level account.
                    if (acct.getParentUID().equals(mAccount.getUID())) {
                        acct.setFullName(mAccount.getFullName() + AccountsDbAdapter.ACCOUNT_NAME_SEPARATOR + acct.getName());
                    }
                    else {
                        acct.setFullName(
                                mapAccount.get(acct.getParentUID()).getFullName() +
                                        AccountsDbAdapter.ACCOUNT_NAME_SEPARATOR +
                                        acct.getName()
                        );
                    }
                }
            }
        }
        accountsToUpdate.add(mAccount);
		if (mAccountsDbAdapter == null)
			mAccountsDbAdapter = new AccountsDbAdapter(getActivity());
        // bulk update, will not update transactions
		mAccountsDbAdapter.bulkAddAccounts(accountsToUpdate);

		finishFragment();
	}

    /**
     * Returns the currently selected account type in the spinner
     * @return {@link org.gnucash.android.model.AccountType} currently selected
     */
    private AccountType getSelectedAccountType() {
        int selectedAccountTypeIndex = mAccountTypeSpinner.getSelectedItemPosition();
        String[] accountTypeEntries = getResources().getStringArray(R.array.key_account_type_entries);
        return AccountType.valueOf(accountTypeEntries[selectedAccountTypeIndex]);
    }

    /**
	 * Retrieves the name of the account which has been entered in the EditText
	 * @return Name of the account which has been entered in the EditText
	 */
	public String getEnteredName(){
		return mNameEditText.getText().toString().trim();
	}

}
