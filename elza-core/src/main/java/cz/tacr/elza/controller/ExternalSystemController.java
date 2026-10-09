package cz.tacr.elza.controller;

import java.util.List;

import jakarta.transaction.Transactional;

import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.api.ApExternalSystemType;
import cz.tacr.elza.controller.vo.DaQueueActionResult;
import cz.tacr.elza.controller.vo.DaQueueDirection;
import cz.tacr.elza.controller.vo.DaQueueItemPage;
import cz.tacr.elza.controller.vo.DigitalRepositoryTestResult;
import cz.tacr.elza.controller.vo.QueueItemState;
import cz.tacr.elza.service.da.DaQueueService;
import cz.tacr.elza.controller.vo.ExtSystemProperty;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.SysExternalSystem;
import cz.tacr.elza.domain.SysExternalSystemProperty;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.exception.AccessDeniedException;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.ExternalSystemService;
import cz.tacr.elza.service.UserService;
import cz.tacr.elza.service.dao.FileSystemRepoBrowser;
import cz.tacr.elza.api.DigitalRepositoryType;
import cz.tacr.elza.connector.DaConnector;

@RestController
@RequestMapping("/api/v1")
public class ExternalSystemController implements ExternalsystemsApi {

    @Autowired
    ExternalSystemService extSystemService;

    @Autowired
    UserService userService;

    @Autowired
    FileSystemRepoBrowser fileSystemRepoBrowser;

    @Autowired
    DaConnector daConnector;

    @Autowired
    DaQueueService daQueueService;

    final UsrPermission.Permission reqPermissions[] = { UsrPermission.Permission.ADMIN,
            UsrPermission.Permission.AP_EXTERNAL_WR };

    final UsrPermission.Permission adminPermissions[] = { UsrPermission.Permission.ADMIN };

    @Override
    @Transactional
    public ResponseEntity<Void> externalSystemExternalSystemResync(String id) {
    	ApExternalSystem extSys = extSystemService.findExternalSystemByCodeOrId(id);
    	// pokud systém nebyl nalezen nebo jeho typ neodpovídá CAM_COMPLETE(_V2)
    	if (extSys == null
    			|| (extSys.getType() != ApExternalSystemType.CAM_COMPLETE
    			&& extSys.getType() != ApExternalSystemType.CAM_COMPLETE_V2)) {
    		return ResponseEntity.notFound().build();
    	}
    	extSystemService.deleteBindingSync(extSys);

    	return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<DaQueueItemPage> externalSystemDaQueue(Integer id, Boolean all, List<QueueItemState> states,
                                                                 DaQueueDirection direction, String aipCode,
                                                                 String batchId, Boolean failedOnly,
                                                                 Integer from, Integer count) {
        DaQueueService.Filter filter = new DaQueueService.Filter(Boolean.TRUE.equals(all), states, direction,
                                                                 aipCode, batchId, Boolean.TRUE.equals(failedOnly));
        return ResponseEntity.ok(daQueueService.find(id, filter, from == null ? 0 : from, count == null ? 50 : count));
    }

    @Override
    public ResponseEntity<DaQueueActionResult> externalSystemDaQueueRetryNow(Integer id, List<Integer> itemIds) {
        return ResponseEntity.ok(daQueueService.retryNow(id, itemIds));
    }

    @Override
    public ResponseEntity<DaQueueActionResult> externalSystemDaQueueWithdraw(Integer id, List<Integer> itemIds) {
        return ResponseEntity.ok(daQueueService.withdraw(id, itemIds));
    }

    @Override
    public ResponseEntity<DaQueueActionResult> externalSystemDaQueueRepeat(Integer id, List<Integer> itemIds) {
        return ResponseEntity.ok(daQueueService.repeat(id, itemIds));
    }

    @Override
    @Transactional
    public ResponseEntity<DigitalRepositoryTestResult> externalSystemTestDigitalRepository(Integer id) {
        UserDetail loggedDetail = userService.getLoggedUserDetail();
        if (loggedDetail == null || !loggedDetail.hasPermission(UsrPermission.Permission.ADMIN)) {
            throw new AccessDeniedException("Only admin can test repository configuration.", adminPermissions);
        }

        SysExternalSystem extSystem = extSystemService.findExternalSystemById(id);
        if (!(extSystem instanceof ArrDigitalRepository)) {
            return ResponseEntity.notFound().build();
        }

        ArrDigitalRepository repository = (ArrDigitalRepository) extSystem;
        if (repository.getDigitalRepositoryType() == DigitalRepositoryType.DA) {
            return ResponseEntity.ok(daConnector.testRepository(repository));
        }
        return ResponseEntity.ok(fileSystemRepoBrowser.testRepository(repository));
    }

    @Override
    @Transactional
    public ResponseEntity<List<ExtSystemProperty>> externalSystemAllProperties(Integer extSystemId, Integer userId) {

        UserDetail loggedDetail = userService.getLoggedUserDetail();

        if (loggedDetail == null) {
            throw new AccessDeniedException("Not logged", reqPermissions);
        }
        if (!loggedDetail.hasPermission(UsrPermission.Permission.ADMIN)) {
            if (userId == null) {
                // without admin perms only own properties might be listed
                userId = loggedDetail.getId();
            } else if (!userId.equals(loggedDetail.getId())) {
                throw new AccessDeniedException("User can list properties only for himself.", reqPermissions);
            }
        }

        // List<ExtSystemProperty> properties = extSystemService.findAllProperties(extSystemId, userId);
        List<ExtSystemProperty> properties = extSystemService.findUserProperties(extSystemId, userId);

        return ResponseEntity.ok(properties);
    }

    @Override
    @Transactional
    public ResponseEntity<Void> externalSystemStoreProperties(List<ExtSystemProperty> extSystemProperties) {
        UserDetail loggedDetail = userService.getLoggedUserDetail();

        if (loggedDetail == null) {
            throw new AccessDeniedException("Not logged", reqPermissions);
        }

        Validate.notNull(extSystemProperties, "ExtSystemProperty shouldn't be null");

        boolean isAdmin = loggedDetail.hasPermission(UsrPermission.Permission.ADMIN);

        for (ExtSystemProperty extSystemProperty : extSystemProperties) {
            Validate.notNull(extSystemProperty.getExtSystemId(),
                             "ExtSystemProperty.externalSystemId shouldn't be null");
            Validate.notNull(extSystemProperty.getName(), "ExtSystemProperty.name shouldn't be null");
            Validate.notNull(extSystemProperty.getValue(), "ExtSystemProperty.value shouldn't be null");

            SysExternalSystem extSystem = extSystemService.findExternalSystemById(extSystemProperty.getExtSystemId());
            UsrUser user = userService.getUserInternal(extSystemProperty.getUserId());

            if (!isAdmin) {
                // without admin perms only own properties might be set
                if (extSystemProperty.getUserId() == null ||
                        !extSystemProperty.getUserId().equals(loggedDetail.getId())) {
                    throw new AccessDeniedException("User can set properties only for himself.", reqPermissions);
                }
            }

            extSystemService.storeProperty(extSystem, user, extSystemProperty);
        }


        return ResponseEntity.ok().build();
    }

    @Override
    @Transactional
    public ResponseEntity<Void> externalSystemDeleteProperties(List<Integer> extSysPropertyIds) {
        UserDetail loggedDetail = userService.getLoggedUserDetail();

        if (loggedDetail == null) {
            throw new AccessDeniedException("Not logged", reqPermissions);
        }
        
        boolean isAdmin = loggedDetail.hasPermission(UsrPermission.Permission.ADMIN);

        for (Integer extSysPropertyId : extSysPropertyIds) {

            SysExternalSystemProperty dbProp = extSystemService.getProperty(extSysPropertyId);

            if (!isAdmin) {
                // without admin perms only own properties might be deleted
                if (dbProp.getUserId() == null ||
                        !dbProp.getUserId().equals(loggedDetail.getId())) {
                    throw new AccessDeniedException("User has no permissions to delete this property.", reqPermissions);
                }
            }

            extSystemService.deleteProperty(extSysPropertyId);
        }

        return ResponseEntity.ok().build();
    }
}
