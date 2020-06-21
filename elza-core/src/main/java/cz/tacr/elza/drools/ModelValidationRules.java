package cz.tacr.elza.drools;

import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.domain.RulExtensionRule;
import cz.tacr.elza.drools.model.ModelPart;
import cz.tacr.elza.drools.model.ModelValidation;
import cz.tacr.elza.drools.model.item.AbstractItem;
import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Component
public class ModelValidationRules extends Rules {

    public static final Logger logger = LoggerFactory.getLogger(AvailableItemsRules.class);

    @Autowired
    private ResourcePathResolver resourcePathResolver;


    public synchronized ModelValidation execute(final List<RulExtensionRule> rules,
                                               final ModelValidation modelValidation) throws Exception {

        for (RulExtensionRule rule : rules) {
            Path path = resourcePathResolver.getDroolFile(rule);
            KieSession kSession = createKieSession(path);
            kSession.insert(modelValidation.getAp());
            kSession.insert(modelValidation.getGeoModel());
            for (ModelPart modelPart : modelValidation.getModelParts()) {
                kSession.insert(modelPart);
            }
            for (AbstractItem item : modelValidation.getItems()) {
                kSession.insert(item);
            }
            kSession.setGlobal("results", modelValidation.getApValidationErrors());
            kSession.fireAllRules();
            kSession.dispose();
        }
        return modelValidation;
    }
}
