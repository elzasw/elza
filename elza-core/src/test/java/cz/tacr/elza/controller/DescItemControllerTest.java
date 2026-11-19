package cz.tacr.elza.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.controller.vo.ApAccessPointVO;
import cz.tacr.elza.controller.vo.ArrFundVersionVO;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeExtVO;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataFileRef;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataStructureRef;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFile;
import cz.tacr.elza.repository.FundRepository;
import cz.tacr.elza.repository.SobjVrequestRepository;
import cz.tacr.elza.service.ArrangementInternalService;
import cz.tacr.elza.test.controller.vo.BulkAppendRequest;
import cz.tacr.elza.test.controller.vo.BulkChangeResult;
import cz.tacr.elza.test.controller.vo.BulkItemValue;
import cz.tacr.elza.test.controller.vo.BulkNodeSelection;
import cz.tacr.elza.test.controller.vo.BulkSelectionType;
import cz.tacr.elza.test.controller.vo.DataRecordRef;
import cz.tacr.elza.test.controller.vo.DataStructureRef;
import cz.tacr.elza.test.controller.vo.DataText;
import cz.tacr.elza.test.controller.vo.DataType;
import cz.tacr.elza.test.controller.vo.Fund;
import cz.tacr.elza.test.controller.vo.ItemDataResult;
import cz.tacr.elza.test.controller.vo.NodeItem;
import cz.tacr.elza.test.controller.vo.NodeRef;
import cz.tacr.elza.test.controller.vo.StructuredObject;

public class DescItemControllerTest extends AbstractControllerTest {

    private static final String SRD_ENTITY_ROLE_2 = "SRD_ENTITY_ROLE_2";

    private static final String SRD_LANGUAGE_2 = "SRD_LANGUAGE_2";

    private static final String SRD_STORAGE_ID = "SRD_STORAGE_ID";

    private static final String SRD_PACKET = "SRD_PACKET";

    private static final String SRD_PACKET_NUMBER = "SRD_PACKET_NUMBER";

    private static final String SRD_ACCESS_RESTRICTION_ID = "SRD_ACCESS_RESTRICTION_ID";

    private static final String SRD_POSITION = "SRD_POSITION";

    private static final String SRD_SIMPLE_DATE = "SRD_SIMPLE_DATE";

    private static final String SRD_FILE = "SRD_FILE";

    @Autowired
    private SobjVrequestRepository sobjVrequestRepository;

    @Autowired
    private FundRepository fundRepository;

    @Autowired
    private ArrangementInternalService arrangementInternalService;

	@Test
	public void createUpdateDeleteDescItemTest() {
		Fund fund = createFund("Test fund name", "internalCode");
		assertNotNull(fund);

        ArrFundVersionVO fundVersion = getOpenVersion(fund);
        List<ArrNodeVO> nodes = createLevels(fundVersion);
        ArrNodeVO rootNode = nodes.get(0);

        // vytvoření hodnoty
        NodeItem nodeItem = buildNodeItem("SRD_SCALE", null, DataType.TEXT, "value", rootNode, null);
        ItemDataResult itemDataResult = descitemsApi.descItemCreateDescItem(fundVersion.getId(), nodeItem);
        NodeItem nodeItemCreated = itemDataResult.getItem();

        assertNotNull(((DataText) nodeItem.getData()).getTextValue().equals(((DataText) nodeItemCreated.getData()).getTextValue()));
        assertNotNull(nodeItemCreated.getPosition());
        assertNotNull(nodeItemCreated.getItemObjectId());

        // aktualizace hodnoty
        ((DataText) nodeItemCreated.getData()).setTextValue("update value");
        itemDataResult = descitemsApi.descItemUpdateDescItem(fundVersion.getId(), true, nodeItemCreated);
        NodeItem nodeItemUpdated = itemDataResult.getItem();

        assertTrue(!nodeItemUpdated.getId().equals(nodeItemCreated.getId()));
        assertTrue(nodeItemUpdated.getItemObjectId().equals(nodeItemCreated.getItemObjectId()));
        assertTrue(nodeItemUpdated.getPosition().equals(nodeItemCreated.getPosition()));
        assertTrue(((DataText) nodeItemUpdated.getData()).getTextValue().equals(((DataText) nodeItemCreated.getData()).getTextValue()));

        // odstranění hodnoty
        itemDataResult = descitemsApi.descItemDeleteDescItem(fundVersion.getId(), nodeItemUpdated);
        NodeItem nodeItemDeleted = itemDataResult.getItem();

        assertTrue(nodeItemDeleted.getId().equals(nodeItemUpdated.getId()));
        assertTrue(nodeItemDeleted.getItemObjectId().equals(nodeItemUpdated.getItemObjectId()));
        assertTrue(nodeItemDeleted.getPosition().equals(nodeItemUpdated.getPosition()));
        assertTrue(((DataText) nodeItemDeleted.getData()).getTextValue().equals(((DataText) nodeItemUpdated.getData()).getTextValue()));
	}

    @Test
    public void appendRecordRefValuesTest() {
        Fund fund = createFund("Bulk append record ref", "bulkAppendRecordRef");
        ArrFundVersionVO fundVersion = getOpenVersion(fund);
        List<ArrNodeVO> nodes = createLevels(fundVersion);
        Integer firstNodeId = nodes.get(1).getId();
        Integer secondNodeId = nodes.get(2).getId();
        List<Integer> nodeIds = List.of(firstNodeId, secondNodeId);

        RulDescItemTypeExtVO type = findDescItemTypeByCode(SRD_ENTITY_ROLE);
        Integer firstSpecId = findDescItemSpecByCode(SRD_ENTITY_ROLE_1, type).getId();
        Integer secondSpecId = findDescItemSpecByCode(SRD_ENTITY_ROLE_2, type).getId();
        List<ApAccessPointVO> accessPoints = findRecord(null, null, null, null, null);
        ApAccessPointVO firstAccessPoint = accessPoints.get(0);
        ApAccessPointVO secondAccessPoint = accessPoints.get(1);

        // the first node already has the first value
        descitemsApi.descItemCreateDescItem(fundVersion.getId(),
                buildNodeItem(SRD_ENTITY_ROLE, SRD_ENTITY_ROLE_1, DataType.RECORD_REF, firstAccessPoint, nodes.get(1), null));
        helperTestService.waitForWorkers();
        List<NodeRef> nodeRefsBeforeAppend = nodeRefs(nodeIds);

        List<BulkItemValue> values = List.of(
                bulkValue(type, SRD_ENTITY_ROLE_1, DataType.RECORD_REF, firstAccessPoint),
                bulkValue(type, SRD_ENTITY_ROLE_2, DataType.RECORD_REF, secondAccessPoint));
        BulkChangeResult result = descitemsApi.descItemAppendDescItems(fundVersion.getId(),
                bulkAppendRequest(type, selectNodes(nodeRefsBeforeAppend), values));
        helperTestService.waitForWorkers();

        assertEquals(3, result.getCreated());
        assertEquals(1, result.getSkippedDuplicate());
        assertEquals(2, result.getChangedNodes());

        List<ItemSnapshot> items = findOpenItems(nodeIds, type);
        assertEquals(4, items.size());
        assertItem(items.get(0), firstNodeId, 1, firstSpecId, firstAccessPoint);
        assertItem(items.get(1), firstNodeId, 2, secondSpecId, secondAccessPoint);
        assertItem(items.get(2), secondNodeId, 1, firstSpecId, firstAccessPoint);
        assertItem(items.get(3), secondNodeId, 2, secondSpecId, secondAccessPoint);
        Set<Integer> appendChangeIds = new HashSet<>(List.of(items.get(1).createChangeId(),
                items.get(2).createChangeId(), items.get(3).createChangeId()));
        assertEquals(1, appendChangeIds.size());
        assertNotEquals(items.get(0).createChangeId(), items.get(1).createChangeId());

        // repeating the request adds nothing and leaves the node versions as they are
        List<NodeRef> nodeRefsAfterAppend = nodeRefs(nodeIds);
        BulkChangeResult repeatedResult = descitemsApi.descItemAppendDescItems(fundVersion.getId(),
                bulkAppendRequest(type, selectNodes(nodeRefsAfterAppend), values));
        assertEquals(0, repeatedResult.getCreated());
        assertEquals(4, repeatedResult.getSkippedDuplicate());
        assertEquals(0, repeatedResult.getChangedNodes());
        assertEquals(nodeRefsAfterAppend, nodeRefs(nodeIds));

        // a node version from before the first append is refused
        List<BulkItemValue> newValues = List.of(bulkValue(type, SRD_ENTITY_ROLE_1, DataType.RECORD_REF, accessPoints.get(2)));
        assertThrows(HttpServerErrorException.class, () -> descitemsApi.descItemAppendDescItems(fundVersion.getId(),
                bulkAppendRequest(type, selectNodes(nodeRefsBeforeAppend), newValues)));
        assertEquals(4, findOpenItems(nodeIds, type).size());
    }

    @Test
    public void appendEnumValuesToWholeFundTest() {
        Fund fund = createFund("Bulk append enum", "bulkAppendEnum");
        ArrFundVersionVO fundVersion = getOpenVersion(fund);
        List<Integer> nodeIds = createLevels(fundVersion).stream().map(ArrNodeVO::getId).toList();

        RulDescItemTypeExtVO type = findDescItemTypeByCode(SRD_LANGUAGE);
        Integer firstSpecId = findDescItemSpecByCode(SRD_LANGUAGE_1, type).getId();
        Integer secondSpecId = findDescItemSpecByCode(SRD_LANGUAGE_2, type).getId();
        List<BulkItemValue> values = List.of(
                bulkValue(type, SRD_LANGUAGE_1, DataType.ENUM, null),
                bulkValue(type, SRD_LANGUAGE_2, DataType.ENUM, null));
        BulkChangeResult result = descitemsApi.descItemAppendDescItems(fundVersion.getId(),
                bulkAppendRequest(type, new BulkNodeSelection().selectionType(BulkSelectionType.FUND), values));
        helperTestService.waitForWorkers();

        // the root node belongs to the whole fund too
        assertEquals(nodeIds.size() * values.size(), result.getCreated());
        assertEquals(0, result.getSkippedDuplicate());
        assertEquals(nodeIds.size(), result.getChangedNodes());

        Map<Integer, List<Integer>> specIdsByNodeId = findOpenItems(nodeIds, type).stream()
                .collect(Collectors.groupingBy(ItemSnapshot::nodeId,
                        Collectors.mapping(ItemSnapshot::specId, Collectors.toList())));
        assertEquals(new HashSet<>(nodeIds), specIdsByNodeId.keySet());
        specIdsByNodeId.values().forEach(specIds -> assertEquals(List.of(firstSpecId, secondSpecId), specIds));
    }

    @Test
    public void appendDescItemsValidationTest() {
        Fund fund = createFund("Bulk append validation", "bulkAppendValidation");
        ArrFundVersionVO fundVersion = getOpenVersion(fund);
        Integer nodeId = createLevels(fundVersion).get(1).getId();

        RulDescItemTypeExtVO languageType = findDescItemTypeByCode(SRD_LANGUAGE);
        RulDescItemTypeExtVO roleType = findDescItemTypeByCode(SRD_ENTITY_ROLE);
        BulkItemValue language = bulkValue(languageType, SRD_LANGUAGE_1, DataType.ENUM, null);
        BulkItemValue specOfOtherType = bulkValue(roleType, SRD_ENTITY_ROLE_1, DataType.ENUM, null);
        BulkItemValue textValue = bulkValue(languageType, SRD_LANGUAGE_1, DataType.TEXT, "text");

        assertAppendFails(fundVersion, languageType, nodeId, List.of(specOfOtherType));
        assertAppendFails(fundVersion, languageType, nodeId, List.of(textValue));
        assertAppendFails(fundVersion, languageType, nodeId, List.of(language, language));

        Fund otherFund = createFund("Bulk append other fund", "bulkAppendOtherFund");
        Integer otherRootNodeId = fundVersionRepository.findById(getOpenVersion(otherFund).getId()).orElseThrow()
                .getRootNodeId();
        assertAppendFails(fundVersion, languageType, otherRootNodeId, List.of(language));

        BulkItemValue missingAccessPoint = bulkValue(roleType, SRD_ENTITY_ROLE_1, DataType.RECORD_REF,
                findRecord(null, null, null, null, null).get(0));
        ((DataRecordRef) missingAccessPoint.getData()).setValue(Integer.MAX_VALUE);
        assertThrows(HttpClientErrorException.NotFound.class, () -> descitemsApi.descItemAppendDescItems(
                fundVersion.getId(), bulkAppendRequest(roleType, selectNodes(nodeRefs(List.of(nodeId))),
                        List.of(missingAccessPoint))));

        List<NodeRef> missingNode = List.of(new NodeRef().id(Integer.MAX_VALUE).version(0));
        assertThrows(HttpClientErrorException.NotFound.class, () -> descitemsApi.descItemAppendDescItems(
                fundVersion.getId(), bulkAppendRequest(languageType, selectNodes(missingNode), List.of(language))));

        assertTrue(findOpenItems(List.of(nodeId), languageType).isEmpty());

        approveVersion(fundVersion);
        assertAppendFails(fundVersion, languageType, nodeId, List.of(language));
    }

    @Test
    public void appendStructuredValuesTest() {
        Fund fund = createFund("Bulk append structured", "bulkAppendStructured");
        ArrFundVersionVO fundVersion = getOpenVersion(fund);
        List<ArrNodeVO> nodes = createLevels(fundVersion);
        Integer firstNodeId = nodes.get(1).getId();
        Integer secondNodeId = nodes.get(2).getId();
        List<Integer> nodeIds = List.of(firstNodeId, secondNodeId);
        RulDescItemTypeExtVO storageType = findDescItemTypeByCode(SRD_STORAGE_ID);

        Integer firstPacketId = createPacket(fund, 1, true);
        Integer secondPacketId = createPacket(fund, 2, true);
        List<BulkItemValue> values = List.of(structuredValue(firstPacketId), structuredValue(secondPacketId));
        BulkChangeResult result = descitemsApi.descItemAppendDescItems(fundVersion.getId(),
                bulkAppendRequest(storageType, selectNodes(nodeRefs(nodeIds)), values));
        helperTestService.waitForWorkers();

        assertEquals(4, result.getCreated());
        assertEquals(0, result.getSkippedDuplicate());
        assertEquals(2, result.getChangedNodes());
        List<ItemSnapshot> items = findOpenItems(nodeIds, storageType);
        assertEquals(List.of(firstNodeId, firstNodeId, secondNodeId, secondNodeId),
                items.stream().map(ItemSnapshot::nodeId).toList());
        assertEquals(List.of(firstPacketId, secondPacketId, firstPacketId, secondPacketId),
                items.stream().map(ItemSnapshot::structuredObjectId).toList());

        BulkChangeResult repeatedResult = descitemsApi.descItemAppendDescItems(fundVersion.getId(),
                bulkAppendRequest(storageType, selectNodes(nodeRefs(nodeIds)), values));
        assertEquals(0, repeatedResult.getCreated());
        assertEquals(4, repeatedResult.getSkippedDuplicate());

        // only objects offered by the client can be appended
        Integer temporaryPacketId = createPacket(fund, 3, false);
        Integer notAssignablePacketId = createPacket(fund, 4, true);
        structureApi.sdoSetDataAssignable(fund.getId(), false, List.of(notAssignablePacketId));
        Fund otherFund = createFund("Bulk append structured other fund", "bulkAppendStructuredOther");
        Integer otherFundPacketId = createPacket(otherFund, 1, true);
        assertAppendFails(fundVersion, storageType, firstNodeId, List.of(structuredValue(temporaryPacketId)));
        assertAppendFails(fundVersion, storageType, firstNodeId, List.of(structuredValue(notAssignablePacketId)));
        assertAppendFails(fundVersion, storageType, firstNodeId, List.of(structuredValue(otherFundPacketId)));
        assertThrows(HttpClientErrorException.NotFound.class, () -> descitemsApi.descItemAppendDescItems(
                fundVersion.getId(), bulkAppendRequest(storageType, selectNodes(nodeRefs(List.of(firstNodeId))),
                        List.of(structuredValue(Integer.MAX_VALUE)))));

        // an object of an anonymous structure type belongs to a single value
        RulDescItemTypeExtVO restrictionType = findDescItemTypeByCode(SRD_ACCESS_RESTRICTION_ID);
        assertAppendFails(fundVersion, restrictionType, firstNodeId, List.of(structuredValue(firstPacketId)));

        assertEquals(4, findOpenItems(nodeIds, storageType).size());
        waitForValidationQueue();
    }

    @Test
    public void appendCoordinatesDateAndFileValuesTest() {
        Fund fund = createFund("Bulk append coordinates, date and file", "bulkAppendCoordinatesDateFile");
        ArrFundVersionVO fundVersion = getOpenVersion(fund);
        Integer nodeId = createLevels(fundVersion).get(1).getId();
        List<Integer> nodeIds = List.of(nodeId);

        RulDescItemTypeExtVO positionType = findDescItemTypeByCode(SRD_POSITION);
        List<BulkItemValue> positions = List.of(valueWithoutSpec(DataType.COORDINATES, "POINT (14 50)"),
                valueWithoutSpec(DataType.COORDINATES, "POINT (15 49)"));
        assertEquals(2, appendToNode(fundVersion, positionType, nodeId, positions).getCreated());
        assertEquals(2, appendToNode(fundVersion, positionType, nodeId, positions).getSkippedDuplicate());

        RulDescItemTypeExtVO dateType = findDescItemTypeByCode(SRD_SIMPLE_DATE);
        List<BulkItemValue> dates = List.of(valueWithoutSpec(DataType.DATE, LocalDate.of(2026, 11, 11)));
        assertEquals(1, appendToNode(fundVersion, dateType, nodeId, dates).getCreated());
        assertEquals(1, appendToNode(fundVersion, dateType, nodeId, dates).getSkippedDuplicate());

        RulDescItemTypeExtVO fileType = findDescItemTypeByCode(SRD_FILE);
        List<BulkItemValue> files = List.of(valueWithoutSpec(DataType.FILE_REF, createFundFile(fund)));
        assertEquals(1, appendToNode(fundVersion, fileType, nodeId, files).getCreated());
        assertEquals(1, appendToNode(fundVersion, fileType, nodeId, files).getSkippedDuplicate());

        assertAppendFails(fundVersion, positionType, nodeId, List.of(valueWithoutSpec(DataType.COORDINATES, " ")));
        assertAppendFails(fundVersion, positionType, nodeId, List.of(valueWithoutSpec(DataType.COORDINATES, "POINT")));

        // only a file of the fund can be appended
        Fund otherFund = createFund("Bulk append file other fund", "bulkAppendFileOther");
        assertAppendFails(fundVersion, fileType, nodeId,
                List.of(valueWithoutSpec(DataType.FILE_REF, createFundFile(otherFund))));
        assertThrows(HttpClientErrorException.NotFound.class, () -> appendToNode(fundVersion, fileType, nodeId,
                List.of(valueWithoutSpec(DataType.FILE_REF, Integer.MAX_VALUE))));

        assertEquals(2, findOpenItems(nodeIds, positionType).size());
        assertEquals(1, findOpenItems(nodeIds, dateType).size());
        assertEquals(1, findOpenItems(nodeIds, fileType).size());
    }

    @Test
    public void replaceFileContentRepointsItemsTest() {
        Fund fund = createFund("Replace attachment content", "replaceAttachment");
        ArrFundVersionVO fundVersion = getOpenVersion(fund);
        List<Integer> nodeIds = createLevels(fundVersion).stream().map(ArrNodeVO::getId).toList();
        RulDescItemTypeExtVO fileType = findDescItemTypeByCode(SRD_FILE);

        Integer oldFileId = createFundFile(fund);
        for (Integer nodeId : nodeIds) {
            appendToNode(fundVersion, fileType, nodeId, List.of(valueWithoutSpec(DataType.FILE_REF, oldFileId)));
        }

        Integer newFileId = replaceFundFile(oldFileId, "opravená příloha");
        helperTestService.waitForWorkers();

        assertNotEquals(oldFileId, newFileId);
        List<Integer> openFileIds = findOpenFileIds(nodeIds, fileType);
        assertEquals(nodeIds.size(), openFileIds.size());
        assertTrue(openFileIds.stream().allMatch(newFileId::equals));
    }

    private BulkChangeResult appendToNode(final ArrFundVersionVO fundVersion,
                                          final RulDescItemTypeExtVO type,
                                          final Integer nodeId,
                                          final List<BulkItemValue> values) {
        BulkChangeResult result = descitemsApi.descItemAppendDescItems(fundVersion.getId(),
                bulkAppendRequest(type, selectNodes(nodeRefs(List.of(nodeId))), values));
        helperTestService.waitForWorkers();
        return result;
    }

    private void assertAppendFails(final ArrFundVersionVO fundVersion,
                                   final RulDescItemTypeExtVO type,
                                   final Integer nodeId,
                                   final List<BulkItemValue> values) {
        BulkAppendRequest request = bulkAppendRequest(type, selectNodes(nodeRefs(List.of(nodeId))), values);
        assertThrows(HttpServerErrorException.class,
                () -> descitemsApi.descItemAppendDescItems(fundVersion.getId(), request));
    }

    private BulkAppendRequest bulkAppendRequest(final RulDescItemTypeExtVO type,
                                                final BulkNodeSelection selection,
                                                final List<BulkItemValue> values) {
        return new BulkAppendRequest().itemTypeId(type.getId()).selection(selection).values(values);
    }

    private BulkItemValue bulkValue(final RulDescItemTypeExtVO type,
                                    final String specCode,
                                    final DataType dataType,
                                    final Object value) {
        return new BulkItemValue()
                .itemSpecId(findDescItemSpecByCode(specCode, type).getId())
                .data(createItemDataByDataTypeAndValue(dataType, value));
    }

    private BulkItemValue structuredValue(final Integer structuredObjectId) {
        return new BulkItemValue()
                .data(new DataStructureRef().dataType(DataType.STRUCTURED).structuredObjectId(structuredObjectId));
    }

    private BulkItemValue valueWithoutSpec(final DataType dataType, final Object value) {
        return new BulkItemValue().data(createItemDataByDataTypeAndValue(dataType, value));
    }

    private Integer createFundFile(final Fund fund) {
        byte[] content = "příloha".getBytes(StandardCharsets.UTF_8);
        return new TransactionTemplate(tm).execute(status -> {
            ArrFile file = new ArrFile();
            file.setFund(fundRepository.findById(fund.getId()).orElseThrow());
            file.setCreateChange(arrangementInternalService.createChange(ArrChange.Type.ADD_ATTACHMENT));
            file.setName("Příloha");
            file.setFileName("priloha.txt");
            file.setMimeType("text/plain");
            file.setFileSize(content.length);
            try {
                dmsService.createFile(file, new ByteArrayInputStream(content));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return file.getFileId();
        });
    }

    /**
     * Replaces the file content the same way as DmsController.updateFile with an uploaded file.
     */
    private Integer replaceFundFile(final Integer fileId, final String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        return new TransactionTemplate(tm).execute(status -> {
            ArrFile oldFile = dmsService.getArrFile(fileId);
            ArrFile newFile = new ArrFile();
            newFile.setFileName("priloha-2.txt");
            newFile.setMimeType("text/plain");
            newFile.setFileSize(bytes.length);
            try {
                dmsService.replaceArrFileContent(oldFile, newFile, new ByteArrayInputStream(bytes));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return newFile.getFileId();
        });
    }

    private List<Integer> findOpenFileIds(final List<Integer> nodeIds, final RulDescItemTypeExtVO type) {
        return new TransactionTemplate(tm).execute(status -> descItemService
                .findByNodeIdsAndDeleteChangeIsNull(nodeIds, List.of(type.getId())).stream()
                .map(DescItemControllerTest::fileIdOf)
                .toList());
    }

    private static Integer fileIdOf(final ArrDescItem item) {
        ArrDataFileRef data = HibernateUtils.unproxy(item.getData());
        return data.getFileId();
    }

    /**
     * Creates a packet with a number, confirmed when asked; a confirmed packet is assignable.
     *
     * The confirmation queues the generation of the packet value, and the generator saves the whole
     * packet. The method returns after the generator is done, so it cannot overwrite a later change
     * of the packet (e.g. of its assignability) with the state it loaded earlier.
     */
    private Integer createPacket(final Fund fund, final int number, final boolean confirm) {
        StructuredObject packet = structureApi.sdoCreateObject(fund.getId(), SRD_PACKET, null);
        structureApi.sdoCreateItem(fund.getId(), packet.getId(),
                buildStructuredObjectItem(SRD_PACKET_NUMBER, null, number, null, null, null));
        if (confirm) {
            structureApi.sdoConfirm(fund.getId(), packet.getId());
            waitForValidationQueue();
        }
        return packet.getId();
    }

    /**
     * Waits until the structured object values are generated, so no revalidation runs after the test.
     */
    private void waitForValidationQueue() {
        await().atMost(2, MINUTES)
                .pollInterval(50, MILLISECONDS)
                .untilAsserted(() -> assertEquals(0, sobjVrequestRepository.count(),
                        "Fronta revalidací strukturovaných objektů nebyla zpracována"));
    }

    private BulkNodeSelection selectNodes(final List<NodeRef> nodeRefs) {
        return new BulkNodeSelection().selectionType(BulkSelectionType.NODES).nodes(nodeRefs);
    }

    private List<NodeRef> nodeRefs(final List<Integer> nodeIds) {
        return nodeIds.stream()
                .map(nodeId -> nodeRepository.findById(nodeId).orElseThrow())
                .map(node -> new NodeRef().id(node.getNodeId()).version(node.getVersion()))
                .toList();
    }

    private record ItemSnapshot(Integer nodeId, Integer position, Integer specId, Integer recordId,
                                Integer structuredObjectId, Integer createChangeId) {
    }

    /**
     * Open items of the type on the nodes, ordered by node and position.
     */
    private List<ItemSnapshot> findOpenItems(final List<Integer> nodeIds, final RulDescItemTypeExtVO type) {
        return new TransactionTemplate(tm).execute(status -> descItemService
                .findByNodeIdsAndDeleteChangeIsNull(nodeIds, List.of(type.getId())).stream()
                .map(item -> {
                    ArrData data = HibernateUtils.unproxy(item.getData());
                    Integer recordId = data instanceof ArrDataRecordRef recordRef ? recordRef.getRecordId() : null;
                    Integer structuredObjectId = data instanceof ArrDataStructureRef structureRef
                            ? structureRef.getStructuredObjectId()
                            : null;
                    return new ItemSnapshot(item.getNodeId(), item.getPosition(), item.getItemSpecId(), recordId,
                            structuredObjectId, item.getCreateChangeId());
                })
                .sorted(Comparator.comparing(ItemSnapshot::nodeId).thenComparing(ItemSnapshot::position))
                .toList());
    }

    private void assertItem(final ItemSnapshot item,
                            final Integer nodeId,
                            final int position,
                            final Integer specId,
                            final ApAccessPointVO accessPoint) {
        assertEquals(nodeId, item.nodeId());
        assertEquals(position, item.position());
        assertEquals(specId, item.specId());
        assertEquals(accessPoint.getId(), item.recordId());
    }
}
