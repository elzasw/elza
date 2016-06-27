package cz.tacr.elza.service;

import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrItemData;
import cz.tacr.elza.domain.ArrItemJsonTable;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ArrOutputItem;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.factory.DescItemFactory;
import cz.tacr.elza.domain.table.ElzaColumn;
import cz.tacr.elza.domain.table.ElzaRow;
import cz.tacr.elza.domain.table.ElzaTable;
import cz.tacr.elza.repository.DataTypeRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Serviska pro import/export dat pro ArrItem.
 *
 * @author Martin Šlapa
 * @since 27.06.2016
 */
@Service
public class ArrIOService {

    @Autowired
    private DescItemFactory descItemFactory;

    @Autowired
    private ItemTypeRepository itemTypeRepository;

    @Autowired
    private DataTypeRepository dataTypeRepository;

    @Autowired
    private DescriptionItemService descriptionItemService;

    @Autowired
    private OutputService outputService;

    /** CSV konfigurace pro CZ Excel. */
    public static final CSVFormat CSV_EXCEL_FORMAT = CSVFormat.DEFAULT
            .withIgnoreEmptyLines(false)
            .withAllowMissingColumnNames()
            .withDelimiter(';')
            .withQuote('"');
    /** Kódování pro CSV soubory. */
    public static final String CSV_EXCEL_ENCODING = "cp1250";


    /**
     * Export dat tabulky do csv formátu, který bude zapsán do streamu.
     * @param item desc item
     * @param os výstupní stream
     */
    public <T extends ArrItem> void csvExport(final T item, final OutputStream os) throws IOException {
        RulItemType descItemType = item.getItemType();
        List<String> columNames = descItemType.getColumnsDefinition()
                .stream()
                .map(ElzaColumn::getCode)
                .collect(Collectors.toList());

        try (
                OutputStreamWriter out = new OutputStreamWriter(os, CSV_EXCEL_ENCODING);
                CSVPrinter csvp = CSV_EXCEL_FORMAT.withHeader(columNames.toArray(new String[columNames.size()])).print(out);
        ) {
            ElzaTable table = ((ArrItemJsonTable) item.getItem()).getValue();

            for (ElzaRow elzaRow : table.getRows()) {
                Map<String, String> values = elzaRow.getValues();
                List<Object> rowValues = descItemType.getColumnsDefinition()
                        .stream()
                        .map(elzaColumn -> values.get(elzaColumn.getCode()))
                        .collect(Collectors.toList());
                csvp.printRecord(rowValues);
            }
        }
    }


    /**
     * Import csv ze stromu do konkrétní hodnoty desc item, která bude nahrazena.
     * @param fundVersionId verze souboru
     * @param nodeId id uzlu
     * @param nodeVersion verze uzlu
     * @param descItemTypeId id typu atributu
     * @param is stream s csv souborem
     * @return vytvořená položka
     */
    public ArrDescItem csvDescImport(final Integer fundVersionId,
                                     final Integer nodeId,
                                     final Integer nodeVersion,
                                     final Integer descItemTypeId,
                                     final InputStream is) throws IOException {

        try (
                Reader in = new InputStreamReader(is, CSV_EXCEL_ENCODING);
        ) {
            ArrDescItem<ArrItemJsonTable> descItem = csvImport(descItemTypeId, in, ArrDescItem.class);

            // Vyvoření nové s naimportovanými daty
            return descriptionItemService.createDescriptionItem(descItem, nodeId, nodeVersion, fundVersionId);
        }
    }

    /**
     * Import csv do output item, která bude nahrazena.
     * @param fundVersionId verze souboru
     * @param outputDefinitionId id outputu
     * @param outputDefinitionVersion verze outputu
     * @param descItemTypeId id typu atributu
     * @param is stream s csv souborem
     * @return vytvořená položka
     */
    public ArrOutputItem csvOutputImport(final Integer fundVersionId,
                                   final Integer outputDefinitionId,
                                   final Integer outputDefinitionVersion,
                                   final Integer descItemTypeId,
                                   final InputStream is) throws IOException {

        try (
                Reader in = new InputStreamReader(is, CSV_EXCEL_ENCODING);
        ) {
            ArrOutputItem<ArrItemJsonTable> outputItem = csvImport(descItemTypeId, in, ArrOutputItem.class);

            // Vyvoření nové s naimportovanými daty
            return outputService.createOutputItem(outputItem, outputDefinitionId, outputDefinitionVersion, fundVersionId);
        }
    }

    /**
     * Import csv.
     *
     * @param descItemTypeId id typu atributu
     * @param in stream s csv souborem
     * @param clazz třída, pro kterou importuju
     * @return vytvořená položka
     */
    private <T extends ArrItem<ArrItemJsonTable>> T csvImport(final Integer descItemTypeId, final Reader in, final Class<T> clazz) throws IOException {
        // Vytvoření instance nové položky
        RulItemType descItemType = itemTypeRepository.getOneCheckExist(descItemTypeId);

        ArrItemJsonTable jsonTable = (ArrItemJsonTable) descItemFactory.createItemByType(dataTypeRepository.findByCode("JSON_TABLE"));

        T item;
        try {
            item = clazz.newInstance();
            item.setItem(jsonTable);
        } catch (IllegalAccessException | InstantiationException e) {
            throw new IllegalStateException(e);
        }

        item.setItemType(descItemType);
        ElzaTable table = new ElzaTable();
        item.getItem().setValue(table);

        // Načtení CSV a naplnění tabulky
        Iterable<CSVRecord> records = CSV_EXCEL_FORMAT.withFirstRecordAsHeader().parse(in);
        for (CSVRecord record : records) {
            ElzaRow row = new ElzaRow();
            for (ElzaColumn elzaColumn : descItemType.getColumnsDefinition()) {
                row.setValue(elzaColumn.getCode(), record.get(elzaColumn.getCode()));
            }
            table.addRow(row);
        }

        return item;
    }

}
