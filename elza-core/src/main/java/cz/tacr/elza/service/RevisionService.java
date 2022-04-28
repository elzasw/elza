package cz.tacr.elza.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.controller.vo.ApPartFormVO;
import cz.tacr.elza.controller.vo.RevStateChange;
import cz.tacr.elza.controller.vo.ap.item.ApItemVO;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.core.security.AuthParam;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApRevIndex;
import cz.tacr.elza.domain.ApRevItem;
import cz.tacr.elza.domain.ApRevPart;
import cz.tacr.elza.domain.ApRevision;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApState.StateApproval;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.ChangeType;
import cz.tacr.elza.domain.RevStateApproval;
import cz.tacr.elza.domain.RulPartType;
import cz.tacr.elza.domain.SyncState;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.groovy.GroovyResult;
import cz.tacr.elza.repository.ApBindingStateRepository;
import cz.tacr.elza.repository.ApItemRepository;
import cz.tacr.elza.repository.ApRevIndexRepository;
import cz.tacr.elza.repository.ApRevisionRepository;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.service.cache.AccessPointCacheService;

@Service
public class RevisionService {

    @Autowired
    private UserService userService;

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
    private StaticDataService staticDataService;

    @Autowired
    private AccessPointService accessPointService;

    @Autowired
    private AccessPointCacheService accessPointCacheService;

    @Autowired
    private ApRevisionRepository revisionRepository;

    @Autowired
    private ApRevIndexRepository revIndexRepository;

    @Autowired
    private ApBindingStateRepository bindingStateRepository;

    @Autowired
    private ApItemRepository itemRepository;

    @Autowired
    private ApStateRepository stateRepository;

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
        revision.setStateApproval(RevStateApproval.ACTIVE);
        revision.setPreferredPart(state.getAccessPoint().getPreferredPart());

        revisionRepository.save(revision);

        List<ApBindingState> bindingStateList = bindingStateRepository.findByAccessPoint(state.getAccessPoint());
        if (CollectionUtils.isNotEmpty(bindingStateList)) {
            bindingStateList.forEach(eid -> eid.setSyncOk(SyncState.NOT_SYNCED));
            bindingStateRepository.saveAll(bindingStateList);
        }
    }

    @Transactional
    public void deleteRevision(ApState state) {
        ApRevision revision = findRevisionByState(state);
        if (revision == null) {
            throw new IllegalStateException("Pro tento přístupový bod neexistuje revize");
        }

        deleteRevision(revision);
    }

    @Transactional
    public void deleteRevision(ApRevision revision) {
        ApChange change = accessPointDataService.createChange(ApChange.Type.AP_DELETE);
        List<ApRevPart> parts = revisionPartService.findPartsByRevision(revision);
        List<ApRevItem> items = null;
        List<ApRevIndex> indices = null;
        if (CollectionUtils.isNotEmpty(parts)) {
            items = revisionItemService.findByParts(parts);
            indices = revIndexRepository.findByParts(parts);
        }

        deleteRevision(revision, change, parts, items, indices);
    }

    private void deleteRevision(ApRevision revision,
                                ApChange change,
                                List<ApRevPart> parts,
                                List<ApRevItem> items,
                                List<ApRevIndex> indices) {
        if (CollectionUtils.isNotEmpty(parts)) {
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

    @Transactional(value = TxType.MANDATORY)
    public void changeStateRevision(ApState state, RevStateChange revStateChange) {
        accessPointService.checkPermissionForEditingConfirmed(state);

        ApRevision revision = findRevisionByState(state);
        if (revision == null) {
            throw new IllegalStateException("Pro tento přístupový bod neexistuje revize");
        }

        // nemůžeme změnit třídu revize, pokud je entita v CAM
        if (!revStateChange.getTypeId().equals(state.getApTypeId())) {
            int countBinding = bindingStateRepository.countByAccessPoint(state.getAccessPoint());
            if (countBinding > 0) {
                throw new SystemException("Třídu revize entity z CAM nelze změnit.", BaseCode.INSUFFICIENT_PERMISSIONS)
                    .set("accessPointId", state.getAccessPointId())
                        .set("revisionId", revision.getRevisionId());
            }
        }

        StaticDataProvider sdp = staticDataService.createProvider();

        // TODO: nutne oddelit do samostatne tabulky revizi a zmenu stavu revize 
        // ApChange change = accessPointDataService.createChange(ApChange.Type.AP_UPDATE);
        // ApRevision revision = createRevision(prevRevision, change);

        if (revStateChange.getState() != null) {
            RevStateApproval stateApproval = RevStateApproval.valueOf(revStateChange.getState().getValue());
            revision.setStateApproval(stateApproval);
        }

        if (revStateChange.getTypeId() != null) {
            ApType type = sdp.getApTypeById(revStateChange.getTypeId());
            revision.setType(type);
        }

        revisionRepository.saveAndFlush(revision);
    }

    /**
     * Create new revision based on previous revision
     * 
     * @param prevRevision
     * @param change
     * @return
     */
    public ApRevision createRevision(@Nonnull ApRevision prevRevision, @Nonnull ApChange change) {
        Validate.isTrue(prevRevision.getDeleteChange() == null);

        prevRevision.setDeleteChange(change);
        prevRevision = revisionRepository.saveAndFlush(prevRevision);

        ApRevision revision = new ApRevision();
        revision.setCreateChange(change);
        revision.setState(prevRevision.getState());
        revision.setType(prevRevision.getType());
        revision.setStateApproval(prevRevision.getStateApproval());
        revision.setPreferredPart(prevRevision.getPreferredPart());
        return revision;
    }

    public ApRevision findRevisionByState(ApState state) {
        return revisionRepository.findByState(state);
    }

    @Transactional
    public void createPart(ApRevision revision, ApPartFormVO apPartFormVO) {
        ApPart parentPart = apPartFormVO.getParentPartId() == null ? null : partService.getPart(apPartFormVO.getParentPartId());
        ApRevPart revParentPart = apPartFormVO.getRevParentPartId() == null ? null : revisionPartService.findById(apPartFormVO.getParentPartId());

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
        revisionItemService.createItems(newPart, apPartFormVO.getItems(), apChange, false);
        updatePartValue(newPart, revision);
    }

    @Transactional(Transactional.TxType.MANDATORY)
    public void updatePartValue(final ApRevPart part, final ApRevision revision) {
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

    @Transactional(Transactional.TxType.MANDATORY)
    public void updatePartValues(final ApRevision revision) {
        List<ApRevPart> partList = revisionPartService.findPartsByRevision(revision);
        Map<Integer, List<ApRevItem>> itemMap = revisionItemService.findByParts(partList).stream()
                .collect(Collectors.groupingBy(ApRevItem::getPartId));

        for (ApRevPart part : partList) {
            List<ApRevPart> childrenParts = findChildrenParts(part, partList);
            List<ApRevItem> items = getItemsForParts(part, childrenParts, itemMap);

            updatePartValue(revision, part, childrenParts, items);
        }
    }

    private void updatePartValue(final ApRevision revision,
                                 final ApRevPart revPart,
                                 final List<ApRevPart> childRevParts,
                                 final List<ApRevItem> revItems) {
        GroovyResult result;

        boolean preferred = isPrefered(revision, revPart);
        ApPart apPart = revPart.getOriginalPart();

        // pokud je to nový Part
        if (apPart == null) {

            result = groovyService.processGroovy(revision.getTypeId(), revPart, childRevParts, revItems, preferred);
        } else {
            List<ApPart> childParts = partService.findPartsByParentPart(apPart);
            List<ApItem> apItems = itemRepository.findValidItemsByPart(apPart);

            result = groovyService.processGroovy(revision.getTypeId(), apPart, childParts, apItems, revItems, preferred);
        }
        revisionPartService.updatePartValue(revPart, result);
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
        List<ApItem> apItems = itemRepository.findValidItemsByPart(apPart);
        ApChange apChange = accessPointDataService.createChange(ApChange.Type.AP_DELETE);

        if (revPart != null) {
            // smazat itemy a indexi
            if (revision.getRevPreferredPartId() != null && revision.getRevPreferredPartId().equals(revPart.getPartId())) {
                throw new IllegalArgumentException("Preferované jméno nemůže být odstraněno");
            }

            List<ApRevIndex> indices = revIndexRepository.findByPart(revPart);
            List<ApRevItem> items = revisionItemService.findByPart(revPart);

            deleteRevisionIndices(indices);
            revisionItemService.deleteRevisionItems(items, apChange);
        } else {
            // vytvořit nový záznam
            revPart = revisionPartService.createPart(revision, apChange, apPart);
        }

        revisionItemService.createDeletedItems(revPart, apChange, apItems);
    }

    @Transactional
    public void deletePart(ApState state, Integer partId) {
        ApRevision revision = revisionRepository.findByState(state);
        if (revision == null) {
            throw new IllegalArgumentException("Neexistuje revize");
        }
        if (revision.getRevPreferredPartId() != null && revision.getRevPreferredPartId().equals(partId)) {
            throw new IllegalArgumentException("Preferované jméno nemůže být odstraněno");
        }
        ApRevPart revPart = revisionPartService.findById(partId);

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
        List<ApItem> apItems = itemRepository.findValidItemsByPart(apPart);

        if (!apPart.getPartType().getCode().equals(defaultPartType.getCode())) {
            throw new IllegalArgumentException("Preferované jméno musí být typu " + defaultPartType.getCode());
        }

        if (apPart.getParentPart() != null) {
            throw new IllegalArgumentException("Návazný part nelze změnit na preferovaný.");
        }

        if (apPart.getDeleteChange() != null || (revPart != null && revisionItemService.allItemsDeleted(revItems, apItems))) {
            throw new IllegalArgumentException("Smazaný part nelze označit za preferovaný");
        }

        revision.setPreferredPart(apPart);
        revision.setRevPreferredPart(revPart);
        revisionRepository.save(revision);

        updatePartValues(revision);
    }

    @Transactional
    public void setPreferName(ApState state, Integer partId) {
        accessPointService.checkPermissionForEditingConfirmed(state);

        ApRevision revision = revisionRepository.findByState(state);
        if (revision == null) {
            throw new IllegalArgumentException("Neexistuje revize");
        }
        StaticDataProvider sdp = StaticDataProvider.getInstance();
        RulPartType defaultPartType = sdp.getDefaultPartType();

        ApRevPart revPart = revisionPartService.findById(partId);
        List<ApRevItem> revItems = revisionItemService.findByPart(revPart);

        if (!revPart.getPartType().getCode().equals(defaultPartType.getCode())) {
            throw new IllegalArgumentException("Preferované jméno musí být typu " + defaultPartType.getCode());
        }

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

    @Transactional
    public void updatePart(ApRevision revision, Integer partId, ApPartFormVO apPartFormVO) {
        if (revision == null) {
            throw new IllegalArgumentException("Neexistuje revize");
        }
        ApRevPart revPart = revisionPartService.findById(partId);

        List<ApRevItem> deleteItems = revisionItemService.findByPart(revPart);

        Map<Integer, ApRevItem> itemMap = deleteItems.stream().collect(Collectors.toMap(ApRevItem::getItemId, i -> i));

        List<ApItem> apItems = new ArrayList<>();
        Map<Integer, ApItem> apItemMap = null;

        if (revPart.getOriginalPart() != null) {
            apItems = itemRepository.findValidItemsByPart(revPart.getOriginalPart());
            apItemMap = apItems.stream().collect(Collectors.toMap(ApItem::getItemId, i -> i));
        }


        List<ApItemVO> itemListVO = apPartFormVO.getItems();
        List<ApItemVO> createItems = new ArrayList<>();
        List<ApItemVO> newRevisionCreateItems = new ArrayList<>();

        // určujeme, které záznamy: přidat, odstranit, nebo ponechat
        for (ApItemVO itemVO : itemListVO) {
            if (itemVO.getId() == null) {
                createItems.add(itemVO); // new -> add
            } else {
                if (itemVO.getOrigObjectId() != null || itemVO.getChangeType() == ChangeType.NEW) {
                    ApRevItem item = itemMap.get(itemVO.getId());
                    if (item != null) {
                        if (itemVO.equalsValue(item)) {
                            deleteItems.remove(item); // no change -> don't delete
                        } else {
                            createItems.add(itemVO); // changed -> add + delete
                        }
                    }
                } else {
                    if (apItemMap != null) {
                        ApItem i = apItemMap.get(itemVO.getId());
                        if (i != null && !itemVO.equalsValue(i)) {
                            newRevisionCreateItems.add(itemVO);
                        }
                    }
                }
            }
        }

        // zjištění, které z původních itemů v partu zůstávají
        List<ApItem> notDeletedItems = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(apItems)) {
            for (ApItemVO createItem : itemListVO) {
                Integer objectId = createItem.getOrigObjectId();
                if (objectId == null && createItem.getChangeType() != ChangeType.NEW) {
                    objectId = createItem.getObjectId();
                }
                if (objectId != null) {
                    for (ApItem apItem : apItems) {
                        if (apItem.getObjectId().equals(objectId)) {
                            notDeletedItems.add(apItem);
                        }
                    }
                }
            }
            apItems.removeAll(notDeletedItems);
        }

        // pokud nedojde ke změně
        if (!createItems.isEmpty() || !deleteItems.isEmpty() || !apItems.isEmpty() || !newRevisionCreateItems.isEmpty()) {
            ApChange change = accessPointDataService.createChange(ApChange.Type.AP_UPDATE);

            revisionItemService.createItems(revPart, createItems, change, false);

            revisionItemService.createItems(revPart, newRevisionCreateItems, change, true);

            revisionItemService.deleteRevisionItems(deleteItems, change);

            //založení záznamů o smazaných původních itemech
            revisionItemService.createDeletedItems(revPart, change, apItems);

            updatePartValue(revPart, revision);
        }
    }

    @Transactional
    public void updatePart(ApRevision revision, ApPart apPart, ApPartFormVO apPartFormVO) {
        ApRevPart revPart = revisionPartService.findByOriginalPart(apPart);
        if (revPart != null) {
            updatePart(revision, revPart.getPartId(), apPartFormVO);
        } else {
            ApChange change = accessPointDataService.createChange(ApChange.Type.AP_CREATE);

            revPart = revisionPartService.createPart(revision, change, apPart);

            List<ApItem> apItems = itemRepository.findValidItemsByPart(revPart.getOriginalPart());
            Map<Integer, ApItem> apItemMap = apItems.stream().collect(Collectors.toMap(ApItem::getItemId, i -> i));
            List<ApItemVO> createItems = new ArrayList<>();

            for (ApItemVO itemVO : apPartFormVO.getItems()) {
                if (itemVO.getId() == null) {
                    createItems.add(itemVO); // new -> add
                } else {
                    ApItem i = apItemMap.get(itemVO.getId());
                    if (i != null && !itemVO.equalsValue(i)) {
                        createItems.add(itemVO);
                    }
                }
            }

            // zjištění, které z původních itemů v partu zůstávají
            List<ApItem> notDeletedItems = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(apItems)) {
                for (ApItemVO itemVO : apPartFormVO.getItems()) {
                    Integer id = itemVO.getId();
                    if (id != null) {
                        ApItem apItem = apItemMap.get(itemVO.getId());
                        if (apItem != null) {
                            notDeletedItems.add(apItem);
                        }
                    }
                }
                apItems.removeAll(notDeletedItems);
            }

            revisionItemService.createItems(revPart, createItems, change, true);

            //založení záznamů o smazaných původních itemech
            revisionItemService.createDeletedItems(revPart, change, apItems);

            updatePartValue(revPart, revision);
        }
    }

    @AuthMethod(permission = { UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR })
    @Transactional(TxType.MANDATORY)
    public void mergeRevision(@AuthParam(type = AuthParam.Type.AP_STATE) ApState apState,
                              ApState.StateApproval newStateApproval) {
        Validate.isTrue(apState.getDeleteChangeId() == null, "Only non deleted ApState is valid");

        ApRevision revision = findRevisionByState(apState);
        if (revision == null) {
            throw new IllegalArgumentException("Neexistuje revize");
        }
        ApAccessPoint accessPoint = apState.getAccessPoint();

        // Check permissions in case of approved AP
        if (!hasPermissionMerge(apState.getScope(), apState.getStateApproval(), newStateApproval, revision
                .getStateApproval())) {
            throw new SystemException("Uživatel nemá oprávnění sloučení změny přístupového bodu",
                    BaseCode.INSUFFICIENT_PERMISSIONS)
                            .set("accessPointId", accessPoint.getAccessPointId())
                            .set("scopeId", apState.getScopeId())
                            .set("oldState", apState.getStateApproval())
                            .set("newState", newStateApproval)
                            .set("revisionState", revision.getStateApproval());
        }

        // má uživatel možnost nastavit požadovaný stav?
        if (!accessPointService.getNextStatesRevision(apState, revision).contains(newStateApproval)) {
            throw new SystemException("Požadovaný stav entity nelze nastavit.", BaseCode.INSUFFICIENT_PERMISSIONS)
                    .set("accessPointId", accessPoint.getAccessPointId())
                    .set("scopeId", apState.getScopeId())
                    .set("oldState", apState.getStateApproval())
                    .set("newState", newStateApproval);
        }

        ApChange change = accessPointDataService.createChange(ApChange.Type.AP_UPDATE);
        List<ApRevPart> revParts = revisionPartService.findPartsByRevision(revision);
        List<ApRevItem> revItems = null;
        List<ApRevIndex> revIndices = null;

        //změna částí entity
        if (CollectionUtils.isNotEmpty(revParts)) {
            revItems = revisionItemService.findByParts(revParts);
            revIndices = revIndexRepository.findByParts(revParts);

            Map<Integer, List<ApRevItem>> revItemMap = revItems.stream()
                    .collect(Collectors.groupingBy(ApRevItem::getPartId));

            List<ApItem> items = itemRepository.findValidItemsByAccessPoint(accessPoint);

            Map<Integer, List<ApItem>> itemMap = items.stream()
                    .collect(Collectors.groupingBy(ApItem::getPartId));

            List<ApRevPart> createdParts = new ArrayList<>();
            List<ApRevPart> updatedParts = new ArrayList<>();
            List<ApRevPart> deletedParts = new ArrayList<>();

            for (ApRevPart revPart : revParts) {
                if (revPart.getOriginalPart() == null) {
                    createdParts.add(revPart);
                } else {
                    List<ApRevItem> revPartItems = revItemMap.get(revPart.getPartId());
                    List<ApItem> apItems = itemMap.get(revPart.getOriginalPartId());

                    if (!revisionItemService.allItemsDeleted(revPartItems, apItems)) {
                        updatedParts.add(revPart);
                    } else {
                        deletedParts.add(revPart);
                    }
                }
            }

            revisionPartService.createParts(createdParts, revItemMap, accessPoint, revision);
            revisionPartService.updateParts(updatedParts, revItemMap, items, change);
            revisionPartService.deleteParts(deletedParts, items, change);


        }

        if (revision.getPreferredPart() != null) {
            ApPart newPrefferdPart = partService.getPart(revision.getPreferredPart().getPartId());
            accessPoint.setPreferredPart(newPrefferdPart);
        }

        //změna stavu entity
        apState.setDeleteChange(change);
        stateRepository.save(apState);

        ApState newState = accessPointService.copyState(apState, change);
        newState.setStateApproval(newStateApproval);
        newState.setApType(revision.getType());
        stateRepository.save(newState);

        accessPointService.updateAndValidate(accessPoint.getAccessPointId());
        accessPointCacheService.createApCachedAccessPoint(accessPoint.getAccessPointId());

        //smazání revize
        deleteRevision(revision, change, revParts, revItems, revIndices);
    }

    /**
     * Vyhodnocuje oprávnění přihlášeného uživatele k úpravám na přístupovém bodu
     * dle uvedené oblasti entit.
     *
     * @param apScope
     *            oblast entit
     * @param oldStateApproval
     *            původní stav schvalování - při zakládání AP může být {@code null}
     * @param newStateApproval
     *            nový stav schvalování - pokud v rámci změny AP nedochází ke změně
     *            stavu schvalovaní, musí být stejný jako {@code oldStateApproval}
     * @return oprávění přihášeného uživatele ke změně AP
     * @throws BusinessException
     *             přechod mezi uvedenými stavy není povolen
     * @throws SystemException
     *             přechod mezi uvedenými stavy není povolen
     */
    private boolean hasPermissionMerge(ApScope scope, StateApproval oldStateApproval,
                                  StateApproval newStateApproval,
                                  RevStateApproval revState) {

        Validate.notNull(scope, "AP Scope is null");
        Validate.notNull(oldStateApproval, "Old State Approval is null");
        Validate.notNull(newStateApproval, "New State Approval is null");
        Validate.notNull(revState, "Rev State Approval is null");

        // admin může cokoliv
        if (userService.hasPermission(Permission.ADMIN)) {
            return true;
        }

        // Je ve stavu ke schválení?
        boolean revToApprove = revState == RevStateApproval.TO_APPROVE;

        if (revToApprove && oldStateApproval.equals(StateApproval.APPROVED) && newStateApproval.equals(
                                                                                                       StateApproval.APPROVED)) {
            // k editaci již schválených přístupových bodů je potřeba "Změna schválených přístupových bodů"
            return userService.hasPermission(Permission.AP_EDIT_CONFIRMED_ALL)
                    || userService.hasPermission(Permission.AP_EDIT_CONFIRMED, scope.getScopeId());            
        }
        // nová nebo k připomínkám s revizí
        if(oldStateApproval.equals(StateApproval.NEW)||oldStateApproval.equals(StateApproval.TO_AMEND)) {
            if (revToApprove && newStateApproval.equals(StateApproval.APPROVED)) {
                // "Schvalování přístupových bodů"
                if (userService.hasPermission(Permission.AP_CONFIRM_ALL)
                    || userService.hasPermission(Permission.AP_CONFIRM, scope.getScopeId())) {
                    return true;
                }
            }
            if (newStateApproval.equals(StateApproval.NEW) ||
                    newStateApproval.equals(StateApproval.TO_AMEND) ||
                    newStateApproval.equals(StateApproval.TO_APPROVE)) {
                if (userService.hasPermission(Permission.AP_SCOPE_WR_ALL)
                        || userService.hasPermission(Permission.AP_SCOPE_WR, scope.getScopeId())) {
                    return true;
                }
            }
        }
        // jiný neznámý případ
        return false;
    }

    public List<ApRevision> findAllRevisionByStateIn(List<ApState> apStates) {
        return revisionRepository.findAllByStateIn(apStates);
    }
}
