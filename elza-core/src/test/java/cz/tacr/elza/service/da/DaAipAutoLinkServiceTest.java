package cz.tacr.elza.service.da;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import cz.tacr.elza.domain.ArrDaLink;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaAipState;
import cz.tacr.elza.repository.AipStateRepository;
import cz.tacr.elza.repository.ArrDaLinkRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.service.ArrangementInternalService;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventIdNodeIdInVersion;

/**
 * Unit tests of the UUID matching of {@link DaAipAutoLinkService}; @Transactional is an AOP
 * concern and is not exercised.
 */
public class DaAipAutoLinkServiceTest {

    private static final int AIP_ID = 11;
    private static final String PACKAGE_UUID = "2fde031b-0cac-4e5a-b04a-8ad8da08bed4";
    private static final String LEVEL_UUID = "2e9d2e65-b2e9-4e5e-ad84-1730553f3be3";
    private static final String REPRESENTATION_UUID = "506b5ba5-b9a4-4907-b0f2-f87e7db046e3";

    private DaAipAutoLinkService service;

    private DaService daService;
    private AipStateRepository aipStateRepository;
    private ArrDaLinkRepository daLinkRepository;
    private NodeRepository nodeRepository;
    private ArrangementInternalService arrangementInternalService;
    private EventNotificationService eventNotificationService;

    private DaAip aip;
    private DaAipState aipState;
    private ArrFund fund;

    @BeforeEach
    void setUp() {
        daService = mock(DaService.class);
        aipStateRepository = mock(AipStateRepository.class);
        daLinkRepository = mock(ArrDaLinkRepository.class);
        nodeRepository = mock(NodeRepository.class);
        arrangementInternalService = mock(ArrangementInternalService.class);
        eventNotificationService = mock(EventNotificationService.class);

        fund = new ArrFund();
        fund.setFundId(1);

        aip = new DaAip();
        aip.setAipId(AIP_ID);
        aip.setCode("uuid-" + PACKAGE_UUID);
        aipState = new DaAipState();
        aipState.setDaAip(aip);
        aipState.setFund(fund);

        when(daService.findAipById(AIP_ID)).thenReturn(aip);
        when(aipStateRepository.findByDaAipAndDeleteChangeIsNull(aip)).thenReturn(aipState);
        when(daLinkRepository.findByAipIdAndDeleteChangeIsNull(AIP_ID)).thenReturn(Collections.emptyList());
        when(nodeRepository.findByFundAndUuidIn(any(), anyCollection())).thenReturn(Collections.emptyList());

        ArrFundVersion fundVersion = new ArrFundVersion();
        fundVersion.setFundVersionId(7);
        when(arrangementInternalService.getOpenVersionByFund(fund)).thenReturn(fundVersion);

        when(daService.connectToJP(any(), eq(AIP_ID))).thenAnswer(inv -> {
            ArrDaLink link = new ArrDaLink();
            link.setDaoLinkId(500);
            link.setAip(aip);
            return link;
        });

        service = new DaAipAutoLinkService();
        setField(service, "daService", daService);
        setField(service, "aipStateRepository", aipStateRepository);
        setField(service, "daLinkRepository", daLinkRepository);
        setField(service, "nodeRepository", nodeRepository);
        setField(service, "arrangementInternalService", arrangementInternalService);
        setField(service, "eventNotificationService", eventNotificationService);
    }

    private static ArrNode node(int nodeId, String uuid) {
        ArrNode node = new ArrNode();
        node.setNodeId(nodeId);
        node.setUuid(uuid);
        return node;
    }

    /** The UUIDs as {@link AipNodeUuids} produces them: package, levels, representations. */
    private static List<String> uuids() {
        return List.of(PACKAGE_UUID, LEVEL_UUID, REPRESENTATION_UUID);
    }

    @Test
    void attachesToTheNodeOfThePackageUuid() {
        when(nodeRepository.findByFundAndUuidIn(eq(fund), anyCollection()))
                .thenReturn(List.of(node(5, PACKAGE_UUID)));

        Optional<ArrDaLink> link = service.linkReceivedAip(AIP_ID, uuids());

        assertTrue(link.isPresent());
        verify(daService).connectToJP(5, AIP_ID);

        ArgumentCaptor<EventIdNodeIdInVersion> event = ArgumentCaptor.forClass(EventIdNodeIdInVersion.class);
        verify(eventNotificationService).publishEvent(event.capture());
        assertEquals(Integer.valueOf(7), event.getValue().getVersionId());
    }

    @Test
    void attachesToTheLevelWhenThePackageItselfIsNotDescribed() {
        when(nodeRepository.findByFundAndUuidIn(eq(fund), anyCollection()))
                .thenReturn(List.of(node(6, LEVEL_UUID)));

        assertTrue(service.linkReceivedAip(AIP_ID, uuids()).isPresent());

        verify(daService).connectToJP(6, AIP_ID);
    }

    @Test
    void attachesToTheRepresentationAsTheLastResort() {
        when(nodeRepository.findByFundAndUuidIn(eq(fund), anyCollection()))
                .thenReturn(List.of(node(7, REPRESENTATION_UUID)));

        assertTrue(service.linkReceivedAip(AIP_ID, uuids()).isPresent());

        verify(daService).connectToJP(7, AIP_ID);
    }

    @Test
    void theOutermostMatchWinsWhenSeveralPartsAreDescribed() {
        // the repository returns them in an arbitrary order, the matching order decides
        when(nodeRepository.findByFundAndUuidIn(eq(fund), anyCollection()))
                .thenReturn(List.of(node(7, REPRESENTATION_UUID), node(6, LEVEL_UUID)));

        assertTrue(service.linkReceivedAip(AIP_ID, uuids()).isPresent());

        verify(daService).connectToJP(6, AIP_ID);
    }

    @Test
    void noMatchingNodeLeavesTheAipUnattached() {
        assertFalse(service.linkReceivedAip(AIP_ID, uuids()).isPresent());

        verify(daService, never()).connectToJP(any(), any());
    }

    @Test
    void aipWithoutUuidsIsSkipped() {
        assertFalse(service.linkReceivedAip(AIP_ID, List.of()).isPresent());

        verify(nodeRepository, never()).findByFundAndUuidIn(any(), anyCollection());
    }

    @Test
    void alreadyLinkedAipIsSkipped() {
        when(daLinkRepository.findByAipIdAndDeleteChangeIsNull(AIP_ID)).thenReturn(List.of(new ArrDaLink()));

        assertFalse(service.linkReceivedAip(AIP_ID, uuids()).isPresent());

        verify(nodeRepository, never()).findByFundAndUuidIn(any(), anyCollection());
    }

    @Test
    void aipWithoutFundIsSkipped() {
        aipState.setFund(null);

        assertFalse(service.linkReceivedAip(AIP_ID, uuids()).isPresent());

        verify(nodeRepository, never()).findByFundAndUuidIn(any(), anyCollection());
    }

    @Test
    void failureOfOneAipDoesNotStopTheOthers() {
        when(daService.findAipById(12)).thenThrow(new IllegalStateException("boom"));
        when(nodeRepository.findByFundAndUuidIn(eq(fund), anyCollection()))
                .thenReturn(List.of(node(5, PACKAGE_UUID)));

        int linked = service.linkReceivedAips(Map.of(12, uuids(), AIP_ID, uuids()));

        assertEquals(1, linked);
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
