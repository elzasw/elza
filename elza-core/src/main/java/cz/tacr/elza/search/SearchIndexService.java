package cz.tacr.elza.search;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.collections4.map.HashedMap;
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
import cz.tacr.elza.domain.ArrIndexWork;
import cz.tacr.elza.service.DescriptionItemService;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;

/**
 * @author <a href="mailto:stepan.marek@coreit.cz">Stepan Marek</a>
 */
@Component
public class SearchIndexService {

    private static final Logger logger = LoggerFactory.getLogger(SearchIndexService.class);

    private ExtendedSearchIntegrator integrator;

    @PersistenceContext
    private EntityManager em;

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
    public void processBatch(List<ArrIndexWork> workList) {

        workList.stream().collect(groupingBy(work -> work.getEntityClass())).forEach((entityClass, list) -> {

            String indexName = entityClass.getName();

            // predpokladame, ze index name odpovida nazvu entity
            IndexManager indexManager = integrator.getIndexManager(indexName);
            if (indexManager == null) {
                logger.error("Index manager not found for entity [{}]", entityClass);
                return;
            }

            Set<Integer> entityIdSet = list.stream().map(work -> work.getEntityId()).collect(toSet());

            Map<Integer, Object> entityMap = findAll(entityClass, entityIdSet);

            WorkPlan plan = new WorkPlan(integrator);

            entityIdSet.removeAll(entityMap.keySet());

            for (Integer id : entityIdSet) {
                plan.addWork(new Work(entityClass, id, WorkType.DELETE));
            }

            for (Map.Entry<Integer, Object> entry : entityMap.entrySet()) {
                plan.addWork(new Work(entry.getValue(), entry.getKey(), WorkType.INDEX));
            }

            indexManager.performOperations(plan.getPlannedLuceneWork(), null);
        });
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
