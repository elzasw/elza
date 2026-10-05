package cz.tacr.elza.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.controller.vo.SetupAdminVO;
import cz.tacr.elza.controller.vo.SetupStatusVO;
import cz.tacr.elza.service.SetupService;

/**
 * First-run setup; available without login (see {@link SetupService}).
 */
@RestController
@RequestMapping("/api/v1")
public class SetupController implements SetupApi {

    @Autowired
    private SetupService setupService;

    @Override
    public ResponseEntity<SetupStatusVO> setupGetSetupStatus() {
        SetupStatusVO result = new SetupStatusVO();
        result.setSetupRequired(setupService.isSetupRequired());
        return ResponseEntity.ok(result);
    }

    @Override
    public ResponseEntity<Void> setupCreateSetupAdmin(SetupAdminVO params) {
        setupService.createAdmin(params.getUsername(), params.getPassword());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
