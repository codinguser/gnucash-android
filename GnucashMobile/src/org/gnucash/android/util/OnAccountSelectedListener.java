package org.gnucash.android.util;

public interface OnAccountSelectedListener {

	public void accountSelected(long accountRowId, String accountName);
	
	public void createNewTransaction(long accountRowId);
}
