package org.gnucash.android.ui.util.widget.searchablespinner;

import android.widget.Adapter;
import android.widget.Filter;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic filter that retain items whose item.toString() contains a text to search
 *
 * @author Thierry
 */
public class ItemToStringContainsTextFilter<T>
        extends Filter {

    /**
     * Adapter whose this filter belongs to
     */
    private Adapter mParentAdapter;

    /**
     * Copy of all items before filtering
     */
    private List<T> mAllItems;

    //     * Lock used to modify the content of {@link #mAllItems}. Any write operation
    //     * performed on the array should be synchronized on this lock. This lock is also
    //     * used by the filter (see {@link #getFilter()} to make a synchronized copy of
    //     * the original array of data.
    private final Object _lock = new Object();



    /**
     * Constructor
     */
    public ItemToStringContainsTextFilter(final Adapter parentAdapter,
                                          final List<T> allItems) {

        mParentAdapter = parentAdapter;

        // TODO TW M 2020-02-25 : Mettre un synchronize(_lock) ?
        mAllItems = allItems;
    }


    /**
     * Build filtered results, which is a structure containing
     * filtered items (whose text contains textToSearch)
     * count of filtered items
     *
     * @param textToSearch
     *         text to search (in item.toString()), to retain item
     *
     * @return structure containing filtered items and count
     */
    @Override
    protected FilterResults performFiltering(CharSequence textToSearch) {

        final FilterResults filterResults = new FilterResults();

        // Get copy of all items
        final List<T> allItems = getCopyOfAllItems();

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

            final List<T> filteredItems = new ArrayList<T>();

            final int count = allItems.size();

            for (int i = 0; i < count; i++) {

                final T item = allItems.get(i);

                // get the item.toString()
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

        // Replace items with filtered ones
        mAllItems.clear();
        mAllItems.addAll((List<T>) filteredResults.values);
    }

    // TODO TW C 2020-02-25 : Vérifier si ça fait vraiment une copie ou juste une autre liste : getNewListOfAllItems() ?
    private List<T> getCopyOfAllItems() {

        final List<T> allItemsCopy;

        synchronized (_lock) {

            allItemsCopy = new ArrayList<>();

            for (int i = 0; i < mParentAdapter.getCount(); i++) {

                allItemsCopy.add((T) mParentAdapter.getItem(i));
            }
        }

        return allItemsCopy;
    }

}
