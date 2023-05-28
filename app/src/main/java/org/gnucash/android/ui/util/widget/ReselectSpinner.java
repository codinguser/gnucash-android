package org.gnucash.android.ui.util.widget;

import android.content.Context;
import androidx.appcompat.widget.AppCompatSpinner;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;

import org.gnucash.android.ui.export.ExportFormFragment;

/**
 * Spinner which fires OnItemSelectedListener even when an item is reselected.
 * Normal Spinners only fire item selected notifications when the selected item changes.
 * <p>This is used in {@code ReportsActivity} for the time range and in the {@link ExportFormFragment}</p>
 * <p>It could happen that the selected item is fired twice especially if the item is the first in the list.
 * The Android system does this internally. In order to capture the first one, check whether the view parameter
 * of {@link android.widget.AdapterView.OnItemSelectedListener#onItemSelected(AdapterView, View, int, long)} is null.
 * That would represent the first call during initialization of the views. This call can be ignored.
 * See {@link ExportFormFragment#bindViewListeners()} for an example
 * </p>
 */
public class ReselectSpinner extends AppCompatSpinner {
    public ReselectSpinner(Context context) {
        super(context);
    }

    public ReselectSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ReselectSpinner(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setSelection(int position) {
        boolean sameSelected = getSelectedItemPosition() == position;
        super.setSelection(position);
        if (sameSelected){
            OnItemSelectedListener listener = getOnItemSelectedListener();
            if (listener != null)
                listener.onItemSelected(this, getSelectedView(), position, getSelectedItemId());
        }
    }
}
