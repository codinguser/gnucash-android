package org.gnucash.android.ui.transactions;

import org.gnucash.android.R;
import org.gnucash.android.util.OnTransactionClickedListener;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;

public class TransactionsActivity extends SherlockFragmentActivity implements OnTransactionClickedListener{

	protected static final String TAG = "AccountsActivity";
	
	public static final String FRAGMENT_TRANSACTIONS_LIST 	= "transactions_list";
	public static final String FRAGMENT_NEW_TRANSACTION 	= "new_transaction";	
	
	private long mAccountId 	= 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_transactions);

		final Intent intent = getIntent();
		mAccountId = intent.getLongExtra(
				TransactionsListFragment.SELECTED_ACCOUNT_ID, -1);	
		
		showTransactionsList();
		
		if (intent.getAction().equals(Intent.ACTION_INSERT_OR_EDIT)) {			
			long transactionId = intent.getLongExtra(
					NewTransactionFragment.SELECTED_TRANSACTION_ID, -1);
			if (transactionId <= 0) {
				createNewTransaction(mAccountId);
			} else {
				editTransaction(transactionId);
			}
		}
	}	

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
	        FragmentManager fm = getSupportFragmentManager();
	        if (fm.getBackStackEntryCount() > 0) {
	            fm.popBackStack();
	        }
	        return true;

		default:
			return false;
		}
	}
	
	/**
	 * Opens a fragment to create a new transaction. 
	 * Is called from the XML views
	 * @param v View which triggered this method
	 */
	public void onNewTransactionClick(View v){
		createNewTransaction(mAccountId);
	}
		
	protected void showTransactionsList(){
		FragmentManager fragmentManager = getSupportFragmentManager();

		TransactionsListFragment transactionsListFragment = (TransactionsListFragment) fragmentManager
				.findFragmentByTag(FRAGMENT_TRANSACTIONS_LIST);

		if (transactionsListFragment == null) {
			FragmentTransaction fragmentTransaction = fragmentManager
					.beginTransaction();
			transactionsListFragment = new TransactionsListFragment();
			Bundle args = new Bundle();
			args.putLong(TransactionsListFragment.SELECTED_ACCOUNT_ID,
					mAccountId);
			transactionsListFragment.setArguments(args);
			Log.i(TAG, "Opening transactions for account id " +  mAccountId);

			fragmentTransaction.add(R.id.fragment_container,
					transactionsListFragment, FRAGMENT_TRANSACTIONS_LIST);
						
			fragmentTransaction.commit();
		}
	}
	
	@Override
	public void createNewTransaction(long accountRowId) {
		FragmentManager fragmentManager = getSupportFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager
				.beginTransaction();
		NewTransactionFragment newTransactionFragment = new NewTransactionFragment();
		Bundle args = new Bundle();
		args.putLong(TransactionsListFragment.SELECTED_ACCOUNT_ID, accountRowId);		
		newTransactionFragment.setArguments(args);
		
		fragmentTransaction.replace(R.id.fragment_container,
				newTransactionFragment, TransactionsActivity.FRAGMENT_NEW_TRANSACTION);

		fragmentTransaction.addToBackStack(null);
		fragmentTransaction.commit();
	}

	@Override
	public void editTransaction(long transactionId){
		FragmentManager fragmentManager = getSupportFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager
				.beginTransaction();
		NewTransactionFragment newTransactionFragment = new NewTransactionFragment();
		Bundle args = new Bundle();
		args.putLong(NewTransactionFragment.SELECTED_TRANSACTION_ID, transactionId);		
		newTransactionFragment.setArguments(args);
		
		fragmentTransaction.replace(R.id.fragment_container,
				newTransactionFragment, TransactionsActivity.FRAGMENT_NEW_TRANSACTION);

		fragmentTransaction.addToBackStack(null);	
		fragmentTransaction.commit();
	}
}
