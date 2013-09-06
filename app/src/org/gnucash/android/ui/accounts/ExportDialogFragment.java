/*
 * Copyright (c) 2012 Ngewi Fet <ngewif@gmail.com>
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

package org.gnucash.android.ui.accounts;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import android.widget.*;
import org.gnucash.android.R;
import org.gnucash.android.export.qif.QifExporter;
import org.gnucash.android.ui.transactions.TransactionsDeleteConfirmationDialog;
import org.gnucash.android.util.OfxFormatter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Dialog fragment for exporting account information as OFX files.
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class ExportDialogFragment extends DialogFragment {
		
	/**
	 * Spinner for selecting destination for the exported file.
	 * The destination could either be SD card, or another application which
	 * accepts files, like Google Drive.
	 */
	Spinner mDestinationSpinner;
	
	/**
	 * Checkbox indicating that all transactions should be exported,
	 * regardless of whether they have been exported previously or not
	 */
	CheckBox mExportAllCheckBox;
	
	/**
	 * Checkbox for deleting all transactions after exporting them
	 */
	CheckBox mDeleteAllCheckBox;
	
	/**
	 * Save button for saving the exported files
	 */
	Button mSaveButton;
	
	/**
	 * Cancels the export dialog
	 */
	Button mCancelButton;
	
	/**
	 * File path for saving the OFX files
	 */
	String mFilePath;
	
	/**
	 * Tag for logging
	 */
	private static final String TAG = "ExportDialogFragment";

    public enum ExportFormat { QIF, OFX};

    private ExportFormat mExportFormat = ExportFormat.QIF;

	/**
	 * Click listener for positive button in the dialog.
	 * @author Ngewi Fet <ngewif@gmail.com>
	 */
	protected class ExportClickListener implements View.OnClickListener {

		@Override
		public void onClick(View v) {
            boolean exportAll = mExportAllCheckBox.isChecked();
            try {
                switch (mExportFormat) {
                    case QIF: {
                        QifExporter qifExporter = new QifExporter(getActivity(), exportAll);
                        String qif = qifExporter.generateQIF();

                        writeQifExternalStorage(qif);
                    }
                    break;

                    case OFX: {
                        Document document = exportOfx(exportAll);
                        writeOfxToExternalStorage(document);
                    }
                    break;
                }
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
                Toast.makeText(getActivity(), R.string.error_exporting,
                        Toast.LENGTH_LONG).show();
                dismiss();
                return;
            }


            int position = mDestinationSpinner.getSelectedItemPosition();
			switch (position) {
			case 0:					
				shareFile(mFilePath);				
				break;

			case 1:				
				File src = new File(mFilePath);
				new File(Environment.getExternalStorageDirectory() + "/gnucash/").mkdirs();
				File dst = new File(Environment.getExternalStorageDirectory() + "/gnucash/" + buildExportFilename(mExportFormat));
				
				try {
					copyFile(src, dst);
				} catch (IOException e) {
					Toast.makeText(getActivity(), 
							getString(R.string.toast_error_exporting_ofx) + dst.getAbsolutePath(), 
							Toast.LENGTH_LONG).show();		
					Log.e(TAG, e.getMessage());
					break;
				}
				
				//file already exists, just let the user know
				Toast.makeText(getActivity(), 
						getString(R.string.toast_ofx_exported_to) + dst.getAbsolutePath(), 
						Toast.LENGTH_LONG).show();					
				break;
				
			default:
				break;
			}
			
			if (mDeleteAllCheckBox.isChecked()){
				Fragment currentFragment = getActivity().getSupportFragmentManager()
						.findFragmentByTag(AccountsActivity.FRAGMENT_ACCOUNTS_LIST);
				TransactionsDeleteConfirmationDialog alertFragment = 
						TransactionsDeleteConfirmationDialog.newInstance(R.string.title_confirm_delete, 0);
				alertFragment.setTargetFragment(currentFragment, 0);
				alertFragment.show(getActivity().getSupportFragmentManager(), "transactions_delete_confirmation_dialog");
			}
			
			dismiss();
		}
		
	}

    public void onRadioButtonClicked(View view){
        switch (view.getId()){
            case R.id.radio_ofx_format:
                mExportFormat = ExportFormat.OFX;
                break;
            case R.id.radio_qif_format:
                mExportFormat = ExportFormat.QIF;
        }
        mFilePath = buildExportFilename(mExportFormat);
        return;
    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.dialog_export_ofx, container, false);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {		
		super.onActivityCreated(savedInstanceState);
		mFilePath = getActivity().getExternalFilesDir(null) + "/" + buildExportFilename(mExportFormat);
		getDialog().setTitle(R.string.menu_export_ofx);
		bindViews();
	}

	/**
	 * Collects references to the UI elements and binds click listeners
	 */
	private void bindViews(){		
		View v = getView();
		mDestinationSpinner = (Spinner) v.findViewById(R.id.spinner_export_destination);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
		        R.array.export_destinations, android.R.layout.simple_spinner_item);		
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);		
		mDestinationSpinner.setAdapter(adapter);
		
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
		mExportAllCheckBox = (CheckBox) v.findViewById(R.id.checkbox_export_all);
		mExportAllCheckBox.setChecked(sharedPrefs.getBoolean(getString(R.string.key_export_all_transactions), false));
		
		mDeleteAllCheckBox = (CheckBox) v.findViewById(R.id.checkbox_post_export_delete);
		mDeleteAllCheckBox.setChecked(sharedPrefs.getBoolean(getString(R.string.key_delete_transactions_after_export), false));
		
		mSaveButton = (Button) v.findViewById(R.id.btn_save);
		mSaveButton.setText(R.string.btn_export);
		mCancelButton = (Button) v.findViewById(R.id.btn_cancel);
		
		mCancelButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {				
				dismiss();
			}
		});
		
		mSaveButton.setOnClickListener(new ExportClickListener());

        String defaultExportFormat = sharedPrefs.getString(getString(R.string.key_default_export_format), ExportFormat.QIF.name());
        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onRadioButtonClicked(view);
            }
        };

        RadioButton ofxRadioButton = (RadioButton) v.findViewById(R.id.radio_ofx_format);
        ofxRadioButton.setChecked(defaultExportFormat.equalsIgnoreCase(ExportFormat.OFX.name()));
        ofxRadioButton.setOnClickListener(clickListener);

        RadioButton qifRadioButton = (RadioButton) v.findViewById(R.id.radio_qif_format);
        qifRadioButton.setChecked(defaultExportFormat.equalsIgnoreCase(ExportFormat.QIF.name()));
        qifRadioButton.setOnClickListener(clickListener);
	}

    private void writeQifExternalStorage(String qif) throws IOException {
        File file = new File(mFilePath);

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
		File file = new File(mFilePath);
		
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
		boolean useXmlHeader = PreferenceManager.getDefaultSharedPreferences(getActivity())
				.getBoolean(getString(R.string.key_xml_ofx_header), false);

		//if we want SGML OFX headers, write first to string and then prepend header
		if (useXmlHeader){
			write(doc, writer, false);
		} else {			
			Node ofxNode = doc.getElementsByTagName("OFX").item(0);
			StringWriter stringWriter = new StringWriter();
			write(ofxNode, stringWriter, true);
			
			StringBuffer stringBuffer = new StringBuffer(OfxFormatter.OFX_SGML_HEADER);
			stringBuffer.append('\n');
			writer.write(stringBuffer.toString() + stringWriter.toString());
		}
		
		writer.flush();
		writer.close();
	}
	
	/**
	 * Callback for when the activity chooser dialog is completed
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		//TODO: fix the exception which is thrown on return
		if (resultCode == Activity.RESULT_OK){
			//uploading or emailing has finished. clean up now.
			File file = new File(mFilePath);
			file.delete();
		}
	}
	
	/**
	 * Starts an intent chooser to allow the user to select an activity to receive
	 * the exported OFX file
	 * @param path String path to the file on disk
	 */
	private void shareFile(String path){
		String defaultEmail = PreferenceManager.getDefaultSharedPreferences(getActivity())
												.getString(getString(R.string.key_default_export_email), null);
		Intent shareIntent = new Intent(Intent.ACTION_SEND);
		shareIntent.setType("application/xml");
		shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://"+ path));
		shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.title_export_email));
		if (defaultEmail != null && defaultEmail.trim().length() > 0){
			shareIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{defaultEmail});
		}			
		SimpleDateFormat formatter = (SimpleDateFormat) SimpleDateFormat.getDateTimeInstance();
		
		shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.description_export_email) 
							+ " " + formatter.format(new Date(System.currentTimeMillis())));
		startActivity(Intent.createChooser(shareIntent, getString(R.string.title_share_ofx_with)));	
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
	 * Builds a file name based on the current time stamp for the exported file
	 * @return String containing the file name
	 */
	public static String buildExportFilename(ExportFormat format){
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
		String filename = formatter.format(
				new Date(System.currentTimeMillis())) 
				+ "_gnucash_all";
        switch (format) {
            case QIF:
                filename += ".qif";
                break;
            case OFX:
                filename += ".ofx";
                break;
        }
		return filename;
	}
	
	/**
	 * Exports transactions in the database to the OFX format.
	 * The accounts are written to a DOM document and returned
	 * @param exportAll Flag to export all transactions or only the new ones since last export
	 * @return DOM {@link Document} containing the OFX file information
	 * @throws ParserConfigurationException
	 */
	protected Document exportOfx(boolean exportAll) throws ParserConfigurationException{		
		DocumentBuilderFactory docFactory = DocumentBuilderFactory
				.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

		Document document = docBuilder.newDocument();
		Element root = document.createElement("OFX");
		
		ProcessingInstruction pi = document.createProcessingInstruction("OFX", OfxFormatter.OFX_HEADER);
		document.appendChild(pi);		
		document.appendChild(root);
		
		OfxFormatter exporter = new OfxFormatter(getActivity(), exportAll);
		exporter.toOfx(document, root);
		
		return document;
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

