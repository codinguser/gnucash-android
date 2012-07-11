/*
 * Written By: Ngewi Fet <ngewif@gmail.com>
 * Copyright (c) 2012 Ngewi Fet
 *
 * This file is part of Gnucash for Android
 * 
 * Gnucash for Android is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, contact:
 *
 * Free Software Foundation           Voice:  +1-617-542-5942
 * 51 Franklin Street, Fifth Floor    Fax:    +1-617-542-2652
 * Boston, MA  02110-1301,  USA       gnu@gnu.org
 */

package org.gnucash.android.ui.accounts;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;

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

import org.gnucash.android.R;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.ui.MainActivity;
import org.gnucash.android.util.OfxFormatter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ProcessingInstruction;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.Toast;

public class ExportDialogFragment extends DialogFragment {
		
	Spinner mDestinationSpinner;
	CheckBox mExportAllCheckBox;
	CheckBox mDeleteAllCheckBox;
	Button mSaveButton;
	Button mCancelButton;
	
	String mFilePath;
	
	protected class ExportClickListener implements View.OnClickListener {

		@Override
		public void onClick(View v) {
			boolean exportAll = mExportAllCheckBox.isChecked();
			Document document = null;				
			try {
				document = exportOfx(exportAll);
				writeToExternalStorage(document);
			} catch (Exception e) {
				Log.e(getTag(), e.getMessage());
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
				File dst = new File(Environment.getExternalStorageDirectory() + "/" + buildExportFilename());
				try {
					copyFile(src, dst);
				} catch (IOException e) {
					Toast.makeText(getActivity(), 
							"Could not write OFX file to :\n" + dst.getAbsolutePath(), 
							Toast.LENGTH_LONG).show();		
					Log.e(getTag(), e.getMessage());
					break;
				}
				
				//file already exists, just let the user know
				Toast.makeText(getActivity(), 
						"OFX file exported to:\n" + dst.getAbsolutePath(), 
						Toast.LENGTH_LONG).show();					
				break;
				
			default:
				break;
			}
			
			if (mDeleteAllCheckBox.isChecked()){
				TransactionsDbAdapter trxnAdapter = new TransactionsDbAdapter(getActivity());
				trxnAdapter.deleteAllTransactions();
				trxnAdapter.close();
			}
			
			Fragment f = getActivity()
			.getSupportFragmentManager()
			.findFragmentByTag(MainActivity.FRAGMENT_ACCOUNTS_LIST);
		
			((AccountsListFragment)f).refreshList();
			dismiss();
		}
		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.dialog_export_ofx, container, false);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {		
		super.onActivityCreated(savedInstanceState);
		mFilePath = getActivity().getExternalFilesDir(null) + "/" + buildExportFilename();
		getDialog().setTitle(R.string.export_ofx);
		bindViews();
	}

	private void bindViews(){
		View v = getView();
		mDestinationSpinner = (Spinner) v.findViewById(R.id.spinner_export_destination);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
		        R.array.export_destinations, android.R.layout.simple_spinner_item);		
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);		
		mDestinationSpinner.setAdapter(adapter);
		
		mExportAllCheckBox = (CheckBox) v.findViewById(R.id.checkbox_export_all);
		mDeleteAllCheckBox = (CheckBox) v.findViewById(R.id.checkbox_post_export_delete);
		
		mSaveButton = (Button) v.findViewById(R.id.btn_save);
		mCancelButton = (Button) v.findViewById(R.id.btn_cancel);
		
		mCancelButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {				
				dismiss();
			}
		});
		
		mSaveButton.setOnClickListener(new ExportClickListener());
	}
	
	private void writeToExternalStorage(Document doc) throws IOException{
		File file = new File(mFilePath);
		
		FileWriter writer = new FileWriter(file);
		write(doc, writer);
		
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		//TODO: fix the exception which is thrown on return
		if (resultCode == Activity.RESULT_OK){
			//uploading or emailing has finished. clean up now.
			File file = new File(mFilePath);
			file.delete();
		}
	}
	
	private void shareFile(String path){
		Intent shareIntent = new Intent(Intent.ACTION_SEND);
		shareIntent.setType("multipart/x-ofx");
		shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://"+ path));
		shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Gnucash OFX export");
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd HH:mm");
		shareIntent.putExtra(Intent.EXTRA_TEXT, "Gnucash accounts export from " 
							+ formatter.format(new Date(System.currentTimeMillis())));
		startActivity(Intent.createChooser(shareIntent, "Sharing OFX file..."));	
	}
	
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
	
	public static String buildExportFilename(){
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmm");
		String filename = formatter.format(
				new Date(System.currentTimeMillis())) 
				+ "_gnucash_all.ofx";
		return filename;
	}
	
	protected Document exportOfx(boolean exportAll) throws ParserConfigurationException{		
		DocumentBuilderFactory docFactory = DocumentBuilderFactory
				.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

		Document document = docBuilder.newDocument();
		Element root = document.createElement("OFX");
		
		ProcessingInstruction pi = document.createProcessingInstruction("OFX", "OFXHEADER=\"200\" VERSION=\"211\" SECURITY=\"NONE\" OLDFILEUID=\"NONE\" NEWFILEUID=\"NONE\"");
		document.appendChild(pi);		
		document.appendChild(root);
		
		OfxFormatter exporter = new OfxFormatter(getActivity(), exportAll);
		exporter.toXml(document, root);
		
		return document;
	}
	
	public void write(Document document, Writer outputWriter){
		try {
			TransformerFactory transformerFactory = TransformerFactory
					.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(document);
			StreamResult result = new StreamResult(outputWriter);
			
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			
			transformer.transform(source, result);
		} catch (TransformerConfigurationException txconfigException) {
			txconfigException.printStackTrace();
		} catch (TransformerException tfException) {
			tfException.printStackTrace();
		}
	}
}

