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
package org.gnucash.android.test.unit.export;

import org.gnucash.android.BuildConfig;
import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.BookDbHelper;
import org.gnucash.android.db.adapter.BooksDbAdapter;
import org.gnucash.android.export.ExportFormat;
import org.gnucash.android.export.ExportParams;
import org.gnucash.android.export.Exporter;
import org.gnucash.android.export.xml.GncXmlExporter;
import org.gnucash.android.importer.GncXmlImporter;
import org.gnucash.android.test.unit.db.AccountsDbAdapterTest;
import org.gnucash.android.test.unit.testutil.GnucashTestRunner;
import org.gnucash.android.test.unit.testutil.ShadowCrashlytics;
import org.gnucash.android.test.unit.testutil.ShadowUserVoice;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test backup and restore functionality
 */
@RunWith(GnucashTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21, packageName = "org.gnucash.android", shadows = {ShadowCrashlytics.class, ShadowUserVoice.class})
public class BackupTest {

    @Before
    public void setUp(){
        loadDefaultAccounts();
    }

    @Test
    public void shouldCreateBackup(){
        boolean backupResult = GncXmlExporter.createBackup();
        assertThat(backupResult).isTrue();
    }

    @Test
    public void shouldCreateBackupFileName() throws Exporter.ExporterException {
        Exporter exporter = new GncXmlExporter(new ExportParams(ExportFormat.XML));
        List<String> xmlFiles = exporter.generateExport();

        assertThat(xmlFiles).hasSize(1);
        assertThat(new File(xmlFiles.get(0)))
                .exists()
                .hasExtension(ExportFormat.XML.getExtension().substring(1));

    }

    /**
     * Loads the default accounts from file resource
     */
    private void loadDefaultAccounts(){
        try {
            String bookUID = GncXmlImporter.parse(GnuCashApplication.getAppContext().getResources().openRawResource(R.raw.default_accounts));
            BooksDbAdapter.getInstance().setActive(bookUID);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not create default accounts");
        }
    }
}
