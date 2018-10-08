package cz.tacr.elza.service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.search.backend.LuceneWork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import cz.tacr.elza.domain.ArrSearchWork;
import cz.tacr.elza.repository.SearchWorkRepository;

@Service
@Transactional(readOnly = true)
public class SearchWorkService {

    private static final Logger logger = LoggerFactory.getLogger(SearchWorkService.class);

    // --- dao ---

    @Autowired
    private SearchWorkRepository searchWorkRepository;

    // --- methods ---

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ArrSearchWork createSearchWork(String indexName, LuceneWork luceneWork) {
        ArrSearchWork work = new ArrSearchWork();
        work.setIndexName(indexName);
        work.setEntityClass(luceneWork.getEntityClass());
        work.setEntityId(Integer.valueOf(luceneWork.getIdInString()));
        work.setWorkType(ArrSearchWork.WorkType.INDEX);
        work.setInsertTime(LocalDateTime.now());
        return searchWorkRepository.save(work);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createSearchWork(String indexName, List<LuceneWork> workList) {
        for (LuceneWork work : workList) {
            createSearchWork(indexName, work);
        }
    }

    public Map<Class, List<ArrSearchWork>> groupByEntityClass() {
        return searchWorkRepository.findAllToIndex().stream().collect(Collectors.groupingBy(w -> w.getEntityClass()));
    }

    @Transactional
    public void updateStartTime(Collection<Integer> workIdList) {
        if (CollectionUtils.isEmpty(workIdList)) {
            return;
        }
        searchWorkRepository.updateStartTime(workIdList);
    }

    @Transactional
    public void clearStartTime() {
        searchWorkRepository.clearStartTime();
    }

    @Transactional
    public void delete(Collection<Integer> workIdList) {
        if (CollectionUtils.isEmpty(workIdList)) {
            return;
        }
        searchWorkRepository.delete(workIdList);
    }
}
