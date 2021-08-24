package cz.tacr.elza.service.cam;

import cz.tacr.cam.schema.cam.BinaryStreamXml;
import cz.tacr.cam.schema.cam.BooleanXml;
import cz.tacr.cam.schema.cam.CodeXml;
import cz.tacr.cam.schema.cam.EntityRecordRefXml;
import cz.tacr.cam.schema.cam.IntegerXml;
import cz.tacr.cam.schema.cam.ItemBinaryXml;
import cz.tacr.cam.schema.cam.ItemBooleanXml;
import cz.tacr.cam.schema.cam.ItemEntityRefXml;
import cz.tacr.cam.schema.cam.ItemEnumXml;
import cz.tacr.cam.schema.cam.ItemIntegerXml;
import cz.tacr.cam.schema.cam.ItemLinkXml;
import cz.tacr.cam.schema.cam.ItemStringXml;
import cz.tacr.cam.schema.cam.ItemUnitDateXml;
import cz.tacr.cam.schema.cam.ObjectFactory;
import cz.tacr.cam.schema.cam.PartTypeXml;
import cz.tacr.cam.schema.cam.PartXml;
import cz.tacr.cam.schema.cam.StringXml;
import cz.tacr.cam.schema.cam.UuidXml;
import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.output.writer.cam.CamUtils;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataBit;
import cz.tacr.elza.domain.ArrDataCoordinates;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataNull;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.ArrDataUriRef;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulPartType;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.service.AccessPointDataService;
import cz.tacr.elza.service.GroovyService;

/**
 * Factory methods for CAM XML
 *
 */
public class CamXmlFactory {

    interface EntityRefHandler {
        /**
         * Return record ref
         * 
         * Return null if record should not be created
         * 
         * @param recordRef
         * @return
         */
        EntityRecordRefXml createEntityRef(ArrDataRecordRef recordRef);
    }

    final protected static ObjectFactory objectFactory = CamUtils.getObjectFactory();

    static public ObjectFactory getObjectFactory() {
        return objectFactory;
    }

    static public Object createItem(StaticDataProvider sdp,
                                    ApItem item,
                                    String uuid,
                                    EntityRefHandler entityRefHandler,
                                    GroovyService groovyService,
                                    AccessPointDataService apDataService,
                                    String externalSystemTypeCode,
                                    ApScope scope) {
        ItemType itemType = sdp.getItemTypeById(item.getItemTypeId());

        String camItemTypeCode = groovyService.findItemTypeCode(externalSystemTypeCode, itemType
                .getCode(), scope.getRuleSetId());
        if (camItemTypeCode == null) {
            return null;
        }

        CodeXml itemTypeCode = new CodeXml(camItemTypeCode);
        CodeXml itemSpecCode;
        if (item.getItemSpecId() != null) {
            RulItemSpec itemSpec = itemType.getItemSpecById(item.getItemSpecId());
            String camItemSpecCode = groovyService.findItemSpecCode(externalSystemTypeCode, itemSpec.getCode(), scope.getRuleSetId());
            if (camItemSpecCode == null) {
                return null;
            }

            itemSpecCode = new CodeXml(camItemSpecCode);
        } else {
            itemSpecCode = null;
        }
        UuidXml uuidXml = new UuidXml(uuid);

        ArrData data = HibernateUtils.unproxy(item.getData());
        DataType dataType = DataType.fromId(itemType.getDataTypeId());
        switch (dataType) {
        case BIT:
            return convertBoolean(data, itemTypeCode, itemSpecCode, uuidXml);
        case URI_REF:
            return convertUriRef(data, itemTypeCode, itemSpecCode, uuidXml);
        case TEXT:
            return convertText(data, itemTypeCode, itemSpecCode, uuidXml);
        case STRING:
            return convertString(data, itemTypeCode, itemSpecCode, uuidXml);
        case INT:
            return convertInteger(data, itemTypeCode, itemSpecCode, uuidXml);
        case UNITDATE:
            return convertUnitdate(data, itemTypeCode, itemSpecCode, uuidXml);
        case ENUM:
            return convertEnum(data, itemTypeCode, itemSpecCode, uuidXml);
        case RECORD_REF:
            return convertEntityRef(data, itemTypeCode, itemSpecCode, uuidXml, entityRefHandler);
        case COORDINATES:
            return convertCoordinates(data, itemTypeCode, itemSpecCode, uuidXml, apDataService);
        default:
            throw new BusinessException("Failed to export item, unsupported data type: " + dataType +
                    ", itemId:" + item.getItemId() +
                    ", class: " + data.getClass(),
                    BaseCode.EXPORT_FAILED);
        }
    }

    private static ItemBinaryXml convertCoordinates(ArrData data, CodeXml itemTypeCode, CodeXml itemSpecCode,
                                                    UuidXml uuidXml,
                                                    AccessPointDataService apDataService) {
        if (!(data instanceof ArrDataCoordinates)) {
            throw new BusinessException("Failed to convert data: " + data.getDataId(),
                    BaseCode.EXPORT_FAILED);
        }
        ArrDataCoordinates dataCoordinates = (ArrDataCoordinates) data;
        ItemBinaryXml itemCoordinates = new ItemBinaryXml();
        itemCoordinates.setValue(new BinaryStreamXml(apDataService.convertGeometryToWKB(dataCoordinates.getValue())));
        itemCoordinates.setT(itemTypeCode);
        itemCoordinates.setS(itemSpecCode);
        itemCoordinates.setUuid(uuidXml);
        return itemCoordinates;
    }

    private static ItemEntityRefXml convertEntityRef(ArrData data, CodeXml itemTypeCode, CodeXml itemSpecCode,
                                                     UuidXml uuidXml, EntityRefHandler entityRefHandler) {
        if (!(data instanceof ArrDataRecordRef)) {
            throw new BusinessException("Failed to convert data: " + data.getDataId(),
                    BaseCode.EXPORT_FAILED);
        }
        ArrDataRecordRef dataRecordRef = (ArrDataRecordRef) data;

        EntityRecordRefXml entityRecordRef = entityRefHandler.createEntityRef(dataRecordRef);
        // check if we have link to external entity
        if (entityRecordRef == null) {
            return null;
        }

        ItemEntityRefXml itemEntityRef = new ItemEntityRefXml();
        itemEntityRef.setRef(entityRecordRef);
        itemEntityRef.setT(itemTypeCode);
        itemEntityRef.setS(itemSpecCode);
        itemEntityRef.setUuid(uuidXml);
        return itemEntityRef;
    }

    private static ItemEnumXml convertEnum(ArrData data, CodeXml itemTypeCode, CodeXml itemSpecCode, UuidXml uuidXml) {
        if (!(data instanceof ArrDataNull)) {
            throw new BusinessException("Failed to convert data: " + data.getDataId(),
                    BaseCode.EXPORT_FAILED);
        }

        ItemEnumXml itemEnum = new ItemEnumXml();
        itemEnum.setT(itemTypeCode);
        itemEnum.setS(itemSpecCode);
        itemEnum.setUuid(uuidXml);
        return itemEnum;
    }

    private static ItemUnitDateXml convertUnitdate(ArrData data, CodeXml itemTypeCode, CodeXml itemSpecCode,
                                                   UuidXml uuidXml) {
        if (!(data instanceof ArrDataUnitdate)) {
            throw new BusinessException("Failed to convert data: " + data.getDataId(),
                    BaseCode.EXPORT_FAILED);
        }

        ArrDataUnitdate dataUnitdate = (ArrDataUnitdate) data;
        ItemUnitDateXml itemUnitDate = new ItemUnitDateXml();
        itemUnitDate.setF(dataUnitdate.getValueFrom());
        itemUnitDate.setFe(dataUnitdate.getValueFromEstimated());
        itemUnitDate.setFmt(dataUnitdate.getFormat());
        itemUnitDate.setTo(dataUnitdate.getValueTo());
        itemUnitDate.setToe(dataUnitdate.getValueToEstimated());
        itemUnitDate.setT(itemTypeCode);
        itemUnitDate.setS(itemSpecCode);
        itemUnitDate.setUuid(uuidXml);
        return itemUnitDate;
    }

    private static ItemIntegerXml convertInteger(ArrData data, CodeXml itemTypeCode, CodeXml itemSpecCode,
                                                 UuidXml uuidXml) {
        if (!(data instanceof ArrDataInteger)) {
            throw new BusinessException("Failed to convert data: " + data.getDataId(),
                    BaseCode.EXPORT_FAILED);
        }

        ArrDataInteger dataInteger = (ArrDataInteger) data;
        ItemIntegerXml itemInteger = new ItemIntegerXml();
        itemInteger.setValue(new IntegerXml(dataInteger.getValueInt().longValue()));
        itemInteger.setT(itemTypeCode);
        itemInteger.setS(itemSpecCode);
        itemInteger.setUuid(uuidXml);
        return itemInteger;
    }

    private static ItemStringXml convertString(ArrData data, CodeXml itemTypeCode, CodeXml itemSpecCode,
                                               UuidXml uuidXml) {
        if (!(data instanceof ArrDataString)) {
            throw new BusinessException("Failed to convert data: " + data.getDataId(),
                    BaseCode.EXPORT_FAILED);
        }
        ArrDataString dataString = (ArrDataString) data;
        ItemStringXml itemString = new ItemStringXml();
        itemString.setValue(new StringXml(dataString.getStringValue()));
        itemString.setT(itemTypeCode);
        itemString.setS(itemSpecCode);
        itemString.setUuid(uuidXml);
        return itemString;
    }

    private static ItemStringXml convertText(ArrData data, CodeXml itemTypeCode, CodeXml itemSpecCode,
                                             UuidXml uuidXml) {
        if (!(data instanceof ArrDataText)) {
            throw new BusinessException("Failed to convert data: " + data.getDataId(),
                    BaseCode.EXPORT_FAILED);
        }
        ArrDataText dataText = (ArrDataText) data;
        ItemStringXml itemText = new ItemStringXml();
        itemText.setValue(new StringXml(dataText.getTextValue()));
        itemText.setT(itemTypeCode);
        itemText.setS(itemSpecCode);
        itemText.setUuid(uuidXml);
        return itemText;
    }

    private static ItemLinkXml convertUriRef(ArrData data, CodeXml itemTypeCode, CodeXml itemSpecCode,
                                             UuidXml uuidXml) {
        if (!(data instanceof ArrDataUriRef)) {
            throw new BusinessException("Failed to convert data: " + data.getDataId(),
                    BaseCode.EXPORT_FAILED);
        }
        ArrDataUriRef dataUriRef = (ArrDataUriRef) data;
        ItemLinkXml itemLink = new ItemLinkXml();
        itemLink.setUrl(new StringXml(dataUriRef.getUriRefValue()));
        itemLink.setNm(new StringXml(dataUriRef.getDescription() != null ? dataUriRef.getDescription() : ""));
        itemLink.setT(itemTypeCode);
        itemLink.setS(itemSpecCode);
        itemLink.setUuid(uuidXml);
        return itemLink;
    }

    private static ItemBooleanXml convertBoolean(ArrData data, CodeXml itemTypeCode, CodeXml itemSpecCode,
                                                 UuidXml uuidXml) {
        if (!(data instanceof ArrDataBit)) {
            throw new BusinessException("Failed to convert data: " + data.getDataId(),
                    BaseCode.EXPORT_FAILED);
        }
        ArrDataBit dataBit = (ArrDataBit) data;
        ItemBooleanXml itemBoolean = new ItemBooleanXml();
        itemBoolean.setValue(new BooleanXml(dataBit.isBitValue()));
        itemBoolean.setT(itemTypeCode);
        itemBoolean.setS(itemSpecCode);
        itemBoolean.setUuid(uuidXml);
        return itemBoolean;
    }

    public static PartXml createPart(StaticDataProvider sdp, ApPart apPart,
                                     final String parentUuid, String uuid) {
        PartXml part = new PartXml();

        RulPartType partType = sdp.getPartTypeById(apPart.getPartTypeId());
        part.setT(PartTypeXml.fromValue(partType.getCode()));
        part.setPid(new UuidXml(uuid));
        if (parentUuid != null) {
            UuidXml parentUuidXml = objectFactory.createUuidXml();
            parentUuidXml.setValue(parentUuid);
            part.setPrnt(parentUuidXml);
        }

        return part;
    }
}
