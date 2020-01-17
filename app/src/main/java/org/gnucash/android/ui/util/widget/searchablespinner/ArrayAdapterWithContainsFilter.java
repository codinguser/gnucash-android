package org.gnucash.android.ui.util.widget.searchablespinner;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ArrayAdapterWithContainsFilter
        extends ArrayAdapter {

    private List<String>      _fileteredList = null;
    private ArrayList<String> _initialList;

    public ArrayAdapterWithContainsFilter(Activity context,
                                          int items_view,
                                          List<String> items) {

        super(context,
              items_view,
              items);

        // save the initialList
        this._initialList = new ArrayList<String>();
        this._initialList.addAll(items);

        //
        this._fileteredList = items;
    }

//    @NonNull
//    @Override
//    public Filter getFilter() {
//
//        return super.getFilter();
//    }

    // Filter method
    public void filter(String textToSearch) {

        textToSearch = textToSearch.toLowerCase(Locale.getDefault());

        _fileteredList.clear();

        if (textToSearch.length() == 0) {

            _fileteredList.addAll(_initialList);

        } else {

            for (String item : _initialList) {

                if (item.toLowerCase(Locale.getDefault())
                        .contains(textToSearch)) {
                    //

                    _fileteredList.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }
}
