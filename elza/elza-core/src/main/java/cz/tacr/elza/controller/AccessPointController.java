package cz.tacr.elza.controller;

import java.util.List;

import javax.transaction.Transactional;
import javax.validation.Valid;

import cz.tacr.elza.controller.vo.RevisionState;
import cz.tacr.elza.service.RevisionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.controller.vo.DeleteAccessPointDetail;
import cz.tacr.elza.controller.vo.DeleteAccessPointsDetail;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.service.AccessPointService;

@RestController
@RequestMapping("/api/v1")
public class AccessPointController implements AccesspointsApi {

    @Autowired
    AccessPointService accessPointService;

    @Autowired
    RevisionService revisionService;

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

    @Override
    @Transactional
    public ResponseEntity<Void> changeStateRevision(Integer id, @Valid RevisionState revisionState) {
        ApState state = accessPointService.getStateInternal(id);
        revisionService.changeStateRevision(state, revisionState);
        return ResponseEntity.ok().build();
    }

}
