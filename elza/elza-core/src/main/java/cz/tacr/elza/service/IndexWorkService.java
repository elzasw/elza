package cz.tacr.elza.service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.search.backend.LuceneWork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import cz.tacr.elza.domain.SysIndexWork;
import cz.tacr.elza.repository.IndexWorkRepository;

@Service
@Transactional(readOnly = true)
public class IndexWorkService {

    private static final Logger logger = LoggerFactory.getLogger(IndexWorkService.class);

    // --- dao ---

    private final IndexWorkRepository indexWorkRepository;

    // --- constructor ---

    @Autowired
    public IndexWorkService(IndexWorkRepository indexWorkRepository) {
        this.indexWorkRepository = indexWorkRepository;
    }

    // --- methods ---

    private SysIndexWork _createIndexWork(Class<?> entityClass, String indexName, Integer entityId) {
        if (!entityClass.getName().equals(indexName)) {
            // predpokladame, ze index name odpovida nazvu entity,
            // jinak je potreba upravit logiku v cz.tacr.elza.search.SearchIndexService.processBatch()
            throw new IllegalStateException("Invalid index name [" + indexName + "] for class [" + entityClass.getName() + "]");
        }
        SysIndexWork work = new SysIndexWork();
        work.setIndexName(indexName);
        work.setEntityClass(entityClass);
        work.setEntityId(entityId);
        work.setInsertTime(LocalDateTime.now());
        return work;
    }

    @Transactional
    public SysIndexWork createIndexWork(Class<?> entityClass, Integer entityId) {
        SysIndexWork work = _createIndexWork(entityClass, entityClass.getName(), entityId);
        return indexWorkRepository.save(work);
    }

    @Transactional
    public List<SysIndexWork> createIndexWork(Class<?> entityClass, Collection<Integer> entityIdList) {
        List<SysIndexWork> workList = entityIdList.stream()
                .distinct()
                .map(entityId -> _createIndexWork(entityClass, entityClass.getName(), entityId))
                .collect(Collectors.toList());
        return indexWorkRepository.saveAll(workList);
    }

    private SysIndexWork _createIndexWork(String indexName, LuceneWork luceneWork) {
        return _createIndexWork(luceneWork.getEntityClass(), indexName, Integer.valueOf(luceneWork.getIdInString()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SysIndexWork createIndexWork(String indexName, LuceneWork luceneWork) {
        SysIndexWork work = _createIndexWork(indexName, luceneWork);
        return indexWorkRepository.save(work);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<SysIndexWork> createIndexWork(String indexName, List<LuceneWork> luceneWorkList) {
        List<SysIndexWork> workList = luceneWorkList.stream().map(luceneWork -> _createIndexWork(indexName, luceneWork)).collect(Collectors.toList());
        return indexWorkRepository.saveAll(workList);
    }

    public Page<SysIndexWork> findAllToIndex(Pageable pageable) {
        return indexWorkRepository.findAllToIndex(pageable);
    }

    @Transactional
    public void updateStartTime(Collection<Long> workIdList) {
        if (CollectionUtils.isEmpty(workIdList)) {
            return;
        }
        indexWorkRepository.updateStartTime(workIdList);
    }

    @Transactional
    public void clearStartTime() {
        indexWorkRepository.clearStartTime();
    }

    @Transactional
    public void delete(Collection<Long> workIdList) {
        if (CollectionUtils.isEmpty(workIdList)) {
            return;
        }
        indexWorkRepository.delete(workIdList);
    }
}
