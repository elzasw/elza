package cz.tacr.elza.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.controller.vo.ApPartFormVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemVO;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.core.security.AuthParam;
import cz.tacr.elza.domain.ApAccessPoint;
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
import cz.tacr.elza.domain.ApStateEnum;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.RevStateApproval;
import cz.tacr.elza.domain.RulPartType;
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
import cz.tacr.elza.service.cache.CachedAccessPoint;
import cz.tacr.elza.service.cache.CachedPart;

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
    public ApRevision createRevision(ApState state) {
        ApRevision revision = findRevisionByState(state);
        if (revision != null) {
            throw new IllegalStateException("Revize pro tento přístupový bod již existuje");
        }

        // check if AP can be edited in revision
        // no missing connected APs
        CachedAccessPoint apCached = this.accessPointCacheService.findCachedAccessPoint(state.getAccessPointId());
        for (CachedPart cachedPart : apCached.getParts()) {
            for (ApItem item : cachedPart.getItems()) {
                ArrData data = item.getData();
                if (data != null) {
                    if (data instanceof ArrDataRecordRef) {
                        ArrDataRecordRef drr = (ArrDataRecordRef) data;
                        if (drr.getRecordId() == null) {
                            throw new IllegalStateException("Přístupový bod obsahuje odkazy na neexistující entity.");
                        }
                    }
                }
            }
        }

        revision = new ApRevision();
        revision.setState(state);
        revision.setType(state.getApType());
        revision.setStateApproval(RevStateApproval.ACTIVE);
        revision.setPreferredPart(state.getAccessPoint().getPreferredPart());

        // Permission check - includes new revision state
        accessPointService.checkPermissionForEdit(state, revision);

        ApChange change = accessPointDataService.createChange(ApChange.Type.AP_CREATE);
        revision.setCreateChange(change);

        return revisionRepository.save(revision);
    }

    @Transactional(TxType.MANDATORY)
    public void deleteRevision(ApState state) {
        ApRevision revision = findRevisionByState(state);
        if (revision == null) {
            throw new IllegalStateException("Pro tento přístupový bod neexistuje revize");
        }

        deleteRevision(state, revision);
    }

    @Transactional(TxType.MANDATORY)
    public void deleteRevision(ApState state, ApRevision revision) {
        // revision might be deleted in any state
        accessPointService.checkPermissionForEdit(state, RevStateApproval.ACTIVE);

        ApChange change = accessPointDataService.createChange(ApChange.Type.AP_DELETE);
        List<ApRevPart> parts = revisionPartService.findPartsByRevision(revision);
        List<ApRevItem> items = revisionItemService.findByParts(parts);

        deleteRevision(revision, change, parts, items);
    }

    private ApRevision deleteRevision(ApRevision revision,
                                ApChange change,
                                List<ApRevPart> parts,
                                List<ApRevItem> items) {

        if (CollectionUtils.isNotEmpty(parts)) {
            List<ApRevIndex> revIndices = revIndexRepository.findByParts(parts);

            deleteRevisionIndices(revIndices);
        }
        revisionItemService.deleteRevisionItems(items, change);
        revisionPartService.deleteRevisionParts(parts, change);

        revision.setDeleteChange(change);
        return revisionRepository.save(revision);
    }

    private void deleteRevisionIndices(List<ApRevIndex> indices) {
        if (CollectionUtils.isNotEmpty(indices)) {
            revIndexRepository.deleteAll(indices);
        }
    }

    @Transactional(value = TxType.MANDATORY)
    public ApRevision changeStateRevision(@NotNull ApState state,
                                          @NotNull Integer nextApTypeId,
                                          @Nullable RevStateApproval revNextState) {

        ApRevision revision = findRevisionByState(state);
        if (revision == null) {
            throw new IllegalStateException("Pro tento přístupový bod neexistuje revize");
        }
        // check permission for other state then to_approve
        accessPointService.checkPermissionForEdit(state,
                                                  revision.getStateApproval() != RevStateApproval.TO_APPROVE ? revision
                                                          .getStateApproval() : revNextState);

        StaticDataProvider sdp = staticDataService.createProvider();

        // TODO: nutne oddelit do samostatne tabulky revizi a zmenu stavu revize 
        // ApChange change = accessPointDataService.createChange(ApChange.Type.AP_UPDATE);
        // ApRevision revision = createRevision(prevRevision, change);

        // nemůžeme změnit třídu revize, pokud je entita v CAM
        if (!nextApTypeId.equals(state.getApTypeId())) {
            int countBinding = bindingStateRepository.countByAccessPoint(state.getAccessPoint());
            if (countBinding > 0) {
                throw new SystemException("Třídu revize entity z CAM nelze změnit.", BaseCode.INSUFFICIENT_PERMISSIONS)
                    .set("accessPointId", state.getAccessPointId())
                        .set("revisionId", revision.getRevisionId());
            }

            ApType type = sdp.getApTypeById(nextApTypeId);
            revision.setType(type);
        }

        if (revNextState != null) {
            revision.setStateApproval(revNextState);
        }

        return revisionRepository.saveAndFlush(revision);
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

    /**
     * Create new revision part
     * 
     * Part is not based on standard part. All items will receive new
     * objectId(s).
     * 
     * @param state
     * @param revision
     * @param apPartFormVO
     * @return
     */
    @Transactional
    public ApRevPart createPart(ApState state, ApRevision revision, ApPartFormVO apPartFormVO) {
        accessPointService.checkPermissionForEdit(state, revision);

        ApPart parentPart = apPartFormVO.getParentPartId() == null ? null : partService.getPart(apPartFormVO.getParentPartId());
        ApRevPart revParentPart = apPartFormVO.getRevParentPartId() == null ? null
                : revisionPartService.findById(apPartFormVO.getRevParentPartId());

        if ((parentPart != null && parentPart.getParentPart() != null)
                || (revParentPart != null && revParentPart.getParentPart() != null)) {
            throw new IllegalArgumentException("Nadřazená část nesmí zároveň být podřazená část");
        }

        if (CollectionUtils.isEmpty(apPartFormVO.getItems())) {
            throw new IllegalArgumentException("Část musí mít alespoň jeden prvek popisu");
        }
        // validate items in part, has to be witout ids
        for (ApItemVO item : apPartFormVO.getItems()) {
            Validate.isTrue(item.getObjectId() == null);
            Validate.isTrue(item.getOrigObjectId() == null);
            Validate.isTrue(item.getId() == null);
        }

        RulPartType partType = partService.getPartTypeByCode(apPartFormVO.getPartTypeCode());
        ApChange apChange = accessPointDataService.createChange(ApChange.Type.AP_CREATE);

        ApRevPart newPart = revisionPartService.createPart(partType, revision, apChange, revParentPart, parentPart);
        revisionItemService.createItems(newPart, apPartFormVO.getItems(), apChange, false);
        updatePartValue(newPart, revision);
        return newPart;
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
    public void deletePart(ApState state, ApRevision revision, Integer partId) {
        accessPointService.checkPermissionForEdit(state, revision);

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

            revPart = this.revisionPartService.deletePart(apPart, revPart);
        } else {
            // vytvořit nový záznam
            revPart = revisionPartService.createPart(revision, apChange, apPart, true);
        }

        revisionItemService.createDeletedItems(revPart, apChange, apItems);
    }

    @Transactional
    public void deletePart(ApState state, Integer partId) {
        ApRevision revision = revisionRepository.findByState(state);
        if (revision == null) {
            throw new IllegalArgumentException("Neexistuje revize");
        }
        accessPointService.checkPermissionForEdit(state, revision);

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
    public void setPreferName(ApState state, ApRevision revision, Integer partId) {
        accessPointService.checkPermissionForEdit(state, revision);

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

        ApRevision revision = revisionRepository.findByState(state);
        if (revision == null) {
            throw new IllegalArgumentException("Neexistuje revize");
        }
        accessPointService.checkPermissionForEdit(state, revision);

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

    /**
     * Update revPart
     * 
     * @param apState
     * @param revision
     * @param revPart
     * @param apPartFormVO
     */
    @AuthMethod(permission = { UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR })
    @Transactional(TxType.MANDATORY)
    public void updatePart(@AuthParam(type = AuthParam.Type.AP_STATE) ApState apState,
                           @NotNull ApRevision revision,
                           @NotNull ApRevPart revPart,
                           ApPartFormVO apPartFormVO) {
        if (revision == null || revPart == null) {
            throw new IllegalArgumentException("Neexistuje revize");
        }
        accessPointService.checkPermissionForEdit(apState, revision);

        List<ApRevItem> revItems = revisionItemService.findByPart(revPart);
        // Map by objectId -> ApRevItem
        Map<Integer, ApRevItem> revItemObjectMap = new HashMap<>();
        for (ApRevItem revItem : revItems) {
            Validate.isTrue(revItem.getObjectId() != null ^ revItem.getOrigObjectId() != null);

            if (revItem.getObjectId() != null) {
                ApRevItem prevItem = revItemObjectMap.put(revItem.getObjectId(), revItem);
                Validate.isTrue(prevItem == null);
            } else if (revItem.getOrigObjectId() != null) {
                ApRevItem prevItem = revItemObjectMap.put(revItem.getOrigObjectId(), revItem);
                Validate.isTrue(prevItem == null);
            }
        }

        List<ApItem> apItems;
        Map<Integer, ApItem> apItemObjectMap;

        if (revPart.getOriginalPart() != null) {
            apItems = itemRepository.findValidItemsByPart(revPart.getOriginalPart());
            apItemObjectMap = apItems.stream().collect(Collectors.toMap(ApItem::getObjectId, i -> i));
        } else {
            apItems = Collections.emptyList();
            apItemObjectMap = Collections.emptyMap();
        }

        // List of ApRevItem for new revisions
        // objectId will be stored
        List<ApItemVO> createItems = new ArrayList<>();

        // List of ApItems for which new ApRevItem will be created
        // origObjectId will be stored
        List<ApItemVO> revisionCreateItems = new ArrayList<>();

        // List of revItems without change
        List<ApRevItem> unchangedRevItems = new ArrayList<>();

        // List of referenced ApItem (not deleted)
        List<ApItem> notDeletedApItems = new ArrayList<>();

        // určujeme, které záznamy: přidat, odstranit, nebo ponechat
        for (ApItemVO itemVO : apPartFormVO.getItems()) {
            // src items
            Integer objectId = itemVO.getObjectId() != null ? itemVO.getObjectId() : itemVO.getOrigObjectId();
            if (objectId == null) {
                // new -> add
                Validate.isTrue(itemVO.getId() == null);
                createItems.add(itemVO);
            } else {
                ApItem origItem = apItemObjectMap.get(objectId);
                ApRevItem revItem = revItemObjectMap.get(objectId);
                if (origItem == null && revItem == null) {
                    // source item not found
                    throw new BusinessException("ApItem nor ApRevItem was found, objectId: " + objectId,
                            BaseCode.ID_NOT_EXIST).set("objectId", objectId);
                }
                if (origItem != null) {
                    // Orig item is somehow transformed
                    notDeletedApItems.add(origItem);
                }
                if(revItem!=null) {
                    if(itemVO.getId()!=null) {
                        // pokud je nastaveno ID, musi byt spravne
                        if (!itemVO.getId().equals(revItem.getItemId())) {
                            // source item not found
                            throw new BusinessException("ApItem ID does not match ApRevItem ID, itemId:" + itemVO
                                    .getId(),
                                    BaseCode.INVALID_STATE)
                                            .set("itemId", itemVO.getId())
                                            .set("revItemId", revItem.getItemId());
                        }
                    }
                    // modifying current item
                    if (itemVO.equalsValue(revItem)) {
                        // no change -> don't delete
                        unchangedRevItems.add(revItem);
                    } else {
                        // value changed -> add + delete
                        createItems.add(itemVO);
                    }
                } else {
                    // origItem exists but not revItem
                    // -> new revItem has to be created
                    // ID should match
                    if (itemVO.getId() != null) {
                        // pokud je nastaveno ID, musi byt spravne
                        Validate.isTrue(itemVO.getId().equals(origItem.getItemId()));
                    }
                    revisionCreateItems.add(itemVO);
                }
            }
        }

        List<ApRevItem> deleteRevItems = new ArrayList<>(revItems);
        deleteRevItems.removeAll(unchangedRevItems);

        List<ApItem> deleteApItems = new ArrayList<>(apItems);
        deleteApItems.removeAll(notDeletedApItems);

        // pokud nedojde ke změně
        if (!createItems.isEmpty() ||
                !deleteRevItems.isEmpty() ||
                !deleteApItems.isEmpty() ||
                !revisionCreateItems.isEmpty()) {
            ApChange change = accessPointDataService.createChange(ApChange.Type.AP_UPDATE);

            // remove old revItems
            revisionItemService.deleteRevisionItems(deleteRevItems, change);

            revisionItemService.createItems(revPart, createItems, change, false);

            revisionItemService.createItems(revPart, revisionCreateItems, change, true);

            //založení záznamů o smazaných původních itemech
            revisionItemService.createDeletedItems(revPart, change, deleteApItems);

            updatePartValue(revPart, revision);
        }
    }

    /**
     * Update part revision
     * 
     * Method will update existing revision or will create new revision.
     * 
     * Items are mapped to original items using objectId
     * 
     * @param apState
     * @param revision
     * @param apPart
     * @param apPartFormVO
     */
    @AuthMethod(permission = { UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR })
    @Transactional(TxType.MANDATORY)
    public void updatePart(@AuthParam(type = AuthParam.Type.AP_STATE) ApState apState,
                           ApRevision revision, ApPart apPart, ApPartFormVO apPartFormVO) {
        accessPointService.checkPermissionForEdit(apState, revision);

        ApRevPart revPart = revisionPartService.findByOriginalPart(apPart);
        if (revPart != null) {
            // update existing revision
            updatePart(apState, revision, revPart, apPartFormVO);
        } else {
            // create new revision part
            ApChange change = accessPointDataService.createChange(ApChange.Type.AP_CREATE);

            revPart = revisionPartService.createPart(revision, change, apPart, false);

            List<ApItem> apItems = itemRepository.findValidItemsByPart(revPart.getOriginalPart());
            // Map objectId -> ApItem
            Map<Integer, ApItem> apItemMap = apItems.stream().collect(Collectors.toMap(ApItem::getObjectId, i -> i));

            List<ApItemVO> createItems = new ArrayList<>();
            List<ApItem> notDeletedItems = new ArrayList<>();

            for (ApItemVO itemVO : apPartFormVO.getItems()) {
                Validate.isTrue(itemVO.getOrigObjectId() == null);

                if (itemVO.getObjectId() == null) {
                    // new -> add                    
                    Validate.isTrue(itemVO.getId() == null);

                    createItems.add(itemVO);
                } else {
                    // item is not new, has to have valid objectId or origObjectId
                    ApItem i = apItemMap.get(itemVO.getObjectId());
                    if (i == null) {
                        throw new BusinessException("Source item not found", BaseCode.ID_NOT_EXIST)
                                .set("objectId", itemVO.getObjectId());
                    }

                    if (itemVO.getId() != null) {
                        // check ID
                        // if ID is sent it have to match itemId
                        Validate.isTrue(itemVO.getId().equals(i.getItemId()));
                    }

                    // zjištění, které z původních itemů v partu zůstávají
                    notDeletedItems.add(i);

                    if (!itemVO.equalsValue(i)) {
                        createItems.add(itemVO);
                    }
                }
            }

            apItems.removeAll(notDeletedItems);

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

        if (newStateApproval == null) {
            newStateApproval = apState.getStateApproval();
        }

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
        List<ApRevItem> revItems = revisionItemService.findByParts(revParts);

        Map<Integer, ApPart> savedParts = mergeParts(accessPoint, revParts, revItems, change);

        ApPart newPrefferdPart = null;
        if (revision.getPreferredPartId() != null) {
            newPrefferdPart = partService.getPart(revision.getPreferredPartId());
        } else
        if (revision.getRevPreferredPartId() != null) {
            newPrefferdPart = savedParts.get(revision.getRevPreferredPartId());
            Validate.notNull(newPrefferdPart, "RevPart for preferred name not found, revPartId: %s", revision
                    .getRevPreferredPartId());
        }
        if (newPrefferdPart != null) {
            accessPoint = accessPointService.setPreferName(accessPoint, newPrefferdPart);
        }

        //změna stavu entity
        apState.setDeleteChange(change);
        apState=stateRepository.save(apState);

        ApState newState = accessPointService.copyState(apState, change);
        newState.setStateApproval(newStateApproval);
        newState.setApType(revision.getType());
        newState = stateRepository.save(newState);

        //smazání revize
        deleteRevision(revision, change, revParts, revItems);

        accessPoint = accessPointService.updateAndValidate(accessPoint.getAccessPointId());
        // Pokud je entita schvalena je nutne overit jeji bezchybnost
        if (newStateApproval == StateApproval.APPROVED) {
            if (accessPoint.getState() == ApStateEnum.ERROR) {
                accessPointService.validateEntityAndFailOnError(accessPoint);
            }
        }
        accessPointCacheService.createApCachedAccessPoint(accessPoint.getAccessPointId());
    }

    private Map<Integer, ApPart> mergeParts(ApAccessPoint accessPoint,
                                            List<ApRevPart> revParts,
                                            List<ApRevItem> revItems,
                                            ApChange change) {
        if (CollectionUtils.isEmpty(revParts)) {
            return Collections.emptyMap();
        }

        // Map of RevPartId to saved ApPart
        Map<Integer, ApPart> revPartMap = new HashMap<>();

        List<ApRevPart> createSubParts = new ArrayList<>();
        List<ApPart> deletedParts = new ArrayList<>();

        for (ApRevPart revPart : revParts) {
            if (revPart.getOriginalPart() == null) {
                Validate.isTrue(!revPart.isDeleted());
                // create new part
                if (revPart.getRevParentPart() != null) {
                    Validate.isTrue(revPart.getParentPart() == null);
                    createSubParts.add(revPart);
                } else {
                    ApPart part = partService.createPart(revPart.getPartType(), accessPoint, revPart.getCreateChange(),
                                                         null);
                    revPartMap.put(revPart.getPartId(), part);
                }
            } else {
                // Cannot have rev as parent
                Validate.isTrue(revPart.getRevParentPart() == null);

                // only add id to map
                revPartMap.put(revPart.getPartId(), revPart.getOriginalPart());

                if (revPart.isDeleted()) {
                    deletedParts.add(revPart.getOriginalPart());
                }
            }
        }

        // Create subparts
        for (ApRevPart revPart : createSubParts) {
            // find parent
            ApPart parentPart = revPartMap.get(revPart.getRevParentPart());
            Validate.notNull(parentPart);

            ApPart part = partService.createPart(revPart.getPartType(), accessPoint, revPart.getCreateChange(),
                                                 parentPart);
            revPartMap.put(revPart.getPartId(), part);
        }

        revisionItemService.mergeItems(accessPoint, change, revParts, revPartMap, revItems);

        partService.deletePartsWithoutItems(deletedParts, change);

        return revPartMap;

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
    private boolean hasPermissionMerge(final ApScope scope,
                                       final StateApproval oldStateApproval,
                                       @Nullable StateApproval newStateApproval,
                                       @Nullable final RevStateApproval revState) {

        Validate.notNull(scope, "AP Scope is null");
        Validate.notNull(oldStateApproval, "Old State Approval is null");
        Validate.notNull(revState, "Rev State Approval is null");

        if (newStateApproval == null) {
            newStateApproval = oldStateApproval;
        }

        // admin může cokoliv
        if (userService.hasPermission(Permission.ADMIN)) {
            return true;
        }

        // Je ve stavu ke schválení?
        boolean revToApprove = revState == RevStateApproval.TO_APPROVE;

        if (revToApprove && oldStateApproval.equals(StateApproval.APPROVED) &&
                newStateApproval.equals(StateApproval.APPROVED)) {
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
