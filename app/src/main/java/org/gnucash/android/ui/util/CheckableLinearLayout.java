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
package org.gnucash.android.ui.util;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.LinearLayout;

/**
 * An implementation of {@link android.widget.LinearLayout} which implements the {@link android.widget.Checkable} interface.
 * This layout keeps track of its checked state or alternatively queries its child views for any {@link View} which is Checkable.
 * If there is a Checkable child view, then that child view determines the check state of the whole layout.
 *
 * <p>This layout is designed for use with ListViews with a choice mode other than {@link android.widget.ListView#CHOICE_MODE_NONE}.
 * Android requires the parent view of the row items in the list to be checkable in order to take advantage of the APIs</p>
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class CheckableLinearLayout extends LinearLayout implements Checkable {
    /**
     * Checkable view which holds the checked state of the linear layout
     */
    private Checkable mCheckable = null;

    /**
     * Fallback check state of the linear layout if there is no {@link Checkable} amongst its child views.
     */
    private boolean mIsChecked = false;

    public CheckableLinearLayout(Context context) {
        super(context);
    }

    public CheckableLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CheckableLinearLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Find any instance of a {@link Checkable} amongst the children of the linear layout and store a reference to it
     */
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        //this prevents us from opening transactions since simply clicking on the item checks the checkable and
        //activates action mode.
//        mCheckable = findCheckableView(this);
    }

    /**
     * Iterates through the child views of <code>parent</code> to an arbitrary depth and returns the first
     * {@link Checkable} view found
     * @param parent ViewGroup in which to search for Checkable children
     * @return First {@link Checkable} child view of parent found
     */
    private Checkable findCheckableView(ViewGroup parent){
        for (int i = 0; i < parent.getChildCount(); i++) {
            View childView = parent.getChildAt(i);

            if (childView instanceof Checkable)
                return (Checkable)childView;

            if (childView instanceof ViewGroup){
                Checkable checkable = findCheckableView((ViewGroup)childView);
                if (checkable != null){
                    return checkable;
                }
            }
        }
        return null;
    }

    @Override
    public void setChecked(boolean b) {
        if (mCheckable != null){
            mCheckable.setChecked(b);
        } else {
            mIsChecked = b;
        }
        refreshDrawableState();
    }

    @Override
    public boolean isChecked() {
        return (mCheckable != null) ? mCheckable.isChecked() : mIsChecked;
    }

    @Override
    public void toggle() {
        if (mCheckable != null){
            mCheckable.toggle();
        } else {
            mIsChecked = !mIsChecked;
        }
        refreshDrawableState();
    }
}
