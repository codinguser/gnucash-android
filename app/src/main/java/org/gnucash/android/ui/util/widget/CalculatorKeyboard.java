/**
 * Copyright 2013 Maarten Pennings extended by SimplicityApks
 *
 * Modified by:
 * Copyright 2015 Àlex Magaz Graça <rivaldi8@gmail.com>
 * Copyright 2015 Ngewi Fet <ngewif@gmail.com>
 *
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p/>
 * If you use this software in a product, an acknowledgment in the product
 * documentation would be appreciated but is not required.
 */

package org.gnucash.android.ui.util.widget;

import android.app.Activity;
import android.content.Context;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener;
import android.provider.Settings;
import android.support.annotation.XmlRes;
import android.text.Editable;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import java.text.DecimalFormatSymbols;


/**
 * When an activity hosts a keyboardView, this class allows several EditText's to register for it.
 *
 * Known issues:
 *  - It's not possible to select text.
 *  - When in landscape, the EditText is covered by the keyboard.
 *  - No i18n.
 *
 * @author Maarten Pennings, extended by SimplicityApks
 * @date 2012 December 23
 *
 * @author Àlex Magaz Graça <rivaldi8@gmail.com>
 * @author Ngewi Fet <ngewif@gmail.com>
 *
 */
public class CalculatorKeyboard {

    public static final int KEY_CODE_DECIMAL_SEPARATOR = 46;
    /** A link to the KeyboardView that is used to render this CalculatorKeyboard. */
    private KeyboardView mKeyboardView;

    private Context mContext;
    private boolean hapticFeedback;

    public static final String LOCALE_DECIMAL_SEPARATOR = Character.toString(DecimalFormatSymbols.getInstance().getDecimalSeparator());

    private OnKeyboardActionListener mOnKeyboardActionListener = new OnKeyboardActionListener() {
        @Override
        public void onKey(int primaryCode, int[] keyCodes) {
            View focusCurrent = ((Activity)mContext).getWindow().getCurrentFocus();
            assert focusCurrent != null;

            /*
            if (focusCurrent == null || focusCurrent.getClass() != EditText.class)
                return;
            */

            CalculatorEditText calculatorEditText = (CalculatorEditText) focusCurrent;
            Editable editable = calculatorEditText.getText();
            int start = calculatorEditText.getSelectionStart();
            int end = calculatorEditText.getSelectionEnd();

            // FIXME: use replace() down
            // delete the selection, if chars are selected:
            if (end > start)
                editable.delete(start, end);

            switch (primaryCode) {
                case KEY_CODE_DECIMAL_SEPARATOR:
                    editable.insert(start, LOCALE_DECIMAL_SEPARATOR);
                    break;
                case 42:
                case 43:
                case 45:
                case 47:
                case 48:
                case 49:
                case 50:
                case 51:
                case 52:
                case 53:
                case 54:
                case 55:
                case 56:
                case 57:
                    //editable.replace(start, end, Character.toString((char) primaryCode));
                    // XXX: could be android:keyOutputText attribute used instead of this?
                    editable.insert(start, Character.toString((char) primaryCode));
                    break;
                case -5:
                    int deleteStart = start > 0 ? start - 1: 0;
                    editable.delete(deleteStart, end);
                    break;
                case 1003: // C[lear]
                    editable.clear();
                    break;
                case 1001:
                    calculatorEditText.evaluate();
                    break;
                case 1002:
                    calculatorEditText.focusSearch(View.FOCUS_DOWN).requestFocus();
                    hideCustomKeyboard();
                    break;
            }
        }

        @Override
        public void onPress(int primaryCode) {
            if (isHapticFeedbackEnabled() && primaryCode != 0)
                mKeyboardView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        }

        @Override public void onRelease(int primaryCode) { }
        @Override public void onText(CharSequence text) { }
        @Override public void swipeLeft() { }
        @Override public void swipeRight() { }
        @Override public void swipeDown() { }
        @Override public void swipeUp() { }
    };

    /**
     * Returns true if the haptic feedback is enabled.
     *
     * @return true if the haptic feedback is enabled in the system settings.
     */
    private boolean isHapticFeedbackEnabled() {
        int value = Settings.System.getInt(mKeyboardView.getContext().getContentResolver(),
                                           Settings.System.HAPTIC_FEEDBACK_ENABLED, 0);
        return value != 0;
    }

    /**
     * Create a custom keyboard, that uses the KeyboardView (with resource id <var>viewid</var>) of the <var>host</var> activity,
     * and load the keyboard layout from xml file <var>layoutid</var> (see {@link Keyboard} for description).
     * Note that the <var>host</var> activity must have a <var>KeyboardView</var> in its layout (typically aligned with the bottom of the activity).
     * Note that the keyboard layout xml file may include key codes for navigation; see the constants in this class for their values.
     *
     * @param context Context within with the calculator is created
     * @param keyboardView KeyboardView in the layout
     * @param keyboardLayoutResId The id of the xml file containing the keyboard layout.
     */
    public CalculatorKeyboard(Context context, KeyboardView keyboardView, @XmlRes int keyboardLayoutResId) {
        mContext = context;
        mKeyboardView = keyboardView;
        Keyboard keyboard = new Keyboard(mContext, keyboardLayoutResId);
        for (Keyboard.Key key : keyboard.getKeys()) {
            if (key.codes[0] == KEY_CODE_DECIMAL_SEPARATOR){
                key.label = LOCALE_DECIMAL_SEPARATOR;
                break;
            }
        }
        mKeyboardView.setKeyboard(keyboard);
        mKeyboardView.setPreviewEnabled(false); // NOTE Do not show the preview balloons
        mKeyboardView.setOnKeyboardActionListener(mOnKeyboardActionListener);
        // Hide the standard keyboard initially
        ((Activity)mContext).getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    /** Returns whether the CalculatorKeyboard is visible. */
    public boolean isCustomKeyboardVisible() {
        return mKeyboardView.getVisibility() == View.VISIBLE;
    }

    /** Make the CalculatorKeyboard visible, and hide the system keyboard for view v. */
    public void showCustomKeyboard(View v) {
        if (v != null)
            ((InputMethodManager) mContext.getSystemService(Activity.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(), 0);

        mKeyboardView.setVisibility(View.VISIBLE);
        mKeyboardView.setEnabled(true);
    }

    /** Make the CalculatorKeyboard invisible. */
    public void hideCustomKeyboard() {
        mKeyboardView.setVisibility(View.GONE);
        mKeyboardView.setEnabled(false);
    }

    public boolean onBackPressed() {
        if (isCustomKeyboardVisible()) {
            hideCustomKeyboard();
            return true;
        } else
            return false;
    }

    /**
     * Returns the context of this keyboard
     * @return Context
     */
    public Context getContext(){
        return mContext;
    }
}
