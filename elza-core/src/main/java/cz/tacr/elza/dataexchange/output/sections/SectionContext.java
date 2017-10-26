package cz.tacr.elza.dataexchange.output.sections;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.core.data.RuleSystem;
import cz.tacr.elza.dataexchange.output.context.ExportContext;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;

public class SectionContext {

    private final Set<Integer> packetIds = new HashSet<>();

    private final ArrFundVersion fundVersion;

    private final int rootNodeId;

    private final ExportContext context;

    SectionContext(ArrFundVersion fundVersion, int rootNodeId, ExportContext context) {
        this.fundVersion = Validate.notNull(fundVersion);
        this.rootNodeId = rootNodeId;
        this.context = Validate.notNull(context);
    }

    SectionContext(ArrFundVersion fundVersion, ExportContext context) {
        this(fundVersion, fundVersion.getRootNodeId(), context);
    }

    public int getRootNodeId() {
        return rootNodeId;
    }

    public ExportContext getContext() {
        return context;
    }

    public String getInstitutionCode() {
        return fundVersion.getFund().getInstitution().getInternalCode();
    }

    public String getFundName() {
        return fundVersion.getFund().getName();
    }

    public String getFundInternalCode() {
        return fundVersion.getFund().getInternalCode();
    }

    public String getTimeRange() {
        return fundVersion.getDateRange();
    }

    public RuleSystem getRuleSystem() {
        return context.getStaticData().getRuleSystems().getByRuleSetId(fundVersion.getRuleSetId());
    }

    public Set<Integer> getPacketIds() {
        return Collections.unmodifiableSet(packetIds);
    }

    public void addPacketId(Integer packetId) {
        Validate.notNull(packetId);
        packetIds.add(packetId);
    }

    /* internal methods */

    ArrFund getFund() {
        return fundVersion.getFund();
    }

    ArrChange getLockChange() {
        return fundVersion.getLockChange();
    }
}
