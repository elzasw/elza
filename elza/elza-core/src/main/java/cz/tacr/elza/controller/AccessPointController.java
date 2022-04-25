package cz.tacr.elza.controller;

import java.util.Arrays;
import java.util.List;

import javax.transaction.Transactional;
import javax.validation.Valid;

import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.controller.vo.AutoValue;
import cz.tacr.elza.controller.vo.DeleteAccessPointDetail;
import cz.tacr.elza.controller.vo.DeleteAccessPointsDetail;
import cz.tacr.elza.controller.vo.ResultAutoItems;
import cz.tacr.elza.controller.vo.RevStateChange;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApRevision;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApState.StateApproval;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.RegistryCode;
import cz.tacr.elza.groovy.GroovyItem;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.GroovyService;
import cz.tacr.elza.service.RevisionService;

@RestController
@RequestMapping("/api/v1")
public class AccessPointController implements AccesspointsApi {

    @Autowired
    AccessPointService accessPointService;

    @Autowired
    StaticDataService staticDataService; 

    @Autowired
    RevisionService revisionService;

    @Autowired
    GroovyService groovyService;

    @Override
    @Transactional
    public ResponseEntity<Void> deleteAccessPoint(String id, @Valid DeleteAccessPointDetail deleteAccessPointDetail) {
        ApAccessPoint accessPoint = accessPointService.getAccessPointByIdOrUuid(id);
        ApState apState = accessPointService.getStateInternal(accessPoint);
        ApAccessPoint replacedBy = null;
        boolean copyAll = false;
        if (deleteAccessPointDetail != null && deleteAccessPointDetail.getReplacedBy() != null) {
            if (deleteAccessPointDetail.getReplacedBy() != null) {
                replacedBy = accessPointService.getAccessPointByIdOrUuid(deleteAccessPointDetail.getReplacedBy());
            }
            copyAll = deleteAccessPointDetail.getReplaceType() != null 
                    && deleteAccessPointDetail.getReplaceType() == DeleteAccessPointDetail.ReplaceTypeEnum.COPY_ALL;
        }
        accessPointService.deleteAccessPoint(apState, replacedBy, copyAll);
        return ResponseEntity.ok().build();
    }

    @Override
    @Transactional
    public ResponseEntity<Void> deleteAccessPoints(@Valid DeleteAccessPointsDetail deleteAccessPointsDetail) {
        List<ApAccessPoint> accessPoints = accessPointService.getAccessPointsByIdOrUuid(deleteAccessPointsDetail.getIds());
        List<ApState> apStates = accessPointService.getStatesInternal(accessPoints);
        accessPointService.deleteAccessPoints(apStates);

        return ResponseEntity.ok().build();
    }

    @Override
    @Transactional
    public ResponseEntity<Void> createRevision(Integer id) {
        ApState state = accessPointService.getStateInternal(id);
        accessPointService.checkPermissionForEditingConfirmed(state);

        // Nelze vytvořit revizi, pokud má archivní entita jiný stav než NEW, TO_AMEND nebo APPROVED
        if (!Arrays.asList(StateApproval.NEW, StateApproval.TO_AMEND, StateApproval.APPROVED).contains(state.getStateApproval())) {
            throw new BusinessException("Nelze vytvořit revizi, protože archivní entita má nevhodný stav", RegistryCode.CANT_CREATE_REVISION)
                .set("state", state.getStateApproval());
        }

        revisionService.createRevision(state);
        return ResponseEntity.ok().build();
    }

    @Override
    @Transactional
    public ResponseEntity<Void> deleteRevision(Integer id) {
        ApState state = accessPointService.getStateInternal(id);
        accessPointService.checkPermissionForEditingConfirmed(state);

        revisionService.deleteRevision(state);
        return ResponseEntity.ok().build();
    }

    @Override
    @Transactional
    public ResponseEntity<Void> changeStateRevision(Integer id, RevStateChange revStateChange) {
        ApState state = accessPointService.getStateInternal(id);
        revisionService.changeStateRevision(state, revStateChange);
        return ResponseEntity.ok().build();
    }

    @Override
    @Transactional
    public ResponseEntity<Void> deleteRevisionPart(Integer id, Integer partId) {
        ApState state = accessPointService.getStateInternal(id);
        accessPointService.checkPermissionForEditingConfirmed(state);

        revisionService.deletePart(state, partId);
        return ResponseEntity.ok().build();
    }

    @Override
    @Transactional
    public ResponseEntity<ResultAutoItems> getAutoitems(String id) {
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

    @Override
    @Transactional
    public ResponseEntity<Void> setPreferNameRevision(Integer id, Integer partId) {
        ApState state = accessPointService.getStateInternal(id);
        revisionService.setPreferName(state, partId);
        return ResponseEntity.ok().build();
    }

}
