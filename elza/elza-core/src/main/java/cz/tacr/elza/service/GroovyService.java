package cz.tacr.elza.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;

import cz.tacr.elza.common.db.HibernateUtils;
import jakarta.validation.constraints.NotNull;

import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.RuleSet;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.data.StructType;
import cz.tacr.elza.core.data.StructTypeExtension;
import cz.tacr.elza.domain.AccessPointItem;
import cz.tacr.elza.domain.AccessPointPart;
import cz.tacr.elza.domain.ApIndex;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApRevItem;
import cz.tacr.elza.domain.ApRevPart;
import cz.tacr.elza.domain.ApRevision;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataBit;
import cz.tacr.elza.domain.ArrDataCoordinates;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.ArrDataUriRef;
import cz.tacr.elza.domain.RulArrangementRule;
import cz.tacr.elza.domain.RulComponent;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulStructureDefinition;
import cz.tacr.elza.domain.RulStructureExtensionDefinition;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.groovy.GroovyAe;
import cz.tacr.elza.groovy.GroovyItem;
import cz.tacr.elza.groovy.GroovyItems;
import cz.tacr.elza.groovy.GroovyPart;
import cz.tacr.elza.groovy.GroovyResult;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.repository.ArrangementRuleRepository;
import cz.tacr.elza.service.cache.AccessPointCacheService;
import cz.tacr.elza.service.cache.CachedAccessPoint;

@Service
public class GroovyService {

    private static final Logger log = LoggerFactory.getLogger(GroovyService.class);

    private static GroovyService _self;

    @Autowired
    private PartService partService;

    @Autowired
    private RevisionPartService revisionPartService;

    @Autowired
    private RevisionItemService revisionItemService;

    @Autowired
    private GroovyScriptService groovyScriptService;

    @Autowired
    private AccessPointItemService accessPointItemService;

    @Autowired
    private AccessPointService apService;

    @Autowired
    private StaticDataService staticDataService;

    @Autowired
    private ApStateRepository apStateRepository;

    @Autowired
    private ResourcePathResolver resourcePathResolver;

    @Autowired
    protected ArrangementRuleRepository arrangementRuleRepository;

    @Autowired
    protected AccessPointCacheService accessPointCacheService;

    @PostConstruct
    public void setStatic() {
        _self = this;
    }

    public GroovyAe convertAe(@NotNull final ApState state,
                              @NotNull final List<ApPart> parts,
                              @NotNull final List<ApItem> items) {
        return convertAe(state, parts, Collections.emptyList(), items, Collections.emptyList());
    }

    public GroovyAe convertAe(@NotNull final ApState state,
                              @NotNull final List<ApPart> parts,
                              @NotNull final List<ApRevPart> revParts,
                              @NotNull final List<ApItem> items,
                              @NotNull final List<ApRevItem> revItems) {
        StaticDataProvider sdp = staticDataService.getData();
        ApType apType = sdp.getApTypeById(state.getApTypeId());
        List<GroovyPart> groovyParts = new ArrayList<>(parts.size());
        Integer preferredPartId = state.getAccessPoint().getPreferredPartId();
        List<AccessPointItem> accessPointItemList = new ArrayList<>(items);
        for (ApPart part : parts) {
            List<AccessPointPart> childrenParts = new ArrayList<>();
            for (ApPart p : parts) {
                if (p.getParentPart() != null && part.getPartId().equals(p.getParentPart().getPartId())) {
                    childrenParts.add(p);
                }
            }

            boolean preferred = Objects.equals(preferredPartId, part.getPartId());
            groovyParts.add(convertPart(state.getApTypeId(), part, childrenParts, accessPointItemList, revItems, preferred));
        }
        // přidat Part(s), které byly přidány v revizi
        for (ApRevPart part : revParts) {
            if (part.getOriginalPartId() == null) {
                groovyParts.add(convertRevPart(state.getApTypeId(), part, revItems));
            }
        }
        return new GroovyAe(apType.getCode(), groovyParts);
    }

    /**
     * 
     * @param apTypeId
     * @param part
     * @param childrenParts
     * @param items
     *            List of item. Might contain also items from another part.
     *            At least should contain items for this part and its childrenParts.
     * @param preferred
     * @return
     */
    public GroovyResult processGroovy(@NotNull final Integer apTypeId,
                                      @NotNull final ApPart part,
                                      @Nullable final List<? extends AccessPointPart> childrenParts,
                                      @NotNull final List<? extends AccessPointItem> items,
                                      final boolean preferred) {
        return processGroovy(apTypeId, part, childrenParts, items, Collections.emptyList(), preferred);
    }

    /**
     * 
     * @param apTypeId
     * @param part
     * @param childrenParts
     * @param items
     *            List of item. Might contain also items from another part.
     *            At least should contain items for this part and its childrenParts.
     * @param revItems
     *            List of revItems. Might contain also revItems from another part.
     *            At least should contain revItems for this part and its
     *            childrenParts.
     * @param preferred
     * @return
     */
    public GroovyResult processGroovy(@NotNull final Integer apTypeId,
                                      @NotNull final AccessPointPart part,
                                      @Nullable final List<? extends AccessPointPart> childrenParts,
                                      @NotNull final List<? extends AccessPointItem> items,
                                      @NotNull final List<ApRevItem> revItems,
                                      final boolean preferred) {
        GroovyPart groovyPart = convertPart(apTypeId, part, childrenParts, items, revItems, preferred);
        return groovyScriptService.process(groovyPart, getGroovyFilePath(groovyPart));
    }

    public List<GroovyItem> getAutoItems(@NotNull final ApState state) {
        ApScope scope = state.getScope();
        List<ApPart> parts = partService.findPartsByAccessPoint(state.getAccessPoint());
        List<ApItem> itemsByParts = accessPointItemService.findItemsByParts(parts);
        GroovyAe groovyAe = convertAe(state, parts, itemsByParts);
        String groovyFilePath = getGroovyFilePath(RulArrangementRule.RuleType.AUTO_ITEMS, scope.getRuleSetId());

        return groovyScriptService.process(groovyAe, groovyFilePath);
    }

    public List<GroovyItem> getAutoItemsForRev(@NotNull final ApState state, @NotNull final ApRevision revision) {
        ApScope scope = state.getScope();
        List<ApPart> parts = partService.findPartsByAccessPoint(state.getAccessPoint());
        List<ApItem> itemsByParts = accessPointItemService.findItemsByParts(parts);
        List<ApRevPart> revParts = revisionPartService.findPartsByRevision(revision);
        List<ApRevItem> itemsByRevParts = revisionItemService.findByParts(revParts);
        GroovyAe groovyAe = convertAe(revision.getState(), parts, revParts, itemsByParts, itemsByRevParts);
        String groovyFilePath = getGroovyFilePath(RulArrangementRule.RuleType.AUTO_ITEMS, scope.getRuleSetId());

        return groovyScriptService.process(groovyAe, groovyFilePath);
    }

    /**
     * 
     * @param apTypeId
     * @param part
     * @param childrenParts
     * @param items
     *            List of item. Might contain also items from another part.
     *            At least should contain items for this part and its childrenParts.
     * @param revItems
     *            List of revItems. Might contain also revItems from another part.
     *            At least should contain revItems for this part and its
     *            childrenParts.
     * @param preferred
     * @return
     */
    public GroovyPart convertPart(@NotNull final Integer apTypeId,
                                  @NotNull final AccessPointPart part,
                                  @Nullable final List<? extends AccessPointPart> childrenParts,
                                  @NotNull final List<? extends AccessPointItem> items,
                                  @NotNull final List<ApRevItem> revItems,
                                  final boolean preferred) {
        if (part == null) {
            throw new IllegalArgumentException("part cannot be null");
        }
        AccessPointPart rawPart = HibernateUtils.unproxy(part);
        ApPart apPart = null;
        ApRevPart apRevPart = null;
        if (rawPart instanceof ApPart) {
            apPart = (ApPart) rawPart;
        } else if (rawPart instanceof ApRevPart) {
            apRevPart = (ApRevPart) rawPart;
        }

        StaticDataProvider sdp = staticDataService.getData();

        // příprava seznamů změněných a přidaných ApRevItem
        HashMap<Integer, ApRevItem> modifiedItems = new HashMap<>();
        List<ApRevItem> appendedItems = new ArrayList<>();
        for (ApRevItem revItem : revItems) {
            ApRevPart itemPart = revItem.getPart();
            boolean ownRevItem = false;
            // check if revItem belongs to the part
            if (apRevPart != null && Objects.equals(apRevPart.getPartId(), itemPart.getPartId())) {
                // item is from this revision part everything is ok
                ownRevItem = true;
            } else if (apPart != null && Objects.equals(apPart.getPartId(), itemPart.getOriginalPartId())) {
                // item is from origPart, everything is ok
                ownRevItem = true;
            }
            if (ownRevItem) {
                if (revItem.getOrigObjectId() == null) {
                    appendedItems.add(revItem);
                } else {
                    modifiedItems.put(revItem.getOrigObjectId(), revItem);
                }
            }
        }

        GroovyItems groovyItems = new GroovyItems();
        for (AccessPointItem item : items) {
            AccessPointPart itemPart = item.getPart();
            if (Objects.equals(itemPart.getPartId(), part.getPartId()) && item.getData() != null) {

                // existuje nová revize item?
                ApRevItem revItem = modifiedItems.get(item.getObjectId());
                if (revItem != null) {
                    // pokud je item smazán, pak null
                    item = revItem.isDeleted()? null : (AccessPointItem) revItem;
                }

                if (item != null) {
                    groovyItems.addItem(convertItem(item, sdp));
                }
            }
        }

        // přidat nový revItems
        for (ApRevItem revItem : appendedItems) {
            groovyItems.addItem(convertItem(revItem, sdp));
        }

        List<GroovyPart> groovyParts = Collections.emptyList();
        if (childrenParts != null) {
            groovyParts = new ArrayList<>(childrenParts.size());
            for (AccessPointPart childPart : childrenParts) {
                groovyParts.add(convertPart(apTypeId, childPart, null, items, revItems, false));
            }
        }

        return new GroovyPart(sdp, apTypeId, part.getPartTypeId(),
                preferred,
                groovyItems,
                groovyParts);
    }

    public GroovyPart convertRevPart(@NotNull final Integer apTypeId,
                                     @NotNull final ApRevPart part,
                                     @NotNull final List<ApRevItem> revItems) {
        StaticDataProvider sdp = staticDataService.getData();

        GroovyItems groovyItems = new GroovyItems();
        for (ApRevItem revItem : revItems) {
            groovyItems.addItem(convertItem(revItem, sdp));
        }

        return new GroovyPart(sdp, apTypeId, part.getPartTypeId(),
                              false,
                              groovyItems,
                              Collections.emptyList());
    }

    public GroovyItem convertItem(AccessPointItem item, StaticDataProvider sdp) {
        ItemType itemType = sdp.getItemTypeById(item.getItemTypeId());
        RulItemSpec itemSpec = item.getItemSpec() == null ? null : sdp.getItemSpecById(item.getItemSpecId());
        
        ArrData data = HibernateUtils.unproxy(item.getData());
        if(data==null) {
        	// probably deleted item - has no data
        	// we have to create it without it
        	return new GroovyItem(itemType, itemSpec); 
        }
        

        DataType dataType = itemType.getDataType();
        GroovyItem groovyItem;
        switch (dataType) {
            case BIT: {
                ArrDataBit dataTmp = (ArrDataBit) data;
                groovyItem = new GroovyItem(itemType, itemSpec, dataTmp.isBitValue());
                break;
            }
            case STRING: {
                ArrDataString dataTmp = (ArrDataString) data;
                groovyItem = new GroovyItem(itemType, itemSpec, dataTmp.getStringValue());
                break;
            }
            case COORDINATES: {
                ArrDataCoordinates dataTmp = (ArrDataCoordinates) data;
                groovyItem = new GroovyItem(itemType, itemSpec, dataTmp.getFulltextValue());
                break;
            }
            case TEXT: {
                ArrDataText dataTmp = (ArrDataText) data;
                if(dataTmp==null) {
                	System.out.println("??");
                }
                groovyItem = new GroovyItem(itemType, itemSpec, dataTmp.getTextValue());
                break;
            }
            case INT: {
                ArrDataInteger dataTmp = (ArrDataInteger) data;
                groovyItem = new GroovyItem(itemType, itemSpec, dataTmp.getIntegerValue());
                break;
            }
            case UNITDATE: {
                ArrDataUnitdate dataTmp = (ArrDataUnitdate) data;
                groovyItem = new GroovyItem(itemType, itemSpec, dataTmp);
                break;
            }
            case RECORD_REF: {
                ArrDataRecordRef dataTmp = (ArrDataRecordRef) data;
                // Name of AP
                String value;
                // Record Id
                Integer intValue;
                CachedAccessPoint accessPoint = null;
                if (dataTmp.getRecord() != null) {
                    intValue = dataTmp.getRecordId();
                    ApIndex index = apService.findPreferredPartIndex(intValue);
                    value = index == null? null : index.getIndexValue();
                    accessPoint = accessPointCacheService.findCachedAccessPoint(intValue);
                } else if (dataTmp.getBinding() != null) {
                    value = dataTmp.getBinding().getValue();
                    // pokud se jedná o pouhý odkaz do externího systému (bez lokálního AP)
                    // nastavuje se id z bindingu jako záporný idnetifikátor pro odlišení
                    // identifikátorů z lokálních AP
                    intValue = dataTmp.getBinding().getBindingId() * -1;
                } else {
                    log.error("Empty RecordRef, dataId: ", dataTmp.getDataId());

                    throw new SystemException("RecordRef without any data, dataId: " + dataTmp.getDataId(),
                            BaseCode.DB_INTEGRITY_PROBLEM)
                                    .set("dataId", dataTmp.getDataId());
                }

                groovyItem = new GroovyItem(itemType, itemSpec, accessPoint, value, intValue);
                break;
            }
            case ENUM: {
                groovyItem = new GroovyItem(itemType, itemSpec, itemSpec != null ? itemSpec.getName() : null);
                break;
            }
            case URI_REF: {
                ArrDataUriRef dataTmp = (ArrDataUriRef) data;
                groovyItem = new GroovyItem(itemType, itemSpec, dataTmp.getFulltextValue());
                break;
            }
            default:
                throw new NotImplementedException("Neimplementovaný typ: " + dataType);
        }
        return groovyItem;
    }

    public List<GroovyAe> findAllParents(@NotNull final Integer entityId,
                                         @NotNull final ItemType itemType) {
        List<GroovyAe> result = new ArrayList<>();
        ApState state = apStateRepository.findLastByAccessPointId(entityId);
        do {
            List<ApPart> parts = partService.findPartsByAccessPoint(state.getAccessPoint());
            List<ApItem> itemsByParts = accessPointItemService.findItemsByParts(parts);
            result.add(convertAe(state, parts, itemsByParts));

            Integer recordId = null;
            for (ApItem aeItem : itemsByParts) {
                if (aeItem.getItemTypeId().equals(itemType.getItemTypeId())) {
                    ArrDataRecordRef data = HibernateUtils.unproxy(aeItem.getData());
                    recordId = data.getRecordId();
                    break;
                }
            }
            state = recordId == null ? null : apStateRepository.findLastByAccessPointId(recordId);

        } while (state != null);

        return result;
    }

    public static List<GroovyAe> findParentAe(final Integer recordId, final ItemType itemType) {
        return _self.findAllParents(recordId, itemType);
    }

    public List<ApItem> filterOutgoingItems(ApPart part, List<ApItem> itemList,
                                            Integer ruleSetId) {
        String filePath = getGroovyFilePath(RulArrangementRule.RuleType.AP_MAPPING_TYPE, ruleSetId);
        return groovyScriptService.filterOutgoingItems(part, itemList, filePath);
    }

    public String getGroovyFilePath(GroovyPart part) {
        StaticDataProvider sdp = staticDataService.getData();

        RulComponent component = null;
        RulPackage rulPackage = null;

        StructType structType = sdp.getStructuredTypeByCode(part.getPartTypeCode());
        ApType apType = sdp.getApTypeByCode(part.getAeType());
        while (apType != null) {
            String extCode = apType.getCode() + "/" + part.getPartTypeCode();
            StructTypeExtension structTypeExt = structType.getExtByCode(extCode);
            if (structTypeExt != null) {
                List<RulStructureExtensionDefinition> structureExtensionDefinitions = structTypeExt
                        .getDefsByType(RulStructureExtensionDefinition.DefType.SERIALIZED_VALUE);
                if (structureExtensionDefinitions.size() > 0) {
                    RulStructureExtensionDefinition structureExtensionDefinition = structureExtensionDefinitions
                            .get(structureExtensionDefinitions.size() - 1);
                    component = structureExtensionDefinition.getComponent();
                    rulPackage = structureExtensionDefinition.getRulPackage();
                    break;
                }
            }
            apType = apType.getParentApType();
        }

        // if not found in extension read from base definition
        if (component == null) {
            // extension not exists -> we will find script in standard definition
            List<RulStructureDefinition> structureDefinitions = structType
                    .getDefsByType(RulStructureDefinition.DefType.SERIALIZED_VALUE);
            if (structureDefinitions.size() > 0) {
                RulStructureDefinition structureDefinition = structureDefinitions.get(structureDefinitions.size() - 1);
                component = structureDefinition.getComponent();
                rulPackage = structureDefinition.getRulPackage();
            } else {
                throw new SystemException("Strukturovaný typ '" + structType.getCode()
                        + "' nemá žádný script pro výpočet hodnoty", BaseCode.INVALID_STATE);
            }
        }

        return resourcePathResolver.getGroovyDir(rulPackage)
                .resolve(component.getFilename())
                .toString();
    }

    public String getGroovyFilePath(RulArrangementRule.RuleType ruleType, Integer ruleSetId) {
        StaticDataProvider sdp = staticDataService.getData();
        RuleSet ruleSet = sdp.getRuleSetById(ruleSetId);
        List<RulArrangementRule> rulArrangementRules = ruleSet.getRulesByType(ruleType);

        RulArrangementRule arrangementRule;

        if (rulArrangementRules.size() > 0) {
            arrangementRule = rulArrangementRules.get(0);
        } else {
            throw new SystemException("Neexistuje žádné pravidlo typu '" + ruleType.toString()
                    + "' pro výpočet hodnoty", BaseCode.INVALID_STATE);
        }

        return resourcePathResolver.getDroolFile(arrangementRule)
                .toString();
    }

}
