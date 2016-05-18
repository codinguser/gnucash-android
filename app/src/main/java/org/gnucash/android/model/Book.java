/*
 * Copyright (c) 2015 Ngewi Fet <ngewif@gmail.com>
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

package org.gnucash.android.model;

import android.net.Uri;

import java.sql.Timestamp;

/**
 * Represents a GnuCash book which is made up of accounts and transactions
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class Book extends BaseModel {

    private Uri mSourceUri;
    private String mDisplayName;
    private String mRootAccountUID;
    private String mRootTemplateUID;
    private boolean mActive;

    private Timestamp mLastSync;

    /**
     * Default constructor
     */
    public Book(){
        init();
    }

    /**
     * Create a new book instance
     * @param rootAccountUID GUID of root account
     */
    public Book(String rootAccountUID){
        this.mRootAccountUID = rootAccountUID;
        init();
    }

    /**
     * Initialize default values for the book
     */
    private void init(){
        this.mRootTemplateUID = generateUID();
        mLastSync = new Timestamp(System.currentTimeMillis());
    }

    /**
     * Return the root account GUID of this book
     * @return GUID of the book root account
     */
    public String getRootAccountUID() {
        return mRootAccountUID;
    }

    /**
     * Sets the GUID of the root account of this book.
     * <p>Each book has only one root account</p>
     * @param rootAccountUID GUID of the book root account
     */
    public void setRootAccountUID(String rootAccountUID) {
        mRootAccountUID = rootAccountUID;
    }

    /**
     * Return GUID of the template root account
     * @return GUID of template root acount
     */
    public String getRootTemplateUID() {
        return mRootTemplateUID;
    }

    /**
     * Set the GUID of the root template account
     * @param rootTemplateUID GUID of the root template account
     */
    public void setRootTemplateUID(String rootTemplateUID) {
        mRootTemplateUID = rootTemplateUID;
    }

    /**
     * Check if this book is the currently active book in the app
     * <p>An active book is one whose data is currently displayed in the UI</p>
     * @return {@code true} if this is the currently active book, {@code false} otherwise
     */
    public boolean isActive() {
        return mActive;
    }

    /**
     * Sets this book as the currently active one in the application
     * @param active Flag for activating/deactivating the book
     */
    public void setActive(boolean active) {
        mActive = active;
    }

    /**
     * Return the Uri of the XML file from which the book was imported.
     * <p>In API level 16 and above, this is the Uri from the storage access framework which will
     * be used for synchronization of the book</p>
     * @return Uri of the book source XML
     */
    public Uri getSourceUri() {
        return mSourceUri;
    }

    /**
     * Set the Uri of the XML source for the book
     * <p>This Uri will be used for sync where applicable</p>
     * @param uri Uri of the GnuCash XML source file
     */
    public void setSourceUri(Uri uri) {
        this.mSourceUri = uri;
    }

    /**
     * Returns a name for the book
     * <p>This is the user readable string which is used in UI unlike the root account GUID which
     * is used for uniquely identifying each book</p>
     * @return Name of the book
     */
    public String getDisplayName() {
        return mDisplayName;
    }

    /**
     * Set a name for the book
     * @param name Name of the book
     */
    public void setDisplayName(String name) {
        this.mDisplayName = name;
    }

    /**
     * Get the time of last synchronization of the book
     * @return Timestamp of last synchronization
     */
    public Timestamp getLastSync() {
        return mLastSync;
    }

    /**
     * Set the time of last synchronization of the book
     * @param lastSync Timestamp of last synchronization
     */
    public void setLastSync(Timestamp lastSync) {
        this.mLastSync = lastSync;
    }
}
