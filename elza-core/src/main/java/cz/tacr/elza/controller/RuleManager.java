package cz.tacr.elza.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.domain.ArrArrangementType;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.repository.ArrangementTypeRepository;
import cz.tacr.elza.repository.RuleSetRepository;

/**
 * API pro práci s pravidly.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 30. 7. 2015
 */
@RestController
@RequestMapping("/api/ruleSetManager")
public class RuleManager implements cz.tacr.elza.api.controller.RuleManager {

    @Autowired
    private RuleSetRepository ruleSetRepository;

    @Autowired
    private ArrangementTypeRepository arrangementTypeRepository;

    @Override
    @RequestMapping(value = "/getRuleSets", method = RequestMethod.GET)
    public List<RulRuleSet> getRuleSets() {
        return ruleSetRepository.findAll();
    }

    @RequestMapping(value = "/getArrangementTypes", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ArrArrangementType> getArrangementTypes() {
        return arrangementTypeRepository.findAll();
    }
}
