/*
 * Copyright (c) 2013 Ngewi Fet <ngewif@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gnucash.android.ui.settings;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Toast;
import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.export.xml.GncXmlExporter;
import org.gnucash.android.ui.widget.WidgetConfigurationActivity;

/**
 * Confirmation dialog for deleting all accounts from the system.
 * This class currently only works with HONEYCOMB and above.
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 */
@TargetApi(11)
public class DeleteAllAccountsConfirmationDialog extends DialogFragment {

    public static DeleteAllAccountsConfirmationDialog newInstance() {
        DeleteAllAccountsConfirmationDialog frag = new DeleteAllAccountsConfirmationDialog();
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setIcon(android.R.drawable.ic_delete)
                .setTitle(R.string.title_confirm_delete).setMessage(R.string.confirm_delete_all_accounts)
                .setPositiveButton(R.string.alert_dialog_ok_delete,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Context context = getDialog().getContext();
                                GncXmlExporter.createBackup();
                                GnuCashApplication.getAccountsDbAdapter().deleteAllRecords();
                                Toast.makeText(context, R.string.toast_all_accounts_deleted, Toast.LENGTH_SHORT).show();
                                WidgetConfigurationActivity.updateAllWidgets(context);
                            }
                        }
                )
                .setNegativeButton(R.string.alert_dialog_cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dismiss();
                            }
                        }
                )
                .create();
    }
}
