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

package org.gnucash.android.ui.account;

import java.util.Arrays;
import java.util.Currency;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import org.gnucash.android.R;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.Money;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.DatabaseHelper;
import org.gnucash.android.ui.UxArgument;
import org.gnucash.android.ui.colorpicker.ColorPickerDialog;
import org.gnucash.android.ui.colorpicker.ColorPickerSwatch;
import org.gnucash.android.ui.colorpicker.ColorSquare;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import org.gnucash.android.util.QualifiedAccountNameCursorAdapter;

/**
 * Fragment used for creating and editing accounts
 * @author Ngewi Fet <ngewif@gmail.com>
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
	 * List of all currency codes (ISO 4217) supported by the app
	 */
	private List<String> mCurrencyCodes;
	
	/**
	 * Record ID of the account which was selected
	 * This is used if we are editing an account instead of creating one
	 */
	private long mSelectedAccountId = 0;
	
	/**
	 * Reference to account object which will be created at end of dialog
	 */
	private Account mAccount = null;

    /**
     * Cursor which will hold set of eligible parent accounts
     */
	private Cursor mParentAccountCursor;

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
     * @see org.gnucash.android.model.Account.AccountType
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
		return f;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
        if (mAccountsDbAdapter == null){
            mAccountsDbAdapter = new AccountsDbAdapter(getSherlockActivity());
        }

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mUseDoubleEntry = sharedPrefs.getBoolean(getString(R.string.key_use_double_entry), false);
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
		mNameEditText.requestFocus();

        mAccountTypeSpinner = (Spinner) view.findViewById(R.id.input_account_type_spinner);
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

        mSelectedAccountId = getArguments().getLong(UxArgument.SELECTED_ACCOUNT_ID);
        if (mSelectedAccountId > 0) {
            mAccount = mAccountsDbAdapter.getAccount(mSelectedAccountId);
            getSherlockActivity().getSupportActionBar().setTitle(R.string.title_edit_account);
        }

        //need to load the cursor adapters for the spinners before initializing the views
        loadParentAccountList();
        loadAccountTypesList();
        loadDefaultTransferAccoutList();
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

        String currencyCode = account.getCurrency().getCurrencyCode();
        setSelectedCurrency(currencyCode);

        mNameEditText.setText(account.getName());
        long parentAccountId = mAccountsDbAdapter.getAccountID(account.getParentUID());
        setParentAccountSelection(parentAccountId);

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
        long parentAccountId = getArguments().getLong(UxArgument.PARENT_ACCOUNT_ID);
        setParentAccountSelection(parentAccountId);

        /* This snippet causes the child account to default to same color as parent. Not sure if we want that

        if (parentAccountId > 0) {
            //child accounts by default have same type as the parent
            setAccountTypeSelection(mAccountsDbAdapter.getAccountType(parentAccountId));
            String colorHex = mAccountsDbAdapter.getAccountColorCode(parentAccountId);
            initializeColorSquarePreview(colorHex);
            mSelectedColor = colorHex;
        }
        */
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
    private void setAccountTypeSelection(Account.AccountType accountType){
        String[] accountTypeEntries = getResources().getStringArray(R.array.account_type_entries);
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
        mCurrencyCodes = Arrays.asList(getResources().getStringArray(R.array.currency_codes));
        if (mCurrencyCodes.contains(currencyCode)){
            mCurrencySpinner.setSelection(mCurrencyCodes.indexOf(currencyCode));
        }
    }

    /**
     * Selects the account with ID <code>parentAccountId</code> in the parent accounts spinner
     * @param parentAccountId Record ID of parent account to be selected
     */
    private void setParentAccountSelection(long parentAccountId){
        if (parentAccountId > 0){
            mParentCheckBox.setChecked(true);
            mParentAccountSpinner.setEnabled(true);
        } else
            return;

        for (int pos = 0; pos < mParentAccountCursorAdapter.getCount(); pos++) {
            if (mParentAccountCursorAdapter.getItemId(pos) == parentAccountId){
                mParentAccountSpinner.setSelection(pos);
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
    private void loadDefaultTransferAccoutList(){
        String condition = DatabaseHelper.KEY_ROW_ID + " != " + mSelectedAccountId
                + " AND " + DatabaseHelper.KEY_PLACEHOLDER + "=0"
                + " AND " + DatabaseHelper.KEY_UID + " != '" + mAccountsDbAdapter.getGnuCashRootAccountUID() + "'";
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
        mParentAccountCursorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mDefaulTransferAccountSpinner.setAdapter(mParentAccountCursorAdapter);
    }

    /**
     * Loads the list of possible accounts which can be set as a parent account and initializes the spinner
     */
	private void loadParentAccountList(){
        String condition = null;
        if (mAccount != null){  //if editing an account
            // limit cyclic account hierarchies. Still technically possible since we don't forbid descendant accounts
            condition = "(" + DatabaseHelper.KEY_PARENT_ACCOUNT_UID + " IS NULL "
                    + " OR " + DatabaseHelper.KEY_PARENT_ACCOUNT_UID + " != '" + mAccount.getUID() + "')"
                    + " AND " + DatabaseHelper.KEY_ROW_ID + "!=" + mSelectedAccountId;
            //TODO: Limit all descendants of the account to eliminate the possibility of cyclic hierarchy
        }

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
		//do not close the database adapter. We got it from the activity, 
		//the activity will take care of it.
	}
	
	private void saveAccount() {
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
		else
			mAccount.setName(getEnteredName());
			
		String curCode = mCurrencyCodes.get(mCurrencySpinner
				.getSelectedItemPosition());
		mAccount.setCurrency(Currency.getInstance(curCode));

        int selectedAccountType = mAccountTypeSpinner.getSelectedItemPosition();
        String[] accountTypeEntries = getResources().getStringArray(R.array.account_type_entries);
        mAccount.setAccountType(Account.AccountType.valueOf(accountTypeEntries[selectedAccountType]));

        mAccount.setPlaceHolderFlag(mPlaceholderCheckBox.isChecked());
        mAccount.setColorCode(mSelectedColor);

		if (mParentCheckBox.isChecked()){
			long id = mParentAccountSpinner.getSelectedItemId();
			mAccount.setParentUID(mAccountsDbAdapter.getAccountUID(id));
		} else {
            //need to do this explicitly in case user removes parent account
			mAccount.setParentUID(null);
		}

        if (mDefaultTransferAccountCheckBox.isChecked()){
            long id = mDefaulTransferAccountSpinner.getSelectedItemId();
            mAccount.setDefaultTransferAccountUID(mAccountsDbAdapter.getAccountUID(id));
        } else {
            //explicitly set in case of removal of default account
            mAccount.setDefaultTransferAccountUID(null);
        }
		
		if (mAccountsDbAdapter == null)
			mAccountsDbAdapter = new AccountsDbAdapter(getActivity());
		mAccountsDbAdapter.addAccount(mAccount);

		finishFragment();
	}
	
	/**
	 * Retrieves the name of the account which has been entered in the EditText
	 * @return
	 */
	public String getEnteredName(){
		return mNameEditText.getText().toString().trim();
	}

}
