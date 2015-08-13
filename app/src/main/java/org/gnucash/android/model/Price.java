package org.gnucash.android.model;

import java.sql.Timestamp;

/**
 * Model for commodity prices
 */
public class Price extends BaseModel {

    private String mCommodityUID;
    private String mCurrencyUID;
    private Timestamp mDate;
    private String mSource;
    private String mType;
    private int mValueNum;
    private int mValueDenom;

    public String getCommodityUID() {
        return mCommodityUID;
    }

    public void setCommodityUID(String mCommodityUID) {
        this.mCommodityUID = mCommodityUID;
    }

    public String getCurrencyUID() {
        return mCurrencyUID;
    }

    public void setCurrencyUID(String currencyUID) {
        this.mCurrencyUID = currencyUID;
    }

    public Timestamp getDate() {
        return mDate;
    }

    public void setDate(Timestamp date) {
        this.mDate = date;
    }

    public String getSource() {
        return mSource;
    }

    public void setSource(String source) {
        this.mSource = source;
    }

    public String getType() {
        return mType;
    }

    public void setType(String type) {
        this.mType = type;
    }

    public int getValueNum() {
        return mValueNum;
    }

    public void setValueNum(int valueNum) {
        this.mValueNum = valueNum;
    }

    public int getValueDenom() {
        return mValueDenom;
    }

    public void setValueDenom(int valueDenom) {
        this.mValueDenom = valueDenom;
    }
}
