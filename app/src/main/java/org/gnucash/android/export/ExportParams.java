/*
 * Copyright (c) 2013 Ngewi Fet <ngewif@gmail.com>
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

package org.gnucash.android.export;

/**
 * Encapsulation of the parameters used for exporting transactions.
 * The parameters are determined by the user in the export dialog and are then transmitted to the asynchronous task which
 * actually performs the export.
 * @see org.gnucash.android.export.ExportDialogFragment
 * @see ExporterAsyncTask
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class ExportParams {
    /**
     * Options for the destination of the exported transctions file.
     * It could be stored on the {@link #SD_CARD} or exported through another program via {@link #SHARING}
     */
    public enum ExportTarget {SD_CARD, SHARING};

    /**
     * Format to use for the exported transactions
     * By default, the {@link ExportFormat#QIF} format is used
     */
    private ExportFormat mExportFormat      = ExportFormat.QIF;

    /**
     * Flag to determine if all transactions (including previously exported ones) should be exported
     * By default only new transactions since the last export will be exported.
     */
    private boolean mExportAllTransactions  = false;

    /**
     * Flag to determine if all transactions should be deleted after exporting is complete
     * By default no transactions are deleted
     */
    private boolean mDeleteTransactionsAfterExport = false;

    /**
     * Destination for the exported transactions
     */
    private ExportTarget mExportTarget      = ExportTarget.SHARING;

    /**
     * File path for the internal saving of transactions before determining export destination.
     */
    private String mTargetFilepath;

    /**
     * Creates a new set of paramters and specifies the export format
     * @param format Format to use when exporting the transactions
     */
    public ExportParams(ExportFormat format){
        mExportFormat = format;
    }

    /**
     * Return the format used for exporting
     * @return {@link ExportFormat}
     */
    public ExportFormat getExportFormat() {
        return mExportFormat;
    }

    /**
     * Set the export format
     * @param exportFormat {@link ExportFormat}
     */
    public void setExportFormat(ExportFormat exportFormat) {
        this.mExportFormat = exportFormat;
    }

    /**
     * Returns flag whether all transactions should be exported, or only new ones since last export
     * @return <code>true</code> if all transactions should be exported, <code>false</code> otherwise
     */
    public boolean shouldExportAllTransactions() {
        return mExportAllTransactions;
    }

    /**
     * Sets flag for exporting all transactions or only new transactions since last export
     * @param exportAll Boolean flag
     */
    public void setExportAllTransactions(boolean exportAll) {
        this.mExportAllTransactions = exportAll;
    }

    /**
     * Returns flag whether transactions should be deleted after export
     * @return <code>true</code> if all transactions will be deleted, <code>false</code> otherwise
     */
    public boolean shouldDeleteTransactionsAfterExport() {
        return mDeleteTransactionsAfterExport;
    }

    /**
     * Set flag to delete transactions after exporting is complete
     * @param deleteTransactions SEt to <code>true</code> if transactions should be deleted, false if not
     */
    public void setDeleteTransactionsAfterExport(boolean deleteTransactions) {
        this.mDeleteTransactionsAfterExport = deleteTransactions;
    }

    /**
     * Get the target for the exported file
     * @return {@link org.gnucash.android.export.ExportParams.ExportTarget}
     */
    public ExportTarget getExportTarget() {
        return mExportTarget;
    }

    /**
     * Set the target for the exported transactions
     * @param mExportTarget Target for exported transactions
     */
    public void setExportTarget(ExportTarget mExportTarget) {
        this.mExportTarget = mExportTarget;
    }

    /**
     * Returns the internal target file path for the exported transactions.
     * This file path is not accessible outside the context of the application
     * @return String path to exported transactions
     */
    public String getTargetFilepath() {
        return mTargetFilepath;
    }

    /**
     * Sets target file path for transactions in private application storage
     * @param mTargetFilepath String path to file
     */
    public void setTargetFilepath(String mTargetFilepath) {
        this.mTargetFilepath = mTargetFilepath;
    }

}
