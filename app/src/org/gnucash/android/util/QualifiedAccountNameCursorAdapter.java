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

package org.gnucash.android.util;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.DatabaseAdapter;
import org.gnucash.android.db.DatabaseHelper;

/**
 * @author Ngewi
 */
public class QualifiedAccountNameCursorAdapter extends SimpleCursorAdapter {
    private AccountsDbAdapter mAccountDbAdapter;

    public QualifiedAccountNameCursorAdapter(Context context, int layout, Cursor c) {
        super(context, layout, c,
                new String[] {DatabaseHelper.KEY_NAME},
                new int[] {android.R.id.text1}, 0);
        mAccountDbAdapter = new AccountsDbAdapter(context);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        super.bindView(view, context, cursor);
        TextView textView = (TextView) view.findViewById(android.R.id.text1);
        textView.setText(mAccountDbAdapter.getFullyQualifiedAccountName(cursor.getLong(DatabaseAdapter.COLUMN_ROW_ID)));
        textView.setEllipsize(TextUtils.TruncateAt.MIDDLE);
    }
}
