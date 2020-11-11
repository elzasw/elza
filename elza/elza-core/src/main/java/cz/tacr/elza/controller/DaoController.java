package cz.tacr.elza.controller;

import java.util.Collection;
import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.DigitizationCode;
import cz.tacr.elza.exception.codes.PackageCode;
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
        ArrDao dao = daoRepository.getOne(id);
        if (dao == null) {
            throw new ObjectNotFoundException("ArrDao ID=" + id + " not found", DigitizationCode.DAO_NOT_FOUND).set("daoId", id);
        }

        List<String> scenarios = null;
        Items items = daoSyncService.unmarshalItemsFromAttributes(dao.getAttributes(), id);
        if (items != null) {
            scenarios = daoSyncService.getAllScenarioNames(items);
        }
        if (!CollectionUtils.isEmpty(scenarios)) {
            if (scenarios.contains(body)) {
                // TODO: Change scenario
                //
                
            } else {
                throw new BusinessException("Specified scenario not found", PackageCode.SCENARIO_NOT_FOUND).set("scenario", body);
            }
        } else {
            throw new BusinessException("Scenario list is empty", PackageCode.SCENARIO_NOT_FOUND);
        }

        return new ResponseEntity<Void>(HttpStatus.OK);
    }
}
