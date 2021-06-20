package org.gnucash.android.export;

import android.database.sqlite.SQLiteDatabase;

import org.gnucash.android.export.csv.CsvAccountExporter;
import org.gnucash.android.export.csv.CsvTransactionsExporter;
import org.gnucash.android.export.ofx.OfxExporter;
import org.gnucash.android.export.qif.QifExporter;
import org.gnucash.android.export.xml.GncXmlExporter;

public class ExporterFactory {

    private static volatile ExporterFactory instance;

    //private constructor.
    private ExporterFactory(){

        //Prevent form the reflection api.
        if (instance != null){
            throw new RuntimeException("Use getInstance() method to get the single instance of this class.");
        }
    }

    public static ExporterFactory getInstance() {
        //Double check locking pattern
        if (instance == null) { //Check for the first time

            synchronized (ExporterFactory.class) {   //Check for the second time.
                //if there is no instance available... create new one
                if (instance == null) instance = new ExporterFactory();
            }
        }

        return instance;
    }


    /**
     * Returns an exporter corresponding to the user settings.
     * @return Object of one of {@link QifExporter}, {@link OfxExporter} or {@link GncXmlExporter}, {@link CsvAccountExporter} or {@link CsvTransactionsExporter}
     */
    public Exporter getExporter(final ExportParams exportParams, final SQLiteDatabase db) {
        switch (exportParams.getExportFormat()) {
            case QIF:
                return new QifExporter(exportParams, db);
            case OFX:
                return new OfxExporter(exportParams, db);
            case CSVA:
                return new CsvAccountExporter(exportParams, db);
            case CSVT:
                return new CsvTransactionsExporter(exportParams, db);
            case XML:
            default:
                return new GncXmlExporter(exportParams, db);
        }
    }
}