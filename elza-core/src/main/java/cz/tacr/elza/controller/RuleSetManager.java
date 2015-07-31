package cz.tacr.elza.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.domain.RuleSet;
import cz.tacr.elza.repository.RuleSetRepository;

/**
 * API pro práci s pravidly.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 30. 7. 2015
 */
@RestController
@RequestMapping("/ruleSetManager")
public class RuleSetManager {

    @Autowired
    private RuleSetRepository ruleSetRepository;

    /**
     * Vrátí všechny sady pravidel.
     *
     * @return všechny sady pravidel
     */
    @RequestMapping(value = "/getRuleSets", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<RuleSet> getRuleSets() {
        return ruleSetRepository.findAll();
    }
}
