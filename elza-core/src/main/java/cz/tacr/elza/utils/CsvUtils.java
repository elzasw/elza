package cz.tacr.elza.utils;

import java.nio.charset.Charset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.apache.commons.csv.CSVFormat;

public class CsvUtils {

    public static final Locale LOCALE_CZECH = new Locale("cs", "CZ");

    /**
     * CSV konfigurace pro CZ Excel.
     */
    public static final CSVFormat CSV_EXCEL_FORMAT = CSVFormat.EXCEL
            .withDelimiter(';')
            .withQuote('"');

    /**
     * Kódování pro CSV soubory.
     */
    public static final String CSV_EXCEL_ENCODING = "windows-1250";

    /**
     * Kódování pro CSV soubory.
     */
    public static final Charset CSV_EXCEL_CHARSET = Charset.forName(CSV_EXCEL_ENCODING);

    public static final DateTimeFormatter CVS_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("d.M.u H:mm:ss");
}
