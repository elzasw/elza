package cz.tacr.elza.utils;

import java.nio.charset.Charset;
import java.time.format.DateTimeFormatter;

import org.apache.commons.csv.CSVFormat;

public class CsvUtils {

    /**
     * CSV konfigurace pro CZ Excel
     */
    public static final CSVFormat CSV_EXCEL_FORMAT = CSVFormat.EXCEL.builder()
    		.setDelimiter(';')
            .setQuote('"')
            .build();

    /**
     * Kódování pro CSV soubory - CP1250
     */
    public static final String CSV_EXCEL_ENCODING = "windows-1250";

    /**
     * Kódování pro CSV soubory - CP1250
     */
    public static final Charset CSV_EXCEL_CHARSET = Charset.forName(CSV_EXCEL_ENCODING);

    /**
     * Formatování datumu a času (s přesností na vteřiny) při exportu do CZ Excelu
     */
    public static final DateTimeFormatter CVS_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("d.M.u H:mm:ss");
}
