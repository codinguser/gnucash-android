package org.gnucash.android.export.log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class LogHelper {
    static final String header = "mod\ttrans_guid\tsplit_guid\ttime_now\tdate_entered\tdate_posted\tacc_guid\tacc_name\tnum\tdescription\tnotes\tmemo\taction\treconciled\tamount\tvalue\tdate_reconciled\n" +
            "-----------------";
    static final String start = "===== START";
    static final String end = "===== END";
    static final String separator = "\t";
    static final String lineEnd = "\n";
    static final String LOG_COMMIT = "C";
    static final String DATE_RECONCILE = "1970-01-01 08:00:00.000000 +08:00";
    static final SimpleDateFormat LOG_DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    public static String getLogFormattedTime(long milliseconds){
        Date date = new Date(milliseconds);
        String dateString = LOG_DATE_FORMATTER.format(date);
        TimeZone tz = Calendar.getInstance().getTimeZone();
        int offset = tz.getRawOffset();
        String sign = offset > 0 ?  "+" : "-";
        offset = offset > 0 ? offset : -offset;
        int hours   = (int) (( offset / (1000*60*60)) % 24);
        int minutes = (offset / (1000 * 60)) % 60;
        return dateString + ".000000 " + sign + String.format("%02d:%02d", hours, minutes);
    }
}
