package cz.tacr.elza.ws;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import cz.tacr.elza.controller.AbstractControllerTest;
import cz.tacr.elza.dataexchange.output.writer.xml.XmlNameConsts;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApChangeRepository;
import cz.tacr.elza.test.ApiException;
import cz.tacr.elza.test.controller.vo.DeleteAccessPointDetail;
import cz.tacr.elza.ws.core.v1.ExportRequestException;
import cz.tacr.elza.ws.core.v1.ExportServiceImpl;
import cz.tacr.elza.ws.types.v1.EntitiesRequest;
import cz.tacr.elza.ws.types.v1.EntityUpdates;
import cz.tacr.elza.ws.types.v1.ExportRequest;
import cz.tacr.elza.ws.types.v1.ExportResponseData;
import cz.tacr.elza.ws.types.v1.IdentifierList;
import cz.tacr.elza.ws.types.v1.SearchEntityUpdates;

public class ExportServiceTest extends AbstractControllerTest {

    @Autowired
    ExportServiceImpl exportServiceImpl;

    @Autowired
    ApChangeRepository changeRepository;

    @Autowired
    ApAccessPointRepository apRepository;

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

    @Test
    public void exportEntityTest() throws ApiException {
        // try to export real entity
        ExportRequest er = new ExportRequest();
        er.setRequiredFormat(XmlNameConsts.SCHEMA_URI);
        EntitiesRequest ents = new EntitiesRequest();
        er.setEntities(ents);
        IdentifierList idents = new IdentifierList();
        // UUID z institution-import.xml
        idents.getIdentifier().add("b50776b9-8d76-4688-ad15-9358b59d057b");
        ents.setIdentifiers(idents);

        // institutions are loaded
        ExportResponseData expData = exportServiceImpl.exportData(er);

        assertNotNull(expData);

        // try to export nonexisting entity
        idents = new IdentifierList();
        // UUID z institution-import.xml
        idents.getIdentifier().add("11111111-1111-1111-1111-111111111111");
        ents.setIdentifiers(idents);
        try {
            expData = exportServiceImpl.exportData(er);
            fail();
        } catch (ExportRequestException ere) {
            // expected exception
        }

        // try to export deleted entity
        ApAccessPoint ap1 = apRepository.findAccessPointByUuid("9f783015-b9af-42fc-bff4-11ff57cdb072");
        assertNotNull(ap1);

        DeleteAccessPointDetail deleteAPDetail = new DeleteAccessPointDetail();
        accesspointsApi.accessPointDeleteAccessPoint(ap1.getAccessPointId().toString(), deleteAPDetail);

        idents = new IdentifierList();
        idents.getIdentifier().add("9f783015-b9af-42fc-bff4-11ff57cdb072");
        ents.setIdentifiers(idents);
        try {
            expData = exportServiceImpl.exportData(er);
            fail();
        } catch (ExportRequestException ere) {
            // expected exception
        }
    }
}
