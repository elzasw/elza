package cz.tacr.elza.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.RulRuleSetVO;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.repository.ArrangementTypeRepository;

/**
 * Kontroler pro pravidla.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 7. 1. 2016
 */
@RestController
@RequestMapping("/api/ruleSetManagerV2")
public class RuleController {

    @Autowired
    private ArrangementTypeRepository arrangementTypeRepository;

    @Autowired
    private ClientFactoryVO factoryVo;

    @RequestMapping(value = "/getRuleSets", method = RequestMethod.GET)
    public List<RulRuleSetVO> getRuleSets() {
        Map<Integer, RulRuleSetVO> ruleSets = new LinkedHashMap<>();
        arrangementTypeRepository.findAllFetchRuleSets().forEach(arrType -> {
            RulRuleSet ruleSet = arrType.getRuleSet();
            RulRuleSetVO ruleSetVO = factoryVo.getOrCreateVo(ruleSet.getRuleSetId(), ruleSet, ruleSets, RulRuleSetVO.class);
            ruleSetVO.getArrangementTypes().add(factoryVo.createArrangementType(arrType));
        });


        return new ArrayList<RulRuleSetVO>(ruleSets.values());
    }
}
