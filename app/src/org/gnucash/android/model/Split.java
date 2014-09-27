package org.gnucash.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.gnucash.android.export.xml.GncXmlHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.UUID;

/**
 * A split amount in a transaction.
 * Every transaction is made up of at least two splits (representing a double entry transaction)
 * <p>The split amount is always stored in the database as the absolute value alongside its transaction type of CREDIT/DEBIT<br/>
 * This is independent of the negative values which are shown in the UI (for user convenience).
 * The actual movement of the balance in the account depends on the type of normal balance of the account and the
 * transaction type of the split.</p>
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class Split {
    /**
     * Amount value of this split
     */
    @NonNull
    private Money mAmount;

    /**
     * Unique ID of this split
     */
    @NonNull
    private String mUID;

    /**
     * Transaction UID which this split belongs to
     */
    @NonNull
    private String mTransactionUID = "";

    /**
     * Account UID which this split belongs to
     */
    @NonNull
    private String mAccountUID;

    /**
     * The type of this transaction, credit or debit
     */
    @NonNull
    private TransactionType mSplitType = TransactionType.CREDIT;

    /**
     * Memo associated with this split
     */
    @Nullable
    private String mMemo;

    /**
     * Initialize split with an amount and account
     * @param amount Money amount of this split
     * @param accountUID String UID of transfer account
     */
    public Split(@NonNull Money amount, @NonNull String accountUID){
        setAmount(amount);
        setAccountUID(accountUID);
        mUID = UUID.randomUUID().toString().replaceAll("-","");
    }

    /**
     * Clones the <code>sourceSplit</code> to create a new instance with same fields
     * @param sourceSplit Split to be cloned
     * @param generateUID Determines if the clone should have a new UID or should maintain the one from source
     */
    public Split(@NonNull Split sourceSplit, boolean generateUID){
        this.mMemo          = sourceSplit.mMemo;
        this.mAccountUID    = sourceSplit.mAccountUID;
        this.mSplitType     = sourceSplit.mSplitType;
        this.mTransactionUID = sourceSplit.mTransactionUID;
        this.mAmount        = sourceSplit.mAmount.absolute();

        if (generateUID){
            mUID = UUID.randomUUID().toString().replaceAll("-","");
        } else {
            this.mUID           = sourceSplit.mUID;
        }
    }

    @NonNull
    public Money getAmount() {
        return mAmount;
    }

    public void setAmount(@NonNull Money amount) {
        this.mAmount = amount;
    }

    @NonNull
    public String getUID() {
        return mUID;
    }

    public void setUID(@NonNull String uid) {
        this.mUID = uid;
    }

    @NonNull
    public String getTransactionUID() {
        return mTransactionUID;
    }

    public void setTransactionUID(@NonNull String transactionUID) {
        this.mTransactionUID = transactionUID;
    }

    @NonNull
    public String getAccountUID() {
        return mAccountUID;
    }

    public void setAccountUID(@NonNull String accountUID) {
        this.mAccountUID = accountUID;
    }

    @NonNull
    public TransactionType getType() {
        return mSplitType;
    }

    public void setType(@NonNull TransactionType transactionType) {
        this.mSplitType = transactionType;
    }

    @Nullable
    public String getMemo() {
        return mMemo;
    }

    public void setMemo(@Nullable String memo) {
        this.mMemo = memo;
    }

    @NonNull
    public Split createPair(@NonNull String accountUID){
        Split pair = new Split(mAmount.absolute(), accountUID);
        pair.setType(mSplitType.invert());
        pair.setMemo(mMemo);

        return pair;
    }

    @NonNull
    protected Split clone() {
        Split split = new Split(mAmount, mAccountUID);
        split.mUID = mUID;
        split.setType(mSplitType);
        split.setMemo(mMemo);
        split.setTransactionUID(mTransactionUID);
        return split;
    }

    /**
     * Checks is this <code>other</code> is a pair split of this.
     * <p>Two splits are considered a pair if they have the same amount and opposite split types</p>
     * @param other the other split of the pair to be tested
     * @return whether the two splits are a pair
     */
    public boolean isPairOf(@NonNull Split other) {
        return mAmount.absolute().equals(other.mAmount.absolute())
                && mSplitType.invert().equals(other.mSplitType);
    }

    @NonNull
    @Override
    public String toString() {
        return mSplitType.name() + " of " + mAmount.toString() + " in account: " + mAccountUID;
    }

    /**
     * Returns a string representation of the split which can be parsed again using {@link org.gnucash.android.model.Split#parseSplit(String)}
     * @return the converted CSV string of this split
     */
    @NonNull
    public String toCsv(){
        String splitString = mAmount.toString() + ";" + mAmount.getCurrency().getCurrencyCode() + ";"
                + mAccountUID + ";" + mSplitType.name();
        if (mMemo != null){
            splitString = splitString + ";" + mMemo;
        }
        return splitString;
    }

    /**
     * Creates a GnuCash XML representation of this split
     * @param doc XML {@link org.w3c.dom.Document} for creating the nodes
     * @param rootNode Parent node to append the split XML to
     * @deprecated Use the {@link org.gnucash.android.export.xml.GncXmlExporter} to generate XML
     */
    public void toGncXml(@NonNull Document doc, @NonNull Element rootNode) {
        Element idNode = doc.createElement(GncXmlHelper.TAG_SPLIT_ID);
        idNode.setAttribute("type", "guid");
        idNode.appendChild(doc.createTextNode(mUID));

        Element memoNode = doc.createElement(GncXmlHelper.TAG_SPLIT_MEMO);
        if (mMemo != null)
            memoNode.appendChild(doc.createTextNode(mMemo));

        Element stateNode = doc.createElement(GncXmlHelper.TAG_RECONCILED_STATE);
        stateNode.appendChild(doc.createTextNode("n"));

        Element valueNode = doc.createElement(GncXmlHelper.TAG_SPLIT_VALUE);
        valueNode.appendChild(doc.createTextNode(GncXmlHelper.formatMoney(this)));

        Element quantityNode = doc.createElement(GncXmlHelper.TAG_SPLIT_QUANTITY);
        quantityNode.appendChild(doc.createTextNode(GncXmlHelper.formatMoney(this)));

        Element accountNode = doc.createElement(GncXmlHelper.TAG_SPLIT_ACCOUNT);
        accountNode.setAttribute("type", "guid");
        accountNode.appendChild(doc.createTextNode(mAccountUID));

        Element splitNode = doc.createElement(GncXmlHelper.TAG_TRN_SPLIT);
        splitNode.appendChild(idNode);
        splitNode.appendChild(memoNode);
        splitNode.appendChild(stateNode);
        splitNode.appendChild(valueNode);
        splitNode.appendChild(quantityNode);
        splitNode.appendChild(accountNode);

        rootNode.appendChild(splitNode);
    }

    /**
     * Parses a split which is in the format "<amount>;<currency_code>;<account_uid>;<type>;<memo>".
     * The split input string is the same produced by the {@link Split#toCsv()} method
     *
     * @param splitString String containing formatted split
     * @return Split instance parsed from the string
     */
    @NonNull
    public static Split parseSplit(@NonNull String splitString) {
        String[] tokens = splitString.split(";");
        Money amount = new Money(tokens[0], tokens[1]);
        Split split = new Split(amount, tokens[2]);
        split.setType(TransactionType.valueOf(tokens[3]));
        if (tokens.length == 5){
            split.setMemo(tokens[4]);
        }
        return split;
    }
}
