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
    /**
     * Unique identifier of this model instance.
     * <p>It is declared private because it is generated only on-demand. Sub-classes should use the accessor methods to read and write this value</p>
     * @see #getUID()
     * @see #setUID(String)
     */
    private String mUID;
    protected Timestamp mCreatedTimestamp;
    protected Timestamp mModifiedTimestamp;

    /**
     * Initializes the model attributes.
     * <p>A GUID for this model is not generated in the constructor.
     * A unique ID will be generated on demand with a call to {@link #getUID()}</p>
     */
    public BaseModel(){
        mCreatedTimestamp = new Timestamp(System.currentTimeMillis());
        mModifiedTimestamp = new Timestamp(System.currentTimeMillis());
    }

    /**
     * Method for generating the Global Unique ID for the model object
     * @return Random GUID for the model object
     */
    public static String generateUID(){
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    /**
     * Returns a unique string identifier for this model instance
     * @return GUID for this model
     */
    public String getUID() {
        if (mUID == null)
        {
            mUID = generateUID();
        }
        return mUID;
    }

    /**
     * Sets the GUID of the model.
     * <p>A new GUID can be generated with a call to {@link #generateUID()}</p>
     * @param uid String unique ID
     */
    public void setUID(String uid) {
        this.mUID = uid;
    }

    /**8
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

    /**
     * Two instances are considered equal if their GUID's are the same
     * @param o BaseModel instance to compare
     * @return {@code true} if both instances are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseModel)) return false;

        BaseModel baseModel = (BaseModel) o;

        return getUID().equals(baseModel.getUID());

    }

    @Override
    public int hashCode() {
        return getUID().hashCode();
    }
}
