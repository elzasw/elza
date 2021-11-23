package cz.tacr.elza.service;

import cz.tacr.elza.controller.vo.ApPartFormVO;
import cz.tacr.elza.controller.vo.RevisionState;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApRevIndex;
import cz.tacr.elza.domain.ApRevItem;
import cz.tacr.elza.domain.ApRevPart;
import cz.tacr.elza.domain.ApRevision;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ItemGroovy;
import cz.tacr.elza.domain.PartGroovy;
import cz.tacr.elza.domain.RulPartType;
import cz.tacr.elza.groovy.GroovyResult;
import cz.tacr.elza.repository.ApRevIndexRepository;
import cz.tacr.elza.repository.ApRevisionRepository;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class RevisionService {

    @Autowired
    private AccessPointDataService accessPointDataService;

    @Autowired
    private RevisionPartService revisionPartService;

    @Autowired
    private PartService partService;

    @Autowired
    private RevisionItemService revisionItemService;

    @Autowired
    private GroovyService groovyService;

    @Autowired
    private ApRevisionRepository revisionRepository;

    @Autowired
    private ApRevIndexRepository revIndexRepository;

    @Transactional
    public void createRevision(ApState state) {
        ApRevision revision = findRevisionByState(state);
        if (revision != null) {
            throw new IllegalStateException("Revize pro tento přístupový bod již existuje");
        }

        ApChange change = accessPointDataService.createChange(ApChange.Type.AP_CREATE);

        revision = new ApRevision();
        revision.setCreateChange(change);
        revision.setState(state);
        revision.setType(state.getApType());
        revision.setStateApproval(ApRevision.StateApproval.ACTIVE);
        revision.setPreferredPart(state.getAccessPoint().getPreferredPart());

        revisionRepository.save(revision);
    }

    @Transactional
    public void deleteRevision(ApState state) {
        ApRevision revision = findRevisionByState(state);
        if (revision == null) {
            throw new IllegalStateException("Pro tento přístupový bod neexistuje revize");
        }

        ApChange change = accessPointDataService.createChange(ApChange.Type.AP_DELETE);
        List<ApRevPart> parts = revisionPartService.findByRevision(revision);
        if (CollectionUtils.isNotEmpty(parts)) {
            List<ApRevItem> items = revisionItemService.findByParts(parts);
            List<ApRevIndex> indices = revIndexRepository.findByParts(parts);

            deleteRevisionIndices(indices);
            revisionItemService.deleteRevisionItems(items, change);
            revisionPartService.deleteRevisionParts(parts, change);
        }

        revision.setDeleteChange(change);
        revisionRepository.save(revision);
    }

    private void deleteRevisionIndices(List<ApRevIndex> indices) {
        if (CollectionUtils.isNotEmpty(indices)) {
            revIndexRepository.deleteAll(indices);
        }
    }

    @Transactional
    public void changeStateRevision(ApState state, RevisionState revisionState) {
        ApRevision revision = findRevisionByState(state);
        if (revision == null) {
            throw new IllegalStateException("Pro tento přístupový bod neexistuje revize");
        }

        ApRevision.StateApproval stateApproval = ApRevision.StateApproval.valueOf(revisionState.getValue());
        revision.setStateApproval(stateApproval);
        revisionRepository.save(revision);
    }

    public ApRevision findRevisionByState(ApState state) {
        return revisionRepository.findByState(state);
    }

    @Transactional
    public void createPart(ApRevision revision, ApPartFormVO apPartFormVO) {
        ApPart parentPart = apPartFormVO.getParentPartId() == null ? null : partService.getPart(apPartFormVO.getParentPartId());
        ApRevPart revParentPart = apPartFormVO.getRevParentPartId() == null ? null : revisionPartService.getPart(apPartFormVO.getParentPartId());

        if ((parentPart != null && parentPart.getParentPart() != null)
                || (revParentPart != null && revParentPart.getParentPart() != null)) {
            throw new IllegalArgumentException("Nadřazená část nesmí zároveň být podřazená část");
        }

        if (CollectionUtils.isEmpty(apPartFormVO.getItems())) {
            throw new IllegalArgumentException("Část musí mít alespoň jeden prvek popisu");
        }

        RulPartType partType = partService.getPartTypeByCode(apPartFormVO.getPartTypeCode());
        ApChange apChange = accessPointDataService.createChange(ApChange.Type.AP_CREATE);

        ApRevPart newPart = revisionPartService.createPart(partType, revision, apChange, revParentPart, parentPart);
        revisionItemService.createItems(newPart, apPartFormVO.getItems(), apChange);
        updatePartValue(newPart, revision);
    }

    private void updatePartValue(final ApRevPart part, final ApRevision revision) {
        ApState state = revision.getState();
        ApPart preferredNamePart = state.getAccessPoint().getPreferredPart();
        List<ApRevPart> childrenParts = revisionPartService.findPartsByParentPart(part.getOriginalPart());
        List<ApRevPart> revChildrenParts = revisionPartService.findPartsByRevParentPart(part);

        List<ApRevPart> parts = new ArrayList<>();
        parts.add(part);
        parts.addAll(childrenParts);
        parts.addAll(revChildrenParts);

        List<ApRevItem> items = revisionItemService.findByParts(parts);

        boolean preferred = preferredNamePart == null || Objects.equals(preferredNamePart.getPartId(), part.getPartId());
        List<PartGroovy> childParts = new ArrayList<>(childrenParts);
        List<ItemGroovy> itemGroovyList = new ArrayList<>(items);
        GroovyResult result = groovyService.processGroovy(state, part, childParts, itemGroovyList, preferred);

        revisionPartService.updatePartValue(part, result);
    }

    @Transactional
    public void deletePart(ApRevision revision, Integer partId) {
    }
}
