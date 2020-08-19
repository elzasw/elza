package cz.tacr.elza.service.cam;

import cz.tacr.cam.schema.cam.BooleanXml;
import cz.tacr.cam.schema.cam.CodeXml;
import cz.tacr.cam.schema.cam.EntityRecordRefXml;
import cz.tacr.cam.schema.cam.IntegerXml;
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
import cz.tacr.elza.common.GeometryConvertor;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.output.writer.cam.CamUtils;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ArrDataBit;
import cz.tacr.elza.domain.ArrDataCoordinates;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.ArrDataUriRef;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulPartType;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;

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

    static public Object createItem(StaticDataProvider sdp, ApItem item, String uuid,
                                    EntityRefHandler entityRefHandler) {
        ItemType itemType = sdp.getItemTypeById(item.getItemTypeId());

        CodeXml itemTypeCode = new CodeXml(itemType.getCode());
        CodeXml itemSpecCode;
        if (item.getItemSpecId() != null) {
            RulItemSpec itemSpec = itemType.getItemSpecById(item.getItemSpecId());
            itemSpecCode = new CodeXml(itemSpec.getCode());
        } else {
            itemSpecCode = null;
        }
        UuidXml uuidXml = new UuidXml(uuid);

        DataType dataType = DataType.fromId(itemType.getDataTypeId());
        switch (dataType) {
        case BIT:
            ArrDataBit dataBit = (ArrDataBit) item.getData();
            ItemBooleanXml itemBoolean = new ItemBooleanXml();
            itemBoolean.setValue(new BooleanXml(dataBit.isValue()));
            itemBoolean.setT(itemTypeCode);
            itemBoolean.setS(itemSpecCode);
            itemBoolean.setUuid(uuidXml);
            return itemBoolean;
        case URI_REF:
            ArrDataUriRef dataUriRef = (ArrDataUriRef) item.getData();
            ItemLinkXml itemLink = new ItemLinkXml();
            itemLink.setUrl(new StringXml(dataUriRef.getValue()));
            itemLink.setNm(new StringXml(dataUriRef.getDescription()));
            itemLink.setT(itemTypeCode);
            itemLink.setS(itemSpecCode);
            itemLink.setUuid(uuidXml);
            return itemLink;
        case TEXT:
            ArrDataText dataText = (ArrDataText) item.getData();
            ItemStringXml itemText = new ItemStringXml();
            itemText.setValue(new StringXml(dataText.getValue()));
            itemText.setT(itemTypeCode);
            itemText.setS(itemSpecCode);
            itemText.setUuid(uuidXml);
            return itemText;
        case STRING:
            ArrDataString dataString = (ArrDataString) item.getData();
            ItemStringXml itemString = new ItemStringXml();
            itemString.setValue(new StringXml(dataString.getValue()));
            itemString.setT(itemTypeCode);
            itemString.setS(itemSpecCode);
            itemString.setUuid(uuidXml);
            return itemString;
        case INT:
            ArrDataInteger dataInteger = (ArrDataInteger) item.getData();
            ItemIntegerXml itemInteger = new ItemIntegerXml();
            itemInteger.setValue(new IntegerXml(dataInteger.getValueInt().longValue()));
            itemInteger.setT(itemTypeCode);
            itemInteger.setS(itemSpecCode);
            itemInteger.setUuid(uuidXml);
            return itemInteger;
        case UNITDATE:
            ArrDataUnitdate dataUnitdate = (ArrDataUnitdate) item.getData();
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
        case ENUM:
            ItemEnumXml itemEnum = new ItemEnumXml();
            itemEnum.setT(itemTypeCode);
            itemEnum.setS(itemSpecCode);
            itemEnum.setUuid(uuidXml);
            return itemEnum;
        case RECORD_REF:
            ArrDataRecordRef dataRecordRef = (ArrDataRecordRef) item.getData();

            EntityRecordRefXml entityRecordRef = entityRefHandler.createEntityRef(dataRecordRef);

            ItemEntityRefXml itemEntityRef = new ItemEntityRefXml();
            itemEntityRef.setRef(entityRecordRef);
            itemEntityRef.setT(itemTypeCode);
            itemEntityRef.setS(itemSpecCode);
            itemEntityRef.setUuid(uuidXml);
            return itemEntityRef;
        case COORDINATES:
            ArrDataCoordinates dataCoordinates = (ArrDataCoordinates) item.getData();
            ItemStringXml itemCoordinates = new ItemStringXml();
            itemCoordinates.setValue(new StringXml(GeometryConvertor.convert(dataCoordinates.getValue())));
            itemCoordinates.setT(itemTypeCode);
            itemCoordinates.setS(itemSpecCode);
            itemCoordinates.setUuid(uuidXml);
            return itemCoordinates;
        }
        throw new BusinessException("Failed to export item, unsupported data type: " + dataType +
                ", itemId:" + item.getItemId(),
                BaseCode.EXPORT_FAILED);
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
