package cz.tacr.elza.drools;

import java.nio.file.Path;
import java.util.List;

import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.domain.RulExtensionRule;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.drools.model.Ap;
import cz.tacr.elza.drools.model.ItemType;
import cz.tacr.elza.drools.model.ModelAvailable;
import cz.tacr.elza.drools.model.item.AbstractItem;

@Component
public class AvailableItemsRules extends Rules {

    public static final Logger logger = LoggerFactory.getLogger(AvailableItemsRules.class);

    @Autowired
    private ResourcePathResolver resourcePathResolver;


    public synchronized ModelAvailable execute(final List<RulExtensionRule> rules,
                                               final ModelAvailable modelAvailable) throws Exception {
        Ap ap = modelAvailable.getAp();
        logger.debug("Executing rules for AccessPoint, accessPointId: {}", ap.getId());

        for (RulExtensionRule rule : rules) {
            Path path = resourcePathResolver.getDroolFile(rule);
            KieSession kSession = createKieSession(path);
            kSession.insert(ap);
            kSession.insert(modelAvailable.getPart());
            for (ItemType itemType : modelAvailable.getItemTypes()) {
                kSession.insert(itemType);
            }
            for (AbstractItem item : modelAvailable.getItems()) {
                kSession.insert(item);
            }
            kSession.fireAllRules();
            kSession.dispose();
        }

        logger.debug("Finished executing rules for AccessPoint, accessPointId: {}", ap.getId());
        return modelAvailable;
    }

    public synchronized List<ItemType> execute(final RulRuleSet rulRuleSet,
                                               List<ItemType> itemTypeList) throws Exception {
        Path path = resourcePathResolver.getDroolFile(rulRuleSet);
        KieSession kSession = createKieSession(path);
        for (ItemType itemType : itemTypeList) {
            kSession.insert(itemType);
        }
        kSession.fireAllRules();
        kSession.dispose();
        return itemTypeList;
    }

}
