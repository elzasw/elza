package cz.tacr.elza.service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ApIndex;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataBit;
import cz.tacr.elza.domain.ArrDataCoordinates;
import cz.tacr.elza.domain.ArrDataDate;
import cz.tacr.elza.domain.ArrDataDecimal;
import cz.tacr.elza.domain.ArrDataFileRef;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataJsonTable;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataStructureRef;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.ArrDataUnitid;
import cz.tacr.elza.domain.ArrDataUriRef;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrInhibitedItem;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;
import cz.tacr.elza.domain.vo.CoordinatesTitleValue;
import cz.tacr.elza.domain.vo.JsonTableTitleValue;
import cz.tacr.elza.domain.vo.TitleValue;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.InhibitedItemRepository;
import cz.tacr.elza.repository.vo.DataResult;

/**
 * Internal service for description items.
 *
 * No permissions are checked on any operation.
 */
@Service
public class DescriptionItemServiceInternal {

    private final DescItemRepository descItemRepository;

    private final InhibitedItemRepository inhibitedItemRepository;

    private final StaticDataService staticDataService;

    private final DataService dataService;

    @Autowired
    public DescriptionItemServiceInternal(DescItemRepository descItemRepository,
    									  InhibitedItemRepository inhibitedItemRepository,
                                          StaticDataService staticDataService,
                                          DataService dataService) {
        this.descItemRepository = descItemRepository;
        this.inhibitedItemRepository = inhibitedItemRepository;
        this.staticDataService = staticDataService;
        this.dataService = dataService;
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
    	Objects.requireNonNull(lockChange);
    	Objects.requireNonNull(node);
        List<ArrDescItem> itemList;
        itemList = dataService.findItemsWithData(() -> descItemRepository.findByNodeAndChange(node, lockChange),
                this::createDataResultList);
        return itemList;
    }

    /**
     * Return list of description items for node
     *
     * Description items are returned including data
     *
     * Method will return only valid / non deleted items.
     *
     * @param node
     * @return
     */
    public List<ArrDescItem> getDescItems(final ArrNode node) {
    	Objects.requireNonNull(node);
        List<ArrDescItem> itemList;
        itemList = dataService.findItemsWithData(() -> descItemRepository.findByNodeAndDeleteChangeIsNull(node),
                this::createDataResultList);
        return itemList;
    }

    public List<DataResult> createDataResultList(List<ArrDescItem> itemList) {
        return itemList.stream()
                .map(i -> new DataResult(i.getData().getDataId(), i.getItemType().getDataType()))
                .collect(Collectors.toList());
    }

    /**
     * Return list of inhibited descItem Ids by lockChange and fund
     *
     * @param lockChange
     * @param nodeIds
     * @return
     */
	public Set<Integer> getInhibitedDescItemIds(ArrChange lockChange, Collection<Integer> nodeIds) {
    	Objects.requireNonNull(lockChange);
    	Objects.requireNonNull(nodeIds);
    	return inhibitedItemRepository.findByNodeIdsAndLockChange(nodeIds, lockChange).stream()
    			.map(i -> i.getDescItemId())
    			.collect(Collectors.toSet());
	}

	public TitleValue createTitleValue(ArrDescItem descItem, Map<Integer, ApIndex> accessPointNames, final boolean dataExport) {
        // prepare item specification if present
        RulItemSpec itemSpec = null;
        if (descItem.getItemSpecId() != null) {
            StaticDataProvider staticData = staticDataService.getData();
            itemSpec = staticData.getItemSpecById(descItem.getItemSpecId());
        }
        // create new title value
        TitleValue titleValue = createTitleValueInternal(HibernateUtils.unproxy(descItem.getData()), itemSpec, accessPointNames, dataExport);
        // set common values
        titleValue.setPosition(descItem.getPosition());
        if (itemSpec != null) {
            String specCode = itemSpec.getCode();
            titleValue.setIconValue(specCode);
            titleValue.setSpecCode(specCode);
            titleValue.setSpecId(descItem.getItemSpecId());
            if (dataExport) {
                titleValue.setSpecName(itemSpec.getName());
            }
        }
        return titleValue;
    }

    private TitleValue createTitleValueInternal(ArrData data, RulItemSpec itemSpec, Map<Integer, ApIndex> accessPointNames, final boolean dataExport) {
        // handle undefined data
        if (data == null) {
            return new TitleValue(ArrangementService.UNDEFINED);
        }
        // resolve title value
        DataType dataType = DataType.fromId(data.getDataTypeId());
        switch (dataType) {
        case ENUM:
            return new TitleValue(itemSpec.getName());
        case RECORD_REF: {
            ArrDataRecordRef apData = (ArrDataRecordRef) data;
            ApIndex apIndex = accessPointNames.get(apData.getRecordId());
            String title = apIndex == null? "unknownName" : apIndex.getIndexValue();
            TitleValue value = new TitleValue(title);
            if (dataExport) {
                value.setEntityId(apData.getRecord().getAccessPointId());
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
            return new TitleValue(UnitDateConvertor.convertToString(unitdateData));
        case STRING:
            ArrDataString strData = (ArrDataString) data;
            return new TitleValue(strData.getStringValue());
        case TEXT:
        case FORMATTED_TEXT:
            ArrDataText textData = (ArrDataText) data;
            return new TitleValue(textData.getTextValue());
        case UNITID:
            ArrDataUnitid unitidData = (ArrDataUnitid) data;
            return new TitleValue(unitidData.getUnitId());
        case INT:
            ArrDataInteger intData = (ArrDataInteger) data;
            return new TitleValue(intData.getIntegerValue().toString());
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
            return new TitleValue(uriRef.getUriRefValue());
        case BIT:
            ArrDataBit bit = (ArrDataBit) data;
            return new TitleValue(Boolean.toString(bit.isBitValue()));
        default:
            throw new SystemException("Failed to create title, uknown data type: " + dataType, BaseCode.SYSTEM_ERROR);
        }
    }
}
