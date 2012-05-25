package org.gnucash.android.ui;

import org.gnucash.android.R;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.actionbarsherlock.app.SherlockDialogFragment;

public class AddAccountDialogFragment extends SherlockDialogFragment {
	private Button mSaveButton;
	private Button mCancelButton;
	private EditText mNameEditText;
	
	static public AddAccountDialogFragment newInstance(){
		AddAccountDialogFragment f = new AddAccountDialogFragment();

		return f;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.dialog_add_account, container, false);
		getDialog().setTitle(R.string.add_account);	
		setStyle(STYLE_NORMAL, R.style.Sherlock___Theme_Dialog);
		mSaveButton = (Button) v.findViewById(R.id.btn_save);
		mCancelButton = (Button) v.findViewById(R.id.btn_cancel);
		mNameEditText = (EditText) v.findViewById(R.id.edit_text_account_name);
		
		
		mNameEditText.addTextChangedListener(new NameFieldWatcher());
		mSaveButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				((AccountsActivity) getActivity()).addAccount(mNameEditText.getText().toString());
				AddAccountDialogFragment.this.dismiss();
			}
		});
		
		mCancelButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				AddAccountDialogFragment.this.dismiss();			
			}
		});
		return v;
	}
	
	private class NameFieldWatcher implements TextWatcher {

		@Override
		public void afterTextChanged(Editable s) {
			if (s.length() > 0)
				AddAccountDialogFragment.this.mSaveButton.setEnabled(true);
			else
				AddAccountDialogFragment.this.mSaveButton.setEnabled(false);
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
			//nothing to see here, move along
			
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			// nothing to see here, move along
			
		}
		
	}

}
