package cz.tacr.elza.cam;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.locationtech.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.elza.cam.adapter.XmlAdapterFactory;
import cz.tacr.elza.cam.adapter.XmlBinaryItemAdapter;
import cz.tacr.elza.cam.adapter.XmlBooleanItemAdapter;
import cz.tacr.elza.cam.adapter.XmlCodeAdapter;
import cz.tacr.elza.cam.adapter.XmlEntityRefItemAdapter;
import cz.tacr.elza.cam.adapter.XmlEnumItemAdapter;
import cz.tacr.elza.cam.adapter.XmlIntegerItemAdapter;
import cz.tacr.elza.cam.adapter.XmlItemAdapter;
import cz.tacr.elza.cam.adapter.XmlLinkItemAdapter;
import cz.tacr.elza.cam.adapter.XmlStringItemAdapter;
import cz.tacr.elza.cam.adapter.XmlUnitDateItemAdapter;
import cz.tacr.elza.common.GeometryConvertor;
import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApBindingItem;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApState;
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
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.converter.CalendarConverter;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApBindingItemRepository;
import cz.tacr.elza.repository.ApBindingRepository;
import cz.tacr.elza.repository.ApBindingStateRepository;
import cz.tacr.elza.repository.ApItemRepository;
import cz.tacr.elza.repository.ApPartRepository;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.repository.DataRecordRefRepository;
import cz.tacr.elza.service.AccessPointItemService;
import cz.tacr.elza.service.AccessPointItemService.DeletedItems;
import cz.tacr.elza.service.AccessPointItemService.ReferencedEntities;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.AsyncRequestService;
import cz.tacr.elza.service.ExternalSystemService;
import cz.tacr.elza.service.PartService;
import cz.tacr.elza.service.RuleService;
import cz.tacr.elza.service.cache.AccessPointCacheService;

/**
 * Společný základ pro dispatchery zpracovávající entity z různých verzí CAM.
 *
 * Obsahuje sdílený stav a operace, které nejsou závislé na konkrétním
 * XML schématu (mazání částí a vazeb, načtení vazeb na položky, porovnání
 * položek). Verzově specifické operace (parsování EntityXml/PartXml apod.)
 * zůstávají v potomcích.
 */
public abstract class AbstractEntityDBDispatcher {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected static final String SCHEMA_UNKNOWN = "UNKNOWN";

    protected final ApAccessPointRepository accessPointRepository;

    protected final ApStateRepository stateRepository;

    protected final ApBindingRepository bindingRepository;

    protected final ApBindingItemRepository bindingItemRepository;

    protected final DataRecordRefRepository dataRecordRefRepository;

    protected final ApPartRepository partRepository;

    protected final ApItemRepository itemRepository;

    protected final ApBindingStateRepository bindingStateRepository;

    protected final ExternalSystemService externalSystemService;

    protected final AccessPointService accessPointService;

    protected final AccessPointItemService accessPointItemService;

    protected final AsyncRequestService asyncRequestService;

    protected final PartService partService;

    protected final AccessPointCacheService accessPointCacheService;

    protected final RuleService ruleService;

    protected final XmlAdapterFactory adapterFactory;

    protected final List<ApState> createdEntities = new ArrayList<>();

    /**
     * Mapping between partUuid and ApBindingItem
     *
     * Valid during synchronization
     */
    protected Map<String, ApBindingItem> bindingPartLookup;

    protected Map<Integer, Map<String, ApBindingItem>> bindingItemsByPart;

    /**
     * Parts without binding
     */
    protected List<ApPart> partsWithoutBinding;

    protected ProcessingContext procCtx;

    /** Newly created binding state for last processed entity. */
    protected ApBindingState bindingState;

    protected AbstractEntityDBDispatcher(final ApAccessPointRepository accessPointRepository,
                                         final ApStateRepository stateRepository,
                                         final ApBindingRepository bindingRepository,
                                         final ApBindingItemRepository bindingItemRepository,
                                         final DataRecordRefRepository dataRecordRefRepository,
                                         final ApPartRepository partRepository,
                                         final ApItemRepository itemRepository,
                                         final ApBindingStateRepository bindingStateRepository,
                                         final ExternalSystemService externalSystemService,
                                         final AccessPointService accessPointService,
                                         final AccessPointItemService accessPointItemService,
                                         final AsyncRequestService asyncRequestService,
                                         final PartService partService,
                                         final AccessPointCacheService accessPointCacheService,
                                         final RuleService ruleService,
                                         final XmlAdapterFactory adapterFactory) {
        this.accessPointRepository = accessPointRepository;
        this.stateRepository = stateRepository;
        this.bindingRepository = bindingRepository;
        this.bindingItemRepository = bindingItemRepository;
        this.dataRecordRefRepository = dataRecordRefRepository;
        this.partRepository = partRepository;
        this.itemRepository = itemRepository;
        this.bindingStateRepository = bindingStateRepository;
        this.externalSystemService = externalSystemService;
        this.accessPointService = accessPointService;
        this.accessPointItemService = accessPointItemService;
        this.asyncRequestService = asyncRequestService;
        this.partService = partService;
        this.accessPointCacheService = accessPointCacheService;
        this.ruleService = ruleService;
        this.adapterFactory = adapterFactory;
    }

    public List<ApState> getApStates() {
        return createdEntities;
    }

    public ApBindingState getBindingState() {
        return bindingState;
    }

    /**
     * Načte vazby (ApBindingItem) pro danou vazbu a roztřídí je do lookup map
     * podle částí a položek. Inicializuje {@link #bindingPartLookup},
     * {@link #bindingItemsByPart} a {@link #partsWithoutBinding}.
     */
    protected void readBindingItems(ApBinding binding, ApAccessPoint accessPoint) {
        List<ApBindingItem> bindingItems = this.externalSystemService.getBindingItems(binding);

        Map<Integer, ApBindingItem> partIdBindingMap = new HashMap<>();
        bindingPartLookup = new HashMap<>();
        bindingItemsByPart = new HashMap<>();

        partsWithoutBinding = partService.findPartsByAccessPoint(accessPoint);

        for (ApBindingItem bindingItem : bindingItems) {
            if (bindingItem.getPart() != null) {
                bindingPartLookup.put(bindingItem.getValue(), bindingItem);
                partIdBindingMap.put(bindingItem.getPart().getPartId(), bindingItem);
                partsWithoutBinding.remove(bindingItem.getPart());
            } else if (bindingItem.getItem() != null) {
                Integer partId = bindingItem.getItem().getPartId();

                Map<String, ApBindingItem> bindingItemLookup = bindingItemsByPart.computeIfAbsent(partId, id -> new HashMap<>());
                bindingItemLookup.put(bindingItem.getValue(), bindingItem);
            } else {
                throw new IllegalStateException();
            }
        }

        // safety check
        // binded item should belong to some part in lookup
        for (Integer partId : bindingItemsByPart.keySet()) {
            ApBindingItem parentBinding = partIdBindingMap.get(partId);
            if (parentBinding == null) {
                log.error("Item with binding, but part is not binded, partId: {}", partId);
                throw new SystemException("Item with binding, but part is not binded",
                        BaseCode.DB_INTEGRITY_PROBLEM)
                                .set("partId", partId);
            }
        }
    }

    /**
     * Smaže části, které zůstaly v {@link #bindingPartLookup} (tj. nebyly
     * spárovány během synchronizace), a kaskádově jejich podřízené části
     * a položky.
     *
     * @param syncQueue {@code true} = synchronizace ze sync fronty (chyba na
     *                  podřízených částech vyhodí výjimku);
     *                  {@code false} = synchronizace z UI (podřízené části se mažou)
     */
    protected void deletePartsInLookup(ApChange apChange, ApAccessPoint accessPoint, boolean syncQueue) {
        if (bindingPartLookup.isEmpty()) {
            return;
        }

        Collection<ApBindingItem> partsBinding = bindingPartLookup.values();

        // získání seznamu podřízených ApPart
        List<ApPart> parts = partService.findPartsByAccessPoint(accessPoint);
        List<ApPart> subParts = parts.stream().filter(p -> p.getParentPartId() != null).collect(Collectors.toList());

        List<ApPart> partList = new ArrayList<>();
        for (ApBindingItem partBinding : partsBinding) {
            ApPart part = partBinding.getPart();
            partList.add(part);
            partBinding.setDeleteChange(apChange);
            log.debug("Deleting part binding, bindingItemId: {}, partId: {}", partBinding.getBindingItemId(), part.getPartId());
        }
        bindingItemRepository.saveAll(partsBinding);
        bindingItemRepository.flush();

        // získání seznamu ID, která odstraníme
        Set<Integer> deletedPartIds = partList.stream().map(p -> p.getPartId()).collect(Collectors.toSet());

        for (ApPart subPart : subParts) {
            if (subPart.getParentPartId() != null
                    && deletedPartIds.contains(subPart.getParentPartId())
                    && !deletedPartIds.contains(subPart.getPartId())) {
                if (syncQueue) {
                    log.error("Removed part has subordinate part(s), accessPointId: {}, partId: {}",
                              accessPoint.getAccessPointId(),
                              subPart.getParentPartId());
                    throw new BusinessException("Removed part has subordinate part(s), accessPointId: " +
                              accessPoint.getAccessPointId() + ", partId: " + subPart.getParentPartId(), BaseCode.EXPORT_FAILED)
                        .set("accessPointId", accessPoint.getAccessPointId())
                        .set("partId", subPart.getParentPartId());
                } else {
                    // pokud pochází z uživatelského rozhraní - musi odstranit i subPart
                    partList.add(subPart);
                }
            }
        }

        // clear lookup
        bindingPartLookup.clear();

        List<ApItem> items = accessPointItemService.findItemsByParts(partList);
        deleteItems(items, apChange);

        partService.deleteParts(partList, apChange);
    }

    protected void deleteItems(List<ApItem> items, ApChange apChange) {
        // delete items in parts
        DeletedItems deletedItems = accessPointItemService.deleteItems(items, apChange);

        // delete bindings from bindingItemsByPart
        for (ApBindingItem bindingItem : deletedItems.getBindings()) {
            for (Integer partId : bindingItemsByPart.keySet()) {
                Map<String, ApBindingItem> bindingItemsPart = bindingItemsByPart.get(partId);
                bindingItemsPart.remove(bindingItem.getValue());
            }
        }
    }

    protected void deleteBindedItems(ApPart part, List<ApBindingItem> bindingItemsInPart, ApChange apChange) {
        if (CollectionUtils.isEmpty(bindingItemsInPart)) {
            return;
        }
        Map<String, ApBindingItem> bindingItemLookup = bindingItemsByPart.get(part.getPartId());
        Objects.requireNonNull(bindingItemLookup);
        for (ApBindingItem bindingItem : bindingItemsInPart) {
            bindingItemLookup.remove(bindingItem.getValue());
        }

        accessPointItemService.deleteBindnedItems(bindingItemsInPart, apChange);
    }

    protected boolean compareItems(final List<ReceivedItem> itemsXml, final List<ApItem> items) {
        if (itemsXml.size() != items.size()) {
            return false;
        }

        for (ReceivedItem item : itemsXml) {
            if (!item.contains(items)) {
                return false;
            }
        }
        return true;
    }

    protected static boolean compareItemSpec(RulItemSpec itemSpec, XmlCodeAdapter itemSpecCode) {
        if (itemSpec == null) {
            return itemSpecCode == null;
        }
        return itemSpec.getCode().equals(itemSpecCode.getValue());
    }

    /**
     * Porovná typ a specifikaci ApItem proti tomu, co přišlo z XML.
     */
    protected boolean matchItemType(ApItem existing, XmlCodeAdapter type, XmlCodeAdapter spec) {
        RulItemType itemType = procCtx.getStaticDataProvider().getItemType(type.getValue());
        if (!Objects.equals(itemType.getItemTypeId(), existing.getItemTypeId())) {
            return false;
        }
        if (existing.getItemSpecId() == null && (spec == null || spec.getValue() == null)) {
            return true;
        }
        if (spec == null || spec.getValue() == null) {
            return false;
        }
        RulItemSpec itemSpec = procCtx.getStaticDataProvider().getItemSpec(spec.getValue());
        if (itemSpec == null) {
            return false;
        }
        return Objects.equals(itemSpec.getItemSpecId(), existing.getItemSpecId());
    }

    /**
     * Porovná hodnotu UnitDate položky.
     *
     * Pozn.: dříve byla v cz.tacr.elza.cam.v2.EntityDBDispatcher.compareUnitDate
     * chyba — porovnávalo se {@code dataUnitdate.getValueFrom()} proti
     * {@code itemUnitDate.getFormat()} místo {@code itemUnitDate.getFrom()}.
     * Sjednocená verze používá správnou logiku z v1.
     */
    protected boolean compareUnitDate(ApItem iud, ArrDataUnitdate dataUnitdate, XmlUnitDateItemAdapter item) {
        if (!iud.getItemType().getCode().equals(item.getType().getValue()) ||
                !compareItemSpec(iud.getItemSpec(), item.getSpec())) {
            return false;
        }
        if (!dataUnitdate.getValueFrom().equals(item.getValueFrom().trim()) ||
                !dataUnitdate.getValueTo().equals(item.getValueTo().trim()) ||
                !dataUnitdate.getFormat().equals(item.getFormat())) {
            return false;
        }
        Boolean fromEstimated = item.isFromEstimate() == null ? Boolean.FALSE : item.isFromEstimate();
        Boolean toEstimated = item.isToEstimate() == null ? Boolean.FALSE : item.isToEstimate();
        return dataUnitdate.getValueFromEstimated().equals(fromEstimated)
                && dataUnitdate.getValueToEstimated().equals(toEstimated);
    }

    /**
     * Najde položky, které jsou nové, změněné nebo beze změny vůči stávajícímu stavu.
     *
     * Volající předává surové ItemXxxXml objekty; metoda je sama zabalí do
     * verzově neutrálních adapterů přes {@link #adapterFactory}.
     */
    public ItemUpdates findNewOrChangedItems(ApPart part, List<Object> items) {
        Map<String, ApBindingItem> bindingItemLookup = bindingItemsByPart.getOrDefault(part.getPartId(), Collections.emptyMap());

        ItemUpdates result = new ItemUpdates();
        for (Object item : items) {
            XmlItemAdapter adapter = adapterFactory.wrapItem(item);
            if (adapter instanceof XmlBinaryItemAdapter) {
                prepareBinaryUpdate(bindingItemLookup, (XmlBinaryItemAdapter) adapter, result);
            } else if (adapter instanceof XmlBooleanItemAdapter) {
                prepareBooleanUpdate(bindingItemLookup, (XmlBooleanItemAdapter) adapter, result);
            } else if (adapter instanceof XmlEntityRefItemAdapter) {
                prepareEntityRefUpdate(bindingItemLookup, (XmlEntityRefItemAdapter) adapter, result);
            } else if (adapter instanceof XmlEnumItemAdapter) {
                prepareEnumUpdate(bindingItemLookup, (XmlEnumItemAdapter) adapter, result);
            } else if (adapter instanceof XmlIntegerItemAdapter) {
                prepareIntegerUpdate(bindingItemLookup, (XmlIntegerItemAdapter) adapter, result);
            } else if (adapter instanceof XmlLinkItemAdapter) {
                prepareLinkUpdate(bindingItemLookup, (XmlLinkItemAdapter) adapter, result);
            } else if (adapter instanceof XmlStringItemAdapter) {
                prepareStringUpdate(bindingItemLookup, (XmlStringItemAdapter) adapter, result);
            } else if (adapter instanceof XmlUnitDateItemAdapter) {
                prepareUnitDateUpdate(bindingItemLookup, (XmlUnitDateItemAdapter) adapter, result);
            } else {
                throw new IllegalArgumentException("Invalid item type");
            }
        }
        return result;
    }

    private void prepareBinaryUpdate(Map<String, ApBindingItem> lookup, XmlBinaryItemAdapter item, ItemUpdates result) {
        ApBindingItem bindingItem = lookup.get(item.getUuid());
        if (bindingItem == null) {
            result.addNewItem(item.getRaw());
            return;
        }
        ApItem existing = bindingItem.getItem();
        boolean processed = false;
        if (matchItemType(existing, item.getType(), item.getSpec())) {
            ArrDataCoordinates dataCoordinates = HibernateUtils.unproxy(existing.getData());
            Geometry value = dataCoordinates.getValue();
            Geometry xmlValue = GeometryConvertor.convertWkb(item.getBinaryValue());
            try {
                if (xmlValue.equals(value)) {
                    result.addNotChanged(bindingItem);
                    processed = true;
                }
            } catch (Exception e) {
                log.error("Failed to compare received coordinates. Item will be updated as changed.", e);
            }
        }
        if (!processed) {
            result.addChanged(bindingItem, item.getRaw());
        }
    }

    private void prepareBooleanUpdate(Map<String, ApBindingItem> lookup, XmlBooleanItemAdapter item, ItemUpdates result) {
        ApBindingItem bindingItem = lookup.get(item.getUuid());
        if (bindingItem == null) {
            result.addNewItem(item.getRaw());
            return;
        }
        ApItem existing = bindingItem.getItem();
        ArrDataBit dataBit = HibernateUtils.unproxy(existing.getData());
        if (existing.getItemType().getCode().equals(item.getType().getValue())
                && compareItemSpec(existing.getItemSpec(), item.getSpec())
                && dataBit.isBitValue().equals(item.getBoolValue())) {
            result.addNotChanged(bindingItem);
        } else {
            result.addChanged(bindingItem, item.getRaw());
        }
    }

    private void prepareEntityRefUpdate(Map<String, ApBindingItem> lookup, XmlEntityRefItemAdapter item, ItemUpdates result) {
        ApBindingItem bindingItem = lookup.get(item.getUuid());
        if (bindingItem == null) {
            result.addNewItem(item.getRaw());
            return;
        }
        ApItem existing = bindingItem.getItem();
        ArrDataRecordRef dataRecordRef = HibernateUtils.unproxy(existing.getData());
        String entityRefId = item.getRefIdOrUuid();
        ApAccessPoint ap = dataRecordRef.getRecord();
        ApBinding binding;
        if (ap != null) {
            ApBindingState bs = externalSystemService.findByAccessPointAndExternalSystem(ap, procCtx.getApExternalSystem());
            binding = bs == null ? null : bs.getBinding();
        } else {
            binding = dataRecordRef.getBinding();
        }
        if (existing.getItemType().getCode().equals(item.getType().getValue())
                && compareItemSpec(existing.getItemSpec(), item.getSpec())
                && binding != null
                && binding.getValue().equals(entityRefId)) {
            result.addNotChanged(bindingItem);
        } else {
            result.addChanged(bindingItem, item.getRaw());
        }
    }

    private void prepareEnumUpdate(Map<String, ApBindingItem> lookup, XmlEnumItemAdapter item, ItemUpdates result) {
        ApBindingItem bindingItem = lookup.get(item.getUuid());
        if (bindingItem == null) {
            result.addNewItem(item.getRaw());
            return;
        }
        ApItem existing = bindingItem.getItem();
        if (existing.getItemType().getCode().equals(item.getType().getValue())
                && compareItemSpec(existing.getItemSpec(), item.getSpec())) {
            result.addNotChanged(bindingItem);
        } else {
            result.addChanged(bindingItem, item.getRaw());
        }
    }

    private void prepareIntegerUpdate(Map<String, ApBindingItem> lookup, XmlIntegerItemAdapter item, ItemUpdates result) {
        ApBindingItem bindingItem = lookup.get(item.getUuid());
        if (bindingItem == null) {
            result.addNewItem(item.getRaw());
            return;
        }
        ApItem existing = bindingItem.getItem();
        ArrDataInteger dataInteger = HibernateUtils.unproxy(existing.getData());
        if (existing.getItemType().getCode().equals(item.getType().getValue())
                && compareItemSpec(existing.getItemSpec(), item.getSpec())
                && dataInteger.getIntegerValue().equals(item.getIntValue())) {
            result.addNotChanged(bindingItem);
        } else {
            result.addChanged(bindingItem, item.getRaw());
        }
    }

    /**
     * Pozn.: dříve mělo CAM v2 přísné porovnání popisku ({@code Objects.equals}),
     * zatímco v1 normalizovalo prázdný řetězec na {@code null}. Sjednocená
     * verze používá tolerantní v1 logiku, aby nedocházelo k falešným
     * "změnám" při prázdných hodnotách.
     */
    private void prepareLinkUpdate(Map<String, ApBindingItem> lookup, XmlLinkItemAdapter item, ItemUpdates result) {
        ApBindingItem bindingItem = lookup.get(item.getUuid());
        if (bindingItem == null) {
            result.addNewItem(item.getRaw());
            return;
        }
        ApItem existing = bindingItem.getItem();
        ArrDataUriRef dataUriRef = HibernateUtils.unproxy(existing.getData());
        String currDescription = dataUriRef.getDescription();
        if (StringUtils.isEmpty(currDescription)) {
            currDescription = null;
        }
        String otherDescription = item.getDescription();
        if (StringUtils.isEmpty(otherDescription)) {
            otherDescription = null;
        }
        if (existing.getItemType().getCode().equals(item.getType().getValue())
                && compareItemSpec(existing.getItemSpec(), item.getSpec())
                && dataUriRef.getUriRefValue().equals(item.getUrl())
                && Objects.equals(currDescription, otherDescription)) {
            result.addNotChanged(bindingItem);
        } else {
            result.addChanged(bindingItem, item.getRaw());
        }
    }

    private void prepareStringUpdate(Map<String, ApBindingItem> lookup, XmlStringItemAdapter item, ItemUpdates result) {
        ApBindingItem bindingItem = lookup.get(item.getUuid());
        if (bindingItem == null) {
            result.addNewItem(item.getRaw());
            return;
        }
        ApItem existing = bindingItem.getItem();
        String value;
        switch (DataType.fromCode(existing.getItemType().getDataType().getCode())) {
            case STRING:
                value = ((ArrDataString) HibernateUtils.unproxy(existing.getData())).getStringValue();
                break;
            case TEXT:
                value = ((ArrDataText) HibernateUtils.unproxy(existing.getData())).getTextValue();
                break;
            case COORDINATES:
                value = GeometryConvertor.convert(((ArrDataCoordinates) HibernateUtils.unproxy(existing.getData())).getValue());
                break;
            default:
                throw new IllegalStateException("Neznámý datový typ " + existing.getItemType().getDataType().getCode());
        }
        if (existing.getItemType().getCode().equals(item.getType().getValue())
                && compareItemSpec(existing.getItemSpec(), item.getSpec())
                && value.equals(item.getStringValue())) {
            result.addNotChanged(bindingItem);
        } else {
            result.addChanged(bindingItem, item.getRaw());
        }
    }

    private void prepareUnitDateUpdate(Map<String, ApBindingItem> lookup, XmlUnitDateItemAdapter item, ItemUpdates result) {
        ApBindingItem bindingItem = lookup.get(item.getUuid());
        if (bindingItem == null) {
            result.addNewItem(item.getRaw());
            return;
        }
        ApItem existing = bindingItem.getItem();
        ArrDataUnitdate dataUnitdate = HibernateUtils.unproxy(existing.getData());
        if (compareUnitDate(existing, dataUnitdate, item)) {
            result.addNotChanged(bindingItem);
        } else {
            result.addChanged(bindingItem, item.getRaw());
        }
    }

    /**
     * Vytvoří seznam {@link ApItem}s pro danou část z předaných XML položek.
     */
    public List<ApItem> createItems(final List<Object> createItems,
                                    final ApPart apPart, final ApChange change,
                                    final ApBinding binding,
                                    final List<ReferencedEntities> dataRefList) {
        List<ApItem> itemsCreated = new ArrayList<>(createItems.size());
        Map<Integer, List<ApItem>> typeIdItemsMap = new HashMap<>();

        for (Object createItem : createItems) {
            itemsCreated.add(createItem(apPart, createItem, change, typeIdItemsMap, binding, dataRefList));
        }
        return itemsCreated;
    }

    private ApItem createItem(final ApPart part,
                              final Object createItem,
                              final ApChange change,
                              final Map<Integer, List<ApItem>> typeIdItemsMap,
                              final ApBinding binding,
                              final List<ReferencedEntities> dataRefList) {
        ReceivedItem receivedItem = createReceivedItem(createItem, dataRefList);
        RulItemType itemType = receivedItem.getItemType();
        List<ApItem> existsItems = typeIdItemsMap.computeIfAbsent(itemType.getItemTypeId(), k -> new ArrayList<>());

        ApItem itemCreated = accessPointItemService.createItemWithSave(part, receivedItem.getData(),
                itemType, receivedItem.getItemSpec(), change, existsItems, binding, receivedItem.getUuid());
        existsItems.add(itemCreated);
        return itemCreated;
    }

    /**
     * Z XML položky vytvoří {@link ReceivedItem} včetně cílového ArrData.
     *
     * Pozn.: dříve nesla metoda překlep "createReveivedItem" — opraveno.
     * Sjednoceno chování v1/v2:
     * - ItemLink: popisek se nastavuje jen pokud není prázdný (dříve v2 mohl
     *   pádit na NPE, pokud chybělo {@code getName()}).
     * - ItemUnitDate: prázdné příznaky FromEstimate/ToEstimate se převedou
     *   na {@code false} (dříve v2 ukládal {@code null}).
     */
    protected ReceivedItem createReceivedItem(Object createItem, List<ReferencedEntities> dataRefList) {
        XmlItemAdapter adapter = adapterFactory.wrapItem(createItem);
        StaticDataProvider sdp = procCtx.getStaticDataProvider();

        RulItemType itemType = sdp.getItemType(adapter.getType().getValue());
        RulItemSpec itemSpec = adapter.getSpec() == null ? null : sdp.getItemSpec(adapter.getSpec().getValue());
        String uuid = adapter.getUuid();
        ArrData data = buildArrData(adapter, itemType, dataRefList);

        validateSpecPresence(itemType, itemSpec);
        return new ReceivedItem(itemType, itemSpec, uuid, data);
    }

    private ArrData buildArrData(XmlItemAdapter adapter, RulItemType itemType,
                                 List<ReferencedEntities> dataRefList) {
        if (adapter instanceof XmlBinaryItemAdapter) {
            ArrDataCoordinates dc = new ArrDataCoordinates();
            dc.setValue(GeometryConvertor.convertWkb(((XmlBinaryItemAdapter) adapter).getBinaryValue()));
            dc.setDataType(DataType.COORDINATES.getEntity());
            return dc;
        }
        if (adapter instanceof XmlBooleanItemAdapter) {
            ArrDataBit db = new ArrDataBit();
            db.setBitValue(((XmlBooleanItemAdapter) adapter).getBoolValue());
            db.setDataType(DataType.BIT.getEntity());
            return db;
        }
        if (adapter instanceof XmlEntityRefItemAdapter) {
            ArrDataRecordRef drr = new ArrDataRecordRef();
            drr.setDataType(DataType.RECORD_REF.getEntity());
            dataRefList.add(new ReferencedEntities(drr, ((XmlEntityRefItemAdapter) adapter).getRefIdOrUuid()));
            return drr;
        }
        if (adapter instanceof XmlEnumItemAdapter) {
            ArrDataNull dn = new ArrDataNull();
            dn.setDataType(DataType.ENUM.getEntity());
            return dn;
        }
        if (adapter instanceof XmlIntegerItemAdapter) {
            ArrDataInteger di = new ArrDataInteger();
            di.setIntegerValue(((XmlIntegerItemAdapter) adapter).getIntValue());
            di.setDataType(DataType.INT.getEntity());
            return di;
        }
        if (adapter instanceof XmlLinkItemAdapter) {
            return buildLinkData((XmlLinkItemAdapter) adapter);
        }
        if (adapter instanceof XmlStringItemAdapter) {
            return buildStringLikeData((XmlStringItemAdapter) adapter, itemType);
        }
        if (adapter instanceof XmlUnitDateItemAdapter) {
            return buildUnitDateData((XmlUnitDateItemAdapter) adapter);
        }
        throw new IllegalArgumentException("Invalid item type");
    }

    private ArrDataUriRef buildLinkData(XmlLinkItemAdapter item) {
        ArrDataUriRef d = new ArrDataUriRef();
        String url = item.getUrl();
        d.setUriRefValue(url);
        String description = item.getDescription();
        if (StringUtils.isNotEmpty(description)) {
            d.setDescription(description);
        }
        String schema = ArrDataUriRef.createSchema(url);
        if (schema == null) {
            log.info("Schema URL: {} is null, will be set {}", url, SCHEMA_UNKNOWN);
            schema = SCHEMA_UNKNOWN;
        }
        d.setSchema(schema);
        d.setArrNode(null);
        d.setDataType(DataType.URI_REF.getEntity());
        return d;
    }

    private ArrData buildStringLikeData(XmlStringItemAdapter item, RulItemType itemType) {
        RulDataType dataType = itemType.getDataType();
        String code = dataType.getCode();
        DataType dt = DataType.fromCode(code);
        if (dt == null) {
            throw new IllegalStateException("Neznámý datový typ " + code);
        }
        switch (dt) {
            case STRING:
                ArrDataString ds = new ArrDataString();
                ds.setStringValue(item.getStringValue());
                if (StringUtils.isEmpty(ds.getStringValue())) {
                    log.error("ItemString is empty, uuid: {}, itemType: {}, itemSpec: {}",
                            item.getUuid(),
                            item.getType() != null ? item.getType().getValue() : null,
                            item.getSpec() != null ? item.getSpec().getValue() : null);
                    // Set some default value
                    ds.setStringValue("N/A");
                }
                ds.setDataType(DataType.STRING.getEntity());
                return ds;
            case TEXT:
                ArrDataText dt2 = new ArrDataText();
                dt2.setTextValue(item.getStringValue());
                if (StringUtils.isEmpty(dt2.getTextValue())) {
                    log.error("ItemText is empty, uuid: {}, itemType: {}, itemSpec: {}",
                            item.getUuid(),
                            item.getType() != null ? item.getType().getValue() : null,
                            item.getSpec() != null ? item.getSpec().getValue() : null);
                    // Set some default value
                    dt2.setTextValue("N/A");
                }
                dt2.setDataType(DataType.TEXT.getEntity());
                return dt2;
            case COORDINATES:
                ArrDataCoordinates dc = new ArrDataCoordinates();
                dc.setValue(GeometryConvertor.convert(item.getStringValue()));
                dc.setDataType(DataType.COORDINATES.getEntity());
                return dc;
            default:
                throw new IllegalStateException("Nepodporovaný datový typ uložen jako řetězec: " + code +
                        ", itemType:" + item.getType().getValue());
        }
    }

    private ArrDataUnitdate buildUnitDateData(XmlUnitDateItemAdapter item) {
        ArrDataUnitdate d = new ArrDataUnitdate();
        String from = item.getValueFrom();
        String to = item.getValueTo();
        d.setValueFrom(from == null ? null : from.trim());
        d.setValueTo(to == null ? null : to.trim());
        d.setFormat(item.getFormat());
        Boolean fromEst = item.isFromEstimate();
        Boolean toEst = item.isToEstimate();
        d.setValueFromEstimated(fromEst == null ? Boolean.FALSE : fromEst);
        d.setValueToEstimated(toEst == null ? Boolean.FALSE : toEst);
        if (from != null) {
            d.setNormalizedFrom(CalendarConverter.toSeconds(LocalDateTime.parse(from.trim(), DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        } else {
            d.setNormalizedFrom(Long.MIN_VALUE);
        }
        if (to != null) {
            d.setNormalizedTo(CalendarConverter.toSeconds(LocalDateTime.parse(to.trim(), DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        } else {
            d.setNormalizedTo(Long.MAX_VALUE);
        }
        d.setDataType(DataType.UNITDATE.getEntity());
        return d;
    }

    private void validateSpecPresence(RulItemType itemType, RulItemSpec itemSpec) {
        Boolean useSpec = itemType.getUseSpecification();
        if (useSpec != null && useSpec) {
            if (itemSpec == null) {
                throw new BusinessException("Received item without specification, itemType: " + itemType.getName(),
                        BaseCode.PROPERTY_IS_INVALID)
                                .set("itemType", itemType.getCode())
                                .set("itemTypeName", itemType.getName());
            }
        } else if (itemSpec != null) {
            throw new BusinessException("Received item with unexpected specification, itemType: " + itemType.getName(),
                    BaseCode.PROPERTY_IS_INVALID)
                            .set("itemType", itemType.getCode())
                            .set("itemTypeName", itemType.getName());
        }
    }
}
