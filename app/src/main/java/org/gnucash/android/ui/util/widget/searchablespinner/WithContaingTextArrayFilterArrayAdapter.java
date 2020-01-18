package org.gnucash.android.ui.util.widget.searchablespinner;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import java.util.ArrayList;
import java.util.List;

public class WithContaingTextArrayFilterArrayAdapter<T>
        extends ArrayAdapter {

    /**
     * Array item filter which keeps only items containing a text
     */
    private class ItemContaingTextArrayFilter
            extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence textToSearch) {

            final FilterResults results = new FilterResults();

            if (textToSearch == null || textToSearch.length() == 0) {
                // Nothing to search

                // Get copy of all items
                final ArrayList<T> allItems = getCopyOfAllItems();

                results.values = allItems;
                results.count = allItems.size();

            } else {
                // There is something to search

                final String textToSearchLowerCase = textToSearch.toString()
                                                                 .toLowerCase();

                // Get copy of all items
                final ArrayList<T> allItems = getCopyOfAllItems();

                //
                // Filter items
                //

                final ArrayList<T> filteredItems = new ArrayList<>();

                final int count = allItems.size();

                for (int i = 0; i < count; i++) {

                    final T item = allItems.get(i);

                    final String itemTextLowerCase = item.toString()
                                                           .toLowerCase();

                    // First match against the whole, non-splitted value
                    if (itemTextLowerCase.contains(textToSearchLowerCase)) {
                        // It matches

                        filteredItems.add(item);

                    } else {
                        // It doesen't match

                        // NTD
                    }
                }

                results.values = filteredItems;
                results.count = filteredItems.size();
            }

            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint,
                                      FilterResults results) {

            // Replace items in ArrayAdapter with filtered ones
            clear();
            addAll((List<T>) results.values);

            if (results.count > 0) {
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }

        private ArrayList<T> getCopyOfAllItems() {

            final ArrayList<T> values;
            synchronized (_lock) {
                values = new ArrayList<>(_allItems);
            }
            return values;
        }

    }

    /**
     * Array item filter which keeps only items containing a text
     */
    private final ItemContaingTextArrayFilter _itemContaingTextArrayFilter = new ItemContaingTextArrayFilter();

    /**
     * Lock used to modify the content of {@link #_allItems}. Any write operation
     * performed on the array should be synchronized on this lock. This lock is also
     * used by the filter (see {@link #getFilter()} to make a synchronized copy of
     * the original array of data.
     */
    private final Object _lock = new Object();

    /**
     * Copy of all items before filtering
     */
    private List<T> _allItems;

    /**
     * Constructor
     *
     * @param context
     * @param items_view
     * @param items
     */
    public WithContaingTextArrayFilterArrayAdapter(Activity context,
                                                   int items_view,
                                                   List<T> items) {

        super(context,
              items_view,
              items);

        // save all the items (before filtering)
        this._allItems = new ArrayList<T>();
        this._allItems.addAll(items);
    }

    @NonNull
    @Override
    public Filter getFilter() {

        return _itemContaingTextArrayFilter;
    }

}
