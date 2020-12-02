package org.gnucash.android.model;


import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import org.gnucash.android.db.adapter.AccountsDbAdapter;

import java.sql.Timestamp;

/**
 * A split amount in a transaction.
 *
 * <p>Every transaction is made up of at least two splits (representing a double
 * entry transaction)</p>
 *
 * <p>Amounts are always stored unsigned. This is independent of the negative values
 * which are shown in the UI (for user convenience). The actual movement of the
 * balance in the account depends on the type of normal balance of the account
 * and the transaction type of the split (CREDIT/DEBIT).</p>
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class Split extends BaseModel implements Parcelable{

    /**
     * Flag indicating that the split has been reconciled
     */
    public static final char FLAG_RECONCILED        = 'y';

    /**
     * Flag indicating that the split has not been reconciled
     */
    public static final char FLAG_NOT_RECONCILED    = 'n';

    /**
     * Flag indicating that the split has been cleared, but not reconciled
     */
    public static final char FLAG_CLEARED           = 'c';


    /**
     * Amount absolute value of this split which is in the currency of the transaction
     */
    private Money mAmountAbsValue;

    /**
     * Amount of the split in the currency of the account to which the split belongs
     */
    private Money mQuantityAbsValue;

    /**
     * Transaction UID which this split belongs to
     */
    private String mTransactionUID = "";

    /**
     * Account UID which this split belongs to
     */
    private String mAccountUID;

    /**
     * The type of this transaction, credit or debit
     */
    private TransactionType mSplitType = TransactionType.CREDIT;

    /**
     * Memo associated with this split
     */
    private String mMemo;

    private char mReconcileState = FLAG_NOT_RECONCILED;

    /**
     * Database required non-null field
     */
    private Timestamp mReconcileDate = new Timestamp(System.currentTimeMillis());

    /**
     * Initialize split with a value and quantity amounts and the owning account
     *
     * <p>The transaction type is set to CREDIT. The amounts are stored unsigned.</p>
     *
     * @param value Money value amount of this split in the currency of the transaction.
     * @param quantity Money value amount of this split in the currency of the
     *                 owning account.
     * @param accountUID String UID of transfer account
     */
    public Split(@NonNull Money value, @NonNull Money quantity, String accountUID){

        // Store absolute value
        setValue(value);
        setQuantity(quantity);

        setAccountUID(accountUID);
    }

    /**
     * Initialize split with a value amount and the owning account
     *
     * <p>The transaction type is set to CREDIT. The amount is stored unsigned.</p>
     *
     * @param amount Money value amount of this split. Value is always in the
     *               currency the owning transaction. This amount will be assigned
     *               as both the value and the quantity of this split.
     * @param accountUID String UID of owning account
     */
    public Split(@NonNull Money amount, String accountUID){
        this(amount, new Money(amount), accountUID);
    }


    /**
     * Clones the <code>sourceSplit</code> to create a new instance with same fields
     *
     * @param sourceSplit Split to be cloned
     * @param generateUID Determines if the clone should have a new UID or should
     *                    maintain the one from source
     */
    public Split(Split sourceSplit, boolean generateUID){
        this.mMemo          = sourceSplit.mMemo;
        this.mAccountUID    = sourceSplit.mAccountUID;
        this.mSplitType     = sourceSplit.mSplitType;
        this.mTransactionUID = sourceSplit.mTransactionUID;
        setValue(new Money(sourceSplit.mAmountAbsValue));
        setQuantity(new Money(sourceSplit.mQuantityAbsValue));

        //todo: clone reconciled status
        if (generateUID){
            generateUID();
        } else {
            setUID(sourceSplit.getUID());
        }
    }

    /**
     * Returns the value (= signed amount) of the split
     *
     * @return Money amount of the split with the currency of the transaction
     *
     * @see #getQuantity()
     */
    public Money getValueWithSignum() {

        // splitAmount (positive or negative number)
        Money signedValue = TransactionType.DEBIT.equals(getType())
                            ? getValue()
                            : getValue().negate();

        return signedValue;
    }


    /**
     * Returns the absolute value of the amount of the split
     *
     * @return Money amount of the split with the currency of the transaction
     * @see #getQuantity()
     */
    public Money getValue() {

        return mAmountAbsValue;
    }

    /**
     * Sets the value amount of the split.
     *
     * <p>The value is in the currency of the containing transaction.
     * It's stored unsigned.</p>
     *
     * @param amountValue Money value of this split
     * @see #setQuantity(Money)
     */
    public void setValue(Money amountValue) {

        mAmountAbsValue = amountValue.abs();
    }

    /**
     * Returns the quantity amount of the split.
     * <p>The quantity is in the currency of the account to which the split is associated</p>
     * @return Money quantity amount
     * @see #getValue()
     */
    public Money getQuantity() {

        return mQuantityAbsValue;
    }

    /**
     * Sets the quantity value of the split.
     *
     * <p>The quantity is in the currency of the owning account.
     * It will be stored unsigned.</p>
     *
     * @param quantityAbsValue Money quantity amount
     * @see #setValue(Money)
     */
    public void setQuantity(Money quantityAbsValue) {

        this.mQuantityAbsValue = quantityAbsValue.abs();
    }

    /**
     * Returns transaction GUID to which the split belongs
     * @return String GUID of the transaction
     */
    public String getTransactionUID() {
        return mTransactionUID;
    }

    /**
     * Sets the transaction to which the split belongs
     * @param transactionUID GUID of transaction
     */
    public void setTransactionUID(String transactionUID) {
        this.mTransactionUID = transactionUID;
    }

    /**
     * Returns the account GUID of this split
     * @return GUID of the account
     */
    public String getAccountUID() {
        return mAccountUID;
    }

    /**
     * Sets the GUID of the account of this split
     * @param accountUID GUID of account
     */
    public void setAccountUID(String accountUID) {
        this.mAccountUID = accountUID;
    }

    /**
     * Returns the type of the split
     * @return {@link TransactionType} of the split
     */
    public TransactionType getType() {
        return mSplitType;
    }

    /**
     * Sets the type of this split
     * @param splitType Type of the split
     */
    public void setType(TransactionType splitType) {
        this.mSplitType = splitType;
    }

    /**
     * Returns the memo of this split
     * @return String memo of this split
     */
    public String getMemo() {
        return mMemo;
    }

    /**
     * Sets this split memo
     * @param memo String memo of this split
     */
    public void setMemo(String memo) {
        this.mMemo = memo;
    }

    /**
     * Creates a split which is a pair of this instance.
     * A pair split has all the same attributes except that the SplitType is inverted and it belongs
     * to another account.
     * @param accountUID GUID of account
     * @return New split pair of current split
     * @see TransactionType#invert()
     */
    public Split createPair(String accountUID) {

        Split pair = new Split(mAmountAbsValue,
                               accountUID);
        pair.setType(mSplitType.invert());
        pair.setMemo(mMemo);
        pair.setTransactionUID(mTransactionUID);
        pair.setQuantity(mQuantityAbsValue);
        return pair;
    }

    /**
     * Clones this split and returns an exact copy.
     * @return New instance of a split which is a copy of the current one
     */
    protected Split clone() throws CloneNotSupportedException {
        super.clone();
        Split split = new Split(mAmountAbsValue,
                                mAccountUID);
        split.setUID(getUID());
        split.setType(mSplitType);
        split.setMemo(mMemo);
        split.setTransactionUID(mTransactionUID);
        split.setQuantity(mQuantityAbsValue);
        return split;
    }

    /**
     * Checks is this <code>other</code> is a pair split of this.
     * <p>Two splits are considered a pair if they have the same amount and
     * opposite split types</p>
     * @param other the other split of the pair to be tested
     * @return whether the two splits are a pair
     */
    public boolean isPairOf(Split other) {

        return mAmountAbsValue.equals(other.mAmountAbsValue) && mSplitType.invert()
                                                                          .equals(other.mSplitType);
    }

    /**
     * Return the reconciled state of this split
     * <p>
     *     The reconciled state is one of the following values:
     *     <ul>
     *         <li><b>y</b>: means this split has been reconciled</li>
     *         <li><b>n</b>: means this split is not reconciled</li>
     *         <li><b>c</b>: means split has been cleared, but not reconciled</li>
     *     </ul>
     * </p>
     * <p>You can check the return value against the reconciled flags
     * {@link #FLAG_RECONCILED}, {@link #FLAG_NOT_RECONCILED}, {@link #FLAG_CLEARED}</p>
     *
     * @return Character showing reconciled state
     */
    public char getReconcileState() {
        return mReconcileState;
    }

    /**
     * Check if this split is reconciled
     * @return {@code true} if the split is reconciled, {@code false} otherwise
     */
    public boolean isReconciled(){
        return mReconcileState == FLAG_RECONCILED;
    }

    /**
     * Set reconciled state of this split.
     * <p>
     *     The reconciled state is one of the following values:
     *     <ul>
     *         <li><b>y</b>: means this split has been reconciled</li>
     *         <li><b>n</b>: means this split is not reconciled</li>
     *         <li><b>c</b>: means split has been cleared, but not reconciled</li>
     *     </ul>
     * </p>
     * @param reconcileState One of the following flags {@link #FLAG_RECONCILED},
     *  {@link #FLAG_NOT_RECONCILED}, {@link #FLAG_CLEARED}
     */
    public void setReconcileState(char reconcileState) {
        this.mReconcileState = reconcileState;
    }

    /**
     * Return the date of reconciliation
     * @return Timestamp
     */
    public Timestamp getReconcileDate() {
        return mReconcileDate;
    }

    /**
     * Set reconciliation date for this split
     * @param reconcileDate Timestamp of reconciliation
     */
    public void setReconcileDate(Timestamp reconcileDate) {
        this.mReconcileDate = reconcileDate;
    }

    @Override
    public String toString() {

        return mSplitType.name() + " of " + mAmountAbsValue.toString()
               + " in account: "
               + mAccountUID
               + " ("
               + AccountsDbAdapter.getInstance()
                                  .getAccountFullName(mAccountUID)
               + ")";
    }

    /**
     * Returns a string representation of the split which can be parsed again
     * using {@link org.gnucash.android.model.Split#parseSplit(String)}
     *
     * <p>The string is formatted as:<br/>
     * "&lt;uid&gt;;&lt;valueNum&gt;;&lt;valueDenom&gt;;&lt;valueCurrencyCode&gt;;&lt;quantityNum&gt;;&lt;quantityDenom&gt;;&lt;quantityCurrencyCode&gt;;&lt;transaction_uid&gt;;&lt;account_uid&gt;;&lt;type&gt;;&lt;memo&gt;"
     * </p>
     *
     * <p><b>Only the memo field is allowed to be null</b></p>
     *
     * @return the converted CSV string of this split
     */
    public String toCsv(){
        String sep = ";";
        //TODO: add reconciled state and date
        String splitString = getUID()
                             + sep
                             + mAmountAbsValue.getNumerator()
                             + sep
                             + mAmountAbsValue.getDenominator()
                             + sep
                             + mAmountAbsValue.getCommodity()
                                              .getCurrencyCode()
                             + sep
                             + mQuantityAbsValue.getNumerator()
                             + sep
                             + mQuantityAbsValue.getDenominator()
                             + sep
                             + mQuantityAbsValue.getCommodity()
                                                .getCurrencyCode()
                             + sep
                             + mTransactionUID
                             + sep
                             + mAccountUID
                             + sep
                             + mSplitType.name();
        if (mMemo != null){
            splitString = splitString + sep + mMemo;
        }
        return splitString;
    }

    /**
     * Parses a split which is in the format:<br/>
     * "<uid>;<valueNum>;<valueDenom>;<currency_code>;<quantityNum>;<quantityDenom>;<currency_code>;<transaction_uid>;<account_uid>;<type>;<memo>".
     *
     * <p>Also supports parsing of the deprecated format
     * "<amount>;<currency_code>;<transaction_uid>;<account_uid>;<type>;<memo>".
     * The split input string is the same produced by the {@link Split#toCsv()} method.</p>
     *
     * @param splitCsvString String containing formatted split
     * @return Split instance parsed from the string
     */
    public static Split parseSplit(String splitCsvString) {
        //TODO: parse reconciled state and date
        String[] tokens = splitCsvString.split(";");
        if (tokens.length < 8) { //old format splits
            Money amount = new Money(tokens[0], tokens[1]);
            Split split = new Split(amount, tokens[2]);
            split.setTransactionUID(tokens[3]);
            split.setType(TransactionType.valueOf(tokens[4]));
            if (tokens.length == 6) {
                split.setMemo(tokens[5]);
            }
            return split;
        } else {
            long valueNum = Long.parseLong(tokens[1]);
            long valueDenom = Long.parseLong(tokens[2]);
            String valueCurrencyCode = tokens[3];
            long quantityNum = Long.parseLong(tokens[4]);
            long quantityDenom = Long.parseLong(tokens[5]);
            String qtyCurrencyCode = tokens[6];

            Money value = new Money(valueNum, valueDenom, valueCurrencyCode);
            Money quantity = new Money(quantityNum, quantityDenom, qtyCurrencyCode);

            Split split = new Split(value, tokens[8]);
            split.setUID(tokens[0]);
            split.setQuantity(quantity);
            split.setTransactionUID(tokens[7]);
            split.setType(TransactionType.valueOf(tokens[9]));
            if (tokens.length == 11) {
                split.setMemo(tokens[10]);
            }
            return split;
        }
    }

    /**
     * Two splits are considered equivalent if all the fields (excluding GUID
     * and timestamps - created, modified, reconciled) are equal.
     *
     * <p>Any two splits which are equal are also equivalent, but the reverse
     * is not true</p>
     *
     * <p>The difference with to {@link #equals(Object)} is that the GUID of
     * the split is not considered. This is useful in cases where a new split
     * is generated for a transaction with the same properties, but a new GUID
     * is generated e.g. when editing a transaction and modifying the splits</p>
     *
     * @param split Other split for which to test equivalence
     * @return {@code true} if both splits are equivalent, {@code false} otherwise
     */
    @SuppressWarnings("SimplifiableIfStatement")
    public boolean isEquivalentTo(Split split){
        if (this == split) return true;
        if (super.equals(split)) return true;

        if (mReconcileState != split.mReconcileState) return false;
        if (!mAmountAbsValue.equals(split.mAmountAbsValue)) {
            return false;
        }
        if (!mQuantityAbsValue.equals(split.mQuantityAbsValue)) {
            return false;
        }
        if (!mTransactionUID.equals(split.mTransactionUID)) return false;
        if (!mAccountUID.equals(split.mAccountUID)) return false;
        if (mSplitType != split.mSplitType) return false;
        return mMemo != null ? mMemo.equals(split.mMemo) : split.mMemo == null;
    }

    /**
     * Two splits are considered equal if all their properties excluding
     * timestamps (created, modified, reconciled) are equal.
     *
     * @param o Other split to compare for equality
     * @return {@code true} if this split is equal to {@code o}, {@code false} otherwise
     */
    @SuppressWarnings("SimplifiableIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Split split = (Split) o;

        if (mReconcileState != split.mReconcileState) return false;
        if (!mAmountAbsValue.equals(split.mAmountAbsValue)) {
            return false;
        }
        if (!mQuantityAbsValue.equals(split.mQuantityAbsValue)) {
            return false;
        }
        if (!mTransactionUID.equals(split.mTransactionUID)) return false;
        if (!mAccountUID.equals(split.mAccountUID)) return false;
        if (mSplitType != split.mSplitType) return false;
        return mMemo != null ? mMemo.equals(split.mMemo) : split.mMemo == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + mAmountAbsValue.hashCode();
        result = 31 * result + mQuantityAbsValue.hashCode();
        result = 31 * result + mTransactionUID.hashCode();
        result = 31 * result + mAccountUID.hashCode();
        result = 31 * result + mSplitType.hashCode();
        result = 31 * result + (mMemo != null ? mMemo.hashCode() : 0);
        result = 31 * result + (int) mReconcileState;
        return result;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getUID());
        dest.writeString(mAccountUID);
        dest.writeString(mTransactionUID);
        dest.writeString(mSplitType.name());

        dest.writeLong(mAmountAbsValue.getNumerator());
        dest.writeLong(mAmountAbsValue.getDenominator());
        dest.writeString(mAmountAbsValue.getCommodity()
                                        .getCurrencyCode());

        dest.writeLong(mQuantityAbsValue.getNumerator());
        dest.writeLong(mQuantityAbsValue.getDenominator());
        dest.writeString(mQuantityAbsValue.getCommodity()
                                          .getCurrencyCode());

        dest.writeString(mMemo == null ? "" : mMemo);
        dest.writeString(String.valueOf(mReconcileState));
        dest.writeString(mReconcileDate.toString());
    }

    /**
     * Constructor for creating a Split object from a Parcel
     * @param source Source parcel containing the split
     * @see #CREATOR
     */
    private Split(Parcel source){
        setUID(source.readString());
        mAccountUID = source.readString();
        mTransactionUID = source.readString();
        mSplitType = TransactionType.valueOf(source.readString());

        long valueNum = source.readLong();
        long valueDenom = source.readLong();
        String valueCurrency = source.readString();
        setValue(new Money(valueNum,
                           valueDenom,
                           valueCurrency));

        long qtyNum = source.readLong();
        long qtyDenom = source.readLong();
        String qtyCurrency = source.readString();
        setQuantity(new Money(qtyNum,
                              qtyDenom,
                              qtyCurrency));

        String memo = source.readString();
        mMemo = memo.isEmpty() ? null : memo;
        mReconcileState = source.readString().charAt(0);
        mReconcileDate = Timestamp.valueOf(source.readString());
    }

    /**
     * Creates new Parcels containing the information in this split during serialization
     */
    public static final Parcelable.Creator<Split> CREATOR
            = new Parcelable.Creator<Split>() {

        @Override
        public Split createFromParcel(Parcel source) {
            return new Split(source);
        }

        @Override
        public Split[] newArray(int size) {
            return new Split[size];
        }
    };

}
