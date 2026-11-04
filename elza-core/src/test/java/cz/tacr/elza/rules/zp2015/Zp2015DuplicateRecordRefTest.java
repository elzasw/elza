package cz.tacr.elza.rules.zp2015;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.internal.io.ResourceFactory;
import org.springframework.test.util.ReflectionTestUtils;

import com.sun.management.ThreadMXBean;

import cz.tacr.elza.domain.ApState.StateApproval;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.vo.DataValidationResult;
import cz.tacr.elza.domain.vo.DataValidationResults;
import cz.tacr.elza.drools.model.ActiveLevel;
import cz.tacr.elza.drools.model.Ap;
import cz.tacr.elza.drools.model.DescItem;
import cz.tacr.elza.drools.model.Level;
import cz.tacr.elza.drools.service.ModelFactory;

/**
 * ZP2015 rules "Kontrola duplicitních hodnot prvků datového typu RECORD_REF" (Validation.drl) on
 * levels built in memory: duplicates are reported once, and a level with thousands of references
 * to distinct entities is validated without holding the pairs of its items (bug 10207).
 */
class Zp2015DuplicateRecordRefTest {

    private static final String VALIDATION_DRL = "/rules-cz-zp2015/rul_rule_set/ZP2015/rules/Validation.drl";

    private static final String DUPLICATE_MSG = "duplicitní hodnotu";

    /**
     * Limit of allocation for 3 000 references of one type. Comparing the entity inside the pattern
     * allocates about 2.4 GB of short-lived objects (each 'from' candidate gets a fact handle);
     * keeping every pair of items until the end of the session allocates about 12 GB.
     */
    private static final long MAX_ALLOCATED_BYTES = 6L * 1024 * 1024 * 1024;

    private static KieBase kieBase;

    private final RulItemType apRefType = itemType("ZP2015_AP_REF", "Odkaz na přístupový bod");
    private final RulItemType entityRoleType = itemType("ZP2015_ENTITY_ROLE", "Role entit");
    private final RulItemSpec roleAuthor = itemSpec("ZP2015_ENTITY_ROLE_1", "autor");
    private final RulItemSpec roleOther = itemSpec("ZP2015_ENTITY_ROLE_2", "jiná role");

    private int nextItemId = 1;

    @BeforeAll
    static void compileRules() throws Exception {
        KieServices ks = KieServices.Factory.get();
        KieFileSystem kfs = ks.newKieFileSystem();
        try (InputStream is = Zp2015DuplicateRecordRefTest.class.getResourceAsStream(VALIDATION_DRL)) {
            assertThat(is).as(VALIDATION_DRL).isNotNull();
            kfs.write(ResourceFactory.newInputStreamResource(is, "UTF-8")
                    .setResourceType(ResourceType.DRL)
                    .setTargetPath(UUID.randomUUID().toString()));
            KieBuilder kBuilder = ks.newKieBuilder(kfs);
            kBuilder.buildAll();
            assertThat(kBuilder.getResults().getMessages(Message.Level.ERROR)).isEmpty();
        }
        kieBase = ks.newKieContainer(ks.getRepository().getDefaultReleaseId()).newKieBase(ks.newKieBaseConfiguration());
    }

    private static RulItemType itemType(String code, String name) {
        RulItemType itemType = new RulItemType();
        itemType.setCode(code);
        itemType.setName(name);
        return itemType;
    }

    private static RulItemSpec itemSpec(String code, String name) {
        RulItemSpec itemSpec = new RulItemSpec();
        itemSpec.setCode(code);
        itemSpec.setName(name);
        return itemSpec;
    }

    private DescItem ref(RulItemType itemType, RulItemSpec itemSpec, int apId) {
        DescItem item = new DescItem(itemType, itemSpec);
        ReflectionTestUtils.setField(item, "descItemId", nextItemId++);
        ReflectionTestUtils.setField(item, "dataType", "RECORD_REF");
        item.setAp(new Ap(apId, "PERSON_INDIVIDUAL", List.of(), StateApproval.APPROVED));
        return item;
    }

    /** Copy of an item of an ancestor as the active level sees it. */
    private static DescItem inherited(DescItem parentItem, int parentNodeId) {
        DescItem item = new DescItem(parentItem);
        item.setNodeId(parentNodeId);
        return item;
    }

    private static Level parentLevel(DescItem... descItems) {
        Level parent = new Level();
        parent.setNodeId(1);
        parent.setDescItems(List.of(descItems));
        return parent;
    }

    private static ActiveLevel activeLevel(List<DescItem> descItems, Level parent) {
        Level level = new Level();
        level.setNodeId(2);
        level.setDescItems(descItems);
        level.setParent(parent);
        return new ActiveLevel(level);
    }

    private static List<DataValidationResult> duplicates(ActiveLevel activeLevel) {
        List<Object> facts = new ArrayList<>();
        ModelFactory.addLevelWithParents(activeLevel, facts);
        DataValidationResults results = new DataValidationResults();
        StatelessKieSession session = kieBase.newStatelessKieSession();
        session.setGlobal("dvResults", results);
        session.execute(facts);
        return results.getResults().stream()
                .filter(r -> r.getMessage() != null && r.getMessage().contains(DUPLICATE_MSG))
                .toList();
    }

    @Test
    void duplicateOnLevelReportedOnceOnLaterItem() {
        DescItem first = ref(apRefType, null, 100);
        DescItem second = ref(apRefType, null, 100);
        DescItem other = ref(apRefType, null, 101);

        List<DataValidationResult> results = duplicates(activeLevel(List.of(first, second, other), null));

        assertThat(results).extracting(DataValidationResult::getDescItemId).containsExactly(second.getDescItemId());
    }

    @Test
    void duplicateOfAncestorReportedOnOwnItem() {
        // the level has its own item of the type, so it gets no inherited copy (ScriptModelFactory)
        DescItem parentItem = ref(apRefType, null, 100);
        DescItem own = ref(apRefType, null, 100);

        List<DataValidationResult> results = duplicates(activeLevel(List.of(own), parentLevel(parentItem)));

        assertThat(results).extracting(DataValidationResult::getDescItemId).containsExactly(own.getDescItemId());
    }

    @Test
    void inheritedItemAloneIsNotDuplicate() {
        DescItem parentItem = ref(apRefType, null, 100);

        List<DataValidationResult> results = duplicates(activeLevel(List.of(inherited(parentItem, 1)),
                                                                    parentLevel(parentItem)));

        assertThat(results).isEmpty();
    }

    @Test
    void sameEntityWithSameSpecIsDuplicateWithOtherSpecIsNot() {
        DescItem author = ref(entityRoleType, roleAuthor, 100);
        DescItem authorAgain = ref(entityRoleType, roleAuthor, 100);
        DescItem otherRole = ref(entityRoleType, roleOther, 100);

        List<DataValidationResult> results = duplicates(activeLevel(List.of(author, authorAgain, otherRole), null));

        assertThat(results).extracting(DataValidationResult::getDescItemId).containsExactly(authorAgain.getDescItemId());
    }

    @Test
    void thousandsOfDistinctReferencesValidateWithoutPairs() {
        List<DescItem> items = new ArrayList<>();
        for (int i = 0; i < 3000; i++) {
            items.add(ref(apRefType, null, 1000 + i));
        }
        DescItem duplicate = ref(apRefType, null, 1000);
        items.add(duplicate);
        ActiveLevel level = activeLevel(items, null);
        // warm up the rule base, so the measurement below counts only the validation
        duplicates(activeLevel(List.of(ref(apRefType, null, 1)), null));

        ThreadMXBean threads = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        long allocatedBefore = threads.getCurrentThreadAllocatedBytes();
        List<DataValidationResult> results = duplicates(level);
        long allocated = threads.getCurrentThreadAllocatedBytes() - allocatedBefore;

        assertThat(results).extracting(DataValidationResult::getDescItemId).containsExactly(duplicate.getDescItemId());
        assertThat(allocated).as("bytes allocated by the validation").isLessThan(MAX_ALLOCATED_BYTES);
    }
}
