package cz.tacr.elza.service;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import cz.tacr.elza.domain.ArrDataStructureRef;
import cz.tacr.elza.domain.ArrStructureData;
import cz.tacr.elza.domain.factory.DescItemFactory;
import cz.tacr.elza.repository.StructureDataRepository;
import org.apache.commons.lang.NotImplementedException;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cz.tacr.elza.controller.ArrangementController;
import cz.tacr.elza.core.data.CalendarType;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataCoordinates;
import cz.tacr.elza.domain.ArrDataDecimal;
import cz.tacr.elza.domain.ArrDataFileRef;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataJsonTable;
import cz.tacr.elza.domain.ArrDataNull;
import cz.tacr.elza.domain.ArrDataPartyRef;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.ArrDataUnitid;
import cz.tacr.elza.domain.ArrFile;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrItemCoordinates;
import cz.tacr.elza.domain.ArrItemDecimal;
import cz.tacr.elza.domain.ArrItemEnum;
import cz.tacr.elza.domain.ArrItemFileRef;
import cz.tacr.elza.domain.ArrItemFormattedText;
import cz.tacr.elza.domain.ArrItemInt;
import cz.tacr.elza.domain.ArrItemJsonTable;
import cz.tacr.elza.domain.ArrItemStructureRef;
import cz.tacr.elza.domain.ArrItemPartyRef;
import cz.tacr.elza.domain.ArrItemRecordRef;
import cz.tacr.elza.domain.ArrItemString;
import cz.tacr.elza.domain.ArrItemText;
import cz.tacr.elza.domain.ArrItemUnitdate;
import cz.tacr.elza.domain.ArrItemUnitid;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.convertor.CalendarConverter;
import cz.tacr.elza.domain.table.ElzaColumn;
import cz.tacr.elza.domain.table.ElzaRow;
import cz.tacr.elza.domain.table.ElzaTable;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.DataCoordinatesRepository;
import cz.tacr.elza.repository.DataDecimalRepository;
import cz.tacr.elza.repository.DataFileRefRepository;
import cz.tacr.elza.repository.DataIntegerRepository;
import cz.tacr.elza.repository.DataJsonTableRepository;
import cz.tacr.elza.repository.DataNullRepository;
import cz.tacr.elza.repository.DataStructureRefRepository;
import cz.tacr.elza.repository.DataPartyRefRepository;
import cz.tacr.elza.repository.DataRecordRefRepository;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.DataStringRepository;
import cz.tacr.elza.repository.DataTextRepository;
import cz.tacr.elza.repository.DataUnitdateRepository;
import cz.tacr.elza.repository.DataUnitidRepository;
import cz.tacr.elza.repository.FundFileRepository;
import cz.tacr.elza.repository.ItemRepository;
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.PartyRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import ma.glasnost.orika.CustomMapper;
import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.impl.DefaultMapperFactory;

/**
 * Serviska pro správu hodnot atributů.
 *
 * @author Martin Šlapa
 * @since 24.06.2016
 */
@Service
public class ItemService implements InitializingBean {

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
    private ItemRepository itemRepository;

    @Autowired
    private DataRepository dataRepository;

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
    private DataStructureRefRepository dataStructureRefRepository;

    @Autowired
    private DataFileRefRepository dataFileRefRepository;

    @Autowired
    private DataNullRepository dataNullRepository;

    @Autowired
    private DataJsonTableRepository dataJsonTableRepository;

    @Autowired
    private ItemSpecRepository itemSpecRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private StructureDataRepository structureDataRepository;

    @Autowired
    private FundFileRepository fundFileRepository;

    @Autowired
    private RegRecordRepository recordRepository;

    @Autowired
    private DescItemFactory descItemFactory;

    /**
     * Kontrola sloupců v JSON tabulce.
     *
     * @param table   kontrolovaná tabulka
     * @param columns seznam definicí sloupců
     */
    public void checkJsonTableData(@NotNull final ElzaTable table,
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

    public ArrData getDataByItem(final ArrItem item) {
        return item.getData();
    }

    @Deprecated
    public <T extends ArrItem> T save(final T item,
                                      final boolean createNewVersion) {
        ArrData data = item.getData();

        if (data != null) {
            if (data instanceof ArrDataJsonTable) {
                checkJsonTableData(((ArrDataJsonTable) data).getValue(), item.getItemType().getColumnsDefinition());
            }

            ArrData dataNew;
            if (createNewVersion) {
                dataNew = facade.map(data, ArrData.class);
                //data.setItem(item);
                dataNew.setDataType(item.getItemType().getDataType());
            } else {
                dataNew = data;
            }

            try {
                mapRepository.get(dataNew.getClass()).save(dataNew);
            } catch (NullPointerException e) {
                throw new NotImplementedException("Nebyla namapována repozitory pro datový typ");
            }

            item.setData(dataNew);
        }
        return itemRepository.save(item);
    }

    /*public <T extends ArrItem> T loadData(final T item) {
        ArrData data = getDataByItem(item);
        ArrItemData itemData;
        if (data == null) {
            itemData = descItemFactory.createItemByType(item.getItemType().getDataType());
        } else {
            itemData = facade.map(data, ArrItemData.class);
        }
        item.setItem(itemData);
        return item;
    }*/

    public <T extends ArrItem> void moveDown(final List<T> items, final ArrChange change) {
        for (ArrItem itemMove : items) {

            itemMove.setDeleteChange(change);
            itemRepository.save(itemMove);

            ArrItem itemNew;
            try {
                itemNew = itemMove.getClass().getConstructor().newInstance();
            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new SystemException(e);
            }

            BeanUtils.copyProperties(itemMove, itemNew, "itemId", "deleteChange");
            itemNew.setCreateChange(change);
            itemNew.setPosition(itemMove.getPosition() + 1);

            // pro odverzovanou hodnotu atributu je nutné vytvořit kopii dat
            copyItemData(itemMove, itemNew);
            itemRepository.save(itemNew);
        }
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
        defineMapStructureRef();
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
                        arrDataPartyRefNew.setParty(arrDataPartyRef.getParty());
                    }
                }).register();
    }

    /**
     * Nadefinování pravidel pro převod formátu StrucureRef.
     */
    private void defineMapStructureRef() {
        factory.classMap(ArrItemStructureRef.class, ArrDataStructureRef.class)
        .customize(new CustomMapper<ArrItemStructureRef, ArrDataStructureRef>() {

            @Override
            public void mapAtoB(final ArrItemStructureRef arrDescItemPartyRef,
                                final ArrDataStructureRef arrDataPartyRef,
                                final MappingContext context) {
                arrDataPartyRef.setStructureData(arrDescItemPartyRef.getStructureData());
            }

            @Override
            public void mapBtoA(final ArrDataStructureRef arrDataPartyRef,
                                final ArrItemStructureRef arrDescItemPartyRef,
                                final MappingContext context) {
                arrDescItemPartyRef.setStructureData(arrDataPartyRef.getStructureData());
            }

        }).register();

        factory.classMap(ArrDataStructureRef.class, ArrDataStructureRef.class)
                .customize(new CustomMapper<ArrDataStructureRef, ArrDataStructureRef>() {
                    @Override
                    public void mapAtoB(final ArrDataStructureRef arrDataPartyRef,
                                        final ArrDataStructureRef arrDataPartyRefNew,
                                        final MappingContext context) {
                        arrDataPartyRefNew.setDataType(arrDataPartyRef.getDataType());
                        arrDataPartyRefNew.setStructureData(arrDataPartyRef.getStructureData());
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
                    throw new BusinessException("Nebyl odeslán formát dat", BaseCode.PROPERTY_NOT_EXIST).set("property", "format");
                } else {
                    String format = arrItemUnitdate.getFormat();
                    if (!format.matches(
                            "(" + PATTERN_UNIT_DATA + ")|(" + PATTERN_UNIT_DATA + INTERVAL_DELIMITER_UNIT_DATA
                            + PATTERN_UNIT_DATA + ")")) {
                        throw new BusinessException("Neplatný formát dat", BaseCode.PROPERTY_IS_INVALID).set("property", "format");
                    }
                }
                arrDataUnitdate.setFormat(arrItemUnitdate.getFormat());

                if (arrItemUnitdate.getCalendarType() == null) {
                    throw new BusinessException("Nebyl zvolen kalendar", BaseCode.PROPERTY_NOT_EXIST).set("property", "calendarType");
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
                    throw new BusinessException("Nebyl zadan platny format datumu 'od'", e, BaseCode.PROPERTY_IS_INVALID).set("property", "valueFrom");
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
                    throw new BusinessException("Nebyl zadan platny format datumu 'od'", e, BaseCode.PROPERTY_IS_INVALID).set("property", "valueTo");
                }

                if (arrItemUnitdate.getValueFrom() != null && arrItemUnitdate.getValueTo() != null) {
                    LocalDateTime from = LocalDateTime
                            .parse(arrItemUnitdate.getValueFrom(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    LocalDateTime to = LocalDateTime
                            .parse(arrItemUnitdate.getValueTo(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    if (from.isAfter(to)) {
                        throw new BusinessException("Neplatný interval ISO datumů: od > do", BaseCode.PROPERTY_IS_INVALID).set("property", "valueFrom > valueTo");
                    }
                } else if (arrItemUnitdate.getValueFrom() == null
                        && arrItemUnitdate.getValueTo() == null) {
                    throw new BusinessException("Neplatný interval ISO datumů: od > do", BaseCode.PROPERTY_NOT_EXIST).set("property", Arrays.asList("valueFrom", "valueTo"));
                }

                String codeCalendar = arrItemUnitdate.getCalendarType().getCode();
                CalendarType calendarType = CalendarType.valueOf(codeCalendar);

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

        factory.classMap(ArrDataUnitdate.class, ArrDataUnitdate.class)
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
                        arrDataUnitdateNew.setNormalizedTo(arrDataUnitdate.getNormalizedTo());
                        arrDataUnitdateNew.setNormalizedFrom(arrDataUnitdate.getNormalizedFrom());
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
        mapRepository.put(ArrDataStructureRef.class, dataStructureRefRepository);
        mapRepository.put(ArrDataFileRef.class, dataFileRefRepository);
        mapRepository.put(ArrDataNull.class, dataNullRepository);
        mapRepository.put(ArrDataJsonTable.class, dataJsonTableRepository);
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

    @Deprecated
    public <T extends ArrItem> void copyItemData(final T dataFrom, final T dataTo) {
        ArrData data = dataFrom.getData();

        ArrData dataNew;
        try {
            dataNew = data.getClass().getConstructor().newInstance();
            BeanUtils.copyProperties(data, dataNew, "dataId");
            dataTo.setData(dataNew);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new SystemException(e.getCause());
        }

        dataRepository.save(dataNew);
    }


    /**
     * Kontrola typu a specifikace.
     *
     * @param item hodnota atributu
     */
    public void checkValidTypeAndSpec(final ArrItem item) {
        RulItemType itemType = item.getItemType();
        RulItemSpec itemSpec = item.getItemSpec();

        Assert.notNull(itemType, "Hodnota atributu musí mít vyplněný typ");

        if (itemType.getUseSpecification()) {
            Assert.notNull(itemSpec, "Pro typ atributu je nutné specifikaci vyplnit");
        } else {
            Assert.isNull(itemSpec, "Pro typ atributu nesmí být specifikace vyplněná");
        }

        if (itemSpec != null) {
            List<RulItemSpec> descItemSpecs = itemSpecRepository.findByItemType(itemType);
            if (!descItemSpecs.contains(itemSpec)) {
                throw new SystemException("Specifikace neodpovídá typu hodnoty atributu");
            }
        }
    }

    /**
     * Kontrola otevřené verze.
     *
     * @param fundVersion verze
     */
    public void checkFundVersionLock(final ArrFundVersion fundVersion) {
        if (fundVersion.getLockChange() != null) {
            throw new BusinessException("Nelze provést verzovanou změnu v uzavřené verzi.", ArrangementCode.VERSION_ALREADY_CLOSED);
        }
    }

    /**
     * Provede posun (a odverzování) hodnot atributů s daty o požadovaný počet.
     *
     * @param change změna operace
     * @param items  seznam posunovaných hodnot atributu
     * @param diff   počet a směr posunu
     */
    public <T extends ArrItem> void copyItems(final ArrChange change,
                                              final List<T> items,
                                              final Integer diff,
                                              final ArrFundVersion version) {
        for (T itemMove : items) {

            T itemNew = copyItem(change, itemMove, itemMove.getPosition() + diff);

            // sockety
            // TODO: publishChangeDescItem(version, descItemNew);

            // pro odverzovanou hodnotu atributu je nutné vytvořit kopii dat
            copyItemData(itemMove, itemNew);
            itemRepository.save(itemNew);
        }
    }

    /**
     * Vytvoří kopii item. Původní hodnotu uzavře a vytvoří novou se stejnými daty (odverzování)
     *
     * @param change   změna, se kterou dojde k uzamčení a vytvoření kopie
     * @param item     hodnota ke zkopírování
     * @param position pozice atributu
     * @return kopie atributu4
     */
    private <T extends ArrItem> T copyItem(final ArrChange change, final T item, final int position) {
        item.setDeleteChange(change);
        itemRepository.save(item);

        T itemNew = null;
        try {
            itemNew = (T) item.getClass().getConstructor().newInstance();
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }

        BeanUtils.copyProperties(item, itemNew, "itemId", "deleteChange");
        itemNew.setCreateChange(change);
        itemNew.setPosition(position);

        return itemNew;
    }

    /**
     * Kopíruje všechny property krom propert, které má zadaná třída.
     *
     * @param from   z objektu
     * @param to     do objektu
     * @param aClass ignorovaná třída (subclass)
     * @param <T>    ignorovaná třída (subclass)
     * @param <TYPE> kopírovaná třída
     */
    public <T, TYPE extends T> void copyPropertiesSubclass(final TYPE from, final TYPE to, final Class<T> aClass) {
        String[] ignoreProperties;
        PropertyDescriptor[] descriptors = BeanUtils.getPropertyDescriptors(aClass);
        ignoreProperties = new String[descriptors.length];
        for (int i = 0; i < descriptors.length; i++) {
            ignoreProperties[i] = descriptors[i].getName();
        }

        BeanUtils.copyProperties(from, to, ignoreProperties);
    }

    /**
     * Načte z DB item dle příslušného typu
     *
     * @param itemId ID požadovaného itemu
     * @return item dle příslušného typu
     */
    public ArrItem loadDataById(final Integer itemId) {
        return itemRepository.findOne(itemId);
    }

    /**
     * Donačítá položky, které jsou typově jako odkaz, podle ID.
     *
     * @param dataItems seznam položek, které je potřeba donačíst podle ID návazných entit
     */
    public void refItemsLoader(final List<ArrItem> dataItems) {

        // mapy pro naplnění ID entit
        Map<Integer, ArrDataPartyRef> partyMap = new HashMap<>();
        Map<Integer, ArrDataStructureRef> structureMap = new HashMap<>();
        Map<Integer, ArrDataFileRef> fileMap = new HashMap<>();
        Map<Integer, ArrDataRecordRef> recordMap = new HashMap<>();

        // prohledávám pouze entity, které mají návazné data
        for (ArrItem dataItem : dataItems) {
            ArrData data = dataItem.getData();
            if (data != null) {
                if (data instanceof ArrDataPartyRef) {
                    ParParty party = ((ArrDataPartyRef) data).getParty();
                    partyMap.put(party.getPartyId(), (ArrDataPartyRef) data);
                } else if (data instanceof ArrDataStructureRef) {
                    ArrStructureData structureData = ((ArrDataStructureRef) data).getStructureData();
                    structureMap.put(structureData.getStructureDataId(), (ArrDataStructureRef) data);
                } else if (data instanceof ArrDataFileRef) {
                    ArrFile file = ((ArrDataFileRef) data).getFile();
                    fileMap.put(file.getFileId(), (ArrDataFileRef) data);
                } else if (data instanceof ArrDataRecordRef) {
                    RegRecord record = ((ArrDataRecordRef) data).getRecord();
                    recordMap.put(record.getRecordId(), (ArrDataRecordRef) data);
                }
            }
        }

        Set<Integer> structureDataIds = structureMap.keySet();
        List<ArrStructureData> structureDataEntities = structureDataRepository.findAll(structureDataIds);
        for (ArrStructureData structureDataEntity : structureDataEntities) {
            structureMap.get(structureDataEntity.getStructureDataId()).setStructureData(structureDataEntity);
        }

        Set<Integer> partyIds = partyMap.keySet();
        List<ParParty> partyEntities = partyRepository.findAll(partyIds);
        for (ParParty partyEntity : partyEntities) {
            partyMap.get(partyEntity.getPartyId()).setParty(partyEntity);
        }

        Set<Integer> fileIds = partyMap.keySet();
        List<ArrFile> fileEntities = fundFileRepository.findAll(fileIds);
        for (ArrFile fileEntity : fileEntities) {
            ArrDataFileRef ref = fileMap.get(fileEntity.getFileId());
            if (ref != null) {
                ref.setFile(fileEntity);
            }
        }

        Set<Integer> recordIds = recordMap.keySet();
        List<RegRecord> recordEntities = recordRepository.findAll(recordIds);
        for (RegRecord recordEntity : recordEntities) {
            recordMap.get(recordEntity.getRecordId()).setRecord(recordEntity);
        }
    }
}
