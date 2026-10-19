package cz.tacr.elza.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import cz.tacr.elza.controller.vo.ArrFundVersionVO;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.core.data.CoreMessage;
import cz.tacr.elza.domain.ArrNodeConformity;
import cz.tacr.elza.domain.ArrNodeConformityError;
import cz.tacr.elza.repository.NodeConformityErrorRepository;
import cz.tacr.elza.repository.NodeConformityRepository;
import cz.tacr.elza.test.controller.vo.DataType;
import cz.tacr.elza.test.controller.vo.Fund;
import cz.tacr.elza.test.controller.vo.NodeItem;

/**
 * Validation reports a link (URI-REF elza-node://) to a deleted unit of description as an
 * error of the referring node.
 */
public class BrokenNodeLinkValidationTest extends AbstractControllerTest {

    private static final String LINK_TYPE = "ODKAZ_OBECNY";

    @Autowired
    private NodeConformityRepository nodeConformityRepository;

    @Autowired
    private NodeConformityErrorRepository nodeConformityErrorRepository;

    @Test
    public void linkToDeletedNode() {
        Fund fund = createFund("Broken link fund", "broken-link");
        ArrFundVersionVO fundVersion = getOpenVersion(fund);
        // four children of the root, in tree order
        List<ArrNodeVO> nodes = createLevels(fundVersion);
        ArrNodeVO referring = nodes.get(0);
        ArrNodeVO target = nodes.get(1);
        ArrNodeVO laterReferring = nodes.get(2);
        String targetLink = "elza-node://" + nodeApi.nodeGetNodeInfoById(target.getId()).getUuid();

        createLink(fundVersion, referring, targetLink);
        helperTestService.waitForWorkers();
        assertEquals(0, countBrokenLinkErrors(referring.getId()), "a link to a live node is valid");

        // deleting the target clears the link target of the referring item
        deleteLevel(fundVersion, target, findRoot(fundVersion));
        helperTestService.waitForWorkers();
        assertEquals(1, countBrokenLinkErrors(referring.getId()));

        // a link saved after the deletion resolves the UUID to the deleted node
        createLink(fundVersion, laterReferring, targetLink);
        helperTestService.waitForWorkers();
        assertEquals(1, countBrokenLinkErrors(laterReferring.getId()));
    }

    private void createLink(final ArrFundVersionVO fundVersion, final ArrNodeVO node, final String link) {
        NodeItem nodeItem = buildNodeItem(LINK_TYPE, null, DataType.URI_REF, link, node, null);
        descitemsApi.descItemCreateDescItem(fundVersion.getId(), nodeItem);
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

    private long countBrokenLinkErrors(final Integer nodeId) {
        List<ArrNodeConformity> conformities = nodeConformityRepository.findByNodeIds(Collections.singletonList(nodeId));
        assertTrue(!conformities.isEmpty(), "node " + nodeId + " is validated");
        List<String> descriptions = nodeConformityErrorRepository.findByNodeConformityInfos(conformities).stream()
                .map(ArrNodeConformityError::getDescription)
                .collect(Collectors.toList());
        return descriptions.stream().filter(d -> d != null && d.contains(CoreMessage.ARR_BROKEN_NODE_REF.name()))
                .count();
    }
}
