package cz.tacr.elza.domain.factory;

import cz.tacr.elza.controller.ArrangementController;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataCoordinates;
import cz.tacr.elza.domain.ArrDataDecimal;
import cz.tacr.elza.domain.ArrDataFileRef;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataJsonTable;
import cz.tacr.elza.domain.ArrDataNull;
import cz.tacr.elza.domain.ArrDataPacketRef;
import cz.tacr.elza.domain.ArrDataPartyRef;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.ArrDataUnitid;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrItemCoordinates;
import cz.tacr.elza.domain.ArrItemDecimal;
import cz.tacr.elza.domain.ArrItemEnum;
import cz.tacr.elza.domain.ArrItemFileRef;
import cz.tacr.elza.domain.ArrItemFormattedText;
import cz.tacr.elza.domain.ArrItemInt;
import cz.tacr.elza.domain.ArrItemJsonTable;
import cz.tacr.elza.domain.ArrItemPacketRef;
import cz.tacr.elza.domain.ArrItemPartyRef;
import cz.tacr.elza.domain.ArrItemRecordRef;
import cz.tacr.elza.domain.ArrItemString;
import cz.tacr.elza.domain.ArrItemText;
import cz.tacr.elza.domain.ArrItemUnitdate;
import cz.tacr.elza.domain.ArrItemUnitid;
import cz.tacr.elza.domain.convertor.CalendarConverter;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.DataCoordinatesRepository;
import cz.tacr.elza.repository.DataDecimalRepository;
import cz.tacr.elza.repository.DataFileRefRepository;
import cz.tacr.elza.repository.DataIntegerRepository;
import cz.tacr.elza.repository.DataJsonTableRepository;
import cz.tacr.elza.repository.DataNullRepository;
import cz.tacr.elza.repository.DataPacketRefRepository;
import cz.tacr.elza.repository.DataPartyRefRepository;
import cz.tacr.elza.repository.DataRecordRefRepository;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.DataStringRepository;
import cz.tacr.elza.repository.DataTextRepository;
import cz.tacr.elza.repository.DataUnitdateRepository;
import cz.tacr.elza.repository.DataUnitidRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.service.ItemService;
import ma.glasnost.orika.CustomMapper;
import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.impl.DefaultMapperFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.NotImplementedException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;


/**
 * Factory pro vytváření a manipulaci s atributama a jejich hodnotama.
 *
 * @author Martin Šlapa
 * @since 16.9.2015
 */
@Component
public class DescItemFactory implements InitializingBean {

    private final String PROPERTY_FORMAT = "format";

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
    private DataPartyRefRepository dataPartyRefRepository;

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
    private DataRepository dataRepository;

    @Autowired
    private DataPacketRefRepository dataPacketRefRepository;

    @Autowired
    private DataFileRefRepository dataFileRefRepository;

    @Autowired
    private DataNullRepository dataNullRepository;

    @Autowired
    private DataJsonTableRepository dataJsonTableRepository;

    @Autowired
    private ItemService itemService;

    public DescItemFactory() {
    }


    @Override
    public void afterPropertiesSet() throws Exception {
        factory = new DefaultMapperFactory.Builder().build();

        defineMapCoordinates();
        defineMapFormattedText();
        defineMapInt();
        defineMapPartyRef();
        defineMapRecordRef();
        defineMapString();
        defineMapText();
        defineMapUnitdate();
        defineMapUnitid();
        defineMapDecimal();
        defineMapPacketRef();
        defineMapFileRef();
        defineMapEnum();
        defineMapJsonTable();

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
     * Nadefinování pravidel pro převod formátu FormattedText.
     */
    private void defineMapFormattedText() {
        factory.classMap(ArrItemFormattedText.class, ArrDataText.class)
                .customize(new CustomMapper<ArrItemFormattedText, ArrDataText>() {

                    @Override
                    public void mapAtoB(final ArrItemFormattedText arrItemFormattedText,
                                        final ArrDataText arrDataText,
                                        final MappingContext context) {
                        arrDataText.setValue(arrItemFormattedText.getValue());
                    }

                    @Override
                    public void mapBtoA(final ArrDataText arrDataText,
                                        final ArrItemFormattedText arrItemFormattedText,
                                        final MappingContext context) {
                        String formattedValue = formatString(context, arrDataText.getValue());
                        arrItemFormattedText.setValue(formattedValue);
                    }
                }).register();

        factory.classMap(ArrDataText.class, ArrDataText.class).customize(new CustomMapper<ArrDataText, ArrDataText>() {
            @Override
            public void mapAtoB(final ArrDataText arrDataText, final ArrDataText arrDataTextNew, final MappingContext context) {
                arrDataTextNew.setDataType(arrDataText.getDataType());
                //arrDataTextNew.setItem(arrDataText.getItem());
                arrDataTextNew.setValue(arrDataText.getValue());
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
                        arrDataInteger.setValue(arrItemInt.getValue());
                    }

                    @Override
                    public void mapBtoA(final ArrDataInteger arrDataInteger,
                                        final ArrItemInt arrItemInt,
                                        final MappingContext context) {
                        arrItemInt.setValue(arrDataInteger.getValue());
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
                        arrDataIntegerNew.setValue(arrDataInteger.getValue());
                    }
                }).register();
    }

    /**
     * Nadefinování pravidel pro převod formátu PartyRef.
     */
    private void defineMapPartyRef() {
        factory.classMap(ArrItemPartyRef.class, ArrDataPartyRef.class)
                .customize(new CustomMapper<ArrItemPartyRef, ArrDataPartyRef>() {

                    @Override
                    public void mapAtoB(final ArrItemPartyRef arrItemPartyRef,
                                        final ArrDataPartyRef arrDataPartyRef,
                                        final MappingContext context) {
                        arrDataPartyRef.setParty(arrItemPartyRef.getParty());
                    }

                    @Override
                    public void mapBtoA(final ArrDataPartyRef arrDataPartyRef,
                                        final ArrItemPartyRef arrItemPartyRef,
                                        final MappingContext context) {
                        arrItemPartyRef.setParty(arrDataPartyRef.getParty());
                    }

                }).register();

        factory.classMap(ArrDataPartyRef.class, ArrDataPartyRef.class)
                .customize(new CustomMapper<ArrDataPartyRef, ArrDataPartyRef>() {
                    @Override
                    public void mapAtoB(final ArrDataPartyRef arrDataPartyRef,
                                        final ArrDataPartyRef arrDataPartyRefNew,
                                        final MappingContext context) {
                        arrDataPartyRefNew.setDataType(arrDataPartyRef.getDataType());
                        //arrDataPartyRefNew.setItem(arrDataPartyRef.getItem());
                        arrDataPartyRefNew.setParty(arrDataPartyRef.getParty());
                    }
                }).register();
    }

    /**
     * Nadefinování pravidel pro převod formátu PacketRef.
     */
    private void defineMapPacketRef() {
        factory.classMap(ArrItemPacketRef.class, ArrDataPacketRef.class)
                .customize(new CustomMapper<ArrItemPacketRef, ArrDataPacketRef>() {

                    @Override
                    public void mapAtoB(final ArrItemPacketRef arrDescItemPartyRef,
                                        final ArrDataPacketRef arrDataPartyRef,
                                        final MappingContext context) {
                        arrDataPartyRef.setPacket(arrDescItemPartyRef.getPacket());
                    }

                    @Override
                    public void mapBtoA(final ArrDataPacketRef arrDataPartyRef,
                                        final ArrItemPacketRef arrDescItemPartyRef,
                                        final MappingContext context) {
                        arrDescItemPartyRef.setPacket(arrDataPartyRef.getPacket());
                    }

                }).register();

        factory.classMap(ArrDataPacketRef.class, ArrDataPacketRef.class)
                .customize(new CustomMapper<ArrDataPacketRef, ArrDataPacketRef>() {
                    @Override
                    public void mapAtoB(final ArrDataPacketRef arrDataPartyRef,
                                        final ArrDataPacketRef arrDataPartyRefNew,
                                        final MappingContext context) {
                        arrDataPartyRefNew.setDataType(arrDataPartyRef.getDataType());
                        //arrDataPartyRefNew.setItem(arrDataPartyRef.getItem());
                        arrDataPartyRefNew.setPacket(arrDataPartyRef.getPacket());
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
                    public void mapAtoB(final ArrItemFileRef arrDescItemPartyRef,
                                        final ArrDataFileRef arrDataPartyRef,
                                        final MappingContext context) {
                        arrDataPartyRef.setFile(arrDescItemPartyRef.getFile());
                    }

                    @Override
                    public void mapBtoA(final ArrDataFileRef arrDataPartyRef,
                                        final ArrItemFileRef arrDescItemPartyRef,
                                        final MappingContext context) {
                        arrDescItemPartyRef.setFile(arrDataPartyRef.getFile());
                    }

                }).register();

        factory.classMap(ArrDataFileRef.class, ArrDataFileRef.class)
                .customize(new CustomMapper<ArrDataFileRef, ArrDataFileRef>() {
                    @Override
                    public void mapAtoB(final ArrDataFileRef arrDataPartyRef,
                                        final ArrDataFileRef arrDataPartyRefNew,
                                        final MappingContext context) {
                        arrDataPartyRefNew.setDataType(arrDataPartyRef.getDataType());
                        //arrDataPartyRefNew.setItem(arrDataPartyRef.getItem());
                        arrDataPartyRefNew.setFile(arrDataPartyRef.getFile());
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
                        arrDataRecordRef.setRecord(arrItemRecordRef.getRecord());
                    }

                    @Override
                    public void mapBtoA(final ArrDataRecordRef arrDataRecordRef,
                                        final ArrItemRecordRef arrItemRecordRef,
                                        final MappingContext context) {
                        arrItemRecordRef.setRecord(arrDataRecordRef.getRecord());
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
                        arrDataString.setValue(arrItemString.getValue());
                    }

                    @Override
                    public void mapBtoA(final ArrDataString arrDataString,
                                        final ArrItemString arrItemString,
                                        final MappingContext context) {
                        String formattedValue = formatString(context, arrDataString.getValue());
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
                        arrDataStringNew.setValue(arrDataString.getValue());
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
                        arrDataText.setValue(arrItemText.getValue());
                    }

                    @Override
                    public void mapBtoA(final ArrDataText arrDataText,
                                        final ArrItemText arrItemText,
                                        final MappingContext context) {
                        String formattedValue = formatString(context, arrDataText.getValue());
                        arrItemText.setValue(formattedValue);
                    }
                }).register();

        factory.classMap(ArrDataText.class, ArrDataText.class).customize(new CustomMapper<ArrDataText, ArrDataText>() {
            @Override
            public void mapAtoB(final ArrDataText arrDataText, final ArrDataText arrDataTextNew, final MappingContext context) {
                arrDataTextNew.setDataType(arrDataText.getDataType());
                //arrDataTextNew.setItem(arrDataText.getItem());
                arrDataTextNew.setValue(arrDataText.getValue());
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

                        if (arrItemUnitdate.getCalendarType() == null) {
                            throw new SystemException("Nebyl zvolen kalendar", BaseCode.PROPERTY_NOT_EXIST).set("property", "calendarType");
                        }
                        arrDataUnitdate.setCalendarType(arrItemUnitdate.getCalendarType());

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

                        String codeCalendar = arrItemUnitdate.getCalendarType().getCode();
                        CalendarConverter.CalendarType calendarType = CalendarConverter.CalendarType.valueOf(codeCalendar);

                        String value;

                        value = arrItemUnitdate.getValueFrom();
                        if (value != null) {
                            arrDataUnitdate.setNormalizedFrom(CalendarConverter.toSeconds(calendarType, LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
                        } else {
                            arrDataUnitdate.setNormalizedFrom(Long.MIN_VALUE);
                        }
                        //TODO Hotfix to set normalizedFrom on memory object
                        arrItemUnitdate.setNormalizedFrom(arrDataUnitdate.getNormalizedFrom());

                        value = arrItemUnitdate.getValueTo();
                        if (value != null) {
                            arrDataUnitdate.setNormalizedTo(CalendarConverter.toSeconds(calendarType, LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
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
                        arrItemUnitdate.setCalendarType(arrDataUnitdate.getCalendarType());
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
                        arrDataUnitdateNew.setCalendarType(arrDataUnitdate.getCalendarType());
                        arrDataUnitdateNew.setValueFrom(arrDataUnitdate.getValueFrom());
                        arrDataUnitdateNew.setValueFromEstimated(arrDataUnitdate.getValueFromEstimated());
                        arrDataUnitdateNew.setValueTo(arrDataUnitdate.getValueTo());
                        arrDataUnitdateNew.setValueToEstimated(arrDataUnitdate.getValueToEstimated());
                        arrDataUnitdateNew.setFormat(arrDataUnitdate.getFormat());

                        String value;

                        String codeCalendar = arrDataUnitdate.getCalendarType().getCode();
                        CalendarConverter.CalendarType calendarType = CalendarConverter.CalendarType.valueOf(codeCalendar);

                        value = arrDataUnitdate.getValueFrom();
                        if (value != null) {
                            arrDataUnitdate.setNormalizedFrom(CalendarConverter.toSeconds(calendarType, LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
                        } else {
                            arrDataUnitdate.setNormalizedFrom(Long.MIN_VALUE);
                        }
                        arrDataUnitdate.setNormalizedFrom(arrDataUnitdate.getNormalizedFrom());

                        value = arrDataUnitdate.getValueTo();
                        if (value != null) {
                            arrDataUnitdate.setNormalizedTo(CalendarConverter.toSeconds(calendarType, LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
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
                        arrDataUnitid.setValue(arrItemUnitid.getValue());
                    }

                    @Override
                    public void mapBtoA(final ArrDataUnitid arrDataUnitid,
                                        final ArrItemUnitid arrItemUnitid,
                                        final MappingContext context) {
                        arrItemUnitid.setValue(arrDataUnitid.getValue());
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
                        arrDataUnitidNew.setValue(arrDataUnitid.getValue());
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
        mapRepository.put(ArrDataPartyRef.class, dataPartyRefRepository);
        mapRepository.put(ArrDataRecordRef.class, dataRecordRefRepository);
        mapRepository.put(ArrDataString.class, dataStringRepository);
        mapRepository.put(ArrDataText.class, dataTextRepository);
        mapRepository.put(ArrDataUnitdate.class, dataUnitdateRepository);
        mapRepository.put(ArrDataUnitid.class, dataUnitidRepository);
        mapRepository.put(ArrDataDecimal.class, dataDecimalRepository);
        mapRepository.put(ArrDataPacketRef.class, dataPacketRefRepository);
        mapRepository.put(ArrDataFileRef.class, dataFileRefRepository);
        mapRepository.put(ArrDataNull.class, dataNullRepository);
        mapRepository.put(ArrDataJsonTable.class, dataJsonTableRepository);
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
     * Uloží hodnotu atributu bez dat.
     *
     * @param descItem hodnota atributu
     * @return nova hodnota atributu
     */
    public ArrDescItem saveDescItem(final ArrDescItem descItem) {
        return descItemRepository.save(descItem);
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
    public ArrDescItem saveDescItemWithData(final ArrDescItem descItem, final Boolean createNewVersion) {
        ArrData data = descItem.getData();

        if (data != null) {
            if (data instanceof ArrDataJsonTable) {
                itemService.checkJsonTableData(((ArrDataJsonTable) data).getValue(), descItem.getItemType().getColumnsDefinition());
            }

            ArrData dataNew;
            if (createNewVersion) {
                if (data instanceof ArrDataCoordinates) {
                    dataNew = facade.map(data, ArrDataCoordinates.class);
                } else if (data instanceof ArrDataInteger) {
                    dataNew = facade.map(data, ArrDataInteger.class);
                } else if (data instanceof ArrDataPartyRef) {
                    dataNew = facade.map(data, ArrDataPartyRef.class);
                } else if (data instanceof ArrDataRecordRef) {
                    dataNew = facade.map(data, ArrDataRecordRef.class);
                } else if (data instanceof ArrDataString) {
                    dataNew = facade.map(data, ArrDataString.class);
                } else if (data instanceof ArrDataText) {
                    dataNew = facade.map(data, ArrDataText.class);
                } else if (data instanceof ArrDataUnitdate) {
                    dataNew = facade.map(data, ArrDataUnitdate.class);
                } else if (data instanceof ArrDataUnitid) {
                    dataNew = facade.map(data, ArrDataUnitid.class);
                } else if (data instanceof ArrDataDecimal) {
                    dataNew = facade.map(data, ArrDataDecimal.class);
                } else if (data instanceof ArrDataFileRef) {
                    dataNew = facade.map(data, ArrDataFileRef.class);
                } else if (data instanceof ArrDataPacketRef) {
                    dataNew = facade.map(data, ArrDataPacketRef.class);
                } else if (data instanceof ArrDataNull) {
                    dataNew = facade.map(data, ArrDataNull.class);
                } else if (data instanceof ArrDataJsonTable) {
                    dataNew = facade.map(data, ArrDataJsonTable.class);
                } else {
                    throw new NotImplementedException("Nebyl namapován datový typ: " + descItem.getClass().getName() + ", data: " + data);
                }
                dataNew.setDataType(descItem.getItemType().getDataType());
                descItem.setData(dataNew);
            } else {
                dataNew = descItem.getData();
            }

            try {
                mapRepository.get(dataNew.getClass()).save(dataNew);
            } catch (NullPointerException e) {
                throw new NotImplementedException("Nebyla namapována repozitory pro datový typ");
            }
        }

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

}
