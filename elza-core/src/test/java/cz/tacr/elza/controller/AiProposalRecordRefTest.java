package cz.tacr.elza.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.controller.vo.ApAccessPointVO;
import cz.tacr.elza.controller.vo.ApScopeVO;
import cz.tacr.elza.controller.vo.ArrFundVersionVO;
import cz.tacr.elza.controller.vo.UsrPermissionVO;
import cz.tacr.elza.controller.vo.UsrUserVO;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.domain.AiConversation;
import cz.tacr.elza.domain.AiExternalSystem;
import cz.tacr.elza.domain.AiRequest;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.repository.AiConversationRepository;
import cz.tacr.elza.repository.AiExternalSystemRepository;
import cz.tacr.elza.repository.AiProposalDecisionRepository;
import cz.tacr.elza.repository.AiRequestRepository;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.repository.ApTypeRepository;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.test.controller.vo.DataType;
import cz.tacr.elza.test.controller.vo.Fund;
import io.restassured.path.json.JsonPath;

/**
 * An AI proposal referencing an entity the description write would refuse
 * (a class the item specification does not allow, a scope not connected to the
 * fund) is shown blocked with the write's own error, before the user confirms
 * it; applying it writes nothing (bug 10244).
 *
 * <p>The check runs while the proposals are rendered, also inside the
 * transaction of an apply that has just written a valid change. A violation
 * must therefore come back as a result, not as an exception crossing a
 * transactional proxy — that would mark the apply's transaction rollback-only.
 */
public class AiProposalRecordRefTest extends AbstractControllerTest {

    private static final String APPLY_URL = REST_CONTROLLER_URL + "/aiprovider/request/{id}/proposal/apply";
    private static final String CONVERSATION_URL = REST_CONTROLLER_URL + "/aiprovider/conversation/{id}";

    private static final String USR_NAME = "ai-proposal-user";
    private static final String USR_PSWD = "ai-proposal-user";

    private static final String SRD_ENTITY_ROLE_2 = "SRD_ENTITY_ROLE_2";

    private static final String CHANGES = "blocks[0].nodes[0].changes";

    private static final String KEY_VALID = "0/0/0";
    private static final String KEY_WRONG_CLASS = "0/0/1";

    @Autowired
    private ApStateRepository apStateRepository;
    @Autowired
    private ApTypeRepository apTypeRepository;
    @Autowired
    private ScopeRepository scopeRepository;
    @Autowired
    private AiExternalSystemRepository aiExternalSystemRepository;
    @Autowired
    private AiConversationRepository aiConversationRepository;
    @Autowired
    private AiRequestRepository aiRequestRepository;
    @Autowired
    private AiProposalDecisionRepository decisionRepository;

    @Test
    public void entityOfDisallowedClassOrScopeIsBlockedBeforeConfirmation() {
        Fund fund = createFund("AI proposal record ref", "aiProposalRecordRef");
        ArrFundVersionVO fundVersion = getOpenVersion(fund);
        List<ArrNodeVO> nodes = createLevels(fundVersion);
        Integer nodeId = nodes.get(1).getId();

        List<ApAccessPointVO> records = findRecord(null, null, null, null, null);
        Integer validApId = records.get(0).getId();
        Integer wrongClassApId = records.get(1).getId();
        Integer wrongScopeApId = records.get(2).getId();
        ApScopeVO otherScope = createScope();
        new TransactionTemplate(tm).executeWithoutResult(status -> {
            // SRD_ENTITY_ROLE_1 allows persons and party groups only
            ApState wrongClass = apStateRepository.findLastByAccessPointId(wrongClassApId);
            wrongClass.setApType(apTypeRepository.findApTypeByCode("GEO"));
            // the fund is connected to the global scope only
            ApState wrongScope = apStateRepository.findLastByAccessPointId(wrongScopeApId);
            wrongScope.setScope(scopeRepository.findById(otherScope.getId()).orElseThrow());
        });

        // The proposals update an existing entity role: SIMPLE-DEV does not offer
        // SRD_ENTITY_ROLE on any level, so a proposal to add one is blocked earlier.
        descitemsApi.descItemCreateDescItem(fundVersion.getId(),
                buildNodeItem(SRD_ENTITY_ROLE, SRD_ENTITY_ROLE_1, DataType.RECORD_REF, records.get(0),
                              nodes.get(1), null));
        helperTestService.waitForWorkers();
        Integer itemObjectId = findRoles(nodeId).get(0).itemObjectId();

        // proposals are owned by a real user (the built-in admin has no usr_user row)
        UsrUserVO user = createUser(records.get(0).getId(), USR_NAME, USR_PSWD);
        UsrPermissionVO adminPerm = new UsrPermissionVO();
        adminPerm.setPermission(Permission.ADMIN);
        addUserPermission(user.getId(), Arrays.asList(adminPerm));
        login(USR_NAME, USR_PSWD);

        AiRequest request = createRequestWithProposals(user.getId(), nodeId, itemObjectId, List.of(
                new ProposedRole(SRD_ENTITY_ROLE_2, validApId),
                new ProposedRole(SRD_ENTITY_ROLE_1, wrongClassApId),
                new ProposedRole(SRD_ENTITY_ROLE_1, wrongScopeApId)));

        // shown before any decision
        JsonPath conversation = get(spec -> spec.pathParam("id", request.getAiConversationId()), CONVERSATION_URL)
                .jsonPath();
        String prefix = "requests[0].";
        assertEquals("PROPOSED", conversation.getString(prefix + CHANGES + "[0].state"),
                () -> String.valueOf(conversation.getMap(prefix + CHANGES + "[0]")));
        assertWrongClass(conversation, prefix + CHANGES + "[1]");
        assertWrongScope(conversation, prefix + CHANGES + "[2]", otherScope);

        // applying a blocked change writes nothing and records no decision
        JsonPath afterBlocked = apply(request.getAiRequestId(), KEY_WRONG_CLASS);
        assertWrongClass(afterBlocked, CHANGES + "[1]");
        assertTrue(decisionRepository.findByAiRequestIdAndChangeKey(request.getAiRequestId(), KEY_WRONG_CLASS)
                .isEmpty());
        assertEquals(List.of(new Role(itemObjectId, SRD_ENTITY_ROLE_1, validApId)), findRoles(nodeId));

        // applying the valid change renders the blocked ones in the same transaction
        JsonPath afterValid = apply(request.getAiRequestId(), KEY_VALID);
        assertEquals("APPLIED", afterValid.getString(CHANGES + "[0].state"));
        assertWrongClass(afterValid, CHANGES + "[1]");
        assertWrongScope(afterValid, CHANGES + "[2]", otherScope);
        assertEquals(List.of(new Role(itemObjectId, SRD_ENTITY_ROLE_2, validApId)), findRoles(nodeId));
    }

    private record ProposedRole(String specCode, Integer accessPointId) {
    }

    private record Role(Integer itemObjectId, String specCode, Integer accessPointId) {
    }

    private void assertWrongClass(final JsonPath json, final String change) {
        assertEquals("BLOCKED", json.getString(change + ".state"));
        assertNull(json.get(change + ".blockedReason"));
        assertEquals("RegistryCode", json.getString(change + ".blockedError.type"));
        assertEquals("FOREIGN_ENTITY_INVALID_SUBTYPE", json.getString(change + ".blockedError.code"));
        Map<String, Object> properties = json.getMap(change + ".blockedError.properties");
        assertNotNull(properties.get("apName"));
        assertNotNull(properties.get("apTypeName"));
        assertTrue(((String) properties.get("itemName")).contains(":"), "item name carries the spec");
    }

    private void assertWrongScope(final JsonPath json, final String change, final ApScopeVO otherScope) {
        assertEquals("BLOCKED", json.getString(change + ".state"));
        assertNull(json.get(change + ".blockedReason"));
        assertEquals("INVALID_ENTITY_SCOPE", json.getString(change + ".blockedError.code"));
        Map<String, Object> properties = json.getMap(change + ".blockedError.properties");
        assertNotNull(properties.get("apName"));
        assertEquals(otherScope.getName(), properties.get("scopeName"));
        assertFalse(properties.containsKey("dataId"), "no data exists before the write");
    }

    private JsonPath apply(final Integer requestId, final String changeKey) {
        return post(spec -> spec.pathParam("id", requestId).body(Map.of("changeKey", changeKey)), APPLY_URL)
                .jsonPath();
    }

    /** A finished request whose output proposes one update of the entity role per proposed role. */
    private AiRequest createRequestWithProposals(final Integer userId, final Integer nodeId,
                                                 final Integer itemObjectId, final List<ProposedRole> roles) {
        StringBuilder changes = new StringBuilder();
        for (ProposedRole role : roles) {
            if (changes.length() > 0) {
                changes.append(',');
            }
            changes.append("{\"reason\":\"role\",\"operations\":[{\"kind\":\"updateItem\",")
                    .append("\"itemObjectId\":").append(itemObjectId).append(",\"newItem\":{")
                    .append("\"type\":\"").append(SRD_ENTITY_ROLE).append("\",")
                    .append("\"spec\":\"").append(role.specCode()).append("\",")
                    .append("\"accessPointId\":").append(role.accessPointId())
                    .append("}}]}");
        }
        String output = "[{\"objectType\":\"elza.nodeUpdateProposals\",\"data\":{\"proposals\":[{\"nodeId\":"
                + nodeId + ",\"changes\":[" + changes + "]}]}}]";

        return new TransactionTemplate(tm).execute(status -> {
            AiExternalSystem externalSystem = new AiExternalSystem();
            externalSystem.setCode("AI_TEST");
            externalSystem.setName("AI test");
            externalSystem = aiExternalSystemRepository.save(externalSystem);

            Date now = new Date();
            AiConversation conversation = new AiConversation();
            conversation.setExternalSystemId(externalSystem.getExternalSystemId());
            conversation.setUserId(userId);
            conversation.setTitle("AI proposal record ref");
            conversation.setCreateDate(now);
            conversation.setLastChangeDate(now);
            conversation = aiConversationRepository.save(conversation);

            AiRequest request = new AiRequest();
            request.setAiConversationId(conversation.getAiConversationId());
            request.setRequestId("test-request");
            request.setTaskType("describe");
            request.setState("done");
            request.setOutput(output);
            request.setCreateDate(now);
            request.setFinishDate(now);
            return aiRequestRepository.save(request);
        });
    }

    /** Open entity-role items of the node. */
    private List<Role> findRoles(final Integer nodeId) {
        Integer itemTypeId = itemTypeRepository.findOneByCode(SRD_ENTITY_ROLE).getItemTypeId();
        return new TransactionTemplate(tm).execute(status -> descItemService
                .findByNodeIdsAndDeleteChangeIsNull(List.of(nodeId), List.of(itemTypeId)).stream()
                .map(item -> new Role(item.getDescItemObjectId(),
                        itemSpecRepository.findById(item.getItemSpecId()).orElseThrow().getCode(),
                        ((ArrDataRecordRef) HibernateUtils.unproxy(item.getData())).getRecordId()))
                .toList());
    }
}
