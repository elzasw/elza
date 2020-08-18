package cz.tacr.elza.search;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.lang.StringUtils;
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
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.impl.PojoIndexedTypeIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.SysIndexWork;
import cz.tacr.elza.service.DescriptionItemService;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;

@Component
public class SearchIndexService {

    private static final Logger logger = LoggerFactory.getLogger(SearchIndexService.class);

    // --- services ---

    private final DescriptionItemService descriptionItemService;

    // --- fields ---

    @PersistenceContext
    private EntityManager em;

    private ExtendedSearchIntegrator integrator = null;

    private Map<Class, SearchIndexSupport> repositoryMap = new HashedMap<>();

    private ExtendedSearchIntegrator getExtendedSearchIntegrator() {
        if (integrator == null) {
            FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);
            integrator = fullTextEntityManager.getSearchFactory().unwrap(ExtendedSearchIntegrator.class);
        }
        return integrator;
    }

    // --- constructor ---

    @Autowired
    public SearchIndexService(DescriptionItemService descriptionItemService) {
        this.descriptionItemService = descriptionItemService;
    }

    // --- methods ---

    @PostConstruct
    public void init() {
        // AbstractDocumentBuilder builder = getEntityBuilder(integrator, ArrDescItem.class);
        repositoryMap.put(ArrDescItem.class, descriptionItemService);
    }

    @Transactional
    public void processBatch(List<SysIndexWork> workList) {

        workList.stream().collect(groupingBy(work -> work.getEntityClass())).forEach((entityClass, list) -> {

            String indexName = entityClass.getName();

            // predpokladame, ze index name odpovida nazvu entity
            IndexManager indexManager = getExtendedSearchIntegrator().getIndexManager(indexName);
            if (indexManager == null) {
                logger.error("Index manager not found for entity [{}]", entityClass);
                return;
            }

            Set<Integer> entityIdSet = list.stream().map(work -> work.getEntityId()).collect(toSet());

            Map<Integer, Object> entityMap = findAll(entityClass, entityIdSet);

            WorkPlan plan = new WorkPlan(getExtendedSearchIntegrator());

            entityIdSet.removeAll(entityMap.keySet());

            for (Integer id : entityIdSet) {
                plan.addWork(new Work(new PojoIndexedTypeIdentifier(entityClass), id, WorkType.DELETE));
            }

            for (Map.Entry<Integer, Object> entry : entityMap.entrySet()) {
                plan.addWork(new Work(entry.getValue(), entry.getKey(), WorkType.INDEX));
            }

            List<LuceneWork> luceneWorkList = plan.getPlannedLuceneWork();

            if (logger.isDebugEnabled()) {
                logger.debug("Index manager perform operations:\n" + StringUtils.join(luceneWorkList, "\n"));
            }

            indexManager.performOperations(luceneWorkList, null);
        });
    }

    private <T> Map<Integer, T> findAll(Class<T> entityClass, Collection<Integer> ids) {
        SearchIndexSupport repository = repositoryMap.get(entityClass);
        return repository.findToIndex(ids);
    }

    private static AbstractDocumentBuilder getEntityBuilder(ExtendedSearchIntegrator extendedIntegrator, IndexedTypeIdentifier entityClass) {
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
