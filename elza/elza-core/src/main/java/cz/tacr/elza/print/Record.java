package cz.tacr.elza.print;

import static cz.tacr.elza.groovy.GroovyResult.DISPLAY_NAME;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;
import org.springframework.util.CollectionUtils;

import cz.tacr.elza.common.ObjectListIterator;
import cz.tacr.elza.controller.factory.ApFactory;
import cz.tacr.elza.core.ElzaLocale;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApIndex;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.print.ap.ExternalId;
import cz.tacr.elza.print.item.Item;
import cz.tacr.elza.print.item.convertors.OutputItemConvertor;
import cz.tacr.elza.print.part.Part;
import cz.tacr.elza.repository.ApBindingRepository;
import cz.tacr.elza.repository.ApBindingStateRepository;
import cz.tacr.elza.repository.ApIndexRepository;
import cz.tacr.elza.repository.ApItemRepository;
import cz.tacr.elza.repository.ApPartRepository;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.service.cache.AccessPointCacheService;
import cz.tacr.elza.service.cache.CachedAccessPoint;
import cz.tacr.elza.service.cache.CachedPart;

/**
 * One record from registry
 * <p>
 * Each record has its type, record name and characteristics
 */
public class Record {

    private final ApAccessPoint ap;

    private final RecordType type;

    private final StaticDataProvider staticData;

    private final ApStateRepository stateRepository;

    private final ApBindingRepository bindingRepository;

    private final ApBindingStateRepository bindingStateRepository;

    private final ApPartRepository partRepository;

    private final ApItemRepository itemRepository;

    private final ApIndexRepository indexRepository;

    private final AccessPointCacheService accessPointCacheService;
    
    private final ElzaLocale elzaLocale;

    private List<ExternalId> eids;

    private List<Part> parts;

    private Part preferredPart;

    private OutputItemConvertor outputItemConvertor;

    public Record(final ApAccessPoint ap,
                  final RecordType type,
                  final StaticDataProvider staticData,
                  final ApStateRepository stateRepository,
                  final ApBindingRepository bindingRepository,
                  final ApPartRepository partRepository,
                  final ApItemRepository itemRepository,
                  final ApBindingStateRepository bindingStateRepository,
                  final ApIndexRepository indexRepository,
                  final OutputItemConvertor outputItemConvertor,
                  final AccessPointCacheService accessPointCacheService,
                  final ElzaLocale elzaLocale) {
        this.ap = ap;
        this.type = type;
        this.staticData = staticData;
        this.stateRepository = stateRepository;
        this.bindingRepository = bindingRepository;
        this.partRepository = partRepository;
        this.itemRepository = itemRepository;
        this.bindingStateRepository = bindingStateRepository;
        this.indexRepository = indexRepository;
        this.outputItemConvertor = outputItemConvertor;
        this.accessPointCacheService = accessPointCacheService;
        this.elzaLocale = elzaLocale;
    }

    /**
     * Copy constructor
     */
    protected Record(Record src) {
        this.ap = src.ap;
        this.type = src.type;
        this.staticData = src.staticData;
        this.stateRepository = src.stateRepository;
        this.bindingRepository = src.bindingRepository;
        this.partRepository = src.partRepository;
        this.itemRepository = src.itemRepository;
        this.eids = src.eids;
        this.preferredPart = src.preferredPart;
        this.parts = src.parts;
        this.bindingStateRepository = src.bindingStateRepository;
        this.indexRepository = src.indexRepository;
        this.outputItemConvertor = src.outputItemConvertor;
        this.accessPointCacheService = src.accessPointCacheService;
        this.elzaLocale = src.elzaLocale;
    }

    private void loadData(List<CachedPart> cachedParts) {

        // sorting parts
        Map<CachedPart, String> sortValues = cachedParts.stream().collect(Collectors.toMap(p -> p, p -> ApFactory.getSortName(p)));
        cachedParts.sort((p1, p2) -> {
            String s1 = sortValues.get(p1);
            String s2 = sortValues.get(p2);
            if (s1 == null || s2 == null) {
                return 0;
            }
            return elzaLocale.getCollator().compare(s1, s2);
        });

        List<Part> subParts = new ArrayList<>();
        Map<Integer, Part> partIdMap = new HashMap<>();

        // prepare parts
        parts = new ArrayList<>(cachedParts.size());
        for (CachedPart cachedPart : cachedParts) {
            Part part = createPart(cachedPart);

            partIdMap.put(cachedPart.getPartId(), part);

            if (part.getParentPartId() != null) {
                subParts.add(part);
            } else {
                // set preferred part
                if (ap.getPreferredPartId().equals(cachedPart.getPartId())) {
                    preferredPart = part;
                }
                parts.add(part);
            }
        }
        parts = Collections.unmodifiableList(parts);

        Map<Part, List<Part>> subPartMap = new HashMap<>();
        // process sub parts
        for (Part subPart : subParts) {
            Part parent = partIdMap.get(subPart.getPartId());
            Validate.notNull(parent, "Parent part not found, partId = %i", subPart.getPartId());
            List<Part> partList = subPartMap.computeIfAbsent(parent, p -> new ArrayList<>());
            partList.add(subPart);
        }
        // store subparts
        subPartMap.forEach((part, list) -> part.setParts(list));
    }

    private Part createPart(CachedPart cachedPart) { 
        String index = ApFactory.findIndexValue(cachedPart.getIndices(), DISPLAY_NAME);
        Part part = new Part(cachedPart, ap.getAccessPointId(), staticData, index);            
        // process items
        List<ApItem> apItems = cachedPart.getItems();
        if (!CollectionUtils.isEmpty(apItems)) {
            for (ApItem apItem : apItems) {
                Item item = outputItemConvertor.convert(apItem);
                part.addItem(item);
            }
        }
        return part;
    }

    private void loadParts() {
        if (parts != null) {
            return;
        }

        CachedAccessPoint cachedAccessPoint = accessPointCacheService.findCachedAccessPoint(ap.getAccessPointId());
        if (cachedAccessPoint == null) {
            throw new IllegalStateException("ApAccessPoint not found in CachedAccessPoint, apAccessPointId=" + ap.getAccessPointId());
        }
        loadData(cachedAccessPoint.getParts());
    }

    public Integer getId() {
        return ap.getAccessPointId();
    }

    public String getUuid() {
        return ap.getUuid();
    }

    public RecordType getType() {
        return type;
    }

    public List<ExternalId> getEids() {
        if (eids == null) {
            List<ApBindingState> apEids = bindingStateRepository.findByAccessPoint(ap);
            eids = new ArrayList<>(apEids.size());
            for (ApBindingState apEid : apEids) {
                ExternalId eid = ExternalId.newInstance(apEid.getBinding(), staticData);
                eids.add(eid);
            }
            // make external ids read-only
            eids = Collections.unmodifiableList(eids);
        }
        return eids;
    }

    /**
     * Return string with formatted list of external ids
     * <p>
     * Format of the result is <type1>: <value1>, <type2>: <value2>...
     */
    public String getFormattedEids() {
        List<ExternalId> eids = getEids();
        if (eids == null) {
            return "";
        } else {
            return eids.stream().map(eid -> {
                return eid.getType().getName() + ": " + eid.getValue();
            }).collect(Collectors.joining(", "));
        }
    }

    public List<Part> getParts() {
        loadParts();

        return parts;
    }

    /**
     * Return single part
     * 
     * @param partTypeCode
     * @return
     */
    public Part getPart(final String partTypeCode) {
        final List<Part> parts = getParts(partTypeCode);
        if (parts.size() == 0) {
            return null;
        }
        if (parts.size() > 1) {
            throw new BusinessException("Multiple parts of required type exists.", BaseCode.INVALID_STATE)
                    .set("partTypeCode", partTypeCode)
                    .set("name", preferredPart.getValue())
                    .set("count", parts.size());
        }
        return parts.get(0);
    }

    public List<Part> getParts(final String partTypeCode) {
        return getParts(Collections.singletonList(partTypeCode));
    }

    public List<Part> getParts(final Collection<String> partTypeCodes) {
        Validate.notNull(partTypeCodes);
        loadParts();

        return parts.stream().filter(part -> {
            String partTypeCode = part.getPartType().getCode();
            return partTypeCodes.contains(partTypeCode);
        }).collect(Collectors.toList());
    }

    public List<Item> getItems() {
        loadParts();

        List<Item> itemList = new ArrayList<>();
        for (Part part : parts) {
            itemList.addAll(part.getItems());
        }
        return itemList;
    }

    public List<Item> getItems(Collection<String> itemTypeCodes) {
        Validate.notNull(itemTypeCodes);

        if (parts == null || itemTypeCodes.isEmpty()) {
            return Collections.emptyList();
        }
        List<Item> itemList = new ArrayList<>();
        for (Part part : parts) {
            itemList.addAll(part.getItems().stream().filter(item -> {
                String itemTypeCode = item.getType().getCode();
                return itemTypeCodes.contains(itemTypeCode);
            }).collect(Collectors.toList()));
        }
        return itemList;
    }

    public Part getPreferredPart() {
        loadParts();

        return preferredPart;
    }

    public void setPreferredPart(Part preferredPart) {
        this.preferredPart = preferredPart;
    }

    public void setParts(List<Part> parts) {
        this.parts = parts;
    }
}
