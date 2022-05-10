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
import java.util.Optional;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import cz.tacr.elza.controller.vo.ApAccessPointVO;
import cz.tacr.elza.controller.vo.ApPartFormVO;
import cz.tacr.elza.controller.vo.ApPartVO;
import cz.tacr.elza.controller.vo.RulPartTypeVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemStringVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemVO;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.service.PartService;
import cz.tacr.elza.test.ApiException;
import cz.tacr.elza.test.controller.vo.DeleteAccessPointDetail;
import cz.tacr.elza.test.controller.vo.DeleteAccessPointsDetail;

public class AccessPointControllerTest extends AbstractControllerTest {

    @Autowired
    PartService partService;
    
    @Autowired
    ApStateRepository stateRepository; 
    
    @Autowired
    ApAccessPointRepository apRepository;

    @Test
    public void deleteAccessPointsTest() throws ApiException {

        List<ApAccessPoint> aps = apRepository.findAll();

        ApAccessPoint ap1 = apRepository.findApAccessPointByUuid("9f783015-b9af-42fc-bff4-11ff57cdb072");
        assertNotNull(ap1);
        List<ApPart> parts = partService.findPartsByAccessPoint(ap1);
        assertTrue(parts.size() == 3);

        ApAccessPoint ap2 = apRepository.findApAccessPointByUuid("c4b13fa0-89a2-44a2-954f-e281934c3dcf");
        assertNotNull(ap2);
        parts = partService.findPartsByAccessPoint(ap2);
        assertTrue(parts.size() == 3);

        DeleteAccessPointsDetail deleteAccessPointsDetail = new DeleteAccessPointsDetail();
        List<String> uuids = Arrays.asList(ap1.getUuid(), ap2.getUuid());
        deleteAccessPointsDetail.setIds(uuids);

        accesspointsApi.deleteAccessPoints(deleteAccessPointsDetail);

        parts = partService.findPartsByAccessPoint(ap1);
        assertTrue(parts.size() == 0);

        parts = partService.findPartsByAccessPoint(ap2);
        assertTrue(parts.size() == 0);
    }

    @Test
    public void deleteAccessPointTest() throws ApiException {

        ApAccessPoint ap1 = apRepository.findApAccessPointByUuid("9f783015-b9af-42fc-bff4-11ff57cdb072");
        assertNotNull(ap1);
        List<ApPart> parts = partService.findPartsByAccessPoint(ap1);
        assertTrue(parts.size() == 3);

        ApAccessPoint ap2 = apRepository.findApAccessPointByUuid("c4b13fa0-89a2-44a2-954f-e281934c3dcf");
        assertNotNull(ap2);
        parts = partService.findPartsByAccessPoint(ap2);
        assertTrue(parts.size() == 3);

        DeleteAccessPointDetail deleteAPDetail = new DeleteAccessPointDetail();
        deleteAPDetail.setReplacedBy(ap2.getAccessPointId().toString());

        accesspointsApi.deleteAccessPoint(ap1.getAccessPointId().toString(), deleteAPDetail);

        Optional<ApState> state = stateRepository.findById(ap1.getAccessPointId());
        assertNotNull(state);
        assertNotNull(state.get().getReplacedBy());
        assertEquals(state.get().getReplacedBy().getAccessPointId(), ap2.getAccessPointId());

        parts = partService.findPartsByAccessPoint(ap1);
        assertTrue(parts.size() == 0);

        parts = partService.findPartsByAccessPoint(ap2);
        assertTrue(parts.size() == 3);
    }
    
    @Test
    public void deleteAccessPointCopyAllTest() throws ApiException {

        ApAccessPoint ap1 = apRepository.findApAccessPointByUuid("9f783015-b9af-42fc-bff4-11ff57cdb072");
        assertNotNull(ap1);
        List<ApPart> parts = partService.findPartsByAccessPoint(ap1);
        assertTrue(parts.size() == 3);

        ApAccessPoint ap2 = apRepository.findApAccessPointByUuid("c4b13fa0-89a2-44a2-954f-e281934c3dcf");
        assertNotNull(ap2);
        parts = partService.findPartsByAccessPoint(ap2);
        assertTrue(parts.size() == 3);

        DeleteAccessPointDetail deleteAPDetail = new DeleteAccessPointDetail();
        deleteAPDetail.setReplacedBy(ap2.getAccessPointId().toString());
        deleteAPDetail.setReplaceType(DeleteAccessPointDetail.ReplaceTypeEnum.COPY_ALL);

        accesspointsApi.deleteAccessPoint(ap1.getAccessPointId().toString(), deleteAPDetail);

        parts = partService.findPartsByAccessPoint(ap1);
        assertTrue(parts.size() == 0);

        parts = partService.findPartsByAccessPoint(ap2);
        assertTrue(parts.size() == 6);
    }

    @Test
    public void sePreferNameRevisionTest() throws ApiException {
        ApAccessPoint ap1 = apRepository.findApAccessPointByUuid("9f783015-b9af-42fc-bff4-11ff57cdb072");
        assertNotNull(ap1);
        ApAccessPointVO apVo = this.getAccessPoint(ap1.getAccessPointId());
        // create revision
        accesspointsApi.createRevision(ap1.getAccessPointId());

        List<ApItemVO> items = new ArrayList<>();
        RulItemType nmMainItemType = itemTypeRepository.findOneByCode(ApControllerTest.NM_MAIN);
        RulItemType nmSupGenItemType = itemTypeRepository.findOneByCode(ApControllerTest.NM_SUP_GEN);
        Map<String, RulPartTypeVO> partTypes = findPartTypesMap();
        RulPartTypeVO ptName = partTypes.get(ApControllerTest.PT_NAME);

        // add new part Karel IV
        items = new ArrayList<>();
        items.add(buildApItem(nmMainItemType.getCode(), null, "Karel", null, null));
        items.add(buildApItem(nmSupGenItemType.getCode(), null, "IV", null, null));

        ApPartFormVO partFormVO = ApControllerTest.createPartFormVO(null, ptName.getCode(), null, items);

        Integer revPartId = createPart(ap1.getAccessPointId(), partFormVO);
        assertNotNull(revPartId);

        accesspointsApi.setPreferNameRevision(apVo.getId(), revPartId);

        // merge
        mergeRevision(ap1.getAccessPointId(), null);

        ApAccessPointVO apVo2 = this.getAccessPoint(ap1.getAccessPointId());
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
