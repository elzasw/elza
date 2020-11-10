package cz.tacr.elza.controller;

import java.util.List;

import javax.validation.Valid;

import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.repository.DaoRepository;
import cz.tacr.elza.service.DaoSyncService;
import cz.tacr.elza.ws.types.v1.Items;
import io.swagger.annotations.ApiParam;

@RestController
@RequestMapping("/api/v1")
public class DaoController implements DaosApi {

    @Autowired
    DaoRepository daoRepository;

    @Autowired
    DaoSyncService daoSyncService;

    @Override
    public ResponseEntity<Void> changeLinkScenario(@ApiParam(value = "Identifikátor dao", required = true) @PathVariable("id") Integer id,
                                                   @ApiParam(value = "Nový scénář", required = true) @Valid @RequestBody String body) {
        // TODO: Change scenario
        return new ResponseEntity<Void>(HttpStatus.OK);
    }
}
