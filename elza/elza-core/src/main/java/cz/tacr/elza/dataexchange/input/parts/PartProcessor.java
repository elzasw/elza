package cz.tacr.elza.dataexchange.input.parts;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.aps.context.AccessPointInfo;
import cz.tacr.elza.dataexchange.input.aps.context.AccessPointsContext;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.parts.context.ItemWrapper;
import cz.tacr.elza.dataexchange.input.parts.context.PartInfo;
import cz.tacr.elza.dataexchange.input.parts.context.PartsContext;
import cz.tacr.elza.dataexchange.input.reader.ItemProcessor;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.schema.v2.*;
import cz.tacr.elza.service.AccessPointDataService;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PartProcessor<P extends Party, E extends ApPart> implements ItemProcessor {


    private AccessPointDataService accessPointDataService;

    protected final Class<E> apPartClass;

    private final PartsContext partsContext;

    private final AccessPointsContext apContext;

    private final StaticDataProvider staticData;

    protected P party;

    protected PartInfo info;

    public PartProcessor(ImportContext context, Class<E> apPartClass) {
        this.apPartClass = apPartClass;
        this.partsContext = context.getParts();
        this.apContext = context.getAccessPoints();
        this.staticData = context.getStaticData();
    }

    @Override
    public void process(Object item) {
        PartyGroup partyGroup = (PartyGroup) item;
        processInternal(partyGroup);
    }

    private void processInternal(PartyGroup partyGroup) {
        processParts(partyGroup);
    }

    private void processParts(PartyGroup pg) {
        AccessPointInfo apInfo = apContext.getApInfo(pg.getId());
        processNames(pg.getNms(), apInfo);
        processPartyGroupPart("PT_BODY", "BRIEF_DESC", pg.getChr(), apInfo);
        processPartyGroupPart("PT_BODY", "SOURCE_INFO", pg.getSrc(), apInfo);
        processPartyGroupPart("PT_BODY", "HISTORY", pg.getHst(), apInfo);
        processPartyGroupPart("PT_BODY", "FOUNDING_NORMS", pg.getFn(), apInfo);
        processPartyGroupPart("PT_BODY", "CORP_PURPOSE", pg.getScp(), apInfo);
        processPartyGroupPart("PT_BODY", "CORP_STRUCTURE", pg.getStr(), apInfo);
        processPartyGroupPart("PT_BODY", "SCOPE_NORMS", pg.getSn(), apInfo);
    }

    private void processNames(PartyNames names, AccessPointInfo apInfo) {
        RulPartType type = staticData.getDefaultPartType();
        for (PartyName name : names.getNm()) {
            E partEntity = createPart(type, apInfo);
            List<ItemWrapper> itemWrapperList = processName(name, partEntity);
            info = partsContext.addPart(partEntity, String.valueOf(partsContext.getCurrentImportId()), apInfo, type, itemWrapperList);
        }
    }

    private List<ItemWrapper> processName(PartyName name, E partEntity) {
        if (StringUtils.isEmpty(name.getMain())) {
            throw new DEImportException("Main part of name not set, partId=" + party.getId());
        }
        List<ItemWrapper> returnList = new ArrayList<>();

        ItemType itemType = staticData.getItemTypeByCode("NM_MAIN");
        ApItem mainEntity = createApItem(partEntity, createItemData(itemType, name.getMain()), itemType, null,returnList);
        ItemWrapper itemWrapper = partsContext.addItem(mainEntity, info);
        returnList.add(itemWrapper);

        itemType = staticData.getItemTypeByCode("NM_TYPE");
        ApItem entity = createApItem(partEntity, createItemData(itemType, null), itemType, name.getFt(), returnList);
        itemWrapper = partsContext.addItem(entity, info);
        returnList.add(itemWrapper);


        if (name.getOth() != null && !name.getOth().isEmpty()) {
            itemType = staticData.getItemTypeByCode("NM_MINOR");
            entity = createApItem(partEntity, createItemData(itemType, name.getOth()), itemType,null, returnList);
            itemWrapper = partsContext.addItem(entity, info);
            returnList.add(itemWrapper);
        }

        if (name.getNote() != null && !name.getNote().isEmpty()) {
            itemType = staticData.getItemTypeByCode("NOTE");
            entity = createApItem(partEntity, createItemData(itemType, name.getNote()), itemType, null,returnList);
            itemWrapper = partsContext.addItem(entity, info);
            returnList.add(itemWrapper);
        }

        if (name.getDgb() != null && !name.getDgb().isEmpty()) {
            itemType = staticData.getItemTypeByCode("NM_DEGREE_PRE");
            entity = createApItem(partEntity, createItemData(itemType, name.getDgb()), itemType,null, returnList);
            itemWrapper = partsContext.addItem(entity, info);
            returnList.add(itemWrapper);
        }

        if (name.getDga() != null && !name.getDga().isEmpty()) {
            itemType = staticData.getItemTypeByCode("NM_DEGREE_POST");
            entity = createApItem(partEntity, createItemData(itemType, name.getDga()), itemType, null, returnList);
            itemWrapper = partsContext.addItem(entity, info);
            returnList.add(itemWrapper);
        }

        if (name.getNcs() != null && !name.getNcs().getNc().isEmpty()) {
            for (NameComplement nc : name.getNcs().getNc()) {
                switch (nc.getCt()) {
                    case "INITIALS":
                        String dataInit = ((ArrDataString) mainEntity.getData()).getValue();
                        ((ArrDataString) mainEntity.getData()).setValue(dataInit + " " + nc.getV());
                        break;
                    case "ROMAN_NUM":
                        String dataRomanNum = ((ArrDataString) mainEntity.getData()).getValue();
                        ((ArrDataString) mainEntity.getData()).setValue(dataRomanNum + " " + nc.getV());
                        break;
                    case "GENERAL":
                        itemType = staticData.getItemTypeByCode("NM_SUP_GEN");
                        entity = createApItem(partEntity, createItemData(itemType, nc.getV()), itemType, null,returnList);
                        itemWrapper = partsContext.addItem(entity, info);
                        returnList.add(itemWrapper);
                        break;
                    case "GEO":
                        itemType = staticData.getItemTypeByCode("NM_SUP_GEO");
                        entity = createApItem(partEntity, createItemData(itemType, nc.getV()), itemType, null, returnList);
                        itemWrapper = partsContext.addItem(entity, info);
                        returnList.add(itemWrapper);
                        break;
                    case "TIME":
                        itemType = staticData.getItemTypeByCode("NM_SUP_CHRO");
                        entity = createApItem(partEntity, createItemData(itemType, nc.getV()), itemType, null, returnList);
                        itemWrapper = partsContext.addItem(entity, info);
                        returnList.add(itemWrapper);
                        break;
                    case "ORDER":
                        itemType = staticData.getItemTypeByCode("NM_ORDER");
                        entity = createApItem(partEntity, createItemData(itemType, nc.getV()), itemType, null, returnList);
                        itemWrapper = partsContext.addItem(entity, info);
                        returnList.add(itemWrapper);
                        break;
                }
            }
        }

        //entity.setItemSpec(staticData.getItemSpecByCode(name.getFt()));
        return returnList;
    }

    private void processPartyGroupPart(String partTypeCode, String itemTypeCode, String value, AccessPointInfo apInfo) {
        if(value != null && !value.isEmpty()) {
            RulPartType type = staticData.getPartTypeByCode(partTypeCode);
            E partEntity = createPart(type, apInfo);

            List<ItemWrapper> itemWrapperList = new ArrayList<>();

            ItemType itemType = staticData.getItemTypeByCode(itemTypeCode);
            ApItem entity = createApItem(partEntity, createItemData(itemType, value), itemType, null, itemWrapperList);
            ItemWrapper itemWrapper = partsContext.addItem(entity, info);
            itemWrapperList.add(itemWrapper);
            info = partsContext.addPart(partEntity, String.valueOf(partsContext.getCurrentImportId()), apInfo, type, itemWrapperList);
        }
    }

    private ApItem createApItem(ApPart partEntity, ArrData data, ItemType itemType, String itemSpecCode, List<ItemWrapper> itemList) {
        ApItem apItem = new ApItem();
        apItem.setPart(partEntity);
        apItem.setData(data);
        apItem.setItemType(itemType.getEntity());
        if(itemSpecCode != null) apItem.setItemSpec(itemType.getItemSpecByCode(itemSpecCode));
        apItem.setCreateChange(apContext.getCreateChange());
        apItem.setObjectId(apContext.nextItemObjectId());
        apItem.setPosition(nextPosition(itemList));
        return apItem;
    }

    private int nextPosition(List<ItemWrapper> itemWrapperList) {
        if (itemWrapperList.size() == 0) {
            return 1;
        }
        int position = 2;
        for (ItemWrapper existsItem : itemWrapperList) {
            ApItem apItem = (ApItem) existsItem.getEntity();
            if (apItem.getDeleteChange() == null) {
                if (apItem.getPosition() >= position) {
                    position = apItem.getPosition() + 1;
                }
            }
        }
        return position;
    }

    protected ArrData createItemData(ItemType itemType, String value) {
        ArrData data = null;
        switch (itemType.getDataType().getCode()) {
            case "FORMATTED_TEXT":
            case "TEXT":
                ArrDataText dataText = new ArrDataText();
                dataText.setValue(value);
                data = dataText;
                break;
            case "STRING":
                ArrDataString itemString = new ArrDataString();
                itemString.setValue(value);
                data = itemString;
                break;
            case "INT":
                ArrDataInteger itemInteger = new ArrDataInteger();
                itemInteger.setValue(Integer.valueOf(value));
                data = itemInteger;
                break;
            case "DATE":
                ArrDataDate dataDate = new ArrDataDate();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                LocalDate localDate = LocalDate.parse(value, formatter);
                dataDate.setValue(localDate);
                dataDate.setDataType(DataType.DATE.getEntity());
                data = dataDate;
                break;
            case "UNITID":
                ArrDataUnitid itemUnitid = new ArrDataUnitid();
                itemUnitid.setUnitId(value);
                data = itemUnitid;
                break;
            case "UNITDATE":
                // TODO : gotzy doresit
                /*ArrDataUnitdate itemUnitdate = createArrDataUnitdate(text);
                data = itemUnitdate;*/
                break;
            case "COORDINATES":
                break;
            case "RECORD_REF":
                // TODO : gotzy doresit
                /*ArrDataRecordRef itemRecordRef = new ArrDataRecordRef();
                ApAccessPoint record = apAccessPointRepository.getOneCheckExist(Integer.valueOf(text));
                itemRecordRef.setRecord(record);
                data = itemRecordRef;*/
                break;
            case "BIT":
                ArrDataBit itemBit = new ArrDataBit();
                itemBit.setValue(Boolean.valueOf(value));
                data = itemBit;
                break;
            case "URI-REF":
                ArrDataUriRef itemUriRef = new ArrDataUriRef();
                itemUriRef.setValue(value);
                data = itemUriRef;
                break;
            case "DECIMAL":
                break;
            case "STRUCTURED":
                break;
            case "ENUM":
                ArrDataNull itemNull = new ArrDataNull();
                data = itemNull;
                break;
            default:
                throw new SystemException("Neplatný typ atributu " + itemType.getDataType().getCode(), BaseCode.INVALID_STATE);
        }
        data.setDataType(itemType.getDataType().getEntity());
        return data;
    }

    protected E createPart(RulPartType type, AccessPointInfo apInfo) {
        E entity;
        try {
            entity = apPartClass.newInstance();
        } catch (Exception e) {
            throw new SystemException("Failed to intialized no arg constructor, entity: " + apPartClass, e);
        }
        entity.setPartType(type);
        entity.setCreateChange(apContext.getCreateChange());
        entity.setState(ApStateEnum.OK);
        return entity;
    }

}
