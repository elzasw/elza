package cz.tacr.elza.controller;

import javax.transaction.Transactional;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.service.DaoSyncService;
import io.swagger.annotations.ApiParam;

@RestController
@RequestMapping("/api/v1")
public class DaoController implements DaosApi {

    @Autowired
    DaoSyncService daoSyncService;

    @Override
    @Transactional
    public ResponseEntity<Void> changeLinkScenario(@ApiParam(value = "Identifikátor dao", required = true) @PathVariable("id") Integer id,
                                                   @ApiParam(value = "Nový scénář", required = true) @Valid @RequestBody String body) {
        // read json - strip "
        if (body.startsWith("\"")) {
            body = body.substring(1, body.length() - 1);
        }
        daoSyncService.changeScenario(id, body);
        return new ResponseEntity<Void>(HttpStatus.OK);
    }
}
