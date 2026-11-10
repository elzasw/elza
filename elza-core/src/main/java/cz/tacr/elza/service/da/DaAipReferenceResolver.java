package cz.tacr.elza.service.da;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import cz.tacr.elza.api.AipProblemType;
import cz.tacr.elza.api.ApExternalSystemType;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.DaAipState;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApBindingRepository;
import cz.tacr.elza.repository.ApBindingStateRepository;
import cz.tacr.elza.repository.FundRepository;
import cz.tacr.elza.repository.InstitutionRepository;

/**
 * Owns the problem state of an AIP: the problem the processing of the package failed with, as
 * it is reported, and the problems derived from the references that could not be resolved.
 *
 * The references an AIP carries are codes of its originating system - the institution and the
 * fund. The fund is looked up by the institution together with the fund number first, because
 * a fund number is only unique within an institution; the fund internal code is the fallback
 * for packages whose FONDS_ID is not a fund number. The same fund code under another institution
 * is another fund, so a fund the fallback finds is not used when it belongs to another institution
 * than the package names. Every fund has an institution, so a
 * resolved fund also says which institution the AIP belongs to - an unresolved institution
 * therefore does not prevent the AIP from being used, while an unresolved fund does.
 *
 * The originator is a third, descriptive reference: the package may name it by its CAM
 * identifier, and the access point taken over from CAM under that identifier is looked up. A
 * package without the identifier names the originator by name only and has no originator
 * problem.
 */
@Service
public class DaAipReferenceResolver {

    private static final Logger logger = LoggerFactory.getLogger(DaAipReferenceResolver.class);

    @Autowired
    private FundRepository fundRepository;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private ApBindingRepository bindingRepository;

    @Autowired
    private ApBindingStateRepository bindingStateRepository;

    @Autowired
    private ApAccessPointRepository accessPointRepository;

    /** Every type of external system binds access points to CAM, by the entity id or its UUID. */
    private static final Set<ApExternalSystemType> CAM_TYPES = EnumSet.allOf(ApExternalSystemType.class);

    /**
     * Resolves the references that are still missing and updates the problem state. References
     * already resolved are kept, so an administrator's manual correction is never overwritten.
     *
     * @return true when a reference was newly resolved
     */
    public boolean resolveReferences(DaAipState aipState) {
        return resolveReferences(aipState, null);
    }

    /**
     * @param formerFund the fund of the previous version of the package when the new version names
     *                   another institution: the same fund code under another institution is
     *                   another fund, so this one is not found again by its code
     * @see #resolveReferences(DaAipState)
     */
    public boolean resolveReferences(DaAipState aipState, @Nullable ArrFund formerFund) {
        boolean hadInstitution = aipState.getInstitution() != null;
        boolean hadFund = aipState.getFund() != null;
        boolean hadOriginator = aipState.getOriginatorAccessPoint() != null;
        String note = null;

        if (!hadInstitution && StringUtils.isNotBlank(aipState.getInstitutionCode())) {
            aipState.setInstitution(institutionRepository.findByInternalCode(aipState.getInstitutionCode()));
        }
        if (!hadFund) {
            FundLookup lookup = findFund(aipState.getInstitution(), aipState.getFundCode(), formerFund);
            aipState.setFund(lookup.fund());
            note = lookup.note();
        }

        if (!hadOriginator && StringUtils.isNotBlank(aipState.getOriginatorCamId())) {
            aipState.setOriginatorAccessPoint(findOriginator(aipState.getOriginatorCamId()));
        }

        updateProblemState(aipState, note);
        return (!hadInstitution && aipState.getInstitution() != null)
                || (!hadFund && aipState.getFund() != null)
                || (!hadOriginator && aipState.getOriginatorAccessPoint() != null);
    }

    /**
     * The originator by its CAM identifier: the access point bound to that identifier in a CAM
     * external system (the entity id, or its UUID where the system binds by it), or else the
     * access point with that UUID - an entity taken over from CAM keeps its UUID even where the
     * binding is by id.
     */
    @Nullable
    private ApAccessPoint findOriginator(String camId) {
        for (ApBinding binding : bindingRepository.findByValueAndExternalSystemTypes(camId, CAM_TYPES)) {
            Optional<ApBindingState> bindingState = bindingStateRepository.findActiveByBinding(binding);
            if (bindingState.isPresent()) {
                return bindingState.get().getAccessPoint();
            }
        }
        return accessPointRepository.findAccessPointByUuid(camId);
    }

    /**
     * Drops the pairing with the fund. The active state is left as if the AIP was received
     * without a matching fund; a historical state only loses the reference.
     */
    public void releaseFund(DaAipState aipState) {
        aipState.setFund(null);
        if (aipState.getDeleteChange() == null) {
            updateProblemState(aipState, null);
        }
    }

    /**
     * Records the problem the processing of the package failed with. It outranks the problems
     * derived from the references, which are described again once it is cleared.
     */
    public void recordProblem(DaAipState aipState, AipProblem problem) {
        aipState.setProblemType(problem.type());
        aipState.setProblemDescription(problem.description());
        aipState.setProblemDetail(problem.detail());
        aipState.setProblemFile(problem.file());
    }

    /**
     * Clears the recorded failure of the processing - the package was processed, or what was
     * read out of it was thrown away. The references are described again.
     */
    public void clearProblem(DaAipState aipState) {
        aipState.setProblemDetail(null);
        aipState.setProblemFile(null);
        aipState.setProblemType(null);
        aipState.setProblemDescription(null);
        updateProblemState(aipState, null);
    }

    /**
     * A recorded failure of the processing outranks the unresolved references: it has to be
     * resolved first, and the references are resolved again with the next processing, so it
     * stays as the described problem until it is cleared.
     */
    private void updateProblemState(DaAipState aipState, @Nullable String note) {
        if (aipState.getProblemType() != null && aipState.getProblemType().isProcessingFailure()) {
            return;
        }

        List<String> problems = new ArrayList<>();
        if (aipState.getFund() == null) {
            problems.add(note != null ? note : fundNotFoundMessage(aipState.getFundCode()));
        }
        if (aipState.getInstitution() == null) {
            problems.add(StringUtils.isBlank(aipState.getInstitutionCode())
                    ? "Balíček neuvádí kód instituce."
                    : "Instituce s kódem '" + aipState.getInstitutionCode() + "' nebyla nalezena.");
        }
        if (aipState.getOriginatorAccessPoint() == null && StringUtils.isNotBlank(aipState.getOriginatorCamId())) {
            problems.add("Původce s identifikátorem CAM '" + aipState.getOriginatorCamId()
                    + "' nebyl mezi přístupovými body nalezen; převezměte entitu z CAM.");
        }

        aipState.setProblemType(derivedProblemType(aipState));
        aipState.setProblemDescription(problems.isEmpty() ? null : String.join(" ", problems));
    }

    /**
     * An unresolved fund outranks an unresolved institution: without the fund the AIP cannot be
     * mapped onto the archival description at all. The originator comes last - it is only
     * descriptive, and only a package that names it by a CAM identifier can miss it.
     */
    private static AipProblemType derivedProblemType(DaAipState aipState) {
        if (aipState.getFund() == null) {
            return AipProblemType.UNKNOWN_FUND;
        }
        if (aipState.getInstitution() == null) {
            return AipProblemType.UNKNOWN_INSTITUTION;
        }
        if (aipState.getOriginatorAccessPoint() == null && StringUtils.isNotBlank(aipState.getOriginatorCamId())) {
            return AipProblemType.UNKNOWN_ORIGINATOR;
        }
        return null;
    }

    private FundLookup findFund(@Nullable ParInstitution institution, @Nullable String fundCode,
                                @Nullable ArrFund formerFund) {
        if (StringUtils.isBlank(fundCode)) {
            return new FundLookup(null, "Balíček neuvádí identifikátor fondu.");
        }
        Integer fundNumber = parseFundNumber(fundCode);
        if (institution != null && fundNumber != null) {
            List<ArrFund> funds = fundRepository.findByInstitutionAndFundNumber(institution, fundNumber);
            if (funds.size() == 1) {
                return new FundLookup(funds.get(0), null);
            }
            if (funds.size() > 1) {
                logger.warn("Institution {} has {} funds with number {}, the fund of the AIP is ambiguous",
                        institution.getInternalCode(), funds.size(), fundNumber);
                return new FundLookup(null, "Instituce '" + institution.getInternalCode()
                        + "' má více fondů s číslem " + fundNumber + ", fond nelze určit jednoznačně.");
            }
        }
        // The internal code is unique across institutions, so the fund it finds may belong to
        // another institution than the package names - and then it is not the fund of the package.
        ArrFund fund = fundRepository.findByInternalCode(fundCode);
        if (fund != null && institution != null
                && !institution.getInstitutionId().equals(fund.getInstitution().getInstitutionId())) {
            return new FundLookup(null, "Fond '" + fundCode + "' patří jiné instituci než '"
                    + institution.getInternalCode() + "'.");
        }
        // an institution ELZA does not know cannot be compared, but it is not the one of the
        // former fund - the package named another one before
        if (fund != null && institution == null && formerFund != null
                && fund.getFundId().equals(formerFund.getFundId())) {
            return new FundLookup(null, "Balíček uvádí jinou instituci než jeho předchozí verze, fond '"
                    + fundCode + "' této instituce nebyl nalezen.");
        }
        return new FundLookup(fund, fund == null ? fundNotFoundMessage(fundCode) : null);
    }

    private static String fundNotFoundMessage(@Nullable String fundCode) {
        return StringUtils.isBlank(fundCode)
                ? "Balíček neuvádí identifikátor fondu."
                : "Fond '" + fundCode + "' nebyl nalezen podle čísla fondu ani podle interního kódu.";
    }

    private static Integer parseFundNumber(String fundCode) {
        try {
            return Integer.valueOf(fundCode.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Result of the fund lookup with the reason it failed, if it did.
     */
    private record FundLookup(@Nullable ArrFund fund, @Nullable String note) {
    }
}
