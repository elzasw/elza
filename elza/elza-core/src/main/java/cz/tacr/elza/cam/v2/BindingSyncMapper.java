package cz.tacr.elza.cam.v2;

import java.util.ArrayList;
import java.util.List;

import cz.tacr.cam.v2.schema.cam.EntityRecordRefXml;
import cz.tacr.cam.v2.schema.cam.EntityXml;
import cz.tacr.cam.v2.schema.cam.ExistingIssueXml;
import cz.tacr.cam.v2.schema.cam.ParticipantActivityXml;
import cz.tacr.cam.v2.schema.cam.ParticipantTypeXml;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApBindingIssue;
import cz.tacr.elza.domain.ApBindingParticipant;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.repository.ApBindingRepository;
import cz.tacr.elza.repository.ApItemRepository;
import cz.tacr.elza.repository.ApPartRepository;

public final class BindingSyncMapper {

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
        List<ApBindingIssue> rows = new ArrayList<>();
        for (ExistingIssueXml issue : entity.getIssues().getIssue()) {
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
            EntityRecordRefXml entityRef = issue.getEntityRef();
            if (entityRef != null) {
                ApBinding related = null;
                if (entityRef.getEntityId() != null) {
                    related = bindingRepository.findByValueAndExternalSystem(
                            Long.toString(entityRef.getEntityId().getValue()), extSystem);
                }
                if (related == null && entityRef.getEntityUuid() != null) {
                    related = bindingRepository.findByValueAndExternalSystem(
                            entityRef.getEntityUuid().getValue(), extSystem);
                }
                if (related != null) bi.setRelatedBinding(related);
            }
            rows.add(bi);
        }
        return rows;
    }

    public static List<ApBindingParticipant> toApBindingParticipants(ApBindingState bindingState,
                                                                     List<ParticipantActivityXml> participants) {
        if (participants == null || participants.isEmpty()) return List.of();
        List<ApBindingParticipant> rows = new ArrayList<>(participants.size());
        for (ParticipantActivityXml pa : participants) {
            ApBindingParticipant bp = new ApBindingParticipant();
            bp.setBindingState(bindingState);
            bp.setRole(mapRole(pa.getRole()));
            bp.setLastChange(pa.getLastChange().getValue());
            bp.setName(CamHelper.getExternalUserName(pa.getExternalUser()));
            // institutionCode — взять из ExternalUser, если такой геттер есть; иначе null
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
