package cz.tacr.elza.domain.factory;

import cz.tacr.elza.api.controller.ArrangementManager;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.domain.convertor.CalendarConverter;
import cz.tacr.elza.repository.*;
import ma.glasnost.orika.CustomMapper;
import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.impl.DefaultMapperFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.NotImplementedException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;


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
    private LinkedHashMap<Class, JpaRepository> mapRepository;

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
                    public void mapAtoB(ArrItemCoordinates arrDescItemCoordinates,
                                        ArrDataCoordinates arrDataCoordinates,
                                        MappingContext context) {
                        arrDataCoordinates.setValue(arrDescItemCoordinates.getValue());
                    }

                    @Override
                    public void mapBtoA(ArrDataCoordinates arrDataCoordinates,
                                        ArrItemCoordinates arrDescItemExtCoordinates,
                                        MappingContext context) {
                        arrDescItemExtCoordinates.setValue(arrDataCoordinates.getValue());
                    }

                }).register();

        factory.classMap(ArrDataCoordinates.class, ArrDataCoordinates.class)
                .customize(new CustomMapper<ArrDataCoordinates, ArrDataCoordinates>() {
                    @Override
                    public void mapAtoB(ArrDataCoordinates arrDataCoordinates,
                                        ArrDataCoordinates arrDataCoordinatesNew,
                                        MappingContext context) {
                        arrDataCoordinatesNew.setDataType(arrDataCoordinates.getDataType());
                        arrDataCoordinatesNew.setItem(arrDataCoordinates.getItem());
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
                    public void mapAtoB(ArrItemJsonTable arrItemJsonTable,
                                        ArrDataJsonTable arrDataJsonTable,
                                        MappingContext context) {
                        arrDataJsonTable.setValue(arrItemJsonTable.getValue());
                    }

                    @Override
                    public void mapBtoA(ArrDataJsonTable arrDataJsonTable,
                                        ArrItemJsonTable arrItemJsonTable,
                                        MappingContext context) {
                        arrItemJsonTable.setValue(arrDataJsonTable.getValue());
                    }

                }).register();

        factory.classMap(ArrDataJsonTable.class, ArrDataJsonTable.class)
                .customize(new CustomMapper<ArrDataJsonTable, ArrDataJsonTable>() {
                    @Override
                    public void mapAtoB(ArrDataJsonTable arrDataJsonTable,
                                        ArrDataJsonTable arrDataJsonTableNew,
                                        MappingContext context) {
                        arrDataJsonTableNew.setDataType(arrDataJsonTable.getDataType());
                        arrDataJsonTableNew.setItem(arrDataJsonTable.getItem());
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
                    public void mapAtoB(ArrItemFormattedText arrItemFormattedText,
                                        ArrDataText arrDataText,
                                        MappingContext context) {
                        arrDataText.setValue(arrItemFormattedText.getValue());
                    }

                    @Override
                    public void mapBtoA(ArrDataText arrDataText,
                                        ArrItemFormattedText arrItemFormattedText,
                                        MappingContext context) {
                        String formattedValue = formatString(context, arrDataText.getValue());
                        arrItemFormattedText.setValue(formattedValue);
                    }
                }).register();

        factory.classMap(ArrDataText.class, ArrDataText.class).customize(new CustomMapper<ArrDataText, ArrDataText>() {
            @Override
            public void mapAtoB(ArrDataText arrDataText, ArrDataText arrDataTextNew, MappingContext context) {
                arrDataTextNew.setDataType(arrDataText.getDataType());
                arrDataTextNew.setItem(arrDataText.getItem());
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
                    public void mapAtoB(ArrItemInt arrItemInt,
                                        ArrDataInteger arrDataInteger,
                                        MappingContext context) {
                        arrDataInteger.setValue(arrItemInt.getValue());
                    }

                    @Override
                    public void mapBtoA(ArrDataInteger arrDataInteger,
                                        ArrItemInt arrItemInt,
                                        MappingContext context) {
                        arrItemInt.setValue(arrDataInteger.getValue());
                    }

                }).register();

        factory.classMap(ArrDataInteger.class, ArrDataInteger.class)
                .customize(new CustomMapper<ArrDataInteger, ArrDataInteger>() {
                    @Override
                    public void mapAtoB(ArrDataInteger arrDataInteger,
                                        ArrDataInteger arrDataIntegerNew,
                                        MappingContext context) {
                        arrDataIntegerNew.setDataType(arrDataInteger.getDataType());
                        arrDataIntegerNew.setItem(arrDataInteger.getItem());
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
                    public void mapAtoB(ArrItemPartyRef arrItemPartyRef,
                                        ArrDataPartyRef arrDataPartyRef,
                                        MappingContext context) {
                        arrDataPartyRef.setParty(arrItemPartyRef.getParty());
                    }

                    @Override
                    public void mapBtoA(ArrDataPartyRef arrDataPartyRef,
                                        ArrItemPartyRef arrItemPartyRef,
                                        MappingContext context) {
                        arrItemPartyRef.setParty(arrDataPartyRef.getParty());
                    }

                }).register();

        factory.classMap(ArrDataPartyRef.class, ArrDataPartyRef.class)
                .customize(new CustomMapper<ArrDataPartyRef, ArrDataPartyRef>() {
                    @Override
                    public void mapAtoB(ArrDataPartyRef arrDataPartyRef,
                                        ArrDataPartyRef arrDataPartyRefNew,
                                        MappingContext context) {
                        arrDataPartyRefNew.setDataType(arrDataPartyRef.getDataType());
                        arrDataPartyRefNew.setItem(arrDataPartyRef.getItem());
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
                    public void mapAtoB(ArrItemPacketRef arrDescItemPartyRef,
                                        ArrDataPacketRef arrDataPartyRef,
                                        MappingContext context) {
                        arrDataPartyRef.setPacket(arrDescItemPartyRef.getPacket());
                    }

                    @Override
                    public void mapBtoA(ArrDataPacketRef arrDataPartyRef,
                                        ArrItemPacketRef arrDescItemPartyRef,
                                        MappingContext context) {
                        arrDescItemPartyRef.setPacket(arrDataPartyRef.getPacket());
                    }

                }).register();

        factory.classMap(ArrDataPacketRef.class, ArrDataPacketRef.class)
                .customize(new CustomMapper<ArrDataPacketRef, ArrDataPacketRef>() {
                    @Override
                    public void mapAtoB(ArrDataPacketRef arrDataPartyRef,
                                        ArrDataPacketRef arrDataPartyRefNew,
                                        MappingContext context) {
                        arrDataPartyRefNew.setDataType(arrDataPartyRef.getDataType());
                        arrDataPartyRefNew.setItem(arrDataPartyRef.getItem());
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
                    public void mapAtoB(ArrItemFileRef arrDescItemPartyRef,
                                        ArrDataFileRef arrDataPartyRef,
                                        MappingContext context) {
                        arrDataPartyRef.setFile(arrDescItemPartyRef.getFile());
                    }

                    @Override
                    public void mapBtoA(ArrDataFileRef arrDataPartyRef,
                                        ArrItemFileRef arrDescItemPartyRef,
                                        MappingContext context) {
                        arrDescItemPartyRef.setFile(arrDataPartyRef.getFile());
                    }

                }).register();

        factory.classMap(ArrDataFileRef.class, ArrDataFileRef.class)
                .customize(new CustomMapper<ArrDataFileRef, ArrDataFileRef>() {
                    @Override
                    public void mapAtoB(ArrDataFileRef arrDataPartyRef,
                                        ArrDataFileRef arrDataPartyRefNew,
                                        MappingContext context) {
                        arrDataPartyRefNew.setDataType(arrDataPartyRef.getDataType());
                        arrDataPartyRefNew.setItem(arrDataPartyRef.getItem());
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
                    public void mapAtoB(ArrItemRecordRef arrItemRecordRef,
                                        ArrDataRecordRef arrDataRecordRef,
                                        MappingContext context) {
                        arrDataRecordRef.setRecord(arrItemRecordRef.getRecord());
                    }

                    @Override
                    public void mapBtoA(ArrDataRecordRef arrDataRecordRef,
                                        ArrItemRecordRef arrItemRecordRef,
                                        MappingContext context) {
                        arrItemRecordRef.setRecord(arrDataRecordRef.getRecord());
                    }

                }).register();

        factory.classMap(ArrDataRecordRef.class, ArrDataRecordRef.class)
                .customize(new CustomMapper<ArrDataRecordRef, ArrDataRecordRef>() {
                    @Override
                    public void mapAtoB(ArrDataRecordRef arrDataRecordRef,
                                        ArrDataRecordRef arrDataRecordRefNew,
                                        MappingContext context) {
                        arrDataRecordRefNew.setDataType(arrDataRecordRef.getDataType());
                        arrDataRecordRefNew.setItem(arrDataRecordRef.getItem());
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
                    public void mapAtoB(ArrItemString arrItemString,
                                        ArrDataString arrDataString,
                                        MappingContext context) {
                        arrDataString.setValue(arrItemString.getValue());
                    }

                    @Override
                    public void mapBtoA(ArrDataString arrDataString,
                                        ArrItemString arrItemString,
                                        MappingContext context) {
                        String formattedValue = formatString(context, arrDataString.getValue());
                        arrItemString.setValue(formattedValue);
                    }
                }).register();

        factory.classMap(ArrDataString.class, ArrDataString.class)
                .customize(new CustomMapper<ArrDataString, ArrDataString>() {
                    @Override
                    public void mapAtoB(ArrDataString arrDataString,
                                        ArrDataString arrDataStringNew,
                                        MappingContext context) {
                        arrDataStringNew.setDataType(arrDataString.getDataType());
                        arrDataStringNew.setItem(arrDataString.getItem());
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
                    public void mapAtoB(ArrItemText arrItemText,
                                        ArrDataText arrDataText,
                                        MappingContext context) {
                        arrDataText.setValue(arrItemText.getValue());
                    }

                    @Override
                    public void mapBtoA(ArrDataText arrDataText,
                                        ArrItemText arrItemText,
                                        MappingContext context) {
                        String formattedValue = formatString(context, arrDataText.getValue());
                        arrItemText.setValue(formattedValue);
                    }
                }).register();

        factory.classMap(ArrDataText.class, ArrDataText.class).customize(new CustomMapper<ArrDataText, ArrDataText>() {
            @Override
            public void mapAtoB(ArrDataText arrDataText, ArrDataText arrDataTextNew, MappingContext context) {
                arrDataTextNew.setDataType(arrDataText.getDataType());
                arrDataTextNew.setItem(arrDataText.getItem());
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
                    public void mapAtoB(ArrItemUnitdate arrItemUnitdate,
                                        ArrDataUnitdate arrDataUnitdate,
                                        MappingContext context) {

                        if (arrItemUnitdate.getFormat() == null) {
                            throw new IllegalArgumentException("Nebyl odeslán formát dat");
                        } else {
                            String format = arrItemUnitdate.getFormat();
                            if (!format.matches(
                                    "(" + PATTERN_UNIT_DATA + ")|(" + PATTERN_UNIT_DATA + INTERVAL_DELIMITER_UNIT_DATA
                                            + PATTERN_UNIT_DATA + ")")) {
                                throw new IllegalArgumentException("Neplatný formát dat");
                            }
                        }
                        arrDataUnitdate.setFormat(arrItemUnitdate.getFormat());

                        if (arrItemUnitdate.getCalendarType() == null) {
                            throw new IllegalArgumentException("Nebyl zvolen kalendar");
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
                            throw new IllegalArgumentException("Nebyl zadan platny format datumu 'od'", e);
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
                            throw new IllegalArgumentException("Nebyl zadan platny format datumu 'do'", e);
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

                        value = arrItemUnitdate.getValueTo();
                        if (value != null) {
                            arrDataUnitdate.setNormalizedTo(CalendarConverter.toSeconds(calendarType, LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
                        } else {
                            arrDataUnitdate.setNormalizedTo(Long.MAX_VALUE);
                        }

                        arrDataUnitdate.setValueToEstimated(arrItemUnitdate.getValueToEstimated());
                    }

                    @Override
                    public void mapBtoA(ArrDataUnitdate arrDataUnitdate,
                                        ArrItemUnitdate arrItemUnitdate,
                                        MappingContext context) {
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

        factory.classMap(ArrDataUnitdate.class, ArrDataUnitdate.class)
                .customize(new CustomMapper<ArrDataUnitdate, ArrDataUnitdate>() {
                    @Override
                    public void mapAtoB(ArrDataUnitdate arrDataUnitdate,
                                        ArrDataUnitdate arrDataUnitdateNew,
                                        MappingContext context) {
                        arrDataUnitdateNew.setDataType(arrDataUnitdate.getDataType());
                        arrDataUnitdateNew.setItem(arrDataUnitdate.getItem());
                        arrDataUnitdateNew.setCalendarType(arrDataUnitdate.getCalendarType());
                        arrDataUnitdateNew.setValueFrom(arrDataUnitdate.getValueFrom());
                        arrDataUnitdateNew.setValueFromEstimated(arrDataUnitdate.getValueFromEstimated());
                        arrDataUnitdateNew.setValueTo(arrDataUnitdate.getValueTo());
                        arrDataUnitdateNew.setValueToEstimated(arrDataUnitdate.getValueToEstimated());
                        arrDataUnitdateNew.setFormat(arrDataUnitdate.getFormat());
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
                    public void mapAtoB(ArrItemUnitid arrItemUnitid,
                                        ArrDataUnitid arrDataUnitid,
                                        MappingContext context) {
                        arrDataUnitid.setValue(arrItemUnitid.getValue());
                    }

                    @Override
                    public void mapBtoA(ArrDataUnitid arrDataUnitid,
                                        ArrItemUnitid arrItemUnitid,
                                        MappingContext context) {
                        arrItemUnitid.setValue(arrDataUnitid.getValue());
                    }
                }).register();

        factory.classMap(ArrDataUnitid.class, ArrDataUnitid.class)
                .customize(new CustomMapper<ArrDataUnitid, ArrDataUnitid>() {
                    @Override
                    public void mapAtoB(ArrDataUnitid arrDataUnitid,
                                        ArrDataUnitid arrDataUnitidNew,
                                        MappingContext context) {
                        arrDataUnitidNew.setDataType(arrDataUnitid.getDataType());
                        arrDataUnitidNew.setItem(arrDataUnitid.getItem());
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
                    public void mapAtoB(ArrItemDecimal arrItemDecimal,
                                        ArrDataDecimal arrDataDecimal,
                                        MappingContext context) {
                        arrDataDecimal.setValue(arrItemDecimal.getValue());
                    }

                    @Override
                    public void mapBtoA(ArrDataDecimal arrDataDecimal,
                                        ArrItemDecimal arrItemDecimal,
                                        MappingContext context) {
                        arrItemDecimal.setValue(arrDataDecimal.getValue());
                    }
                }).register();

        factory.classMap(ArrDataDecimal.class, ArrDataDecimal.class)
                .customize(new CustomMapper<ArrDataDecimal, ArrDataDecimal>() {
                    @Override
                    public void mapAtoB(ArrDataDecimal arrDataDecimal,
                                        ArrDataDecimal arrDataDecimalNew,
                                        MappingContext context) {
                        arrDataDecimalNew.setDataType(arrDataDecimal.getDataType());
                        arrDataDecimalNew.setItem(arrDataDecimal.getItem());
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
                    public void mapAtoB(ArrItemEnum arrItemEnum,
                                        ArrDataNull arrDataNull,
                                        MappingContext context) {
                    }

                    @Override
                    public void mapBtoA(ArrDataNull arrDataNull,
                                        ArrItemEnum arrItemEnum,
                                        MappingContext context) {

                    }
                }).register();

        factory.classMap(ArrDataNull.class, ArrDataNull.class).customize(new CustomMapper<ArrDataNull, ArrDataNull>() {
            @Override
            public void mapAtoB(ArrDataNull arrDataNull, ArrDataNull arrDataNullNew, MappingContext context) {
                arrDataNullNew.setDataType(arrDataNull.getDataType());
                arrDataNullNew.setItem(arrDataNull.getItem());
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
    public ArrDescItem getDescItem(ArrDescItem descItem) {
        ArrData data = getDataByDescItem(descItem);
        ArrItemData item = createItemByType(descItem.getItemType().getDataType());
        BeanUtils.copyProperties(data, item);
        descItem.setItem(item);
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

        for (ArrDescItem descItem : descItems) {
            result.add(getDescItem(descItem));
        }
        return result;
    }

    /**
     * Vytvoření objektu atributu s formátovanou hodnotou atributu.
     *
     * @param descItem   atributu bez dat
     * @param formatData požadovaný formát dat
     * @return výsledný atributu s daty
     */
    public ArrDescItem getDescItem(ArrDescItem descItem, String formatData) {
        if (descItem.getItem() != null) {
            return descItem;
        }

        ArrData data = getDataByDescItem(descItem);

        if (formatData != null) {
            Map<Object, Object> map = new HashMap<>();
            map.put(PROPERTY_FORMAT, formatData);
            MappingContext mappingContext = new MappingContext(map);
            facade.map(data, descItem, mappingContext);
        } else {
            ArrItemData item = createItemByType(descItem.getItemType().getDataType());
            BeanUtils.copyProperties(data, item);
            descItem.setItem(item);
            item.setSpec(descItem.getItemSpec());
        }


        return descItem;
    }

    /**
     * Načte hodnotu k atributu.
     *
     * @param descItem atribut ke kterému hledáme data
     * @return nalezená data k atributu
     */
    private ArrData getDataByDescItem(ArrDescItem descItem) {
        List<ArrData> dataList = dataRepository.findByItem(descItem);
        if (dataList.size() != 1) {
            throw new IllegalStateException("Hodnota musí být právě jedna");
        }
        return dataList.get(0);
    }

    /**
     * Uloží hodnotu atributu bez dat.
     *
     * @param descItem hodnota atributu
     * @return nova hodnota atributu
     */
    public ArrDescItem saveDescItem(ArrDescItem descItem) {
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
    public ArrDescItem saveDescItemWithData(ArrDescItem descItem, Boolean createNewVersion) {

        descItemRepository.save(descItem);

        ArrItemData item = descItem.getItem();
        ArrData data;

        if (createNewVersion) {
            if (item instanceof ArrItemCoordinates) {
                data = facade.map(item, ArrDataCoordinates.class);
            } else if (item instanceof ArrItemFormattedText) {
                data = facade.map(item, ArrDataText.class);
            } else if (item instanceof ArrItemInt) {
                data = facade.map(item, ArrDataInteger.class);
            } else if (item instanceof ArrItemPartyRef) {
                data = facade.map(item, ArrDataPartyRef.class);
            } else if (item instanceof ArrItemRecordRef) {
                data = facade.map(item, ArrDataRecordRef.class);
            } else if (item instanceof ArrItemString) {
                data = facade.map(item, ArrDataString.class);
            } else if (item instanceof ArrItemText) {
                data = facade.map(item, ArrDataText.class);
            } else if (item instanceof ArrItemUnitdate) {
                data = facade.map(item, ArrDataUnitdate.class);
            } else if (item instanceof ArrItemUnitid) {
                data = facade.map(item, ArrDataUnitid.class);
            } else if (item instanceof ArrItemDecimal) {
                data = facade.map(item, ArrDataDecimal.class);
            } else if (item instanceof ArrItemFileRef) {
                data = facade.map(item, ArrDataFileRef.class);
            } else if (item instanceof ArrItemPacketRef) {
                data = facade.map(item, ArrDataPacketRef.class);
            } else if (item instanceof ArrItemEnum) {
                data = facade.map(item, ArrDataNull.class);
            } else if (item instanceof ArrItemJsonTable) {
                data = facade.map(item, ArrDataJsonTable.class);
            } else {
                throw new NotImplementedException("Nebyl namapován datový typ: " + descItem.getClass().getName() + ", item: " + item);
            }
            data.setItem(descItem);
            data.setDataType(descItem.getItemType().getDataType());
        } else {
            data = getDataByDescItem(descItem);
            facade.map(descItem, data);
        }

        try {
            mapRepository.get(data.getClass()).save(data);
        } catch (NullPointerException e) {
            e.printStackTrace();
            throw new NotImplementedException("Nebyla namapována repozitory pro datový typ");
        }

        return descItem;
    }

    /**
     * Vytvoření objektu podle datového typu.
     *
     * @param dataType zvolený datový typ
     * @return nový objekt
     */
    public ArrItemData createItemByType(RulDataType dataType) {
        Assert.notNull(dataType);

        switch (dataType.getCode()) {
            case "INT":
                return new ArrItemInt();
            case "STRING":
                return new ArrItemString();
            case "TEXT":
                return new ArrItemText();
            case "UNITDATE":
                return new ArrItemUnitdate();
            case "UNITID":
                return new ArrItemUnitid();
            case "FORMATTED_TEXT":
                return new ArrItemFormattedText();
            case "COORDINATES":
                return new ArrItemCoordinates();
            case "PARTY_REF":
                return new ArrItemPartyRef();
            case "RECORD_REF":
                return new ArrItemRecordRef();
            case "DECIMAL":
                return new ArrItemDecimal();
            case "FILE_REF":
                return new ArrItemFileRef();
            case "PACKET_REF":
                return new ArrItemPacketRef();
            case "ENUM":
                return new ArrItemEnum();
            case "JSON_TABLE":
                return new ArrItemJsonTable();
            default:
                throw new NotImplementedException("Nebyl namapován datový typ");
        }
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
                        && ArrangementManager.FORMAT_ATTRIBUTE_SHORT.equals(format)) {
                    valueRet = stringValue.substring(0, 250);
                }
            }
        }
        return valueRet;
    }

}
