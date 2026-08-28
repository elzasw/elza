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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import cz.tacr.elza.config.da.DaAutoLinkConfig;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ArrDaLink;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaAipState;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.repository.AipStateRepository;
import cz.tacr.elza.repository.ArrDaLinkRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.service.ArrangementInternalService;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventIdNodeIdInVersion;

/**
 * Unit tests of the node lookup of {@link DaAipAutoLinkService}; @Transactional is an AOP
 * concern and is not exercised.
 */
public class DaAipAutoLinkServiceTest {

    private static final int AIP_ID = 11;

    private DaAipAutoLinkService service;

    private DaService daService;
    private AipStateRepository aipStateRepository;
    private ArrDaLinkRepository daLinkRepository;
    private DescItemRepository descItemRepository;
    private ArrangementInternalService arrangementInternalService;
    private EventNotificationService eventNotificationService;

    private DaAip aip;
    private DaAipState aipState;
    private ArrFund fund;
    private RulItemType itemType;
    private RulItemSpec itemSpec;

    @BeforeEach
    void setUp() {
        daService = mock(DaService.class);
        aipStateRepository = mock(AipStateRepository.class);
        daLinkRepository = mock(ArrDaLinkRepository.class);
        descItemRepository = mock(DescItemRepository.class);
        arrangementInternalService = mock(ArrangementInternalService.class);
        eventNotificationService = mock(EventNotificationService.class);

        fund = new ArrFund();
        fund.setFundId(1);

        aip = new DaAip();
        aip.setAipId(AIP_ID);
        aip.setCode("aip-code");
        aipState = new DaAipState();
        aipState.setDaAip(aip);
        aipState.setFund(fund);

        when(daService.findAipById(AIP_ID)).thenReturn(aip);
        when(aipStateRepository.findByDaAipAndDeleteChangeIsNull(aip)).thenReturn(aipState);
        when(daLinkRepository.findByAipIdAndDeleteChangeIsNull(AIP_ID)).thenReturn(Collections.emptyList());

        itemType = new RulItemType();
        itemType.setItemTypeId(100);
        itemSpec = new RulItemSpec();
        itemSpec.setItemSpecId(200);
        ItemType type = mock(ItemType.class);
        when(type.getEntity()).thenReturn(itemType);
        when(type.getItemSpecByCode("ZP2015_OTHERID_SOURCEID")).thenReturn(itemSpec);
        StaticDataProvider sdp = mock(StaticDataProvider.class);
        when(sdp.getItemTypeByCode("ZP2015_OTHER_ID")).thenReturn(type);
        StaticDataService staticDataService = mock(StaticDataService.class);
        when(staticDataService.getData()).thenReturn(sdp);

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
        setField(service, "descItemRepository", descItemRepository);
        setField(service, "staticDataService", staticDataService);
        setField(service, "arrangementInternalService", arrangementInternalService);
        setField(service, "eventNotificationService", eventNotificationService);
        setField(service, "config", new DaAutoLinkConfig());
    }

    private static ArrDescItem itemOfNode(int nodeId) {
        ArrDescItem item = mock(ArrDescItem.class);
        when(item.getNodeId()).thenReturn(nodeId);
        return item;
    }

    @Test
    void linkReceivedAip_singleMatchingNode_attachesAndNotifies() {
        List<ArrDescItem> items = List.of(itemOfNode(5), itemOfNode(5));
        when(descItemRepository.findOpenByFundTypeSpecAndStringValues(eq(fund), eq(itemType), eq(itemSpec), anyCollection()))
                .thenReturn(items);

        Optional<ArrDaLink> link = service.linkReceivedAip(AIP_ID, Set.of("eSSL:DOC-1"));

        assertTrue(link.isPresent());
        verify(daService).connectToJP(5, AIP_ID);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<String>> values = ArgumentCaptor.forClass(Collection.class);
        verify(descItemRepository).findOpenByFundTypeSpecAndStringValues(eq(fund), eq(itemType), eq(itemSpec), values.capture());
        assertEquals(Set.of("eSSL:DOC-1", "aip-code"), Set.copyOf(values.getValue()));

        ArgumentCaptor<EventIdNodeIdInVersion> event = ArgumentCaptor.forClass(EventIdNodeIdInVersion.class);
        verify(eventNotificationService).publishEvent(event.capture());
        assertEquals(Integer.valueOf(7), event.getValue().getVersionId());
    }

    @Test
    void linkReceivedAip_noMatchingNode_leavesAipUnattached() {
        when(descItemRepository.findOpenByFundTypeSpecAndStringValues(eq(fund), eq(itemType), eq(itemSpec), anyCollection()))
                .thenReturn(Collections.emptyList());

        assertFalse(service.linkReceivedAip(AIP_ID, Set.of("unknown")).isPresent());
        verify(daService, never()).connectToJP(any(), any());
    }

    @Test
    void linkReceivedAip_severalMatchingNodes_leavesAipUnattached() {
        List<ArrDescItem> items = List.of(itemOfNode(5), itemOfNode(6));
        when(descItemRepository.findOpenByFundTypeSpecAndStringValues(eq(fund), eq(itemType), eq(itemSpec), anyCollection()))
                .thenReturn(items);

        assertFalse(service.linkReceivedAip(AIP_ID, Set.of("eSSL:DOC-1")).isPresent());
        verify(daService, never()).connectToJP(any(), any());
    }

    @Test
    void linkReceivedAip_alreadyLinked_isSkipped() {
        when(daLinkRepository.findByAipIdAndDeleteChangeIsNull(AIP_ID)).thenReturn(List.of(new ArrDaLink()));

        assertFalse(service.linkReceivedAip(AIP_ID, Set.of("eSSL:DOC-1")).isPresent());
        verify(descItemRepository, never()).findOpenByFundTypeSpecAndStringValues(any(), any(), any(), anyCollection());
    }

    @Test
    void linkReceivedAip_withoutFund_isSkipped() {
        aipState.setFund(null);

        assertFalse(service.linkReceivedAip(AIP_ID, Set.of("eSSL:DOC-1")).isPresent());
        verify(descItemRepository, never()).findOpenByFundTypeSpecAndStringValues(any(), any(), any(), anyCollection());
    }

    @Test
    void linkReceivedAips_failureOfOneAipDoesNotStopOthers() {
        when(daService.findAipById(12)).thenThrow(new IllegalStateException("boom"));
        List<ArrDescItem> items = List.of(itemOfNode(5));
        when(descItemRepository.findOpenByFundTypeSpecAndStringValues(eq(fund), eq(itemType), eq(itemSpec), anyCollection()))
                .thenReturn(items);

        int linked = service.linkReceivedAips(Map.of(12, Set.of("x"), AIP_ID, Set.of("eSSL:DOC-1")));

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
