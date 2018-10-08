package cz.tacr.elza.search;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.collections4.map.HashedMap;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.engine.impl.WorkPlan;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.spi.AbstractDocumentBuilder;
import org.hibernate.search.engine.spi.DocumentBuilderContainedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.service.DescriptionItemService;
import cz.tacr.elza.service.SearchWorkService;

/**
 * @author <a href="mailto:stepan.marek@coreit.cz">Stepan Marek</a>
 */
@Component
public class SearchWorkIndexService {

    private static final Logger logger = LoggerFactory.getLogger(SearchWorkIndexService.class);

    private ExtendedSearchIntegrator integrator;

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private SearchWorkService searchWorkService;

    @Autowired
    private DescriptionItemService descriptionItemService;

    private Map<Class, SearchIndexSupport> repositoryMap = new HashedMap<>();

    @PostConstruct
    public void init() {
        FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);
        this.integrator = fullTextEntityManager.getSearchFactory().unwrap(ExtendedSearchIntegrator.class);
        // AbstractDocumentBuilder builder = getEntityBuilder(integrator, ArrDescItem.class);
        repositoryMap.put(ArrDescItem.class, descriptionItemService);
    }

    @Transactional
    public void processBatch(Class entityClass, Set<Integer> entityIdList) {

        Map<Integer, Object> entityMap = findAll(entityClass, entityIdList);

        entityIdList.removeAll(entityMap.keySet());

        WorkPlan plan = new WorkPlan(integrator);
        for (Integer id : entityIdList) {
            plan.addWork(new Work(entityClass, id, WorkType.DELETE));
        }

        for (Map.Entry<Integer, Object> entry : entityMap.entrySet()) {
            plan.addWork(new Work(entry.getValue(), entry.getKey(), WorkType.INDEX));
        }

        List<LuceneWork> queue = plan.getPlannedLuceneWork();

        EntityIndexBinding binding = integrator.getIndexBindings().get(entityClass);
        IndexManager[] indexManagers = binding.getIndexManagers();

        IndexManager indexManager = integrator.getIndexManager(entityClass.getName());
        if (indexManager == null) {
            logger.error("Received a remote message about an unknown index '{}': discarding message!", entityClass);
            return;
        }

        indexManager.performOperations(queue, null);
    }

    private <T> Map<Integer, T> findAll(Class<T> entityClass, Collection<Integer> ids) {
        SearchIndexSupport repository = repositoryMap.get(entityClass);
        return repository.findToIndex(ids);
    }

    private static AbstractDocumentBuilder getEntityBuilder(ExtendedSearchIntegrator extendedIntegrator, Class<?> entityClass) {
        EntityIndexBinding entityIndexBinding = extendedIntegrator.getIndexBinding(entityClass);
        if (entityIndexBinding == null) {
            DocumentBuilderContainedEntity entityBuilder = extendedIntegrator.getDocumentBuilderContainedEntity(entityClass);
            if (entityBuilder == null) {
                // should never happen but better be safe than sorry
                throw new SearchException("Unable to perform work. Entity Class is not @Indexed nor hosts @ContainedIn: " + entityClass);
            } else {
                return entityBuilder;
            }
        } else {
            return entityIndexBinding.getDocumentBuilder();
        }
    }
}
