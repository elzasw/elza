package cz.tacr.elza.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.domain.FindingAid;
import cz.tacr.elza.repository.FindingAidRepository;

/**
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@RestController
@RequestMapping("/arrangementManager")
public class ArrangementManager {

    @Autowired
    private FindingAidRepository findingAidRepository;

    /**
     * Vytvoří novou archivní pomůcku se zadaným názvem. Jako datum založení vyplní aktuální datum a čas.
     *
     * @param name název archivní pomůcky
     */
    @RequestMapping(value = "/createFindingAid", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE, params = {"name"})
    public void createFindingAid(@RequestParam(value="name") final String name) {
        //        Assert.notNull(name);

        FindingAid findingAid = new FindingAid();
        findingAid.setCreateDate(LocalDateTime.now());
        findingAid.setName(name);
        findingAidRepository.save(findingAid);
    }

    /**
     * Smaže archivní pomůcku se zadaným id. Maže kompletní strukturu se všemi závislostmi.
     *
     * @param idArchivniPomucka id archivní pomůcky
     */
    @RequestMapping(value = "/deleteFindingAid", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE, params = {"findingAidId"})
    public void deleteFindingAid(@RequestParam(value="findingAidId") final Integer findingAidId) {
        Assert.notNull(findingAidId);

        findingAidRepository.delete(findingAidId);
    }

    /**
     * Vrátí všechny archivní pomůcky.
     *
     * @return všechny archivní pomůcky
     */
    @RequestMapping(value = "/getFindingAids", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<FindingAid> getFindingAids() {
        return findingAidRepository.findAll();
    }

    /**
     * Aktualizace názvu archivní pomůcky.
     *
     * @param findingAidId id archivní pomůcky
     * @param name název arhivní pomůcky
     *
     * @return
     */
    @RequestMapping(value = "/updateFindingAid", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE, params = {"findingAidId", "name"})
    public void updateFindingAid(@RequestParam(value="findingAidId") final Integer findingAidId, @RequestParam(value="name") final String name) {
        Assert.notNull(findingAidId);
        Assert.notNull(name);

        FindingAid findingAid = findingAidRepository.getOne(findingAidId);
        findingAid.setName(name);
        findingAidRepository.save(findingAid);
    }
}
