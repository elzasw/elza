package cz.tacr.elza.domain.factory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cz.tacr.elza.domain.convertor.CalendarConverter;
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

import cz.tacr.elza.api.controller.ArrangementManager;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataCoordinates;
import cz.tacr.elza.domain.ArrDataDecimal;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataNull;
import cz.tacr.elza.domain.ArrDataPacketRef;
import cz.tacr.elza.domain.ArrDataPartyRef;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.ArrDataUnitid;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrDescItemCoordinates;
import cz.tacr.elza.domain.ArrDescItemDecimal;
import cz.tacr.elza.domain.ArrDescItemEnum;
import cz.tacr.elza.domain.ArrDescItemFormattedText;
import cz.tacr.elza.domain.ArrDescItemInt;
import cz.tacr.elza.domain.ArrDescItemPacketRef;
import cz.tacr.elza.domain.ArrDescItemPartyRef;
import cz.tacr.elza.domain.ArrDescItemRecordRef;
import cz.tacr.elza.domain.ArrDescItemString;
import cz.tacr.elza.domain.ArrDescItemText;
import cz.tacr.elza.domain.ArrDescItemUnitdate;
import cz.tacr.elza.domain.ArrDescItemUnitid;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.repository.DataCoordinatesRepository;
import cz.tacr.elza.repository.DataDecimalRepository;
import cz.tacr.elza.repository.DataIntegerRepository;
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
    private DataNullRepository dataNullRepository;

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
        defineMapEnum();

        facade = factory.getMapperFacade();

        initMapRepository();
    }

    /**
     * Nadefinování pravidel pro převod formátu Coordinates.
     */
    private void defineMapCoordinates() {
        factory.classMap(ArrDescItemCoordinates.class, ArrDataCoordinates.class)
                .customize(new CustomMapper<ArrDescItemCoordinates, ArrDataCoordinates>() {

                    @Override
                    public void mapAtoB(ArrDescItemCoordinates arrDescItemCoordinates,
                                        ArrDataCoordinates arrDataCoordinates,
                                        MappingContext context) {
                        arrDataCoordinates.setDataType(arrDescItemCoordinates.getItemType().getDataType());
                        arrDataCoordinates.setDescItem(arrDescItemCoordinates);
                        arrDataCoordinates.setValue(arrDescItemCoordinates.getValue());
                    }

                    @Override
                    public void mapBtoA(ArrDataCoordinates arrDataCoordinates,
                                        ArrDescItemCoordinates arrDescItemExtCoordinates,
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
                        arrDataCoordinatesNew.setDescItem(arrDataCoordinates.getDescItem());
                        arrDataCoordinatesNew.setValue(arrDataCoordinates.getValue());
                    }
                }).register();
    }

    /**
     * Nadefinování pravidel pro převod formátu FormattedText.
     */
    private void defineMapFormattedText() {
        factory.classMap(ArrDescItemFormattedText.class, ArrDataText.class)
                .customize(new CustomMapper<ArrDescItemFormattedText, ArrDataText>() {

                    @Override
                    public void mapAtoB(ArrDescItemFormattedText arrDescItemFormattedText,
                                        ArrDataText arrDataText,
                                        MappingContext context) {
                        arrDataText.setDataType(arrDescItemFormattedText.getItemType().getDataType());
                        arrDataText.setDescItem(arrDescItemFormattedText);
                        arrDataText.setValue(arrDescItemFormattedText.getValue());
                    }

                    @Override
                    public void mapBtoA(ArrDataText arrDataText,
                                        ArrDescItemFormattedText arrDescItemFormattedText,
                                        MappingContext context) {
                        String formattedValue = formatString(context, arrDataText.getValue());
                        arrDescItemFormattedText.setValue(formattedValue);
                    }
                }).register();

        factory.classMap(ArrDataText.class, ArrDataText.class).customize(new CustomMapper<ArrDataText, ArrDataText>() {
            @Override
            public void mapAtoB(ArrDataText arrDataText, ArrDataText arrDataTextNew, MappingContext context) {
                arrDataTextNew.setDataType(arrDataText.getDataType());
                arrDataTextNew.setDescItem(arrDataText.getDescItem());
                arrDataTextNew.setValue(arrDataText.getValue());
            }
        }).register();
    }

    /**
     * Nadefinování pravidel pro převod formátu Int.
     */
    private void defineMapInt() {
        factory.classMap(ArrDescItemInt.class, ArrDataInteger.class)
                .customize(new CustomMapper<ArrDescItemInt, ArrDataInteger>() {

                    @Override
                    public void mapAtoB(ArrDescItemInt arrDescItemInt,
                                        ArrDataInteger arrDataInteger,
                                        MappingContext context) {
                        arrDataInteger.setDataType(arrDescItemInt.getItemType().getDataType());
                        arrDataInteger.setDescItem(arrDescItemInt);
                        arrDataInteger.setValue(arrDescItemInt.getValue());
                    }

                    @Override
                    public void mapBtoA(ArrDataInteger arrDataInteger,
                                        ArrDescItemInt arrDescItemInt,
                                        MappingContext context) {
                        arrDescItemInt.setValue(arrDataInteger.getValue());
                    }

                }).register();

        factory.classMap(ArrDataInteger.class, ArrDataInteger.class)
                .customize(new CustomMapper<ArrDataInteger, ArrDataInteger>() {
                    @Override
                    public void mapAtoB(ArrDataInteger arrDataInteger,
                                        ArrDataInteger arrDataIntegerNew,
                                        MappingContext context) {
                        arrDataIntegerNew.setDataType(arrDataInteger.getDataType());
                        arrDataIntegerNew.setDescItem(arrDataInteger.getDescItem());
                        arrDataIntegerNew.setValue(arrDataInteger.getValue());
                    }
                }).register();
    }

    /**
     * Nadefinování pravidel pro převod formátu PartyRef.
     */
    private void defineMapPartyRef() {
        factory.classMap(ArrDescItemPartyRef.class, ArrDataPartyRef.class)
                .customize(new CustomMapper<ArrDescItemPartyRef, ArrDataPartyRef>() {

                    @Override
                    public void mapAtoB(ArrDescItemPartyRef arrDescItemPartyRef,
                                        ArrDataPartyRef arrDataPartyRef,
                                        MappingContext context) {
                        arrDataPartyRef.setDataType(arrDescItemPartyRef.getItemType().getDataType());
                        arrDataPartyRef.setDescItem(arrDescItemPartyRef);
                        arrDataPartyRef.setParty(arrDescItemPartyRef.getParty());
                    }

                    @Override
                    public void mapBtoA(ArrDataPartyRef arrDataPartyRef,
                                        ArrDescItemPartyRef arrDescItemPartyRef,
                                        MappingContext context) {
                        arrDescItemPartyRef.setParty(arrDataPartyRef.getParty());
                    }

                }).register();

        factory.classMap(ArrDataPartyRef.class, ArrDataPartyRef.class)
                .customize(new CustomMapper<ArrDataPartyRef, ArrDataPartyRef>() {
                    @Override
                    public void mapAtoB(ArrDataPartyRef arrDataPartyRef,
                                        ArrDataPartyRef arrDataPartyRefNew,
                                        MappingContext context) {
                        arrDataPartyRefNew.setDataType(arrDataPartyRef.getDataType());
                        arrDataPartyRefNew.setDescItem(arrDataPartyRef.getDescItem());
                        arrDataPartyRefNew.setParty(arrDataPartyRef.getParty());
                    }
                }).register();
    }

    /**
     * Nadefinování pravidel pro převod formátu PacketRef.
     */
    private void defineMapPacketRef() {
        factory.classMap(ArrDescItemPacketRef.class, ArrDataPacketRef.class)
                .customize(new CustomMapper<ArrDescItemPacketRef, ArrDataPacketRef>() {

                    @Override
                    public void mapAtoB(ArrDescItemPacketRef arrDescItemPartyRef,
                                        ArrDataPacketRef arrDataPartyRef,
                                        MappingContext context) {
                        arrDataPartyRef.setDataType(arrDescItemPartyRef.getItemType().getDataType());
                        arrDataPartyRef.setDescItem(arrDescItemPartyRef);
                        arrDataPartyRef.setPacket(arrDescItemPartyRef.getPacket());
                    }

                    @Override
                    public void mapBtoA(ArrDataPacketRef arrDataPartyRef,
                                        ArrDescItemPacketRef arrDescItemPartyRef,
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
                        arrDataPartyRefNew.setDescItem(arrDataPartyRef.getDescItem());
                        arrDataPartyRefNew.setPacket(arrDataPartyRef.getPacket());
                    }
                }).register();
    }

    /**
     * Nadefinování pravidel pro převod formátu RecordRef.
     */
    private void defineMapRecordRef() {
        factory.classMap(ArrDescItemRecordRef.class, ArrDataRecordRef.class)
                .customize(new CustomMapper<ArrDescItemRecordRef, ArrDataRecordRef>() {

                    @Override
                    public void mapAtoB(ArrDescItemRecordRef arrDescItemRecordRef,
                                        ArrDataRecordRef arrDataRecordRef,
                                        MappingContext context) {
                        arrDataRecordRef.setDataType(arrDescItemRecordRef.getItemType().getDataType());
                        arrDataRecordRef.setDescItem(arrDescItemRecordRef);
                        arrDataRecordRef.setRecord(arrDescItemRecordRef.getRecord());
                    }

                    @Override
                    public void mapBtoA(ArrDataRecordRef arrDataRecordRef,
                                        ArrDescItemRecordRef arrDescItemRecordRef,
                                        MappingContext context) {
                        arrDescItemRecordRef.setRecord(arrDataRecordRef.getRecord());
                    }

                }).register();

        factory.classMap(ArrDataRecordRef.class, ArrDataRecordRef.class)
                .customize(new CustomMapper<ArrDataRecordRef, ArrDataRecordRef>() {
                    @Override
                    public void mapAtoB(ArrDataRecordRef arrDataRecordRef,
                                        ArrDataRecordRef arrDataRecordRefNew,
                                        MappingContext context) {
                        arrDataRecordRefNew.setDataType(arrDataRecordRef.getDataType());
                        arrDataRecordRefNew.setDescItem(arrDataRecordRef.getDescItem());
                        arrDataRecordRefNew.setRecord(arrDataRecordRef.getRecord());
                    }
                }).register();
    }

    /**
     * Nadefinování pravidel pro převod formátu String.
     */
    private void defineMapString() {
        factory.classMap(ArrDescItemString.class, ArrDataString.class).customize(
                new CustomMapper<ArrDescItemString, ArrDataString>() {

                    @Override
                    public void mapAtoB(ArrDescItemString arrDescItemString,
                                        ArrDataString arrDataString,
                                        MappingContext context) {
                        arrDataString.setDataType(arrDescItemString.getItemType().getDataType());
                        arrDataString.setDescItem(arrDescItemString);
                        arrDataString.setValue(arrDescItemString.getValue());
                    }

                    @Override
                    public void mapBtoA(ArrDataString arrDataString,
                                        ArrDescItemString arrDescItemString,
                                        MappingContext context) {
                        String formattedValue = formatString(context, arrDataString.getValue());
                        arrDescItemString.setValue(formattedValue);
                    }
                }).register();

        factory.classMap(ArrDataString.class, ArrDataString.class)
                .customize(new CustomMapper<ArrDataString, ArrDataString>() {
                    @Override
                    public void mapAtoB(ArrDataString arrDataString,
                                        ArrDataString arrDataStringNew,
                                        MappingContext context) {
                        arrDataStringNew.setDataType(arrDataString.getDataType());
                        arrDataStringNew.setDescItem(arrDataString.getDescItem());
                        arrDataStringNew.setValue(arrDataString.getValue());
                    }
                }).register();
    }

    /**
     * Nadefinování pravidel pro převod formátu Text.
     */
    private void defineMapText() {
        factory.classMap(ArrDescItemText.class, ArrDataText.class)
                .customize(new CustomMapper<ArrDescItemText, ArrDataText>() {

                    @Override
                    public void mapAtoB(ArrDescItemText arrDescItemText,
                                        ArrDataText arrDataText,
                                        MappingContext context) {
                        arrDataText.setDataType(arrDescItemText.getItemType().getDataType());
                        arrDataText.setDescItem(arrDescItemText);
                        arrDataText.setValue(arrDescItemText.getValue());
                    }

                    @Override
                    public void mapBtoA(ArrDataText arrDataText,
                                        ArrDescItemText arrDescItemText,
                                        MappingContext context) {
                        String formattedValue = formatString(context, arrDataText.getValue());
                        arrDescItemText.setValue(formattedValue);
                    }
                }).register();

        factory.classMap(ArrDataText.class, ArrDataText.class).customize(new CustomMapper<ArrDataText, ArrDataText>() {
            @Override
            public void mapAtoB(ArrDataText arrDataText, ArrDataText arrDataTextNew, MappingContext context) {
                arrDataTextNew.setDataType(arrDataText.getDataType());
                arrDataTextNew.setDescItem(arrDataText.getDescItem());
                arrDataTextNew.setValue(arrDataText.getValue());
            }
        }).register();
    }

    /**
     * Nadefinování pravidel pro převod formátu Unitdate.
     */
    private void defineMapUnitdate() {
        factory.classMap(ArrDescItemUnitdate.class, ArrDataUnitdate.class)
                .customize(new CustomMapper<ArrDescItemUnitdate, ArrDataUnitdate>() {

                    @Override
                    public void mapAtoB(ArrDescItemUnitdate arrDescItemUnitdate,
                                        ArrDataUnitdate arrDataUnitdate,
                                        MappingContext context) {
                        arrDataUnitdate.setDataType(arrDescItemUnitdate.getItemType().getDataType());
                        arrDataUnitdate.setDescItem(arrDescItemUnitdate);

                        if (arrDescItemUnitdate.getFormat() == null) {
                            throw new IllegalArgumentException("Nebyl odeslán formát dat");
                        } else {
                            String format = arrDescItemUnitdate.getFormat();
                            if (!format.matches(
                                    "(" + PATTERN_UNIT_DATA + ")|(" + PATTERN_UNIT_DATA + INTERVAL_DELIMITER_UNIT_DATA
                                            + PATTERN_UNIT_DATA + ")")) {
                                throw new IllegalArgumentException("Neplatný formát dat");
                            }
                        }
                        arrDataUnitdate.setFormat(arrDescItemUnitdate.getFormat());

                        if (arrDescItemUnitdate.getCalendarType() == null) {
                            throw new IllegalArgumentException("Nebyl zvolen kalendar");
                        }
                        arrDataUnitdate.setCalendarType(arrDescItemUnitdate.getCalendarType());

                        try {
                            String value = arrDescItemUnitdate.getValueFrom();
                            if (value != null) {
                                value = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                                        .format(LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                            }
                            arrDataUnitdate.setValueFrom(value);
                        } catch (DateTimeParseException e) {
                            throw new IllegalArgumentException("Nebyl zadan platny format datumu 'od'", e);
                        }

                        arrDataUnitdate.setValueFromEstimated(arrDescItemUnitdate.getValueFromEstimated());

                        try {
                            String value = arrDescItemUnitdate.getValueTo();
                            if (value != null) {
                                value = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                                        .format(LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                            }
                            arrDataUnitdate.setValueTo(value);
                        } catch (DateTimeParseException e) {
                            throw new IllegalArgumentException("Nebyl zadan platny format datumu 'do'", e);
                        }

                        if (arrDescItemUnitdate.getValueFrom() != null && arrDescItemUnitdate.getValueTo() != null) {
                            LocalDateTime from = LocalDateTime
                                    .parse(arrDescItemUnitdate.getValueFrom(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                            LocalDateTime to = LocalDateTime
                                    .parse(arrDescItemUnitdate.getValueTo(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                            if (from.isAfter(to)) {
                                throw new IllegalArgumentException("Neplatný interval ISO datumů: od > do");
                            }
                        } else if (arrDescItemUnitdate.getValueFrom() == null
                                && arrDescItemUnitdate.getValueTo() == null) {
                            throw new IllegalArgumentException("Nebyl zadán interval ISO datumů");
                        }

                        String codeCalendar = arrDescItemUnitdate.getCalendarType().getCode();
                        CalendarConverter.CalendarType calendarType = CalendarConverter.CalendarType.valueOf(codeCalendar);

                        String value;

                        value = arrDescItemUnitdate.getValueFrom();
                        if (value != null) {
                            arrDataUnitdate.setNormalizedFrom(CalendarConverter.toSeconds(calendarType, LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
                        } else {
                            arrDataUnitdate.setNormalizedFrom(Long.MIN_VALUE);
                        }

                        value = arrDescItemUnitdate.getValueTo();
                        if (value != null) {
                            arrDataUnitdate.setNormalizedTo(CalendarConverter.toSeconds(calendarType, LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
                        } else {
                            arrDataUnitdate.setNormalizedTo(Long.MAX_VALUE);
                        }

                        arrDataUnitdate.setValueToEstimated(arrDescItemUnitdate.getValueToEstimated());
                    }

                    @Override
                    public void mapBtoA(ArrDataUnitdate arrDataUnitdate,
                                        ArrDescItemUnitdate arrDescItemUnitdate,
                                        MappingContext context) {
                        arrDescItemUnitdate.setCalendarType(arrDataUnitdate.getCalendarType());
                        arrDescItemUnitdate.setValueFrom(arrDataUnitdate.getValueFrom());
                        arrDescItemUnitdate.setValueFromEstimated(arrDataUnitdate.getValueFromEstimated());
                        arrDescItemUnitdate.setValueTo(arrDataUnitdate.getValueTo());
                        arrDescItemUnitdate.setValueToEstimated(arrDataUnitdate.getValueToEstimated());
                        arrDescItemUnitdate.setFormat(arrDataUnitdate.getFormat());
                    }
                }).register();

        factory.classMap(ArrDataUnitdate.class, ArrDataUnitdate.class)
                .customize(new CustomMapper<ArrDataUnitdate, ArrDataUnitdate>() {
                    @Override
                    public void mapAtoB(ArrDataUnitdate arrDataUnitdate,
                                        ArrDataUnitdate arrDataUnitdateNew,
                                        MappingContext context) {
                        arrDataUnitdateNew.setDataType(arrDataUnitdate.getDataType());
                        arrDataUnitdateNew.setDescItem(arrDataUnitdate.getDescItem());
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
        factory.classMap(ArrDescItemUnitid.class, ArrDataUnitid.class)
                .customize(new CustomMapper<ArrDescItemUnitid, ArrDataUnitid>() {

                    @Override
                    public void mapAtoB(ArrDescItemUnitid arrDescItemUnitid,
                                        ArrDataUnitid arrDataUnitid,
                                        MappingContext context) {
                        arrDataUnitid.setDataType(arrDescItemUnitid.getItemType().getDataType());
                        arrDataUnitid.setDescItem(arrDescItemUnitid);
                        arrDataUnitid.setValue(arrDescItemUnitid.getValue());
                    }

                    @Override
                    public void mapBtoA(ArrDataUnitid arrDataUnitid,
                                        ArrDescItemUnitid arrDescItemUnitid,
                                        MappingContext context) {
                        arrDescItemUnitid.setValue(arrDataUnitid.getValue());
                    }
                }).register();

        factory.classMap(ArrDataUnitid.class, ArrDataUnitid.class)
                .customize(new CustomMapper<ArrDataUnitid, ArrDataUnitid>() {
                    @Override
                    public void mapAtoB(ArrDataUnitid arrDataUnitid,
                                        ArrDataUnitid arrDataUnitidNew,
                                        MappingContext context) {
                        arrDataUnitidNew.setDataType(arrDataUnitid.getDataType());
                        arrDataUnitidNew.setDescItem(arrDataUnitid.getDescItem());
                        arrDataUnitidNew.setValue(arrDataUnitid.getValue());
                    }
                }).register();
    }

    /**
     * Nadefinování pravidel pro převod formátu Decimal.
     */
    private void defineMapDecimal() {
        factory.classMap(ArrDescItemDecimal.class, ArrDataDecimal.class)
                .customize(new CustomMapper<ArrDescItemDecimal, ArrDataDecimal>() {

                    @Override
                    public void mapAtoB(ArrDescItemDecimal arrDescItemDecimal,
                                        ArrDataDecimal arrDataDecimal,
                                        MappingContext context) {
                        arrDataDecimal.setDataType(arrDescItemDecimal.getItemType().getDataType());
                        arrDataDecimal.setDescItem(arrDescItemDecimal);
                        arrDataDecimal.setValue(arrDescItemDecimal.getValue());
                    }

                    @Override
                    public void mapBtoA(ArrDataDecimal arrDataDecimal,
                                        ArrDescItemDecimal arrDescItemDecimal,
                                        MappingContext context) {
                        arrDescItemDecimal.setValue(arrDataDecimal.getValue());
                    }
                }).register();

        factory.classMap(ArrDataDecimal.class, ArrDataDecimal.class)
                .customize(new CustomMapper<ArrDataDecimal, ArrDataDecimal>() {
                    @Override
                    public void mapAtoB(ArrDataDecimal arrDataDecimal,
                                        ArrDataDecimal arrDataDecimalNew,
                                        MappingContext context) {
                        arrDataDecimalNew.setDataType(arrDataDecimal.getDataType());
                        arrDataDecimalNew.setDescItem(arrDataDecimal.getDescItem());
                        arrDataDecimalNew.setValue(arrDataDecimal.getValue());
                    }
                }).register();
    }

    /**
     * Nadefinování pravidel pro převod formátu Enum.
     */
    private void defineMapEnum() {
        factory.classMap(ArrDescItemEnum.class, ArrDataNull.class)
                .customize(new CustomMapper<ArrDescItemEnum, ArrDataNull>() {

                    @Override
                    public void mapAtoB(ArrDescItemEnum arrDescItemEnum,
                                        ArrDataNull arrDataNull,
                                        MappingContext context) {
                        arrDataNull.setDataType(arrDescItemEnum.getItemType().getDataType());
                        arrDataNull.setDescItem(arrDescItemEnum);
                    }

                    @Override
                    public void mapBtoA(ArrDataNull arrDataNull,
                                        ArrDescItemEnum arrDescItemEnum,
                                        MappingContext context) {

                    }
                }).register();

        factory.classMap(ArrDataNull.class, ArrDataNull.class).customize(new CustomMapper<ArrDataNull, ArrDataNull>() {
            @Override
            public void mapAtoB(ArrDataNull arrDataNull, ArrDataNull arrDataNullNew, MappingContext context) {
                arrDataNullNew.setDataType(arrDataNull.getDataType());
                arrDataNullNew.setDescItem(arrDataNull.getDescItem());
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
        mapRepository.put(ArrDataNull.class, dataNullRepository);
    }

    /**
     * Vytvoření objektu atributu s hodnotou atributu.
     *
     * @param descItem atributu bez dat
     * @return výsledný atributu s daty
     */
    public ArrDescItem getDescItem(ArrDescItem descItem) {
        ArrData data = getDataByDescItem(descItem);
        ArrDescItem descItemTmp = createDescItemByType(descItem.getItemType().getDataType());
        BeanUtils.copyProperties(descItem, descItemTmp);
        facade.map(data, descItemTmp);
        return descItemTmp;
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
        ArrData data = getDataByDescItem(descItem);
        ArrDescItem descItemTmp = createDescItemByType(data.getDataType());
        BeanUtils.copyProperties(descItem, descItemTmp);

        if (formatData != null) {
            Map<Object, Object> map = new HashMap<>();
            map.put(PROPERTY_FORMAT, formatData);
            MappingContext mappingContext = new MappingContext(map);
            facade.map(data, descItemTmp, mappingContext);
        } else {
            facade.map(data, descItemTmp);
        }

        return descItemTmp;
    }

    /**
     * Načte hodnotu k atributu.
     *
     * @param descItem atribut ke kterému hledáme data
     * @return nalezená data k atributu
     */
    private ArrData getDataByDescItem(ArrDescItem descItem) {
        List<ArrData> dataList = dataRepository.findByDescItem(descItem);
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
        ArrDescItem descItemRaw = new ArrDescItem();
        BeanUtils.copyProperties(descItem, descItemRaw);
        descItemRaw = descItemRepository.save(descItemRaw);
        BeanUtils.copyProperties(descItemRaw, descItem);
        return descItem;
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

        ArrDescItem descItemRaw = new ArrDescItem();
        BeanUtils.copyProperties(descItem, descItemRaw);
        descItemRaw = descItemRepository.save(descItemRaw);
        BeanUtils.copyProperties(descItemRaw, descItem);

        ArrData data;

        if (createNewVersion) {
            if (descItem instanceof ArrDescItemCoordinates) {
                data = facade.map(descItem, ArrDataCoordinates.class);
            } else if (descItem instanceof ArrDescItemFormattedText) {
                data = facade.map(descItem, ArrDataText.class);
            } else if (descItem instanceof ArrDescItemInt) {
                data = facade.map(descItem, ArrDataInteger.class);
            } else if (descItem instanceof ArrDescItemPartyRef) {
                data = facade.map(descItem, ArrDataPartyRef.class);
            } else if (descItem instanceof ArrDescItemRecordRef) {
                data = facade.map(descItem, ArrDataRecordRef.class);
            } else if (descItem instanceof ArrDescItemString) {
                data = facade.map(descItem, ArrDataString.class);
            } else if (descItem instanceof ArrDescItemText) {
                data = facade.map(descItem, ArrDataText.class);
            } else if (descItem instanceof ArrDescItemUnitdate) {
                data = facade.map(descItem, ArrDataUnitdate.class);
            } else if (descItem instanceof ArrDescItemUnitid) {
                data = facade.map(descItem, ArrDataUnitid.class);
            } else if (descItem instanceof ArrDescItemDecimal) {
                data = facade.map(descItem, ArrDataDecimal.class);
            } else if (descItem instanceof ArrDescItemPacketRef) {
                data = facade.map(descItem, ArrDataPacketRef.class);
            } else if (descItem instanceof ArrDescItemEnum) {
                data = facade.map(descItem, ArrDataNull.class);
            } else {
                throw new NotImplementedException("Nebyl namapován datový typ: " + descItem.getClass().getName());
            }
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
    public ArrDescItem createDescItemByType(RulDataType dataType) {
        Assert.notNull(dataType);

        switch (dataType.getCode()) {
            case "INT":
                return new ArrDescItemInt();
            case "STRING":
                return new ArrDescItemString();
            case "TEXT":
                return new ArrDescItemText();
            case "UNITDATE":
                return new ArrDescItemUnitdate();
            case "UNITID":
                return new ArrDescItemUnitid();
            case "FORMATTED_TEXT":
                return new ArrDescItemFormattedText();
            case "COORDINATES":
                return new ArrDescItemCoordinates();
            case "PARTY_REF":
                return new ArrDescItemPartyRef();
            case "RECORD_REF":
                return new ArrDescItemRecordRef();
            case "DECIMAL":
                return new ArrDescItemDecimal();
            case "PACKET_REF":
                return new ArrDescItemPacketRef();
            case "ENUM":
                return new ArrDescItemEnum();
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
