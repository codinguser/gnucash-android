package org.gnucash.android.util;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.adapter.BooksDbAdapter;
import org.gnucash.android.export.Exporter;
import org.gnucash.android.importer.GncXmlImporter;
import org.gnucash.android.test.unit.testutil.ShadowCrashlytics;
import org.gnucash.android.test.unit.testutil.ShadowUserVoice;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.xml.sax.SAXException;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import static org.assertj.core.api.Assertions.assertThat;


@RunWith(RobolectricTestRunner.class) //package is required so that resources can be found in dev mode
@Config(sdk = 21, packageName = "org.gnucash.android",
        shadows = {ShadowCrashlytics.class, ShadowUserVoice.class})
public class BackupManagerTest {
    private BooksDbAdapter mBooksDbAdapter;

    @Before
    public void setUp() throws Exception {
        mBooksDbAdapter = BooksDbAdapter.getInstance();
        mBooksDbAdapter.deleteAllRecords();
        assertThat(mBooksDbAdapter.getRecordsCount()).isEqualTo(0);
    }

    @Test
    public void backupAllBooks() throws Exception {
        String activeBookUID = createNewBookWithDefaultAccounts();
        BookUtils.activateBook(activeBookUID);
        createNewBookWithDefaultAccounts();
        assertThat(mBooksDbAdapter.getRecordsCount()).isEqualTo(2);

        BackupManager.backupAllBooks();

        for (String bookUID : mBooksDbAdapter.getAllBookUIDs()) {
            File backupFolder = new File(Exporter.getBackupFolderPath(bookUID));
            assertThat(backupFolder.list().length).isEqualTo(1);
        }
    }

    /**
     * Creates a new database with default accounts
     * @return The book UID for the new database
     * @throws RuntimeException if the new books could not be created
     */
    private String createNewBookWithDefaultAccounts(){
        try {
            return GncXmlImporter.parse(GnuCashApplication.getAppContext().getResources().openRawResource(R.raw.default_accounts));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not create default accounts");
        }
    }
}