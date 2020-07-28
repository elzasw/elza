package cz.tacr.elza.print;

import cz.tacr.elza.core.data.PartType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.print.ap.ExternalId;
import cz.tacr.elza.print.item.Item;
import cz.tacr.elza.print.part.Part;
import cz.tacr.elza.repository.ApBindingRepository;
import cz.tacr.elza.repository.ApBindingStateRepository;
import cz.tacr.elza.repository.ApPartRepository;
import cz.tacr.elza.repository.ApStateRepository;
import org.apache.commons.lang3.Validate;

import java.util.*;
import java.util.stream.Collectors;

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

    private List<ExternalId> eids;

    private List<Part> parts;

    private final Map<Integer, PartType> partTypeIdMap = new HashMap<>();

    private final Map<Integer, Part> partIdMap = new HashMap<>();

    private Part preferredPart;

    //private final

    public Record(ApAccessPoint ap,
                  RecordType type,
                  StaticDataProvider staticData,
                  ApStateRepository stateRepository,
                  ApBindingRepository bindingRepository,
                  ApPartRepository partRepository,
                  ApBindingStateRepository bindingStateRepository) {
        this.ap = ap;
        this.type = type;
        this.staticData = staticData;
        this.stateRepository = stateRepository;
        this.bindingRepository = bindingRepository;
        this.partRepository = partRepository;
        this.bindingStateRepository = bindingStateRepository;
       // this.preferredPart = new Part(partRepository.getOne(ap.getPreferredPart().getPartId()), staticData);
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
        this.eids = src.eids;
        this.preferredPart = src.preferredPart;
        this.bindingStateRepository = src.bindingStateRepository;
    }

    public int getId() {
        return ap.getAccessPointId().intValue();
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

    public Part getPart(Integer partId) {
        Part part = partIdMap.get(partId);
        if (part != null) {
            return part;
        }
        ApPart apPart = partRepository.findById(partId)
                .orElseThrow(() -> new ObjectNotFoundException("Parta neexistuje", BaseCode.ID_NOT_EXIST).setId(partId));
        part = new Part(apPart, staticData);
        partIdMap.put(partId, part);
        return part;
    }

    public List<Part> getParts() {
        if (parts == null) {
            List<ApPart> apParts = partRepository.findValidPartByAccessPoint(ap);
            parts = new ArrayList<>(apParts.size());
            for (ApPart apPart : apParts) {
                Part part = new Part(apPart, staticData);
                parts.add(part);
            }
            parts = Collections.unmodifiableList(parts);
        }
        return parts;
    }

    public List<Part> getParts(final Collection<String> partTypeCodes) {
        Validate.notNull(partTypeCodes);

        if (parts == null || partTypeCodes.isEmpty()) {
            return Collections.emptyList();
        }

        return parts.stream().filter(part -> {
            String partTypeCode = part.getPartType().getCode();
            return partTypeCodes.contains(partTypeCode);
        }).collect(Collectors.toList());
    }

    public List<Item> getItems() {
        if (parts == null) {
            return null;
        }
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

    public PartType getPartTypeById(Integer id) {
        PartType partType = partTypeIdMap.get(id);
        if (partType != null) {
            return partType;
        }

        RulPartType rulPartType = staticData.getPartTypeById(id);
        partType = new PartType(rulPartType);
        partTypeIdMap.put(id, partType);
        return partType;
    }

    public Part getPreferredPart() {
        return preferredPart;
    }

    public void setPreferredPart(Part preferredPart) {
        this.preferredPart = preferredPart;
    }

    public void setParts(List<Part> parts) {
        this.parts = parts;
    }
}
