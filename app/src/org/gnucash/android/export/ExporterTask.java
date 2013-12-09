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

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;
import org.gnucash.android.R;
import org.gnucash.android.export.ofx.OfxExporter;
import org.gnucash.android.export.qif.QifExporter;
import org.gnucash.android.ui.accounts.AccountsActivity;
import org.gnucash.android.ui.transactions.TransactionsDeleteConfirmationDialog;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Asynchronous task for exporting transactions.
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class ExporterTask extends AsyncTask<ExportParams, Void, Boolean> {
    /**
     * App context
     */
    private final Context mContext;

    private ProgressDialog mProgressDialog;

    /**
     * Log tag
     */
    public static final String TAG = "ExporterTask";

    /**
     * Export parameters
     */
    private ExportParams mExportParams;

    public ExporterTask(Context context){
        this.mContext = context;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mProgressDialog = new ProgressDialog(mContext);
        mProgressDialog.setTitle(R.string.title_progress_exporting_transactions);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.show();
    }

    /**
     * Generates the appropriate exported transactions file for the given parameters
     * @param params Export parameters
     * @return <code>true</code> if export was successful, <code>false</code> otherwise
     */
    @Override
    protected Boolean doInBackground(ExportParams... params) {
        mExportParams = params[0];
        boolean exportAllTransactions = mExportParams.shouldExportAllTransactions();
        try {
            switch (mExportParams.getExportFormat()) {
                case QIF: {
                    QifExporter qifExporter = new QifExporter(mContext, exportAllTransactions);
                    String qif = qifExporter.generateQIF();

                    writeQifExternalStorage(qif);
                }
                return true;

                case OFX: {
                    Document document = exportOfx(exportAllTransactions);
                    writeOfxToExternalStorage(document);
                }
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
            Toast.makeText(mContext, R.string.error_exporting,
                    Toast.LENGTH_LONG).show();
        };
        return false;
    }

    /**
     * Transmits the exported transactions to the designated location, either SD card or third-party application
     * @param exportResult Result of background export execution
     */
    @Override
    protected void onPostExecute(Boolean exportResult) {
        mProgressDialog.dismiss();

        if (!exportResult){
            Toast.makeText(mContext,
                    mContext.getString(R.string.toast_error_exporting),
                    Toast.LENGTH_LONG).show();
            return;
        }

        switch (mExportParams.getExportTarget()) {
            case SHARING:
                shareFile(mExportParams.getTargetFilepath());
                break;

            case SD_CARD:
                File src = new File(mExportParams.getTargetFilepath());
                new File(Environment.getExternalStorageDirectory() + "/gnucash/").mkdirs();
                File dst = new File(Environment.getExternalStorageDirectory()
                        + "/gnucash/" + ExportDialogFragment.buildExportFilename(mExportParams.getExportFormat()));

                try {
                    copyFile(src, dst);
                } catch (IOException e) {
                    Toast.makeText(mContext,
                            mContext.getString(R.string.toast_error_exporting_ofx) + dst.getAbsolutePath(),
                            Toast.LENGTH_LONG).show();
                    Log.e(TAG, e.getMessage());
                    break;
                }

                //file already exists, just let the user know
                Toast.makeText(mContext,
                        mContext.getString(R.string.toast_ofx_exported_to) + dst.getAbsolutePath(),
                        Toast.LENGTH_LONG).show();
                break;

            default:
                break;
        }

        if (mExportParams.shouldDeleteTransactionsAfterExport()){
            android.support.v4.app.FragmentManager fragmentManager = ((FragmentActivity)mContext).getSupportFragmentManager();
            Fragment currentFragment = fragmentManager
                    .findFragmentByTag(AccountsActivity.FRAGMENT_ACCOUNTS_LIST);
            TransactionsDeleteConfirmationDialog alertFragment =
                    TransactionsDeleteConfirmationDialog.newInstance(R.string.title_confirm_delete, 0);
            alertFragment.setTargetFragment(currentFragment, 0);

            alertFragment.show(fragmentManager, "transactions_delete_confirmation_dialog");
        }

    }


    /**
     * Exports transactions in the database to the OFX format.
     * The accounts are written to a DOM document and returned
     * @param exportAll Flag to export all transactions or only the new ones since last export
     * @return DOM {@link Document} containing the OFX file information
     * @throws javax.xml.parsers.ParserConfigurationException
     */
    protected Document exportOfx(boolean exportAll) throws ParserConfigurationException {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory
                .newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

        Document document = docBuilder.newDocument();
        Element root = document.createElement("OFX");

        ProcessingInstruction pi = document.createProcessingInstruction("OFX", OfxExporter.OFX_HEADER);
        document.appendChild(pi);
        document.appendChild(root);

        OfxExporter exporter = new OfxExporter(mContext, exportAll);
        exporter.toOfx(document, root);

        return document;
    }

    /**
     * Writes out the String containing the exported transaction in QIF format to disk
     * @param qif String containing exported transactions
     * @throws IOException
     */
    private void writeQifExternalStorage(String qif) throws IOException {
        File file = new File(mExportParams.getTargetFilepath());

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
        writer.write(qif);

        writer.flush();
    }

    /**
     * Writes the OFX document <code>doc</code> to external storage
     * @param doc Document containing OFX file data
     * @throws IOException if file could not be saved
     */
    private void writeOfxToExternalStorage(Document doc) throws IOException{
        File file = new File(mExportParams.getTargetFilepath());

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
        boolean useXmlHeader = PreferenceManager.getDefaultSharedPreferences(mContext)
                .getBoolean(mContext.getString(R.string.key_xml_ofx_header), false);

        //if we want SGML OFX headers, write first to string and then prepend header
        if (useXmlHeader){
            write(doc, writer, false);
        } else {
            Node ofxNode = doc.getElementsByTagName("OFX").item(0);
            StringWriter stringWriter = new StringWriter();
            write(ofxNode, stringWriter, true);

            StringBuffer stringBuffer = new StringBuffer(OfxExporter.OFX_SGML_HEADER);
            stringBuffer.append('\n');
            writer.write(stringBuffer.toString() + stringWriter.toString());
        }

        writer.flush();
        writer.close();
    }

    /**
     * Starts an intent chooser to allow the user to select an activity to receive
     * the exported OFX file
     * @param path String path to the file on disk
     */
    private void shareFile(String path){
        String defaultEmail = PreferenceManager.getDefaultSharedPreferences(mContext)
                .getString(mContext.getString(R.string.key_default_export_email), null);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/xml");
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + path));
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, mContext.getString(R.string.title_export_email));
        if (defaultEmail != null && defaultEmail.trim().length() > 0){
            shareIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{defaultEmail});
        }
        SimpleDateFormat formatter = (SimpleDateFormat) SimpleDateFormat.getDateTimeInstance();

        shareIntent.putExtra(Intent.EXTRA_TEXT, mContext.getString(R.string.description_export_email)
                + " " + formatter.format(new Date(System.currentTimeMillis())));
        mContext.startActivity(Intent.createChooser(shareIntent, mContext.getString(R.string.title_share_ofx_with)));
    }

    /**
     * Copies a file from <code>src</code> to <code>dst</code>
     * @param src Absolute path to the source file
     * @param dst Absolute path to the destination file
     * @throws IOException if the file could not be copied
     */
    public static void copyFile(File src, File dst) throws IOException
    {
        //TODO: Make this asynchronous at some time, t in the future.
        FileChannel inChannel = new FileInputStream(src).getChannel();
        FileChannel outChannel = new FileOutputStream(dst).getChannel();
        try
        {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        }
        finally
        {
            if (inChannel != null)
                inChannel.close();
            if (outChannel != null)
                outChannel.close();
        }
    }

    /**
     * Writes out the document held in <code>node</code> to <code>outputWriter</code>
     * @param node {@link Node} containing the OFX document structure. Usually the parent node
     * @param outputWriter {@link Writer} to use in writing the file to stream
     * @param omitXmlDeclaration Flag which causes the XML declaration to be omitted
     */
    public void write(Node node, Writer outputWriter, boolean omitXmlDeclaration){
        try {
            TransformerFactory transformerFactory = TransformerFactory
                    .newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(node);
            StreamResult result = new StreamResult(outputWriter);

            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            if (omitXmlDeclaration) {
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            }

            transformer.transform(source, result);
        } catch (TransformerConfigurationException txconfigException) {
            txconfigException.printStackTrace();
        } catch (TransformerException tfException) {
            tfException.printStackTrace();
        }
    }
}
