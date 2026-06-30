package cz.tacr.elza.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.HttpClientErrorException;

import cz.tacr.elza.api.ApExternalSystemType;
import cz.tacr.elza.controller.vo.ApExternalSystemVO;
import cz.tacr.elza.controller.vo.ApScopeVO;
import cz.tacr.elza.controller.vo.SysExternalSystemVO;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApBindingIssue;
import cz.tacr.elza.domain.ApBindingParticipant;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApStateEnum;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApBindingIssueRepository;
import cz.tacr.elza.repository.ApBindingParticipantRepository;
import cz.tacr.elza.repository.ApBindingRepository;
import cz.tacr.elza.repository.ApBindingStateRepository;
import cz.tacr.elza.repository.ApChangeRepository;
import cz.tacr.elza.service.ExternalSystemService;
import cz.tacr.elza.test.controller.vo.ExtHistory;
import cz.tacr.elza.test.controller.vo.ExtIssue;
import cz.tacr.elza.test.controller.vo.ExtIssueSeverity;
import cz.tacr.elza.test.controller.vo.ExtIssueStatus;
import cz.tacr.elza.test.controller.vo.ExtParticipantRole;
import cz.tacr.elza.test.controller.vo.ExtRevision;

/**
 * Integration tests for {@link AccessPointBindingInternalController}.
 *
 * One {@code @Test} per controller method; each test walks through the
 * relevant edge cases (404 → empty → populated → boundary scenarios) on
 * one binding so fixtures stay cheap and shared.
 */
public class AccessPointBindingInternalControllerTest extends AbstractControllerTest {

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ExternalSystemService externalSystemService;

    @Autowired
    private ApAccessPointRepository accessPointRepository;

    @Autowired
    private ApChangeRepository apChangeRepository;

    @Autowired
    private ApBindingRepository bindingRepository;

    @Autowired
    private ApBindingStateRepository bindingStateRepository;

    @Autowired
    private ApBindingIssueRepository bindingIssueRepository;

    @Autowired
    private ApBindingParticipantRepository bindingParticipantRepository;

    @Test
    public void accessPointBindingGetBindingIssuesTest() {
        // 1) Unknown binding → 404.
        HttpClientErrorException notFound = assertThrows(HttpClientErrorException.class,
                () -> accesspointIntApi.accessPointBindingGetBindingIssues(999_999));
        assertEquals(HttpStatus.NOT_FOUND, notFound.getStatusCode());

        // 2) Existing binding without issues → empty list.
        Fixture fx = seedBindingWithoutChildren();
        assertTrue(accesspointIntApi.accessPointBindingGetBindingIssues(fx.bindingId).isEmpty());

        // 3) Seed:
        //    - a WARNING with no status (fresh issue, not yet evaluated)
        //    - an ERROR with status + every optional field populated,
        //      pointing at a sibling binding via relatedBinding (must
        //      stay within the same external system).
        Integer relatedBindingId = txExec(() -> {
            ApExternalSystem ext = externalSystemService.getExternalSystemInternal(fx.externalSystemId);
            return externalSystemService.createApBinding("REL-" + UUID.randomUUID(), ext, true)
                    .getBindingId();
        });
        OffsetDateTime issueFrom = OffsetDateTime.parse("2026-01-15T12:30:00Z");
        Integer errIssueId = txExec(() -> {
            ApBinding binding = bindingRepository.findById(fx.bindingId).orElseThrow();
            ApBinding related = bindingRepository.findById(relatedBindingId).orElseThrow();

            ApBindingIssue warn = new ApBindingIssue();
            warn.setBinding(binding);
            warn.setSeverity(ApBindingIssue.Severity.WARNING);
            bindingIssueRepository.save(warn);

            ApBindingIssue err = new ApBindingIssue();
            err.setBinding(binding);
            err.setUuid("issue-uuid-1");
            err.setSeverity(ApBindingIssue.Severity.ERROR);
            err.setStatus(ApBindingIssue.Status.IR_FIX_NEEDED);
            err.setRuleCode("R-001");
            err.setIssueCode("DUPLICATE_NAME");
            err.setMessage("Duplicate preferred name");
            err.setSource("CAM");
            err.setDetail("Detail text");
            err.setNote("Note text");
            err.setIssueFrom(issueFrom);
            err.setExtFromRev("rev-7");
            err.setRelatedBinding(related);
            return bindingIssueRepository.saveAndFlush(err).getBindingIssueId();
        });

        List<ExtIssue> issues = accesspointIntApi.accessPointBindingGetBindingIssues(fx.bindingId);
        assertEquals(2, issues.size());

        ExtIssue warning = issues.stream()
                .filter(i -> i.getSeverity() == ExtIssueSeverity.WARNING)
                .findFirst().orElseThrow();
        assertNull(warning.getStatus(), "Status must be absent on the wire for new issues");

        ExtIssue error = issues.stream()
                .filter(i -> i.getSeverity() == ExtIssueSeverity.ERROR)
                .findFirst().orElseThrow();
        assertEquals(errIssueId, error.getId());
        assertEquals("issue-uuid-1", error.getUuid());
        assertEquals(ExtIssueStatus.IR_FIX_NEEDED, error.getStatus());
        assertEquals("R-001", error.getRuleCode());
        assertEquals("DUPLICATE_NAME", error.getIssueCode());
        assertEquals("Duplicate preferred name", error.getMessage());
        assertEquals("CAM", error.getSource());
        assertEquals("Detail text", error.getDetail());
        assertEquals("Note text", error.getNote());
        assertNotNull(error.getIssueFrom());
        assertEquals(issueFrom.toInstant(), error.getIssueFrom().toInstant());
        assertEquals("rev-7", error.getExtFromRev());
        assertEquals(relatedBindingId, error.getRelatedBindingId());
        // relatedBindingExtValue is resolved server-side from related ApBinding.value.
        assertNotNull(error.getRelatedBindingExtValue());
        assertTrue(error.getRelatedBindingExtValue().startsWith("REL-"));
    }

    @Test
    public void accessPointBindingGetBindingHistoryTest() {
        // 1) Unknown binding → 404.
        HttpClientErrorException notFound = assertThrows(HttpClientErrorException.class,
                () -> accesspointIntApi.accessPointBindingGetBindingHistory(999_999, 0, 100));
        assertEquals(HttpStatus.NOT_FOUND, notFound.getStatusCode());

        // 2) Existing binding without revisions → empty page, complete chain.
        Fixture fx = seedBindingWithoutChildren();
        ExtHistory empty = accesspointIntApi.accessPointBindingGetBindingHistory(fx.bindingId, 0, 100);
        assertEquals(0, empty.getTotalCount());
        assertFalse(empty.getIncomplete());
        assertTrue(empty.getRevisions().isEmpty());

        // 3) Seed four revisions in creation order: A → B → C → D.
        //    D has extCreatedAt = null (exercises the createChangeAt fallback).
        //    C carries two participants stored in reverse lastChange order
        //    (must come back ASC).
        OffsetDateTime laterChange = OffsetDateTime.parse("2026-02-01T10:00:00Z");
        OffsetDateTime earlierChange = OffsetDateTime.parse("2026-02-01T09:00:00Z");
        int[] stateIds = txExec(() -> {
            ApBinding binding = bindingRepository.findById(fx.bindingId).orElseThrow();
            ApAccessPoint ap = accessPointRepository.findById(fx.accessPointId).orElseThrow();

            ApBindingState a = newBindingState(binding, ap, "rev-A", null,
                    OffsetDateTime.parse("2026-01-01T00:00:00Z"));
            ApBindingState b = newBindingState(binding, ap, "rev-B", "rev-A",
                    OffsetDateTime.parse("2026-01-02T00:00:00Z"));
            ApBindingState c = newBindingState(binding, ap, "rev-C", "rev-B",
                    OffsetDateTime.parse("2026-01-03T00:00:00Z"));
            ApBindingState d = newBindingState(binding, ap, "rev-D", "rev-B", null);

            ApBindingParticipant p1 = new ApBindingParticipant();
            p1.setBindingState(c);
            p1.setRole(ApBindingParticipant.Role.AUTHOR);
            p1.setName("Alice");
            p1.setInstitutionCode("INST-A");
            p1.setLastChange(laterChange);
            bindingParticipantRepository.save(p1);

            ApBindingParticipant p2 = new ApBindingParticipant();
            p2.setBindingState(c);
            p2.setRole(ApBindingParticipant.Role.APPROVAL);
            p2.setName("Bob");
            p2.setInstitutionCode("INST-B");
            p2.setLastChange(earlierChange);
            bindingParticipantRepository.save(p2);

            return new int[] {
                    a.getBindingStateId(), b.getBindingStateId(),
                    c.getBindingStateId(), d.getBindingStateId()
            };
        });

        // Newest first by bindingStateId DESC: D, C, B, A; complete chain.
        ExtHistory all = accesspointIntApi.accessPointBindingGetBindingHistory(fx.bindingId, 0, 100);
        assertEquals(4, all.getTotalCount());
        assertFalse(all.getIncomplete());
        assertEquals(List.of(stateIds[3], stateIds[2], stateIds[1], stateIds[0]),
                all.getRevisions().stream().map(ExtRevision::getBindingStateId).toList());

        // createChangeAt is the fallback — populated only when extCreatedAt is null (rev-D).
        ExtRevision dto_D = all.getRevisions().get(0);
        assertEquals(stateIds[3], dto_D.getBindingStateId());
        assertNull(dto_D.getExtCreatedAt());
        assertNotNull(dto_D.getCreateChangeAt(),
                "createChangeAt must be set as fallback when extCreatedAt is null");
        ExtRevision dto_C = all.getRevisions().get(1);
        assertNotNull(dto_C.getExtCreatedAt());
        assertNull(dto_C.getCreateChangeAt(),
                "createChangeAt must NOT be set when extCreatedAt is present");

        // Participants on rev-C: ASC by lastChange — Bob (earlier) before Alice.
        assertEquals(2, dto_C.getParticipants().size());
        assertEquals("Bob", dto_C.getParticipants().get(0).getName());
        assertEquals(ExtParticipantRole.APPROVAL, dto_C.getParticipants().get(0).getRole());
        assertEquals("INST-B", dto_C.getParticipants().get(0).getInstitutionCode());
        assertEquals("Alice", dto_C.getParticipants().get(1).getName());
        assertEquals(ExtParticipantRole.AUTHOR, dto_C.getParticipants().get(1).getRole());

        // Pagination — disjoint pages of size 2 cover all four revisions.
        ExtHistory firstPage = accesspointIntApi.accessPointBindingGetBindingHistory(fx.bindingId, 0, 2);
        assertEquals(4, firstPage.getTotalCount());
        assertEquals(List.of(stateIds[3], stateIds[2]),
                firstPage.getRevisions().stream().map(ExtRevision::getBindingStateId).toList());
        ExtHistory secondPage = accesspointIntApi.accessPointBindingGetBindingHistory(fx.bindingId, 2, 2);
        assertEquals(List.of(stateIds[1], stateIds[0]),
                secondPage.getRevisions().stream().map(ExtRevision::getBindingStateId).toList());

        // Server caps oversized limit (≤ 100) and applies the documented defaults
        // when offset/limit are null. Both calls must not fail and must return
        // every revision we seeded.
        assertEquals(4, accesspointIntApi.accessPointBindingGetBindingHistory(fx.bindingId, 0, 500)
                .getRevisions().size());
        assertEquals(4, accesspointIntApi.accessPointBindingGetBindingHistory(fx.bindingId, null, null)
                .getRevisions().size());

        // 4) Introduce a chain gap: a revision whose extPrevRevision points to
        //    a revision Elza does not have.
        txExec(() -> {
            ApBinding binding = bindingRepository.findById(fx.bindingId).orElseThrow();
            ApAccessPoint ap = accessPointRepository.findById(fx.accessPointId).orElseThrow();
            newBindingState(binding, ap, "rev-E", "rev-MISSING", OffsetDateTime.now());
            return null;
        });
        ExtHistory withGap = accesspointIntApi.accessPointBindingGetBindingHistory(fx.bindingId, 0, 100);
        assertEquals(5, withGap.getTotalCount());
        assertTrue(withGap.getIncomplete(),
                "incomplete=true expected: extPrevRevision points to a revision not in DB");
    }

    // ----------------------------------------------------------------------
    // fixtures
    // ----------------------------------------------------------------------

    /** Minimal fixture: scope, external system, AP, binding (no states / issues / participants). */
    private static class Fixture {
        Integer externalSystemId;
        Integer accessPointId;
        Integer bindingId;
    }

    /** Create scope + external system over REST, then AP + binding directly via repos. */
    private Fixture seedBindingWithoutChildren() {
        ApScopeVO scope = createScope();
        ApExternalSystemVO vo = new ApExternalSystemVO();
        String code = "ES-" + UUID.randomUUID();
        vo.setCode(code);
        vo.setName(code);
        vo.setUrl("http://localhost/unused");
        vo.setApiKeyId("k");
        vo.setApiKeyValue("v");
        vo.setType(ApExternalSystemType.CAM_V2);
        vo.setScopeId(scope.getId());
        SysExternalSystemVO created = createExternalSystem(vo);

        Fixture fx = new Fixture();
        fx.externalSystemId = created.getId();
        Integer[] ids = txExec(() -> {
            ApExternalSystem ext = externalSystemService.getExternalSystemInternal(fx.externalSystemId);

            ApAccessPoint ap = new ApAccessPoint();
            ap.setUuid(UUID.randomUUID().toString());
            ap.setState(ApStateEnum.OK);
            ap.setLastUpdate(LocalDateTime.now());
            ap = accessPointRepository.save(ap);

            ApBinding binding = externalSystemService.createApBinding(
                    "VAL-" + UUID.randomUUID(), ext, true);

            return new Integer[] { ap.getAccessPointId(), binding.getBindingId() };
        });
        fx.accessPointId = ids[0];
        fx.bindingId = ids[1];
        return fx;
    }

    /**
     * Persist one ApBindingState. Must be called inside a {@link #txExec}
     * block — entities passed in must be attached.
     */
    private ApBindingState newBindingState(ApBinding binding,
                                           ApAccessPoint ap,
                                           String extRevision,
                                           String extPrevRevision,
                                           OffsetDateTime extCreatedAt) {
        ApChange change = new ApChange();
        change.setChangeDate(OffsetDateTime.now());
        change.setType(ApChange.Type.AP_SYNCH);
        change = apChangeRepository.save(change);

        ApBindingState st = new ApBindingState();
        st.setBinding(binding);
        st.setAccessPoint(ap);
        st.setApExternalSystem(binding.getApExternalSystem());
        st.setExtRevision(extRevision);
        st.setExtPrevRevision(extPrevRevision);
        st.setExtUser("user-" + UUID.randomUUID());
        st.setExtCreatedAt(extCreatedAt);
        st.setCreateChange(change);
        return bindingStateRepository.saveAndFlush(st);
    }

    private <T> T txExec(Supplier<T> work) {
        return new TransactionTemplate(transactionManager).execute(tx -> work.get());
    }
}