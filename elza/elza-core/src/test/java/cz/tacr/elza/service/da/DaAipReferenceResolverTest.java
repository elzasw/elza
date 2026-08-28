package cz.tacr.elza.service.da;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cz.tacr.elza.api.AipProblemType;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.DaAipState;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.repository.FundRepository;
import cz.tacr.elza.repository.InstitutionRepository;

/**
 * Unit tests of the AIP reference resolution: the fund is looked up by the institution and the
 * fund number first, by the fund internal code second.
 */
public class DaAipReferenceResolverTest {

    private DaAipReferenceResolver resolver;

    private FundRepository fundRepository;
    private InstitutionRepository institutionRepository;

    private ParInstitution institution;
    private ArrFund fund;

    @BeforeEach
    void setUp() {
        fundRepository = mock(FundRepository.class);
        institutionRepository = mock(InstitutionRepository.class);

        institution = new ParInstitution();
        institution.setInstitutionId(1);
        institution.setInternalCode("INST-1");

        fund = new ArrFund();
        fund.setFundId(5);
        fund.setFundNumber(42);
        fund.setInternalCode("FUND-42");

        when(institutionRepository.findByInternalCode(any())).thenReturn(null);
        when(fundRepository.findByInternalCode(any())).thenReturn(null);
        when(fundRepository.findByInstitutionAndFundNumber(any(), any())).thenReturn(Collections.emptyList());

        resolver = new DaAipReferenceResolver();
        setField(resolver, "fundRepository", fundRepository);
        setField(resolver, "institutionRepository", institutionRepository);
    }

    private static DaAipState state(String institutionCode, String fundCode) {
        DaAipState state = new DaAipState();
        state.setInstitutionCode(institutionCode);
        state.setFundCode(fundCode);
        return state;
    }

    @Test
    void resolvesFundByInstitutionAndFundNumber() {
        when(institutionRepository.findByInternalCode("INST-1")).thenReturn(institution);
        when(fundRepository.findByInstitutionAndFundNumber(institution, 42)).thenReturn(List.of(fund));
        DaAipState aipState = state("INST-1", "42");

        assertTrue(resolver.resolveReferences(aipState));

        assertSame(institution, aipState.getInstitution());
        assertSame(fund, aipState.getFund());
        assertNull(aipState.getProblemType());
        assertNull(aipState.getProblemDescription());
        // the fund number lookup wins, the internal code is not consulted
        verify(fundRepository, never()).findByInternalCode(any());
    }

    @Test
    void fallsBackToFundInternalCodeWhenFundCodeIsNotANumber() {
        when(institutionRepository.findByInternalCode("INST-1")).thenReturn(institution);
        when(fundRepository.findByInternalCode("FUND-42")).thenReturn(fund);
        DaAipState aipState = state("INST-1", "FUND-42");

        assertTrue(resolver.resolveReferences(aipState));

        assertSame(fund, aipState.getFund());
        assertNull(aipState.getProblemType());
        verify(fundRepository, never()).findByInstitutionAndFundNumber(any(), any());
    }

    @Test
    void fallsBackToFundInternalCodeWhenTheInstitutionIsUnknown() {
        when(fundRepository.findByInternalCode("42")).thenReturn(fund);
        DaAipState aipState = state("INST-X", "42");

        assertTrue(resolver.resolveReferences(aipState));

        assertSame(fund, aipState.getFund());
        assertNull(aipState.getInstitution());
        // the fund is known, so the AIP is usable - the unresolved institution is descriptive
        assertEquals(AipProblemType.UNKNOWN_INSTITUTION, aipState.getProblemType());
        assertTrue(aipState.getProblemDescription().contains("INST-X"));
    }

    @Test
    void ambiguousFundNumberIsNotResolved() {
        ArrFund other = new ArrFund();
        other.setFundId(6);
        when(institutionRepository.findByInternalCode("INST-1")).thenReturn(institution);
        when(fundRepository.findByInstitutionAndFundNumber(institution, 42)).thenReturn(List.of(fund, other));
        DaAipState aipState = state("INST-1", "42");

        assertTrue(resolver.resolveReferences(aipState));

        assertNull(aipState.getFund());
        assertEquals(AipProblemType.UNKNOWN_FUND, aipState.getProblemType());
        assertTrue(aipState.getProblemDescription().contains("více fondů"));
        // an ambiguous fund number must not silently fall through to the internal code
        verify(fundRepository, never()).findByInternalCode(any());
    }

    @Test
    void unresolvedFundOutranksUnresolvedInstitution() {
        DaAipState aipState = state("INST-X", "999");

        assertFalse(resolver.resolveReferences(aipState));

        assertEquals(AipProblemType.UNKNOWN_FUND, aipState.getProblemType());
        assertTrue(aipState.getProblemDescription().contains("999"));
        assertTrue(aipState.getProblemDescription().contains("INST-X"));
    }

    @Test
    void metadataErrorOutranksUnresolvedReferences() {
        DaAipState aipState = state("INST-X", "999");
        aipState.setMetadataError(true);
        aipState.setMetadataErrorException("Balíček neobsahuje soubor METS.xml");

        resolver.resolveReferences(aipState);

        assertEquals(AipProblemType.METADATA_ERROR, aipState.getProblemType());
        assertTrue(aipState.getProblemDescription().contains("METS.xml"));
        // the reference problems stay in the description
        assertTrue(aipState.getProblemDescription().contains("999"));
    }

    @Test
    void missingCodesAreDescribed() {
        DaAipState aipState = state(null, null);

        assertFalse(resolver.resolveReferences(aipState));

        assertEquals(AipProblemType.UNKNOWN_FUND, aipState.getProblemType());
        assertTrue(aipState.getProblemDescription().contains("neuvádí identifikátor fondu"));
        assertTrue(aipState.getProblemDescription().contains("neuvádí kód instituce"));
        verify(institutionRepository, never()).findByInternalCode(any());
    }

    @Test
    void alreadyResolvedReferencesAreKept() {
        DaAipState aipState = state("INST-1", "42");
        aipState.setInstitution(institution);
        aipState.setFund(fund);

        assertFalse(resolver.resolveReferences(aipState));

        assertSame(fund, aipState.getFund());
        assertNull(aipState.getProblemType());
        verify(institutionRepository, never()).findByInternalCode(any());
        verify(fundRepository, never()).findByInstitutionAndFundNumber(any(), any());
        verify(fundRepository, never()).findByInternalCode(any());
    }

    @Test
    void updateProblemStateDoesNotResolveAnything() {
        DaAipState aipState = state("INST-1", "42");

        resolver.updateProblemState(aipState);

        assertEquals(AipProblemType.UNKNOWN_FUND, aipState.getProblemType());
        verify(institutionRepository, never()).findByInternalCode(any());
        verify(fundRepository, never()).findByInstitutionAndFundNumber(any(), eq(42));
    }

    @Test
    void problemIsClearedOnceEverythingResolves() {
        DaAipState aipState = state("INST-1", "42");
        aipState.setProblemType(AipProblemType.UNKNOWN_FUND);
        aipState.setProblemDescription("Fond '42' nebyl nalezen.");
        when(institutionRepository.findByInternalCode("INST-1")).thenReturn(institution);
        when(fundRepository.findByInstitutionAndFundNumber(institution, 42)).thenReturn(List.of(fund));

        assertTrue(resolver.resolveReferences(aipState));

        assertNull(aipState.getProblemType());
        assertNull(aipState.getProblemDescription());
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
