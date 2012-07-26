package org.gnucash.android.util;

public interface OnTransactionClickedListener {

	public void createNewTransaction(long accountRowId);
	
	public void editTransaction(long transactionId);	
}
