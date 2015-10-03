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
    private long mValueNum;
    private long mValueDenom;

    /**
     * String indicating that the price was provided by the user
     */
    public static final String SOURCE_USER = "user:xfer-dialog";

    public Price(){
        mDate = new Timestamp(System.currentTimeMillis());
    }

    /**
     * Create new instance with the GUIDs of the commodities
     * @param commodityUID GUID of the origin commodity
     * @param currencyUID GUID of the target commodity
     */
    public Price(String commodityUID, String currencyUID){
        this.mCommodityUID = commodityUID;
        this.mCurrencyUID = currencyUID;
        mDate = new Timestamp(System.currentTimeMillis());
    }

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

    public long getValueNum() {
        return mValueNum;
    }

    public void setValueNum(long valueNum) {
        this.mValueNum = valueNum;
    }

    public long getValueDenom() {
        return mValueDenom;
    }

    public void setValueDenom(long valueDenom) {
        this.mValueDenom = valueDenom;
    }

    public void reduce() {
        if (mValueDenom < 0) {
            mValueDenom = -mValueDenom;
            mValueNum = -mValueNum;
        }
        if (mValueDenom != 0 && mValueNum != 0) {
            long num1 = mValueNum;
            if (num1 < 0) {
                num1 = -num1;
            }
            long num2 = mValueDenom;
            long commonDivisor = 1;
            for(;;) {
                long r = num1 % num2;
                if (r == 0) {
                    commonDivisor = num2;
                    break;
                }
                num1 = r;
                r = num2 % num1;
                if (r == 0) {
                    commonDivisor = num1;
                    break;
                }
                num2 = r;
            }
            mValueNum /= commonDivisor;
            mValueDenom /= commonDivisor;
        }
    }
}
