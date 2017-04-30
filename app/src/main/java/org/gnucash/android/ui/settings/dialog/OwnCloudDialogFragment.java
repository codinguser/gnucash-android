package org.gnucash.android.ui.settings.dialog;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientFactory;
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory;
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.lib.resources.status.GetRemoteStatusOperation;
import com.owncloud.android.lib.resources.users.GetRemoteUserNameOperation;

import org.gnucash.android.R;

/**
 * A fragment for adding an ownCloud account.
 */
public class OwnCloudDialogFragment extends DialogFragment {

    /**
     * Dialog positive button. Ok to save and validate the data
     */
    private Button mOkButton;

    /**
     * Cancel button
     */
    private Button mCancelButton;

    /**
     * ownCloud vars
     */
    private String mOC_server;
    private String mOC_username;
    private String mOC_password;
    private String mOC_dir;

    private EditText mServer;
    private EditText mUsername;
    private EditText mPassword;
    private EditText mDir;

    private TextView mServerError;
    private TextView mUsernameError;
    private TextView mDirError;

    private SharedPreferences mPrefs;
    private Context mContext;

    private static CheckBoxPreference ocCheckBox;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment OwnCloudDialogFragment.
     */
    public static OwnCloudDialogFragment newInstance(Preference pref) {
        OwnCloudDialogFragment fragment = new OwnCloudDialogFragment();
        ocCheckBox = pref == null ? null : (CheckBoxPreference) pref;
        return fragment;
    }

    public OwnCloudDialogFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, 0);

        mContext = getActivity();
        mPrefs = mContext.getSharedPreferences(getString(R.string.owncloud_pref), Context.MODE_PRIVATE);

        mOC_server = mPrefs.getString(getString(R.string.key_owncloud_server), getString(R.string.owncloud_server));
        mOC_username = mPrefs.getString(getString(R.string.key_owncloud_username), null);
        mOC_password = mPrefs.getString(getString(R.string.key_owncloud_password), null);
        mOC_dir = mPrefs.getString(getString(R.string.key_owncloud_dir), getString(R.string.app_name));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.dialog_owncloud_account, container, false);

        mServer = (EditText) view.findViewById(R.id.owncloud_hostname);
        mUsername = (EditText) view.findViewById(R.id.owncloud_username);
        mPassword = (EditText) view.findViewById(R.id.owncloud_password);
        mDir = (EditText) view.findViewById(R.id.owncloud_dir);

        mServer.setText(mOC_server);
        mDir.setText(mOC_dir);
        mPassword.setText(mOC_password); // TODO: Remove - debugging only
        mUsername.setText(mOC_username);

        mServerError = (TextView) view.findViewById(R.id.owncloud_hostname_invalid);
        mUsernameError = (TextView) view.findViewById(R.id.owncloud_username_invalid);
        mDirError = (TextView) view.findViewById(R.id.owncloud_dir_invalid);
        mServerError.setVisibility(View.GONE);
        mUsernameError.setVisibility(View.GONE);
        mDirError.setVisibility(View.GONE);

        mCancelButton = (Button) view.findViewById(R.id.btn_cancel);
        mOkButton = (Button) view.findViewById(R.id.btn_save);
        mOkButton.setText(R.string.btn_test);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setListeners();
    }

    private void saveButton() {
        if (mDirError.getText().toString().equals(getString(R.string.owncloud_dir_ok)) &&
                mUsernameError.getText().toString().equals(getString(R.string.owncloud_user_ok)) &&
                mServerError.getText().toString().equals(getString(R.string.owncloud_server_ok)))
            mOkButton.setText(R.string.btn_save);
        else
            mOkButton.setText(R.string.btn_test);
    }

    private void save() {
        SharedPreferences.Editor edit = mPrefs.edit();
        edit.clear();
        edit.putString(getString(R.string.key_owncloud_server), mOC_server);
        edit.putString(getString(R.string.key_owncloud_username), mOC_username);
        edit.putString(getString(R.string.key_owncloud_password), mOC_password);
        edit.putString(getString(R.string.key_owncloud_dir), mOC_dir);
        edit.putBoolean(getString(R.string.owncloud_sync), true);
        edit.apply();

        if (ocCheckBox != null) ocCheckBox.setChecked(true);

        dismiss();
    }

    private void checkData() {
        mServerError.setVisibility(View.GONE);
        mUsernameError.setVisibility(View.GONE);
        mDirError.setVisibility(View.GONE);

        mOC_server = mServer.getText().toString().trim();
        mOC_username = mUsername.getText().toString().trim();
        mOC_password = mPassword.getText().toString().trim();
        mOC_dir = mDir.getText().toString().trim();

        Uri serverUri = Uri.parse(mOC_server);
        OwnCloudClient mClient = OwnCloudClientFactory.createOwnCloudClient(serverUri, mContext, true);
        mClient.setCredentials(
                OwnCloudCredentialsFactory.newBasicCredentials(mOC_username, mOC_password)
        );

        final Handler mHandler = new Handler();

        OnRemoteOperationListener listener = new OnRemoteOperationListener() {
            @Override
            public void onRemoteOperationFinish(RemoteOperation caller, RemoteOperationResult result) {
                if (!result.isSuccess()) {
                    Log.e("OC", result.getLogMessage(), result.getException());

                    if (caller instanceof GetRemoteStatusOperation) {
                        mServerError.setTextColor(ContextCompat.getColor(getContext(), R.color.debit_red));
                        mServerError.setText(getString(R.string.owncloud_server_invalid));
                        mServerError.setVisibility(View.VISIBLE);

                    } else if (caller instanceof GetRemoteUserNameOperation &&
                            mServerError.getText().toString().equals(getString(R.string.owncloud_server_ok))) {
                        mUsernameError.setTextColor(ContextCompat.getColor(getContext(), R.color.debit_red));
                        mUsernameError.setText(getString(R.string.owncloud_user_invalid));
                        mUsernameError.setVisibility(View.VISIBLE);
                    }
                } else {
                    if (caller instanceof GetRemoteStatusOperation) {
                        mServerError.setTextColor(ContextCompat.getColor(getContext(), R.color.theme_primary));
                        mServerError.setText(getString(R.string.owncloud_server_ok));
                        mServerError.setVisibility(View.VISIBLE);
                    } else if (caller instanceof GetRemoteUserNameOperation) {
                        mUsernameError.setTextColor(ContextCompat.getColor(getContext(), R.color.theme_primary));
                        mUsernameError.setText(getString(R.string.owncloud_user_ok));
                        mUsernameError.setVisibility(View.VISIBLE);
                    }
                }
                saveButton();
            }
        };

        GetRemoteStatusOperation g = new GetRemoteStatusOperation(mContext);
        g.execute(mClient, listener, mHandler);

        GetRemoteUserNameOperation gu = new GetRemoteUserNameOperation();
        gu.execute(mClient, listener, mHandler);

        if (FileUtils.isValidPath(mOC_dir, false)) {
            mDirError.setTextColor(ContextCompat.getColor(getContext(), R.color.theme_primary));
            mDirError.setText(getString(R.string.owncloud_dir_ok));
            mDirError.setVisibility(View.VISIBLE);
        } else {
            mDirError.setTextColor(ContextCompat.getColor(getContext(), R.color.debit_red));
            mDirError.setText(getString(R.string.owncloud_dir_invalid));
            mDirError.setTextColor(ContextCompat.getColor(getContext(), R.color.debit_red));
            mDirError.setVisibility(View.VISIBLE);
        }
        saveButton();
    }

    /**
     * Binds click listeners for the dialog buttons
     */
    private void setListeners(){

        mCancelButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        mOkButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // If data didn't change
                if(mOkButton.getText().toString().equals(getString(R.string.btn_save)) &&
                        mOC_server.equals(mServer.getText().toString().trim()) &&
                        mOC_username.equals(mUsername.getText().toString().trim()) &&
                        mOC_password.equals(mPassword.getText().toString().trim()) &&
                        mOC_dir.equals(mDir.getText().toString().trim())
                        )
                    save();
                else
                    checkData();
            }
        });
    }
}
