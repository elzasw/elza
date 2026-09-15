package cz.tacr.elza.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.api.ApExternalSystemType;
import cz.tacr.elza.controller.vo.ApExternalSystemVO;
import cz.tacr.elza.controller.vo.ApScopeVO;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.repository.ApBindingRepository;
import cz.tacr.elza.repository.ApBindingStateRepository;
import cz.tacr.elza.service.ExternalSystemService;

/**
 * Import of access points with external ids (&lt;eid&gt;).
 *
 * External id is stored as {@link ApBinding} + {@link ApBindingState}. Binding is shared
 * between all states of the same (value, external system) pair, therefore it has to be
 * either paired with the existing one or persisted before its state is saved.
 */
public class DataExchangeApEidTest extends AbstractControllerTest {

    private final static String AP_EID_XML = "ap-eid-import.xml";

    /** External system code used in {@link #AP_EID_XML}. */
    private final static String EXT_SYSTEM_CODE = "TSTEID";

    private final static String EID_VALUE_1 = "1001";

    private final static String EID_VALUE_2 = "1002";

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ExternalSystemService externalSystemService;

    @Autowired
    private ApBindingRepository bindingRepository;

    @Autowired
    private ApBindingStateRepository bindingStateRepository;

    @Test
    public void importApWithExternalIdTest() {
        ApScopeVO scope = createScope();
        Integer extSystemId = createApExternalSystem(scope);
        File file = getResourceFile(AP_EID_XML);

        // first import creates both bindings and their states
        importXmlFile(null, scope.getId(), file);

        ApBinding binding1 = findBinding(extSystemId, EID_VALUE_1);
        ApBinding binding2 = findBinding(extSystemId, EID_VALUE_2);
        assertNotNull(binding1);
        assertNotNull(binding2);

        Integer apId1 = activeStateApId(binding1);
        Integer apId2 = activeStateApId(binding2);
        assertNotNull(apId1);
        assertNotNull(apId2);

        // second import pairs APs by external id, existing bindings must be reused
        importXmlFile(null, scope.getId(), file);

        assertEquals(binding1.getBindingId(), findBinding(extSystemId, EID_VALUE_1).getBindingId());
        assertEquals(binding2.getBindingId(), findBinding(extSystemId, EID_VALUE_2).getBindingId());
        assertEquals(apId1, activeStateApId(binding1));
        assertEquals(apId2, activeStateApId(binding2));
    }

    private Integer createApExternalSystem(ApScopeVO scope) {
        ApExternalSystemVO vo = new ApExternalSystemVO();
        vo.setCode(EXT_SYSTEM_CODE);
        vo.setName(EXT_SYSTEM_CODE);
        vo.setUrl("http://localhost/unused");
        vo.setApiKeyId("k");
        vo.setApiKeyValue("v");
        vo.setType(ApExternalSystemType.CAM_V2);
        vo.setScopeId(scope.getId());
        return createExternalSystem(vo).getId();
    }

    private ApBinding findBinding(Integer extSystemId, String value) {
        return txExec(() -> {
            ApExternalSystem ext = externalSystemService.getExternalSystemInternal(extSystemId);
            return bindingRepository.findByValueAndExternalSystem(value, ext);
        });
    }

    /** Access point of the single active state of given binding. */
    private Integer activeStateApId(ApBinding binding) {
        return txExec(() -> {
            List<ApBindingState> states = bindingStateRepository.findByBindings(List.of(binding));
            assertEquals(1, states.size());
            return states.get(0).getAccessPointId();
        });
    }

    private <T> T txExec(Supplier<T> work) {
        return new TransactionTemplate(transactionManager).execute(tx -> work.get());
    }
}
