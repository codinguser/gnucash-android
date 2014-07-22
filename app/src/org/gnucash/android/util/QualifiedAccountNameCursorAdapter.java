/*
 * Copyright (c) 2013 - 2014 Ngewi Fet <ngewif@gmail.com>
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
import org.gnucash.android.db.DatabaseSchema;

/**
 * Cursor adapter which looks up the fully qualified account name and returns that instead of just the simple name.
 * <p>The fully qualified account name includes the parent hierarchy</p>
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class QualifiedAccountNameCursorAdapter extends SimpleCursorAdapter {

    public QualifiedAccountNameCursorAdapter(Context context, int layout, Cursor c) {
        super(context, layout, c,
                new String[] {DatabaseSchema.AccountEntry.COLUMN_FULL_NAME},
                new int[] {android.R.id.text1}, 0);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        super.bindView(view, context, cursor);
        TextView textView = (TextView) view.findViewById(android.R.id.text1);
        textView.setEllipsize(TextUtils.TruncateAt.MIDDLE);
    }
}
