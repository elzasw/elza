package cz.tacr.elza.controller;

import javax.transaction.Transactional;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.controller.vo.DeleteAccessPointDetail;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.service.AccessPointService;

@RestController
@RequestMapping("/api/v1")
public class AccessPointController implements AccesspointsApi {

    @Autowired
    AccessPointService accessPointService;

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
                    && deleteAccessPointDetail.getReplaceType().equals("COPY_ALL");
        }
        accessPointService.deleteAccessPoint(apState, replacedBy, copyAll);
        return ResponseEntity.ok().build();
    }

}
