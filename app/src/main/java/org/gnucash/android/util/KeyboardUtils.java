package org.gnucash.android.util;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

/**
 * Created by JeanGarf on 2020-02-01.
 */
public class KeyboardUtils {

    /**
     * Hide keyboard
     *
     * @param editTextView
     */
    public static void hideKeyboard(final View editTextView) {

        //
        // Hide keyboard
        //

        InputMethodManager keyboard = (InputMethodManager) editTextView.getContext()
                                                                       .getSystemService(Context.INPUT_METHOD_SERVICE);

        keyboard.hideSoftInputFromWindow(editTextView.getWindowToken(),
                                         0);
    }

    /**
     * Hide keyboard after a delay
     *
     * @param editTextView
     * @param delay
     */
    public static void hideKeyboard(final View editTextView,
                                    final long delay) {

//        editTextView.requestFocus();

        // Delay the keyboard hiding
        editTextView.postDelayed(new Runnable() {
                                     @Override
                                     public void run() {

                                         // Hide keyboard
                                         hideKeyboard(editTextView);
                                     }
                                 },
                                 delay);
    }


}
