package cz.tacr.elza.service;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.search.backend.LuceneWork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import cz.tacr.elza.domain.ArrSearchWork;
import cz.tacr.elza.repository.SearchWorkRepository;

@Service
public class SearchWorkService {

    private static final Logger logger = LoggerFactory.getLogger(SearchWorkService.class);

    @Autowired
    private SearchWorkRepository searchWorkRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ArrSearchWork createWork(String indexName, LuceneWork luceneWork) {
        ArrSearchWork work = new ArrSearchWork();
        work.setIndexName(indexName);
        work.setEntityClass(luceneWork.getEntityClass().getName());
        work.setEntityId(Integer.valueOf(luceneWork.getIdInString()));
        work.setWorkType(ArrSearchWork.WorkType.INDEX);
        work.setLastUpdate(LocalDateTime.now());
        return searchWorkRepository.save(work);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createWork(String indexName, List<LuceneWork> workList) {
        for (LuceneWork work : workList) {
            createWork(indexName, work);
        }
    }

}
