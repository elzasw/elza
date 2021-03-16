package cz.tacr.elza.controller;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.service.PartService;
import cz.tacr.elza.test.ApiException;
import cz.tacr.elza.test.controller.vo.DeleteAccessPointDetail;

public class AccessPointControllerTest extends AbstractControllerTest {

    @Autowired
    PartService partService;
    
    @Autowired
    ApStateRepository stateRepository; 
    
    @Autowired
    ApAccessPointRepository apRepository;

    @Test
    public void deleteAccessPointTest() throws ApiException {

        List<ApAccessPoint> aps = apRepository.findAll();

        ApAccessPoint ap1 = apRepository.findApAccessPointByUuid("9f783015-b9af-42fc-bff4-11ff57cdb072");
        assertNotNull(ap1);
        List<ApPart> parts = partService.findPartsByAccessPoint(ap1);
        assertTrue(parts.size() == 3);

        ApAccessPoint ap2 = apRepository.findApAccessPointByUuid("c4b13fa0-89a2-44a2-954f-e281934c3dcf");
        assertNotNull(ap2);
        parts = partService.findPartsByAccessPoint(ap1);
        assertTrue(parts.size() == 3);

        DeleteAccessPointDetail deleteAPDetail = new DeleteAccessPointDetail();
        deleteAPDetail.setReplacedBy(ap2.getAccessPointId().toString());

        accesspointsApi.deleteAccessPoint(ap1.getAccessPointId().toString(), deleteAPDetail);

        Optional<ApState> state = stateRepository.findById(ap1.getAccessPointId());
        assertNotNull(state);
        assertTrue(state.get().getReplacedBy().getAccessPointId().equals(ap2.getAccessPointId()));

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
        assertTrue(parts.size() == 6);

        parts = partService.findPartsByAccessPoint(ap2);
        assertTrue(parts.size() == 6);
    }

}
