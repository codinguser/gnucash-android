/*
 * Copyright (c) 2017 Àlex Magaz Graça <alexandre.magaz@gmail.com>
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

package org.gnucash.android.ui.settings.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import org.gnucash.android.R;
import org.gnucash.android.db.adapter.BooksDbAdapter;
import org.gnucash.android.ui.common.Refreshable;

/**
 * Confirmation dialog for deleting a book.
 *
 * @author Àlex Magaz <alexandre.magaz@gmail.com>
 */
public class DeleteBookConfirmationDialog extends DialogFragment {

    public static DeleteBookConfirmationDialog newInstance(String bookUID) {
        DeleteBookConfirmationDialog frag = new DeleteBookConfirmationDialog();
        Bundle args = new Bundle();
        args.putString("bookUID", bookUID);
        frag.setArguments(args);
        return frag;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final String bookUID = getArguments().getString("bookUID");

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setTitle(getString(R.string.title_confirm_delete_book))
                .setIcon(R.drawable.ic_close_black_24dp)
                .setView(R.layout.dialog_double_confirm)
                .setMessage(getString(R.string.msg_all_book_data_will_be_deleted));
        dialogBuilder.setPositiveButton(getString(R.string.btn_delete_book), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                BooksDbAdapter.getInstance().deleteBook(bookUID);
                ((Refreshable) getTargetFragment()).refresh();
            }
        });
        dialogBuilder.setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        return dialogBuilder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        final AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            CheckBox confirmCheckBox = dialog.findViewById(R.id.checkbox_confirm);
            confirmCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(b);
                }
            });
        }
    }
}
