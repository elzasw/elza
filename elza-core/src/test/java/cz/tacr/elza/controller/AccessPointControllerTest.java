package cz.tacr.elza.controller;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cz.tacr.elza.service.AccessPointItemService;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import cz.tacr.elza.controller.vo.ApAccessPointVO;
import cz.tacr.elza.controller.vo.ApPartFormVO;
import cz.tacr.elza.controller.vo.ApPartVO;
import cz.tacr.elza.controller.vo.CreatedPartVO;
import cz.tacr.elza.controller.vo.RulPartTypeVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemStringVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemVO;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApItemRepository;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.service.PartService;
import cz.tacr.elza.test.ApiException;
import cz.tacr.elza.test.controller.vo.CopyAccessPointDetail;
import cz.tacr.elza.test.controller.vo.DeleteAccessPointDetail;
import cz.tacr.elza.test.controller.vo.DeleteAccessPointsDetail;
import cz.tacr.elza.test.controller.vo.EntityRef;
import cz.tacr.elza.test.controller.vo.ReplaceType;

public class AccessPointControllerTest extends AbstractControllerTest {

    @Autowired
    PartService partService;

    @Autowired
    ApItemRepository itemRepository;

    @Autowired
    ApStateRepository stateRepository;

    @Autowired
    ApAccessPointRepository apRepository;

    @Autowired
    AccessPointItemService itemService;

    @Test
    public void copyAccessPointsTest() throws ApiException {

        long count = apRepository.count();
        assertTrue(count == 3);
        ApAccessPoint ap = apRepository.findAccessPointByUuid("9f783015-b9af-42fc-bff4-11ff57cdb072");
        assertNotNull(ap);
        List<ApPart> parts = partService.findPartsByAccessPoint(ap);
        assertTrue(parts.size() == 3);
        List<ApItem> items = itemService.findValidItemsByAccessPoint(ap);
        assertTrue(items.size() == 8);

        // let's delete the last part
        List<ApItem> itemsSkip = itemService.findValidItemsByPartId(parts.get(parts.size() - 1).getPartId());
        List<Integer> skipItems = itemsSkip.stream().map(p -> p.getItemId()).collect(Collectors.toList());

        CopyAccessPointDetail copyAccessPointDetail = new CopyAccessPointDetail();
        copyAccessPointDetail.setScope(SCOPE_GLOBAL);
        copyAccessPointDetail.setReplace(true);
        copyAccessPointDetail.setSkipItems(skipItems);

        EntityRef entityRef = accesspointsApi.copyAccessPoint(ap.getUuid(), copyAccessPointDetail);
        count = apRepository.count();
        assertTrue(count == 4); // +1

        ApAccessPoint copyAp = apRepository.findAccessPointByUuid(entityRef.getId());
        assertNotNull(copyAp);
        List<ApPart> copyParts = partService.findPartsByAccessPoint(copyAp);
        assertTrue(copyParts.size() == 2); // -1
        List<ApItem> copyItems = itemService.findValidItemsByAccessPoint(copyAp);
        assertTrue(copyItems.size() == 5); // -3
    }

    @Test
    public void deleteAccessPointsTest() throws ApiException {

        ApAccessPoint ap1 = apRepository.findAccessPointByUuid("9f783015-b9af-42fc-bff4-11ff57cdb072");
        assertNotNull(ap1);
        List<ApPart> parts = partService.findPartsByAccessPoint(ap1);
        assertTrue(parts.size() == 3);

        ApAccessPoint ap2 = apRepository.findAccessPointByUuid("c4b13fa0-89a2-44a2-954f-e281934c3dcf");
        assertNotNull(ap2);
        parts = partService.findPartsByAccessPoint(ap2);
        assertTrue(parts.size() == 3);

        DeleteAccessPointsDetail deleteAccessPointsDetail = new DeleteAccessPointsDetail();
        List<String> uuids = Arrays.asList(ap1.getUuid(), ap2.getUuid());
        deleteAccessPointsDetail.setIds(uuids);

        accesspointsApi.deleteAccessPoints(deleteAccessPointsDetail);

        ApAccessPointVO ap1Vo = getAccessPoint(ap1.getAccessPointId().toString());
        assertTrue(ap1Vo.isInvalid());
        assertTrue(ap1Vo.getParts().size() == 3);

        ApAccessPointVO ap2Vo = getAccessPoint(ap1.getAccessPointId().toString());
        assertTrue(ap2Vo.isInvalid());
        assertTrue(ap2Vo.getParts().size() == 3);
    }

    @Test
    public void deleteAccessPointTest() throws ApiException {

        ApAccessPoint ap1 = apRepository.findAccessPointByUuid("9f783015-b9af-42fc-bff4-11ff57cdb072");
        assertNotNull(ap1);
        List<ApPart> parts = partService.findPartsByAccessPoint(ap1);
        assertTrue(parts.size() == 3);

        ApAccessPoint ap2 = apRepository.findAccessPointByUuid("c4b13fa0-89a2-44a2-954f-e281934c3dcf");
        assertNotNull(ap2);
        parts = partService.findPartsByAccessPoint(ap2);
        assertTrue(parts.size() == 3);

        DeleteAccessPointDetail deleteAPDetail = new DeleteAccessPointDetail();
        deleteAPDetail.setReplacedBy(ap2.getAccessPointId().toString());

        accesspointsApi.accessPointDeleteAccessPoint(ap1.getAccessPointId().toString(), deleteAPDetail);

        // check if deleted
        ApAccessPointVO apInfo = this.getAccessPoint(ap1.getAccessPointId());
        assertNotNull(apInfo);
        assertTrue(apInfo.isInvalid());
        assertEquals(apInfo.getReplacedById(), ap2.getAccessPointId());

        assertEquals(apInfo.getParts().size(), 3);

        parts = partService.findPartsByAccessPoint(ap2);
        assertTrue(parts.size() == 3);

        // try to restore AP
        accesspointsApi.restoreAccessPoint(ap1.getAccessPointId().toString());
        apInfo = this.getAccessPoint(ap1.getAccessPointId());
        assertNotNull(apInfo);
        assertTrue(!apInfo.isInvalid());
        assertNull(apInfo.getReplacedById());
        assertEquals(apInfo.getStateApproval(), ApState.StateApproval.NEW);
    }

    @Test
    public void deleteAccessPointCopyAllTest() throws ApiException {

        ApAccessPoint ap1 = apRepository.findAccessPointByUuid("9f783015-b9af-42fc-bff4-11ff57cdb072");
        assertNotNull(ap1);
        List<ApPart> parts = partService.findPartsByAccessPoint(ap1);
        assertTrue(parts.size() == 3);

        ApAccessPoint ap2 = apRepository.findAccessPointByUuid("c4b13fa0-89a2-44a2-954f-e281934c3dcf");
        assertNotNull(ap2);
        parts = partService.findPartsByAccessPoint(ap2);
        assertTrue(parts.size() == 3);

        DeleteAccessPointDetail deleteAPDetail = new DeleteAccessPointDetail();
        deleteAPDetail.setReplacedBy(ap2.getAccessPointId().toString());
        deleteAPDetail.setReplaceType(ReplaceType.COPY_ALL);

        accesspointsApi.accessPointDeleteAccessPoint(ap1.getAccessPointId().toString(), deleteAPDetail);

        ApAccessPointVO apInfo = this.getAccessPoint(ap1.getAccessPointId());
        assertNotNull(apInfo);
        assertTrue(apInfo.isInvalid());
        assertEquals(apInfo.getReplacedById(), ap2.getAccessPointId());
        assertEquals(apInfo.getParts().size(), 3);

        ApAccessPointVO apInfo2 = this.getAccessPoint(ap2.getAccessPointId());
        assertEquals(apInfo2.getParts().size(), 6);
    }

    @Test
    public void setPreferNameRevisionTest() throws ApiException, InterruptedException {
        ApAccessPoint ap1 = apRepository.findAccessPointByUuid("9f783015-b9af-42fc-bff4-11ff57cdb072");
        assertNotNull(ap1);
        ApAccessPointVO apVo = this.getAccessPoint(ap1.getAccessPointId());

        // create revision
        accesspointsApi.createRevision(ap1.getAccessPointId());

        RulItemType nmMainItemType = itemTypeRepository.findOneByCode(ApControllerTest.NM_MAIN);
        RulItemType nmSupGenItemType = itemTypeRepository.findOneByCode(ApControllerTest.NM_SUP_GEN);
        Map<String, RulPartTypeVO> partTypes = findPartTypesMap();
        RulPartTypeVO ptName = partTypes.get(ApControllerTest.PT_NAME);

        // add new part Karel IV
        List<ApItemVO> items = new ArrayList<>();
        items.add(buildApItem(nmMainItemType.getCode(), null, "Karel", null, null));
        items.add(buildApItem(nmSupGenItemType.getCode(), null, "IV", null, null));

        ApPartFormVO partFormVO = ApControllerTest.createPartFormVO(null, ptName.getCode(), null, items);

        CreatedPartVO createdPart = createPart(ap1.getAccessPointId(), partFormVO);
        Integer revPartId = createdPart.getPartId();
        assertNotNull(revPartId);

        accesspointsApi.accessPointSetPreferNameRevision(apVo.getId(), revPartId, null);

        // merge
        mergeRevision(ap1.getAccessPointId(), null);

        ApAccessPointVO apVo2;
        do {
            apVo2 = getAccessPoint(ap1.getAccessPointId());
            assertNotNull(apVo2);
            if (StringUtils.equals("Karel (IV)", apVo2.getName())) {
                break;
            }
            counter("Čekání na validaci ap kvůli změně položek hlavního jména");
            Thread.sleep(100);
        } while (true);

        // check preferred part
        ApPartVO prefPart = null;
        for (ApPartVO partVo : apVo2.getParts()) {
            if (partVo.getId().equals(apVo2.getPreferredPart())) {
                prefPart = partVo;
            }
        }
        assertNotNull(prefPart);
        assertEquals(prefPart.getValue(), "Karel (IV)");
        assertEquals(prefPart.getItems().size(), 2);
        for (ApItemVO item : prefPart.getItems()) {
            if (item.getTypeId().equals(nmMainItemType.getItemTypeId())) {
                assertNull(item.getSpecId());
                ApItemStringVO stringVo = (ApItemStringVO) item;
                assertEquals(stringVo.getValue(), "Karel");
            } else if (item.getTypeId().equals(nmSupGenItemType.getItemTypeId())) {
                assertNull(item.getSpecId());
                ApItemStringVO stringVo = (ApItemStringVO) item;
                assertEquals(stringVo.getValue(), "IV");
            } else {
                fail("Unexpected item");
            }
        }
    }
}
