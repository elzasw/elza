package cz.tacr.elza.controller;

import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.ParInstitutionVO;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.repository.InstitutionRepository;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.transaction.Transactional;
import java.util.List;

/**
 * Kontrolér pro osoby.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 21.12.2015
 */
@RestController
@RequestMapping(value = "/api/party", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
public class PartyController {

    @Autowired
    private ClientFactoryVO factoryVo;

    @Autowired
    private InstitutionRepository institutionRepository;

    /**
     * Načte seznam institucí
     * @return seznam institucí
     */
    @RequestMapping(value = "/institutions", method = RequestMethod.GET)
    @Transactional
    public List<ParInstitutionVO> getInstitutions(@RequestParam(value = "hasFund", required = false) Boolean hasFund) {
        List<ParInstitution> instsFromDB;
        if (hasFund == null) {
            //findAll()
            instsFromDB = institutionRepository.findAllWithFetch();
        } else if (hasFund) {
            instsFromDB = institutionRepository.findListByFundFetchAccessPointFetchPreferredPart();
        } else {
            throw new NotImplementedException("Search for institutions without Funds is not implemented.");
        }
        return factoryVo.createInstitutionList(instsFromDB);
    }
}
