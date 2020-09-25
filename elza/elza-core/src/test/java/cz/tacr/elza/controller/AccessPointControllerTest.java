package cz.tacr.elza.controller;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import cz.tacr.elza.controller.vo.DeleteAccessPointDetail;
import cz.tacr.elza.domain.ApState;
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

        delete(spec -> spec.pathParam("id", 2).body(deleteAPDetail), DELETE_ACCESSPOINTS_ID);

        state = stateRepository.findById(2).orElse(null);

        assertTrue(state.getReplacedBy().getAccessPointId() == 3);
    }

}
