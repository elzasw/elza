package cz.tacr.elza.print;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApExternalId;
import cz.tacr.elza.print.ap.ExternalId;
import cz.tacr.elza.repository.ApExternalIdRepository;
import cz.tacr.elza.repository.ApStateRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * One record from registry
 *
 * Each record has its type, record name and characteristics
 */
public class Record {

    private final ApAccessPoint ap;

    private final RecordType type;

    private final StaticDataProvider staticData;

    private final ApStateRepository stateRepository;

    private final ApExternalIdRepository eidRepository;

    private List<ExternalId> eids;

    public Record(ApAccessPoint ap,
                  RecordType type,
                  StaticDataProvider staticData,
                  ApStateRepository stateRepository,
                  ApExternalIdRepository eidRepository) {
        this.ap = ap;
        this.type = type;
        this.staticData = staticData;
        this.stateRepository = stateRepository;
        this.eidRepository = eidRepository;
    }

    /**
     * Copy constructor
     */
    protected Record(Record src) {
        this.ap = src.ap;
        this.type = src.type;
        this.staticData = src.staticData;
        this.stateRepository = src.stateRepository;
        this.eidRepository = src.eidRepository;
        this.eids = src.eids;
    }

    public int getId() {
        return ap.getAccessPointId().intValue();
    }

    public RecordType getType() {
        return type;
    }

    public List<ExternalId> getEids() {
        if (eids == null) {
            List<ApExternalId> apEids = eidRepository.findByAccessPoint(ap);
            eids = new ArrayList<>(apEids.size());
            for (ApExternalId apEid : apEids) {
                ExternalId eid = ExternalId.newInstance(apEid, staticData);
                eids.add(eid);
            }
            // make external ids read-only
            eids = Collections.unmodifiableList(eids);
        }
        return eids;
    }

    /**
     * Return string with formatted list of external ids
     *
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
}
