package cz.tacr.elza.service;

import java.time.format.DateTimeFormatter;
import java.util.List;

import cz.tacr.elza.domain.*;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;
import cz.tacr.elza.domain.vo.CoordinatesTitleValue;
import cz.tacr.elza.domain.vo.JsonTableTitleValue;
import cz.tacr.elza.domain.vo.TitleValue;
import cz.tacr.elza.domain.vo.UnitdateTitleValue;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ApNameRepository;
import cz.tacr.elza.repository.DescItemRepository;

/**
 * Internal service for description items.
 *
 * No permissions are checked on any operation.
 */
@Service
public class DescriptionItemServiceInternal {

    private final DescItemRepository descItemRepository;

    private final StaticDataService staticDataService;

    private final ApNameRepository apNameRepository;

    @Autowired
    public DescriptionItemServiceInternal(DescItemRepository descItemRepository,
                                          StaticDataService staticDataService,
                                          ApNameRepository apNameRepository) {
        this.descItemRepository = descItemRepository;
        this.staticDataService = staticDataService;
        this.apNameRepository = apNameRepository;
    }

    /**
     * Return list of description items for the node.
     *
     * Description items are returned including data.
     *
     * Method is using NodeChage to read current values.
     *
     * @param lockChange
     *            Change for which items are returned. lockChange cannot be null
     * @param node
     * @return
     */
    public List<ArrDescItem> getDescItems(final ArrChange lockChange, final ArrNode node) {
        Validate.notNull(lockChange);
        List<ArrDescItem> itemList;
        itemList = descItemRepository.findByNodeAndChange(node, lockChange);
        return itemList;
    }

    public TitleValue createTitleValue(ArrDescItem descItem, final boolean dataExport) {
        // prepare item specification if present
        RulItemSpec itemSpec = null;
        if (descItem.getItemSpecId() != null) {
            StaticDataProvider staticData = staticDataService.getData();
            itemSpec = staticData.getItemSpecById(descItem.getItemSpecId());
        }
        // create new title value
        TitleValue titleValue = createTitleValueInternal(descItem.getData(), itemSpec, dataExport);
        // set common values
        titleValue.setPosition(descItem.getPosition());
        if (itemSpec != null) {
            String specCode = itemSpec.getCode();
            titleValue.setIconValue(specCode);
            titleValue.setSpecCode(specCode);
            if (dataExport) {
                titleValue.setSpecName(itemSpec.getName());
            }
        }
        return titleValue;
    }

    private TitleValue createTitleValueInternal(ArrData data, RulItemSpec itemSpec, final boolean dataExport) {
        // handle undefined data
        if (data == null) {
            return new TitleValue(ArrangementService.UNDEFINED);
        }
        // resolve title value
        DataType dataType = DataType.fromId(data.getDataTypeId());
        switch (dataType) {
        case ENUM:
            return new TitleValue(itemSpec.getName());
        case PARTY_REF: {
            ArrDataPartyRef partyData = (ArrDataPartyRef) data;
            ApName partyName = apNameRepository.findPreferredNameByAccessPoint(partyData.getParty().getAccessPoint());
            TitleValue value = new TitleValue(partyName.getFullName());
            if (dataExport) {
                value.setEntityId(partyData.getPartyId());
            }
            return value;
        }
        case RECORD_REF: {
            ArrDataRecordRef apData = (ArrDataRecordRef) data;
            ApName apName = apNameRepository.findPreferredNameByAccessPoint(apData.getRecord());
            TitleValue value = new TitleValue(apName.getFullName());
            if (dataExport) {
                value.setEntityId(apName.getAccessPointId());
            }
            return value;
        }
        case STRUCTURED: {
            ArrDataStructureRef structData = (ArrDataStructureRef) data;
            TitleValue value = new TitleValue(structData.getStructuredObject().getValue());
            if (dataExport) {
                value.setEntityId(structData.getStructuredObjectId());
            }
            return value;
        }
        case UNITDATE:
            ArrDataUnitdate unitdateData = (ArrDataUnitdate) data;
            return new UnitdateTitleValue(UnitDateConvertor.convertToString(unitdateData),
                    unitdateData.getCalendarTypeId());
        case STRING:
        case STRING_50:
        case STRING_250:
            ArrDataString strData = (ArrDataString) data;
            return new TitleValue(strData.getValue());
        case TEXT:
        case FORMATTED_TEXT:
            ArrDataText textData = (ArrDataText) data;
            return new TitleValue(textData.getValue());
        case UNITID:
            ArrDataUnitid unitidData = (ArrDataUnitid) data;
            return new TitleValue(unitidData.getUnitId());
        case INT:
            ArrDataInteger intData = (ArrDataInteger) data;
            return new TitleValue(intData.getValue().toString());
        case DECIMAL:
            ArrDataDecimal decimalData = (ArrDataDecimal) data;
            return new TitleValue(decimalData.getValue().toPlainString());
        case COORDINATES:
            ArrDataCoordinates coordsData = (ArrDataCoordinates) data;
            return new CoordinatesTitleValue(coordsData.getValue());
        case JSON_TABLE:
            ArrDataJsonTable table = (ArrDataJsonTable) data;
            return new JsonTableTitleValue(table.getJsonValue(), table.getValue().getRows().size());
        case FILE_REF:
            ArrDataFileRef fileData = (ArrDataFileRef) data;
            return new TitleValue(fileData.getFile().getName());
        case DATE:
            ArrDataDate dataDate = (ArrDataDate) data;
            return new TitleValue(DateTimeFormatter.ISO_LOCAL_DATE.format(dataDate.getValue()));
        case URI_REF:
            ArrDataUriRef uriRef = (ArrDataUriRef) data;
            return new TitleValue(uriRef.getValue());
        case BIT:
            ArrDataBit bit = (ArrDataBit) data;
            return new TitleValue(Boolean.toString(bit.isValue()));
        default:
            throw new SystemException("Failed to create title, uknown data type: " + dataType, BaseCode.SYSTEM_ERROR);
        }
    }
}
