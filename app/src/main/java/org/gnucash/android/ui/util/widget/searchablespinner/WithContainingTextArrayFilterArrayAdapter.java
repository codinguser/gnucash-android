package org.gnucash.android.ui.util.widget.searchablespinner;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class WithContainingTextArrayFilterArrayAdapter<T>
        extends ArrayAdapter {

    /**
     * Array item filter which keeps only items containing a text
     */
    private class ItemContaingTextArrayFilter
            extends Filter {

        /**
         * Build filtered results, which is a structure containing
         *      filtered items (whose text contains textToSearch)
         *      count of filtered items
         *
         * @param textToSearch
         *          text to search to retain item
         *
         * @return
         *      structure containing filtered items and count
         */
        @Override
        protected FilterResults performFiltering(CharSequence textToSearch) {

            final FilterResults filterResults = new FilterResults();

            // Get copy of all items
            final ArrayList<T> allItems = getCopyOfAllItems();

            if (textToSearch == null || textToSearch.length() == 0) {
                // Nothing to search

                filterResults.values = allItems;
                filterResults.count = allItems.size();

            } else {
                // There is something to search

                final String textToSearchLowerCase = textToSearch.toString()
                                                                 .toLowerCase();

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

                filterResults.values = filteredItems;
                filterResults.count = filteredItems.size();
            }

            return filterResults;
        }

        @Override
        protected void publishResults(CharSequence constraint,
                                      FilterResults filteredResults) {

            // Replace items in ArrayAdapter with filtered ones
            clear();
            addAll((List<T>) filteredResults.values);

            if (filteredResults.count > 0) {
                // There are filtered items

                notifyDataSetChanged();

            } else {
                // There is none filtered items

                notifyDataSetInvalidated();
            }
        }

        // TODO TW C 2020-02-25 : Vérifier si ça fait vraiment une copie ou juste une autre liste
        private ArrayList<T> getCopyOfAllItems() {

            final ArrayList<T> allItemsCopy;

            synchronized (_lock) {
                allItemsCopy = new ArrayList<>(_allItems);
            }
            return allItemsCopy;
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
     * @param itemView
     * @param items
     */
    public WithContainingTextArrayFilterArrayAdapter(Activity context,
                                                     int itemView,
                                                     List<T> items) {

        super(context,
              itemView,
              items);

        // TODO TW C 2020-02-25 : Pas sûr que ça fait une copie des éléments, c'est peut-être juste une autre liste qui pointe
        //  vers les mêmes éléments
        // Make a copy of all the items (before filtering them)
        this._allItems = new ArrayList<T>();
        this._allItems.addAll(items);
    }

    @NonNull
    @Override
    public Filter getFilter() {

        return _itemContaingTextArrayFilter;
    }

    @NonNull
    @Override
    public View getView(int position,
                        View convertView,
                        ViewGroup parent) {

        // TODO TW C 2020-01-25 : Optimiser en utilisant un ViewHolder

        View itemView = super.getView(position,
                                      convertView,
                                      parent);

//        // item text
//        TextView text1 = (TextView) itemView.findViewById(android.R.id.text1);

        return itemView;
    }
}
