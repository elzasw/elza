package cz.tacr.elza.controller;

import java.util.Arrays;
import java.util.List;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.controller.factory.ApFactory;
import cz.tacr.elza.controller.vo.ApStateUpdate;
import cz.tacr.elza.controller.vo.AutoValue;
import cz.tacr.elza.controller.vo.CopyAccessPointDetail;
import cz.tacr.elza.controller.vo.DeleteAccessPointDetail;
import cz.tacr.elza.controller.vo.DeleteAccessPointsDetail;
import cz.tacr.elza.controller.vo.EntityRef;
import cz.tacr.elza.controller.vo.ReplaceType;
import cz.tacr.elza.controller.vo.ResultAutoItems;
import cz.tacr.elza.controller.vo.RevStateChange;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApRevState;
import cz.tacr.elza.domain.ApRevision;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApState.StateApproval;
import cz.tacr.elza.domain.RevStateApproval;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.RegistryCode;
import cz.tacr.elza.groovy.GroovyItem;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.GroovyService;
import cz.tacr.elza.service.PartService;
import cz.tacr.elza.service.RevisionPartService;
import cz.tacr.elza.service.RevisionService;
import cz.tacr.elza.service.RuleService;
import cz.tacr.elza.service.cache.AccessPointCacheService;
import cz.tacr.elza.service.cache.CachedAccessPoint;

@RestController
@RequestMapping("/api/v1")
public class AccessPointController implements AccesspointsApi {

    @Autowired
    AccessPointCacheService apCacheService;

    @Autowired
    RevisionPartService revisionPartService;

    @Autowired
    AccessPointService accessPointService;

    @Autowired
    StaticDataService staticDataService;

    @Autowired
    RevisionService revisionService;

    @Autowired
    GroovyService groovyService;

    @Autowired
    PartService partService;

    @Autowired
    RuleService ruleService;

    @Autowired
    ApFactory apFactory;

    @Override
    @Transactional
    public ResponseEntity<EntityRef> copyAccessPoint(String id, @Valid CopyAccessPointDetail copyAccessPointDetail) {
        ApAccessPoint accessPoint = accessPointService.getAccessPointByIdOrUuid(id);
        ApScope scope = accessPointService.getApScope(copyAccessPointDetail.getScope());
        ApAccessPoint copyAccessPoint = accessPointService.copyAccessPoint(accessPoint, scope, copyAccessPointDetail.getReplace(), 
                                                                           copyAccessPointDetail.getSkipItems());
        CachedAccessPoint cachedAccessPoint = apCacheService.findCachedAccessPoint(copyAccessPoint.getAccessPointId());
        EntityRef entityRef = apCacheService.createEntityRef(cachedAccessPoint);
        return ResponseEntity.ok(entityRef);
    }

    @Override
    @Transactional
    public ResponseEntity<Void> accessPointDeleteAccessPoint(String id, @Valid DeleteAccessPointDetail deleteAccessPointDetail) {
        ApAccessPoint accessPoint = accessPointService.getAccessPointByIdOrUuid(id);
        accessPointService.lockWrite(accessPoint);
        ApState apState = accessPointService.getStateInternal(accessPoint);
        ApAccessPoint replacedBy = null;
        boolean copyAll = false;
        if (deleteAccessPointDetail != null && deleteAccessPointDetail.getReplacedBy() != null) {
            if (deleteAccessPointDetail.getReplacedBy() != null) {
                replacedBy = accessPointService.getAccessPointByIdOrUuid(deleteAccessPointDetail.getReplacedBy());
            }
            copyAll = deleteAccessPointDetail.getReplaceType() != null
                    && deleteAccessPointDetail.getReplaceType() == ReplaceType.COPY_ALL;
        }
        ApRevision revision = revisionService.findRevisionByState(apState);
        if (revision != null) {
            revisionService.deleteRevision(apState, revision);
        }

        accessPointService.deleteAccessPoint(apState, replacedBy, copyAll);
        return ResponseEntity.ok().build();
    }

    @Override
    @Transactional
    public ResponseEntity<Void> deleteAccessPoints(@Valid DeleteAccessPointsDetail deleteAccessPointsDetail) {
        List<ApAccessPoint> accessPoints = accessPointService.getAccessPointsByIdOrUuid(deleteAccessPointsDetail.getIds());
        List<ApState> apStates = accessPointService.getStatesInternal(accessPoints);
        List<ApRevision> revisions = revisionService.findAllRevisionByStateIn(apStates);
        // TODO: Reimplement as one query/delete
        for (ApRevision revision : revisions) {
            revisionService.deleteRevision(revision.getState(), revision);
        }
        accessPointService.deleteAccessPoints(apStates);

        return ResponseEntity.ok().build();
    }

    @Override
    @Transactional
    public ResponseEntity<Void> restoreAccessPoint(String id) {
        ApAccessPoint accessPoint = accessPointService.getAccessPointByIdOrUuid(id);
        ApState apState = accessPointService.getStateInternal(accessPoint);

        accessPointService.restoreAccessPoint(apState);

        return ResponseEntity.ok().build();
    }

    @Override
    @Transactional
    public ResponseEntity<Void> validateAccessPoint(String id) {
        ApAccessPoint accessPoint = accessPointService.getAccessPointByIdOrUuid(id);
        ApState apState = accessPointService.getStateInternal(accessPoint);

        accessPointService.checkPermissionForEdit(apState);
        accessPointService.validate(accessPoint, apState, true);

        return ResponseEntity.ok().build();
    }

    /**
     * Změna stavu přístupového bodu.
     *
     * @param accessPointId identifikátor přístupového bodu
     * @param stateUpdate   nový stav přístupového bodu
     * @param apVersion     verze přístupového bodu
     * @return nová verze = verze + 1
     */
    @Override
    @Transactional
    public ResponseEntity<Integer> accessPointChangeState(Integer accessPointId, ApStateUpdate stateUpdate, Integer apVersion) {
        Validate.notNull(stateUpdate.getStateApproval(), "AP State is null");

        ApAccessPoint accessPoint = accessPointService.lockAccessPoint(accessPointId, apVersion);
        ApState state = accessPointService.getApState(accessPoint);
        ApRevision revision = revisionService.findRevisionByState(state);
        StateApproval newState = StateApproval.valueOf(stateUpdate.getStateApproval().toString());

        // Nelze změnit stav archivní entity, která má revizi
        if (revision != null) {
            throw new BusinessException("Nelze změnit stav archivní entity, která má revizi", RegistryCode.CANT_CHANGE_STATE_ENTITY_WITH_REVISION);
        }

        accessPointService.updateApState(accessPoint, newState, stateUpdate.getComment(), stateUpdate.getTypeId(), stateUpdate.getScopeId());
        accessPoint = accessPointService.updateAndValidate(accessPoint);
        if (accessPointService.isRevalidaceRequired(state.getStateApproval(), newState)) {
            ruleService.revalidateNodes(accessPoint.getAccessPointId());
        }
        apCacheService.createApCachedAccessPoint(accessPoint.getAccessPointId());

        return ResponseEntity.ok(accessPoint.getVersion());
    }

//    /**
//     * Založení nové části přístupového bodu.
//     *
//     * @param accessPointId identifikátor přístupového bodu (PK)
//     * @param apVersion     verze přístupového bodu
//     * @param apPartFormVO  data pro vytvoření části
//     * @return CreatedPart
//     */
//    @Override
//    @Transactional
//    public ResponseEntity<CreatedPart> createPart(Integer accessPointId, Integer apVersion, ApPartFormVO apPartForm) {
//
//        ApAccessPoint accessPoint = accessPointService.getAccessPoint(accessPointId);
//        ApState state = accessPointService.getStateInternal(accessPoint);
//        ApRevision revision = revisionService.findRevisionByState(state);
//
//        CreatedPart result = new CreatedPart();
//        if (revision != null) {
//            // Permission check is part of revisionService
//            ApRevPart revPart = revisionService.createPart(state, revision, apPartForm); // TODO temporary cover
//            result.setPartId(revPart.getPartId());
//        } else {
//            accessPointService.checkPermissionForEdit(state);
//
//            ApPart apPart = partService.createPart(accessPoint, apPartForm);
//            accessPointService.generateSync(state, apPart);
//            apCacheService.createApCachedAccessPoint(accessPointId);
//
//            result.setPartId(apPart.getPartId());
//        }
//
//        result.setApVersion(accessPointService.lockAccessPoint(accessPointId, apVersion));
//        return ResponseEntity.ok(result);
//    }

    /**
     * Smazání části přístupového bodu.
     *
     * @param accessPointId identifikátor přístupového bodu (PK)
     * @param partId        identifikátor mazané části
     * @param apVersion     verze přístupového bodu
     * @return nová verze = verze + 1
     */
    @Override
    @Transactional
    public ResponseEntity<Integer> accessPointDeletePart(Integer accessPointId, Integer partId, Integer apVersion) {

        ApAccessPoint accessPoint = accessPointService.lockAccessPoint(accessPointId, apVersion);
        ApState state = accessPointService.getStateInternal(accessPoint);

        ApRevision revision = revisionService.findRevisionByState(state);
        if (revision != null) {
            revisionService.deletePart(state, revision, partId);
        } else {
            accessPointService.checkPermissionForEdit(state);
            partService.deletePart(accessPoint, partId);
            accessPoint = accessPointService.updateAndValidate(accessPoint);
            apCacheService.createApCachedAccessPoint(accessPoint.getAccessPointId());
        }

        return ResponseEntity.ok(accessPoint.getVersion());
    }

//    /**
//     * Úprava části přístupového bodu.
//     * 
//     * V případě revize:
//     * 
//     * <ul>
//     * <li>1. Zalozeni noveho itemu
//     * id = null
//     * objectId = null
//     * origObjectId = null
//     * <li>2. Zmena itemu
//     * id = itemId (z puvodniho part)
//     * objectId = objectId (z puvodniho part)
//     * origObjectId = null
//     * <li>3. Vymazani itemu
//     * item neprijde
//     * </ul>
//     * 
//     * @param accessPointId     identifikátor přístupového bodu (PK)
//     * @param partId            identifikátor upravované části
//     * @param apVersion verze přístupového bodu
//     * @param apPartFormVO      data pro úpravu části
//     * @return nová verze = verze + 1
//     */
//    @Override
//    @Transactional
//    public ResponseEntity<Integer> updatePart(Integer accessPointId, 
//                                              Integer partId,
//                                              Integer apVersion,
//                                              ApPartFormVO apPartFormVO) {
//
//        ApAccessPoint accessPoint = accessPointService.getAccessPoint(accessPointId);
//        ApState state = accessPointService.getStateInternal(accessPoint);
//        ApPart apPart = partService.getPart(partId);
//        ApRevision revision = revisionService.findRevisionByState(state);
//        if (revision != null) {
//            revisionService.updatePart(state, revision, apPart, apPartFormVO);
//        } else {
//            if (accessPointService.updatePart(accessPoint, state, apPart, apPartFormVO)) {
//                apCacheService.createApCachedAccessPoint(accessPointId);
//            }
//        }
//
//        Integer version = accessPointService.lockAccessPoint(accessPointId, apVersion);
//        return ResponseEntity.ok(version);
//    }

    /**
     * Nastavení preferovaného jména přístupového bodu.
     * Možné pouze pro části typu Označení.
     *
     * @param accessPointId identifikátor přístupového bodu (PK)
     * @param partId        identifikátor části, kterou nastavujeme jako preferovanou
     * @param apVersion     verze přístupového bodu
     * @return nová verze = verze + 1
     */
    @Override
    @Transactional
    public ResponseEntity<Integer> accessPointSetPreferName(Integer accessPointId, Integer partId, Integer apVersion) {

        ApAccessPoint accessPoint = accessPointService.lockAccessPoint(accessPointId, apVersion);

        ApState state = accessPointService.getStateInternal(accessPoint);
        ApRevState revState = revisionService.findRevStateByState(state);
        if (revState != null) {
            revisionService.setPreferName(state, revState, partId, null);
        } else {
            accessPointService.checkPermissionForEdit(state);
            ApPart apPart = partService.getPart(partId);
            accessPoint = accessPointService.setPreferName(accessPoint, apPart);
            accessPoint = accessPointService.updateAndValidate(accessPoint);
            apCacheService.createApCachedAccessPoint(accessPoint.getAccessPointId());
        }

        return ResponseEntity.ok(accessPoint.getVersion());
    }

    @Override
    @Transactional
    public ResponseEntity<Void> createRevision(Integer id) {
        ApState state = accessPointService.getStateInternal(id);

        // Nelze vytvořit revizi, pokud má archivní entita jiný stav než NEW, TO_AMEND nebo APPROVED
        if (!Arrays.asList(StateApproval.NEW, StateApproval.TO_AMEND, StateApproval.APPROVED).contains(state.getStateApproval())) {
            throw new BusinessException("Nelze vytvořit revizi, protože archivní entita má nevhodný stav",
                    RegistryCode.CANT_CREATE_REVISION)
                            .set("state", state.getStateApproval());
        }

        revisionService.createRevision(state);
        return ResponseEntity.ok().build();
    }

    @Override
    @Transactional
    public ResponseEntity<Void> deleteRevision(Integer id) {
        ApState state = accessPointService.getStateInternal(id);

        revisionService.deleteRevision(state);
        return ResponseEntity.ok().build();
    }

    /**
     * Sloučení revize a přístupového bodu.
     * 
     * @param accessPointId  identifikátor přístupového bodu
     * @param stateUpdate    nový stav rvize přístupového bodu + komentář
     * @param apVersion      verze přístupového bodu
     * @return nová verze = verze + 1
     */
    @Override
    @Transactional
    public ResponseEntity<Integer> accessPointMergeRevision(Integer accessPointId, ApStateUpdate stateUpdate, Integer apVersion) {
        ApAccessPoint accessPoint = accessPointService.lockAccessPoint(accessPointId, apVersion);
        ApState apState = accessPointService.getStateInternal(accessPoint);
        StateApproval state = StateApproval.valueOf(stateUpdate.getStateApproval().toString());

        revisionService.mergeRevision(apState, state, stateUpdate.getComment());

        return ResponseEntity.ok(accessPoint.getVersion());
    }

    /**
     * Změna stavu revize přístupového bodu.
     *
     * @param accessPointId  identifikátor přístupového bodu
     * @param revStateChange nový stav rvize přístupového bodu
     * @param apVersion      verze přístupového bodu
     * @return nová verze = verze + 1
     */
    @Override
    @Transactional
    public ResponseEntity<Integer> accessPointChangeStateRevision(Integer accessPointId, RevStateChange revStateChange, Integer apVersion) {
        ApAccessPoint accessPoint = accessPointService.lockAccessPoint(accessPointId, apVersion);
        ApState state = accessPointService.getStateInternal(accessPoint);
        RevStateApproval revNextState = RevStateApproval.valueOf(revStateChange.getState().getValue());
        Integer nextTypeId = revStateChange.getTypeId();
        if (nextTypeId == null) {
            nextTypeId = state.getApTypeId();
        }

        revisionService.changeStateRevision(state, nextTypeId, revNextState, revStateChange.getComment());

        return ResponseEntity.ok(accessPoint.getVersion());
    }

//    /**
//     * Úprava části přístupového bodu z revize
//     *
//     * @param accessPointId identifikátor přístupového bodu (PK)
//     * @param partId        identifikátor upravované části
//     * @param apPartFormVO  data pro úpravu části
//     * @return nová verze = verze + 1
//     */
//    @Override
//    @Transactional
//    public ResponseEntity<Integer> updateRevisionPart(Integer accessPointId,
//                                   Integer partId,
//                                   Integer apVersion,
//                                   ApPartFormVO apPartFormVO) {
//
//        ApState state = accessPointService.getStateInternal(accessPointId);
//        ApRevision revision = revisionService.findRevisionByState(state);
//        ApRevPart revPart = revisionPartService.findById(partId);
//        revisionService.updatePart(state, revision, revPart, apPartFormVO);
//
//        Integer version = accessPointService.lockAccessPoint(accessPointId, apVersion);
//        return ResponseEntity.ok(version);
//    }

    /**
     * Smazání části revize přístupového bodu.
     *
     * @param accessPointId identifikátor přístupového bodu (PK)
     * @param partId        identifikátor mazané části revize
     * @param apVersion     verze přístupového bodu
     * @return nová verze = verze + 1
     */
    @Override
    @Transactional
    public ResponseEntity<Integer> accessPointDeleteRevisionPart(Integer accessPointId, Integer partId,
                                                                 Integer apVersion) {
        ApAccessPoint accessPoint = accessPointService.lockAccessPoint(accessPointId, apVersion);
        ApState state = accessPointService.getStateInternal(accessPoint);

        revisionService.deleteRevPart(state, partId);

        return ResponseEntity.ok(accessPoint.getVersion());
    }

    /**
     * Nastavení preferovaného jména revizi přístupového bodu.
     *
     * @param accessPointId identifikátor přístupového bodu (PK)
     * @param partId        identifikátor části, kterou nastavujeme jako preferovanou
     * @param apVersion     verze přístupového bodu
     * @return nová verze = verze + 1
     */
    @Override
    @Transactional
    public ResponseEntity<Integer> accessPointSetPreferNameRevision(Integer accessPointId, Integer partId, Integer apVersion) {
        ApAccessPoint accessPoint = accessPointService.lockAccessPoint(accessPointId, apVersion);
        ApState state = accessPointService.getStateInternal(accessPoint);

        revisionService.setPreferName(state, partId);

        return ResponseEntity.ok(accessPoint.getVersion());
    }

    @Override
    @Transactional
    public ResponseEntity<ResultAutoItems> accessPointGetAutoitems(String id) {
        Integer accessPointId = Integer.parseInt(id);
        ApState state;
        try {
            state = accessPointService.getApState(accessPointId);
        } catch (ObjectNotFoundException e) {
            return ResponseEntity.notFound().build();
        }

        List<GroovyItem> items = groovyService.processAPItems(state);

        return ResponseEntity.ok(convertGroovyItems(items));
    }

    @Override
    @Transactional
    public ResponseEntity<ResultAutoItems> getRevAutoitems(String id) {
        Integer accessPointId = Integer.parseInt(id);
        ApState state;
        try {
            state = accessPointService.getApState(accessPointId);
        } catch (ObjectNotFoundException e) {
            return ResponseEntity.notFound().build();
        }

        ApRevision revision = revisionService.findRevisionByState(state);
        if (revision == null) {
            return ResponseEntity.notFound().build();
        }

        List<GroovyItem> items = groovyService.processRevItems(state, revision);

        return ResponseEntity.ok(convertGroovyItems(items));
    }

    private ResultAutoItems convertGroovyItems(List<GroovyItem> items) {
        ResultAutoItems resultAutoItems = new ResultAutoItems();
        StaticDataProvider sdp = staticDataService.getData();
        for (GroovyItem item : items) {
            ItemType itemType = sdp.getItemTypeByCode(item.getTypeCode());
            Validate.notNull(itemType, "Incorrect item type: %s", item.getTypeCode());

            AutoValue autoValue = new AutoValue();
            autoValue.setItemSpecId(item.getSpecId());
            autoValue.setItemTypeId(itemType.getItemTypeId());
            autoValue.setValue(item.getValue());

            resultAutoItems.addItemsItem(autoValue);
        }

        return resultAutoItems;
    }
}
