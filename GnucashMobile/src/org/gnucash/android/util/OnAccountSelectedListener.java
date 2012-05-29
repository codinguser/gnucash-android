package org.gnucash.android.util;

public interface OnAccountSelectedListener {

	public void accountSelected(long accountRowId);
	
	public void createNewTransaction(long accountRowId);
}
