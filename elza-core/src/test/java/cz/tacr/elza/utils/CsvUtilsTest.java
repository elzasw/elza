package cz.tacr.elza.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;

/**
 * MS Excel leaves a space at the end of a row when a CSV is edited and saved again. The shared
 * Excel format therefore ignores the spaces surrounding a value - see #10003.
 */
public class CsvUtilsTest {

    private static List<CSVRecord> parseWithHeader(String csv) throws IOException {
        return CsvUtils.CSV_EXCEL_FORMAT.withFirstRecordAsHeader().parse(new StringReader(csv)).getRecords();
    }

    @Test
    void trailingSpaceInHeaderDoesNotBreakTheColumnLookup() throws IOException {
        // the space after COUNT is what Excel adds; the lookup is by column code, so without
        // ignoreSurroundingSpaces the header reads as "COUNT " and get("COUNT") throws
        List<CSVRecord> records = parseWithHeader("NAME;COUNT \nklic1;1\n");

        assertEquals(1, records.size());
        assertEquals("klic1", records.get(0).get("NAME"));
        assertEquals("1", records.get(0).get("COUNT"));
    }

    @Test
    void spacesSurroundingAValueAreIgnored() throws IOException {
        List<CSVRecord> records = parseWithHeader("NAME;COUNT\nklic1;1 \n klic2 ;2\n");

        assertEquals(2, records.size());
        assertEquals("1", records.get(0).get("COUNT"));
        assertEquals("klic2", records.get(1).get("NAME"));
    }

    @Test
    void spacesInsideAQuotedValueAreKept() throws IOException {
        // only the surrounding spaces go - a value the user deliberately padded inside quotes
        // still arrives as written
        List<CSVRecord> records = parseWithHeader("NAME;COUNT\n\" klic1 \";1\n");

        assertEquals(" klic1 ", records.get(0).get("NAME"));
    }
}
