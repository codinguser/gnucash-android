package org.gnucash.android.ui.settings;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.app.DialogFragment;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.owncloud.android.lib.common.OwnCloudClientFactory;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory;
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.status.GetRemoteStatusOperation;
import com.owncloud.android.lib.resources.users.GetRemoteUserNameOperation;
import com.owncloud.android.lib.resources.files.FileUtils;

import org.gnucash.android.R;

import java.util.prefs.PreferenceChangeEvent;

/**
 * A fragment for adding an owncloud account.
 */
@TargetApi(11)
public class OwncloudDialogFragment extends DialogFragment {

    /**
     * Dialog positive button. Ok to save and validade the data
     */
    Button mOkButton;

    /**
     * Cancel button
     */
    Button mCancelButton;

    /**
     * Owncloud vars
     */
    String mOC_server;
    String mOC_username;
    String mOC_password;
    String mOC_dir;

    EditText mServer;
    EditText mUsername;
    EditText mPassword;
    EditText mDir;

    TextView mServerError;
    TextView mUsernameError;
    TextView mDirError;

    SharedPreferences mPrefs;
    Context mContext;

    private static CheckBoxPreference ocCheckBox;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment OwncloudDialogFragment.
     */
    public static OwncloudDialogFragment newInstance(Preference pref) {
        OwncloudDialogFragment fragment = new OwncloudDialogFragment();
        ocCheckBox = pref == null ? null : (CheckBoxPreference) pref;
        return fragment;
    }

    public OwncloudDialogFragment() {
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

    private void checkdata() {
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
                        mServerError.setText(getString(R.string.owncloud_server_invalid));
                        mServerError.setVisibility(View.VISIBLE);

                    } else if (caller instanceof GetRemoteUserNameOperation &&
                            mServerError.getText().toString().equals(getString(R.string.owncloud_server_ok))) {
                        mUsernameError.setText(getString(R.string.owncloud_user_invalid));
                        mUsernameError.setVisibility(View.VISIBLE);
                    }
                } else {
                    if (caller instanceof GetRemoteStatusOperation) {
                        mServerError.setText(getString(R.string.owncloud_server_ok));
                        mServerError.setVisibility(View.VISIBLE);
                    } else if (caller instanceof GetRemoteUserNameOperation) {
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
            mDirError.setText(getString(R.string.owncloud_dir_ok));
            mDirError.setVisibility(View.VISIBLE);
        } else {
            mDirError.setText(getString(R.string.owncloud_dir_invalid));
            mDirError.setVisibility(View.VISIBLE);
        }
        saveButton();
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
                // If data didn't change
                if(mOkButton.getText().toString().equals(getString(R.string.btn_save)) &&
                        mOC_server.equals(mServer.getText().toString().trim()) &&
                        mOC_username.equals(mUsername.getText().toString().trim()) &&
                        mOC_password.equals(mPassword.getText().toString().trim()) &&
                        mOC_dir.equals(mDir.getText().toString().trim())
                        )
                    save();
                else
                    checkdata();
            }
        });
    }
}
