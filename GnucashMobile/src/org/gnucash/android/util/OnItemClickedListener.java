package org.gnucash.android.util;

public interface OnItemClickedListener {

	public void accountSelected(long accountRowId, String accountName);
	
	public void createNewTransaction(long accountRowId);
	
	public void editTransaction(long transactionId);	
}
