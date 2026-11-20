package cz.tacr.elza.service.output;

import java.nio.file.Path;

import org.springframework.stereotype.Component;

import cz.tacr.elza.core.ElzaLocale;
import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.dataexchange.output.filters.FilterRules;
import cz.tacr.elza.dataexchange.output.filters.NodeRestrictionEvaluator;
import cz.tacr.elza.dataexchange.output.sections.StructObjectInfoLoader;
import cz.tacr.elza.domain.RulOutputFilter;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.print.OutputFilterConfig;
import cz.tacr.elza.repository.OutputFilterRepository;
import cz.tacr.elza.service.DataService;
import cz.tacr.elza.service.cache.AccessPointCacheProvider;
import cz.tacr.elza.service.cache.AccessPointCacheService;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

/**
 * Creates evaluators of output filters outside of output generation, e.g. for bulk actions
 * which compute output items for outputs with a filter.
 */
@Component
public class NodeRestrictionEvaluatorFactory {

    private final OutputFilterRepository outputFilterRepository;

    private final ResourcePathResolver resourcePathResolver;

    private final StaticDataService staticDataService;

    private final AccessPointCacheService accessPointCacheService;

    private final DataService dataService;

    private final ElzaLocale elzaLocale;

    private final EntityManager em;

    public NodeRestrictionEvaluatorFactory(final OutputFilterRepository outputFilterRepository,
                                           final ResourcePathResolver resourcePathResolver,
                                           final StaticDataService staticDataService,
                                           final AccessPointCacheService accessPointCacheService,
                                           final DataService dataService,
                                           final ElzaLocale elzaLocale,
                                           final EntityManager em) {
        this.outputFilterRepository = outputFilterRepository;
        this.resourcePathResolver = resourcePathResolver;
        this.staticDataService = staticDataService;
        this.accessPointCacheService = accessPointCacheService;
        this.dataService = dataService;
        this.elzaLocale = elzaLocale;
        this.em = em;
    }

    /**
     * Creates a new evaluator for a single tree walk.
     *
     * @param ruleSetId
     *            rule set which defines the output filter
     * @param outputFilterCode
     *            code of the output filter
     * @throws BusinessException
     *             when the rule set has no such output filter
     */
    @Transactional(Transactional.TxType.MANDATORY)
    public NodeRestrictionEvaluator create(int ruleSetId, String outputFilterCode) {
        RulOutputFilter outputFilter = outputFilterRepository.findByRuleSetIdAndCode(ruleSetId, outputFilterCode);
        if (outputFilter == null) {
            throw new BusinessException("Output filter not found, code: " + outputFilterCode, BaseCode.ID_NOT_EXIST)
                    .set("outputFilterCode", outputFilterCode)
                    .set("ruleSetId", ruleSetId);
        }
        Path outputFilterFile = resourcePathResolver.getOutputFilterFile(outputFilter);
        OutputFilterConfig outputFilterConfig = OutputFilterConfig.load(outputFilterFile);

        StaticDataProvider sdp = staticDataService.getData();
        FilterRules filterRules = new FilterRules(outputFilterConfig, sdp);
        return new NodeRestrictionEvaluator(filterRules, sdp,
                new AccessPointCacheProvider(accessPointCacheService),
                elzaLocale.getLocale(),
                new StructObjectInfoLoader(em, 1, sdp, dataService));
    }
}
