package cz.tacr.elza.cam.v2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cz.tacr.cam.v2.schema.cam.BatchChangeFailureXml;
import cz.tacr.cam.v2.schema.cam.EntityIssuesXml;
import cz.tacr.cam.v2.schema.cam.EntityRecordRefXml;
import cz.tacr.cam.v2.schema.cam.EntityXml;
import cz.tacr.cam.v2.schema.cam.ExistingIssueXml;
import cz.tacr.cam.v2.schema.cam.ItemRefXml;
import cz.tacr.cam.v2.schema.cam.PartRefXml;
import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApBindingItem;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApIndex;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataBit;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulPartType;
import cz.tacr.elza.groovy.GroovyResult;
import cz.tacr.elza.repository.ApBindingItemRepository;
import cz.tacr.elza.repository.ApBindingRepository;
import cz.tacr.elza.repository.ApBindingStateRepository;
import cz.tacr.elza.service.cache.AccessPointCacheService;
import cz.tacr.elza.service.cache.CachedAccessPoint;
import cz.tacr.elza.service.cache.CachedPart;

/**
 * Resolves CAM-domain UUIDs in {@link ExistingIssueXml} refs (partRef, itemRef,
 * entityRef) into ELZA DB ids (partId, itemId, accessPointId) and
 * human-readable names for UI navigation.
 *
 * <p>Has two construction entry points for the two flows where CAM issues are
 * processed:
 * <ul>
 *   <li>{@link #build} — export-failure path. Resolves part/item ids <em>and</em>
 *       entityRefs <em>and</em> display names for the failure VO shown in the UI.
 *       Source of refs is a {@link BatchChangeFailureXml}; ids may come from a
 *       transient {@code upload_map} payload attached to the export queue item
 *       (parts/items created in the same upload don't have an {@link ApBindingItem} yet).</li>
 *   <li>{@link #buildForImport} — import path. Resolves <em>only</em> part/item
 *       ids (the only thing the import mapper stores); all parts/items already
 *       have an {@link ApBindingItem} for the binding, so one batch query
 *       suffices and no names/entityRefs are computed.</li>
 * </ul>
 *
 * Part/item id resolution checks, in order: the transient seed map (export only),
 * then {@link ApBindingItem}. Name resolution (export only) uses the
 * {@link CachedAccessPoint} of the AP and of each referenced entity; both come
 * from {@code ap_cached_access_point} and already hold the DISPLAY_NAME indexes.
 *
 * Unresolved refs return {@code null}; callers fall back to rendering the
 * issue without a nav target rather than guessing.
 */
public class IssueRefResolver {

    private static final int ITEM_VALUE_PREVIEW_LIMIT = 50;

    private final Map<String, Integer> partUuidToId;
    private final Map<String, Integer> itemUuidToId;
    /** Keyed by {@link ApBinding#getValue()} — the CAM entityId (CAM_V2) or entityUuid (CAM_UUID_V2). */
    private final Map<String, Integer> entityValueToAccessPointId;
    private final Map<Integer, Integer> itemIdToPartId;
    private final Map<Integer, String> partIdToName;
    private final Map<Integer, String> itemIdToName;
    private final Map<Integer, String> accessPointIdToName;

    private IssueRefResolver(Map<String, Integer> partUuidToId,
                             Map<String, Integer> itemUuidToId,
                             Map<String, Integer> entityValueToAccessPointId,
                             Map<Integer, Integer> itemIdToPartId,
                             Map<Integer, String> partIdToName,
                             Map<Integer, String> itemIdToName,
                             Map<Integer, String> accessPointIdToName) {
        this.partUuidToId = partUuidToId;
        this.itemUuidToId = itemUuidToId;
        this.entityValueToAccessPointId = entityValueToAccessPointId;
        this.itemIdToPartId = itemIdToPartId;
        this.partIdToName = partIdToName;
        this.itemIdToName = itemIdToName;
        this.accessPointIdToName = accessPointIdToName;
    }

    /**
     * Build a resolver for the export-failure path. Does a single batch lookup
     * per external source:
     * <ul>
     *   <li>one {@link ApBindingItem} query for all unresolved part/item uuids,</li>
     *   <li>one {@link ApBinding} query for all referenced entity ids/uuids,</li>
     *   <li>one {@link CachedAccessPoint} fetch per distinct AP referenced.</li>
     * </ul>
     */
    public static IssueRefResolver build(BatchChangeFailureXml failure,
                                         String uploadMapJson,
                                         Integer exportedApId,
                                         ApBinding apBinding,
                                         ApExternalSystem externalSystem,
                                         ApBindingItemRepository bindingItemRepository,
                                         ApBindingRepository bindingRepository,
                                         ApBindingStateRepository bindingStateRepository,
                                         AccessPointCacheService accessPointCacheService,
                                         StaticDataProvider staticData) {
        // 1. seed from the transient payload (new parts/items in this upload);
        //    only the uuid mappings are relevant here, participants are ignored
        Map<String, Integer> partSeed = new HashMap<>();
        Map<String, Integer> itemSeed = new HashMap<>();
        for (UuidMapping m : UploadMapping.deserialize(uploadMapJson).getUuidMappings()) {
            if (m.getPartId() != null) {
                partSeed.put(m.getUuid(), m.getPartId());
            } else if (m.getItemId() != null) {
                itemSeed.put(m.getUuid(), m.getItemId());
            }
        }

        // 2. flatten failure's per-entity issues into one stream
        List<ExistingIssueXml> issues = new ArrayList<>();
        for (EntityIssuesXml entityIssues : failure.getIssues()) {
            issues.addAll(entityIssues.getIssue());
        }

        return buildInternal(issues, partSeed, itemSeed, exportedApId, apBinding, externalSystem,
                bindingItemRepository, bindingRepository, bindingStateRepository,
                accessPointCacheService, staticData);
    }

    /**
     * Build a resolver for the CAM-import path. Only part/item ids are resolved
     * (the only thing the import mapper consumes); names and entityRefs are not
     * — the stored {@code ap_binding_issue} keeps ids, and the UI resolves names
     * at read time. All parts/items already have an {@link ApBindingItem} for
     * this binding, so a single batch query is enough.
     */
    public static IssueRefResolver buildForImport(EntityXml entity,
                                                  ApBinding apBinding,
                                                  ApBindingItemRepository bindingItemRepository) {
        List<ExistingIssueXml> issues = (entity.getIssues() != null)
                ? entity.getIssues().getIssue()
                : Collections.emptyList();

        Set<String> refUuids = new HashSet<>();
        for (ExistingIssueXml issue : issues) {
            PartRefXml partRef = issue.getPartRef();
            if (partRef != null && partRef.getPartUuid() != null) {
                refUuids.add(partRef.getPartUuid().getValue());
            }
            ItemRefXml itemRef = issue.getItemRef();
            if (itemRef != null && itemRef.getUuid() != null) {
                refUuids.add(itemRef.getUuid().getValue());
            }
        }

        Map<String, Integer> partUuidToId = new HashMap<>();
        Map<String, Integer> itemUuidToId = new HashMap<>();
        if (!refUuids.isEmpty() && apBinding != null) {
            for (ApBindingItem bi : bindingItemRepository.findByBindingAndUuidIn(apBinding, refUuids)) {
                if (bi.getPartId() != null) {
                    partUuidToId.put(bi.getValue(), bi.getPartId());
                } else if (bi.getItemId() != null) {
                    itemUuidToId.put(bi.getValue(), bi.getItemId());
                }
            }
        }
        return new IssueRefResolver(partUuidToId, itemUuidToId, Map.of(),
                Map.of(), Map.of(), Map.of(), Map.of());
    }

    private static IssueRefResolver buildInternal(List<ExistingIssueXml> issues,
                                                  Map<String, Integer> partUuidToIdSeed,
                                                  Map<String, Integer> itemUuidToIdSeed,
                                                  Integer apId,
                                                  ApBinding apBinding,
                                                  ApExternalSystem externalSystem,
                                                  ApBindingItemRepository bindingItemRepository,
                                                  ApBindingRepository bindingRepository,
                                                  ApBindingStateRepository bindingStateRepository,
                                                  AccessPointCacheService accessPointCacheService,
                                                  StaticDataProvider staticData) {
        Map<String, Integer> partUuidToId = new HashMap<>(partUuidToIdSeed);
        Map<String, Integer> itemUuidToId = new HashMap<>(itemUuidToIdSeed);

        // collect uuids/ids referenced by issues that aren't in the seed
        Set<String> unresolvedUuids = new HashSet<>();
        Set<String> referencedEntityUuids = new HashSet<>();
        Set<String> referencedEntityIds = new HashSet<>();
        for (ExistingIssueXml issue : issues) {
            PartRefXml partRef = issue.getPartRef();
            if (partRef != null && partRef.getPartUuid() != null) {
                String u = partRef.getPartUuid().getValue();
                if (!partUuidToId.containsKey(u)) {
                    unresolvedUuids.add(u);
                }
            }
            ItemRefXml itemRef = issue.getItemRef();
            if (itemRef != null && itemRef.getUuid() != null) {
                String u = itemRef.getUuid().getValue();
                if (!itemUuidToId.containsKey(u)) {
                    unresolvedUuids.add(u);
                }
            }
            EntityRecordRefXml entityRef = issue.getEntityRef();
            if (entityRef != null) {
                if (entityRef.getEntityUuid() != null) {
                    referencedEntityUuids.add(entityRef.getEntityUuid().getValue());
                }
                if (entityRef.getEntityId() != null) {
                    referencedEntityIds.add(Long.toString(entityRef.getEntityId().getValue()));
                }
            }
        }

        // one batch query for the part/item binding items, if any remain
        if (!unresolvedUuids.isEmpty() && apBinding != null) {
            List<ApBindingItem> items = bindingItemRepository.findByBindingAndUuidIn(apBinding, unresolvedUuids);
            for (ApBindingItem bi : items) {
                if (bi.getPartId() != null) {
                    partUuidToId.put(bi.getValue(), bi.getPartId());
                } else if (bi.getItemId() != null) {
                    itemUuidToId.put(bi.getValue(), bi.getItemId());
                }
            }
        }

        // entityRef resolution — an ApBinding.value equals the CAM entityId
        // (CAM_V2) or entityUuid (CAM_UUID_V2); resolveEntity checks both forms.
        Map<String, Integer> entityValueToAp = new HashMap<>();
        if (!referencedEntityIds.isEmpty() || !referencedEntityUuids.isEmpty()) {
            Set<String> all = new HashSet<>();
            all.addAll(referencedEntityIds);
            all.addAll(referencedEntityUuids);
            List<ApBinding> bindings = bindingRepository.findByValuesAndExternalSystem(new ArrayList<>(all), externalSystem);
            List<ApBindingState> states = bindingStateRepository.findByBindings(bindings);
            Map<Integer, Integer> bindingIdToAp = new HashMap<>();
            for (ApBindingState s : states) {
                if (s.getAccessPointId() != null) {
                    bindingIdToAp.put(s.getBinding().getBindingId(), s.getAccessPointId());
                }
            }
            for (ApBinding b : bindings) {
                Integer apIdResolved = bindingIdToAp.get(b.getBindingId());
                if (apIdResolved != null) {
                    entityValueToAp.put(b.getValue(), apIdResolved);
                }
            }
        }

        // resolve names — parts / items via the AP's cache;
        // entities via each referenced AP's cache.
        Map<Integer, Integer> itemIdToPartId = new HashMap<>();
        Map<Integer, String> partIdToName = new HashMap<>();
        Map<Integer, String> itemIdToName = new HashMap<>();
        Map<Integer, String> apIdToName = new HashMap<>();

        if (apId != null) {
            CachedAccessPoint cachedAp = accessPointCacheService.findCachedAccessPoint(apId);
            if (cachedAp != null) {
                indexLocalNames(cachedAp, staticData, partIdToName, itemIdToName, itemIdToPartId);
            }
        }
        Set<Integer> distinctEntityApIds = new HashSet<>(entityValueToAp.values());
        for (Integer refApId : distinctEntityApIds) {
            CachedAccessPoint cachedAp = accessPointCacheService.findCachedAccessPoint(refApId);
            if (cachedAp != null) {
                String name = extractAccessPointDisplayName(cachedAp);
                if (name != null) {
                    apIdToName.put(refApId, name);
                }
            }
        }

        return new IssueRefResolver(partUuidToId, itemUuidToId, entityValueToAp,
                itemIdToPartId, partIdToName, itemIdToName, apIdToName);
    }

    public Integer resolvePart(PartRefXml partRef) {
        if (partRef == null || partRef.getPartUuid() == null) return null;
        return partUuidToId.get(partRef.getPartUuid().getValue());
    }

    public Integer resolveItem(ItemRefXml itemRef) {
        if (itemRef == null || itemRef.getUuid() == null) return null;
        return itemUuidToId.get(itemRef.getUuid().getValue());
    }

    public Integer resolveEntity(EntityRecordRefXml entityRef) {
        if (entityRef == null) return null;
        if (entityRef.getEntityId() != null) {
            Integer apId = entityValueToAccessPointId.get(Long.toString(entityRef.getEntityId().getValue()));
            if (apId != null) return apId;
        }
        if (entityRef.getEntityUuid() != null) {
            return entityValueToAccessPointId.get(entityRef.getEntityUuid().getValue());
        }
        return null;
    }

    /** Containing part for an item — so an item-only ref still produces a scrollable target. */
    public Integer partIdForItem(Integer itemId) {
        return itemId == null ? null : itemIdToPartId.get(itemId);
    }

    public String resolvePartName(Integer partId) {
        return partId == null ? null : partIdToName.get(partId);
    }

    public String resolveItemName(Integer itemId) {
        return itemId == null ? null : itemIdToName.get(itemId);
    }

    public String resolveEntityName(Integer accessPointId) {
        return accessPointId == null ? null : accessPointIdToName.get(accessPointId);
    }

    // --------------------------------------------------------------
    // cache indexing
    // --------------------------------------------------------------

    private static void indexLocalNames(CachedAccessPoint cachedAp,
                                        StaticDataProvider staticData,
                                        Map<Integer, String> partIdToName,
                                        Map<Integer, String> itemIdToName,
                                        Map<Integer, Integer> itemIdToPartId) {
        if (cachedAp.getParts() == null) return;
        for (CachedPart part : cachedAp.getParts()) {
            Integer partId = part.getPartId();
            if (partId == null) continue;
            partIdToName.put(partId, extractPartDisplayName(part, staticData));
            if (part.getItems() != null) {
                for (ApItem item : part.getItems()) {
                    Integer itemId = item.getItemId();
                    if (itemId == null) continue;
                    itemIdToPartId.put(itemId, partId);
                    itemIdToName.put(itemId, formatItemPreview(item, staticData));
                }
            }
        }
    }

    /** Prefer the DISPLAY_NAME index value; fall back to the part type description / code. */
    private static String extractPartDisplayName(CachedPart part, StaticDataProvider staticData) {
        if (part.getIndices() != null) {
            for (ApIndex idx : part.getIndices()) {
                if (GroovyResult.DISPLAY_NAME.equals(idx.getIndexType())) {
                    String v = idx.getIndexValue();
                    if (v != null && !v.isBlank()) return v;
                }
            }
        }
        RulPartType type = staticData.getPartTypeByCode(part.getPartTypeCode());
        if (type != null) {
            return type.getName() != null ? type.getName() : type.getCode();
        }
        return part.getPartTypeCode();
    }

    private static String extractAccessPointDisplayName(CachedAccessPoint cachedAp) {
        Integer preferredPartId = cachedAp.getPreferredPartId();
        if (preferredPartId == null || cachedAp.getParts() == null) return null;
        for (CachedPart part : cachedAp.getParts()) {
            if (!preferredPartId.equals(part.getPartId()) || part.getIndices() == null) continue;
            for (ApIndex idx : part.getIndices()) {
                if (GroovyResult.DISPLAY_NAME.equals(idx.getIndexType())) {
                    return idx.getIndexValue();
                }
            }
        }
        return null;
    }

    /** "{@code typeDesc: valuePreview}" for scalar data, just type description otherwise. */
    private static String formatItemPreview(ApItem item, StaticDataProvider staticData) {
        String typeDesc = null;
        if (item.getItemTypeId() != null && staticData.getItemTypeById(item.getItemTypeId()) != null) {
            RulItemType t = staticData.getItemTypeById(item.getItemTypeId()).getEntity();
            typeDesc = t.getName() != null ? t.getName() : t.getCode();
        }
        String value = extractScalarValue(item.getData());
        if (value != null && !value.isBlank()) {
            String preview = value.length() > ITEM_VALUE_PREVIEW_LIMIT
                    ? value.substring(0, ITEM_VALUE_PREVIEW_LIMIT) + "…"
                    : value;
            return typeDesc != null ? typeDesc + ": " + preview : preview;
        }
        return typeDesc;
    }

    private static String extractScalarValue(ArrData data) {
        if (data == null) return null;
        ArrData unproxied = HibernateUtils.unproxy(data);
        if (unproxied instanceof ArrDataString) {
            return ((ArrDataString) unproxied).getStringValue();
        }
        if (unproxied instanceof ArrDataText) {
            return ((ArrDataText) unproxied).getTextValue();
        }
        if (unproxied instanceof ArrDataInteger) {
            Integer i = ((ArrDataInteger) unproxied).getValueInt();
            return i == null ? null : i.toString();
        }
        if (unproxied instanceof ArrDataBit) {
            Boolean b = ((ArrDataBit) unproxied).isBitValue();
            return b == null ? null : b.toString();
        }
        return null;
    }

    // for tests / debug
    Collection<Map.Entry<String, Integer>> knownParts() { return partUuidToId.entrySet(); }
    Collection<Map.Entry<String, Integer>> knownItems() { return itemUuidToId.entrySet(); }
}