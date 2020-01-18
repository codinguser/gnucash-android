package org.gnucash.android.ui.util.widget.searchablespinner;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import java.util.ArrayList;
import java.util.List;

public class WithContainsFilterArrayAdapter<T>
        extends ArrayAdapter {

    /**
     * <p>An array filter constrains the content of the array adapter with
     * items containing a text.</p>
     */
    private class ArrayTextFilter
            extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence textToSearch) {

            final FilterResults results = new FilterResults();

//            if (mOriginalValues == null) {
//                synchronized (mLock) {
//                    mOriginalValues = new ArrayList<>(mObjects);
//                }
//            }

            if (textToSearch == null || textToSearch.length() == 0) {
                // Nothing to search

                // Get copy of all values
                final ArrayList<T> values = getCopyOfOriginalValues();

                results.values = values;
                results.count = values.size();

            } else {
                // There is something to search

                final String textToSearchLowerCase = textToSearch.toString()
                                                                 .toLowerCase();

                // Get copy of all values
                final ArrayList<T> values = getCopyOfOriginalValues();

                //
                // Filter values
                //

                final ArrayList<T> filteredValues = new ArrayList<>();

                final int count = values.size();

                for (int i = 0; i < count; i++) {

                    final T value = values.get(i);

                    final String valueTextLowerCase = value.toString()
                                                           .toLowerCase();

                    // First match against the whole, non-splitted value
                    if (valueTextLowerCase.contains(textToSearchLowerCase)) {
                        // It matches

                        filteredValues.add(value);

                    } else {
                        //

//                        final String[] words = valueText.split(" ");
//                        for (String word : words) {
//                            if (word.startsWith(textToSearchLowerCase)) {
//                                newValues.add(value);
//                                break;
//                            }
//                        }
                    }
                }

                results.values = filteredValues;
                results.count = filteredValues.size();
            }

            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint,
                                      FilterResults results) {

            clear();
            addAll((List<T>) results.values);

//            mObjects = (List<T>) results.values;

            if (results.count > 0) {
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }

        private ArrayList<T> getCopyOfOriginalValues() {

            final ArrayList<T> values;
            synchronized (mLock) {
                values = new ArrayList<>(mOriginalValues);
            }
            return values;
        }

    }

    /**
     * Lock used to modify the content of {@link #mOriginalValues}. Any write operation
     * performed on the array should be synchronized on this lock. This lock is also
     * used by the filter (see {@link #getFilter()} to make a synchronized copy of
     * the original array of data.
     */
    private final Object mLock = new Object();

    private List<T> mOriginalValues;
//    private List<T> mObjects;

    private ArrayTextFilter _arrayTextFilter = new ArrayTextFilter();

    /**
     * Constructor
     *
     * @param context
     * @param items_view
     * @param items
     */
    public WithContainsFilterArrayAdapter(Activity context,
                                          int items_view,
                                          List<T> items) {

        super(context,
              items_view,
              items);

        // save the initialList
        this.mOriginalValues = new ArrayList<T>();
        this.mOriginalValues.addAll(items);

        //
//        this.mObjects = items;
    }

    @NonNull
    @Override
    public Filter getFilter() {

//        return super.getFilter();
        return _arrayTextFilter;
    }

//    // Filter method
//    public void filter(String textToSearch) {
//
//        textToSearch = textToSearch.toLowerCase(Locale.getDefault());
//
//        mObjects.clear();
//
//        if (textToSearch.length() == 0) {
//
//            mObjects.addAll(mOriginalValues);
//
//        } else {
//
//            for (T item : mOriginalValues) {
//
//                if (item.toString().toLowerCase(Locale.getDefault())
//                        .contains(textToSearch)) {
//                    //
//
//                    mObjects.add(item);
//                }
//            }
//        }
//        notifyDataSetChanged();
//    }
}
