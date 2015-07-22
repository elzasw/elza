package cz.tacr.elza.controller;

import cz.tacr.elza.domain.FindingAid;
import cz.tacr.elza.repository.FindingAidRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@RestController
@RequestMapping("/findingAid")
public class FindingAidController {

    @Autowired
    FindingAidRepository findingAidRepository;

    @RequestMapping(value = "/list", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE)
    List<FindingAid> list() {
        return findingAidRepository.findAll();
    }

}
