package cz.tacr.elza.service;

import cz.tacr.elza.controller.vo.ApPartFormVO;
import cz.tacr.elza.controller.vo.RevisionState;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApRevIndex;
import cz.tacr.elza.domain.ApRevItem;
import cz.tacr.elza.domain.ApRevPart;
import cz.tacr.elza.domain.ApRevision;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.AccessPointItem;
import cz.tacr.elza.domain.AccessPointPart;
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
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
        ApRevPart revParentPart = apPartFormVO.getRevParentPartId() == null ? null : revisionPartService.findById(apPartFormVO.getParentPartId());

        //todo ????? parentPart
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
        //todo ????? parentPart
        //todo add child appart and apitem
        List<ApRevPart> childrenParts = revisionPartService.findPartsByParentPart(part.getOriginalPart());
        List<ApRevPart> revChildrenParts = revisionPartService.findPartsByRevParentPart(part);
        if (CollectionUtils.isEmpty(childrenParts)) {
            childrenParts = new ArrayList<>();
        }
        childrenParts.addAll(revChildrenParts);

        List<ApRevPart> parts = new ArrayList<>();
        parts.add(part);
        parts.addAll(childrenParts);

        List<ApRevItem> items = revisionItemService.findByParts(parts);

        updatePartValue(revision, part, childrenParts, items);
    }

    private void updatePartValues(final ApRevision revision) {
        //todo add child appart and apitem
        List<ApRevPart> partList = revisionPartService.findByRevision(revision);
        Map<Integer, List<ApRevItem>> itemMap = revisionItemService.findByParts(partList).stream()
                .collect(Collectors.groupingBy(ApRevItem::getPartId));

        for (ApRevPart part : partList) {
            List<ApRevPart> childrenParts = findChildrenParts(part, partList);
            List<ApRevItem> items = getItemsForParts(part, childrenParts, itemMap);

            updatePartValue(revision, part, childrenParts, items);
        }
    }

    private void updatePartValue(final ApRevision revision,
                                 final ApRevPart part,
                                 final List<ApRevPart> childrenParts,
                                 final List<ApRevItem> items) {

        boolean preferred = isPrefered(revision, part);
        List<AccessPointPart> childParts = new ArrayList<>(childrenParts);
        List<AccessPointItem> accessPointItemList = new ArrayList<>(items);
        GroovyResult result = groovyService.processGroovy(revision.getTypeId(), part, childParts, accessPointItemList, preferred);

        revisionPartService.updatePartValue(part, result);
    }

    private boolean isPrefered(final ApRevision revision, ApRevPart part) {
        if (revision.getPreferredPartId() != null) {
            return part.getOriginalPart() != null && Objects.equals(revision.getPreferredPartId(), part.getOriginalPartId());
        }
        return Objects.equals(part.getPartId(), revision.getRevPreferredPartId());
    }

    private List<ApRevPart> findChildrenParts(final ApRevPart part, final List<ApRevPart> partList) {
        List<ApRevPart> childrenParts = new ArrayList<>();
        for (ApRevPart p : partList) {
            //todo ????? parentPart
            if (p.getRevParentPart() != null && p.getRevParentPart().getPartId().equals(part.getPartId())) {
                childrenParts.add(p);
            }
        }
        return childrenParts;
    }

    private List<ApRevItem> getItemsForParts(final ApRevPart part,
                                             final List<ApRevPart> childrenParts,
                                             final Map<Integer, List<ApRevItem>> itemMap) {

        List<ApRevItem> itemsSrc = itemMap.get(part.getPartId());

        List<ApRevItem> result;
        if (CollectionUtils.isEmpty(itemsSrc)) {
            result = new ArrayList<>();
        } else {
            result = new ArrayList<>(itemsSrc);
        }

        // get items from children parts
        if (CollectionUtils.isNotEmpty(childrenParts)) {
            for (ApRevPart p : childrenParts) {
                List<ApRevItem> childItemList = itemMap.get(p.getPartId());
                if (CollectionUtils.isNotEmpty(childItemList)) {
                    result.addAll(childItemList);
                }
            }
        }

        return result;
    }

    @Transactional
    public void deletePart(ApRevision revision, Integer partId) {
        if (revision.getPreferredPartId() != null && revision.getPreferredPartId().equals(partId)) {
            throw new IllegalArgumentException("Preferované jméno nemůže být odstraněno");
        }
        ApPart apPart = partService.getPart(partId);
        ApRevPart revPart = revisionPartService.findByOriginalPart(apPart);

        //todo check children parts
//        if (partRepository.countApPartsByParentPartAndDeleteChangeIsNull(apPart) > 0) {
//            throw new IllegalArgumentException("Nelze smazat part, který má aktivní návazné party");
//        }

        if (revPart != null) {
            // smazat itemy a indexi
            if (revision.getRevPreferredPartId() != null && revision.getRevPreferredPartId().equals(revPart.getPartId())) {
                throw new IllegalArgumentException("Preferované jméno nemůže být odstraněno");
            }

            ApChange apChange = accessPointDataService.createChange(ApChange.Type.AP_DELETE);
            List<ApRevIndex> indices = revIndexRepository.findByPart(revPart);
            List<ApRevItem> items = revisionItemService.findByPart(revPart);

            deleteRevisionIndices(indices);
            revisionItemService.deleteRevisionItems(items, apChange);
        } else {
            // vytvořit nový záznam
            ApChange apChange = accessPointDataService.createChange(ApChange.Type.AP_CREATE);
            revisionPartService.createPart(revision, apChange, apPart);
        }
    }

    @Transactional
    public void deletePart(ApState state, Integer partId) {
        ApRevision revision = revisionRepository.findByState(state);
        if (revision.getRevPreferredPartId() != null && revision.getRevPreferredPartId().equals(partId)) {
            throw new IllegalArgumentException("Preferované jméno nemůže být odstraněno");
        }
        ApRevPart revPart = revisionPartService.findById(partId);

        //todo check children parts
//        if (partRepository.countApPartsByParentPartAndDeleteChangeIsNull(apPart) > 0) {
//            throw new IllegalArgumentException("Nelze smazat part, který má aktivní návazné party");
//        }

        ApChange apChange = accessPointDataService.createChange(ApChange.Type.AP_DELETE);
        List<ApRevIndex> indices = revIndexRepository.findByPart(revPart);
        List<ApRevItem> items = revisionItemService.findByPart(revPart);

        deleteRevisionIndices(indices);
        revisionItemService.deleteRevisionItems(items, apChange);
        revisionPartService.deleteRevisionPart(revPart, apChange);
    }

    @Transactional
    public void setPreferName(ApRevision revision, Integer partId) {
        StaticDataProvider sdp = StaticDataProvider.getInstance();
        RulPartType defaultPartType = sdp.getDefaultPartType();

        ApPart apPart = partService.getPart(partId);
        ApRevPart revPart = revisionPartService.findByOriginalPart(apPart);
        List<ApRevItem> revItems = revPart != null ? revisionItemService.findByPart(revPart) : null;

        if (!apPart.getPartType().getCode().equals(defaultPartType.getCode())) {
            throw new IllegalArgumentException("Preferované jméno musí být typu " + defaultPartType.getCode());
        }

        if (apPart.getParentPart() != null) {
            throw new IllegalArgumentException("Návazný part nelze změnit na preferovaný.");
        }

        if (apPart.getDeleteChange() != null || (revPart != null && CollectionUtils.isEmpty(revItems))) {
            throw new IllegalArgumentException("Smazaný part nelze označit za preferovaný");
        }

        revision.setPreferredPart(apPart);
        revision.setRevPreferredPart(revPart);
        revisionRepository.save(revision);

        updatePartValues(revision);
    }

    @Transactional
    public void setPreferName(ApState state, Integer partId) {
        ApRevision revision = revisionRepository.findByState(state);
        StaticDataProvider sdp = StaticDataProvider.getInstance();
        RulPartType defaultPartType = sdp.getDefaultPartType();

        ApRevPart revPart = revisionPartService.findById(partId);
        List<ApRevItem> revItems = revisionItemService.findByPart(revPart);

        if (!revPart.getPartType().getCode().equals(defaultPartType.getCode())) {
            throw new IllegalArgumentException("Preferované jméno musí být typu " + defaultPartType.getCode());
        }

        //todo ????? parentPart
        if (revPart.getParentPart() != null || revPart.getRevParentPart() != null) {
            throw new IllegalArgumentException("Návazný part nelze změnit na preferovaný.");
        }

        if (CollectionUtils.isEmpty(revItems)) {
            throw new IllegalArgumentException("Smazaný part nelze označit za preferovaný");
        }

        revision.setPreferredPart(null);
        revision.setRevPreferredPart(revPart);
        revisionRepository.save(revision);

        updatePartValues(revision);
    }
}
