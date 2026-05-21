package cz.tacr.elza.controller;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cz.tacr.elza.service.AccessPointItemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.controller.vo.ApAccessPointVO;
import cz.tacr.elza.controller.vo.ApPartFormVO;
import cz.tacr.elza.controller.vo.ApPartVO;
import cz.tacr.elza.controller.vo.RulPartTypeVO;
import cz.tacr.elza.controller.vo.UsrPermissionVO;
import cz.tacr.elza.controller.vo.UsrUserVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemStringVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemVO;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.WfTask;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.domain.WfTask.Status;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApItemRepository;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.repository.WfTaskRepository;
import cz.tacr.elza.service.PartService;
import cz.tacr.elza.test.controller.vo.AccessPointBatchExportParams;
import cz.tacr.elza.test.controller.vo.ApStateApproval;
import cz.tacr.elza.test.controller.vo.ApStateUpdate;
import cz.tacr.elza.test.controller.vo.CopyAccessPointDetail;
import cz.tacr.elza.test.controller.vo.CreatedPart;
import cz.tacr.elza.test.controller.vo.DeleteAccessPointDetail;
import cz.tacr.elza.test.controller.vo.DeleteAccessPointsDetail;
import cz.tacr.elza.test.controller.vo.EntityRef;
import cz.tacr.elza.test.controller.vo.ExportRequestStatus;
import cz.tacr.elza.test.controller.vo.InvalidatedEntities;
import cz.tacr.elza.test.controller.vo.ReplaceType;
import cz.tacr.elza.test.controller.vo.RequestProcessState;
import cz.tacr.elza.test.controller.vo.RevStateChange;
import cz.tacr.elza.test.controller.vo.RevisionState;

public class AccessPointControllerTest extends AbstractControllerTest {

    @Autowired
    PartService partService;

    @Autowired
    ApItemRepository itemRepository;

    @Autowired
    ApStateRepository stateRepository;

    @Autowired
    ApAccessPointRepository apRepository;

    @Autowired
    AccessPointItemService itemService;

	@Autowired
	private WfTaskRepository wfTaskRepository;

    @Test
    public void copyAccessPointsTest() {

        long count = apRepository.count();
        assertTrue(count == 3);
        ApAccessPoint ap = apRepository.findAccessPointByUuid("9f783015-b9af-42fc-bff4-11ff57cdb072");
        assertNotNull(ap);
        List<ApPart> parts = partService.findPartsByAccessPoint(ap);
        assertTrue(parts.size() == 3);
        List<ApItem> items = new TransactionTemplate(tm).execute(a -> {
        	return itemService.findValidItemsByAccessPoint(ap);
        });
        assertTrue(items.size() == 8);

        // let's delete the last part
        List<ApItem> itemsSkip = new TransactionTemplate(tm).execute(a -> {
        	return itemService.findValidItemsByPartId(parts.get(parts.size() - 1).getPartId());
        });
        List<Integer> skipItems = itemsSkip.stream().map(p -> p.getItemId()).collect(Collectors.toList());

        CopyAccessPointDetail copyAccessPointDetail = new CopyAccessPointDetail();
        copyAccessPointDetail.setScope(SCOPE_GLOBAL);
        copyAccessPointDetail.setReplace(true);
        copyAccessPointDetail.setSkipItems(skipItems);

        EntityRef entityRef = accesspointsApi.accessPointCopyAccessPoint(ap.getUuid(), copyAccessPointDetail);
        count = apRepository.count();
        assertTrue(count == 4); // +1

        ApAccessPoint copyAp = apRepository.findAccessPointByUuid(entityRef.getId());
        assertNotNull(copyAp);
        List<ApPart> copyParts = partService.findPartsByAccessPoint(copyAp);
        assertTrue(copyParts.size() == 2); // -1
        List<ApItem> copyItems = new TransactionTemplate(tm).execute(a -> {
        	return itemService.findValidItemsByAccessPoint(copyAp);
        });
        assertTrue(copyItems.size() == 5); // -3
    }

    @Test
    public void deleteAccessPointsTest() {

        ApAccessPoint ap1 = apRepository.findAccessPointByUuid("9f783015-b9af-42fc-bff4-11ff57cdb072");
        assertNotNull(ap1);
        List<ApPart> parts = partService.findPartsByAccessPoint(ap1);
        assertTrue(parts.size() == 3);

        ApAccessPoint ap2 = apRepository.findAccessPointByUuid("c4b13fa0-89a2-44a2-954f-e281934c3dcf");
        assertNotNull(ap2);
        parts = partService.findPartsByAccessPoint(ap2);
        assertTrue(parts.size() == 3);

        DeleteAccessPointsDetail deleteAccessPointsDetail = new DeleteAccessPointsDetail();
        List<String> uuids = Arrays.asList(ap1.getUuid(), ap2.getUuid());
        deleteAccessPointsDetail.setIds(uuids);

        accesspointsApi.accessPointDeleteAccessPoints(deleteAccessPointsDetail);

        ApAccessPointVO ap1Vo = getAccessPoint(ap1.getAccessPointId().toString());
        assertTrue(ap1Vo.isInvalid());
        assertTrue(ap1Vo.getParts().size() == 3);

        ApAccessPointVO ap2Vo = getAccessPoint(ap1.getAccessPointId().toString());
        assertTrue(ap2Vo.isInvalid());
        assertTrue(ap2Vo.getParts().size() == 3);
    }

    @Test
    public void deleteAccessPointTest() {

        ApAccessPoint ap1 = apRepository.findAccessPointByUuid("9f783015-b9af-42fc-bff4-11ff57cdb072");
        assertNotNull(ap1);
        List<ApPart> parts = partService.findPartsByAccessPoint(ap1);
        assertEquals(3, parts.size());

        ApAccessPoint ap2 = apRepository.findAccessPointByUuid("c4b13fa0-89a2-44a2-954f-e281934c3dcf");
        assertNotNull(ap2);
        parts = partService.findPartsByAccessPoint(ap2);
        assertEquals(3, parts.size());

        DeleteAccessPointDetail deleteAPDetail = new DeleteAccessPointDetail();
        deleteAPDetail.setReplacedBy(ap2.getAccessPointId().toString());

        accesspointsApi.accessPointDeleteAccessPoint(ap1.getAccessPointId().toString(), deleteAPDetail);

        // check if deleted
        ApAccessPointVO apInfo = this.getAccessPoint(ap1.getAccessPointId());
        assertNotNull(apInfo);
        assertTrue(apInfo.isInvalid());
        assertEquals(ap2.getAccessPointId(), apInfo.getReplacedById());
        assertEquals(3, apInfo.getParts().size());

        parts = partService.findPartsByAccessPoint(ap2);
        assertEquals(3, parts.size());

        // find deleted entities
        InvalidatedEntities invalidated = accesspointsApi.accessPointGetInvalidatedEntities(null, null);
        assertEquals(1, invalidated.getTotalCount());

        // try to restore AP
        accesspointsApi.accessPointRestoreAccessPoint(ap1.getAccessPointId().toString());
        apInfo = this.getAccessPoint(ap1.getAccessPointId());
        assertNotNull(apInfo);
        assertTrue(!apInfo.isInvalid());
        assertNull(apInfo.getReplacedById());
        assertEquals(apInfo.getStateApproval(), ApState.StateApproval.NEW);

        // find deleted entities
        invalidated = accesspointsApi.accessPointGetInvalidatedEntities(null, null);
        assertEquals(0, invalidated.getTotalCount());
    }

    @Test
    public void deleteAccessPointCopyAllTest() {

        ApAccessPoint ap1 = apRepository.findAccessPointByUuid("9f783015-b9af-42fc-bff4-11ff57cdb072");
        assertNotNull(ap1);
        List<ApPart> parts = partService.findPartsByAccessPoint(ap1);
        assertTrue(parts.size() == 3);

        ApAccessPoint ap2 = apRepository.findAccessPointByUuid("c4b13fa0-89a2-44a2-954f-e281934c3dcf");
        assertNotNull(ap2);
        parts = partService.findPartsByAccessPoint(ap2);
        assertTrue(parts.size() == 3);

        DeleteAccessPointDetail deleteAPDetail = new DeleteAccessPointDetail();
        deleteAPDetail.setReplacedBy(ap2.getAccessPointId().toString());
        deleteAPDetail.setReplaceType(ReplaceType.COPY_ALL);

        accesspointsApi.accessPointDeleteAccessPoint(ap1.getAccessPointId().toString(), deleteAPDetail);

        ApAccessPointVO apInfo = this.getAccessPoint(ap1.getAccessPointId());
        assertNotNull(apInfo);
        assertTrue(apInfo.isInvalid());
        assertEquals(apInfo.getReplacedById(), ap2.getAccessPointId());
        assertEquals(apInfo.getParts().size(), 3);

        ApAccessPointVO apInfo2 = this.getAccessPoint(ap2.getAccessPointId());
        assertEquals(apInfo2.getParts().size(), 6);
    }

    @Test
    public void setPreferNameRevisionTest() throws InterruptedException {
        ApAccessPoint ap1 = apRepository.findAccessPointByUuid("9f783015-b9af-42fc-bff4-11ff57cdb072");
        assertNotNull(ap1);
        ApAccessPointVO apVo = this.getAccessPoint(ap1.getAccessPointId());

        // create revision
        accesspointsApi.accessPointCreateRevision(ap1.getAccessPointId());

        RulItemType nmMainItemType = itemTypeRepository.findOneByCode(ApControllerTest.NM_MAIN);
        RulItemType nmSupGenItemType = itemTypeRepository.findOneByCode(ApControllerTest.NM_SUP_GEN);
        Map<String, RulPartTypeVO> partTypes = findPartTypesMap();
        RulPartTypeVO ptName = partTypes.get(ApControllerTest.PT_NAME);

        // add new part Karel IV
        List<ApItemVO> items = new ArrayList<>();
        items.add(buildApItem(nmMainItemType.getCode(), null, "Karel", null, null));
        items.add(buildApItem(nmSupGenItemType.getCode(), null, "IV", null, null));

        ApPartFormVO partFormVO = ApControllerTest.createPartFormVO(null, ptName.getCode(), null, items);

        CreatedPart createdPart = createPart(ap1.getAccessPointId(), partFormVO);
        Integer revPartId = createdPart.getPartId();
        assertNotNull(revPartId);

        accesspointsApi.accessPointSetPreferNameRevision(apVo.getId(), revPartId, null);

        // merge
        mergeRevision(ap1.getAccessPointId(), new ApStateUpdate().stateApproval(ApStateApproval.NEW));

        // check modified preferred part
        ApAccessPointVO apVo2 = waitForAccessPointName(ap1.getAccessPointId(), "Karel (IV)");

        // check preferred part
        ApPartVO prefPart = null;
        for (ApPartVO partVo : apVo2.getParts()) {
            if (partVo.getId().equals(apVo2.getPreferredPart())) {
                prefPart = partVo;
            }
        }
        assertNotNull(prefPart);
        assertEquals(prefPart.getValue(), "Karel (IV)");
        assertEquals(prefPart.getItems().size(), 2);
        for (ApItemVO item : prefPart.getItems()) {
            if (item.getTypeId().equals(nmMainItemType.getItemTypeId())) {
                assertNull(item.getSpecId());
                ApItemStringVO stringVo = (ApItemStringVO) item;
                assertEquals(stringVo.getValue(), "Karel");
            } else if (item.getTypeId().equals(nmSupGenItemType.getItemTypeId())) {
                assertNull(item.getSpecId());
                ApItemStringVO stringVo = (ApItemStringVO) item;
                assertEquals(stringVo.getValue(), "IV");
            } else {
                fail("Unexpected item");
            }
        }
    }

    @Test
    public void changeStateTest() {
    	final String USR_NAME1 = "usr1";
    	final String USR_PSWD1 = "1";
    	final String USR_NAME2 = "usr2";
    	final String USR_PSWD2 = "2";
    	//final String USR_ADMIN = "admin";
    	final String USR_ELZAADMIN = "elzaadmin";
    	final String USR_ELZAADMIN_PSWD = "elzaadmin";

        List<ApAccessPointVO> records = findRecord(null, null, null, null, null);
        ApAccessPointVO ap = records.get(0);
        ApAccessPointVO apUser = records.get(1);

        ApState state = stateRepository.findLastByAccessPointId(ap.getId());
        assertNotNull(state);
        
        // Create admin
        UsrUserVO userAdminVO = createUser(apUser.getId(), USR_ELZAADMIN, USR_ELZAADMIN_PSWD);
        UsrPermissionVO permissionSuper = new UsrPermissionVO();
        permissionSuper.setPermission(Permission.ADMIN);
        addUserPermission(userAdminVO.getId(), Arrays.asList(permissionSuper));
        
        login(USR_ELZAADMIN, USR_ELZAADMIN_PSWD);

        // permissions
        UsrPermissionVO permissionApRdAll = new UsrPermissionVO();
        permissionApRdAll.setPermission(Permission.AP_SCOPE_RD_ALL);
        UsrPermissionVO permissionApConfirmAll = new UsrPermissionVO();
        permissionApConfirmAll.setPermission(Permission.AP_CONFIRM_ALL);
        UsrPermissionVO permissionApEditConfirmAll = new UsrPermissionVO();
        permissionApEditConfirmAll.setPermission(Permission.AP_EDIT_CONFIRMED_ALL);

        // create 1st user to assign change state
        UsrUserVO userVO = createUser(apUser.getId(), USR_NAME1, USR_PSWD1);
        addUserPermission(userVO.getId(), Arrays.asList(permissionApRdAll, permissionApConfirmAll));

        // no tasks assigned to the user
        List<WfTask> tasks = wfTaskRepository.findNewByAssigneeId(userVO.getId());
        assertTrue(tasks.isEmpty());

        // prepare data
        ApStateUpdate stateUpdate = new ApStateUpdate();
        stateUpdate.setScopeId(state.getScopeId());
        stateUpdate.setTypeId(state.getApTypeId());
        stateUpdate.setStateApproval(ApStateApproval.TO_APPROVE);

        Integer version = ap.getVersion();

        // change state
        Integer newVersion = accesspointsApi.accessPointChangeState(ap.getId(), stateUpdate, version, userVO.getId());
        assertTrue(newVersion > version);
        version = newVersion;

        // one tasks assigned to the user
        tasks = wfTaskRepository.findNewByAssigneeId(userVO.getId());
        assertTrue(tasks.size() == 1);
        assertTrue(tasks.get(0).getAssigneeId() == userVO.getId());
        assertTrue(tasks.get(0).getStatus().equals(Status.NEW));

        // switch to 1st user
        login(USR_NAME1, USR_PSWD1);

        // to approve
        stateUpdate.setStateApproval(ApStateApproval.APPROVED);
        newVersion = accesspointsApi.accessPointChangeState(ap.getId(), stateUpdate, version, null);
        assertTrue(newVersion > version);
        version = newVersion;

        // check task status (finished)
        tasks = wfTaskRepository.findAllByAssigneeId(userVO.getId());
        assertTrue(tasks.size() == 1);
        assertTrue(tasks.get(0).getClosedById() == userVO.getId());
        assertTrue(tasks.get(0).getStatus().equals(Status.FINISHED));

        // switch to admin
        login(USR_ELZAADMIN, USR_ELZAADMIN_PSWD);

        // --- Test: creating revision cancels existing ApState task and reassigns to same user ---

        // change state to TO_AMEND and assign to usr1 (creates a task on ApState)
        // TO_AMEND is used because it allows both task creation and revision creation
        stateUpdate.setStateApproval(ApStateApproval.TO_AMEND);
        newVersion = accesspointsApi.accessPointChangeState(ap.getId(), stateUpdate, version, userVO.getId());
        assertTrue(newVersion > version);
        version = newVersion;

        // verify task exists for usr1 on ApState
        tasks = wfTaskRepository.findNewByAssigneeId(userVO.getId());
        assertTrue(tasks.size() == 1);
        assertTrue(tasks.get(0).getStatus().equals(Status.NEW));
        int apStateTaskId = tasks.get(0).getTaskId();

        // create revision - should cancel the ApState task and create a new revision task for the same user
        accesspointsApi.accessPointCreateRevision(ap.getId());

        // verify the old ApState task is now CANCELLED
        WfTask cancelledTask = wfTaskRepository.findById(apStateTaskId).orElse(null);
        assertNotNull(cancelledTask);
        assertTrue(cancelledTask.getStatus().equals(Status.CANCELLED));

        // verify a new revision task exists for usr1 (same assignee as cancelled task)
        tasks = wfTaskRepository.findNewByAssigneeId(userVO.getId());
        assertTrue(tasks.size() == 1);
        assertTrue(tasks.get(0).getAssigneeId() == userVO.getId());
        assertTrue(tasks.get(0).getStatus().equals(Status.NEW));
        assertTrue(tasks.get(0).getTaskId() != apStateTaskId); // different task

        // cleanup: delete the revision so we can continue with the rest of the test
        accesspointsApi.accessPointDeleteRevision(ap.getId());
        // to approve
        stateUpdate.setStateApproval(ApStateApproval.APPROVED);
        newVersion = accesspointsApi.accessPointChangeState(ap.getId(), stateUpdate, version, null);
        assertTrue(newVersion > version);
        version = newVersion;

        // create 2nd user to assign change revision state
        userVO = createUser(apUser.getId(), USR_NAME2, USR_PSWD2);
        addUserPermission(userVO.getId(), Arrays.asList(permissionApRdAll, permissionApConfirmAll, permissionApEditConfirmAll));

        // create revision
        accesspointsApi.accessPointCreateRevision(ap.getId());

        // prepare data
        RevStateChange revStateChange = new RevStateChange();
        revStateChange.setTypeId(state.getApTypeId());
        revStateChange.setState(RevisionState.TO_APPROVE);

        // change revision state
        newVersion = accesspointsApi.accessPointChangeStateRevision(ap.getId(), revStateChange, version, userVO.getId());
        assertTrue(newVersion > version);
        version = newVersion;

        // check task status (new) by userId
        tasks = wfTaskRepository.findAllByAssigneeId(userVO.getId());
        assertTrue(tasks.size() == 1);
        assertTrue(tasks.get(0).getAssigneeId() == userVO.getId());
        assertTrue(tasks.get(0).getStatus().equals(Status.NEW));

        // switch to 2st user
        login(USR_NAME2, USR_PSWD2);

        // to merge
        newVersion = accesspointsApi.accessPointMergeRevision(ap.getId(), stateUpdate, version);
        assertTrue(newVersion > version);

        // check task status (finished) by userId
        tasks = wfTaskRepository.findAllByAssigneeId(userVO.getId());
        assertTrue(tasks.size() == 1);
        assertTrue(tasks.get(0).getClosedById() == userVO.getId());
        assertTrue(tasks.get(0).getStatus().equals(Status.FINISHED));
    }

    /**
     * End-to-end test of POST /accesspoint/export.
     *
     * Triggers the async CSV export against the SIMPLE-DEV seed (3 access points), polls the IO
     * status endpoint until FINISHED, downloads the file, and verifies the UTF-8 BOM, header line,
     * ascending {@code accessPointId} order, and that at least one seed UUID appears.
     */
    @Test
    public void batchExportTest() throws IOException, InterruptedException {
        helperTestService.waitForIndexUpdate();

        AccessPointBatchExportParams params = new AccessPointBatchExportParams();
        int requestId = accesspointsApi.accessPointBatchExport(params);
        assertTrue(requestId > 0);

        ExportRequestStatus expStatus = null;
        for (int i = 0; i < 200; i++) {
            Thread.sleep(50);
            expStatus = ioApi.ioGetExportStatus(requestId);
            if (expStatus.getState() == RequestProcessState.FINISHED) {
                break;
            }
        }
        assertNotNull(expStatus);
        assertEquals(RequestProcessState.FINISHED, expStatus.getState());

        Resource file = ioApi.ioGetExportFile(requestId);
        assertNotNull(file);

        byte[] bytes;
        try (InputStream is = file.getInputStream()) {
            bytes = is.readAllBytes();
        }
        assertTrue(bytes.length > 3, "file should contain at least BOM + content");
        assertEquals((byte) 0xEF, bytes[0]);
        assertEquals((byte) 0xBB, bytes[1]);
        assertEquals((byte) 0xBF, bytes[2]);

        String content = new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        String[] lines = content.split("\r?\n");
        assertTrue(lines.length >= 2, "expected header + at least one data row, got " + lines.length);
        assertTrue(lines[0].startsWith("accessPointId,uuid,externalId"),
                "unexpected header: " + lines[0]);

        // At least one seed AP UUID must be present.
        assertTrue(content.contains("9f783015-b9af-42fc-bff4-11ff57cdb072"),
                "expected seed AP UUID in CSV content");

        // Data rows must be sorted by accessPointId ascending.
        int previous = Integer.MIN_VALUE;
        int dataRows = 0;
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.isEmpty()) {
                continue;
            }
            int comma = line.indexOf(',');
            assertTrue(comma > 0, "missing comma in row: " + line);
            int apId = Integer.parseInt(line.substring(0, comma));
            assertTrue(apId > previous, "rows not in ascending order: " + apId + " <= " + previous);
            previous = apId;
            dataRows++;
        }
        assertTrue(dataRows >= 3, "expected at least 3 data rows from seed, got " + dataRows);
    }
}
