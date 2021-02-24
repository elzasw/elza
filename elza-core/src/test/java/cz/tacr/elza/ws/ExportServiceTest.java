package cz.tacr.elza.ws;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import cz.tacr.elza.controller.AbstractControllerTest;
import cz.tacr.elza.repository.ApChangeRepository;
import cz.tacr.elza.ws.core.v1.ExportServiceImpl;
import cz.tacr.elza.ws.types.v1.EntityUpdates;
import cz.tacr.elza.ws.types.v1.SearchEntityUpdates;

public class ExportServiceTest extends AbstractControllerTest {

    @Autowired
    ExportServiceImpl exportServiceImpl;

    @Autowired
    ApChangeRepository changeRepository;

    @Test
    public void searchEntityUpdatesTest() {
        EntityUpdates updates = exportServiceImpl.searchEntityUpdates(new SearchEntityUpdates());
        int toId = changeRepository.findTop1ByOrderByChangeIdDesc().getChangeId();

        assertNotNull(updates);
        assertTrue(toId > 0);
        assertTrue(updates.getFromTrans().equals("0"));
        assertTrue(updates.getToTrans().equals(String.valueOf(toId)));
        assertTrue(updates.getEntityIds().getIdentifier().size() > 0);
    }

}
