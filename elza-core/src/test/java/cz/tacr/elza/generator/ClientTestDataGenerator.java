package cz.tacr.elza.generator;


import org.apache.commons.lang3.RandomUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import cz.tacr.elza.ElzaCore;
import cz.tacr.elza.controller.ArrangementManager;
import cz.tacr.elza.domain.ArrangementType;
import cz.tacr.elza.domain.FaChange;
import cz.tacr.elza.domain.FaLevel;
import cz.tacr.elza.domain.FaVersion;
import cz.tacr.elza.domain.FindingAid;
import cz.tacr.elza.domain.RuleSet;
import cz.tacr.elza.repository.ArrangementTypeRepository;
import cz.tacr.elza.repository.ChangeRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.RuleSetRepository;
import cz.tacr.elza.repository.VersionRepository;

/**
 * Generuje data pro testování z klienta.
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
    private static final int MAX_NODES_IN_LEVEL = 10000;

    protected static final String TEST_CODE = "Tcode";
    protected static final String TEST_NAME = "Test name";

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

    private int nodesCreated = 0;
    private int maxNodeId = 0;
    private int nodeIdGenerator = 0;

    @Test
    @Transactional(noRollbackFor = IllegalStateException.class)
    public void generateTree() {
        maxNodeId = levelRepository.findMaxNodeId();
        nodeIdGenerator = maxNodeId;

        for (int i = 0; i < FA_COUNT; i++) {
            nodesCreated = maxNodeId;
            FindingAid findingAid = createFindingAid(TEST_NAME + i);
            FaVersion version = versionRepository.findByFindingAidAndLockChangeIsNull(findingAid);
            nodeIdGenerator++;
            try {
                createTree(findingAid, version);
            } catch (IllegalStateException ex) {}
        }
    }

    private void createTree(final FindingAid findingAid, final FaVersion version) {
        int nodesInLevel = RandomUtils.nextInt(1, 20);
        for (int i = 0; i < nodesInLevel; i++) {
            checkIfCreateMoreNodes();
            FaLevel level = createLevel(findingAid, version.getRootNode(), i, version.getCreateChange());
            createSubtree(findingAid, version.getCreateChange(), level, 0);
        }
    }


    private void checkIfCreateMoreNodes() {
        if (nodesCreated - maxNodeId >= NODE_COUNT) {
            throw new IllegalStateException();
        }
    }

    private void createSubtree(final FindingAid findingAid, final FaChange createChange, final FaLevel parent, int depth) {
        if (RandomUtils.nextInt(1, TREE_DEPTH) < depth) {
            return;
        }

        int nodesInLevel =  RandomUtils.nextInt(1, MAX_NODES_IN_LEVEL);
        for (int i = 0; i < nodesInLevel; i++) {
            checkIfCreateMoreNodes();
            FaLevel level = createLevel(findingAid, parent, i, createChange);
            createSubtree(findingAid, createChange, level, depth + 1);
        }
    }

    private FaLevel createLevel(final FindingAid findingAid, final FaLevel parent, final int position, final FaChange change) {
        FaLevel level = new FaLevel();
        level.setCreateChange(change);
        level.setNodeId(++nodeIdGenerator);
        level.setParentNode(parent);
        level.setPosition(position);
        nodesCreated++;

        return levelRepository.save(level);
    }

    protected ArrangementType createArrangementType() {
        ArrangementType arrangementType = new ArrangementType();
        arrangementType.setName(TEST_NAME);
        arrangementType.setCode(TEST_CODE);
        arrangementTypeRepository.save(arrangementType);
        return arrangementType;
    }

    protected RuleSet createRuleSet() {
        RuleSet ruleSet = new RuleSet();
        ruleSet.setName(TEST_NAME);
        ruleSet.setCode(TEST_CODE);
        ruleSetRepository.save(ruleSet);
        return ruleSet;
    }

    protected FindingAid createFindingAid(final String name) {
        RuleSet ruleSet = createRuleSet();
        ArrangementType arrangementType = createArrangementType();

        return arrangementManager.createFindingAid(name, arrangementType.getId(), ruleSet.getId());
    }
}
