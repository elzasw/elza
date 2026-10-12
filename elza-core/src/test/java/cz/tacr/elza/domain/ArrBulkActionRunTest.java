package cz.tacr.elza.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import cz.tacr.elza.bulkaction.generator.result.CopyActionResult;
import cz.tacr.elza.bulkaction.generator.result.Result;

/**
 * The result of a bulk action is stored as JSON; copied items reach java.time
 * values (#10127: an attachment in the copied introduction).
 */
public class ArrBulkActionRunTest {

    @Test
    void resultWithAttachmentAndDateRoundTrips() {
        ArrFile file = new ArrFile();
        file.setFileId(6268);
        file.setName("Příloha");
        file.setFileName("priloha.pdf");
        file.setCreatedAt(OffsetDateTime.of(2023, 11, 3, 15, 19, 1, 0, ZoneOffset.ofHours(1)));
        ArrDataFileRef fileRef = new ArrDataFileRef();
        fileRef.setFile(file);
        ArrDescItem fileItem = new ArrDescItem();
        fileItem.setData(fileRef);

        ArrDataDate date = new ArrDataDate();
        date.setValue(LocalDate.of(2026, 10, 12));
        ArrDescItem dateItem = new ArrDescItem();
        dateItem.setData(date);

        CopyActionResult copy = new CopyActionResult();
        copy.setItemType("ZP2015_INTRO");
        copy.setDataItems(List.of(fileItem, dateItem));
        Result result = new Result();
        result.getResults().add(copy);

        ArrBulkActionRun run = new ArrBulkActionRun();
        run.setResult(result);
        Result read = run.getResult();

        CopyActionResult readCopy = (CopyActionResult) read.getResults().get(0);
        ArrFile readFile = ((ArrDataFileRef) readCopy.getDataItems().get(0).getData()).getFile();
        assertEquals(6268, readFile.getFileId());
        assertEquals(file.getCreatedAt().toInstant(), readFile.getCreatedAt().toInstant());
        assertEquals(LocalDate.of(2026, 10, 12),
                ((ArrDataDate) readCopy.getDataItems().get(1).getData()).getValue());
    }
}
