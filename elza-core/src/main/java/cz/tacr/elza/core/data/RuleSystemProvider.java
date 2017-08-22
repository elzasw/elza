package cz.tacr.elza.core.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;

import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.repository.PacketTypeRepository;
import cz.tacr.elza.repository.RuleSetRepository;

public class RuleSystemProvider {

    private Map<Integer, RuleSystem> ruleSetIdMap;

    private Map<String, RuleSystem> ruleSetCodeMap;

    RuleSystemProvider() {
    }

    public RuleSystem getByRuleSetId(int id) {
        return ruleSetIdMap.get(id);
    }

    public RuleSystem getByRuleSetCode(String code) {
        Assert.hasLength(code);
        return ruleSetCodeMap.get(code);
    }

    /**
     * Init all values. Method must be called inside transaction and synchronized.
     */
    void init(RuleSetRepository ruleSetRepository,
              PacketTypeRepository packetTypeRepository,
              ItemTypeRepository itemTypeRepository,
              ItemSpecRepository itemSpecRepository) {
        List<RulRuleSet> ruleSets = ruleSetRepository.findAll();

        Map<Integer, RuleSystem> idMap = new HashMap<>(ruleSets.size());
        Map<String, RuleSystem> codeMap = new HashMap<>(ruleSets.size());

        for (RulRuleSet rs : ruleSets) {
            // create initialized rule system
            RuleSystem ruleSystem = new RuleSystem(rs);
            ruleSystem.init(packetTypeRepository, itemTypeRepository, itemSpecRepository);

            // update lookups
            idMap.put(rs.getRuleSetId(), ruleSystem);
            codeMap.put(rs.getCode(), ruleSystem);
        }
        // update fields
        ruleSetIdMap = idMap;
        ruleSetCodeMap = codeMap;
    }
}
