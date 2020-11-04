package cz.tacr.elza.controller;

import java.util.List;

import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.repository.DaoRepository;
import cz.tacr.elza.service.DaoSyncService;
import cz.tacr.elza.ws.types.v1.Items;

@RestController
public class DaoController {

    @Autowired
    DaoRepository daoRepository;

    @Autowired
    DaoSyncService daoSyncService;

    /**
     * Načtení seznamu dostupných scénářů
     * 
     * @param daoId
     * @return List<String> nebo null
     */
    @RequestMapping(value = "/api/dao/getScenarios", method = RequestMethod.GET)
    public List<String> getLinkScenarios(Integer daoId) {
        ArrDao dao = daoRepository.getOne(daoId);
        Validate.notNull(dao, "Požadovaný dao objekt daoId={} neexistuje", daoId);

        Items items = daoSyncService.unmarshalItemsFromAttributes(dao.getAttributes(), daoId);
        if (items != null) {
            return daoSyncService.findAllScenarios(items);
        }
        return null;
    }

}
