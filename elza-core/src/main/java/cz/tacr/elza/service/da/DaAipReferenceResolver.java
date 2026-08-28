package cz.tacr.elza.service.da;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import cz.tacr.elza.api.AipProblemType;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.DaAipState;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.repository.FundRepository;
import cz.tacr.elza.repository.InstitutionRepository;

/**
 * Resolves the references an AIP carries as codes of its originating system - the institution
 * and the fund - and describes what could not be resolved.
 *
 * The fund is looked up by the institution together with the fund number first, because a fund
 * number is only unique within an institution; the fund internal code is the fallback for
 * packages whose FONDS_ID is not a fund number. Every fund has an institution, so a resolved
 * fund also says which institution the AIP belongs to - an unresolved institution therefore
 * does not prevent the AIP from being used, while an unresolved fund does.
 */
@Service
public class DaAipReferenceResolver {

    private static final Logger logger = LoggerFactory.getLogger(DaAipReferenceResolver.class);

    @Autowired
    private FundRepository fundRepository;

    @Autowired
    private InstitutionRepository institutionRepository;

    /**
     * Resolves the references that are still missing and updates the problem state. References
     * already resolved are kept, so an administrator's manual correction is never overwritten.
     *
     * @return true when a reference was newly resolved
     */
    public boolean resolveReferences(DaAipState aipState) {
        boolean hadInstitution = aipState.getInstitution() != null;
        boolean hadFund = aipState.getFund() != null;
        String note = null;

        if (!hadInstitution && StringUtils.isNotBlank(aipState.getInstitutionCode())) {
            aipState.setInstitution(institutionRepository.findByInternalCode(aipState.getInstitutionCode()));
        }
        if (!hadFund) {
            FundLookup lookup = findFund(aipState.getInstitution(), aipState.getFundCode());
            aipState.setFund(lookup.fund());
            note = lookup.note();
        }

        updateProblemState(aipState, note);
        return (!hadInstitution && aipState.getInstitution() != null)
                || (!hadFund && aipState.getFund() != null);
    }

    /**
     * Recomputes the problem state from the current state of the AIP, without resolving
     * anything - used when only the result of the metadata processing changed.
     */
    public void updateProblemState(DaAipState aipState) {
        updateProblemState(aipState, null);
    }

    private void updateProblemState(DaAipState aipState, @Nullable String note) {
        List<String> problems = new ArrayList<>();
        if (Boolean.TRUE.equals(aipState.getMetadataError())
                && StringUtils.isNotBlank(aipState.getMetadataErrorException())) {
            problems.add("Chyba při zpracování metadat: " + aipState.getMetadataErrorException());
        }
        if (aipState.getFund() == null) {
            problems.add(note != null ? note : fundNotFoundMessage(aipState.getFundCode()));
        }
        if (aipState.getInstitution() == null) {
            problems.add(StringUtils.isBlank(aipState.getInstitutionCode())
                    ? "Balíček neuvádí kód instituce."
                    : "Instituce s kódem '" + aipState.getInstitutionCode() + "' nebyla nalezena.");
        }

        aipState.setProblemType(problemType(aipState));
        aipState.setProblemDescription(problems.isEmpty() ? null : String.join(" ", problems));
    }

    /**
     * A failure of the metadata processing outranks the unresolved references: it has to be
     * resolved first and the references are resolved again with the next processing.
     */
    private static AipProblemType problemType(DaAipState aipState) {
        if (Boolean.TRUE.equals(aipState.getMetadataError())) {
            return AipProblemType.METADATA_ERROR;
        }
        if (aipState.getFund() == null) {
            return AipProblemType.UNKNOWN_FUND;
        }
        if (aipState.getInstitution() == null) {
            return AipProblemType.UNKNOWN_INSTITUTION;
        }
        return null;
    }

    private FundLookup findFund(@Nullable ParInstitution institution, @Nullable String fundCode) {
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
        ArrFund fund = fundRepository.findByInternalCode(fundCode);
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
