/*
 * Copyright (c) 2013 - 2015 Ngewi Fet <ngewif@gmail.com>
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

import org.gnucash.android.ui.export.ExportFormFragment;

import java.sql.Timestamp;

/**
 * Encapsulation of the parameters used for exporting transactions.
 * The parameters are determined by the user in the export dialog and are then transmitted to the asynchronous task which
 * actually performs the export.
 * @see ExportFormFragment
 * @see ExportAsyncTask
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class ExportParams {
    /**
     * Options for the destination of the exported transctions file.
     * It could be stored on the {@link #SD_CARD} or exported through another program via {@link #SHARING}
     */
    public enum ExportTarget {SD_CARD, SHARING, DROPBOX, GOOGLE_DRIVE }

    /**
     * Format to use for the exported transactions
     * By default, the {@link ExportFormat#QIF} format is used
     */
    private ExportFormat mExportFormat      = ExportFormat.QIF;

    /**
     * All transactions created after this date will be exported
     */
    private Timestamp mExportStartTime = Timestamp.valueOf(Exporter.TIMESTAMP_ZERO);

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
     * Creates a new set of paramters and specifies the export format
     * @param format Format to use when exporting the transactions
     */
    public ExportParams(ExportFormat format){
        setExportFormat(format);
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
     * Return date from which to start exporting transactions
     * <p>Transactions created or modified after this timestamp will be exported</p>
     * @return Timestamp from which to export
     */
    public Timestamp getExportStartTime(){
        return mExportStartTime;
    }

    /**
     * Set the timestamp after which all transactions created/modified will be exported
     * @param exportStartTime Timestamp
     */
    public void setExportStartTime(Timestamp exportStartTime){
        this.mExportStartTime = exportStartTime;
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

    @Override
    public String toString() {
        return "Export all transactions created since " + mExportStartTime.toString()
                + " as "+ mExportFormat.name() + " to " + mExportTarget.name();
    }

    /**
     * Returns the export parameters formatted as CSV.
     * <p>The CSV format is: exportformat;exportTarget;shouldExportAllTransactions;shouldDeleteAllTransactions</p>
     * @return String containing CSV format of ExportParams
     */
    public String toCsv(){
        String separator = ";";

        return mExportFormat.name() + separator + mExportTarget.name() + separator
                + mExportStartTime.toString() + separator
                + Boolean.toString(mDeleteTransactionsAfterExport);
    }

    /**
     * Parses csv generated by {@link #toCsv()} to create
     * @param csvParams String containing csv of params
     * @return ExportParams from the csv
     */
    public static ExportParams parseCsv(String csvParams){
        String[] tokens = csvParams.split(";");
        ExportParams params = new ExportParams(ExportFormat.valueOf(tokens[0]));
        params.setExportTarget(ExportTarget.valueOf(tokens[1]));
        params.setExportStartTime(Timestamp.valueOf(tokens[2]));
        params.setDeleteTransactionsAfterExport(Boolean.parseBoolean(tokens[3]));

        return params;
    }
}
