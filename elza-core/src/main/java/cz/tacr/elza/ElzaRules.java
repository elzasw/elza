package cz.tacr.elza;


import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.repository.FindingAidVersionRepository;


/**
 * Nastavení strategií pravidel.
 *
 * @author Martin Šlapa
 * @since 10.12.2015
 */
@Component
@ConfigurationProperties(prefix = "elza")
public class ElzaRules {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private FindingAidVersionRepository findingAidVersionRepository;

    /**
     * Seznam strategii podle pravidel.
     */
    private Map<String, Map<String, List<String>>> rules;

    public Map<String, Map<String, List<String>>> getRules() {
        return rules;
    }

    public void setRules(final Map<String, Map<String, List<String>>> rules) {
        this.rules = rules;
    }

    /**
     * Vrací seznam strategií.
     *
     * @param versionId identifikátor verze AP
     * @return seznam strategií
     */
    public Set<String> getStrategies(final Integer versionId) {
        Assert.notNull(versionId);

        ArrFindingAidVersion version = findingAidVersionRepository.findOne(versionId);

        Assert.notNull(version, "Verzev s ID:" + versionId + " neexistuje");

        String ruleCode = version.getRuleSet().getCode();

        Map<String, List<String>> ruleMap = rules.get(ruleCode);

        if (ruleMap == null) {
            logger.warn("Nejsou nastavené strategie pro kód pravidel: " + ruleCode);
            return new HashSet<>();
        }

        List<String> strategies = ruleMap.get("version_" + versionId);

        if (strategies == null) {
            strategies = ruleMap.get("default");
            if (strategies == null) {
                throw new IllegalStateException("Nejsou nastavené výchozí (default) strategie pro kód pravidel: " + ruleCode);
            }
        }

        return new HashSet<>(strategies);
    }

}
