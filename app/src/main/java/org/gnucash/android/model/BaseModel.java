package org.gnucash.android.model;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * Represents the base data model which is persisted to the database.
 * All other models should extend this base model and add entries.
 */
public abstract class BaseModel {
    protected String mUID;
    protected Timestamp mCreatedTimestamp;
    protected Timestamp mModifiedTimestamp;

    /**
     * Initializes the model attributes and generates a GUID
     */
    public BaseModel(){
        mUID = generateUID();
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

    public String getUID() {
        return mUID;
    }

    public void setUID(String uid) {
        this.mUID = uid;
    }

    public Timestamp getCreatedTimestamp() {
        return mCreatedTimestamp;
    }

    public void setCreatedTimestamp(Timestamp createdTimestamp) {
        this.mCreatedTimestamp = createdTimestamp;
    }

    public Timestamp getModifiedTimestamp() {
        return mModifiedTimestamp;
    }

    public void setModifiedTimestamp(Timestamp modifiedTimestamp) {
        this.mModifiedTimestamp = modifiedTimestamp;
    }
}
