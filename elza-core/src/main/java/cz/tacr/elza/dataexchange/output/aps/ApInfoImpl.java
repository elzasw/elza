package cz.tacr.elza.dataexchange.output.aps;

import java.util.Collection;

import cz.tacr.elza.dataexchange.output.writer.ApInfo;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApDescription;
import cz.tacr.elza.domain.ApName;

public class ApInfoImpl extends BaseApInfoImpl implements ApInfo {

    private Collection<ApName> names;

    private ApDescription desc;

    private boolean partyAp;
    
    public ApInfoImpl(ApAccessPoint ap) {
        super(ap);
    }

    @Override
    public Collection<ApName> getNames() {
        return names;
    }

    public void setNames(Collection<ApName> names) {
        this.names = names;
    }

    @Override
    public ApDescription getDesc() {
        return desc;
    }

    public void setDesc(ApDescription desc) {
        this.desc = desc;
    }

    public boolean isPartyAp() {
        return partyAp;
    }

    public void setPartyAp(boolean partyAp) {
        this.partyAp = partyAp;
    }
}
