package cz.tacr.elza.drools;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.kie.api.runtime.StatelessKieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.RulEntityRule;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.drools.model.ModelValidation;

@Component
public class ModelValidationRules extends Rules {

    public static final Logger logger = LoggerFactory.getLogger(AvailableItemsRules.class);

    @Autowired
    private ResourcePathResolver resourcePathResolver;

    @Autowired
    private StaticDataService staticDataService;


    public synchronized ModelValidation execute(final List<RulEntityRule> rules,
                                               final ModelValidation modelValidation) throws Exception {
        
        long startTime = System.currentTimeMillis();

        List<Object> facts = new ArrayList<>(2 + modelValidation.getModelParts().size() + modelValidation.getItems()
                .size());
        facts.add(modelValidation.getAp());
        facts.add(modelValidation.getGeoModel());
        facts.addAll(modelValidation.getModelParts());
        facts.addAll(modelValidation.getItems());
        facts.add(modelValidation.getExpectedItems());

        logger.debug("Model (workerId: {}) for AP created",
                     Thread.currentThread().getId(),
                     System.currentTimeMillis() - startTime);

        for (RulEntityRule rule : rules) {
            Path path = resourcePathResolver.getDroolFile(rule);

            StatelessKieSession ksession = createKieStatelessSession(path);
            // messages with a key belong to the package of the rule
            RulPackage rulPackage = staticDataService.getData().getPackageById(rule.getPackageId());
            modelValidation.getApValidationErrors().setPackageCode(rulPackage != null ? rulPackage.getCode() : null);
            ksession.setGlobal("results", modelValidation.getApValidationErrors());
            executeStateless(ksession, facts);
        }

        logger.debug("Finalized results (workerId: {}) in {}ms from start",
                     Thread.currentThread().getId(),
                     System.currentTimeMillis() - startTime);
        return modelValidation;
    }
}
