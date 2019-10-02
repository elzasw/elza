package cz.tacr.elza.print;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApDescription;
import cz.tacr.elza.domain.ApExternalId;
import cz.tacr.elza.domain.ApName;
import cz.tacr.elza.print.ap.ExternalId;
import cz.tacr.elza.print.ap.Name;
import cz.tacr.elza.repository.ApDescriptionRepository;
import cz.tacr.elza.repository.ApExternalIdRepository;
import cz.tacr.elza.repository.ApNameRepository;
import cz.tacr.elza.repository.ApStateRepository;

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

    private final ApDescriptionRepository descRepository;

    private final ApNameRepository nameRepository;

    private final ApExternalIdRepository eidRepository;

    private String desc;

    private List<Name> names;

    private List<ExternalId> eids;

    public Record(ApAccessPoint ap,
                  RecordType type,
                  StaticDataProvider staticData,
                  ApStateRepository stateRepository,
                  ApDescriptionRepository descRepository,
                  ApNameRepository nameRepository,
                  ApExternalIdRepository eidRepository) {
        this.ap = ap;
        this.type = type;
        this.staticData = staticData;
        this.stateRepository = stateRepository;
        this.descRepository = descRepository;
        this.nameRepository = nameRepository;
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
        this.descRepository = src.descRepository;
        this.nameRepository = src.nameRepository;
        this.eidRepository = src.eidRepository;
        this.desc = src.desc;
        this.names = src.names;
        this.eids = src.eids;
    }

    public int getId() {
        return ap.getAccessPointId().intValue();
    }

    public RecordType getType() {
        return type;
    }

    public String getDesc() {
        if (desc == null) {
            ApDescription apDesc = descRepository.findByAccessPoint(ap);
            if (apDesc == null) {
                desc = StringUtils.EMPTY;
            } else {
                desc = apDesc.getDescription();
            }
        }
        return desc;
    }

    public Name getPrefName() {
        List<Name> names = getNames();
        Name prefName = names.get(0);
        Validate.isTrue(prefName.isPreferred());
        return prefName;
    }

    public List<Name> getNames() {
        if (names == null) {
            List<ApName> apNames = nameRepository.findByAccessPoint(ap);
            Iterator<ApName> it = apNames.iterator();
            names = new ArrayList<>(apNames.size());
            // add preferred name
            Name name = Name.newInstance(it.next(), staticData);
            Validate.isTrue(name.isPreferred());
            names.add(name);
            // add other names
            while (it.hasNext()) {
                name = Name.newInstance(it.next(), staticData);
                Validate.isTrue(!name.isPreferred());
                names.add(name);
            }
            // make names read-only
            names = Collections.unmodifiableList(names);
        }
        return names;
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
