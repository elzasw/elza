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
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cz.tacr.elza.api.AipProblemType;
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
 * Unit tests of the AIP reference resolution: the fund is looked up by the institution and the
 * fund number first, by the fund internal code second.
 */
public class DaAipReferenceResolverTest {

    private DaAipReferenceResolver resolver;

    private FundRepository fundRepository;
    private InstitutionRepository institutionRepository;
    private ApBindingRepository bindingRepository;
    private ApBindingStateRepository bindingStateRepository;
    private ApAccessPointRepository accessPointRepository;

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
        fund.setInstitution(institution);

        when(institutionRepository.findByInternalCode(any())).thenReturn(null);
        when(fundRepository.findByInternalCode(any())).thenReturn(null);
        when(fundRepository.findByInstitutionAndFundNumber(any(), any())).thenReturn(Collections.emptyList());

        resolver = new DaAipReferenceResolver();
        setField(resolver, "fundRepository", fundRepository);
        setField(resolver, "institutionRepository", institutionRepository);
        bindingRepository = mock(ApBindingRepository.class);
        bindingStateRepository = mock(ApBindingStateRepository.class);
        accessPointRepository = mock(ApAccessPointRepository.class);
        when(bindingRepository.findByValueAndExternalSystemTypes(any(), any())).thenReturn(Collections.emptyList());
        setField(resolver, "bindingRepository", bindingRepository);
        setField(resolver, "bindingStateRepository", bindingStateRepository);
        setField(resolver, "accessPointRepository", accessPointRepository);
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

    /** The same fund code under another institution is another fund. */
    @Test
    void fundOfAnotherInstitutionIsNotFoundByItsInternalCode() {
        ParInstitution other = new ParInstitution();
        other.setInstitutionId(2);
        other.setInternalCode("INST-2");
        when(institutionRepository.findByInternalCode("INST-2")).thenReturn(other);
        when(fundRepository.findByInternalCode("FUND-42")).thenReturn(fund);
        DaAipState aipState = state("INST-2", "FUND-42");

        resolver.resolveReferences(aipState);

        assertNull(aipState.getFund());
        assertEquals(AipProblemType.UNKNOWN_FUND, aipState.getProblemType());
        assertTrue(aipState.getProblemDescription().contains("INST-2"));
    }

    /** A new version naming an unknown institution does not fall back to the fund it had before. */
    @Test
    void formerFundIsNotFoundAgainUnderAnUnknownInstitution() {
        when(fundRepository.findByInternalCode("FUND-42")).thenReturn(fund);
        DaAipState aipState = state("INST-X", "FUND-42");

        resolver.resolveReferences(aipState, fund);

        assertNull(aipState.getFund());
        assertEquals(AipProblemType.UNKNOWN_FUND, aipState.getProblemType());
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
    void aRecordedProblemIsDescribedWithTheFileItIsAbout() {
        DaAipState aipState = state("INST-1", "42");
        aipState.setInstitution(institution);
        aipState.setFund(fund);

        resolver.recordProblem(aipState, AipProblem.of(AipProblemException.metadata(
                "Inherentní archivní popis se nepodařilo načíst", "metadata/descriptive/pruvodka.xml", null)));

        assertEquals(AipProblemType.METADATA_ERROR, aipState.getProblemType());
        assertEquals("Inherentní archivní popis se nepodařilo načíst", aipState.getProblemDescription());
        assertEquals("metadata/descriptive/pruvodka.xml", aipState.getProblemFile());
        // the description stays free of the technical detail
        assertFalse(aipState.getProblemDescription().contains(AipProblemException.class.getName()));
        assertTrue(aipState.getProblemDetail().contains(AipProblemException.class.getName()));
    }

    @Test
    void aRecordedProblemOutranksUnresolvedReferences() {
        DaAipState aipState = state("INST-X", "999");
        resolver.recordProblem(aipState, AipProblem.of(
                AipProblemException.metadata("Balíček neobsahuje soubor METS.xml")));

        // resolving the references must not describe them over the failure that has to be fixed first
        assertFalse(resolver.resolveReferences(aipState));

        assertEquals(AipProblemType.METADATA_ERROR, aipState.getProblemType());
        assertEquals("Balíček neobsahuje soubor METS.xml", aipState.getProblemDescription());
    }

    @Test
    void clearingTheProblemDescribesTheReferencesAgain() {
        DaAipState aipState = state("INST-X", "999");
        resolver.recordProblem(aipState, AipProblem.of(AipProblemException.metadata(
                "Balíček neobsahuje soubor METS.xml", "METS.xml", null)));

        resolver.clearProblem(aipState);

        assertEquals(AipProblemType.UNKNOWN_FUND, aipState.getProblemType());
        assertTrue(aipState.getProblemDescription().contains("999"));
        assertNull(aipState.getProblemDetail());
        assertNull(aipState.getProblemFile());
    }

    @Test
    void clearingTheProblemOfASoundAipLeavesNothing() {
        DaAipState aipState = state("INST-1", "42");
        aipState.setInstitution(institution);
        aipState.setFund(fund);
        resolver.recordProblem(aipState, AipProblem.of(
                AipProblemException.metadata("Balíček neobsahuje soubor METS.xml")));

        resolver.clearProblem(aipState);

        assertNull(aipState.getProblemType());
        assertNull(aipState.getProblemDescription());
        assertNull(aipState.getProblemDetail());
        assertNull(aipState.getProblemFile());
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
    void clearingTheProblemDoesNotResolveAnything() {
        DaAipState aipState = state("INST-1", "42");

        resolver.clearProblem(aipState);

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

    /** An AIP whose institution and fund are known, so only the originator is in question. */
    private DaAipState soundState(String originatorCamId) {
        DaAipState aipState = state("INST-1", "42");
        aipState.setInstitution(institution);
        aipState.setFund(fund);
        aipState.setOriginatorCamId(originatorCamId);
        return aipState;
    }

    private static ApAccessPoint accessPoint(int id) {
        ApAccessPoint accessPoint = new ApAccessPoint();
        accessPoint.setAccessPointId(id);
        return accessPoint;
    }

    @Test
    void originatorIsResolvedByItsCamBinding() {
        ApAccessPoint accessPoint = accessPoint(7);
        ApBinding binding = new ApBinding();
        ApBindingState bindingState = new ApBindingState();
        bindingState.setAccessPoint(accessPoint);
        when(bindingRepository.findByValueAndExternalSystemTypes(eq("1234567"), any())).thenReturn(List.of(binding));
        when(bindingStateRepository.findActiveByBinding(binding)).thenReturn(Optional.of(bindingState));
        DaAipState aipState = soundState("1234567");

        assertTrue(resolver.resolveReferences(aipState));

        assertSame(accessPoint, aipState.getOriginatorAccessPoint());
        assertNull(aipState.getProblemType());
        assertNull(aipState.getProblemDescription());
    }

    /** An entity taken over from CAM keeps its UUID even where the binding is by the entity id. */
    @Test
    void originatorIsResolvedByItsUuidWhenNoBindingCarriesTheIdentifier() {
        ApAccessPoint accessPoint = accessPoint(7);
        String uuid = "7d4b3c2a-1e0f-4a5b-9c8d-123456789abc";
        when(accessPointRepository.findAccessPointByUuid(uuid)).thenReturn(accessPoint);
        DaAipState aipState = soundState(uuid);

        assertTrue(resolver.resolveReferences(aipState));

        assertSame(accessPoint, aipState.getOriginatorAccessPoint());
        assertNull(aipState.getProblemType());
    }

    @Test
    void unknownOriginatorIsAProblemTheUserIsToldAbout() {
        DaAipState aipState = soundState("1234567");

        assertFalse(resolver.resolveReferences(aipState));

        assertNull(aipState.getOriginatorAccessPoint());
        assertEquals(AipProblemType.UNKNOWN_ORIGINATOR, aipState.getProblemType());
        assertTrue(aipState.getProblemDescription().contains("1234567"), aipState.getProblemDescription());
        assertTrue(aipState.getProblemDescription().contains("CAM"), aipState.getProblemDescription());
    }

    /** A package that names the originator without a CAM identifier has nothing to resolve. */
    @Test
    void originatorWithoutCamIdentifierIsNoProblem() {
        DaAipState aipState = soundState(null);
        aipState.setOriginator("Univerzita Karlova. 1. lékařská fakulta");

        assertFalse(resolver.resolveReferences(aipState));

        assertNull(aipState.getProblemType());
        assertNull(aipState.getProblemDescription());
        verify(bindingRepository, never()).findByValueAndExternalSystemTypes(any(), any());
    }

    @Test
    void unresolvedInstitutionOutranksUnresolvedOriginator() {
        DaAipState aipState = state("INST-X", "42");
        aipState.setFund(fund);
        aipState.setOriginatorCamId("1234567");

        assertFalse(resolver.resolveReferences(aipState));

        assertEquals(AipProblemType.UNKNOWN_INSTITUTION, aipState.getProblemType());
        assertTrue(aipState.getProblemDescription().contains("INST-X"));
        assertTrue(aipState.getProblemDescription().contains("1234567"));
    }

    @Test
    void alreadyResolvedOriginatorIsKept() {
        ApAccessPoint accessPoint = accessPoint(7);
        DaAipState aipState = soundState("1234567");
        aipState.setOriginatorAccessPoint(accessPoint);

        assertFalse(resolver.resolveReferences(aipState));

        assertSame(accessPoint, aipState.getOriginatorAccessPoint());
        verify(bindingRepository, never()).findByValueAndExternalSystemTypes(any(), any());
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
