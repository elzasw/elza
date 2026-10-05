package cz.tacr.elza.bulkaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.AbstractServiceTest;
import cz.tacr.elza.bulkaction.generator.result.ContentMigrationResult;
import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemStringVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemTextVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemVO;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.service.FundLevelService;
import cz.tacr.elza.service.FundLevelService.AddLevelDirection;
import cz.tacr.elza.service.LevelTreeCacheService;

/**
 * Test of the {@link cz.tacr.elza.bulkaction.generator.ContentMigration} bulk
 * action over the {@code SIMPLE-DEV} package action {@code SRD_MIGRATE_CONTENT}:
 *
 * <ul>
 * <li>SRD_TITLE (TEXT) -&gt; SRD_NAME (STRING) for short single-line values,</li>
 * <li>SRD_UNIT_CONTENT (TEXT) -&gt; SRD_TITLE (TEXT) only when SRD_TITLE is empty.</li>
 * </ul>
 *
 * The whole test runs in a single transaction (as in other service tests) and
 * the action is executed synchronously, so the resulting description items can be
 * asserted directly.
 */
public class ContentMigrationTest extends AbstractServiceTest {

    private static final String ACTION_CODE = "SRD_MIGRATE_CONTENT";

    @Autowired
    private FundLevelService fundLevelService;

    @Autowired
    private BulkActionConfigManager bulkActionConfigManager;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private LevelTreeCacheService levelTreeCacheService;

    @Test
    public void migrateContent() {
        TransactionTemplate tt = new TransactionTemplate(txManager);
        tt.executeWithoutResult(r -> migrateContentInTransaction());
    }

    private void migrateContentInTransaction() {
        authorizeAsAdmin();

        RulItemType titleType = itemTypeRepository.findOneByCode("SRD_TITLE");
        RulItemType nameType = itemTypeRepository.findOneByCode("SRD_NAME");
        RulItemType unitContentType = itemTypeRepository.findOneByCode("SRD_UNIT_CONTENT");
        Assertions.assertNotNull(titleType);
        Assertions.assertNotNull(nameType);
        Assertions.assertNotNull(unitContentType);

        FundInfo fi = createFund("F-content-migration");
        Integer fvId = fi.getFundVersionId();
        Integer rootId = fi.getRootNodeId();

        String longValue = "x".repeat(300);

        // all test nodes are direct children of the root (parent always known to the tree cache)
        Integer nodeA = addChild(fi.getFundVersion(), rootId);   // short single-line title -> moved to NAME
        Integer nodeB = addChild(fi.getFundVersion(), rootId);   // long title -> stays
        Integer nodeC = addChild(fi.getFundVersion(), rootId);   // short multi-line title -> stays
        Integer nodeD = addChild(fi.getFundVersion(), rootId);   // short title + unit content -> fallback fills emptied title
        Integer nodeE = addChild(fi.getFundVersion(), rootId);   // long title + unit content -> fallback skipped
        Integer nodeF = addChild(fi.getFundVersion(), rootId);   // short title + existing NAME -> split skipped

        // refresh the tree cache so the newly added nodes are visible for item creation
        levelTreeCacheService.invalidateFundVersion(fi.getFundVersion());

        addText(nodeA, titleType, "Krátký název A", fvId);
        addText(nodeB, titleType, longValue, fvId);
        addText(nodeC, titleType, "Řádek 1\nŘádek 2", fvId);
        addText(nodeD, titleType, "Krátký název D", fvId);
        addText(nodeD, unitContentType, "Tematický popis D", fvId);
        addText(nodeE, titleType, longValue, fvId);
        addText(nodeE, unitContentType, "Tematický popis E", fvId);
        addText(nodeF, titleType, "Krátký název F", fvId);
        addString(nodeF, nameType, "Existující název F", fvId);

        levelTreeCacheService.invalidateFundVersion(fi.getFundVersion());

        // run the migration synchronously
        ContentMigrationResult result = runMigration(fvId, rootId);

        // A -> moved (title to NAME); D -> two moves (title to NAME, unit content to title)
        Assertions.assertEquals(3, result.getMovedItems(), "Unexpected number of moved items");

        // A: short single-line -> NAME, TITLE gone
        assertSingle(nodeA, nameType, "Krátký název A");
        assertNone(nodeA, titleType);

        // B: long -> stays in TITLE, no NAME
        assertNone(nodeB, nameType);
        assertSingle(nodeB, titleType, longValue);

        // C: short but multi-line -> stays in TITLE (STRING cannot hold line breaks), no NAME
        assertNone(nodeC, nameType);
        assertSingle(nodeC, titleType, "Řádek 1\nŘádek 2");

        // D: title -> NAME, then unit content -> emptied TITLE; unit content removed
        assertSingle(nodeD, nameType, "Krátký název D");
        assertSingle(nodeD, titleType, "Tematický popis D");
        assertNone(nodeD, unitContentType);

        // E: long title stays -> fallback skipped (TITLE occupied), unit content kept, no NAME
        assertNone(nodeE, nameType);
        assertSingle(nodeE, titleType, longValue);
        assertSingle(nodeE, unitContentType, "Tematický popis E");

        // F: NAME already present -> split skipped, both kept
        assertSingle(nodeF, nameType, "Existující název F");
        assertSingle(nodeF, titleType, "Krátký název F");
    }

    @Test
    public void migrateBlankValues() {
        TransactionTemplate tt = new TransactionTemplate(txManager);
        tt.executeWithoutResult(r -> migrateBlankValuesInTransaction());
    }

    /**
     * Blank values could be stored before 2022 (now rejected by the validator).
     * They must neither break the action nor block the target.
     */
    private void migrateBlankValuesInTransaction() {
        authorizeAsAdmin();

        RulItemType titleType = itemTypeRepository.findOneByCode("SRD_TITLE");
        RulItemType nameType = itemTypeRepository.findOneByCode("SRD_NAME");
        RulItemType unitContentType = itemTypeRepository.findOneByCode("SRD_UNIT_CONTENT");

        FundInfo fi = createFund("F-content-migration-blank");
        Integer fvId = fi.getFundVersionId();
        Integer rootId = fi.getRootNodeId();

        Integer nodeG = addChild(fi.getFundVersion(), rootId);   // empty title -> stays, nothing moved
        Integer nodeH = addChild(fi.getFundVersion(), rootId);   // blank title + unit content -> blank title replaced
        Integer nodeI = addChild(fi.getFundVersion(), rootId);   // title + empty NAME -> empty NAME replaced
        Integer nodeJ = addChild(fi.getFundVersion(), rootId);   // title with trailing line break -> trimmed to NAME

        levelTreeCacheService.invalidateFundVersion(fi.getFundVersion());

        addLegacyText(nodeG, titleType, "", fvId);
        addLegacyText(nodeH, titleType, "   ", fvId);
        addText(nodeH, unitContentType, "Tematický popis H", fvId);
        addText(nodeI, titleType, "Krátký název I", fvId);
        addLegacyString(nodeI, nameType, "", fvId);
        addLegacyText(nodeJ, titleType, "Krátký název J\n", fvId);

        // drop entities with placeholder values, the action has to read the legacy ones
        em.clear();
        levelTreeCacheService.invalidateFundVersion(fi.getFundVersion());

        ContentMigrationResult result = runMigration(fvId, rootId);

        // H -> unit content to title; I -> title to NAME; J -> title to NAME
        Assertions.assertEquals(3, result.getMovedItems(), "Unexpected number of moved items");

        // G: empty title is not moved
        assertNone(nodeG, nameType);
        assertSingle(nodeG, titleType, "");

        // H: blank title is not moved to NAME, it is replaced by unit content
        assertNone(nodeH, nameType);
        assertSingle(nodeH, titleType, "Tematický popis H");
        assertNone(nodeH, unitContentType);

        // I: empty NAME does not block the move
        assertSingle(nodeI, nameType, "Krátký název I");
        assertNone(nodeI, titleType);

        // J: trailing line break is trimmed, value is single-line
        assertSingle(nodeJ, nameType, "Krátký název J");
        assertNone(nodeJ, titleType);
    }

    /**
     * Creates a TEXT item and overwrites its value in the database, bypassing the validator.
     */
    private void addLegacyText(Integer nodeId, RulItemType itemType, String rawValue, Integer fundVersionId) {
        ArrItemTextVO vo = new ArrItemTextVO();
        vo.setValue("placeholder");
        ArrDescItem item = addItem(nodeId, itemType, vo, fundVersionId);
        overwriteValue("UPDATE arr_data_text SET text_value = :value WHERE data_id = :dataId", item, rawValue);
    }

    /**
     * Creates a STRING item and overwrites its value in the database, bypassing the validator.
     */
    private void addLegacyString(Integer nodeId, RulItemType itemType, String rawValue, Integer fundVersionId) {
        ArrItemStringVO vo = new ArrItemStringVO();
        vo.setValue("placeholder");
        ArrDescItem item = addItem(nodeId, itemType, vo, fundVersionId);
        overwriteValue("UPDATE arr_data_string SET string_value = :value WHERE data_id = :dataId", item, rawValue);
    }

    private void overwriteValue(String sql, ArrDescItem item, String rawValue) {
        em.flush();
        int updated = em.createNativeQuery(sql)
                .setParameter("value", rawValue)
                .setParameter("dataId", item.getData().getDataId())
                .executeUpdate();
        Assertions.assertEquals(1, updated);
    }

    private Integer addChild(ArrFundVersion fundVersion, Integer parentNodeId) {
        ArrNode parent = nodeRepository.findById(parentNodeId).orElseThrow();
        List<ArrLevel> levels = fundLevelService.addNewLevel(fundVersion, parent, parent, AddLevelDirection.CHILD,
                "Série", null, null, null, null);
        // the newly created level has the highest level id
        return levels.stream().max(Comparator.comparing(ArrLevel::getLevelId)).orElseThrow().getNode().getNodeId();
    }

    private void addText(Integer nodeId, RulItemType itemType, String value, Integer fundVersionId) {
        ArrItemTextVO vo = new ArrItemTextVO();
        vo.setValue(value);
        addItem(nodeId, itemType, vo, fundVersionId);
    }

    private void addString(Integer nodeId, RulItemType itemType, String value, Integer fundVersionId) {
        ArrItemStringVO vo = new ArrItemStringVO();
        vo.setValue(value);
        addItem(nodeId, itemType, vo, fundVersionId);
    }

    private ArrDescItem addItem(Integer nodeId, RulItemType itemType, ArrItemVO vo, Integer fundVersionId) {
        vo.setItemTypeId(itemType.getItemTypeId());
        // reload node to get the current optimistic-lock version (multiple items per node)
        ArrNode node = nodeRepository.findById(nodeId).orElseThrow();
        return createDescItem(vo, node, fundVersionId);
    }

    private ContentMigrationResult runMigration(Integer fundVersionId, Integer rootNodeId) {
        ArrBulkActionRun run = new ArrBulkActionRun();
        run.setBulkActionCode(ACTION_CODE);
        run.setChange(arrangementInternalService.createChange(ArrChange.Type.BULK_ACTION));
        ArrFundVersion versionStub = new ArrFundVersion();
        versionStub.setFundVersionId(fundVersionId);
        run.setFundVersion(versionStub);

        BulkAction action = bulkActionConfigManager.getBulkAction(ACTION_CODE);
        Assertions.assertNotNull(action, "Bulk action " + ACTION_CODE + " is not registered");
        try {
            action.execute(new ActionRunContext(Collections.singletonList(rootNodeId), run));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

        List<?> results = run.getResult().getResults();
        Assertions.assertEquals(1, results.size());
        return (ContentMigrationResult) results.get(0);
    }

    private void assertSingle(Integer nodeId, RulItemType itemType, String expected) {
        List<String> values = loadValues(nodeId, itemType);
        Assertions.assertEquals(1, values.size(), itemType.getCode() + " count on node " + nodeId);
        Assertions.assertEquals(expected, values.get(0), itemType.getCode() + " value on node " + nodeId);
    }

    private void assertNone(Integer nodeId, RulItemType itemType) {
        Assertions.assertTrue(loadValues(nodeId, itemType).isEmpty(),
                itemType.getCode() + " should be empty on node " + nodeId);
    }

    private List<String> loadValues(Integer nodeId, RulItemType itemType) {
        ArrNode node = nodeRepository.findById(nodeId).orElseThrow();
        List<ArrDescItem> items = descriptionItemService
                .findByNodeAndDeleteChangeIsNullAndItemTypeId(node, itemType.getItemTypeId());
        List<String> values = new ArrayList<>();
        for (ArrDescItem item : items) {
            values.add(readValue(item));
        }
        return values;
    }

    private static String readValue(ArrDescItem item) {
        ArrData data = HibernateUtils.unproxy(item.getData());
        if (data instanceof ArrDataText text) {
            return text.getTextValue();
        }
        if (data instanceof ArrDataString string) {
            return string.getStringValue();
        }
        return null;
    }
}
