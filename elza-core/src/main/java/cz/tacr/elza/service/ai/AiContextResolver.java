package cz.tacr.elza.service.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
import cz.tacr.elza.controller.vo.AiContextObjectVO;
import cz.tacr.elza.domain.ApIndex;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.repository.FundRepository;
import cz.tacr.elza.service.AccessPointService;

/**
 * Resolves the UI's typed context objects ({@code AiContextObject}) into the
 * provider's typed objects ({@code AiObject}) that travel with a task. It reads
 * the referenced domain data from the DB — e.g. an {@code AiContextFund} becomes
 * an {@code elza.fundInfo} object built from its {@link ArrFund}. Context types
 * with no provider payload yet (node, access point) resolve to nothing (logged);
 * the request still goes out with whatever could be resolved.
 */
@Service
public class AiContextResolver {

    private static final Logger logger = LoggerFactory.getLogger(AiContextResolver.class);

    @Autowired
    private FundRepository fundRepository;

    @Autowired
    private AccessPointService accessPointService;

    /** Resolves each context object, dropping the ones that cannot be mapped yet. */
    public List<AiObject> resolveAll(final List<AiContextObjectVO> contextObjects) {
        List<AiObject> resolved = new ArrayList<>();
        if (contextObjects != null) {
            for (AiContextObjectVO ctx : contextObjects) {
                resolve(ctx).ifPresent(resolved::add);
            }
        }
        return resolved;
    }

    /** Resolves one context object to a provider object, or empty when unsupported/missing. */
    public Optional<AiObject> resolve(final AiContextObjectVO ctx) {
        if (ctx instanceof AiContextFundVO fund) {
            return resolveFund(fund);
        }
        // Node and access-point objects have no provider payload defined yet.
        logger.info("AI context object {} is not resolvable to a provider object yet",
                ctx == null ? null : ctx.getClass().getSimpleName());
        return Optional.empty();
    }

    private Optional<AiObject> resolveFund(final AiContextFundVO fund) {
        if (fund.getFundId() == null) {
            return Optional.empty();
        }
        Optional<ArrFund> arrFund = fundRepository.findById(fund.getFundId());
        if (arrFund.isEmpty()) {
            logger.info("AI context fund {} not found; skipped", fund.getFundId());
        }
        return arrFund.map(this::toFundInfoObject);
    }

    private AiObject toFundInfoObject(final ArrFund fund) {
        FundInfo info = new FundInfo()
                .name(fund.getName())
                .internalCode(fund.getInternalCode())
                .fundNumber(fund.getFundNumber())
                .mark(fund.getMark())
                .unitDate(fund.getUnitdate());
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
