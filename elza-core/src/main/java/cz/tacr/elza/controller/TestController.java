package cz.tacr.elza.controller;

import cz.tacr.elza.domain.FindingAid;
import cz.tacr.elza.repository.FindingAidRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 6.8.15
 */
@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    FindingAidRepository findingAidRepository;

    // http://localhost/api/test/3
    @RequestMapping(value = "/{findingAidId}")
    public FindingAid getPath(@PathVariable("findingAidId") Integer findingAidId) {
        return findingAidRepository.findOne(findingAidId);
    }

    // http://localhost/api/test?findingAidId=3
    @RequestMapping
    public FindingAid getParam(@RequestParam("findingAidId") Integer findingAidId) {
        return findingAidRepository.findOne(findingAidId);
    }

    /* http://localhost/api/test /PUT/
    {
        "findingAidId": 3,
        "name": "Modifikovano !!!"
    }
    */
    @RequestMapping(method = RequestMethod.PUT)
    public FindingAid putBody(@RequestBody FindingAid findingAid) {
        findingAid.setName(findingAid.getName() + "!!!!");
        return findingAid;
    }

    // http://localhost/api/test/mod?findingAidId=3&name=Nove+jmeno
    @RequestMapping(value = "/mod")
    public FindingAid putParam(@RequestParam("findingAidId") Integer findingAidId, @RequestParam("name") String name) {
        FindingAid findingAid = findingAidRepository.findOne(findingAidId);
        findingAid.setName(name);
        return findingAid;
    }

}
