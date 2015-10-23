package org.gnucash.android.ui.settings;

import android.annotation.TargetApi;
import android.net.Uri;
import android.os.Bundle;
import android.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import org.gnucash.android.R;
import org.gnucash.android.ui.util.Refreshable;

/**
 * A fragment for adding an owncloud account.
 * Activities that contain this fragment must implement the
 * {@link OwncloudAccountDialog.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link OwncloudAccountDialog#newInstance} factory method to
 * create an instance of this fragment.
 */
@TargetApi(11)
public class OwncloudAccountDialog extends DialogFragment {

    /**
     * Dialog positive button. Ok to save and validade the data
     */
    Button mOkButton;

    /**
     * Cancel button
     */
    Button mCancelButton;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment OwncloudAccountDialog.
     */
    // TODO: Rename and change types and number of parameters
    public static OwncloudAccountDialog newInstance() {
        OwncloudAccountDialog fragment = new OwncloudAccountDialog();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public OwncloudAccountDialog() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.dialog_owncloud_account, container, false);

        mCancelButton = (Button) view.findViewById(R.id.btn_cancel);
        mOkButton = (Button) view.findViewById(R.id.btn_save);
        mOkButton.setText(R.string.btn_save);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setListeners();
    }
    /**
     * Binds click listeners for the dialog buttons
     */
    protected void setListeners(){

        mCancelButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        mOkButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                ((Refreshable)getTargetFragment()).refresh();
                dismiss();
            }
        });
    }
}
