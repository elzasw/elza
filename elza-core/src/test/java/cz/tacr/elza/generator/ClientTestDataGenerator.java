package cz.tacr.elza.generator;


import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.Session;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import cz.tacr.elza.ElzaCore;
import cz.tacr.elza.configuration.hibernate.impl.TableIdGenerator;
import cz.tacr.elza.controller.ArrangementManager;
import cz.tacr.elza.domain.ArrArrangementType;
import cz.tacr.elza.domain.ArrFaChange;
import cz.tacr.elza.domain.ArrFaLevel;
import cz.tacr.elza.domain.ArrFaVersion;
import cz.tacr.elza.domain.ArrFindingAid;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.repository.ArrangementTypeRepository;
import cz.tacr.elza.repository.ChangeRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.RuleSetRepository;
import cz.tacr.elza.repository.VersionRepository;

/**
 * Generuje data pro testování z klienta.
 * Pro ychlejší generování je potřeba nastavit přidělování identifikátorů z db po větších počtech
 * @see {@link TableIdGenerator}.
 *
 *  počet id / počet vložených záznamů po kterých se provede session flush a clean / čas v minutách
 *  1000    500 40
 *  1000    1   38
 *  100     1   38
 *  100     75  35
 *  100     50  36
 *  1000000 75  36
 *  2000000 75  34
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 7. 8. 2015
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = ElzaCore.class)
@TransactionConfiguration(transactionManager = "transactionManager", defaultRollback = false)
public class ClientTestDataGenerator {

    private static final int FA_COUNT = 20;
    private static final int TREE_DEPTH = 20;
    private static final int NODE_COUNT = 100000;
    private static final int MAX_LEVEL_NODES_COUNT = 10000;
    private static final int NORMAL_LEVEL_NODES_COUNT = 100;
    private static final int FLUSH_COUNT = 75;

    protected static final String TEST_CODE = "Tcode";
    protected static final String TEST_NAME = "Test name";

    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private ArrangementManager arrangementManager;
    @Autowired
    private VersionRepository versionRepository;
    @Autowired
    private LevelRepository levelRepository;
    @Autowired
    private ChangeRepository changeRepository;
    @Autowired
    private ArrangementTypeRepository arrangementTypeRepository;
    @Autowired
    private RuleSetRepository ruleSetRepository;
    @Autowired
    private NodeRepository nodeRepository;

    private int nodesCreated = 0;

    @Test
    @Transactional(noRollbackFor = IllegalStateException.class)
    public void generateTree() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        RulRuleSet ruleSet = createRuleSet();
        ArrArrangementType arrangementType = createArrangementType();

        IntStream.range(0, FA_COUNT).forEach( i -> {
            nodesCreated = 0;
            ArrFindingAid findingAid = createFindingAid(TEST_NAME + i, ruleSet, arrangementType);
            ArrFaVersion version = versionRepository.findByFindingAidIdAndLockChangeIsNull(findingAid.getFindingAidId());
            try {
                createTree(version.getRootFaLevel(), version.getCreateChange());
            } catch (IllegalStateException ex) {}
        });

        stopWatch.stop();
        System.out.println(stopWatch.getTime());
    }

    private void createTree(final ArrFaLevel rootNode, final ArrFaChange createChange) {
        List<ArrFaLevel> firstLevelNodes = IntStream.range(0, RandomUtils.nextInt(5, 20)).mapToObj( i -> {
            checkIfCreateMoreNodes();
            return createLevel(rootNode, i, createChange);
        }).collect(Collectors.toList());

        ArrFaLevel firstNode = firstLevelNodes.get(0);
        ArrFaLevel secondNode = firstLevelNodes.get(1);

        createMaxDepthLevel(firstNode, TREE_DEPTH - 1, createChange);
        createMaxNodesLevel(secondNode, createChange);
        IntStream.range(2, firstLevelNodes.size()).forEach( i -> {
            checkIfCreateMoreNodes();
            createSubtree(createChange, firstLevelNodes.get(i), 0);
        });
    }

    private void createMaxNodesLevel(ArrFaLevel firstNode, ArrFaChange createChange) {
        IntStream.range(0, MAX_LEVEL_NODES_COUNT).forEach( i -> {
            checkIfCreateMoreNodes();
            createLevel(firstNode, i, createChange);
        });
    }

    protected ArrNode createNode() {
        ArrNode node = new ArrNode();
        node.setLastUpdate(LocalDateTime.now());
        return nodeRepository.save(node);
    }

    private void createMaxDepthLevel(ArrFaLevel secondNode, int depth, ArrFaChange createChange) {
        if (depth == 0) {
            return;
        }

        checkIfCreateMoreNodes();
        ArrFaLevel node = createLevel(secondNode, 0, createChange);
        createMaxDepthLevel(node, depth - 1, createChange);
    }

    private void createSubtree(final ArrFaChange createChange, final ArrFaLevel parent, int depth) {
        if (RandomUtils.nextInt(1, TREE_DEPTH + 1) < depth) {
            return;
        }

        int nodesInLevel =  RandomUtils.nextInt(1, NORMAL_LEVEL_NODES_COUNT);
        IntStream.range(0, nodesInLevel).mapToObj( i -> {
            checkIfCreateMoreNodes();
            return createLevel(parent, i, createChange);
        }).collect(Collectors.toList()).forEach( node -> {
            checkIfCreateMoreNodes();
            createSubtree(createChange, node, depth + 1);
        });
    }

    private void checkIfCreateMoreNodes() {
        if (nodesCreated % FLUSH_COUNT == 0) {
            entityManager.unwrap(Session.class).flush();
            entityManager.unwrap(Session.class).clear();
        }

        if (nodesCreated >= NODE_COUNT) {
            entityManager.unwrap(Session.class).flush();
            entityManager.unwrap(Session.class).clear();
            throw new IllegalStateException();
        }
    }

    private ArrFaLevel createLevel(final ArrFaLevel parent, final int position, final ArrFaChange change) {
        ArrFaLevel level = new ArrFaLevel();
        level.setCreateChange(change);
        level.setNode(createNode());
        level.setParentNode(parent.getNode());
        level.setPosition(position + 1);
        nodesCreated++;

        return levelRepository.save(level);
    }

    protected ArrArrangementType createArrangementType() {
        ArrArrangementType arrangementType = new ArrArrangementType();
        arrangementType.setName(TEST_NAME);
        arrangementType.setCode(TEST_CODE);
        arrangementTypeRepository.save(arrangementType);
        return arrangementType;
    }

    protected RulRuleSet createRuleSet() {
        RulRuleSet ruleSet = new RulRuleSet();
        ruleSet.setName(TEST_NAME);
        ruleSet.setCode(TEST_CODE);
        ruleSetRepository.save(ruleSet);
        return ruleSet;
    }

    protected ArrFindingAid createFindingAid(final String name, RulRuleSet ruleSet, ArrArrangementType arrangementType) {
        return arrangementManager.createFindingAid(name, arrangementType.getArrangementTypeId(), ruleSet.getRuleSetId());
    }
}
