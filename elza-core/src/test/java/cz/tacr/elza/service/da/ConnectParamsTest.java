package cz.tacr.elza.service.da;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/** The parameters of an action are stored with it; those stored before a field was added still read. */
public class ConnectParamsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void storedBeforeTheFileplanOption_readsWithoutIt() throws Exception {
        DaService.ConnectParams params = objectMapper.readValue(
                "{\"nodeId\":5,\"changeId\":null,\"levelViewId\":7}", DaService.ConnectParams.class);

        assertEquals(5, params.nodeId());
        assertEquals(7, params.levelViewId());
        assertNull(params.fileplanAsRoot());
    }

    @Test
    void fileplanOption_roundTrips() throws Exception {
        String json = objectMapper.writeValueAsString(new DaService.ConnectParams(5, null, null, true));

        assertTrue(objectMapper.readValue(json, DaService.ConnectParams.class).fileplanAsRoot());
    }
}
