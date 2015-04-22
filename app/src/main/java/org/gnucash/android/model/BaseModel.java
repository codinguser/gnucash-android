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

import java.sql.Timestamp;
import java.util.UUID;

/**
 * Abstract class representing the base data model which is persisted to the database.
 * All other models should extend this base model.
 */
public abstract class BaseModel {
    protected String mUID;
    protected Timestamp mCreatedTimestamp;
    protected Timestamp mModifiedTimestamp;

    /**
     * Initializes the model attributes and generates a GUID
     */
    public BaseModel(){
        mCreatedTimestamp = new Timestamp(System.currentTimeMillis());
        mModifiedTimestamp = new Timestamp(System.currentTimeMillis());
    }

    /**
     * Method for generating the Global Unique ID for the object.
     * Subclasses can override this method to provide a different implementation
     * @return Random GUID for the model object
     */
    protected String generateUID(){
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    /**
     * Returns the GUID of the model
     * @return String unique identifier for this model
     */
    public String getUID() {
        if (mUID == null)
        {
            mUID = generateUID();
        }
        return mUID;
    }

    /**
     * Sets the GUID of the model
     * @param uid String unique ID
     */
    public void setUID(String uid) {
        this.mUID = uid;
    }

    /**
     * Returns the timestamp when this model entry was created in the database
     * @return Timestamp of creation of model
     */
    public Timestamp getCreatedTimestamp() {
        return mCreatedTimestamp;
    }

    /**
     * Sets the timestamp when the model was created
     * @param createdTimestamp Timestamp of model creation
     */
    public void setCreatedTimestamp(Timestamp createdTimestamp) {
        this.mCreatedTimestamp = createdTimestamp;
    }

    /**
     * Returns the timestamp when the model record in the database was last modified.
     * @return Timestamp of last modification
     */
    public Timestamp getModifiedTimestamp() {
        return mModifiedTimestamp;
    }

    /**
     * Sets the timestamp when the model was last modified in the database
     * <p>Although the database automatically has triggers for entering the timestamp,
     * when SQL INSERT OR REPLACE syntax is used, it is possible to override the modified timestamp.
     * <br/>In that case, it has to be explicitly set in the SQL statement.</p>
     * @param modifiedTimestamp Timestamp of last modification
     */
    public void setModifiedTimestamp(Timestamp modifiedTimestamp) {
        this.mModifiedTimestamp = modifiedTimestamp;
    }
}
