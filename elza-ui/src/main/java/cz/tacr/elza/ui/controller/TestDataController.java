package cz.tacr.elza.ui.controller;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.controller.ArrangementManager;
import cz.tacr.elza.controller.RuleManager;
import cz.tacr.elza.domain.ArrArrangementType;
import cz.tacr.elza.domain.ArrDescItemExt;
import cz.tacr.elza.domain.ArrFaChange;
import cz.tacr.elza.domain.ArrFaLevel;
import cz.tacr.elza.domain.ArrFaVersion;
import cz.tacr.elza.domain.ArrFindingAid;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.vo.ArrDescItemSavePack;
import cz.tacr.elza.repository.AbstractPartyRepository;
import cz.tacr.elza.repository.ArrangementTypeRepository;
import cz.tacr.elza.repository.ChangeRepository;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.ExternalSourceRepository;
import cz.tacr.elza.repository.FindingAidRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.PartySubtypeRepository;
import cz.tacr.elza.repository.PartyTypeRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.repository.RegisterTypeRepository;
import cz.tacr.elza.repository.RuleSetRepository;
import cz.tacr.elza.repository.VariantRecordRepository;
import cz.tacr.elza.repository.VersionRepository;

/**
 * Kontroler pro testovací data.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 3. 9. 2015
 */
@RestController
@RequestMapping("/api/testData")
public class TestDataController {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private ArrangementManager arrangementManager;
    @Autowired
    private RuleManager ruleManager;

    @Autowired
    private AbstractPartyRepository abstractPartyRepository;
    @Autowired
    private DataRepository dataRepository;
    @Autowired
    protected DescItemRepository descItemRepository;
    @Autowired
    private ExternalSourceRepository externalSourceRepository;
    @Autowired
    private FindingAidRepository findingAidRepository;
    @Autowired
    protected ChangeRepository changeRepository;
    @Autowired
    protected LevelRepository levelRepository;
    @Autowired
    private NodeRepository nodeRepository;
    @Autowired
    private PartySubtypeRepository partySubtypeRepository;
    @Autowired
    private PartyTypeRepository partyTypeRepository;
    @Autowired
    private RegisterTypeRepository registerTypeRepository;
    @Autowired
    private RegRecordRepository recordRepository;
    @Autowired
    private VariantRecordRepository variantRecordRepository;
    @Autowired
    private VersionRepository versionRepository;

    @Autowired
    private ArrangementTypeRepository arrangementTypeRepository;
    @Autowired
    private RuleSetRepository ruleSetRepository;

    private static final int DEPTH = 1;
    private static final int NODES_IN_LEVEL = 15;
    private static final String FA_NAME = "Testovací Archivní pomůcka";
    private static final String[] attCodes = {"ZP2015_UNIT_ID",
        "ZP2015_OTHER_ID",
        "ZP2015_SERIAL_NUMBER",
        "ZP2015_TITLE",
        "ZP2015_UNIT_DATE",
        "ZP2015_UNIT_TYPE",
        "ZP2015_STORAGE_ID",
        "ZP2015_ORIGINATOR",
        "ZP2015_UNIT_HIST",
        "ZP2015_POSITION",
        "ZP2015_LEGEND"};

    static {
        Arrays.sort(attCodes);
    }

    /** Vytvoří testovací data. Databáze nemusí být prázdná. */
    @Transactional
    @RequestMapping(value = "/createData", method = RequestMethod.POST)
    public void createData() {
        ArrArrangementType arrArrangementType = arrangementTypeRepository.findAll().iterator().next();
        RulRuleSet ruleSet = ruleSetRepository.findAll().iterator().next();
        ArrFindingAid findingAid = arrangementManager.createFindingAid(FA_NAME, arrArrangementType.getArrangementTypeId(), ruleSet.getRuleSetId());
        ArrFaVersion version = arrangementManager.getOpenVersionByFindingAidId(findingAid.getFindingAidId());

        createTree(DEPTH, NODES_IN_LEVEL, version);
    }

    private void createTree(int depth, int nodesInLevel, ArrFaVersion version) {
        Queue<ArrNode> parents = new LinkedList<>();
        parents.add(version.getRootFaLevel().getNode());

        ArrFaChange change = new ArrFaChange();
        change.setChangeDate(LocalDateTime.now());
        changeRepository.save(change);
        Session session = entityManager.unwrap(Session.class);

        while (depth > 0) {
            depth--;
            Queue<ArrNode> newParents = new LinkedList<>();
            while (!parents.isEmpty()) {
                ArrNode parent = parents.poll();
                for (int position = 1; position <= nodesInLevel; position++) {
                    ArrFaLevel level = createLevel(change, parent, position);
                    ArrNode node = level.getNode();
                    createLevelAttributes(node, change, depth, version);
                    newParents.add(node);
                };
                session.flush();
                session.clear();
            }
            parents = newParents;
        }
    }

    private void createLevelAttributes(ArrNode node, ArrFaChange change, int depth, ArrFaVersion version) {
        ArrDescItemSavePack descItemSavePack = new ArrDescItemSavePack();
        descItemSavePack.setCreateNewVersion(true);
        descItemSavePack.setFaVersionId(version.getFaVersionId());
        descItemSavePack.setNode(node);

//        List<RulDescItemTypeExt> descriptionItemTypes = ruleManager.getDescriptionItemTypes(version.getRuleSet().getRuleSetId());
//        for (RulDescItemTypeExt rulDescItemTypeExt : descriptionItemTypes) {
//            if (Arrays.binarySearch(attCodes, rulDescItemTypeExt.getCode()) > 0) {
//                rulDescItemTypeExt.getDataType().g
//
//                ArrDescItemExt descItemExt = new ArrDescItemExt();
//                descItemExt.setAbstractParty(abstractParty);
//                descItemExt.setData(data);
//                descItemExt.setDescItemSpec(descItemSpec);
//                descItemExt.setDescItemType(rulDescItemTypeExt);
//                descItemExt.setRecord(record);
//            }
//        }

        List<ArrDescItemExt> descItems = new LinkedList<ArrDescItemExt>();


        descItemSavePack.setDescItems(descItems);

        arrangementManager.saveDescriptionItems(descItemSavePack);
    }

    private ArrFaLevel createLevel(final ArrFaChange createChange, final ArrNode parentNode, final Integer position) {
        Assert.notNull(createChange);

        ArrFaLevel level = new ArrFaLevel();
        level.setPosition(position);
        level.setCreateChange(createChange);
        level.setParentNode(parentNode);
        level.setNode(createNode());
        return levelRepository.save(level);
    }

    private ArrNode createNode() {
        ArrNode node = new ArrNode();
        node.setLastUpdate(LocalDateTime.now());
        return nodeRepository.save(node);
    }

    /** Odstraní data z databáze, kromě tabulek s prefixem rul_. */
    @Transactional
    @RequestMapping(value = "/removeData", method = RequestMethod.DELETE)
    public void removeData() {
        versionRepository.deleteAllInBatch();
        findingAidRepository.deleteAllInBatch();
        abstractPartyRepository.deleteAllInBatch();
        dataRepository.deleteAllInBatch();
        descItemRepository.deleteAllInBatch();
        externalSourceRepository.deleteAllInBatch();
        partySubtypeRepository.deleteAllInBatch();
        partyTypeRepository.deleteAllInBatch();
        registerTypeRepository.deleteAllInBatch();
        variantRecordRepository.deleteAllInBatch();
        recordRepository.deleteAllInBatch();
        levelRepository.deleteAllInBatch();
        nodeRepository.deleteAllInBatch();
        changeRepository.deleteAllInBatch();
    }
}
