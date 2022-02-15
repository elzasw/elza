package cz.tacr.elza.service.cam;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import cz.tacr.elza.api.ApExternalSystemType;
import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.service.AccessPointDataService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.Validate;import org.drools.core.common.InstanceEqualsConstraint.InstanceEqualsConstraintContextEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import cz.tacr.cam.schema.cam.ItemsXml;
import cz.tacr.cam.schema.cam.NewItemsXml;
import cz.tacr.cam.schema.cam.ObjectFactory;
import cz.tacr.cam.schema.cam.PartTypeXml;
import cz.tacr.cam.schema.cam.PartXml;
import cz.tacr.cam.schema.cam.PartsXml;
import cz.tacr.cam.schema.cam.StringXml;
import cz.tacr.cam.schema.cam.UuidXml;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.output.writer.cam.CamUtils;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBindingItem;
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
import cz.tacr.elza.service.GroovyService;

/**
 * Builder for CAM XML
 *
 * This builder will create XML for one binding
 */
abstract public class CamXmlBuilder {

    private static final Logger log = LoggerFactory.getLogger(CamXmlBuilder.class);
    
    final protected static ObjectFactory objectFactory = CamUtils.getObjectFactory();

    protected final StaticDataProvider sdp;
    protected final ApAccessPoint accessPoint;
    protected final ApScope scope;

    protected final GroovyService groovyService;
    protected final AccessPointDataService apDataService;

    /**
     * Collection of all part UUIDS
     */
    protected final Map<Integer, String> partUuidMap = new HashMap<>();

    /**
     * Map of new UUIDS for items
     */
    private Map<Integer, String> itemUuids = new HashMap<>();

    /**
     * Map of new UUIDS for parts
     */
    private Map<Integer, String> partUuids = new HashMap<>();    

    public Map<Integer, String> getItemUuids() {
        return itemUuids;
    }

    public Map<Integer, String> getPartUuids() {
        return partUuids;
    }

	CamXmlBuilder(final StaticDataProvider sdp,
                  final ApAccessPoint accessPoint,
                  final GroovyService groovyService,
                  final AccessPointDataService apDataService,
                  final ApScope scope) {
        this.sdp = sdp;
        this.accessPoint = accessPoint;
        this.groovyService = groovyService;
        this.apDataService = apDataService;
        this.scope = scope;
    }
	
    protected NewItemsXml createNewItems(ApBindingItem changedPart, Collection<ApItem> itemList) {
        NewItemsXml newItems = new NewItemsXml();
        newItems.setPid(new UuidXml(changedPart.getValue()));
        newItems.setT(PartTypeXml.fromValue(changedPart.getPart().getPartType().getCode()));

        createXmlItems(itemList, newItems.getItems());
        if (newItems.getItems().size() == 0) {
            return null;
        }

        return newItems;
    }

    protected PartsXml createParts(Collection<ApPart> partList,
                                   Map<Integer, List<ApItem>> itemMap) {
        // if no parts available -> return null
        if (CollectionUtils.isEmpty(partList)) {
            // schema allows empty element prts
            return null;
        }

        Collection<ApPart> adjustedPartList = partList;
        // check if prefered part is first
        ApPart preferedPart = accessPoint.getPreferredPart();
        if (preferedPart != null) {
            // prepare list with first pref.part
            adjustedPartList = new ArrayList<>(partList.size());
            adjustedPartList.add(preferedPart);

            ArrayList<ApPart> subparts = new ArrayList<>();
            for (ApPart part : partList) {
                if (!part.getPartId().equals(preferedPart.getPartId())) {
                    // check if subpart
                    if (part.getParentPartId() != null) {
                        subparts.add(part);
                    } else {
                        adjustedPartList.add(part);
                    }
                }
            }
            // sub parts will be added at the end
            adjustedPartList.addAll(subparts);
        }
        
        // if no parts available -> create item without parts
        List<PartXml> partxmlList = createNewParts(adjustedPartList, itemMap);
        // if no parts available -> return null
        if (CollectionUtils.isEmpty(partxmlList)) {
            // schema allows empty element prts
            return null;
        }

        PartsXml parts = new PartsXml();
        for (PartXml part : partxmlList) {
            parts.getList().add(part);
        }
        return parts;
    }

    /**
     * Create list of new parts
     * 
     * @param partList
     * @param itemMap
     * @param externalSystemTypeCode
     * @return
     */
    protected List<PartXml> createNewParts(Collection<ApPart> partList,
                                           Map<Integer, List<ApItem>> itemMap) {
        if (CollectionUtils.isEmpty(partList)) {
            return Collections.emptyList();
        }

        // collection of available parts for export
        // note: if parent part is deleted, subparts may still be 
        //       included in partList, these parts without parent
        //       parts have to be filtered out.
        Set<String> availableParts = new HashSet<>();
        Map<String, Integer> subpartCounter = new HashMap<>();

        List<PartXml> partXmlList = new ArrayList<>();
        for (ApPart part : partList) {
            List<ApItem> srcPartItems = itemMap.get(part.getPartId());

            // filter parts without mapping
            List<ApItem> partItems = filterOutItemsWithoutExtSysMapping(part, srcPartItems);
            if (CollectionUtils.isNotEmpty(srcPartItems) && CollectionUtils.isEmpty(partItems)) {
                log.debug("Ignoring part, missing mapping to external system, partId={}", part.getPartId());
                continue;
            }

            PartXml partXml = createPart(part, partItems);
            partXmlList.add(partXml);
            availableParts.add(partXml.getPid().getValue());

            log.debug("Exporting part, partId={}, partUuid={}, parentPartId={}", part.getPartId(),
                      partXml.getPid().getValue(),
                      (part.getParentPart() != null) ? part.getParentPart().getPartId() : null);

            if (partXml.getPrnt() != null) {
                int cnt = subpartCounter.getOrDefault(partXml.getPrnt().getValue(), 0);
                cnt++;
                subpartCounter.put(partXml.getPrnt().getValue(), cnt++);
            }
        }

        // do filtering
        boolean modified;
        do {
            int size = partXmlList.size();
            partXmlList = partXmlList.stream()
                    .filter(p -> {
                        // filter ignored subparts (parent part is already ignored)
                        if (p.getPrnt() != null && !availableParts.contains(p.getPrnt().getValue())) {
                            log.debug("Ignoring part, due to ignored parent part, parentPartUuid={}, partUuid={}",
                                      p.getPrnt().getValue(),
                                      p.getPid().getValue());

                            availableParts.remove(p.getPid().getValue());
                            removePart(p);
                            return false;
                        }
                        // filter empty parts without subparts
                        if (p.getItms() == null || p.getItms().getItems().size() == 0) {
                            // no items, we have to check if has subpart
                            Integer cnt = subpartCounter.getOrDefault(p.getPid().getValue(), 0);
                            if (cnt == 0) {
                                log.debug("Ignoring part, due missing items, partUuid={}",
                                          p.getPid().getValue());
                                availableParts.remove(p.getPid().getValue());
                                // decrement parent counter
                                if (p.getPrnt() != null) {
                                    cnt = subpartCounter.getOrDefault(p.getPrnt().getValue(), 0);
                                    if (cnt > 0) {
                                        cnt--;
                                        subpartCounter.put(p.getPrnt().getValue(), cnt);
                                    }
                                }
                                removePart(p);
                                return false;
                            }
                        }
                        return true;
                    })
                    .collect(Collectors.toList());
            modified = (size > partXmlList.size());
        } while (modified);

        return partXmlList;
    }

    /**
     * Remove previously prepared XML part
     * @param partXml
     */
    private void removePart(PartXml partXml) {
    	String uuid = partXml.getPid().getValue();
		for(Entry<Integer, String> ep: partUuids.entrySet()) {
			if(Objects.equals(uuid, ep.getValue())) {
				partUuids.remove(ep.getKey());
				break;
			}
		}
		// remove items
		ItemsXml items = partXml.getItms();
		if(items!=null) {
			for(Object item: items.getItems()) {
				removeItem(item);
			}
		}
	}

	private void removeItem(Object item) {
		if(item instanceof ItemStringXml ) {
			ItemStringXml its = (ItemStringXml)item;
			removeItemByUuid(its.getUuid().getValue());
		} else 
		if(item instanceof ItemIntegerXml) {
			ItemIntegerXml ix = (ItemIntegerXml)item;
			removeItemByUuid(ix.getUuid().getValue());			
		} else 
		if(item instanceof ItemBooleanXml) {
			ItemBooleanXml ix = (ItemBooleanXml)item;
			removeItemByUuid(ix.getUuid().getValue());			
		} else 
		if(item instanceof ItemBinaryXml) {
			ItemBinaryXml ix = (ItemBinaryXml)item;
			removeItemByUuid(ix.getUuid().getValue());			
		} else 
		if(item instanceof ItemEntityRefXml) {
			ItemEntityRefXml ix = (ItemEntityRefXml)item;
			removeItemByUuid(ix.getUuid().getValue());			
		} else 
		if(item instanceof ItemEnumXml) {
			ItemEnumXml ix = (ItemEnumXml)item;
			removeItemByUuid(ix.getUuid().getValue());			
		} else 
		if(item instanceof ItemUnitDateXml) {
			ItemUnitDateXml ix = (ItemUnitDateXml)item;
			removeItemByUuid(ix.getUuid().getValue());			
		} else 
		if(item instanceof ItemLinkXml) {
			ItemLinkXml ix = (ItemLinkXml)item;
			removeItemByUuid(ix.getUuid().getValue());			
		} else {
			throw new IllegalStateException("Unrecognized item type: "+ item.getClass().toString()
					+", item: "+item);
		}
	}

	/**
	 * Remove previously prepared XML item 
	 * @param uuid
	 */
	private void removeItemByUuid(String uuid) {
		for(Entry<Integer, String> ep: itemUuids.entrySet()) {
			if(Objects.equals(uuid, ep.getValue())) {
				itemUuids.remove(ep.getKey());
				break;
			}
		}	
	}

	/**
     * Create part
     * 
     * @param apPart
     * @param partItems
     * @param externalSystemTypeCode
     * @return
     */
    private PartXml createPart(ApPart apPart, List<ApItem> partItems) {
        Validate.isTrue(partItems.size() > 0, "Empty part list, entityId: ", apPart.getAccessPointId());

        String uuid = getUuidForPart(apPart);

        log.debug("Creating part, partId: {}, partUuid: {}", apPart.getPartId(), uuid);

        String parentUuid;
        if (apPart.getParentPart() != null) {
            parentUuid = getUuidForPart(apPart.getParentPart());
        } else {
            parentUuid = null;
        }

        PartXml part = createPart(apPart, parentUuid, uuid);

        ItemsXml itemsXml = createItems(apPart, partItems);
        part.setItms(itemsXml);
        return part;
    }

    private ItemsXml createItems(ApPart apPart, Collection<ApItem> itemList) {
        ItemsXml items = new ItemsXml();
        if (CollectionUtils.isNotEmpty(itemList)) {
            createXmlItems(itemList, items.getItems());
        }
        return items;
    }

    /**
     * Fill list of XML items
     * 
     * @param itemList
     * @param binding
     * @param sdp
     * @param trgList
     */
    public void createXmlItems(Collection<ApItem> itemList, List<Object> trgList) {
        for (ApItem item : itemList) {
            String uuid = UUID.randomUUID().toString();
            Object i = createItem(item, uuid);
            if (i != null) {
                itemUuids.put(item.getItemId(), uuid);
                trgList.add(i);
            }
        }
    }

    /**
     * Return Uuid for given part
     * 
     * @param apPart
     * 
     * @return
     */
    protected String getUuidForPart(ApPart apPart) {
        String uuid = partUuidMap.get(apPart.getPartId());
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
            partUuidMap.put(apPart.getPartId(), uuid);
            // Collection of UUIDs for new parts
            partUuids.put(apPart.getPartId(), uuid);
        }
        return uuid;
    }

    /**
     * Metoda odfiltruje itemy, které nemají mapování v externím systému
     * 
     * @param part
     *
     * @param itemList
     *            itemy k filtrování
     * @param externalSystemTypeCode
     *            kód typu externího systému
     * @return kolekce itemů k poslání do externího systému
     */
    protected List<ApItem> filterOutItemsWithoutExtSysMapping(ApPart part, List<ApItem> itemList) {
        List<ApItem> filteredItems = groovyService.filterOutgoingItems(part,
                                                                       itemList,
                                                                       scope.getRuleSetId());

        return filteredItems;
    }

	protected Object createItem(ApItem item, String uuid) {
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
			return convertEntityRef(data, itemTypeCode, itemSpecCode, uuidXml);
		case COORDINATES:
			return convertCoordinates(data, itemTypeCode, itemSpecCode, uuidXml, apDataService);
		default:
			throw new BusinessException("Failed to export item, unsupported data type: " + dataType + ", itemId:"
					+ item.getItemId() + ", class: " + data.getClass(), BaseCode.EXPORT_FAILED);
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
    
    /*
    protected EntityRecordRefXml createEntityRef(ArrDataRecordRef dataRecordRef) {
        // create record ref only for records with same binding
        if (dataRecordRef.getBinding() == null || !dataRecordRef.getBinding().getApExternalSystem()
                .getExternalSystemId().equals(binding.getApExternalSystem().getExternalSystemId())) {
            return null;
        }
        EntityRecordRefXml entityRecordRef = new EntityRecordRefXml();
        entityRecordRef.setEid(new EntityIdXml(Long.parseLong(dataRecordRef.getBinding().getValue())));
        return entityRecordRef;
    }
    */
    
    abstract protected EntityRecordRefXml createEntityRef(ArrDataRecordRef recordRef);
    
    private ItemEntityRefXml convertEntityRef(ArrData data, CodeXml itemTypeCode, CodeXml itemSpecCode,
                                                     UuidXml uuidXml) {
        if (!(data instanceof ArrDataRecordRef)) {
            throw new BusinessException("Failed to convert data: " + data.getDataId(),
                    BaseCode.EXPORT_FAILED);
        }
        ArrDataRecordRef dataRecordRef = (ArrDataRecordRef) data;

        EntityRecordRefXml entityRecordRef = createEntityRef(dataRecordRef);
        // check if we have link to external entity
        if (entityRecordRef == null) {
        	log.info("Failed to create entity reference for external system, dataId={}", dataRecordRef.getDataId());
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


    public PartXml createPart(ApPart apPart,
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
