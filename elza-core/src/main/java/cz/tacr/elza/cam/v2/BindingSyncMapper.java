package cz.tacr.elza.cam.v2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.cam.v2.schema.cam.EntityRecordRefXml;
import cz.tacr.cam.v2.schema.cam.EntityXml;
import cz.tacr.cam.v2.schema.cam.ExistingIssueXml;
import cz.tacr.cam.v2.schema.cam.ParticipantActivityXml;
import cz.tacr.cam.v2.schema.cam.ParticipantTypeXml;
import cz.tacr.cam.v2.schema.cam.UserInfoXml;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApBindingIssue;
import cz.tacr.elza.domain.ApBindingParticipant;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.repository.ApBindingRepository;
import cz.tacr.elza.repository.ApItemRepository;
import cz.tacr.elza.repository.ApPartRepository;

public final class BindingSyncMapper {

    private static final Logger log = LoggerFactory.getLogger(BindingSyncMapper.class);

    private BindingSyncMapper() {
    }

    public static List<ApBindingIssue> toApBindingIssues(EntityXml entity,
                                                         ApBinding binding,
                                                         IssueRefResolver resolver,
                                                         ApBindingRepository bindingRepository,
                                                         ApPartRepository partRepository,
                                                         ApItemRepository itemRepository) {
        if (entity.getIssues() == null || entity.getIssues().getIssue().isEmpty()) {
            return List.of();
        }
        ApExternalSystem extSystem = binding.getApExternalSystem();
        List<ExistingIssueXml> camIssues = entity.getIssues().getIssue();
        Map<String, ApBinding> relatedByValue = loadRelatedBindings(camIssues, extSystem, bindingRepository);
        List<ApBindingIssue> rows = new ArrayList<>();
        for (ExistingIssueXml issue : camIssues) {
            ApBindingIssue bi = new ApBindingIssue();
            bi.setBinding(binding);
            bi.setUuid(issue.getUuid().getValue());
            bi.setSeverity(ApBindingIssue.Severity.valueOf(issue.getSeverity().value()));
            if (issue.getStatus() != null && issue.getStatus().getResolution() != null) {
                bi.setStatus(ApBindingIssue.Status.valueOf(issue.getStatus().getResolution().value()));
            }
            bi.setRuleCode(issue.getRuleCode() != null ? issue.getRuleCode().getValue() : null);
            bi.setIssueCode(issue.getIssueCode() != null ? issue.getIssueCode().getValue() : null);
            bi.setMessage(issue.getMessage() != null ? issue.getMessage().getValue() : null);
            bi.setSource(issue.getSource() != null ? issue.getSource().getValue() : null);
            bi.setDetail(issue.getDetail() != null ? issue.getDetail().getValue() : null);
            bi.setIssueFrom(issue.getFrom() != null ? issue.getFrom().getValue() : null);
            bi.setExtFromRev(issue.getFromRev() != null ? issue.getFromRev().getValue() : null);

            if (resolver != null) {
                Integer partId = resolver.resolvePart(issue.getPartRef());
                Integer itemId = resolver.resolveItem(issue.getItemRef());
                if (partId != null) bi.setPart(partRepository.getReferenceById(partId));
                if (itemId != null) bi.setItem(itemRepository.getReferenceById(itemId));
            }
            ApBinding related = resolveRelatedBinding(issue.getEntityRef(), relatedByValue);
            if (related != null) {
                bi.setRelatedBinding(related);
            }
            rows.add(bi);
        }
        return rows;
    }

    /**
     * One batch lookup of the bindings referenced by all issues' {@code entityRef}s,
     * keyed by {@link ApBinding#getValue()} (the CAM entityId for CAM_V2, the
     * entityUuid for CAM_UUID_V2 — both live in that column).
     */
    private static Map<String, ApBinding> loadRelatedBindings(List<ExistingIssueXml> issues,
                                                              ApExternalSystem extSystem,
                                                              ApBindingRepository bindingRepository) {
        Set<String> values = new HashSet<>();
        for (ExistingIssueXml issue : issues) {
            EntityRecordRefXml ref = issue.getEntityRef();
            if (ref == null) {
                continue;
            }
            if (ref.getEntityId() != null) {
                values.add(Long.toString(ref.getEntityId().getValue()));
            }
            if (ref.getEntityUuid() != null) {
                values.add(ref.getEntityUuid().getValue());
            }
        }
        if (values.isEmpty()) {
            return Map.of();
        }
        Map<String, ApBinding> byValue = new HashMap<>();
        for (ApBinding b : bindingRepository.findByValuesAndExternalSystem(values, extSystem)) {
            byValue.put(b.getValue(), b);
        }
        return byValue;
    }

    private static ApBinding resolveRelatedBinding(EntityRecordRefXml entityRef,
                                                   Map<String, ApBinding> relatedByValue) {
        if (entityRef == null) {
            return null;
        }
        if (entityRef.getEntityId() != null) {
            ApBinding related = relatedByValue.get(Long.toString(entityRef.getEntityId().getValue()));
            if (related != null) {
                return related;
            }
        }
        if (entityRef.getEntityUuid() != null) {
            return relatedByValue.get(entityRef.getEntityUuid().getValue());
        }
        return null;
    }

    public static List<ApBindingParticipant> toApBindingParticipants(ApBindingState bindingState,
                                                                     List<ParticipantActivityXml> participants) {
        if (participants == null || participants.isEmpty()) {
            return List.of();
        }
        List<ApBindingParticipant> rows = new ArrayList<>(participants.size());
        for (ParticipantActivityXml pa : participants) {
            // externalUser is optional in the CAM schema, but ap_binding_participant.name
            // is NOT NULL — a participant the external system did not tie to a named user
            // carries no useful information, so skip it rather than abort the whole sync.
            UserInfoXml user = CamHelper.resolveUserInfo(pa.getExternalUser());
            if (user == null || user.getName() == null || user.getName().getValue() == null) {
                log.warn("Skipping CAM participant without a named external user, role: {}", pa.getRole());
                continue;
            }
            ApBindingParticipant bp = new ApBindingParticipant();
            bp.setBindingState(bindingState);
            bp.setRole(mapRole(pa.getRole()));
            bp.setLastChange(pa.getLastChange().getValue());
            bp.setName(user.getName().getValue());
            if (user.getInstitution() != null) {
                bp.setInstitutionCode(user.getInstitution().getValue());
            }
            rows.add(bp);
        }
        return rows;
    }

    private static ApBindingParticipant.Role mapRole(ParticipantTypeXml role) {
        return switch (role) {
            case EDITOR   -> ApBindingParticipant.Role.AUTHOR;
            case APPROVER -> ApBindingParticipant.Role.APPROVAL;
        };
    }
}
