package cz.tacr.elza.controller;

import java.time.LocalDateTime;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import org.hibernate.Session;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import cz.tacr.elza.domain.FaChange;
import cz.tacr.elza.domain.FaLevel;
import cz.tacr.elza.repository.FaChangeRepository;
import cz.tacr.elza.repository.LevelRepository;

/**
 *
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 5. 8. 2015
 */
public class TreeTest extends AbstractRestTest {

    @Autowired
    LevelRepository levelRepository;
    @Autowired
    FaChangeRepository changeRepository;
    @PersistenceContext
    EntityManager entityManager;

    private Integer childId;

    @Before
    @Transactional
    public void prepareData() {
        FaChange change = new FaChange();
        change.setChangeDate(LocalDateTime.now());
        changeRepository.save(change);

        FaLevel parent = null;
        for (int i = 1; i <= 10; i++) {
            parent = createLevel(i, parent, change);
        }
        childId = parent.getFaLevelId();
        entityManager.unwrap(Session.class).flush();
        entityManager.unwrap(Session.class).clear();
    }

    private FaLevel createLevel(final Integer position, final FaLevel parent, final FaChange change) {
        FaLevel level = new FaLevel();
        level.setPosition(position);
        level.setParentNode(parent);
        level.setCreateChange(change);
        Integer maxNodeId = levelRepository.findMaxNodeId();
        if (maxNodeId == null) {
            maxNodeId = 0;
        }
        level.setNodeId(maxNodeId + 1);

        return levelRepository.save(level);
    }

    @Test
    @Transactional
    public void sqlTest() throws Exception {
        FaLevel level = levelRepository.findOne(childId);
        System.out.println(level.getParentNodeId());
        System.out.println(level.getParentNode());
        System.out.println(level.getCreateChange());
    }
}
