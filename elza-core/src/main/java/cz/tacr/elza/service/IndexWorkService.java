package cz.tacr.elza.service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

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

import cz.tacr.elza.domain.ArrIndexWork;
import cz.tacr.elza.repository.IndexWorkRepository;

@Service
@Transactional(readOnly = true)
public class IndexWorkService {

    private static final Logger logger = LoggerFactory.getLogger(IndexWorkService.class);

    // --- dao ---

    @Autowired
    private IndexWorkRepository indexWorkRepository;

    // --- methods ---

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ArrIndexWork createIndexWork(String indexName, LuceneWork luceneWork) {
        Class<?> entityClass = luceneWork.getEntityClass();
        if (!entityClass.getName().equals(indexName)) {
            // predpokladame, ze index name odpovida nazvu entity,
            // jinak je potreba upravit logiku v cz.tacr.elza.search.SearchIndexService.processBatch()
            throw new IllegalStateException("Invalid index name [" + indexName + "] for class [" + entityClass.getName() + "]");
        }
        ArrIndexWork work = new ArrIndexWork();
        work.setIndexName(indexName);
        work.setEntityClass(entityClass);
        work.setEntityId(Integer.valueOf(luceneWork.getIdInString()));
        work.setInsertTime(LocalDateTime.now());
        return indexWorkRepository.save(work);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createIndexWork(String indexName, List<LuceneWork> workList) {
        for (LuceneWork work : workList) {
            createIndexWork(indexName, work);
        }
    }

    public Page<ArrIndexWork> findAllToIndex(Pageable pageable) {
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
