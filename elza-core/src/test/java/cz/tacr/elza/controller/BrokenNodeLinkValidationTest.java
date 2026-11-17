package cz.tacr.elza.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.controller.vo.ArrFundVO;
import cz.tacr.elza.controller.vo.ArrFundVersionVO;
import cz.tacr.elza.controller.vo.UsrPermissionVO;
import cz.tacr.elza.controller.vo.UsrUserVO;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemUriRefVO;
import cz.tacr.elza.core.data.CoreMessage;
import cz.tacr.elza.core.data.PackageTexts;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.data.ValidationMessage;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeConformity;
import cz.tacr.elza.domain.ArrNodeConformityError;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.repository.NodeConformityErrorRepository;
import cz.tacr.elza.repository.NodeConformityRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.test.controller.vo.DataType;
import cz.tacr.elza.test.controller.vo.DataUriRef;
import cz.tacr.elza.test.controller.vo.Fund;
import cz.tacr.elza.test.controller.vo.ItemDataResult;
import cz.tacr.elza.test.controller.vo.NodeItem;

/**
 * A link (URI-REF elza-node://) to a deleted unit of description has no target and validation
 * reports it as an error of the referring node.
 */
public class BrokenNodeLinkValidationTest extends AbstractControllerTest {

    private static final String LINK_TYPE = "ODKAZ_OBECNY";

    private static final String FUND_NAME = "Broken link fund";

    private static final String USR_NAME = "deleting-user";
    private static final String USR_PSWD = "deleting-user";

    private static final Set<String> BROKEN_LINK_KEYS = Set.of(CoreMessage.ARR_DELETED_NODE_REF.key(),
            CoreMessage.ARR_UNKNOWN_NODE_REF.key());

    /**
     * Units of {@link #ALL_IN_ONE_XML}: the first one links to the second one.
     */
    private static final String IMPORTED_REFERRING_UUID = "44a39a98-79c5-48c2-8515-af93758fad15";
    private static final String IMPORTED_TARGET_UUID = "f4b883cd-b140-4282-9d42-5a080a7a8333";

    @Autowired
    private NodeConformityRepository nodeConformityRepository;

    @Autowired
    private NodeConformityErrorRepository nodeConformityErrorRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private PackageTexts packageTexts;

    @Autowired
    private StaticDataService staticDataService;

    @Test
    public void linkToDeletedNode() {
        Fund fund = createFund(FUND_NAME, "broken-link");
        ArrFundVersionVO fundVersion = getOpenVersion(fund);
        // four children of the root, in tree order
        List<ArrNodeVO> nodes = createLevels(fundVersion);
        ArrNodeVO referring = nodes.get(0);
        ArrNodeVO target = nodes.get(1);
        ArrNodeVO laterReferring = nodes.get(2);
        ArrNodeVO unknownReferring = nodes.get(3);
        String targetLink = "elza-node://" + nodeApi.nodeGetNodeInfoById(target.getId()).getUuid();

        createLink(fundVersion, referring, targetLink);
        helperTestService.waitForWorkers();
        assertEquals(target.getId(), getLinkTarget(fundVersion, referring.getId()));
        assertEquals(List.of(), brokenLinkErrors(referring.getId()), "a link to a live node is valid");

        // the target is moved by the built-in admin (a change without a user): its old level is
        // deleted by the move, so the unit has two deleted levels after its deletion
        ArrNodeVO root = findRoot(fundVersion);
        moveLevelUnder(fundVersion, current(unknownReferring), current(root), List.of(current(target)),
                       current(root));
        helperTestService.waitForWorkers();

        // deleting the target clears the link target of the referring item; the error names
        // the fund and the user of the deletion, not of the move (the built-in admin has no
        // usr_user row, so a real user deletes it)
        UsrUserVO user = createUser(findRecord(null, null, null, null, null).get(0).getId(), USR_NAME, USR_PSWD);
        UsrPermissionVO adminPerm = new UsrPermissionVO();
        adminPerm.setPermission(Permission.ADMIN);
        addUserPermission(user.getId(), List.of(adminPerm));
        login(USR_NAME, USR_PSWD);
        deleteLevel(fundVersion, current(target), current(unknownReferring));
        login("admin", "admin");
        helperTestService.waitForWorkers();
        assertNull(getLinkTarget(fundVersion, referring.getId()));
        List<String> errors = brokenLinkErrors(referring.getId());
        assertEquals(1, errors.size());
        ValidationMessage deleted = ValidationMessage.decode(errors.get(0));
        assertEquals(CoreMessage.ARR_DELETED_NODE_REF.key(), deleted.getKey());
        assertEquals(targetLink, deleted.getArgs().get(1));
        assertEquals(FUND_NAME, deleted.getArgs().get(2));
        assertEquals(USR_NAME, deleted.getArgs().get(4));
        new TransactionTemplate(tm).executeWithoutResult(status -> {
            StaticDataProvider sdp = staticDataService.getData();
            String cs = packageTexts.render(errors.get(0), sdp.getSysLanguageByTag("cs"));
            assertTrue(cs.contains("byla smazána z archivního souboru „" + FUND_NAME + "“ dne "), cs);
            assertTrue(cs.contains("(uživatel: " + USR_NAME + ")"), cs);
            String en = packageTexts.render(errors.get(0), sdp.getSysLanguageByTag("en"));
            assertTrue(en.contains("was deleted from the fund “" + FUND_NAME + "” on "), en);
            assertTrue(en.contains(String.valueOf(LocalDate.now().getYear())), en);
        });

        // a link saved after the deletion is not resolved to the deleted node
        ItemDataResult created = createLink(fundVersion, current(laterReferring), targetLink);
        helperTestService.waitForWorkers();
        assertNull(getLinkTarget(fundVersion, laterReferring.getId()));
        assertEquals(List.of(CoreMessage.ARR_DELETED_NODE_REF.key()), brokenLinkErrorKeys(laterReferring.getId()));

        // neither is an updated one
        NodeItem link = created.getItem();
        ((DataUriRef) link.getData()).setDescription("updated link");
        descitemsApi.descItemUpdateDescItem(fundVersion.getId(), true, link);
        helperTestService.waitForWorkers();
        assertNull(getLinkTarget(fundVersion, laterReferring.getId()));
        assertEquals(List.of(CoreMessage.ARR_DELETED_NODE_REF.key()), brokenLinkErrorKeys(laterReferring.getId()));

        // a link to a UUID no unit has is reported as not found
        createLink(fundVersion, current(unknownReferring), "elza-node://" + UUID.randomUUID());
        helperTestService.waitForWorkers();
        assertNull(getLinkTarget(fundVersion, unknownReferring.getId()));
        assertEquals(List.of(CoreMessage.ARR_UNKNOWN_NODE_REF.key()), brokenLinkErrorKeys(unknownReferring.getId()));

        // an import resolves the unresolved links of the whole database: its own link is
        // resolved, the links to the deleted node stay without target
        importXmlFile(null, getAllScopes().get(0).getId(), getFile(ALL_IN_ONE_XML));
        helperTestService.waitForWorkers();
        ArrNode importedReferring = nodeRepository.findOneByUuid(IMPORTED_REFERRING_UUID);
        ArrNode importedTarget = nodeRepository.findOneByUuid(IMPORTED_TARGET_UUID);
        assertNotNull(importedReferring);
        assertNotNull(importedTarget);
        ArrFundVersionVO importedVersion = getOpenVersion(findImportedFund(fund).getId());
        assertEquals(importedTarget.getNodeId(), getLinkTarget(importedVersion, importedReferring.getNodeId()));

        assertNull(getLinkTarget(fundVersion, referring.getId()));
        assertNull(getLinkTarget(fundVersion, laterReferring.getId()));
    }

    /**
     * @return the node with its current version (a move or a deletion changes the versions)
     */
    private ArrNodeVO current(final ArrNodeVO node) {
        ArrNode arrNode = nodeRepository.findById(node.getId()).orElseThrow();
        ArrNodeVO currentNode = new ArrNodeVO();
        currentNode.setId(arrNode.getNodeId());
        currentNode.setVersion(arrNode.getVersion());
        return currentNode;
    }

    private ItemDataResult createLink(final ArrFundVersionVO fundVersion, final ArrNodeVO node, final String link) {
        NodeItem nodeItem = buildNodeItem(LINK_TYPE, null, DataType.URI_REF, link, node, null);
        return descitemsApi.descItemCreateDescItem(fundVersion.getId(), nodeItem);
    }

    /**
     * @return target of the only elza-node link of the node, as read from the node cache
     */
    private Integer getLinkTarget(final ArrFundVersionVO fundVersion, final Integer nodeId) {
        List<ArrItemUriRefVO> links = getNodeFormData(nodeId, fundVersion.getId()).getDescItems().stream()
                .filter(ArrItemUriRefVO.class::isInstance)
                .map(ArrItemUriRefVO.class::cast)
                .filter(i -> i.getValue() != null && i.getValue().startsWith("elza-node://"))
                .collect(Collectors.toList());
        assertEquals(1, links.size(), "node " + nodeId + " has one elza-node link");
        return links.get(0).getNodeId();
    }

    private ArrFundVO findImportedFund(final Fund otherFund) {
        List<ArrFundVO> imported = getFunds().stream()
                .filter(f -> !f.getId().equals(otherFund.getId()))
                .collect(Collectors.toList());
        assertEquals(1, imported.size());
        return imported.get(0);
    }

    private ArrNodeVO findRoot(final ArrFundVersionVO fundVersion) {
        ArrangementController.FaTreeParam input = new ArrangementController.FaTreeParam();
        input.setVersionId(fundVersion.getId());
        var root = getFundTree(input).getNodes().iterator().next();
        ArrNodeVO rootNode = new ArrNodeVO();
        rootNode.setId(root.getId());
        rootNode.setVersion(root.getVersion());
        return rootNode;
    }

    /**
     * @return stored descriptions of the broken link errors of the node
     */
    private List<String> brokenLinkErrors(final Integer nodeId) {
        List<ArrNodeConformity> conformities = nodeConformityRepository.findByNodeIds(Collections.singletonList(nodeId));
        assertTrue(!conformities.isEmpty(), "node " + nodeId + " is validated");
        List<ArrNodeConformityError> errors = nodeConformityErrorRepository.findByNodeConformityInfos(conformities);
        return errors.stream()
                .map(ArrNodeConformityError::getDescription)
                .filter(BrokenNodeLinkValidationTest::isBrokenLinkError)
                .collect(Collectors.toList());
    }

    private static boolean isBrokenLinkError(final String description) {
        ValidationMessage message = ValidationMessage.decode(description);
        return message != null && BROKEN_LINK_KEYS.contains(message.getKey());
    }

    private List<String> brokenLinkErrorKeys(final Integer nodeId) {
        return brokenLinkErrors(nodeId).stream()
                .map(d -> ValidationMessage.decode(d).getKey())
                .collect(Collectors.toList());
    }
}
