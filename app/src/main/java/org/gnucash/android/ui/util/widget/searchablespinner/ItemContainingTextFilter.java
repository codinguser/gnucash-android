package org.gnucash.android.ui.util.widget.searchablespinner;

import android.widget.Filter;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic filter that filters (it modifies it) the given list
 * to retain items whose item contains a text to search
 *
 * The isFoundInItem() method can be overridden to change the search criteria
 * The default search criteria consist to find the lower case text to search in
 * the lower case of the item.toString()
 *
 * @author Thierry
 */
public class ItemContainingTextFilter<T_ITEM>
        extends Filter {

    /**
     * Copy of original all items list
     */
    private List<T_ITEM> mOriginalNonFilteredItemsList;

    /**
     * Pointer on Adapter's item list (which will be filtered)
     */
    private List<T_ITEM> mAdaptersItemsList;

//    //     * Lock used to modify the content of {@link #mOriginalNonFilteredItemsList}. Any write operation
//    //     * performed on the array should be synchronized on this lock. This lock is also
//    //     * used by the filter (see {@link #getFilter()} to make a synchronized copy of
//    //     * the original array of data.
//    private final Object _lock = new Object();



    /**
     * Constructor
     */
    public ItemContainingTextFilter(final List<T_ITEM> adaptersItemsList) {

//        mParentAdapter = parentAdapter;

        // Store a pointer to adapter's item list
        setAdaptersItemsList(adaptersItemsList);

        // Create a second list which won't be filtered to store the original non filtered items
        setOriginalNonFilteredItemsList(new ArrayList<>(adaptersItemsList));
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

        // Create a new list to store filtered items (the list points to the same items as the original's one, but not all the
        // items
        final List<T_ITEM> filteredItems = new ArrayList<T_ITEM>();

        if (textToSearch == null || textToSearch.length() == 0) {
            // Nothing to search

            // Create a new List pointing on the same items as the original one
            // in order not to alter the original non filtered items list
            filteredItems.addAll(getOriginalNonFilteredItemsList());

        } else {
            // There is something to search

            //
            // Filter original items list
            //

            final int count = getOriginalNonFilteredItemsList().size();

            for (int i = 0; i < count; i++) {

                // Get item from original non filtered list
                final T_ITEM item = getOriginalNonFilteredItemsList().get(i);

                final boolean isFoundInItem = isFoundInItem(textToSearch,
                                                            item);

                if (isFoundInItem) {
                    // It matches

                    // Add it to filtered list
                    filteredItems.add(item);

                } else {
                    // It doesen't match

                    // NTD
                }
            } // for

        }

        filterResults.values = filteredItems;
        filterResults.count = filteredItems.size();

        return filterResults;
    }

    @Override
    protected void publishResults(CharSequence constraint,
                                  FilterResults filteredResults) {

        // Replace Adapter's items list with the filtered list
        getAdaptersItemsList().clear();
        getAdaptersItemsList().addAll((List<T_ITEM>) filteredResults.values);
    }

    //
    // Methods to be overridden
    //

    /**
     * Return true if textToSearch has been found in item
     * <p>
     * In this default implementation, the text is found if the lower case
     * textToSearch is found in the lower case of the item.toString() string
     *
     * @param textToSearch
     * @param item
     *
     * @return
     *      Return true if textToSearch has been found in item
     */
    protected boolean isFoundInItem(final CharSequence textToSearch,
                                    final T_ITEM item) {

        // get the item.toString()
        final String itemTextLowerCase = item.toString()
                                             .toLowerCase();

        final String textToSearchLowerCase = textToSearch.toString()
                                                         .toLowerCase();

        // First match against the whole, non-splitted value
        return itemTextLowerCase.contains(textToSearchLowerCase);
    }

    //
    // Local methods
    //

//    private List<T_ITEM> getCopyOfAdaptersAllItems() {
//
//        final List<T_ITEM> allItemsCopy;
//
//        synchronized (_lock) {
//
//            allItemsCopy = new ArrayList<>();
//
//            for (int i = 0; i < mParentAdapter.getCount(); i++) {
//
//                allItemsCopy.add((T_ITEM) mParentAdapter.getItem(i));
//            }
//        }
//
//        return allItemsCopy;
//    }

    //
    // Getters/Setters
    //


    protected List<T_ITEM> getOriginalNonFilteredItemsList() {

        return mOriginalNonFilteredItemsList;
    }

    protected void setOriginalNonFilteredItemsList(final List<T_ITEM> originalNonFilteredItemsList) {

        mOriginalNonFilteredItemsList = originalNonFilteredItemsList;
    }

    protected List<T_ITEM> getAdaptersItemsList() {

        return mAdaptersItemsList;
    }

    protected void setAdaptersItemsList(final List<T_ITEM> adaptersItemsList) {

        mAdaptersItemsList = adaptersItemsList;
    }
}
