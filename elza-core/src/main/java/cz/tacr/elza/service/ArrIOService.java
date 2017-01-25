package cz.tacr.elza.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.kml.KML;
import org.geotools.kml.KMLConfiguration;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.xml.Encoder;
import org.geotools.xml.Parser;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrItemCoordinates;
import cz.tacr.elza.domain.ArrItemData;
import cz.tacr.elza.domain.ArrItemJsonTable;
import cz.tacr.elza.domain.ArrOutputItem;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.factory.DescItemFactory;
import cz.tacr.elza.domain.table.ElzaColumn;
import cz.tacr.elza.domain.table.ElzaRow;
import cz.tacr.elza.domain.table.ElzaTable;
import cz.tacr.elza.repository.DataTypeRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.repository.OutputItemRepository;

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
    private DataTypeRepository dataTypeRepository;

    @Autowired
    private DescriptionItemService descriptionItemService;

    @Autowired
    private OutputService outputService;

    @Autowired
    private ItemTypeRepository itemTypeRepository;

    @Autowired
    private RuleService ruleService;

    @Autowired
    private DescItemRepository descItemRepository;

    @Autowired
    private FundVersionRepository fundVersionRepository;

    @Autowired
    private OutputItemRepository outputItemRepository;

    @Autowired
    private ItemService itemService;

    /**
     * CSV konfigurace pro CZ Excel.
     */
    public static final CSVFormat CSV_EXCEL_FORMAT = CSVFormat.DEFAULT
            .withIgnoreEmptyLines(false)
            .withAllowMissingColumnNames()
            .withDelimiter(';')
            .withQuote('"');
    /**
     * Kódování pro CSV soubory.
     */
    public static final String CSV_EXCEL_ENCODING = "cp1250";


    /**
     * Export dat tabulky do csv formátu, který bude zapsán do streamu.
     *
     * @param item desc item
     * @param os   výstupní stream
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
     *
     * @param fundVersionId  verze souboru
     * @param nodeId         id uzlu
     * @param nodeVersion    verze uzlu
     * @param descItemTypeId id typu atributu
     * @param is             stream s csv souborem
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
            ArrDescItem descItem = csvImport(descItemTypeId, in, ArrDescItem.class);

            // Vyvoření nové s naimportovanými daty
            return descriptionItemService.createDescriptionItem(descItem, nodeId, nodeVersion, fundVersionId);
        }
    }

    /**
     * Import csv do output item, která bude nahrazena.
     *
     * @param fundVersionId           verze souboru
     * @param outputDefinitionId      id outputu
     * @param outputDefinitionVersion verze outputu
     * @param descItemTypeId          id typu atributu
     * @param is                      stream s csv souborem
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
            ArrOutputItem outputItem = csvImport(descItemTypeId, in, ArrOutputItem.class);

            // Vyvoření nové s naimportovanými daty
            return outputService.createOutputItem(outputItem, outputDefinitionId, outputDefinitionVersion, fundVersionId);
        }
    }

    /**
     * Import csv.
     *
     * @param descItemTypeId id typu atributu
     * @param in             stream s csv souborem
     * @param clazz          třída, pro kterou importuju
     * @return vytvořená položka
     */
    private <T extends ArrItem> T csvImport(final Integer descItemTypeId, final Reader in, final Class<T> clazz) throws IOException {
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
        ((ArrItemJsonTable) item.getItem()).setValue(table);

        // Načtení CSV a naplnění tabulky
        Iterable<CSVRecord> records = CSV_EXCEL_FORMAT.withFirstRecordAsHeader().parse(in);
        for (CSVRecord record : records) {
            ElzaRow row = new ElzaRow();
            for (ElzaColumn elzaColumn : descItemType.getColumnsDefinition()) {
                row.setValue(elzaColumn.getCode(), record.get(elzaColumn.getCode()));
            }
            table.addRow(row);
        }

        // kontrola datových typů tabulky
        itemService.checkJsonTableData(table, descItemType.getColumnsDefinition());

        return item;
    }


    public List<Integer> coordinatesOutputImport(final Integer fundVersionId,
                                                 final Integer descItemTypeId,
                                                 final Integer outputDefinitionId,
                                                 final Integer outputDefinitionVersion,
                                                 final MultipartFile importFile) throws ParserConfigurationException, SAXException, IOException {
        return coordinatesImport(fundVersionId, descItemTypeId, outputDefinitionId, outputDefinitionVersion, importFile, ArrOutputItem.class);
    }

    public List<Integer> coordinatesDescImport(final Integer fundVersionId,
                                               final Integer descItemTypeId,
                                               final Integer nodeId,
                                               final Integer nodeVersion,
                                               final MultipartFile importFile) throws IOException, SAXException, ParserConfigurationException {
        return coordinatesImport(fundVersionId, descItemTypeId, nodeId, nodeVersion, importFile, ArrDescItem.class);
    }

    private <T extends ArrItem> List<Integer> coordinatesImport(final Integer fundVersionId,
                                                                final Integer descItemTypeId,
                                                                final Integer parentId,
                                                                final Integer parentVersion,
                                                                final MultipartFile importFile,
                                                                final Class<T> clazz) throws IOException, SAXException, ParserConfigurationException {
        RulItemType descItemType = itemTypeRepository.findOne(descItemTypeId);
        if (descItemType == null) {
            throw new IllegalStateException("Typ s ID=" + descItemTypeId + " neexistuje");
        }
        if (!"COORDINATES".equals(descItemType.getDataType().getCode())) {
            throw new UnsupportedOperationException("Pouze typ COORDINATES může být importován pomocí KML.");
        }

        Parser parser = new Parser(new KMLConfiguration());
        SimpleFeature document = (SimpleFeature) parser.parse(importFile.getInputStream());
        Collection<SimpleFeature> placemarks = getPlacemars(document);

        List<T> toCreate = new ArrayList<>();
        for (SimpleFeature placemark : placemarks) {
            Geometry geometry = (Geometry) placemark.getAttribute("Geometry");
            switch (geometry.getGeometryType()) {
                case "Point":
                case "LineString":
                case "Polygon":
                    break;
                default:
                    continue;
            }

            ArrItemCoordinates itemData = new ArrItemCoordinates();
            itemData.setValue(geometry);
            T item;
            try {
                item = clazz.newInstance();
                item.setItem(itemData);
            } catch (IllegalAccessException | InstantiationException e) {
                throw new IllegalStateException(e);
            }
            item.setItemType(descItemType);
            toCreate.add(item);
        }

        if (toCreate.isEmpty()) {
            throw new IllegalStateException("Nebyli nalezeny souřadnice.");
        }

        List<Integer> ids = new ArrayList<>();

        if (clazz.isAssignableFrom(ArrOutputItem.class)) {

            List<RulItemTypeExt> outputItemTypes = ruleService.getOutputItemTypes(fundVersionId);

            RulItemTypeExt rule = null;

            for (RulItemTypeExt desc : outputItemTypes) {
                if (descItemType.getItemTypeId().equals(desc.getItemTypeId())) {
                    rule = desc;
                    break;
                }
            }

            if (rule == null) {
                throw new IllegalStateException("Pravidlo s ID=" + descItemTypeId + " neexistuje");
            }

            if (!rule.getRepeatable()) {
                if (toCreate.size() > 1) {
                    throw new IllegalStateException("Do neopakovatelného prvku lze importovat pouze jeden geoobjekt.");
                }

                outputService.deleteOutputItemsByTypeWithoutVersion(fundVersionId, parentId, descItemTypeId);
            }

            outputService.createOutputItems((List<ArrOutputItem>) toCreate, parentId, parentVersion, fundVersionId).forEach(arrOutputItem -> {
                ids.add(arrOutputItem.getDescItemObjectId());
            });

        } else if (clazz.isAssignableFrom(ArrDescItem.class)) {

            List<RulItemTypeExt> descriptionItemTypes = ruleService.getDescriptionItemTypes(fundVersionId, parentId);

            RulItemTypeExt rule = null;

            for (RulItemTypeExt desc : descriptionItemTypes) {
                if (descItemType.getItemTypeId().equals(desc.getItemTypeId())) {
                    rule = desc;
                    break;
                }
            }

            if (rule == null) {
                throw new IllegalStateException("Pravidlo s ID=" + descItemTypeId + " neexistuje");
            }

            if (!rule.getRepeatable()) {
                if (toCreate.size() > 1) {
                    throw new IllegalStateException("Do neopakovatelného prvku lze importovat pouze jeden geoobjekt.");
                }

                descriptionItemService.deleteDescriptionItemsByTypeWithoutVersion(fundVersionId, parentId, parentVersion, descItemTypeId);
            }

            descriptionItemService.createDescriptionItems((List<ArrDescItem>) toCreate, parentId, parentVersion, fundVersionId).forEach(arrDescItem -> {
                ids.add(arrDescItem.getDescItemObjectId());
            });
        } else {
            throw new IllegalStateException("Nedefinovaná třída pro import: " + clazz.getSimpleName());
        }

        return ids;
    }

    public Collection<SimpleFeature> getPlacemars(final SimpleFeature document) throws IllegalArgumentException {
        Collection<SimpleFeature> placemarks;
        Collection<SimpleFeature> features;
        try {
            features = (Collection<SimpleFeature>) document.getAttribute("Feature");
            if (features.size() == 1 && features.iterator().next().getAttribute("Geometry") == null) {
                placemarks = (Collection<SimpleFeature>) features.iterator().next().getAttribute("Feature");
            } else {
                placemarks = features;
            }
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Chybná data pro import. Nepodařilo se zpracovat.");
        }
        return placemarks;
    }

    public void coordinatesDescExport(final HttpServletResponse response,
                                      final Integer descItemObjectId,
                                      final Integer fundVersionId) throws IOException {
        coordinatesOutputExport(response, descItemObjectId, fundVersionId, ArrDescItem.class);
    }

    public <T extends ArrItem> void coordinatesOutputExport(final HttpServletResponse response, final Integer descItemObjectId, final Integer fundVersionId, final Class<T> clazz) throws IOException {

        BiFunction<Integer, ArrChange, List<ArrOutputItem>> find1Output = outputItemRepository::findByDescItemObjectIdAndBetweenVersionChangeId;
        BiFunction<Integer, ArrChange, List<ArrOutputItem>> find2Output = outputItemRepository::findByDescItemObjectIdAndLockChangeId;
        BiFunction<Integer, ArrChange, List<ArrDescItem>> find1Desc = descItemRepository::findByDescItemObjectIdAndBetweenVersionChangeId;
        BiFunction<Integer, ArrChange, List<ArrDescItem>> find2Desc = descItemRepository::findByDescItemObjectIdAndLockChangeId;

        ArrFundVersion fundVersion = fundVersionRepository.findOne(fundVersionId);
        ArrChange change = fundVersion.getLockChange();
        List<T> items;

        if (clazz.isAssignableFrom(ArrOutputItem.class)) {
            if (change == null) {
                change = fundVersion.getCreateChange();
                items = (List<T>) find1Output.apply(descItemObjectId, change);
            } else {
                items = (List<T>) find2Output.apply(descItemObjectId, change);
            }
        } else if (clazz.isAssignableFrom(ArrDescItem.class)) {
            if (change == null) {
                change = fundVersion.getCreateChange();
                items = (List<T>) find1Desc.apply(descItemObjectId, change);
            } else {
                items = (List<T>) find2Desc.apply(descItemObjectId, change);
            }
        } else {
            throw new IllegalStateException("Nedefinovaná třída pro import: " + clazz.getSimpleName());
        }

        if (items.size() < 1) {
            throw new IllegalArgumentException("Neexistují data pro verzi:" + fundVersionId);
        }

        T one = items.get(items.size() - 1);

        Assert.notNull(one);

        one = itemService.loadData(one);

        ArrItemData item = one.getItem();

        if (!(item instanceof ArrItemCoordinates)) {
            throw new UnsupportedOperationException("Pouze typ COORDINATES může být exportován do KML.");
        }
        ArrItemCoordinates cords = (ArrItemCoordinates) item;

        toKml(response, cords.getValue());
    }

    public void coordinatesOutputExport(final HttpServletResponse response, final Integer descItemObjectId, final Integer fundVersionId) throws IOException {
        coordinatesOutputExport(response, descItemObjectId, fundVersionId, ArrOutputItem.class);
    }


    public void toKml(final HttpServletResponse response, final Geometry geometry) throws IOException {
        response.setHeader("Content-Disposition", "attachment;filename=export.kml");

        ServletOutputStream out = response.getOutputStream();
        Encoder encoder = new Encoder(new KMLConfiguration());
        encoder.setIndenting(false);
        encoder.setNamespaceAware(false);

        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("geometry");
        String geotype = geometry.getGeometryType();
        switch (geotype) {
            case "Point":
                typeBuilder.add("geometry", Point.class, DefaultGeographicCRS.WGS84);
                break;
            case "LineString":
                typeBuilder.add("geometry", LineString.class, DefaultGeographicCRS.WGS84);
                break;
            case "Polygon":
                typeBuilder.add("geometry", Polygon.class, DefaultGeographicCRS.WGS84);
                break;
            default:
                throw new UnsupportedOperationException("Neznámý typ geodat:" + geotype + ". Nelze exportovat.");
        }

        SimpleFeatureType TYPE = typeBuilder.buildFeatureType();

        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);
        DefaultFeatureCollection features = new DefaultFeatureCollection();

        featureBuilder.add(geometry);
        features.add(featureBuilder.buildFeature("1"));

        encoder.encode(features, KML.kml, out);
    }
}
