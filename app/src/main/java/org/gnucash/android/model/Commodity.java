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

/**
 * Commodities are the currencies used in the application.
 * At the moment only ISO4217 currencies are supported
 */
public class Commodity extends BaseModel {
    public enum Namespace { ISO4217 } //Namespace for commodities

    private Namespace mNamespace = Namespace.ISO4217;

    /**
     * This is the currency code for ISO4217 currencies
     */
    private String mMnemonic;
    private String mFullname;
    private String mCusip;
    private String mLocalSymbol = "";
    private int mSmallestFraction;
    private int mQuoteFlag;

    /**
     * Create a new commodity
     * @param fullname Official full name of the currency
     * @param mnemonic Official abbreviated designation for the currency
     * @param smallestFraction Number of sub-units that the basic commodity can be divided into, as power of 10. e.g. 10^&lt;number_of_fraction_digits&gt;
     */
    public Commodity(String fullname, String mnemonic, int smallestFraction){
        this.mFullname = fullname;
        this.mMnemonic = mnemonic;
        setSmallestFraction(smallestFraction);
    }

    public Namespace getNamespace() {
        return mNamespace;
    }

    public void setNamespace(Namespace namespace) {
        this.mNamespace = namespace;
    }

    /**
     * Returns the mnemonic, or currency code for ISO4217 currencies
     * @return Mnemonic of the commodity
     */
    public String getMnemonic() {
        return mMnemonic;
    }

    public void setMnemonic(String mMnemonic) {
        this.mMnemonic = mMnemonic;
    }

    public String getFullname() {
        return mFullname;
    }

    public void setFullname(String mFullname) {
        this.mFullname = mFullname;
    }

    public String getCusip() {
        return mCusip;
    }

    public void setCusip(String mCusip) {
        this.mCusip = mCusip;
    }

    public String getLocalSymbol() {
        return mLocalSymbol;
    }

    /**
     * Returns the symbol for this commodity.
     * <p>Normally this would be the local symbol, but in it's absence, the mnemonic (currency code)
     * is returned.</p>
     * @return
     */
    public String getSymbol(){
        if (mLocalSymbol == null || mLocalSymbol.isEmpty()){
            return mMnemonic;
        }
        return mLocalSymbol;
    }

    public void setLocalSymbol(String localSymbol) {
        this.mLocalSymbol = localSymbol;
    }

    /**
     * Returns the smallest fraction supported by the commodity as a power of 10.
     * <p>i.e. for commodities with no fractions, 1 is returned, for commodities with 2 fractions, 100 is returned</p>
     * @return Smallest fraction as power of 10
     */
    public int getSmallestFraction() {
        return mSmallestFraction;
    }

    /**
     * Returns the (minimum) number of digits that this commodity supports in its fractional part
     * @return Number of digits in fraction
     */
    public int getSmallestFractionDigits(){
        switch (mSmallestFraction) {
            case 1:     return 0;
            case 10:    return 1;
            case 100:   return 2;
            case 1000:  return 3;
            default:
                throw new UnsupportedOperationException("Invalid fraction digits in commodity with fraction: " + mSmallestFraction);
        }
    }

    /**
     * Sets the smallest fraction for the commodity.
     * <p>The fraction is a power of 10. So commodities with 2 fraction digits, have fraction of 10^2 = 100.<br>
     *     If the parameter is any other value, a default fraction of 100 will be set</p>
     * @param smallestFraction Smallest fraction as power of ten
     * @throws IllegalArgumentException if the smallest fraction is not a power of 10
     */
    public void setSmallestFraction(int smallestFraction) {
        if (smallestFraction != 1 && smallestFraction != 10 && smallestFraction != 100 && smallestFraction != 1000 && smallestFraction != 10000) //make sure we are not getting digits
            this.mSmallestFraction = 100;
        else
            this.mSmallestFraction = smallestFraction;
    }

    public int getQuoteFlag() {
        return mQuoteFlag;
    }

    public void setQuoteFlag(int quoteFlag) {
        this.mQuoteFlag = quoteFlag;
    }
}
