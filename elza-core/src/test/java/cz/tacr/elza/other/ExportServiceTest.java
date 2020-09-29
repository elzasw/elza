package cz.tacr.elza.other;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import cz.tacr.elza.controller.AbstractControllerTest;
import cz.tacr.elza.ws.core.v1.ExportServiceImpl;
import cz.tacr.elza.ws.types.v1.EntityUpdates;
import cz.tacr.elza.ws.types.v1.SearchEntityUpdates;

public class ExportServiceTest extends AbstractControllerTest {

    @Autowired
    ExportServiceImpl exportServiceImpl;

    @Test
    public void searchEntityUpdatesTest() {
        EntityUpdates updates = exportServiceImpl.searchEntityUpdates(new SearchEntityUpdates());
        assertNotNull(updates);
        assertTrue(updates.getFromTrans().equals("0"));
        assertTrue(updates.getToTrans().equals("3"));
        assertTrue(updates.getEntityIds().getIdentifier().size() == 3);
    }

}
