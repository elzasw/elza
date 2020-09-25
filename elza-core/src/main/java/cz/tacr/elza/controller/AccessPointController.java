package cz.tacr.elza.controller;

import javax.transaction.Transactional;
import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.controller.vo.DeleteAccessPointDetail;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.service.AccessPointService;

@RestController
@RequestMapping("/api/v1")
public class AccessPointController implements AccesspointsApi {

    @Autowired
    ApAccessPointRepository apAccessPointRepository;

    @Autowired
    AccessPointService accessPointService;

    @Override
    @Transactional
    public ResponseEntity<Void> deleteAccessPoint(String id, @Valid DeleteAccessPointDetail deleteAccessPointDetail) {
        ApAccessPoint accessPoint = findAccessPointByIdOrUuid(id);
        ApState apState = accessPointService.getState(accessPoint);
        ApAccessPoint replacedBy = null;
        if (deleteAccessPointDetail != null) {
            replacedBy = findAccessPointByIdOrUuid(deleteAccessPointDetail.getReplacedBy());
        }
        accessPointService.deleteAccessPoint(apState, replacedBy);
        return ResponseEntity.ok().build();
    }

    private ApAccessPoint findAccessPointByIdOrUuid(String id) {
        ApAccessPoint accessPoint;
        if (!StringUtils.isNumeric(id)) {
            accessPoint = apAccessPointRepository.findApAccessPointByUuid(id);
        } else {
            accessPoint = apAccessPointRepository.findById(Integer.valueOf(id)).orElse(null);
        }
        if (accessPoint == null) {
            throw new ObjectNotFoundException("Přístupový bod neexistuje", BaseCode.ID_NOT_EXIST);
        }
        return accessPoint;
    }

}
