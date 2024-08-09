package cz.tacr.elza.controller.config;

import static cz.tacr.elza.repository.ExceptionThrow.institution;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cz.tacr.elza.FilterTools;
import cz.tacr.elza.bulkaction.generator.PersistentSortRunConfig;
import cz.tacr.elza.common.GeometryConvertor;
import cz.tacr.elza.controller.vo.ArrFundVO;
import cz.tacr.elza.controller.vo.DataBit;
import cz.tacr.elza.controller.vo.DataCoordinates;
import cz.tacr.elza.controller.vo.DataDate;
import cz.tacr.elza.controller.vo.DataDecimal;
import cz.tacr.elza.controller.vo.DataInteger;
import cz.tacr.elza.controller.vo.DataRecordRef;
import cz.tacr.elza.controller.vo.DataString;
import cz.tacr.elza.controller.vo.DataStructureRef;
import cz.tacr.elza.controller.vo.DataJsonTable;
import cz.tacr.elza.controller.vo.DataText;
import cz.tacr.elza.controller.vo.DataFileRef;
import cz.tacr.elza.controller.vo.DataFormattedText;
import cz.tacr.elza.controller.vo.DataUnitdate;
import cz.tacr.elza.controller.vo.DataUnitid;
import cz.tacr.elza.controller.vo.DataUriRef;
import cz.tacr.elza.controller.vo.ItemData;
import cz.tacr.elza.controller.vo.NodeItem;
import cz.tacr.elza.controller.vo.PersistentSortConfigVO;
import cz.tacr.elza.controller.vo.UISettingsVO;
import cz.tacr.elza.controller.vo.UpdateFund;
import cz.tacr.elza.controller.vo.UsrPermissionVO;
import cz.tacr.elza.controller.vo.filter.Condition;
import cz.tacr.elza.controller.vo.filter.Filter;
import cz.tacr.elza.controller.vo.filter.Filters;
import cz.tacr.elza.controller.vo.filter.ValuesTypes;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemVO;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataBit;
import cz.tacr.elza.domain.ArrDataCoordinates;
import cz.tacr.elza.domain.ArrDataDate;
import cz.tacr.elza.domain.ArrDataDecimal;
import cz.tacr.elza.domain.ArrDataFileRef;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataJsonTable;
import cz.tacr.elza.domain.ArrDataNull;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataStructureRef;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.ArrDataUnitid;
import cz.tacr.elza.domain.ArrDataUriRef;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFile;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrOutputItem;
import cz.tacr.elza.domain.ArrStructuredItem;
import cz.tacr.elza.domain.ArrStructuredObject;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.UISettings;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.table.ElzaTable;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.filter.DescItemTypeFilter;
import cz.tacr.elza.filter.condition.BeginDescItemCondition;
import cz.tacr.elza.filter.condition.ContainDescItemCondition;
import cz.tacr.elza.filter.condition.DescItemCondition;
import cz.tacr.elza.filter.condition.EndDescItemCondition;
import cz.tacr.elza.filter.condition.EqDescItemCondition;
import cz.tacr.elza.filter.condition.EqIntervalDescItemCondition;
import cz.tacr.elza.filter.condition.GeDescItemCondition;
import cz.tacr.elza.filter.condition.GtDescItemCondition;
import cz.tacr.elza.filter.condition.IntersectDescItemCondition;
import cz.tacr.elza.filter.condition.Interval;
import cz.tacr.elza.filter.condition.IntervalDescItemCondition;
import cz.tacr.elza.filter.condition.LeDescItemCondition;
import cz.tacr.elza.filter.condition.LtDescItemCondition;
import cz.tacr.elza.filter.condition.NeDescItemCondition;
import cz.tacr.elza.filter.condition.NoValuesCondition;
import cz.tacr.elza.filter.condition.NotContainDescItemCondition;
import cz.tacr.elza.filter.condition.NotEmptyDescItemCondition;
import cz.tacr.elza.filter.condition.NotIntervalDescItemCondition;
import cz.tacr.elza.filter.condition.SelectedSpecificationsDescItemEnumCondition;
import cz.tacr.elza.filter.condition.SelectedValuesDescItemEnumCondition;
import cz.tacr.elza.filter.condition.SelectsNothingCondition;
import cz.tacr.elza.filter.condition.SubsetDescItemCondition;
import cz.tacr.elza.filter.condition.UndefinedDescItemCondition;
import cz.tacr.elza.filter.condition.UnselectedSpecificationsDescItemEnumCondition;
import cz.tacr.elza.filter.condition.UnselectedValuesDescItemEnumCondition;
import cz.tacr.elza.repository.InstitutionRepository;
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import jakarta.persistence.EntityManager;

/**
 * Továrna na vytváření DO objektů z VO objektů.
 */
@Service
public class ClientFactoryDO {

    @Autowired
    private EntityManager em;

    @Autowired
    private ItemTypeRepository itemTypeRepository;

    @Autowired
    private ItemSpecRepository itemSpecRepository;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private StaticDataService staticDataService;

    /**
     * Vytvoří node z VO.
     *
     * @param nodeVO vo node
     * @return DO node
     */
    public ArrNode createNode(final ArrNodeVO nodeVO) {
        Assert.notNull(nodeVO, "JP musí být vyplněna");
        return nodeVO.createEntity();
    }

    /**
     * Vytvoří seznam DO z VO.
     *
     * @param nodeVoList VO seznam nodů
     * @return DO seznam nodů
     */
    public List<ArrNode> createNodes(final Collection<ArrNodeVO> nodeVoList) {
        Validate.notNull(nodeVoList, "Seznam JP musí být vyplněn");

        List<ArrNode> result = new ArrayList<>(nodeVoList.size());
        for (ArrNodeVO arrNodeVO : nodeVoList) {
            result.add(createNode(arrNodeVO));
        }

        return result;
    }

    /**
     * Vytvoření hodnoty atributu.
     *
     * @param descItemVO     VO hodnoty atributu
     * @param itemTypeId     identifkátor typu hodnoty atributu
     * @return
     */
    @Deprecated
    public ArrDescItem createDescItem(final ArrItemVO descItemVO, final Integer itemTypeId) {
        ArrData data = null;

        if (!descItemVO.isUndefined()) {
            data = descItemVO.createDataEntity(em);
        }
        ArrDescItem descItem = new ArrDescItem();
        descItem.setData(data);

        var sdp = staticDataService.getData();
        var itemType = sdp.getItemTypeById(itemTypeId);
        if (itemType == null) {
        	throw new BusinessException("Failed to get item type, itemTypeId: " + itemTypeId,
        			 BaseCode.ID_NOT_EXIST);
        }
        descItem.setItemType(itemType.getEntity());

        if (descItemVO.getDescItemSpecId() != null) {
            RulItemSpec itemSpec = itemType.getItemSpecById(descItemVO.getDescItemSpecId());
            if (itemSpec == null) {
            	throw new BusinessException("Failed to get item spec, itemTypeId: " + itemTypeId
            			+ ", itemSpecId: " + descItemVO.getDescItemSpecId(), BaseCode.ID_NOT_EXIST);
            }
            descItem.setItemSpec(itemSpec);
        }

        return descItem;
    }

   /**
     * Vytvoření hodnoty atributu (nová).
     *
     * @param nodeItem       hodnota atributu
     * @return
     */
    public ArrDescItem createDescItem(final NodeItem nodeItem) {
    	ArrDescItem descItem = new ArrDescItem();

        var sdp = staticDataService.getData();
        var itemType = sdp.getItemTypeById(nodeItem.getItemTypeId());
        if (itemType == null) {
        	throw new BusinessException("Failed to get item type, itemTypeId: " + nodeItem.getItemTypeId(), BaseCode.ID_NOT_EXIST);
        }
        descItem.setItemType(itemType.getEntity());

        if (nodeItem.getItemSpecId() != null) {
        	var itemSpecId = nodeItem.getItemSpecId();
            var itemSpec = itemType.getItemSpecById(itemSpecId);
            if (itemSpec == null) {
            	throw new BusinessException("Failed to get item spec, itemTypeId: " + nodeItem.getItemTypeId() + ", itemSpecId: " + itemSpecId, BaseCode.ID_NOT_EXIST);
            }
            descItem.setItemSpec(itemSpec);
        }

        // Item is not undefined -> parse data
        if (!Boolean.TRUE.equals(nodeItem.getUndefined())) {
        	ArrData data = createArrData(nodeItem.getData());
            descItem.setData(data);
        }

        // Copy other properties to application object
		descItem.setItemId(nodeItem.getId());
		descItem.setDescItemObjectId(nodeItem.getItemObjectId());
		descItem.setPosition(nodeItem.getPosition());
        descItem.setReadOnly(nodeItem.getReadOnly() != null ? nodeItem.getReadOnly() : false);

        return descItem;
    }

    /**
     * Vytvoření hodnoty ArrData.
     * 
     * @param data
     * @return
     */
    public ArrData createArrData(ItemData itemData) {
    	Integer value;
    	ArrData data = null;
    	DataType dataType = DataType.fromId(itemData.getDataTypeId());
        switch (dataType) {
	        case INT:
	            data = new ArrDataInteger();
	            ((ArrDataInteger) data).setIntegerValue(((DataInteger) itemData).getIntegerValue());
	        	break;
	        case STRING:
	            data = new ArrDataString();
	            ((ArrDataString) data).setStringValue(((DataString) itemData).getStringValue());
	        	break;
	        case TEXT:
	        	data = new ArrDataText();
	        	((ArrDataText) data).setTextValue(((DataText) itemData).getTextValue());
	        	break;
	        case UNITDATE:
	        	data = ArrDataUnitdate.valueOf(((DataUnitdate) itemData).getValue());
	        	break;
	        case UNITID:
	            data = new ArrDataUnitid();
	            ((ArrDataUnitid) data).setUnitId(((DataUnitid) itemData).getUnitId());
	            break;
	        case FORMATTED_TEXT:
	        	data = new ArrDataText();
	        	((ArrDataText) data).setTextValue(((DataFormattedText) itemData).getValue());
	        	break;
	        case COORDINATES:
	        	data = new ArrDataCoordinates();
	        	((ArrDataCoordinates) data).setValue(GeometryConvertor.convert(((DataCoordinates) itemData).getValue()));
	        	break;
	        case RECORD_REF:
	        	data = new ArrDataRecordRef();
	            ApAccessPoint record = null;
	            value = ((DataRecordRef) itemData).getValue();
	            if (value != null) {
	                record = em.getReference(ApAccessPoint.class, value);
	            }
	        	((ArrDataRecordRef) data).setRecord(record);
	        	break;
	        case DECIMAL:
	        	data = new ArrDataDecimal();
	        	((ArrDataDecimal) data).setValue(((DataDecimal) itemData).getValue());
	        	break;
	        case STRUCTURED:
	        	data = new ArrDataStructureRef();
	            ArrStructuredObject structObj = null;
	            value = ((DataStructureRef) itemData).getStructuredObjectId();
	            if (value != null) {
	                structObj = em.getReference(ArrStructuredObject.class, value);
	            }
	            ((ArrDataStructureRef) data).setStructuredObject(structObj);
	            break;
	        case ENUM:
	        	data = new ArrDataNull();
	        	break;
	        case FILE_REF:
	        	data = new ArrDataFileRef();
	            ArrFile file = null;
	            value = ((DataFileRef) itemData).getFileId();
	            if (value != null) {
	                file = em.getReference(ArrFile.class, value);
	            }
	            ((ArrDataFileRef) data).setFile(file);
	        	break;
	        case JSON_TABLE:
	        	data = new ArrDataJsonTable();
	        	((ArrDataJsonTable) data).setValue(ElzaTable.fromJsonString(((DataJsonTable) itemData).getValue()));
	        	break;
	        case DATE:
	        	data = new ArrDataDate();
	        	((ArrDataDate) data).setValue(((DataDate) itemData).getValue());
	        	break;
	        case URI_REF:
	        	data = new ArrDataUriRef();
	        	String stringValue = ((DataUriRef) itemData).getValue();
	        	((ArrDataUriRef) data).setUriRefValue(stringValue);
	        	((ArrDataUriRef) data).setSchema(ArrDataUriRef.createSchema(stringValue));
	        	((ArrDataUriRef) data).setDescription(((DataUriRef) itemData).getDescription());
	        	break;
	        case BIT:
	        	data = new ArrDataBit();
	        	((ArrDataBit) data).setBitValue(((DataBit) itemData).getBitValue());
	        	break;
	        default:
                throw new IllegalStateException("Neimplementovaný datový typ atributu -> CODE: " + dataType.getCode());
        }

        data.setDataType(dataType.getEntity());
    	return data;
    }

    public ArrStructuredItem createStructureItem(final ArrItemVO itemVO, final Integer itemTypeId) {
        ArrData data = itemVO.createDataEntity(em);
        ArrStructuredItem structureItem = new ArrStructuredItem();
        structureItem.setData(data);

        var sdp = staticDataService.getData();
        var itemType = sdp.getItemTypeById(itemTypeId);
        if(itemType==null) {
        	throw new BusinessException("Failed to get item type, itemTypeId: " + itemTypeId,
        			 BaseCode.ID_NOT_EXIST);
        }
        structureItem.setItemType(itemType.getEntity());

        if (itemVO.getDescItemSpecId() != null) {
        	var itemSpec = itemType.getItemSpecById(itemVO.getDescItemSpecId());
        	if(itemSpec==null) {
            	throw new BusinessException("Failed to get item specification, itemTypeId: " + itemTypeId
            			+", itemSpecId: " + itemVO.getDescItemSpecId(),
           			 BaseCode.ID_NOT_EXIST);        		
        	}
            structureItem.setItemSpec(itemSpec);
        }

        return structureItem;
    }

    public ArrStructuredItem createStructureItem(final ArrItemVO descItemVO) {        
        ArrStructuredItem structureItem = new ArrStructuredItem();        
        structureItem.setItemId(descItemVO.getId());
        structureItem.setDescItemObjectId(descItemVO.getDescItemObjectId());
        structureItem.setPosition(descItemVO.getPosition());

        // item type
        var sdp = staticDataService.getData();
        var itemType = sdp.getItemTypeById(descItemVO.getItemTypeId());
        if(itemType==null) {
        	throw new BusinessException("Failed to get item type, itemTypeId: "+descItemVO.getItemTypeId(), 
        			BaseCode.ID_NOT_EXIST);
        }
        structureItem.setItemType(itemType.getEntity());

        // specification
        if (descItemVO.getDescItemSpecId() != null) {
            RulItemSpec itemSpec = itemType.getItemSpecById(descItemVO.getDescItemSpecId());
            if(itemSpec==null) {
            	throw new BusinessException("Failed to get item spec, itemTypeId: " + descItemVO.getItemTypeId()
            			+", itemSpecId: " + descItemVO.getDescItemSpecId(), BaseCode.ID_NOT_EXIST);
            }
            structureItem.setItemSpec(itemSpec);
        }
        
        ArrData data = descItemVO.createDataEntity(em);
        structureItem.setData(data);

        return structureItem;
    }

    public List<ArrStructuredItem> createStructureItem(final Map<Integer, List<ArrItemVO>> descItemVO) {
        List<ArrStructuredItem> result = new ArrayList<>();
        for (Map.Entry<Integer, List<ArrItemVO>> entry : descItemVO.entrySet()) {
            result.addAll(entry.getValue().stream()
                    .map(si -> createStructureItem(si, entry.getKey()))
                    .collect(Collectors.toList()));
        }
        return result;
    }

    @Deprecated
    public ArrDescItem createDescItem(final StaticDataProvider sdp, final ArrItemVO itemVO) {

    	var itemType = sdp.getItemTypeById(itemVO.getItemTypeId());
    	if (itemType == null) {
    		throw new BusinessException("Cannot find item type, itemTypeId: " + itemVO.getItemTypeId(), BaseCode.ID_NOT_EXIST)
    			.set("itemTypeId", itemVO.getItemTypeId());
    	}

        ArrDescItem descItem = new ArrDescItem();
        descItem.setItemType(itemType.getEntity());
        
        if (itemVO.getDescItemSpecId() != null) {
            RulItemSpec descItemSpec = itemType.getItemSpecById(itemVO.getDescItemSpecId());
            if (descItemSpec == null) {
        		throw new BusinessException("Cannot find item spec, itemTypeId: " + itemVO.getItemTypeId()
        		+ ", itemSpecId: " + itemVO.getDescItemSpecId(), BaseCode.ID_NOT_EXIST)
    				.set("itemTypeId", itemVO.getItemTypeId())
    				.set("itemSpecId", itemVO.getDescItemSpecId());
            }
            descItem.setItemSpec(descItemSpec);
        }        

        // Item is not undefined -> parse data
        if (itemVO.getUndefined() != Boolean.TRUE) {            
            ArrData data = itemVO.createDataEntity(em);
            descItem.setData(data);
        } 
        // Copy properties to application object
        itemVO.fill(descItem);

        return descItem;
    }

    /**
     * Vytvoří DO archivní pomůcky
     *
     * @param fundVO VO archivní pomůcka
     * @return DO
     */
    public ArrFund createFund(final ArrFundVO fundVO) {
        Assert.notNull(fundVO, "AS musí být vyplněno");
        ArrFund fund = fundVO.createEntity();
        ParInstitution institution = institutionRepository.findById(fundVO.getInstitutionId())
                .orElseThrow(institution(fundVO.getInstitutionId()));
        fund.setInstitution(institution);
        return fund;
    }

    public ArrFund createFund(final UpdateFund fund, ParInstitution institution, String id) {
        Assert.notNull(fund, "AS musí být vyplněno");
        ArrFund arrFund = new ArrFund();
        arrFund.setFundId(Integer.valueOf(id));
        arrFund.setUnitdate(fund.getUnitdate());
        arrFund.setName(fund.getName());
        arrFund.setFundNumber(fund.getFundNumber());
        arrFund.setInternalCode(fund.getInternalCode());
        arrFund.setMark(fund.getMark());
        arrFund.setInstitution(institution);
        return arrFund;
    }

    public List<DescItemTypeFilter> createFilters(final Filters filters, final Integer lockChangeId) {
        if (filters == null || filters.getFilters() == null || filters.getFilters().isEmpty()) {
            return null;
        }

        Map<Integer, Filter> filtersMap = filters.getFilters();
        Set<Integer> descItemTypeIds = filtersMap.keySet();
        List<RulItemType> descItemTypes = itemTypeRepository.findAllById(descItemTypeIds);

        List<DescItemTypeFilter> descItemTypeFilters = new ArrayList<>(descItemTypes.size());

        descItemTypes.forEach(type -> {
            Filter filter = filtersMap.get(type.getItemTypeId());
            if (filter != null) {
                DescItemTypeFilter descItemTypeFilter = createDescItemFilter(type, filter, lockChangeId);
                if (descItemTypeFilter != null) {
                    descItemTypeFilters.add(descItemTypeFilter);
                }
            }
        });

        return descItemTypeFilters;
    }

    /**
     * Převede VO filter na filtr se kterým pracuje BL.
     *
     * @param descItemType typ atributu
     * @param filter       VO filtr
     * @param lockChangeId
     * @return filtr pro daný typ atributu
     */
    private DescItemTypeFilter createDescItemFilter(final RulItemType descItemType, final Filter filter, final Integer lockChangeId) {
        Assert.notNull(descItemType, "Typ atributu musí být vyplněn");
        Assert.notNull(filter, "Filter musí být vyplněn");

        List<DescItemCondition> valuesConditions = createValuesEnumCondition(filter.getValuesType(), filter.getValues(),
                ArrDescItem.FULLTEXT_ATT);
        List<DescItemCondition> specsConditions = createSpecificationsEnumCondition(filter.getSpecsType(), filter.getSpecs(),
                ArrDescItem.SPECIFICATION_ATT);
        List<Integer> itemSpecIds = Objects.equals(filter.getSpecsType(), ValuesTypes.UNSELECTED) ? null : filter.getSpecs();

        List<DescItemCondition> conditions = new LinkedList<>();
        Condition conditionType = filter.getConditionType();
        if (conditionType != null && conditionType != Condition.NONE) {
            RulDataType rulDataType = descItemType.getDataType();
            conditionType.checkSupport(rulDataType.getCode());
            DataType dataType = DataType.fromId(rulDataType.getDataTypeId());

            DescItemCondition condition;
            switch (conditionType) {
                case BEGIN: {
                    String conditionValue = getConditionValueString(filter.getCondition());
                    condition = new BeginDescItemCondition<>(conditionValue, ArrDescItem.FULLTEXT_ATT);
                    break;
                }
                case CONTAIN: {
                    String conditionValue = getConditionValueString(filter.getCondition());
                    condition = new ContainDescItemCondition<>(conditionValue, ArrDescItem.FULLTEXT_ATT);
                    break;
                }
                case EMPTY: {
                    condition = new NoValuesCondition();
                    break;
                }
                case END: {
                    String conditionValue = getConditionValueString(filter.getCondition());
                    condition = new EndDescItemCondition<>(conditionValue, ArrDescItem.FULLTEXT_ATT);
                    break;
                }
                case EQ: {
                    if (dataType == DataType.INT) {
                        Integer conditionValue = getConditionValueInteger(filter.getCondition());
                        String attributeName = ArrDescItem.INTGER_ATT;
                        condition = new EqDescItemCondition<>(conditionValue, attributeName);
                    } else if (dataType == DataType.DECIMAL) {
                        Double conditionValue = getConditionValueDouble(filter.getCondition());
                        String attributeName = ArrDescItem.DECIMAL_ATT;
                        condition = new EqDescItemCondition<>(conditionValue, attributeName);
                    } else if (dataType == DataType.UNITDATE) {
                        Interval<Long> conditionValue = getConditionValueIntervalLong(filter.getCondition());
                        condition = new EqIntervalDescItemCondition<>(conditionValue,
                                ArrDescItem.NORMALIZED_FROM_ATT,
                                ArrDescItem.NORMALIZED_TO_ATT);
                    } else if (dataType == DataType.DATE) {
                        Date conditionValue = getConditionValueDate(filter.getCondition());
                        condition = new EqDescItemCondition<>(conditionValue, ArrDescItem.DATE_ATT);
                    } else {
                        String conditionValue = getConditionValueString(filter.getCondition());
                        condition = new EqDescItemCondition<>(conditionValue, ArrDescItem.FULLTEXT_ATT);
                    }
                    break;
                }
                case GE: {
                    if (dataType == DataType.INT) {
                        Integer conditionValue = getConditionValueInteger(filter.getCondition());
                        String attributeName = ArrDescItem.INTGER_ATT;
                        condition = new GeDescItemCondition<>(conditionValue, attributeName);
                    } else if (dataType == DataType.DECIMAL) {
                        Double conditionValue = getConditionValueDouble(filter.getCondition());
                        String attributeName = ArrDescItem.DECIMAL_ATT;
                        condition = new GeDescItemCondition<>(conditionValue, attributeName);
                    } else if (dataType == DataType.DATE) {
                        Date conditionValue = getConditionValueDate(filter.getCondition());
                        String attributeName = ArrDescItem.DATE_ATT;
                        condition = new GeDescItemCondition<>(conditionValue, attributeName);
                    } else {
                        throw new NotImplementedException("Neimplementovaný typ: " + dataType.getCode());
                    }
                    break;
                }
                case GT: {
                    if (dataType == DataType.UNITDATE) {
                        ArrDataUnitdate unitDate = getConditionValueUnitdate(filter.getCondition());
                        String attributeName = ArrDescItem.NORMALIZED_TO_ATT;
                        condition = new GtDescItemCondition<>(unitDate.getNormalizedFrom(), attributeName);
                    } else if (dataType == DataType.INT) {
                        Integer conditionValue = getConditionValueInteger(filter.getCondition());
                        String attributeName = ArrDescItem.INTGER_ATT;
                        condition = new GtDescItemCondition<>(conditionValue, attributeName);
                    } else if (dataType == DataType.DECIMAL) {
                        Double conditionValue = getConditionValueDouble(filter.getCondition());
                        String attributeName = ArrDescItem.DECIMAL_ATT;
                        condition = new GtDescItemCondition<>(conditionValue, attributeName);
                    } else if (dataType == DataType.DATE) {
                        Date conditionValue = getConditionValueDate(filter.getCondition());
                        String attributeName = ArrDescItem.DATE_ATT;
                        condition = new GtDescItemCondition<>(conditionValue, attributeName);
                    } else {
                        throw new NotImplementedException("Neimplementovaný typ: " + dataType.getCode());
                    }
                    break;
                }
                case INTERVAL: {
                    if (dataType == DataType.INT) {
                        Interval<Integer> conditionValue = getConditionValueIntervalInteger(filter.getCondition());
                        String attributeName = ArrDescItem.INTGER_ATT;
                        condition = new IntervalDescItemCondition<>(conditionValue, attributeName);
                    } else if (dataType == DataType.DECIMAL) {
                        Interval<Double> conditionValue = getConditionValueIntervalDouble(filter.getCondition());
                        String attributeName = ArrDescItem.DECIMAL_ATT;
                        condition = new IntervalDescItemCondition<>(conditionValue, attributeName);
                    } else if (dataType == DataType.DATE) {
                        Interval<Date> conditionValue = getConditionValueIntervalDate(filter.getCondition());
                        String attributeName = ArrDescItem.DATE_ATT;
                        condition = new IntervalDescItemCondition<>(conditionValue, attributeName);
                    } else {
                        throw new NotImplementedException("Neimplementovaný typ: " + dataType.getCode());
                    }
                    break;
                }
                case LE: {
                    if (dataType == DataType.INT) {
                        Integer conditionValue = getConditionValueInteger(filter.getCondition());
                        String attributeName = ArrDescItem.INTGER_ATT;
                        condition = new LeDescItemCondition<>(conditionValue, attributeName);
                    } else if (dataType == DataType.DECIMAL) {
                        Double conditionValue = getConditionValueDouble(filter.getCondition());
                        String attributeName = ArrDescItem.DECIMAL_ATT;
                        condition = new LeDescItemCondition<>(conditionValue, attributeName);
                    } else if (dataType == DataType.DATE) {
                        Date conditionValue = getConditionValueDate(filter.getCondition());
                        String attributeName = ArrDescItem.DATE_ATT;
                        condition = new LeDescItemCondition<>(conditionValue, attributeName);
                    } else {
                        throw new NotImplementedException("Neimplementovaný typ: " + dataType.getCode());
                    }
                    break;
                }
                case LT: {
                    if (dataType == DataType.UNITDATE) {
                        ArrDataUnitdate unitDate = getConditionValueUnitdate(filter.getCondition());
                        String attributeName = ArrDescItem.NORMALIZED_FROM_ATT;
                        condition = new LtDescItemCondition<>(unitDate.getNormalizedTo(), attributeName);
                    } else if (dataType == DataType.INT) {
                        Integer conditionValue = getConditionValueInteger(filter.getCondition());
                        String attributeName = ArrDescItem.INTGER_ATT;
                        condition = new LtDescItemCondition<>(conditionValue, attributeName);
                    } else if (dataType == DataType.DECIMAL) {
                        Double conditionValue = getConditionValueDouble(filter.getCondition());
                        String attributeName = ArrDescItem.DECIMAL_ATT;
                        condition = new LtDescItemCondition<>(conditionValue, attributeName);
                    } else if (dataType == DataType.DATE) {
                        Date conditionValue = getConditionValueDate(filter.getCondition());
                        String attributeName = ArrDescItem.DATE_ATT;
                        condition = new LtDescItemCondition<>(conditionValue, attributeName);
                    } else {
                        throw new NotImplementedException("Neimplementovaný typ: " + dataType.getCode());
                    }
                    break;
                }
                case NE: {
                    if (dataType == DataType.INT) {
                        Integer conditionValue = getConditionValueInteger(filter.getCondition());
                        String attributeName = ArrDescItem.INTGER_ATT;
                        condition = new NeDescItemCondition<>(conditionValue, attributeName);
                    } else if (dataType == DataType.DECIMAL) {
                        Double conditionValue = getConditionValueDouble(filter.getCondition());
                        String attributeName = ArrDescItem.DECIMAL_ATT;
                        condition = new NeDescItemCondition<>(conditionValue, attributeName);
                    } else if (dataType == DataType.DATE) {
                        Date conditionValue = getConditionValueDate(filter.getCondition());
                        String attributeName = ArrDescItem.DATE_ATT;
                        condition = new NeDescItemCondition<>(conditionValue, attributeName);
                    } else {
                        throw new NotImplementedException("Neimplementovaný typ: " + dataType.getCode());
                    }
                    break;
                }
                case NOT_CONTAIN: {
                    String conditionValue = getConditionValueString(filter.getCondition());
                    condition = new NotContainDescItemCondition<>(conditionValue, ArrDescItem.FULLTEXT_ATT);
                    break;
                }
                case NOT_EMPTY:
                    condition = new NotEmptyDescItemCondition(); // fulltextValue
                    break;
                case UNDEFINED:
                    condition = new UndefinedDescItemCondition();
                    break;
                case NOT_INTERVAL: {
                    if (dataType == DataType.INT) {
                        Interval<Integer> conditionValue = getConditionValueIntervalInteger(filter.getCondition());
                        String attributeName = ArrDescItem.INTGER_ATT;
                        condition = new NotIntervalDescItemCondition<>(conditionValue, attributeName);
                    } else if (dataType == DataType.DECIMAL) {
                        Interval<Double> conditionValue = getConditionValueIntervalDouble(filter.getCondition());
                        String attributeName = ArrDescItem.DECIMAL_ATT;
                        condition = new NotIntervalDescItemCondition<>(conditionValue, attributeName);
                    } else if (dataType == DataType.DATE) {
                        Interval<Date> conditionValue = getConditionValueIntervalDate(filter.getCondition());
                        String attributeName = ArrDescItem.DECIMAL_ATT;
                        condition = new NotIntervalDescItemCondition<>(conditionValue, attributeName);
                    } else {
                        throw new NotImplementedException("Neimplementovaný typ: " + dataType.getCode());
                    }
                    break;
                }
                case INTERSECT: {
                    Interval<Long> conditionValue = getConditionValueIntervalLong(filter.getCondition());
                    condition = new IntersectDescItemCondition<>(conditionValue,
                            ArrDescItem.NORMALIZED_FROM_ATT,
                            ArrDescItem.NORMALIZED_TO_ATT);
                    break;
                }
                case SUBSET: {
                    Interval<Long> conditionValue = getConditionValueIntervalLong(filter.getCondition());
                    condition = new SubsetDescItemCondition<>(conditionValue,
                            ArrDescItem.NORMALIZED_FROM_ATT,
                            ArrDescItem.NORMALIZED_TO_ATT);
                    break;
                }
                default:
                    throw new IllegalArgumentException("Neznámý typ podmínky " + conditionType);
            }

            conditions.add(condition);
        }

        if (!valuesConditions.isEmpty() || !specsConditions.isEmpty() || !conditions.isEmpty()) {
            return new DescItemTypeFilter(descItemType, itemSpecIds, valuesConditions, specsConditions, conditions, lockChangeId);
        }

        return null;
    }

    private <T> T getSingleValue(final List<String> conditions, Function<String, T> convertor) {
        if (CollectionUtils.isEmpty(conditions)) {
            throw new BusinessException("Není předána hodnota podmínky.", BaseCode.PROPERTY_IS_INVALID)
            	.set("property", "conditions");
        }
        if (conditions.size() > 1) {
            throw new BusinessException("Musí existovat pouze jedna podmínka.", BaseCode.PROPERTY_IS_INVALID)
            .set("property", "conditions")
            .set("size", conditions.size());
        }
        String value = conditions.iterator().next();
        if(StringUtils.isBlank(value)) {
            throw new BusinessException("Hodnota podmínky je prázdná.", BaseCode.PROPERTY_IS_INVALID)
        		.set("property", "conditions");
        }
        return convertor.apply(value);
    }

    private <T> Interval<T> getConditionValueInterval(final List<String> conditions,
    		final Function<String, T> convertor) {
        if (CollectionUtils.isEmpty(conditions)) {
            throw new BusinessException("Není předána hodnota podmínky.", BaseCode.PROPERTY_IS_INVALID).set("property", "conditions");
        }

        Iterator<String> iterator = conditions.iterator();

        String fromString = iterator.next();
        if (StringUtils.isBlank(fromString)) {
            throw new BusinessException("Není předána první hodnota intervalu.", BaseCode.PROPERTY_IS_INVALID).set("property", "toString");
        }
        T from = convertor.apply(fromString);

        String toString = iterator.next();
        if (StringUtils.isBlank(toString)) {
            throw new BusinessException("Není předána druhá hodnota intervalu.", BaseCode.PROPERTY_IS_INVALID).set("property", "toString");
        }

        T to = convertor.apply(toString);

        return new Interval<>(from, to);
    }

    /**
     * Vrací hodnotu jako řetězec pro filtr.
     * Pro vyhledávání bez ohledu na velikost písmen, text musí být napsán malými písmeny.
     *
     * @param conditions
     * @return String
     */
    private String getConditionValueString(final List<String> conditions) {
        return getSingleValue(conditions, Function.identity()).toLowerCase();
    }

    private Double getConditionValueDouble(final List<String> conditions) {
        return getSingleValue(conditions, value -> Double.valueOf(value.replace(',', '.')));
    }

    private static Date valueOfDate(String value) {
    	return Date.from(LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private Date getConditionValueDate(final List<String> conditions) {
        return getSingleValue(conditions, ClientFactoryDO::valueOfDate);
    }

    private Interval<Date> getConditionValueIntervalDate(final List<String> conditions) {
        return getConditionValueInterval(conditions, ClientFactoryDO::valueOfDate);
    }

    private Interval<Double> getConditionValueIntervalDouble(final List<String> conditions) {
        return getConditionValueInterval(conditions, value -> Double.valueOf(value.replace(',', '.')));
    }

    private Integer getConditionValueInteger(final List<String> conditions) {
        return getSingleValue(conditions, Integer::valueOf);
    }

    private Interval<Integer> getConditionValueIntervalInteger(final List<String> conditions) {
        return getConditionValueInterval(conditions, Integer::valueOf);
    }

    private ArrDataUnitdate getConditionValueUnitdate(final List<String> conditions) {
        if (CollectionUtils.isEmpty(conditions)) {
            throw new BusinessException("Není předána hodnota podmínky.", BaseCode.PROPERTY_IS_INVALID)
            	.set("property", "conditions");
        }
        String firstValue = conditions.iterator().next();
        if(StringUtils.isBlank(firstValue)) {
            throw new BusinessException("Hodnota podmínky je prázdná.", BaseCode.PROPERTY_IS_INVALID)
        	.set("property", "conditions");
        }
        ArrDataUnitdate result = ArrDataUnitdate.valueOf(firstValue);
        switch(conditions.size()) {
        case 1:
        	break;
        case 2:
        	String secondValue = conditions.get(1);
        	if(!StringUtils.isBlank(secondValue)) {
        		ArrDataUnitdate to = ArrDataUnitdate.valueOf(secondValue);
        		result.setNormalizedTo(to.getNormalizedTo());
        		result.setValueTo(to.getValueTo());
        		result.setValueToEstimated(to.getValueToEstimated());
        	}
        	break;
        default:
            throw new BusinessException("Není předána hodnota podmínky.", BaseCode.PROPERTY_IS_INVALID)
        			.set("property", "conditions")
        			.set("coount", conditions.size());
        }
        return result;
    }

    private Interval<Long> getConditionValueIntervalLong(final List<String> conditions) {
        ArrDataUnitdate unitdate = getConditionValueUnitdate(conditions);

        return new Interval<>(unitdate.getNormalizedFrom(), unitdate.getNormalizedTo());
    }

    /**
     * Vytvoří podmínku pro odškrtlé/zaškrtlé položky hodnot.
     *
     * @param valuesTypes typ výběru - zaškrtnutí/odškrtnutí
     * @param values      hodnoty
     * @param attName     název atributu na který se podmínka aplikuje
     * @return seznam podmínek
     */
    private List<DescItemCondition> createValuesEnumCondition(final ValuesTypes valuesTypes, final List<String> values,
                                                              final String attName) {
        if (valuesTypes == null && values == null) {
            return Collections.emptyList();
        }
        Assert.notNull(valuesTypes, "Typ vybraných hodnot musí být vyplněno");
        Assert.notNull(values, "Hodnoty musí být vyplněny");

        boolean noValues = CollectionUtils.isEmpty(values);
        boolean containsNull = FilterTools.removeNullValues(values);

        List<DescItemCondition> conditions = new LinkedList<>();
        if (valuesTypes == ValuesTypes.SELECTED) {
            if (noValues) { // nehledat nic
                conditions.add(new SelectsNothingCondition());
            } else if (containsNull && !values.isEmpty()) { // vybrané hodnoty i "Prázdné"
                conditions.add(new SelectedValuesDescItemEnumCondition(values, attName));
                conditions.add(new NoValuesCondition());
            } else if (!values.isEmpty()) { // vybrané jen hodnoty
                conditions.add(new SelectedValuesDescItemEnumCondition(values, attName));
            } else { // vybrané jen "Prázdné"
                conditions.add(new NoValuesCondition());
            }
        } else {
            if (containsNull && !values.isEmpty()) { // odškrtlé hodnoty i "Prázdné" = hodnoty které neobsahují proškrtlé položky
                conditions.add(new UnselectedValuesDescItemEnumCondition(values, attName));
            } else if (!values.isEmpty()) { // odškrtlé jen hodnoty = hodnoty které neobsahují proškrtlé položky + nody bez hodnot
                conditions.add(new UnselectedValuesDescItemEnumCondition(values, attName));
                conditions.add(new NoValuesCondition());
            } else if (containsNull) { // odškrtlé jen "Prázdné" = vše s hodnotou
                conditions.add(new NotEmptyDescItemCondition());
            } else {
                // není potřeba vkládat podmínku, pokud vznikne ještě jiná podmínka tak by se udělal průnik výsledků a když bude seznam podmínek prázdný tak se vrátí všechna data
            }
        }

        return conditions;
    }

    /**
     * Vytvoří podmínku pro odškrtlé/zaškrtlé položky specifikací.
     *
     * @param valuesTypes typ výběru - zaškrtnutí/odškrtnutí
     * @param values      id specifikací
     * @param attName     název atributu na který se podmínka aplikuje
     */
    private List<DescItemCondition> createSpecificationsEnumCondition(final ValuesTypes valuesTypes, final List<Integer> values,
                                                                      final String attName) {
        if (valuesTypes == null && values == null) {
            return Collections.emptyList();
        }
        Assert.notNull(valuesTypes, "Typ vybraných hodnot musí být vyplněno");
        Assert.notNull(values, "Hodnoty musí být vyplněny");

        boolean noValues = CollectionUtils.isEmpty(values);
        boolean containsNull = FilterTools.removeNullValues(values);

        List<DescItemCondition> conditions = new LinkedList<>();
        if (valuesTypes == ValuesTypes.SELECTED) {
            if (noValues) { // nehledat nic
                conditions.add(new SelectsNothingCondition());
            } else if (containsNull && !values.isEmpty()) { // vybrané hodnoty i "Prázdné"
                conditions.add(new SelectedSpecificationsDescItemEnumCondition(values, attName));
                conditions.add(new NoValuesCondition());
            } else if (!values.isEmpty()) { // vybrané jen hodnoty
                conditions.add(new SelectedSpecificationsDescItemEnumCondition(values, attName));
            } else { // vybrané jen "Prázdné"
                conditions.add(new NoValuesCondition());
            }
        } else {
            if (containsNull && !values.isEmpty()) { // odškrtlé hodnoty i "Prázdné" = hodnoty které neobsahují proškrtlé položky
                conditions.add(new UnselectedSpecificationsDescItemEnumCondition(values, attName));
            } else if (!values.isEmpty()) { // odškrtlé jen hodnoty = hodnoty které neobsahují proškrtlé položky + nody bez hodnot
                conditions.add(new UnselectedSpecificationsDescItemEnumCondition(values, attName));
                conditions.add(new NoValuesCondition());
            } else if (containsNull) { // odškrtlé jen "Prázdné" = vše s hodnotou
                conditions.add(new NotEmptyDescItemCondition());
            } else {
                // není potřeba vkládat podmínku, pokud vznikne ještě jiná podmínka tak by se udělal průnik výsledků a když bude seznam podmínek prázdný tak se vrátí všechna data
            }
        }

        return conditions;
    }

    public ArrOutputItem createOutputItem(final ArrItemVO outputItemVO, final Integer itemTypeId) {

        ArrOutputItem outputItem = new ArrOutputItem();

        var sdp = staticDataService.getData();
        var itemType = sdp.getItemTypeById(itemTypeId);
        if(itemType==null) {
        	throw new BusinessException("Failed to get item type, itemTypeId: " + itemTypeId,
        			 BaseCode.ID_NOT_EXIST);
        }
        outputItem.setItemType(itemType.getEntity());

        if (outputItemVO.getDescItemSpecId() != null) {
            RulItemSpec descItemSpec = itemType.getItemSpecById(outputItemVO.getDescItemSpecId());
            outputItem.setItemSpec(descItemSpec);
        }

        ArrData data = outputItemVO.createDataEntity(em);
        outputItem.setData(data);
        return outputItem;
    }

    public ArrOutputItem createOutputItem(final ArrItemVO itemVO) {
        ArrOutputItem outputItem = new ArrOutputItem();
        
        outputItem.setItemId(itemVO.getId());
        outputItem.setDescItemObjectId(itemVO.getDescItemObjectId());
        outputItem.setPosition(itemVO.getPosition());
        
        var sdp = staticDataService.getData();
        var itemType = sdp.getItemTypeById(itemVO.getItemTypeId());
        if(itemType==null) {
        	throw new BusinessException("Failed to get item type, itemTypeId: " + itemVO.getItemTypeId(),
        			 BaseCode.ID_NOT_EXIST);
        }
        outputItem.setItemType(itemType.getEntity());
        

        if (itemVO.getDescItemSpecId() != null) {
            RulItemSpec descItemSpec = itemType.getItemSpecById(itemVO.getDescItemSpecId());
            if (descItemSpec == null) {
                throw new SystemException("Specifikace s ID=" + itemVO.getDescItemSpecId() + " neexistuje", BaseCode.ID_NOT_EXIST);
            }
            outputItem.setItemSpec(descItemSpec);
        }

        ArrData data = itemVO.createDataEntity(em);
        outputItem.setData(data);
        return outputItem;
    }

    /**
     * Převod seznamu oprávnávnění VO na DO.
     *
     * @param permissions seznam oprávnění
     * @return seznam DO
     */
    public List<UsrPermission> createPermissionList(final List<UsrPermissionVO> permissions) {

        StaticDataProvider staticData = staticDataService.getData();
        List<UsrPermission> result = permissions.stream().map(
                                                              pvo -> pvo.createEntity(staticData))
                .collect(Collectors.toList());
        return result;
    }

    /**
     * Převod seznamu nastavení VO na DO.
     *
     * @param settings
     *            seznam nastavení
     * @return seznam DO
     */
    public List<UISettings> createSettingsList(final List<UISettingsVO> settings) {
        if (CollectionUtils.isEmpty(settings)) {
            return Collections.emptyList();
        }
        return settings.stream().map(UISettingsVO::createEntity).collect(Collectors.toList());
    }

    public PersistentSortRunConfig createPersistentSortRunConfig(final PersistentSortConfigVO configVO) {
        Assert.notNull(configVO, "Nastavení musí být vyplněno");
        return configVO.createEntity();
    }
}
