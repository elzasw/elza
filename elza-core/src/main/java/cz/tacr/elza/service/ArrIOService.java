package cz.tacr.elza.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.kml.KML;
import org.geotools.kml.KMLConfiguration;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.xml.Encoder;
import org.geotools.xml.Parser;
import org.hibernate.Hibernate;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import cz.tacr.elza.common.FileDownload;
import cz.tacr.elza.controller.vo.FilterNode;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataCoordinates;
import cz.tacr.elza.domain.ArrDataJsonTable;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrOutputItem;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.table.ElzaColumn;
import cz.tacr.elza.domain.table.ElzaRow;
import cz.tacr.elza.domain.table.ElzaTable;
import cz.tacr.elza.domain.vo.DescItemValue;
import cz.tacr.elza.domain.vo.DescItemValues;
import cz.tacr.elza.domain.vo.JsonTableTitleValue;
import cz.tacr.elza.domain.vo.TitleValue;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.ExternalCode;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.ItemRepository;
import cz.tacr.elza.repository.ItemTypeRepository;

import static cz.tacr.elza.utils.CsvUtils.CSV_EXCEL_ENCODING;
import static cz.tacr.elza.utils.CsvUtils.CSV_EXCEL_FORMAT;

/**
 * Serviska pro import/export dat pro ArrItem.
 *
 * @author Martin Šlapa
 * @since 27.06.2016
 */
@Service
public class ArrIOService {

    @Autowired
    private DescriptionItemService descriptionItemService;

    @Autowired
    private OutputService outputService;

    @Autowired
    private ItemTypeRepository itemTypeRepository;

    @Autowired
    private FundVersionRepository fundVersionRepository;

    @Autowired
    private ItemService itemService;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private FilterTreeService filterTreeService;

    @Autowired
    private RuleService ruleService;

    @Autowired
    private StaticDataService staticDataService;

    /**
     * Export dat tabulky do csv formátu, který bude zapsán do streamu.
     *
     * @param item desc item
     * @param os   výstupní stream
     */
    public <T extends ArrItem> void csvExport(final T item, final OutputStream os) throws IOException {
        RulItemType descItemType = item.getItemType();
        List<String> columNames = ((List<ElzaColumn>) descItemType.getViewDefinition())
                .stream()
                .map(ElzaColumn::getCode)
                .collect(Collectors.toList());

        try (
                OutputStreamWriter out = new OutputStreamWriter(os, CSV_EXCEL_ENCODING);
                CSVPrinter csvp = CSV_EXCEL_FORMAT.withHeader(columNames.toArray(new String[columNames.size()])).print(out);
        ) {
            ElzaTable table = ((ArrDataJsonTable) item.getData()).getValue();

            for (ElzaRow elzaRow : table.getRows()) {
                Map<String, String> values = elzaRow.getValues();
                List<Object> rowValues = ((List<ElzaColumn>) descItemType.getViewDefinition())
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

        ArrDataJsonTable jsonTable = new ArrDataJsonTable();

        T item;
        try {
            item = clazz.newInstance();
            item.setData(jsonTable);
        } catch (IllegalAccessException | InstantiationException e) {
            throw new SystemException(e);
        }

        item.setItemType(descItemType);
        ElzaTable table = new ElzaTable();

        // Načtení CSV a naplnění tabulky
        Iterable<CSVRecord> records = CSV_EXCEL_FORMAT.withFirstRecordAsHeader().parse(in);
        for (CSVRecord record : records) {
            ElzaRow row = new ElzaRow();
            for (ElzaColumn elzaColumn : (List<ElzaColumn>) descItemType.getViewDefinition()) {
                row.setValue(elzaColumn.getCode(), record.get(elzaColumn.getCode()));
            }
            table.addRow(row);
        }

        jsonTable.setValue(table);

        // kontrola datových typů tabulky
        itemService.checkJsonTableData(table, (List<ElzaColumn>) descItemType.getViewDefinition());

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
            throw new BusinessException("Typ s ID=" + descItemTypeId + " neexistuje", ArrangementCode.ITEM_TYPE_NOT_FOUND).set("id", descItemTypeId);
        }
        if (!"COORDINATES".equals(descItemType.getDataType().getCode())) {
            throw new SystemException("Pouze typ COORDINATES může být importován pomocí KML.", BaseCode.PROPERTY_HAS_INVALID_TYPE)
                    .set("property", "descItemTypeId")
                    .set("expected", "COORDINATES")
                    .set("actual", descItemType.getDataType().getCode());
        }

        Parser parser = new Parser(new KMLConfiguration());
        SimpleFeature document = (SimpleFeature) parser.parse(importFile.getInputStream());
        Collection<SimpleFeature> placemarks = getPlacemarks(document);

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

            ArrDataCoordinates itemData = new ArrDataCoordinates();
            itemData.setValue(geometry);
            T item;
            try {
                item = clazz.newInstance();
                item.setData(itemData);
            } catch (IllegalAccessException | InstantiationException e) {
                throw new SystemException(e);
            }
            item.setItemType(descItemType);
            toCreate.add(item);
        }

        if (toCreate.isEmpty()) {
            throw new BusinessException("Nebyli nalezeny souřadnice.", BaseCode.PROPERTY_NOT_EXIST).set("property", "geometry");
        }

        List<Integer> ids = new ArrayList<>();

        if (clazz.isAssignableFrom(ArrOutputItem.class)) {
            outputService.createOutputItems((List<ArrOutputItem>) toCreate, parentId, parentVersion, fundVersionId).forEach(arrOutputItem -> {
                ids.add(arrOutputItem.getDescItemObjectId());
            });

        } else if (clazz.isAssignableFrom(ArrDescItem.class)) {
            descriptionItemService.createDescriptionItems((List<ArrDescItem>) toCreate, parentId, parentVersion, fundVersionId).forEach(arrDescItem -> {
                ids.add(arrDescItem.getDescItemObjectId());
            });
        } else {
            throw new IllegalStateException("Nedefinovaná třída pro import: " + clazz.getSimpleName());
        }

        return ids;
    }

    public Collection<SimpleFeature> getPlacemarks(final SimpleFeature document) throws IllegalArgumentException {
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
            throw new SystemException("Chybná data pro import. Nepodařilo se zpracovat.", e, ExternalCode.IMPORT_FAIL);
        }
        return placemarks;
    }

    public void coordinatesExport(final HttpServletResponse response, final Integer descItemObjectId, final Integer fundVersionId) throws IOException {

        ArrFundVersion fundVersion = fundVersionRepository.findOne(fundVersionId);
        ArrChange lockChange = fundVersion.getLockChange();

        ArrItem item;
        if (lockChange == null) {
            item = itemRepository.findByItemObjectIdAndDeleteChangeIsNullFetchData(descItemObjectId);
        } else {
            item = itemRepository.findByItemObjectIdAndChangeFetchData(descItemObjectId, lockChange);
        }

        if (item == null || item.isUndefined()) {
            throw new SystemException(
                    "Coordinates data not found, fundVersionId:" + fundVersionId + ", itemObjectId:" + descItemObjectId,
                    BaseCode.DB_INTEGRITY_PROBLEM);
        }

        ArrData data = item.getData();
        Class<?> cls = Hibernate.getClass(data);

        if (!ArrDataCoordinates.class.isAssignableFrom(cls)) {
            throw new SystemException("Pouze typ COORDINATES může být importován pomocí KML.", BaseCode.PROPERTY_HAS_INVALID_TYPE)
                .set("property", "descItemObjectId")
                .set("expected", "COORDINATES")
                .set("actual", item.getClass().getSimpleName());
        }

        toKml(response, ((ArrDataCoordinates) data).getValue());
    }

    public void toKml(final HttpServletResponse response, final Geometry geometry) throws IOException {
        FileDownload.addContentDispositionAsAttachment(response, "export.kml");

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

    /**
     * Return name of export
     * 
     * @param fund
     * @return
     */
    private static String getExportFileName(ArrFund fund, String postfix) {
        StringBuilder exportName = new StringBuilder();
        if (StringUtils.isNotBlank(fund.getInternalCode())) {
            exportName.append(fund.getInternalCode());
        } else 
        if (StringUtils.isNotBlank(fund.getName())) {
            exportName.append(fund.getName());
        } else {
            exportName.append(fund.getFundId().toString());
        }
        exportName.append(postfix);
        exportName.append(".csv");
        return exportName.toString();
    }

    @Transactional
    public void dataGridDataExport(final HttpServletResponse response,
                                   final Integer versionId,
                                   final List<Integer> rulItemTypeIds)
            throws IOException {
        StaticDataProvider sdp = staticDataService.createProvider();

        List<String> columNames = new LinkedList<>();
        columNames.add("Číslo záznamu");
        columNames.add("Číslo JP");
        columNames.add("Atribut");
        columNames.add("Specifikace");
        columNames.add("Hodnota");
        columNames.add("ID entity");

        ArrFundVersion version = fundVersionRepository.getOneCheckExist(versionId);
        FileDownload.addContentDispositionAsAttachment(response, getExportFileName(version.getFund(), "-data"));

        ArrayList<Integer> filteredIds = filterTreeService.getFilteredIds(versionId);
        int page = 0;
        int pageSize = 100;
        try (ServletOutputStream os = response.getOutputStream();
             OutputStreamWriter out = new OutputStreamWriter(os, CSV_EXCEL_ENCODING);
             CSVPrinter csvp = CSV_EXCEL_FORMAT.withHeader(columNames.toArray(new String[columNames.size()])).print(out);
        ) {
            List<FilterNode> filteredData;
            int i = 0;
            do {
                filteredData = filterTreeService.getFilteredData(version, page, pageSize, rulItemTypeIds, true,
                        filteredIds);
                for (FilterNode node : filteredData) {
                    ++i;
                    writeNodeData(sdp, columNames, i, node, csvp);
                }
                csvp.flush();
                page++;
            } while (filteredData.size() == 100);
        }
    }

    private void writeNodeData(StaticDataProvider sdp, List<String> columNames, int i, FilterNode node,
                               CSVPrinter csvp)
            throws IOException {
        for (Map.Entry<Integer, DescItemValues> entry : node.getValuesMap().entrySet()) {
            ItemType itemType = sdp.getItemTypeById(entry.getKey());
            DescItemValues descItemValues = entry.getValue();
            if (descItemValues != null && !descItemValues.getValues().isEmpty()) {
                for (DescItemValue v : descItemValues.getValues()) {
                    List<Object> rowValues = new ArrayList<>(columNames.size());
                    rowValues.add(i);
                    rowValues.add(StringUtils.join(node.getReferenceMark()));
                    rowValues.add(itemType.getEntity().getName());

                    String specname = null;
                    Integer entityId = null;
                    if (v instanceof TitleValue) {
                        TitleValue tv = (TitleValue) v;
                        if (itemType.getDataType() != DataType.ENUM) {
                            specname = tv.getSpecName();
                        }
                        entityId = tv.getEntityId();
                    }

                    String value;
                    if (v instanceof JsonTableTitleValue) {
                        JsonTableTitleValue tableValue = (JsonTableTitleValue) v;
                        value = "Tabulka 2x" + tableValue.getRows();
                    } else {
                        value = v.getValue();
                    }

                    rowValues.add(specname);
                    rowValues.add(value);
                    rowValues.add(entityId);
                    csvp.printRecord(rowValues);
                }
            }
        }
    }


    @Transactional
    public void dataGridTableExport(final HttpServletResponse response,
                                   final Integer versionId,
                                   final List<Integer> rulItemTypeIds) throws IOException {
        List<RulItemType> orderedItemTypes = ruleService.findItemTypesByIdsOrdered(rulItemTypeIds);

        Map<Integer, RulItemType> orderedItemTypeMap = new LinkedHashMap<>(orderedItemTypes.size());
        for (RulItemType rulItemType : orderedItemTypes) {
            orderedItemTypeMap.put(rulItemType.getItemTypeId(), rulItemType);
        }
        List<String> columNames = new ArrayList<>(orderedItemTypeMap.size() + 2);
        columNames.add("Číslo záznamu");
        columNames.add("Číslo JP");
        for (RulItemType rulItemType : orderedItemTypeMap.values()) {
            if (rulItemType != null) {
                columNames.add(rulItemType.getName());
            }
        }

        ArrFundVersion version = fundVersionRepository.getOneCheckExist(versionId);
        FileDownload.addContentDispositionAsAttachment(response, getExportFileName(version.getFund(), "-table"));
        ArrayList<Integer> filteredIds = filterTreeService.getFilteredIds(versionId);
        int page = 0;
        int pageSize = 100;
        try (ServletOutputStream os = response.getOutputStream();
             OutputStreamWriter out = new OutputStreamWriter(os, CSV_EXCEL_ENCODING);
             CSVPrinter csvp = CSV_EXCEL_FORMAT.withHeader(columNames.toArray(new String[columNames.size()])).print(out);
        ) {
            List<FilterNode> filteredData;
            int i = 1;
            do {
                filteredData = filterTreeService.getFilteredData(version, page, pageSize, rulItemTypeIds, true,
                        filteredIds);
                for (FilterNode node : filteredData) {
                    List<Object> rowValues = new ArrayList<>(orderedItemTypeMap.size());
                    rowValues.add(i++);
                    rowValues.add(StringUtils.join(node.getReferenceMark()));

                    Map<Integer, DescItemValues> valuesMap = node.getValuesMap();
                    for (RulItemType rulItemType : orderedItemTypeMap.values()) {
                        DescItemValues descItemValues = valuesMap.get(rulItemType.getItemTypeId());
                        String value = null;
                        if (descItemValues != null && !descItemValues.getValues().isEmpty()) {
                            value = descItemValues.getValues().stream().map(v -> {
                                if (DataType.fromId(rulItemType.getDataTypeId()) != DataType.ENUM
                                        && rulItemType.getUseSpecification()
                                        && v instanceof TitleValue) {
                                    TitleValue tv = (TitleValue)  v;
                                    return tv.getSpecName() + "|" + StringUtils.trimToEmpty(v.getValue());
                                } else if (v instanceof JsonTableTitleValue) {
                                    JsonTableTitleValue tableValue = (JsonTableTitleValue) v;
                                    return "Tabulka 2x" + tableValue.getRows();
                                } else {
                                    return StringUtils.trimToNull(v.getValue());
                                }
                            }).collect(Collectors.joining("\n"));
                        }
                        rowValues.add(value);
                    }
                    csvp.printRecord(rowValues);
                }
                csvp.flush();
                page++;
            } while (filteredData.size() == 100);
        }
    }
}
