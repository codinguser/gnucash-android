package org.gnucash.android.ui;

import org.gnucash.android.R;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.EditText;

import com.actionbarsherlock.app.SherlockDialogFragment;

public class NewAccountDialogFragment extends SherlockDialogFragment {
	private Button mSaveButton;
	private Button mCancelButton;
	private EditText mNameEditText;
	private View.OnClickListener mListener;
	
	public NewAccountDialogFragment(View.OnClickListener listener) {
		mListener = listener;
	}
	
	static public NewAccountDialogFragment newInstance(View.OnClickListener listener){
		NewAccountDialogFragment f = new NewAccountDialogFragment(listener);
		
		return f;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.dialog_new_account, container, false);
		getDialog().setTitle(R.string.add_account);	
		setStyle(STYLE_NORMAL, R.style.Sherlock___Theme_Dialog);
		mSaveButton = (Button) v.findViewById(R.id.btn_save);
		mCancelButton = (Button) v.findViewById(R.id.btn_cancel);
		mNameEditText = (EditText) v.findViewById(R.id.edit_text_account_name);
		mNameEditText.requestFocus();
        getDialog().getWindow().setSoftInputMode(
                LayoutParams.SOFT_INPUT_STATE_VISIBLE);
		
		mNameEditText.addTextChangedListener(new NameFieldWatcher());
		
		mSaveButton.setOnClickListener(mListener);
		
		mCancelButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				NewAccountDialogFragment.this.dismiss();			
			}
		});
		return v;
	}
	
	public String getEnteredName(){
		return mNameEditText.getText().toString();
	}
	
	private class NameFieldWatcher implements TextWatcher {

		@Override
		public void afterTextChanged(Editable s) {
			if (s.length() > 0)
				NewAccountDialogFragment.this.mSaveButton.setEnabled(true);
			else
				NewAccountDialogFragment.this.mSaveButton.setEnabled(false);
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
