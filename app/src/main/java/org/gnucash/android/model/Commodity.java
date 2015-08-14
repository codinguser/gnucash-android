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
    private int mFraction;
    private int mQuoteFlag;

    /**
     * Create a new commodity
     * @param fullname Official full name of the currency
     * @param mnemonic Official abbreviated designation for the currency
     * @param fraction Number of sub-units that the basic commodity can be divided into
     */
    public Commodity(String fullname, String mnemonic, int fraction){
        this.mFullname = fullname;
        this.mMnemonic = mnemonic;
        this.mFraction = fraction;
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

    public int getFraction() {
        return mFraction;
    }

    public void setFraction(int fraction) {
        this.mFraction = fraction;
    }

    public int getQuoteFlag() {
        return mQuoteFlag;
    }

    public void setQuoteFlag(int quoteFlag) {
        this.mQuoteFlag = quoteFlag;
    }
}
