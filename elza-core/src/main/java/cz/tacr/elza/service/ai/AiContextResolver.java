package cz.tacr.elza.service.ai;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.aiprovider.client.vo.AiObject;
import cz.tacr.elza.aiprovider.client.vo.FundInfo;
import cz.tacr.elza.aiprovider.client.vo.FundInfoObject;
import cz.tacr.elza.aiprovider.client.vo.InstitutionInfo;
import cz.tacr.elza.aiprovider.client.vo.ObjectType;
import cz.tacr.elza.controller.vo.AiContextFundVO;
import cz.tacr.elza.controller.vo.AiContextNodeVO;
import cz.tacr.elza.controller.vo.AiContextObjectVO;
import cz.tacr.elza.domain.ApIndex;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.repository.FundRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.service.AccessPointService;

/**
 * Resolves the UI's typed context objects ({@code AiContextObject}) into the
 * provider's typed objects ({@code AiObject}) that travel with a task. It reads
 * the referenced domain data from the DB — e.g. an {@code AiContextFund} becomes
 * an {@code elza.fundInfo} object built from its {@link ArrFund}. An
 * {@code AiContextNode} contributes the same {@code elza.fundInfo} for its fund,
 * so the provider always has self-contained fund context for the active level;
 * fund info is sent once per fund. Context that still has no provider payload
 * (an access point, and a node's own level data) resolves to nothing (logged);
 * the request still goes out with whatever could be resolved.
 */
@Service
public class AiContextResolver {

    private static final Logger logger = LoggerFactory.getLogger(AiContextResolver.class);

    @Autowired
    private FundRepository fundRepository;

    @Autowired
    private FundVersionRepository fundVersionRepository;

    @Autowired
    private AccessPointService accessPointService;

    /**
     * Resolves the context objects into provider objects, dropping the ones that
     * cannot be mapped yet. Fund info is sent once per fund: several context
     * objects (the fund itself and any nodes within it) resolve to the same fund.
     */
    public List<AiObject> resolveAll(final List<AiContextObjectVO> contextObjects) {
        List<AiObject> resolved = new ArrayList<>();
        if (contextObjects == null) {
            return resolved;
        }
        Set<Integer> fundInfoAdded = new HashSet<>();
        for (AiContextObjectVO ctx : contextObjects) {
            if (ctx instanceof AiContextFundVO fund) {
                addFundInfo(resolved, fundInfoAdded, fund.getFundId());
            } else if (ctx instanceof AiContextNodeVO node) {
                // A node carries no provider payload of its own yet, but its fund
                // is self-contained context for the active level; send the fund
                // info derived from the node.
                addFundInfo(resolved, fundInfoAdded, node.getFundId());
            } else {
                logger.info("AI context object {} is not resolvable to a provider object yet",
                        ctx == null ? null : ctx.getClass().getSimpleName());
            }
        }
        return resolved;
    }

    /**
     * Adds an {@code elza.fundInfo} object for the given fund, unless one for that
     * fund was already added. A missing {@code fundId} or an unknown fund is
     * skipped (the latter logged).
     */
    private void addFundInfo(final List<AiObject> resolved, final Set<Integer> fundInfoAdded,
                             final Integer fundId) {
        if (fundId == null || !fundInfoAdded.add(fundId)) {
            return;
        }
        fundRepository.findById(fundId).ifPresentOrElse(
                fund -> resolved.add(toFundInfoObject(fund)),
                () -> logger.info("AI context fund {} not found; skipped", fundId));
    }

    private AiObject toFundInfoObject(final ArrFund fund) {
        FundInfo info = new FundInfo()
                .name(fund.getName())
                .internalCode(fund.getInternalCode())
                .fundNumber(fund.getFundNumber())
                .mark(fund.getMark())
                .unitDate(fund.getUnitdate());
        ArrFundVersion openVersion = fundVersionRepository.findByFundIdAndLockChangeIsNull(fund.getFundId());
        if (openVersion != null && openVersion.getRuleSet() != null) {
            info.ruleSetCode(openVersion.getRuleSet().getCode());
        }
        if (fund.getInstitution() != null) {
            info.institution(toInstitutionInfo(fund.getInstitution()));
        }
        // objectType is set explicitly for in-memory use (e.g. matching against a
        // task's declared parameter types); on the wire Jackson writes it from the
        // type, and the field is @JsonIgnoreProperties-ignored on serialize.
        return new FundInfoObject().objectType(ObjectType.ELZA_FUND_INFO).data(info);
    }

    private InstitutionInfo toInstitutionInfo(final ParInstitution institution) {
        InstitutionInfo info = new InstitutionInfo().code(institution.getInternalCode());
        if (institution.getAccessPointId() != null) {
            // The institution's name is the preferred (display) name of its access point.
            ApIndex preferredName = accessPointService.findPreferredPartIndex(institution.getAccessPointId());
            if (preferredName != null) {
                info.name(preferredName.getIndexValue());
            }
        }
        return info;
    }
}
