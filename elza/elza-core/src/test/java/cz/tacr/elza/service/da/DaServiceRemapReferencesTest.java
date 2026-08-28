package cz.tacr.elza.service.da;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import cz.tacr.elza.api.AipType;
import cz.tacr.elza.api.DaOnReceivedAction;
import cz.tacr.elza.api.DigitalRepositoryType;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaAipState;
import cz.tacr.elza.domain.DaSyncQueueItem;
import cz.tacr.elza.repository.AipRepository;
import cz.tacr.elza.repository.AipStateRepository;
import cz.tacr.elza.repository.DaSyncQueueItemRepository;

/**
 * A repository that downloads metadata automatically cannot do so while the fund of an AIP is
 * unknown. Once the administrator creates the fund and has the references resolved again, the
 * download has to follow without a further action.
 */
public class DaServiceRemapReferencesTest {

    private static final int AIP_ID = 11;

    private DaService service;

    private AipRepository aipRepository;
    private AipStateRepository aipStateRepository;
    private DaSyncQueueItemRepository syncQueueItemRepository;
    private DaAipReferenceResolver referenceResolver;

    private DaAip aip;
    private DaAipState aipState;
    private ArrDigitalRepository repository;
    private ArrFund fund;

    @BeforeEach
    void setUp() {
        aipRepository = mock(AipRepository.class);
        aipStateRepository = mock(AipStateRepository.class);
        syncQueueItemRepository = mock(DaSyncQueueItemRepository.class);
        referenceResolver = mock(DaAipReferenceResolver.class);

        repository = new ArrDigitalRepository();
        repository.setExternalSystemId(3);
        repository.setCode("DA-REPO");
        repository.setDigitalRepositoryType(DigitalRepositoryType.DA);
        repository.setOnReceived(DaOnReceivedAction.DOWNLOAD_METADATA);

        fund = new ArrFund();
        fund.setFundId(1);

        aip = new DaAip();
        aip.setAipId(AIP_ID);
        aip.setCode("aip-code");
        aip.setDigitalRepository(repository);

        aipState = new DaAipState();
        aipState.setDaAip(aip);
        aipState.setAipVersion("1");

        when(aipRepository.findAllById(anyCollection())).thenReturn(List.of(aip));
        when(aipStateRepository.findByDaAipInAndDeleteChangeIsNull(any())).thenReturn(List.of(aipState));
        when(syncQueueItemRepository.save(any(DaSyncQueueItem.class))).thenAnswer(inv -> inv.getArgument(0));

        service = new DaService();
        setField(service, "aipRepository", aipRepository);
        setField(service, "aipStateRepository", aipStateRepository);
        setField(service, "syncQueueItemRepository", syncQueueItemRepository);
        setField(service, "referenceResolver", referenceResolver);
    }

    /** The resolver reports success and fills the fund in, as it does against a real database. */
    private void resolverFindsTheFund() {
        when(referenceResolver.resolveReferences(aipState)).thenAnswer(inv -> {
            aipState.setFund(fund);
            return true;
        });
    }

    @Test
    void pairedAipHasItsMetadataRequested() {
        resolverFindsTheFund();

        assertEquals(1, service.remapReferences(List.of(AIP_ID)));

        ArgumentCaptor<DaSyncQueueItem> item = ArgumentCaptor.forClass(DaSyncQueueItem.class);
        verify(syncQueueItemRepository).save(item.capture());
        assertEquals(AipType.METADATA_BASE, item.getValue().getAipType());
        assertEquals(DaSyncQueueItem.QueueItemState.UPDATE, item.getValue().getState());
        assertEquals("aip-code", item.getValue().getCode());
        assertTrue(aipState.getMetadataLoad());
    }

    @Test
    void repositoryThatDoesNotDownloadAutomaticallyOnlyPairs() {
        repository.setOnReceived(DaOnReceivedAction.NONE);
        resolverFindsTheFund();

        assertEquals(1, service.remapReferences(List.of(AIP_ID)));

        verify(syncQueueItemRepository, never()).save(any(DaSyncQueueItem.class));
        assertNull(aipState.getMetadataLoad());
    }

    @Test
    void aipThatStillHasNoFundRequestsNothing() {
        when(referenceResolver.resolveReferences(aipState)).thenReturn(false);

        assertEquals(0, service.remapReferences(List.of(AIP_ID)));

        verify(syncQueueItemRepository, never()).save(any(DaSyncQueueItem.class));
    }

    @Test
    void metadataAlreadyLoadedAreNotRequestedAgain() {
        aipState.setMetadataLoad(true);
        resolverFindsTheFund();

        assertEquals(1, service.remapReferences(List.of(AIP_ID)));

        verify(syncQueueItemRepository, never()).save(any(DaSyncQueueItem.class));
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
