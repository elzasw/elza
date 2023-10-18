package cz.tacr.elza.domain.factory;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import cz.tacr.elza.controller.ArrangementController;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataBit;
import cz.tacr.elza.domain.ArrDataCoordinates;
import cz.tacr.elza.domain.ArrDataDate;
import cz.tacr.elza.domain.ArrDataDecimal;
import cz.tacr.elza.domain.ArrDataFileRef;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataJsonTable;
import cz.tacr.elza.domain.ArrDataNull;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataStructureRef;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.ArrDataUnitid;
import cz.tacr.elza.domain.ArrDataUriRef;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrItemBit;
import cz.tacr.elza.domain.ArrItemCoordinates;
import cz.tacr.elza.domain.ArrItemDate;
import cz.tacr.elza.domain.ArrItemDecimal;
import cz.tacr.elza.domain.ArrItemEnum;
import cz.tacr.elza.domain.ArrItemFileRef;
import cz.tacr.elza.domain.ArrItemFormattedText;
import cz.tacr.elza.domain.ArrItemInt;
import cz.tacr.elza.domain.ArrItemJsonTable;
import cz.tacr.elza.domain.ArrItemRecordRef;
import cz.tacr.elza.domain.ArrItemString;
import cz.tacr.elza.domain.ArrItemStructureRef;
import cz.tacr.elza.domain.ArrItemText;
import cz.tacr.elza.domain.ArrItemUnitdate;
import cz.tacr.elza.domain.ArrItemUnitid;
import cz.tacr.elza.domain.ArrItemUriRef;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.convertor.CalendarConverter;
import cz.tacr.elza.domain.table.ElzaColumn;
import cz.tacr.elza.domain.table.ElzaRow;
import cz.tacr.elza.domain.table.ElzaTable;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.DataBitRepository;
import cz.tacr.elza.repository.DataCoordinatesRepository;
import cz.tacr.elza.repository.DataDateRepository;
import cz.tacr.elza.repository.DataDecimalRepository;
import cz.tacr.elza.repository.DataFileRefRepository;
import cz.tacr.elza.repository.DataIntegerRepository;
import cz.tacr.elza.repository.DataJsonTableRepository;
import cz.tacr.elza.repository.DataNullRepository;
import cz.tacr.elza.repository.DataRecordRefRepository;
import cz.tacr.elza.repository.DataStringRepository;
import cz.tacr.elza.repository.DataStructureRefRepository;
import cz.tacr.elza.repository.DataTextRepository;
import cz.tacr.elza.repository.DataUnitdateRepository;
import cz.tacr.elza.repository.DataUnitidRepository;
import cz.tacr.elza.repository.DataUriRefRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.NodeRepository;
import ma.glasnost.orika.CustomMapper;
import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.impl.DefaultMapperFactory;

/**
 * Factory pro vytváření a manipulaci s atributama a jejich hodnotama.
 *
 */
@Component
public class DescItemFactory implements InitializingBean {

    private final String PROPERTY_FORMAT = "format";
    public final static String ELZA_NODE = "elza-node";

    /**
     * Povolené protokoly
     */
    private final String PATTERN_PROTOCOL = "^(https?|elza-node)://.*";

    /**
     * Povolenoné zkratky
     */
    public final String PATTERN_UNIT_DATA = "(C|Y|YM|D|DT|)";

    /**
     * Oddělovač intervalu datace
     */
    public final String INTERVAL_DELIMITER_UNIT_DATA = "-";

    private DefaultMapperFactory factory;
    private MapperFacade facade;
    private LinkedHashMap<Class<? extends ArrData>, JpaRepository> mapRepository;

    @Autowired
    private DescItemRepository descItemRepository;

    @Autowired
    private DataCoordinatesRepository dataCoordinatesRepository;

    @Autowired
    private DataIntegerRepository dataIntegerRepository;

    @Autowired
    private DataRecordRefRepository dataRecordRefRepository;

    @Autowired
    private DataStringRepository dataStringRepository;

    @Autowired
    private DataTextRepository dataTextRepository;

    @Autowired
    private DataUnitdateRepository dataUnitdateRepository;

    @Autowired
    private DataUnitidRepository dataUnitidRepository;

    @Autowired
    private DataDecimalRepository dataDecimalRepository;

    @Autowired
    private DataStructureRefRepository dataStructureRefRepository;

    @Autowired
    private DataFileRefRepository dataFileRefRepository;

    @Autowired
    private DataNullRepository dataNullRepository;

    @Autowired
    private DataJsonTableRepository dataJsonTableRepository;

    @Autowired
    private DataUriRefRepository dataUriRefRepository;

    @Autowired
    private DataDateRepository dataDateRepository;

    @Autowired
    private DataBitRepository dataBitRepository;

    @Autowired
    private NodeRepository nodeRepository;

    public DescItemFactory() {
    }

    public DescItemRepository getDescItemRepository() {
        return descItemRepository;
    }

    @Override
    public void afterPropertiesSet() {
        factory = new DefaultMapperFactory.Builder().build();

        defineMapCoordinates();
        defineMapFormattedText();
        defineMapInt();
        defineMapRecordRef();
        defineMapString();
        defineMapText();
        defineMapUnitdate();
        defineMapUnitid();
        defineMapDecimal();
        defineMapStructureRef();
        defineMapFileRef();
        defineMapEnum();
        defineMapJsonTable();
        defineMapUriRef();
        defineMapDate();
        defineMapBit();

        facade = factory.getMapperFacade();

        initMapRepository();
    }

    /**
     * Nadefinování pravidel pro převod formátu Coordinates.
     */
    private void defineMapCoordinates() {
        factory.classMap(ArrItemCoordinates.class, ArrDataCoordinates.class)
                .customize(new CustomMapper<ArrItemCoordinates, ArrDataCoordinates>() {

                    @Override
                    public void mapAtoB(final ArrItemCoordinates arrDescItemCoordinates,
                                        final ArrDataCoordinates arrDataCoordinates,
                                        final MappingContext context) {
                        arrDataCoordinates.setValue(arrDescItemCoordinates.getValue());
                    }

                    @Override
                    public void mapBtoA(final ArrDataCoordinates arrDataCoordinates,
                                        final ArrItemCoordinates arrDescItemExtCoordinates,
                                        final MappingContext context) {
                        arrDescItemExtCoordinates.setValue(arrDataCoordinates.getValue());
                    }

                }).register();

        factory.classMap(ArrDataCoordinates.class, ArrDataCoordinates.class)
                .customize(new CustomMapper<ArrDataCoordinates, ArrDataCoordinates>() {
                    @Override
                    public void mapAtoB(final ArrDataCoordinates arrDataCoordinates,
                                        final ArrDataCoordinates arrDataCoordinatesNew,
                                        final MappingContext context) {
                        arrDataCoordinatesNew.setDataType(arrDataCoordinates.getDataType());
                        //arrDataCoordinatesNew.setItem(arrDataCoordinates.getItem());
                        arrDataCoordinatesNew.setValue(arrDataCoordinates.getValue());
                    }
                }).register();
    }

    /**
     * Nadefinování pravidel pro převod formátu JsonTable.
     */
    private void defineMapJsonTable() {
        factory.classMap(ArrItemJsonTable.class, ArrDataJsonTable.class)
                .customize(new CustomMapper<ArrItemJsonTable, ArrDataJsonTable>() {

                    @Override
                    public void mapAtoB(final ArrItemJsonTable arrItemJsonTable,
                                        final ArrDataJsonTable arrDataJsonTable,
                                        final MappingContext context) {
                        arrDataJsonTable.setValue(arrItemJsonTable.getValue());
                    }

                    @Override
                    public void mapBtoA(final ArrDataJsonTable arrDataJsonTable,
                                        final ArrItemJsonTable arrItemJsonTable,
                                        final MappingContext context) {
                        arrItemJsonTable.setValue(arrDataJsonTable.getValue());
                    }

                }).register();

        factory.classMap(ArrDataJsonTable.class, ArrDataJsonTable.class)
                .customize(new CustomMapper<ArrDataJsonTable, ArrDataJsonTable>() {
                    @Override
                    public void mapAtoB(final ArrDataJsonTable arrDataJsonTable,
                                        final ArrDataJsonTable arrDataJsonTableNew,
                                        final MappingContext context) {
                        arrDataJsonTableNew.setDataType(arrDataJsonTable.getDataType());
                        //arrDataJsonTableNew.setItem(arrDataJsonTable.getItem());
                        arrDataJsonTableNew.setValue(arrDataJsonTable.getValue());
                    }
                }).register();
    }

    /**
     * Nadefinování pravidel pro převod formátu UriRef.
     */
    private void defineMapUriRef() {
        factory.classMap(ArrItemUriRef.class, ArrDataUriRef.class)
                .customize(new CustomMapper<ArrItemUriRef, ArrDataUriRef>() {
                    @Override
                    public void mapAtoB(ArrItemUriRef arrItemUriRef,
                                        ArrDataUriRef arrDataUriRef,
                                        MappingContext context) {
                        arrDataUriRef.setSchema(arrItemUriRef.getSchema());
                        arrDataUriRef.setUriRefValue(arrItemUriRef.getValue());
                        arrDataUriRef.setDescription(arrItemUriRef.getSchema());
                        arrDataUriRef.setArrNode(arrItemUriRef.getNode());

                    }

                    @Override
                    public void mapBtoA(ArrDataUriRef arrDataUriRef,
                                        ArrItemUriRef arrItemUriRef,
                                        MappingContext context) {
                        arrItemUriRef.setSchema(arrDataUriRef.getSchema());
                        arrItemUriRef.setValue(arrDataUriRef.getUriRefValue());
                        arrItemUriRef.setDescription(arrDataUriRef.getSchema());
                        arrItemUriRef.setNode(arrDataUriRef.getArrNode());
                    }
                }).register();

        factory.classMap(ArrDataUriRef.class, ArrDataUriRef.class)
                .customize(new CustomMapper<ArrDataUriRef, ArrDataUriRef>() {
                    @Override
                    public void mapAtoB(ArrDataUriRef arrDataUriRef,
                                        ArrDataUriRef arrDataUriRefNew,
                                        MappingContext context) {
                        arrDataUriRefNew.setDataType(arrDataUriRef.getDataType());

                        arrDataUriRefNew.setSchema(arrDataUriRef.getSchema());
                        arrDataUriRefNew.setUriRefValue(arrDataUriRef.getUriRefValue());
                        arrDataUriRefNew.setDescription(arrDataUriRef.getSchema());
                        arrDataUriRefNew.setArrNode(arrDataUriRef.getArrNode());
                    }
                }).register();

        factory.classMap(ArrDataText.class, ArrDataText.class)
                .customize(new CustomMapper<ArrDataText, ArrDataText>() {
            @Override
            public void mapAtoB(final ArrDataText arrDataText, final ArrDataText arrDataTextNew, final MappingContext context) {
                arrDataTextNew.setDataType(arrDataText.getDataType());
                //arrDataTextNew.setItem(arrDataText.getItem());
                arrDataTextNew.setTextValue(arrDataText.getTextValue());
            }
        }).register();
    }

    /**
     * Nadefinování pravidel pro převod formátu FormattedText.
     */
    private void defineMapFormattedText() {
        factory.classMap(ArrItemFormattedText.class, ArrDataText.class)
                .customize(new CustomMapper<ArrItemFormattedText, ArrDataText>() {

                    @Override
                    public void mapAtoB(final ArrItemFormattedText arrItemFormattedText,
                                        final ArrDataText arrDataText,
                                        final MappingContext context) {
                        arrDataText.setTextValue(arrItemFormattedText.getValue());
                    }

                    @Override
                    public void mapBtoA(final ArrDataText arrDataText,
                                        final ArrItemFormattedText arrItemFormattedText,
                                        final MappingContext context) {
                        String formattedValue = formatString(context, arrDataText.getTextValue());
                        arrItemFormattedText.setValue(formattedValue);
                    }
                }).register();

        factory.classMap(ArrDataText.class, ArrDataText.class).customize(new CustomMapper<ArrDataText, ArrDataText>() {
            @Override
            public void mapAtoB(final ArrDataText arrDataText, final ArrDataText arrDataTextNew, final MappingContext context) {
                arrDataTextNew.setDataType(arrDataText.getDataType());
                //arrDataTextNew.setItem(arrDataText.getItem());
                arrDataTextNew.setTextValue(arrDataText.getTextValue());
            }
        }).register();
    }

    /**
     * Nadefinování pravidel pro převod formátu Int.
     */
    private void defineMapInt() {
        factory.classMap(ArrItemInt.class, ArrDataInteger.class)
                .customize(new CustomMapper<ArrItemInt, ArrDataInteger>() {

                    @Override
                    public void mapAtoB(final ArrItemInt arrItemInt,
                                        final ArrDataInteger arrDataInteger,
                                        final MappingContext context) {
                        arrDataInteger.setIntegerValue(arrItemInt.getValue());
                    }

                    @Override
                    public void mapBtoA(final ArrDataInteger arrDataInteger,
                                        final ArrItemInt arrItemInt,
                                        final MappingContext context) {
                        arrItemInt.setValue(arrDataInteger.getIntegerValue());
                    }

                }).register();

        factory.classMap(ArrDataInteger.class, ArrDataInteger.class)
                .customize(new CustomMapper<ArrDataInteger, ArrDataInteger>() {
                    @Override
                    public void mapAtoB(final ArrDataInteger arrDataInteger,
                                        final ArrDataInteger arrDataIntegerNew,
                                        final MappingContext context) {
                        arrDataIntegerNew.setDataType(arrDataInteger.getDataType());
                        //arrDataIntegerNew.setItem(arrDataInteger.getItem());
                        arrDataIntegerNew.setIntegerValue(arrDataInteger.getIntegerValue());
                    }
                }).register();
    }

    /**
     * Nadefinování pravidel pro převod formátu PacketRef.
     */
    private void defineMapStructureRef() {
        factory.classMap(ArrItemStructureRef.class, ArrDataStructureRef.class)
                .customize(new CustomMapper<ArrItemStructureRef, ArrDataStructureRef>() {

                    @Override
                    public void mapAtoB(final ArrItemStructureRef itemStructureRef,
                                        final ArrDataStructureRef dataStructureRef,
                                        final MappingContext context) {
                        dataStructureRef.setStructuredObject(itemStructureRef.getStructuredObject());
                    }

                    @Override
                    public void mapBtoA(final ArrDataStructureRef dataStructureRef,
                                        final ArrItemStructureRef itemStructureRef,
                                        final MappingContext context) {
                        itemStructureRef.setStructuredObject(dataStructureRef.getStructuredObject());
                    }

                }).register();

        factory.classMap(ArrDataStructureRef.class, ArrDataStructureRef.class)
                .customize(new CustomMapper<ArrDataStructureRef, ArrDataStructureRef>() {
                    @Override
                    public void mapAtoB(final ArrDataStructureRef dataStructureRef,
                                        final ArrDataStructureRef dataStructureRefNew,
                                        final MappingContext context) {
                        dataStructureRefNew.setDataType(dataStructureRef.getDataType());
                        dataStructureRefNew.setStructuredObject(dataStructureRef.getStructuredObject());
                    }
                }).register();
    }


    /**
     * Nadefinování pravidel pro převod formátu FileRef.
     */
    private void defineMapFileRef() {
        factory.classMap(ArrItemFileRef.class, ArrDataFileRef.class)
                .customize(new CustomMapper<ArrItemFileRef, ArrDataFileRef>() {

                    @Override
                    public void mapAtoB(final ArrItemFileRef arrDescItemFileRef,
                                        final ArrDataFileRef arrDataFileRef,
                                        final MappingContext context) {
                        arrDataFileRef.setFile(arrDescItemFileRef.getFile());
                    }

                    @Override
                    public void mapBtoA(final ArrDataFileRef arrDataFileRef,
                                        final ArrItemFileRef arrDescItemFileRef,
                                        final MappingContext context) {
                        arrDescItemFileRef.setFile(arrDataFileRef.getFile());
                    }

                }).register();

        factory.classMap(ArrDataFileRef.class, ArrDataFileRef.class)
                .customize(new CustomMapper<ArrDataFileRef, ArrDataFileRef>() {
                    @Override
                    public void mapAtoB(final ArrDataFileRef arrDataFileRef,
                                        final ArrDataFileRef arrDataPartyRefNew,
                                        final MappingContext context) {
                        arrDataPartyRefNew.setDataType(arrDataFileRef.getDataType());
                        arrDataPartyRefNew.setFile(arrDataFileRef.getFile());
                    }
                }).register();
    }

    /**
     * Nadefinování pravidel pro převod formátu RecordRef.
     */
    private void defineMapRecordRef() {
        factory.classMap(ArrItemRecordRef.class, ArrDataRecordRef.class)
                .customize(new CustomMapper<ArrItemRecordRef, ArrDataRecordRef>() {

                    @Override
                    public void mapAtoB(final ArrItemRecordRef arrItemRecordRef,
                                        final ArrDataRecordRef arrDataRecordRef,
                                        final MappingContext context) {
                        arrDataRecordRef.setRecord(arrItemRecordRef.getAccessPoint());
                    }

                    @Override
                    public void mapBtoA(final ArrDataRecordRef arrDataRecordRef,
                                        final ArrItemRecordRef arrItemRecordRef,
                                        final MappingContext context) {
                        arrItemRecordRef.setAccessPoint(arrDataRecordRef.getRecord());
                    }

                }).register();

        factory.classMap(ArrDataRecordRef.class, ArrDataRecordRef.class)
                .customize(new CustomMapper<ArrDataRecordRef, ArrDataRecordRef>() {
                    @Override
                    public void mapAtoB(final ArrDataRecordRef arrDataRecordRef,
                                        final ArrDataRecordRef arrDataRecordRefNew,
                                        final MappingContext context) {
                        arrDataRecordRefNew.setDataType(arrDataRecordRef.getDataType());
                        //arrDataRecordRefNew.setItem(arrDataRecordRef.getItem());
                        arrDataRecordRefNew.setRecord(arrDataRecordRef.getRecord());
                    }
                }).register();
    }

    /**
     * Nadefinování pravidel pro převod formátu String.
     */
    private void defineMapString() {
        factory.classMap(ArrItemString.class, ArrDataString.class).customize(
                new CustomMapper<ArrItemString, ArrDataString>() {

                    @Override
                    public void mapAtoB(final ArrItemString arrItemString,
                                        final ArrDataString arrDataString,
                                        final MappingContext context) {
                        arrDataString.setStringValue(arrItemString.getValue());
                    }

                    @Override
                    public void mapBtoA(final ArrDataString arrDataString,
                                        final ArrItemString arrItemString,
                                        final MappingContext context) {
                        String formattedValue = formatString(context, arrDataString.getStringValue());
                        arrItemString.setValue(formattedValue);
                    }
                }).register();

        factory.classMap(ArrDataString.class, ArrDataString.class)
                .customize(new CustomMapper<ArrDataString, ArrDataString>() {
                    @Override
                    public void mapAtoB(final ArrDataString arrDataString,
                                        final ArrDataString arrDataStringNew,
                                        final MappingContext context) {
                        arrDataStringNew.setDataType(arrDataString.getDataType());
                        //arrDataStringNew.setItem(arrDataString.getItem());
                        arrDataStringNew.setStringValue(arrDataString.getStringValue());
                    }
                }).register();
    }

    /**
     * Nadefinování pravidel pro převod formátu Bit.
     */
    private void defineMapBit() {
        factory.classMap(ArrItemBit.class, ArrDataBit.class).customize(
                new CustomMapper<ArrItemBit, ArrDataBit>() {

                    @Override
                    public void mapAtoB(final ArrItemBit arrItemBit,
                                        final ArrDataBit arrDataBit,
                                        final MappingContext context) {
                        arrDataBit.setBitValue(arrItemBit.isValue());
                    }

                    @Override
                    public void mapBtoA(final ArrDataBit arrDataBit,
                                        final ArrItemBit arrItemBit,
                                        final MappingContext context) {
                        arrItemBit.setValue(arrDataBit.isBitValue());
                    }
                }).register();

        factory.classMap(ArrDataBit.class, ArrDataBit.class)
                .customize(new CustomMapper<ArrDataBit, ArrDataBit>() {
                    @Override
                    public void mapAtoB(final ArrDataBit arrDataBit,
                                        final ArrDataBit arrDataBitNew,
                                        final MappingContext context) {
                        arrDataBitNew.setDataType(arrDataBit.getDataType());
                        arrDataBitNew.setBitValue(arrDataBit.isBitValue());
                    }
                }).register();
    }

    /**
     * Nadefinování pravidel pro převod formátu Date.
     */
    private void defineMapDate() {
        factory.classMap(ArrItemDate.class, ArrDataDate.class).customize(
                new CustomMapper<ArrItemDate, ArrDataDate>() {

                    @Override
                    public void mapAtoB(final ArrItemDate arrItemString,
                                        final ArrDataDate arrDataString,
                                        final MappingContext context) {
                        arrDataString.setValue(arrItemString.getValue());
                    }

                    @Override
                    public void mapBtoA(final ArrDataDate arrDataString,
                                        final ArrItemDate arrItemString,
                                        final MappingContext context) {
                        arrItemString.setValue(arrDataString.getValue());
                    }
                }).register();

        factory.classMap(ArrDataDate.class, ArrDataDate.class)
                .customize(new CustomMapper<ArrDataDate, ArrDataDate>() {
                    @Override
                    public void mapAtoB(final ArrDataDate arrDataDate,
                                        final ArrDataDate arrDataDateNew,
                                        final MappingContext context) {
                        arrDataDateNew.setDataType(arrDataDate.getDataType());
                        arrDataDateNew.setValue(arrDataDate.getValue());
                    }
                }).register();
    }

    /**
     * Nadefinování pravidel pro převod formátu Text.
     */
    private void defineMapText() {
        factory.classMap(ArrItemText.class, ArrDataText.class)
                .customize(new CustomMapper<ArrItemText, ArrDataText>() {

                    @Override
                    public void mapAtoB(final ArrItemText arrItemText,
                                        final ArrDataText arrDataText,
                                        final MappingContext context) {
                        arrDataText.setTextValue(arrItemText.getValue());
                    }

                    @Override
                    public void mapBtoA(final ArrDataText arrDataText,
                                        final ArrItemText arrItemText,
                                        final MappingContext context) {
                        String formattedValue = formatString(context, arrDataText.getTextValue());
                        arrItemText.setValue(formattedValue);
                    }
                }).register();

        factory.classMap(ArrDataText.class, ArrDataText.class).customize(new CustomMapper<ArrDataText, ArrDataText>() {
            @Override
            public void mapAtoB(final ArrDataText arrDataText, final ArrDataText arrDataTextNew, final MappingContext context) {
                arrDataTextNew.setDataType(arrDataText.getDataType());
                //arrDataTextNew.setItem(arrDataText.getItem());
                arrDataTextNew.setTextValue(arrDataText.getTextValue());
            }
        }).register();
    }

    /**
     * Nadefinování pravidel pro převod formátu Unitdate.
     */
    private void defineMapUnitdate() {
        factory.classMap(ArrItemUnitdate.class, ArrDataUnitdate.class)
                .customize(new CustomMapper<ArrItemUnitdate, ArrDataUnitdate>() {

                    @Override
                    public void mapAtoB(final ArrItemUnitdate arrItemUnitdate,
                                        final ArrDataUnitdate arrDataUnitdate,
                                        final MappingContext context) {

                        if (arrItemUnitdate.getFormat() == null) {
                            throw new SystemException("Nebyl odeslán formát dat", BaseCode.PROPERTY_NOT_EXIST).set("property", "format");
                        } else {
                            String format = arrItemUnitdate.getFormat();
                            if (!format.matches(
                                    "(" + PATTERN_UNIT_DATA + ")|(" + PATTERN_UNIT_DATA + INTERVAL_DELIMITER_UNIT_DATA
                                            + PATTERN_UNIT_DATA + ")")) {
                                throw new SystemException("Neplatný formát dat", BaseCode.PROPERTY_IS_INVALID).set("property", "format");
                            }
                        }
                        arrDataUnitdate.setFormat(arrItemUnitdate.getFormat());

                        try {
                            String value = arrItemUnitdate.getValueFrom();
                            if (value != null) {
                                value = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                                        .format(LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                            }
                            arrDataUnitdate.setValueFrom(value);
                        } catch (DateTimeParseException e) {
                            throw new SystemException("Nebyl zadan platny format datumu 'od'", e, BaseCode.PROPERTY_IS_INVALID).set("property", "valueFrom");
                        }

                        arrDataUnitdate.setValueFromEstimated(arrItemUnitdate.getValueFromEstimated());

                        try {
                            String value = arrItemUnitdate.getValueTo();
                            if (value != null) {
                                value = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                                        .format(LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                            }
                            arrDataUnitdate.setValueTo(value);
                        } catch (DateTimeParseException e) {
                            throw new SystemException("Nebyl zadan platny format datumu 'do'", e, BaseCode.PROPERTY_IS_INVALID).set("property", "valueTo");
                        }

                        if (arrItemUnitdate.getValueFrom() != null && arrItemUnitdate.getValueTo() != null) {
                            LocalDateTime from = LocalDateTime
                                    .parse(arrItemUnitdate.getValueFrom(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                            LocalDateTime to = LocalDateTime
                                    .parse(arrItemUnitdate.getValueTo(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                            if (from.isAfter(to)) {
                                throw new IllegalArgumentException("Neplatný interval ISO datumů: od > do");
                            }
                        } else if (arrItemUnitdate.getValueFrom() == null
                                && arrItemUnitdate.getValueTo() == null) {
                            throw new IllegalArgumentException("Nebyl zadán interval ISO datumů");
                        }

                        String value;

                        value = arrItemUnitdate.getValueFrom();
                        if (value != null) {
                            arrDataUnitdate.setNormalizedFrom(CalendarConverter.toSeconds(LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
                        } else {
                            arrDataUnitdate.setNormalizedFrom(Long.MIN_VALUE);
                        }
                        //TODO Hotfix to set normalizedFrom on memory object
                        arrItemUnitdate.setNormalizedFrom(arrDataUnitdate.getNormalizedFrom());

                        value = arrItemUnitdate.getValueTo();
                        if (value != null) {
                            arrDataUnitdate.setNormalizedTo(CalendarConverter.toSeconds(LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
                        } else {
                            arrDataUnitdate.setNormalizedTo(Long.MAX_VALUE);
                        }
                        //TODO Hotfix to set normalizedTo on memory object
                        arrItemUnitdate.setNormalizedTo(arrDataUnitdate.getNormalizedTo());

                        arrDataUnitdate.setValueToEstimated(arrItemUnitdate.getValueToEstimated());
                    }

                    @Override
                    public void mapBtoA(final ArrDataUnitdate arrDataUnitdate,
                                        final ArrItemUnitdate arrItemUnitdate,
                                        final MappingContext context) {
                        arrItemUnitdate.setValueFrom(arrDataUnitdate.getValueFrom());
                        arrItemUnitdate.setValueFromEstimated(arrDataUnitdate.getValueFromEstimated());
                        arrItemUnitdate.setValueTo(arrDataUnitdate.getValueTo());
                        arrItemUnitdate.setValueToEstimated(arrDataUnitdate.getValueToEstimated());
                        arrItemUnitdate.setFormat(arrDataUnitdate.getFormat());
                        arrItemUnitdate.setNormalizedTo(arrDataUnitdate.getNormalizedTo());
                        arrItemUnitdate.setNormalizedFrom(arrDataUnitdate.getNormalizedFrom());
                    }
                }).register();

        factory.classMap(ArrDataUnitdate.class, ArrDataUnitdate.class).byDefault()
                .customize(new CustomMapper<ArrDataUnitdate, ArrDataUnitdate>() {
                    @Override
                    public void mapAtoB(final ArrDataUnitdate arrDataUnitdate,
                                        final ArrDataUnitdate arrDataUnitdateNew,
                                        final MappingContext context) {
                        arrDataUnitdateNew.setDataType(arrDataUnitdate.getDataType());
                        arrDataUnitdateNew.setValueFrom(arrDataUnitdate.getValueFrom());
                        arrDataUnitdateNew.setValueFromEstimated(arrDataUnitdate.getValueFromEstimated());
                        arrDataUnitdateNew.setValueTo(arrDataUnitdate.getValueTo());
                        arrDataUnitdateNew.setValueToEstimated(arrDataUnitdate.getValueToEstimated());
                        arrDataUnitdateNew.setFormat(arrDataUnitdate.getFormat());

                                // Proc to tady je?
                        String value;

                        value = arrDataUnitdate.getValueFrom();
                        if (value != null) {
                            arrDataUnitdate.setNormalizedFrom(CalendarConverter.toSeconds(LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
                        } else {
                            arrDataUnitdate.setNormalizedFrom(Long.MIN_VALUE);
                                }
                        arrDataUnitdate.setNormalizedFrom(arrDataUnitdate.getNormalizedFrom());

                        value = arrDataUnitdate.getValueTo();
                        if (value != null) {
                            arrDataUnitdate.setNormalizedTo(CalendarConverter.toSeconds(LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
                        } else {
                            arrDataUnitdate.setNormalizedTo(Long.MAX_VALUE);
                        }
                        arrDataUnitdate.setNormalizedTo(arrDataUnitdate.getNormalizedTo());

                    }
                }).register();
    }

    /**
     * Nadefinování pravidel pro převod formátu Unitid.
     */
    private void defineMapUnitid() {
        factory.classMap(ArrItemUnitid.class, ArrDataUnitid.class)
                .customize(new CustomMapper<ArrItemUnitid, ArrDataUnitid>() {

                    @Override
                    public void mapAtoB(final ArrItemUnitid arrItemUnitid,
                                        final ArrDataUnitid arrDataUnitid,
                                        final MappingContext context) {
                        arrDataUnitid.setUnitId(arrItemUnitid.getValue());
                    }

                    @Override
                    public void mapBtoA(final ArrDataUnitid arrDataUnitid,
                                        final ArrItemUnitid arrItemUnitid,
                                        final MappingContext context) {
                        arrItemUnitid.setValue(arrDataUnitid.getUnitId());
                    }
                }).register();

        factory.classMap(ArrDataUnitid.class, ArrDataUnitid.class)
                .customize(new CustomMapper<ArrDataUnitid, ArrDataUnitid>() {
                    @Override
                    public void mapAtoB(final ArrDataUnitid arrDataUnitid,
                                        final ArrDataUnitid arrDataUnitidNew,
                                        final MappingContext context) {
                        arrDataUnitidNew.setDataType(arrDataUnitid.getDataType());
                        //arrDataUnitidNew.setItem(arrDataUnitid.getItem());
                        arrDataUnitidNew.setUnitId(arrDataUnitid.getUnitId());
                    }
                }).register();
    }

    /**
     * Nadefinování pravidel pro převod formátu Decimal.
     */
    private void defineMapDecimal() {
        factory.classMap(ArrItemDecimal.class, ArrDataDecimal.class)
                .customize(new CustomMapper<ArrItemDecimal, ArrDataDecimal>() {

                    @Override
                    public void mapAtoB(final ArrItemDecimal arrItemDecimal,
                                        final ArrDataDecimal arrDataDecimal,
                                        final MappingContext context) {
                        arrDataDecimal.setValue(arrItemDecimal.getValue());
                    }

                    @Override
                    public void mapBtoA(final ArrDataDecimal arrDataDecimal,
                                        final ArrItemDecimal arrItemDecimal,
                                        final MappingContext context) {
                        arrItemDecimal.setValue(arrDataDecimal.getValue());
                    }
                }).register();

        factory.classMap(ArrDataDecimal.class, ArrDataDecimal.class)
                .customize(new CustomMapper<ArrDataDecimal, ArrDataDecimal>() {
                    @Override
                    public void mapAtoB(final ArrDataDecimal arrDataDecimal,
                                        final ArrDataDecimal arrDataDecimalNew,
                                        final MappingContext context) {
                        arrDataDecimalNew.setDataType(arrDataDecimal.getDataType());
                        //arrDataDecimalNew.setItem(arrDataDecimal.getItem());
                        arrDataDecimalNew.setValue(arrDataDecimal.getValue());
                    }
                }).register();
    }

    /**
     * Nadefinování pravidel pro převod formátu Enum.
     */
    private void defineMapEnum() {
        factory.classMap(ArrItemEnum.class, ArrDataNull.class)
                .customize(new CustomMapper<ArrItemEnum, ArrDataNull>() {

                    @Override
                    public void mapAtoB(final ArrItemEnum arrItemEnum,
                                        final ArrDataNull arrDataNull,
                                        final MappingContext context) {
                    }

                    @Override
                    public void mapBtoA(final ArrDataNull arrDataNull,
                                        final ArrItemEnum arrItemEnum,
                                        final MappingContext context) {

                    }
                }).register();

        factory.classMap(ArrDataNull.class, ArrDataNull.class).customize(new CustomMapper<ArrDataNull, ArrDataNull>() {
            @Override
            public void mapAtoB(final ArrDataNull arrDataNull, final ArrDataNull arrDataNullNew, final MappingContext context) {
                arrDataNullNew.setDataType(arrDataNull.getDataType());
                //arrDataNullNew.setItem(arrDataNull.getItem());
            }
        }).register();
    }

    /**
     * Inicializace mapy pro všechny repository.
     */
    private void initMapRepository() {
        mapRepository = new LinkedHashMap<>();
        mapRepository.put(ArrDataCoordinates.class, dataCoordinatesRepository);
        mapRepository.put(ArrDataInteger.class, dataIntegerRepository);
        mapRepository.put(ArrDataRecordRef.class, dataRecordRefRepository);
        mapRepository.put(ArrDataString.class, dataStringRepository);
        mapRepository.put(ArrDataText.class, dataTextRepository);
        mapRepository.put(ArrDataUnitdate.class, dataUnitdateRepository);
        mapRepository.put(ArrDataUnitid.class, dataUnitidRepository);
        mapRepository.put(ArrDataDecimal.class, dataDecimalRepository);
        mapRepository.put(ArrDataStructureRef.class, dataStructureRefRepository);
        mapRepository.put(ArrDataFileRef.class, dataFileRefRepository);
        mapRepository.put(ArrDataNull.class, dataNullRepository);
        mapRepository.put(ArrDataJsonTable.class, dataJsonTableRepository);
        mapRepository.put(ArrDataDate.class, dataDateRepository);
        mapRepository.put(ArrDataUriRef.class, dataUriRefRepository);
        mapRepository.put(ArrDataBit.class, dataBitRepository);
    }

    /**
     * Vytvoření objektu atributu s hodnotou atributu.
     *
     * @param descItem atributu bez dat
     * @return výsledný atributu s daty
     */
    @Deprecated
    public ArrDescItem getDescItem(final ArrDescItem descItem) {

        return descItem;
    }

    /**
     * Připojení hodnot k záznamu atributu.
     *
     * @param descItems seznam holých hodnot atributů
     * @return seznam převedených hodnot atributů
     */
    public List<ArrDescItem> getDescItems(final Collection<ArrDescItem> descItems){
        List<ArrDescItem> result = new ArrayList<>(descItems.size());
        if(CollectionUtils.isEmpty(descItems)){
            return result;
        }

        result.addAll(descItems);
        return result;
    }

	/**
	 * Save data
	 *
	 * @param data
	 *            Data to be saved. Data can be null
	 * @return
	 */
	public ArrData saveData(RulItemType itemType, ArrData data) {
		if (data == null) {
			return null;
		}
		// Check data
		if (data instanceof ArrDataJsonTable) {
			checkJsonTableData(((ArrDataJsonTable) data).getValue(), (List<ElzaColumn>) itemType.getViewDefinition());
		}

        if(data instanceof ArrDataUriRef) {
            ArrDataUriRef dataTemp = (ArrDataUriRef) data;
            String uriRefValue = dataTemp.getUriRefValue();
            if(StringUtils.isEmpty(uriRefValue)) {
                throw new IllegalArgumentException("Nebyl zadán odkaz, nebo je odkaz prázdný");
            }
            if (!uriRefValue.matches(PATTERN_PROTOCOL)) {
                throw new BusinessException("Zadaný odkaz URI není platný, hodnota: " + uriRefValue, BaseCode.INVALID_URI)
                    .set("uri", uriRefValue);
            }
            URI tempUri = URI.create(uriRefValue).normalize();
            dataTemp.setSchema(tempUri.getScheme());

            if(!dataTemp.isDeletingProcess() && dataTemp.getSchema().equals(ELZA_NODE)) {
                ArrNode node = nodeRepository.findOneByUuid(tempUri.getAuthority()); //hledání podle UUID
                if(node != null) {
                    dataTemp.setArrNode(node);
                }
            }
        }

		// Set data type
		// dataType is not set when object is received as JSON from client
		if (data.getDataType() == null) {
			data.setDataType(itemType.getDataType());
		}

		// Get repository
		JpaRepository dataRepos = mapRepository.get(data.getClass());
		if (dataRepos == null) {
			throw new NotImplementedException("Nebyla namapována repozitory pro datový typ: " + data.getClass());
		}
		return (ArrData) dataRepos.save(data);
	}

	/**
	 * Save data as new
	 *
	 * @param itemType
	 * @param srcData
	 *            source data object. Can be null
	 * @return
	 */
	// TODO: copy data types based on itemType such code already somewhere exists
	public ArrData saveDataAsNew(RulItemType itemType, ArrData srcData) {
		if (srcData == null) {
			return null;
		}
		// create new instance
		ArrData dataNew;
		if (srcData instanceof ArrDataCoordinates) {
			dataNew = facade.map(srcData, ArrDataCoordinates.class);
		} else if (srcData instanceof ArrDataInteger) {
			dataNew = facade.map(srcData, ArrDataInteger.class);
		} else if (srcData instanceof ArrDataRecordRef) {
			dataNew = facade.map(srcData, ArrDataRecordRef.class);
		} else if (srcData instanceof ArrDataString) {
			dataNew = facade.map(srcData, ArrDataString.class);
		} else if (srcData instanceof ArrDataText) {
			dataNew = facade.map(srcData, ArrDataText.class);
		} else if (srcData instanceof ArrDataUnitdate) {
			dataNew = facade.map(srcData, ArrDataUnitdate.class);
		} else if (srcData instanceof ArrDataDate) {
            dataNew = facade.map(srcData, ArrDataDate.class);
        } else if (srcData instanceof ArrDataUnitid) {
			dataNew = facade.map(srcData, ArrDataUnitid.class);
		} else if (srcData instanceof ArrDataDecimal) {
			dataNew = facade.map(srcData, ArrDataDecimal.class);
		} else if (srcData instanceof ArrDataFileRef) {
			dataNew = facade.map(srcData, ArrDataFileRef.class);
		} else if (srcData instanceof ArrDataStructureRef) {
			dataNew = facade.map(srcData, ArrDataStructureRef.class);
		} else if (srcData instanceof ArrDataNull) {
			dataNew = facade.map(srcData, ArrDataNull.class);
		} else if (srcData instanceof ArrDataJsonTable) {
			dataNew = facade.map(srcData, ArrDataJsonTable.class);
		} else if (srcData instanceof ArrDataUriRef) {
		    dataNew = facade.map(srcData, ArrDataUriRef.class);
        } else if (srcData instanceof ArrDataBit) {
            dataNew = facade.map(srcData, ArrDataBit.class);
        } else {
			throw new NotImplementedException(
			        "Nebyl namapován datový typ: " + srcData.getClass().getName());
		}
		//?? Have to be same as type in srcData - have to be refactorized
		dataNew.setDataType(itemType.getDataType());
		return saveData(itemType, dataNew);
	}

    /**
     * Uloží hodnotu atributu i s daty.
     *
     * @param descItem         hodnota atributu
     * @param createNewVersion vytvořit novou verzi?
     *                         true - vytvoří novou hodnoty atributu
     *                         false - načte původní hodnotu a upraví jí podle nové
     * @return uložená hodnota atributu
     */
    public ArrDescItem saveItemVersionWithData(final ArrDescItem descItem, final Boolean createNewVersion) {
        ArrData data = descItem.getData();
        ArrData savedData;

        if (data != null) {
            if (data instanceof ArrDataJsonTable) {
                checkJsonTableData(((ArrDataJsonTable) data).getValue(), (List<ElzaColumn>) descItem.getItemType().getViewDefinition());
            }

            if(data instanceof ArrDataUriRef) {
                ArrDataUriRef dataTemp = (ArrDataUriRef) data;
                String uriRefValue = dataTemp.getUriRefValue();
                if (StringUtils.isEmpty(uriRefValue)) {
                    throw new IllegalArgumentException("Nebyl zadán odkaz, nebo je odkaz prázdný");
                }
                if (!uriRefValue.matches(PATTERN_PROTOCOL)) {
                    throw new BusinessException("Zadaný odkaz URI není platný, hodnota: " + uriRefValue, BaseCode.INVALID_URI)
                        .set("uri", uriRefValue);
                }
                URI tempUri = URI.create(uriRefValue).normalize();
                dataTemp.setDataType(descItem.getItemType().getDataType());
                if (StringUtils.isEmpty(tempUri.getScheme())) {
                    throw new BusinessException("Nebylo zadáno schéma v souladu s RFC2396, hodnota: " + uriRefValue, BaseCode.PROPERTY_IS_INVALID)
                        .set("uri", uriRefValue);
                }
                dataTemp.setSchema(tempUri.getScheme());

                if(dataTemp.getSchema().equals(ELZA_NODE)) {
                    ArrNode node = nodeRepository.findOneByUuid(tempUri.getAuthority()); //hledání podle UUID
                    if(node != null) {
                        dataTemp.setArrNode(node);
                    }
                }
            }

            ArrData dataNew;
            if (createNewVersion) {
                dataNew = saveDataAsNew(descItem.getItemType(), data);
                descItem.setData(dataNew);
            } else {
                dataNew = descItem.getData();
            }

            savedData = saveData(descItem.getItemType(), dataNew);
        } else {
        	savedData = null;
        }
        descItem.setData(savedData);

        return descItemRepository.save(descItem);
    }

    /**
     * Formátuje výstupní hodnotu dat pokud je to vyžadováno (existuje podmínka v kontextu.
     *
     * @param context kontext
     * @param value   hodnota pro naformátování
     * @return upravená hodnota
     */
    private String formatString(final MappingContext context, final String value) {
        String valueRet = value;
        if (context != null) {
            String format = (String) context.getProperty(PROPERTY_FORMAT);
            if (format != null) {
                String stringValue = value;
                if (stringValue != null && stringValue.length() > 250 && format != null
                        && ArrangementController.FORMAT_ATTRIBUTE_SHORT.equals(format)) {
                    valueRet = stringValue.substring(0, 250);
                }
            }
        }
        return valueRet;
    }

    /**
     * Kontrola sloupců v JSON tabulce.
     *
     * @param table   kontrolovaná tabulka
     * @param columns seznam definicí sloupců
     */
    static public void checkJsonTableData(@NotNull final ElzaTable table,
                                   @NotEmpty final List<ElzaColumn> columns) {
        Map<String, ElzaColumn.DataType> typeMap = columns.stream().collect(Collectors.toMap(ElzaColumn::getCode, ElzaColumn::getDataType));
        for (ElzaRow row : table.getRows()) {
            for (Map.Entry<String, String> entry : row.getValues().entrySet()) {
                ElzaColumn.DataType dataType = typeMap.get(entry.getKey());
                if (dataType == null) {
                    throw new BusinessException("Sloupec s kódem '" + entry.getKey() +  "' neexistuje v definici tabulky", BaseCode.PROPERTY_IS_INVALID)
                    .set("property", entry.getKey());
                }

                switch (dataType) {
                    case INTEGER:
                        try {
                            Integer.parseInt(entry.getValue());
                        } catch (NumberFormatException e) {
                            throw new BusinessException("Neplatný vstup: Hodnota sloupce '" + entry.getKey() + "' musí být celé číslo", e,
                                    BaseCode.PROPERTY_IS_INVALID)
                            .set("property", entry.getKey());
                        }
                        break;

                    case TEXT:
                        if (entry.getValue() == null) {
                            throw new BusinessException("Neplatný vstup: Hodnota sloupce '" + entry.getKey() + "' nesmí být null",
                                    BaseCode.PROPERTY_IS_INVALID)
                            .set("property", entry.getKey());
                        }
                        break;

                    default:
                        throw new BusinessException("Neznámý typ sloupce '" + dataType.name() + "' ve validaci JSON tabulky",
                                BaseCode.PROPERTY_IS_INVALID)
                        .set("property", dataType.name());
                }
            }
        }
    }
}
