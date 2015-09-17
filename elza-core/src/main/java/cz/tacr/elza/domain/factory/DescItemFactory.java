package cz.tacr.elza.domain.factory;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataPartyRef;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.ArrDataUnitid;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrDescItemCoordinates;
import cz.tacr.elza.domain.ArrDescItemFormattedText;
import cz.tacr.elza.domain.ArrDescItemInt;
import cz.tacr.elza.domain.ArrDescItemPartyRef;
import cz.tacr.elza.domain.ArrDescItemRecordRef;
import cz.tacr.elza.domain.ArrDescItemString;
import cz.tacr.elza.domain.ArrDescItemText;
import cz.tacr.elza.domain.ArrDescItemUnitdate;
import cz.tacr.elza.domain.ArrDescItemUnitid;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.repository.DataCoordinatesRepository;
import cz.tacr.elza.repository.DataIntegerRepository;
import cz.tacr.elza.repository.DataPartyRefRepository;
import cz.tacr.elza.repository.DataRecordRefRepository;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.DataStringRepository;
import cz.tacr.elza.repository.DataTextRepository;
import cz.tacr.elza.repository.DataTypeRepository;
import cz.tacr.elza.repository.DataUnitdateRepository;
import cz.tacr.elza.repository.DataUnitidRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.PartyRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import ma.glasnost.orika.CustomMapper;
import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.impl.DefaultMapperFactory;


@Component
public class DescItemFactory implements InitializingBean {

    private final String PROPERTY_FORMAT = "format";

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
    private DataRepository dataRepository;

    @Autowired
    private DataTypeRepository dataTypeRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private RegRecordRepository regRecordRepository;

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

        facade = factory.getMapperFacade();

        initMapRepository();
    }

    private void defineMapCoordinates() {
        factory.classMap(ArrDescItemCoordinates.class, ArrDataCoordinates.class).customize(new CustomMapper<ArrDescItemCoordinates, ArrDataCoordinates>() {

            @Override
            public void mapAtoB(ArrDescItemCoordinates arrDescItemCoordinates, ArrDataCoordinates arrDataCoordinates, MappingContext context) {
                arrDataCoordinates.setDataType(arrDescItemCoordinates.getDescItemType().getDataType());
                arrDataCoordinates.setDescItem(arrDescItemCoordinates);
                arrDataCoordinates.setValue(arrDescItemCoordinates.getValue());
            }

            @Override
            public void mapBtoA(ArrDataCoordinates arrDataCoordinates, ArrDescItemCoordinates arrDescItemExtCoordinates, MappingContext context) {
                arrDescItemExtCoordinates.setValue(arrDataCoordinates.getValue());
            }

        }).register();

        factory.classMap(ArrDataCoordinates.class, ArrDataCoordinates.class).customize(new CustomMapper<ArrDataCoordinates, ArrDataCoordinates>() {
            @Override
            public void mapAtoB(ArrDataCoordinates arrDataCoordinates, ArrDataCoordinates arrDataCoordinatesNew, MappingContext context) {
                arrDataCoordinatesNew.setDataType(arrDataCoordinates.getDataType());
                arrDataCoordinatesNew.setDescItem(arrDataCoordinates.getDescItem());
                arrDataCoordinatesNew.setValue(arrDataCoordinates.getValue());
            }
        }).register();
    }

    private void defineMapFormattedText() {
        factory.classMap(ArrDescItemFormattedText.class, ArrDataText.class).customize(new CustomMapper<ArrDescItemFormattedText, ArrDataText>() {

            @Override
            public void mapAtoB(ArrDescItemFormattedText arrDescItemFormattedText, ArrDataText arrDataText, MappingContext context) {
                arrDataText.setDataType(arrDescItemFormattedText.getDescItemType().getDataType());
                arrDataText.setDescItem(arrDescItemFormattedText);
                arrDataText.setValue(arrDescItemFormattedText.getValue());
            }

            @Override
            public void mapBtoA(ArrDataText arrDataText, ArrDescItemFormattedText arrDescItemFormattedText, MappingContext context) {
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

    private void defineMapInt() {
        factory.classMap(ArrDescItemInt.class, ArrDataInteger.class).customize(new CustomMapper<ArrDescItemInt, ArrDataInteger>() {

            @Override
            public void mapAtoB(ArrDescItemInt arrDescItemInt, ArrDataInteger arrDataInteger, MappingContext context) {
                arrDataInteger.setDataType(arrDescItemInt.getDescItemType().getDataType());
                arrDataInteger.setDescItem(arrDescItemInt);
                arrDataInteger.setValue(arrDescItemInt.getValue());
            }

            @Override
            public void mapBtoA(ArrDataInteger arrDataInteger, ArrDescItemInt arrDescItemInt, MappingContext context) {
                arrDescItemInt.setValue(arrDataInteger.getValue());
            }

        }).register();

        factory.classMap(ArrDataInteger.class, ArrDataInteger.class).customize(new CustomMapper<ArrDataInteger, ArrDataInteger>() {
            @Override
            public void mapAtoB(ArrDataInteger arrDataInteger, ArrDataInteger arrDataIntegerNew, MappingContext context) {
                arrDataIntegerNew.setDataType(arrDataInteger.getDataType());
                arrDataIntegerNew.setDescItem(arrDataInteger.getDescItem());
                arrDataIntegerNew.setValue(arrDataInteger.getValue());
            }
        }).register();
    }

    private void defineMapPartyRef() {
        factory.classMap(ArrDescItemPartyRef.class, ArrDataPartyRef.class).customize(new CustomMapper<ArrDescItemPartyRef, ArrDataPartyRef>() {

            @Override
            public void mapAtoB(ArrDescItemPartyRef arrDescItemPartyRef, ArrDataPartyRef arrDataPartyRef, MappingContext context) {
                arrDataPartyRef.setDataType(arrDescItemPartyRef.getDescItemType().getDataType());
                arrDataPartyRef.setDescItem(arrDescItemPartyRef);
                arrDataPartyRef.setPartyId(arrDescItemPartyRef.getParty().getPartyId());
            }

            @Override
            public void mapBtoA(ArrDataPartyRef arrDataPartyRef, ArrDescItemPartyRef arrDescItemPartyRef, MappingContext context) {
                ParParty party = partyRepository.findOne(arrDataPartyRef.getPartyId());
                arrDescItemPartyRef.setParty(party);
            }

        }).register();

        factory.classMap(ArrDataPartyRef.class, ArrDataPartyRef.class).customize(new CustomMapper<ArrDataPartyRef, ArrDataPartyRef>() {
            @Override
            public void mapAtoB(ArrDataPartyRef arrDataPartyRef, ArrDataPartyRef arrDataPartyRefNew, MappingContext context) {
                arrDataPartyRefNew.setDataType(arrDataPartyRef.getDataType());
                arrDataPartyRefNew.setDescItem(arrDataPartyRef.getDescItem());
                arrDataPartyRefNew.setPartyId(arrDataPartyRef.getPartyId());
            }
        }).register();
    }

    private void defineMapRecordRef() {
        factory.classMap(ArrDescItemRecordRef.class, ArrDataRecordRef.class).customize(new CustomMapper<ArrDescItemRecordRef, ArrDataRecordRef>() {

            @Override
            public void mapAtoB(ArrDescItemRecordRef arrDescItemRecordRef, ArrDataRecordRef arrDataRecordRef, MappingContext context) {
                arrDataRecordRef.setDataType(arrDescItemRecordRef.getDescItemType().getDataType());
                arrDataRecordRef.setDescItem(arrDescItemRecordRef);
                arrDataRecordRef.setRecordId(arrDescItemRecordRef.getRecord().getRecordId());
            }

            @Override
            public void mapBtoA(ArrDataRecordRef arrDataRecordRef, ArrDescItemRecordRef arrDescItemRecordRef, MappingContext context) {
                RegRecord record = regRecordRepository.findOne(arrDataRecordRef.getRecordId());
                arrDescItemRecordRef.setRecord(record);
            }

        }).register();

        factory.classMap(ArrDataRecordRef.class, ArrDataRecordRef.class).customize(new CustomMapper<ArrDataRecordRef, ArrDataRecordRef>() {
            @Override
            public void mapAtoB(ArrDataRecordRef arrDataRecordRef, ArrDataRecordRef arrDataRecordRefNew, MappingContext context) {
                arrDataRecordRefNew.setDataType(arrDataRecordRef.getDataType());
                arrDataRecordRefNew.setDescItem(arrDataRecordRef.getDescItem());
                arrDataRecordRefNew.setRecordId(arrDataRecordRef.getRecordId());
            }
        }).register();
    }

    private void defineMapString() {
        factory.classMap(ArrDescItemString.class, ArrDataString.class).customize(new CustomMapper<ArrDescItemString, ArrDataString>() {

            @Override
            public void mapAtoB(ArrDescItemString arrDescItemString, ArrDataString arrDataString, MappingContext context) {
                arrDataString.setDataType(arrDescItemString.getDescItemType().getDataType());
                arrDataString.setDescItem(arrDescItemString);
                arrDataString.setValue(arrDescItemString.getValue());
            }

            @Override
            public void mapBtoA(ArrDataString arrDataString, ArrDescItemString arrDescItemString, MappingContext context) {
                String formattedValue = formatString(context, arrDataString.getValue());
                arrDescItemString.setValue(formattedValue);
            }
        }).register();

        factory.classMap(ArrDataString.class, ArrDataString.class).customize(new CustomMapper<ArrDataString, ArrDataString>() {
            @Override
            public void mapAtoB(ArrDataString arrDataString, ArrDataString arrDataStringNew, MappingContext context) {
                arrDataStringNew.setDataType(arrDataString.getDataType());
                arrDataStringNew.setDescItem(arrDataString.getDescItem());
                arrDataStringNew.setValue(arrDataString.getValue());
            }
        }).register();
    }

    private void defineMapText() {
        factory.classMap(ArrDescItemText.class, ArrDataText.class).customize(new CustomMapper<ArrDescItemText, ArrDataText>() {

            @Override
            public void mapAtoB(ArrDescItemText arrDescItemText, ArrDataText arrDataText, MappingContext context) {
                arrDataText.setDataType(arrDescItemText.getDescItemType().getDataType());
                arrDataText.setDescItem(arrDescItemText);
                arrDataText.setValue(arrDescItemText.getValue());
            }

            @Override
            public void mapBtoA(ArrDataText arrDataText, ArrDescItemText arrDescItemText, MappingContext context) {
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

    private void defineMapUnitdate() {
        factory.classMap(ArrDescItemUnitdate.class, ArrDataUnitdate.class).customize(new CustomMapper<ArrDescItemUnitdate, ArrDataUnitdate>() {

            @Override
            public void mapAtoB(ArrDescItemUnitdate arrDescItemUnitdate, ArrDataUnitdate arrDataUnitdate, MappingContext context) {
                arrDataUnitdate.setDataType(arrDescItemUnitdate.getDescItemType().getDataType());
                arrDataUnitdate.setDescItem(arrDescItemUnitdate);
                arrDataUnitdate.setValue(arrDescItemUnitdate.getValue());
            }

            @Override
            public void mapBtoA(ArrDataUnitdate arrDataUnitdate, ArrDescItemUnitdate arrDescItemUnitdate, MappingContext context) {
                arrDescItemUnitdate.setValue(arrDataUnitdate.getValue());
            }
        }).register();

        factory.classMap(ArrDataUnitdate.class, ArrDataUnitdate.class).customize(new CustomMapper<ArrDataUnitdate, ArrDataUnitdate>() {
            @Override
            public void mapAtoB(ArrDataUnitdate arrDataUnitdate, ArrDataUnitdate arrDataUnitdateNew, MappingContext context) {
                arrDataUnitdateNew.setDataType(arrDataUnitdate.getDataType());
                arrDataUnitdateNew.setDescItem(arrDataUnitdate.getDescItem());
                arrDataUnitdateNew.setValue(arrDataUnitdate.getValue());
            }
        }).register();
    }

    private void defineMapUnitid() {
        factory.classMap(ArrDescItemUnitid.class, ArrDataUnitid.class).customize(new CustomMapper<ArrDescItemUnitid, ArrDataUnitid>() {

            @Override
            public void mapAtoB(ArrDescItemUnitid arrDescItemUnitid, ArrDataUnitid arrDataUnitid, MappingContext context) {
                arrDataUnitid.setDataType(arrDescItemUnitid.getDescItemType().getDataType());
                arrDataUnitid.setDescItem(arrDescItemUnitid);
                arrDataUnitid.setValue(arrDescItemUnitid.getValue());
            }

            @Override
            public void mapBtoA(ArrDataUnitid arrDataUnitid, ArrDescItemUnitid arrDescItemUnitid, MappingContext context) {
                arrDescItemUnitid.setValue(arrDataUnitid.getValue());
            }
        }).register();

        factory.classMap(ArrDataUnitid.class, ArrDataUnitid.class).customize(new CustomMapper<ArrDataUnitid, ArrDataUnitid>() {
            @Override
            public void mapAtoB(ArrDataUnitid arrDataUnitid, ArrDataUnitid arrDataUnitidNew, MappingContext context) {
                arrDataUnitidNew.setDataType(arrDataUnitid.getDataType());
                arrDataUnitidNew.setDescItem(arrDataUnitid.getDescItem());
                arrDataUnitidNew.setValue(arrDataUnitid.getValue());
            }
        }).register();
    }

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
    }

    public ArrDescItem getDescItem(ArrDescItem descItem) {
        ArrData data = getDataByDescItem(descItem);
        ArrDescItem descItemTmp = createDescItemByType(descItem.getDescItemType().getDataType());
        BeanUtils.copyProperties(descItem, descItemTmp);
        facade.map(data, descItemTmp);
        return descItemTmp;
    }

    public ArrDescItem getDescItem(ArrDescItem descItem, String formatData) {
        ArrData data = getDataByDescItem(descItem);
        ArrDescItem descItemTmp = createDescItemByType(data.getDataType());
        BeanUtils.copyProperties(descItem, descItemTmp);
        Map<Object,Object> map = new HashMap<>();
        map.put(PROPERTY_FORMAT, formatData);
        MappingContext x = new MappingContext(map);
        facade.map(data, descItemTmp, x);
        return descItemTmp;
    }

    private ArrData getDataByDescItem(ArrDescItem descItem) {
        List<ArrData> dataList = dataRepository.findByDescItem(descItem);
        if (dataList.size() != 1) {
            throw new IllegalStateException("Hodnota musí být právě jedna");
        }
        return dataList.get(0);
    }

    public ArrDescItem saveDescItem(ArrDescItem descItem, Boolean createNewVersion) {

        // TODO: nějak normálněj?
        ArrDescItem x = new ArrDescItem();
        BeanUtils.copyProperties(descItem, x);
        x = descItemRepository.save(x);
        BeanUtils.copyProperties(x, descItem);

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
            default:
                throw new NotImplementedException("Nebyl namapován datový typ");
        }
    }

    public void copyDescItemValues(ArrDescItem descItemFrom, ArrDescItem descItemTo) {
        ArrData data = getDataByDescItem(descItemFrom);
        ArrData dataNew = facade.map(data, data.getClass());
        dataNew.setDescItem(descItemTo);
        try {
            mapRepository.get(dataNew.getClass()).save(data);
        } catch (NullPointerException e) {
            e.printStackTrace();
            throw new NotImplementedException("Nebyla namapována repozitory pro datový typ");
        }
    }

    private String formatString(final MappingContext context, final String value) {
        String valueRet = value;
        if (context != null) {
            String format = (String) context.getProperty(PROPERTY_FORMAT);
            if (format != null) {
                String stringValue = value;
                if (stringValue != null && stringValue.length() > 250 && format != null && ArrangementManager.FORMAT_ATTRIBUTE_SHORT.equals(format)) {
                    valueRet = stringValue.substring(0, 250);
                }
            }
        }
        return valueRet;
    }




}
