package cz.tacr.elza.utils;

import java.nio.charset.Charset;
import java.time.format.DateTimeFormatter;

import org.apache.commons.csv.CSVFormat;

public class CsvUtils {

	public static final String CSV_TYPE_NAME = "CSV"; 
	
    /**
     * CSV konfigurace pro CZ Excel
     *
     * MS Excel přidává mezery na konec řádků, proto se okolní mezery při čtení ignorují.
     * Bez toho selže i dohledání sloupce podle hlavičky (viz ArrIOService.csvImport).
     */
    public static final CSVFormat CSV_EXCEL_FORMAT = CSVFormat.EXCEL.builder()
    		.setDelimiter(';')
            .setQuote('"')
            .setIgnoreSurroundingSpaces(true)
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
