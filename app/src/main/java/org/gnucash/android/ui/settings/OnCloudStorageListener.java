/*
 * Copyright (c) 2012 Ngewi Fet <ngewif@gmail.com>
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

package org.gnucash.android.ui.settings;

/**
 * Listener interface for cloud storage events.
 *
 * @author moshe.w
 */
public interface OnCloudStorageListener {
    /**
     * Notification that a folder has been created on Google Drive.
     *
     * @param folderId the unique folder id.
     */
    void onGoogleDriveFolderCreated(String folderId);

    /**
     * Notification that a folder has been forgotten on Google Drive.
     *
     * @param folderId the forgotten unique folder id.
     */
    void onGoogleDriveFolderForgot(String folderId);
}
