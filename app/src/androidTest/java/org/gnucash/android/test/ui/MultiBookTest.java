/*
 * Copyright (c) 2016 Ngewi Fet <ngewif@gmail.com>
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
package org.gnucash.android.test.ui;

import android.support.test.espresso.contrib.DrawerActions;
import android.support.test.espresso.intent.Intents;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.gnucash.android.R;
import org.gnucash.android.db.adapter.BooksDbAdapter;
import org.gnucash.android.model.Book;
import org.gnucash.android.test.ui.util.DisableAnimationsRule;
import org.gnucash.android.ui.account.AccountsActivity;
import org.gnucash.android.ui.settings.PreferenceActivity;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.swipeUp;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test support for multiple books in the application
 */
@RunWith(AndroidJUnit4.class)
public class MultiBookTest {

    private static BooksDbAdapter mBooksDbAdapter;

    @ClassRule
    public static DisableAnimationsRule disableAnimationsRule = new DisableAnimationsRule();

    @Rule
    public IntentsTestRule<AccountsActivity> mActivityRule = new IntentsTestRule<>(AccountsActivity.class);

    @BeforeClass
    public static void prepTestCase(){
        mBooksDbAdapter = BooksDbAdapter.getInstance();
    }

    @Test
    public void shouldOpenBookManager(){
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());
        onView(withId(R.id.book_name)).check(matches(isDisplayed())).perform(click());

        onView(withText(R.string.menu_manage_books)).perform(click());

        Intents.intended(hasComponent(PreferenceActivity.class.getName()));
    }

    @Test
    public void creatingNewAccounts_shouldCreatedNewBook(){
        long booksCount = mBooksDbAdapter.getRecordsCount();

        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());
        onView(withId(R.id.drawer_layout)).perform(swipeUp());
        onView(withText(R.string.title_settings)).perform(click());

        Intents.intended(hasComponent(PreferenceActivity.class.getName()));

        onView(withText(R.string.header_account_settings)).perform(click());
        onView(withText(R.string.title_create_default_accounts)).perform(click());
        onView(withId(android.R.id.button1)).perform(click());

        //// TODO: 18.05.2016 wait for import to finish instead
        sleep(2000); //give import time to finish

        assertThat(mBooksDbAdapter.getRecordsCount()).isEqualTo(booksCount+1);

        Book activeBook = mBooksDbAdapter.getRecord(mBooksDbAdapter.getActiveBookUID());
        assertThat(activeBook.getDisplayName()).isEqualTo("Book " + (booksCount+1));
    }

    private static void sleep(long millis){
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
