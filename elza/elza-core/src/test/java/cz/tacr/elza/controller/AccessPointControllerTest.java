package cz.tacr.elza.controller;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import cz.tacr.elza.controller.vo.DeleteAccessPointDetail;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApStateEnum;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApStateRepository;

public class AccessPointControllerTest extends AbstractControllerTest {

    @Autowired
    ApAccessPointRepository accessPointRepository;
    
    @Autowired
    ApStateRepository stateRepository; 
    
    @Test
    public void deleteAccessPointTest() {

        ApState state = stateRepository.findById(2).orElse(null);

        assertTrue(state != null);
        assertTrue(state.getAccessPointId() == 2);
        assertTrue(state.getReplacedBy() == null);

        DeleteAccessPointDetail deleteAPDetail = new DeleteAccessPointDetail();
        deleteAPDetail.setReplacedBy("3");

        delete(spec -> spec.pathParam("id", 2).body(deleteAPDetail), "/api/v1/accesspoints/{id}");

        state = stateRepository.findById(2).orElse(null);

        assertTrue(state.getReplacedBy().getAccessPointId() == 3);
    }
    
}
