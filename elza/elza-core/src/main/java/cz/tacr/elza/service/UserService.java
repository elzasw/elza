package cz.tacr.elza.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.annotation.Nullable;

import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.scrypt.SCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;

import cz.tacr.elza.common.ObjectListIterator;
import cz.tacr.elza.controller.vo.UserInfoVO;
import cz.tacr.elza.core.data.SearchType;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.core.security.AuthParam;
import cz.tacr.elza.core.security.Authorization;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.UsrAuthentication;
import cz.tacr.elza.domain.UsrGroup;
import cz.tacr.elza.domain.UsrGroupUser;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.domain.WfIssueList;
import cz.tacr.elza.exception.AccessDeniedException;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.Level;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.RegistryCode;
import cz.tacr.elza.exception.codes.UserCode;
import cz.tacr.elza.repository.AuthenticationRepository;
import cz.tacr.elza.repository.FilteredResult;
import cz.tacr.elza.repository.FundRepository;
import cz.tacr.elza.repository.GroupRepository;
import cz.tacr.elza.repository.GroupUserRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.PermissionRepository;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.repository.UserRepository;
import cz.tacr.elza.repository.WfIssueListRepository;
import cz.tacr.elza.security.AuthorizationRequest;
import cz.tacr.elza.security.Sha256Support;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.security.UserPermission;
import cz.tacr.elza.service.eventnotification.events.EventId;
import cz.tacr.elza.service.eventnotification.events.EventType;

/**
 * Service to check and manage user permissions
 *
 */
@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    static {
        Map<String, PasswordEncoder> encoders = new HashMap<>();
        encoders.put("bcrypt", new BCryptPasswordEncoder());
        encoders.put("scrypt", SCryptPasswordEncoder.defaultsForSpringSecurity_v5_8());
        encoder = new DelegatingPasswordEncoder("bcrypt", encoders);
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private GroupUserRepository groupUserRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private AccessPointService accessPointService;

    @Autowired
    private IEventNotificationService eventNotificationService;

    @Autowired
    private FundRepository fundRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private ScopeRepository scopeRepository;

    @Autowired
    private WfIssueListRepository issueListRepository;

    @Autowired
    LevelTreeCacheService levelTreeCacheService;

    @Autowired
    private AuthenticationRepository authenticationRepository;

    @Value("${elza.security.salt:kdFss=+4Df_%}")
    private String SALT;

    private static final PasswordEncoder encoder;

    @Value("${elza.security.defaultUsername:admin}")
    private String defaultUsername;

    @Value("${elza.security.defaultPassword:{bcrypt}$2a$10$blaAW9EHjsDVpLv1DJSTIuIfgyrF0uxlIVPV2wcPPdTSFLqDpoAMa}")
    private String defaultPassword;

    @Value("${elza.security.allowDefaultUser:true}")
    private Boolean allowDefaultUser;

    /**
     * Cache pro nakešování oprávnění uživatele.
     */
    private final LoadingCache<Integer, Collection<UserPermission>> userPermissionsCache;

	/**
	 * Seznam oprávnění, které se mají nastavit při vytváření AS a přiřazení
	 * uživatele nebo skupiny jako správce.
	 */
	private static final UsrPermission.Permission FUND_ADMIN_PERMISSIONS[] = {
	        UsrPermission.Permission.FUND_RD,
	        UsrPermission.Permission.FUND_ARR,
	        UsrPermission.Permission.FUND_OUTPUT_WR,
	        UsrPermission.Permission.FUND_CL_VER_WR,
	        UsrPermission.Permission.FUND_EXPORT,
	        UsrPermission.Permission.FUND_BA,
	        UsrPermission.Permission.FUND_VER_WR,
            UsrPermission.Permission.FUND_ISSUE_ADMIN,
	};

    public static class ChangedPermissionResult {
        List<UsrPermission> permissionsAdd = new ArrayList<>();
        List<UsrPermission> permissionsUpdate = new ArrayList<>();
        List<UsrPermission> permissionsNoChange = new ArrayList<>();
        List<UsrPermission> permissionsDelete = new ArrayList<>();

        public void addPermission(UsrPermission permission) {
            permissionsAdd.add(permission);
        }

        public List<UsrPermission> getNewPermissions() {
            return permissionsAdd;
        }

        /**
         * Return if permissions were changed and updated.
         * 
         * @return
         */
        public boolean isPermissionsChanged() {
            return (permissionsAdd.size() > 0 || permissionsUpdate.size() > 0 || permissionsDelete.size() > 0);
        }

        /**
         * Method will check if some of new permission does not already exist or is
         * scheduled for
         * removal.
         */
        public void optimize() {
            logger.debug("Optimizing permissions.");
            boolean modified;
            do {
                modified = false;

                for (UsrPermission permAdd : permissionsAdd) {
                    // check if same exists in unchanged
                    UsrPermission existingPerm = permissionExists(permAdd, permissionsNoChange);
                    if (existingPerm != null) {
                        logger.debug("Optimize permissions: Found same existing permission, type: {}, id: {}",
                                     permAdd.getPermission(), existingPerm.getPermissionId());
                        permissionsAdd.remove(permAdd);
                        modified = true;
                        break;
                    }
                    // check if same exists in changed
                    existingPerm = permissionExists(permAdd, permissionsUpdate);
                    if (existingPerm != null) {
                        logger.debug("Optimize permissions: Found same permission marked for update, type: {}, id: {}",
                                     permAdd.getPermission(), existingPerm.getPermissionId());
                        permissionsAdd.remove(permAdd);
                        modified = true;
                        break;
                    }
                    // check if same exists in deleted
                    UsrPermission permDelete = permissionExists(permAdd, permissionsDelete);
                    if (permDelete != null) {
                        logger.debug("Optimize permissions: Found same permission marked for delete, type: {}, id: {}",
                                     permAdd.getPermission(), permDelete.getPermissionId());
                        // remove both permissions
                        permissionsAdd.remove(permAdd);
                        permissionsDelete.remove(permDelete);
                        permissionsNoChange.add(permDelete);
                        modified = true;
                        break;
                    }
                }
            } while (modified);

        }

        private UsrPermission permissionExists(UsrPermission perm, List<UsrPermission> list) {
            for (UsrPermission p : list) {
                if (p.isSamePermission(perm)) {
                    return p;
                }
            }
            return null;
        }

    }

    public UserService() {
        userPermissionsCache = CacheBuilder.newBuilder()
                .maximumSize(150)
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .build(new CacheLoader<Integer, Collection<UserPermission>>() {
                    @Override
                    public Collection<UserPermission> load(final Integer userId) {
                        final UsrUser user = getUser(userId);
                        return calcUserPermission(user);
                    }
                });
    }

    /**
     * Vyhledá AS na které jsou vázaná nějaká oprávnění.
     *
     * @param search      hledané řetězec
     * @param firstResult od jakého záznamu
     * @param maxResults  maximální počet vrácených záznamů
     * @return výsledek
     */
    public FilteredResult<ArrFund> findFundsWithPermissions(final String search,
                                                            final Integer firstResult,
                                                            final Integer maxResults) {
		UserDetail userDetail = getLoggedUserDetail();

		if (userDetail.hasPermission(UsrPermission.Permission.USR_PERM)) {
			// nefiltruje se dle přiřazených oprávnění, vrací všechny AS
			return fundRepository.findFunds(search, null, firstResult, maxResults);
		} else {
			// filtruje se dle přiřazeníé oprávnění na AS pro daného uživatele
			return fundRepository.findFundsWithPermissions(search, null, firstResult, maxResults, userDetail.getId());
		}
    }

    private enum ChangePermissionType {
        SYNCHRONIZE,
        ADD,
        DELETE
    }

    /**
     * Update DB permisssion according received permissions
     * 
     * @param permission
     * @param permissionDB
     * @param result
     */
    private void updatePermission(UsrPermission permission, UsrPermission permissionDB,
                                  ChangedPermissionResult result) {
        boolean modified = false;
        if (permissionDB.getPermission() != permission.getPermission()) {
            permissionDB.setPermission(permission.getPermission());
            modified = true;
        }
        if (setNodeRelation(permissionDB, permission.getNodeId(), permission.getFundId())) {
            modified = true;
        }
        if (setFundRelation(permissionDB, permission.getFundId())) {
            modified = true;
        }
        if (setScopeRelation(permissionDB, permission.getScopeId())) {
            modified = true;
        }
        if (setControlUserRelation(permissionDB, permission.getUserControlId())) {
            modified = true;
        }
        if (setControlGroupRelation(permissionDB, permission.getGroupControlId())) {
            modified = true;
        }
        if (setIssueListRelation(permission, permission.getIssueListId())) {
            modified = true;
        }

        if (modified) {
            result.permissionsUpdate.add(permissionDB);
        } else {
            result.permissionsNoChange.add(permissionDB);
        }
    }

    private void addPermission(UsrUser user, UsrGroup group,
                               UsrPermission permission, ChangedPermissionResult result) {
        permission.setUser(user);
        permission.setGroup(group);
        setNodeRelation(permission, permission.getNodeId(), permission.getFundId());
        setFundRelation(permission, permission.getFundId());
        setScopeRelation(permission, permission.getScopeId());
        setControlUserRelation(permission, permission.getUserControlId());
        setControlGroupRelation(permission, permission.getGroupControlId());
        setIssueListRelation(permission, permission.getIssueListId());
        result.addPermission(permission);
    }

    private void changePermission(ChangedPermissionResult result, UsrUser user, UsrGroup group,
                                  @NotNull ChangePermissionType changePermissionType,
                                  UsrPermission permission,
                                  Map<Integer, UsrPermission> permissionMap) {

        // pokud se jedná o pokus o přidělení práv superuživatele
        if (permission.getPermission().equals(UsrPermission.Permission.ADMIN)) {
            if (!hasPermission(Permission.ADMIN)) {
                throw new BusinessException("Přístup superuživatele může udělit pouze superuživatel",
                        BaseCode.INSUFFICIENT_PERMISSIONS);
            }
        }

        switch (changePermissionType) {
        case ADD:
            // pro akci add nelze předat vyplněné id
            if (permission.getPermissionId() != null) {
                throw new SystemException("V akci add nelze předat oprávnění s vyplněným id",
                        UserCode.PERM_ILLEGAL_INPUT);
            }
            addPermission(user, group, permission, result);
            break;
        case DELETE:
            if (permission.getPermissionId() == null) {
                //pokud se jedná o akci delete, nesmí být předán záznam bez id
                if (changePermissionType == ChangePermissionType.DELETE) {
                    throw new SystemException("V akci delete nelze předat oprávnění s nevyplněným id",
                            UserCode.PERM_ILLEGAL_INPUT);
                }
            } else {
                UsrPermission permissionDB = permissionMap.remove(permission.getPermissionId());
                if (permissionDB == null) {
                    throw new SystemException("Oprávnění neexistuje a proto nemůže být upraveno",
                            UserCode.PERM_NOT_EXIST);
                }
                result.permissionsDelete.add(permissionDB);
            }
            break;
        case SYNCHRONIZE:
            // jen zde přidáváme, jinak se jedná o akci delete
            if (permission.getPermissionId() != null) {
                UsrPermission permissionDB = permissionMap.remove(permission.getPermissionId());
                if (permissionDB == null) {
                    throw new SystemException("Oprávnění neexistuje a proto nemůže být upraveno",
                            UserCode.PERM_NOT_EXIST);
                }
                updatePermission(permission, permissionDB, result);
            } else {
                // new permission can be added
                addPermission(user, group, permission, result);
            }
            break;
        default:
            throw new IllegalStateException("Nepodporovaný typ změny oprávění: " + changePermissionType);
        }
    }

    /**
     * Update user permissions
     * 
     * @param user
     * @param group
     * @param permissions
     * @param changePermissionType
     * @param checkPermission
     * @return Return number of modified DB permissions
     */
    private ChangedPermissionResult changePermission(final UsrUser user,
                                                 final UsrGroup group,
                                                 final @NotNull List<UsrPermission> permissions,
                                                 final @NotNull ChangePermissionType changePermissionType,
                                                 final boolean checkPermission) {
        Validate.isTrue(user != null ^ group != null);

        if (logger.isDebugEnabled()) {
            logger.debug("changePermission, user: {}, group: {}, type: {}, count: {}",
                         user != null ? user.getUserId() : null,
                         group != null ? group.getGroupId() : null,
                         changePermissionType,
                         permissions.size());
        }

        List<UsrPermission> permissionsDB = (user != null) ? permissionRepository.findByUserOrderByPermissionIdAsc(user)
                : permissionRepository.findByGroupOrderByPermissionIdAsc(group);
        Map<Integer, UsrPermission> permissionMap = permissionsDB.stream()
                .collect(Collectors.toMap(UsrPermission::getPermissionId, Function.identity()));

        ChangedPermissionResult result = new ChangedPermissionResult();

        if (checkPermission) {
            checkPermission(permissions);
        }


        for (UsrPermission permission : permissions) {
            // applied permissions will be removed from permissionMap
            changePermission(result, user, group, changePermissionType, permission, permissionMap);
        }

        // Remaining permissions should be deleted in sync 
        if (changePermissionType == ChangePermissionType.SYNCHRONIZE) {
            result.permissionsDelete.addAll(permissionMap.values());
        }

        result.optimize();

        for (UsrPermission permission : permissions) {
            validatePermission(permission);
        }

        // Collect results
        if (CollectionUtils.isNotEmpty(result.permissionsDelete)) {
            permissionRepository.deleteAll(result.permissionsDelete);
        }
        if (CollectionUtils.isNotEmpty(result.permissionsAdd)) {
            result.permissionsAdd = permissionRepository.saveAll(result.permissionsAdd);
        }
        if (CollectionUtils.isNotEmpty(result.permissionsUpdate)) {
            result.permissionsUpdate = permissionRepository.saveAll(result.permissionsUpdate);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("changePermission - results, user: {}, group: {}, type: {}, permissions: [new: {}, deleted: {}, updated: {}, unchanged: {}]",
                         user != null ? user.getUserId() : null,
                         group != null ? group.getGroupId() : null,
                         changePermissionType,
                         result.permissionsAdd.size(),
                         result.permissionsDelete.size(),
                         result.permissionsUpdate.size(),
                         result.permissionsNoChange.size());
        }
        return result;
    }

    /**
     * Validate permission object
     * 
     * @param permission
     */
    private void validatePermission(UsrPermission permission) {
        switch (permission.getPermission().getType()) {
        case ALL: {
            if (permission.getScopeId() != null || permission.getFundId() != null
                    || permission.getUserControlId() != null || permission.getGroupControlId() != null
                    || permission.getIssueListId() != null || permission.getNodeId() != null) {
                throw new SystemException("Neplatný vstup oprávnění: ALL", UserCode.PERM_ILLEGAL_INPUT).set("type",
                                                                                                            "ALL");
            }
            break;
        }
        case SCOPE: {
            if (permission.getScopeId() == null || permission.getFundId() != null
                    || permission.getUserControlId() != null || permission.getGroupControlId() != null
                    || permission.getIssueListId() != null || permission.getNodeId() != null) {
                throw new SystemException("Neplatný vstup oprávnění: SCOPE", UserCode.PERM_ILLEGAL_INPUT).set("type",
                                                                                                              "SCOPE");
            }
            break;
        }
        case FUND: {
            if (permission.getScopeId() != null || permission.getFundId() == null
                    || permission.getUserControlId() != null || permission.getGroupControlId() != null
                    || permission.getIssueListId() != null || permission.getNodeId() != null) {
                throw new SystemException("Neplatný vstup oprávnění: FUND", UserCode.PERM_ILLEGAL_INPUT).set("type",
                                                                                                             "FUND");
            }
            break;
        }
        case NODE: {
            if (permission.getScopeId() != null || permission.getFundId() == null
                    || permission.getUserControlId() != null || permission.getGroupControlId() != null
                    || permission.getIssueListId() != null || permission.getNodeId() == null) {
                throw new SystemException("Neplatný vstup oprávnění: NODE", UserCode.PERM_ILLEGAL_INPUT).set("type",
                                                                                                             "NODE");
            }
            break;
        }
        case USER:
            if (permission.getScopeId() != null || permission.getFundId() != null
                    || permission.getUserControlId() == null || permission.getGroupControlId() != null
                    || permission.getIssueListId() != null) {
                throw new SystemException("Neplatný vstup oprávnění: USER", UserCode.PERM_ILLEGAL_INPUT).set("type",
                                                                                                             "USER");
            }
            break;
        case GROUP:
            if (/*permission.getScopeId() != null || permission.getFundId() != null || permission.getUserControlId() != null || permission.getGroupControlId() != null || permission.getIssueListId() != null) {*/
            permission.getGroupControlId() == null) {
                throw new SystemException("Neplatný vstup oprávnění: GROUP", UserCode.PERM_ILLEGAL_INPUT).set("type",
                                                                                                              "GROUP");
            }
            break;
        case ISSUE_LIST: {
            if (permission.getScopeId() != null || permission.getFundId() != null
                    || permission.getUserControlId() != null || permission.getGroupControlId() != null
                    || permission.getIssueListId() == null || permission.getNodeId() != null) {
                throw new SystemException("Neplatný vstup oprávnění: ISSUE_LIST", UserCode.PERM_ILLEGAL_INPUT)
                        .set("type", "ISSUE_LIST");
            }
            break;
        }
        default:
            throw new IllegalStateException("Nedefinovaný typ oprávnění");
        }
    }

    /**
     * Kontrola, že s těmito oprávněními může přihlášený uživatel operovat (tzn. má je také).
     * Pokud uživatel má oprávnění {@link UsrPermission.Permission#ADMIN} nebo
     * {@link UsrPermission.Permission#USR_PERM}, může měnit cokoliv.
     *
     * @param permissions kontrolovaná oprávnění
     */
    private void checkPermission(final List<UsrPermission> permissions) {
        UserDetail userDetail = getLoggedUserDetail();
        if(userDetail.hasPermission(UsrPermission.Permission.USR_PERM))
            return;

        // Check new permissions
        for (UsrPermission usrPermission : permissions) {
            if(!userDetail.hasPermission(usrPermission)) {
                throw Authorization.createAccessDeniedException(usrPermission.getPermission())
                    .level(Level.WARNING);
            }
        }

    }

    /**
     * Nastaví vazbu na soubor, pokud je předané id. Pokud předané není, je vazba
     * odstraněna.
     * Kontroluje existenci objektu s daným id.
     *
     * @param permission
     *            oprávnění
     * @param fundId
     *            id objektu, na který má být přidána vazba
     * @return Return true if permission object was modified.
     *         Return false if modification was not done.
     */
    private boolean setFundRelation(final UsrPermission permission, final Integer fundId) {
        if (fundId != null) {
            if (permission.getFund() != null && fundId.equals(permission.getFundId())) {
                return false;
            }
            ArrFund fund = fundRepository.findById(fundId)
                    .orElseThrow(() -> new SystemException("Neplatný archivní soubor", ArrangementCode.FUND_NOT_FOUND).set("id", fundId));
            permission.setFund(fund);
        } else {
            if (permission.getFund() == null) {
                return false;
            }
            permission.setFund(null);
        }
        return true;
    }

    /**
     * Nastaví vazbu na JP, pokud je předané id. Pokud předané není, je vazba
     * odstraněna.
     * Kontroluje existenci objektu s daným id.
     *
     * @param permission
     *            oprávnění
     * @param nodeId
     *            id objektu, na který má být přidána vazba
     * @param fundId
     *            id AS, který vztahuje k JP
     * @return Return true if permission object was modified.
     *         Return false if modification was not done.
     */
    private boolean setNodeRelation(final UsrPermission permission, final Integer nodeId, final Integer fundId) {
        if (nodeId != null) {
            if(permission.getNode()!=null&&nodeId.equals(permission.getNodeId())) {
                return false;
            }
            ArrNode node = nodeRepository.findById(nodeId)
                    .orElseThrow(() -> new SystemException("Neplatná JP", ArrangementCode.NODE_NOT_FOUND).set("id", nodeId));
            if (!node.getFundId().equals(fundId)) {
                throw new SystemException("Neplatná JP v závislosti k AS", ArrangementCode.NODE_NOT_FOUND)
                        .set("id", nodeId)
                        .set("fundId", fundId);
            }
            permission.setNode(node);
        } else {
            if (permission.getNodeId() == null) {
                return false;
            }
            permission.setNode(null);
        }
        return true;
    }

    /**
     * Smazání všech oprávnění s vazbou na JP.
     *
     * @param nodeIds identifikátory JP
     */
    @Transactional(Transactional.TxType.MANDATORY)
    public void deletePermissionByNodeIds(final Collection<Integer> nodeIds) {
        if (CollectionUtils.isEmpty(nodeIds)) {
            return;
        }

        // naleznu všechny oprávnění
        List<List<Integer>> nodeIdsLists = Lists.partition(new ArrayList<>(nodeIds),
                                                           ObjectListIterator.getMaxBatchSize());
        List<UsrPermission> permissions = new ArrayList<>();
        for (List<Integer> subNodeIds : nodeIdsLists) {
            permissions.addAll(permissionRepository.findByNodeIds(subNodeIds));
        }

        // vyhledám všechny uživatele, u kterých se budou oprávnění mazat
        Set<UsrUser> users = new HashSet<>();
        for (UsrPermission permission : permissions) {
            users.add(permission.getUser());
        }

        // invaliduji jejich cache s oprávněním
        for (UsrUser user : users) {
            invalidateCache(user);
        }

        // nakonec smažu všechny oprávnění s vazbou na JP
        permissionRepository.deleteAll(permissions);
    }

    /**
     * Nastaví vazbu na scope, pokud je předané id. Pokud předané není, je vazba
     * odstraněna.
     * Kontroluje existenci objektu s daným id.
     *
     * @param permission
     *            oprávnění
     * @param scopeId
     *            id objektu, na který má být přidána vazba
     * @return Return true if permission object was modified.
     *         Return false if modification was not done.
     */
    private boolean setScopeRelation(final UsrPermission permission, final Integer scopeId) {
        if (scopeId != null) {
            if (permission.getScope() != null && scopeId.equals(permission.getScopeId())) {
                return false;
            }
            ApScope scope = scopeRepository.findById(scopeId)
                    .orElseThrow(() -> new SystemException("Neplatný scope", BaseCode.ID_NOT_EXIST));
            permission.setScope(scope);
        } else {
            if (permission.getScope() == null) {
                return false;
            }
            permission.setScope(null);
        }

        return true;
    }

    /**
     * Nastaví vazbu na uživatele - spravovaná etita, pokud je předané id. Pokud
     * předané není, je vazba odstraněna.
     * Kontroluje existenci objektu s daným id.
     *
     * @param permission
     *            oprávnění
     * @param userId
     *            id objektu, na který má být přidána vazba
     * @return Return true if permission object was modified.
     *         Return false if modification was not done.
     */
    private boolean setControlUserRelation(final UsrPermission permission, final Integer userId) {
        if (userId != null) {
            if (permission.getUserControl() != null && userId.equals(permission.getUserControlId())) {
                return false;
            }

            UsrUser user = userRepository.findById(userId)
                    .orElseThrow(() -> new SystemException("Neplatný uživatel", BaseCode.ID_NOT_EXIST));
            permission.setUserControl(user);
        } else {
            if (permission.getUserControl() == null) {
                return false;
            }
            permission.setUserControl(null);
        }
        return true;
    }

    /**
     * Nastaví vazbu na skupinu - spravovaná etita, pokud je předané id. Pokud
     * předané není, je vazba odstraněna.
     * Kontroluje existenci objektu s daným id.
     *
     * @param permission
     *            oprávnění
     * @param groupId
     *            id objektu, na který má být přidána vazba
     * @return Return true if permission object was modified.
     *         Return false if modification was not done.
     */
    private boolean setControlGroupRelation(final UsrPermission permission, final Integer groupId) {

        if (groupId != null) {
            if (permission.getGroupControl() != null && groupId.equals(permission.getGroupControlId())) {
                return false;
            }

            UsrGroup group = groupRepository.findById(groupId)
                    .orElseThrow(() -> new SystemException("Neplatná skupina", BaseCode.ID_NOT_EXIST));
            permission.setGroupControl(group);
        } else {
            if (permission.getGroupControl() == null) {
                return false;
            }
            permission.setGroupControl(null);
        }
        return true;
    }

    /**
     * Nastaví vazbu na protokol, pokud je předané id. Pokud předané není, je vazba
     * odstraněna.
     * Kontroluje existenci objektu s daným id.
     *
     * @param permission
     *            oprávnění
     * @param issueListId
     *            id objektu, na který má být přidána vazba
     * @return Return true if permission object was modified.
     *         Return false if modification was not done.
     */
    private boolean setIssueListRelation(final UsrPermission permission, final Integer issueListId) {
        if (issueListId != null) {
            if (permission.getIssueList() != null && issueListId.equals(permission.getIssueListId())) {
                return false;
            }
            WfIssueList issueList = issueListRepository.findById(issueListId)
                    .orElseThrow(() -> new SystemException("Neplatný protokol", BaseCode.ID_NOT_EXIST));
            permission.setIssueList(issueList);
        } else {
            if (permission.getIssueList() == null) {
                return false;
            }
            permission.setIssueList(null);
        }
        return true;
    }

    @AuthMethod(permission = {UsrPermission.Permission.USR_PERM, UsrPermission.Permission.GROUP_CONTROL_ENTITITY})
    public void changeGroupPermission(@AuthParam(type = AuthParam.Type.GROUP) @NotNull final UsrGroup group,
                                      @NotNull final List<UsrPermission> permissions) {
        ChangedPermissionResult result = changePermission(null, group, permissions, ChangePermissionType.SYNCHRONIZE,
                                                          true);
        if (result.isPermissionsChanged()) {
            changeGroupEvent(group);
        }
    }

    @AuthMethod(permission = {UsrPermission.Permission.USR_PERM, UsrPermission.Permission.USER_CONTROL_ENTITITY})
    public void changeUserPermission(@AuthParam(type = AuthParam.Type.USER) @NotNull final UsrUser user,
                                     @NotNull final List<UsrPermission> permissions) {
        ChangedPermissionResult result = changePermission(user, null, permissions, ChangePermissionType.SYNCHRONIZE,
                                                          true);
        if (result.isPermissionsChanged()) {
            invalidateCache(user);
            changeUserEvent(user);
        }
    }

    @AuthMethod(permission = {UsrPermission.Permission.USR_PERM, UsrPermission.Permission.USER_CONTROL_ENTITITY})
    public ChangedPermissionResult addUserPermission(@AuthParam(type = AuthParam.Type.USER) @NotNull final UsrUser user,
                                                 @NotNull final List<UsrPermission> permissions, final boolean checkPermission) {
        ChangedPermissionResult result = changePermission(user, null, permissions, ChangePermissionType.ADD,
                                                      checkPermission);
        if (result.isPermissionsChanged()) {
            invalidateCache(user);
            changeUserEvent(user);
        }

        return result;
    }

    @AuthMethod(permission = {UsrPermission.Permission.USR_PERM, UsrPermission.Permission.GROUP_CONTROL_ENTITITY})
    public ChangedPermissionResult addGroupPermission(@AuthParam(type = AuthParam.Type.GROUP) @NotNull final UsrGroup group,
                                                  @NotNull final List<UsrPermission> permissions, final boolean checkPermission) {
        ChangedPermissionResult result = changePermission(null, group, permissions, ChangePermissionType.ADD,
                                                          checkPermission);
        if (result.isPermissionsChanged()) {
            invalidateCache(group);
            changeGroupEvent(group);
        }
        return result;
    }

    @AuthMethod(permission = {UsrPermission.Permission.USR_PERM, UsrPermission.Permission.USER_CONTROL_ENTITITY})
    public void deleteUserPermission(@AuthParam(type = AuthParam.Type.USER) @NotNull final UsrUser user,
                                     @NotNull final List<UsrPermission> permissions) {
        ChangedPermissionResult result = changePermission(user, null, permissions, ChangePermissionType.DELETE, true);
        if (result.isPermissionsChanged()) {
            invalidateCache(user);
            changeUserEvent(user);
        }
    }

    @AuthMethod(permission = {UsrPermission.Permission.USR_PERM, UsrPermission.Permission.GROUP_CONTROL_ENTITITY})
    public void deleteGroupPermission(@AuthParam(type = AuthParam.Type.GROUP) @NotNull final UsrGroup group,
                                      @NotNull final List<UsrPermission> permissions) {
        ChangedPermissionResult result = changePermission(null, group, permissions, ChangePermissionType.DELETE, true);
        if (result.isPermissionsChanged()) {
            invalidateCache(group);
            changeGroupEvent(group);
        }
    }

    /**
     * Smaže všechna oprávnení uživatele na daný AS.
     *
     * @param user   uživatel
     * @param fundId id AS
     */
    @AuthMethod(permission = {UsrPermission.Permission.USR_PERM, UsrPermission.Permission.USER_CONTROL_ENTITITY})
    public void deleteUserFundPermissions(@AuthParam(type = AuthParam.Type.USER) @NotNull final UsrUser user,
                                          @NotNull final Integer fundId) {
        List<UsrPermission> permissionsDB = permissionRepository.findByUserOrderByPermissionIdAsc(user);
        List<UsrPermission> permissionsToDelete = permissionsDB.stream()
                .filter(x -> fundId.equals(x.getFundId()))
                .collect(Collectors.toList());
        deleteUserPermission(user, permissionsToDelete);
    }

    /**
     * Smaže všechna oprávnení uživatele typu AS all.
     *
     * @param user   uživatel
     */
    @AuthMethod(permission = {UsrPermission.Permission.USR_PERM, UsrPermission.Permission.USER_CONTROL_ENTITITY})
    public void deleteUserFundAllPermissions(@AuthParam(type = AuthParam.Type.USER) @NotNull final UsrUser user) {
        List<UsrPermission> permissionsDB = permissionRepository.findByUserOrderByPermissionIdAsc(user);
        List<UsrPermission> permissionsToDelete = permissionsDB.stream()
                .filter(x -> UsrPermission.Permission.getFundAllPerms().contains(x.getPermission()))
                .collect(Collectors.toList());
        deleteUserPermission(user, permissionsToDelete);
    }

    /**
     * Smaže všechna oprávnení skupiny na daný AS.
     *
     * @param group  skupina
     * @param fundId id AS
     */
    @AuthMethod(permission = {UsrPermission.Permission.USR_PERM, UsrPermission.Permission.GROUP_CONTROL_ENTITITY})
    public void deleteGroupFundPermissions(@AuthParam(type = AuthParam.Type.GROUP) @NotNull final UsrGroup group,
                                           @NotNull final Integer fundId) {
        List<UsrPermission> permissionsDB = permissionRepository.findByGroupOrderByPermissionIdAsc(group);
        List<UsrPermission> permissionsToDelete = permissionsDB.stream()
                .filter(x -> fundId.equals(x.getFundId()))
                .collect(Collectors.toList());
        deleteGroupPermission(group, permissionsToDelete);
    }

    /**
     * Smaže všechna oprávnení skupiny typu AS all.
     *
     * @param group  skupina
     */
    @AuthMethod(permission = {UsrPermission.Permission.USR_PERM, UsrPermission.Permission.GROUP_CONTROL_ENTITITY})
    public void deleteGroupFundAllPermissions(@AuthParam(type = AuthParam.Type.GROUP) @NotNull final UsrGroup group) {
        List<UsrPermission> permissionsDB = permissionRepository.findByGroupOrderByPermissionIdAsc(group);
        List<UsrPermission> permissionsToDelete = permissionsDB.stream()
                .filter(x -> UsrPermission.Permission.getFundAllPerms().contains(x.getPermission()))
                .collect(Collectors.toList());
        deleteGroupPermission(group, permissionsToDelete);
    }

    /**
     * Smaže všechna oprávnení uživatel na daný typ rejstříku.
     *
     * @param user    uživatel
     * @param scopeId id typu rejstříku
     */
    @AuthMethod(permission = {UsrPermission.Permission.USR_PERM, UsrPermission.Permission.USER_CONTROL_ENTITITY})
    public void deleteUserScopePermissions(@AuthParam(type = AuthParam.Type.USER) @NotNull final UsrUser user,
                                           @NotNull final Integer scopeId) {
        List<UsrPermission> permissionsDB = permissionRepository.findByUserOrderByPermissionIdAsc(user);
        List<UsrPermission> permissionsToDelete = permissionsDB.stream()
                .filter(x -> scopeId.equals(x.getScopeId()))
                .collect(Collectors.toList());
        deleteUserPermission(user, permissionsToDelete);
    }

    /**
     * Smaže všechna oprávnení skupiny na daný typ rejstříku.
     *
     * @param group   skupina
     * @param scopeId id typu rejstříku
     */
    @AuthMethod(permission = {UsrPermission.Permission.USR_PERM, UsrPermission.Permission.GROUP_CONTROL_ENTITITY})
    public void deleteGroupScopePermissions(@AuthParam(type = AuthParam.Type.GROUP) @NotNull final UsrGroup group,
                                            @NotNull final Integer scopeId) {
        List<UsrPermission> permissionsDB = permissionRepository.findByGroupOrderByPermissionIdAsc(group);
        List<UsrPermission> permissionsToDelete = permissionsDB.stream()
                .filter(x -> scopeId.equals(x.getScopeId()))
                .collect(Collectors.toList());
        deleteGroupPermission(group, permissionsToDelete);
    }

    public UsrAuthentication findAuthentication(UsrUser user, UsrAuthentication.AuthType authType) {
    	Objects.requireNonNull(user);
    	Objects.requireNonNull(authType);
        UsrAuthentication result;
        if (allowDefaultUser && user.getUsername().equalsIgnoreCase(defaultUsername)) {
            result = new UsrAuthentication();
            result.setUser(user);
            result.setAuthType(UsrAuthentication.AuthType.PASSWORD);
            result.setAuthValue(defaultPassword);
        } else {
            result = authenticationRepository.findByUserAndAuthType(user, authType);
        }
        return result;
    }

    public List<UsrAuthentication> findAuthentication(String value, UsrAuthentication.AuthType authType) {
    	Objects.requireNonNull(value);
    	Objects.requireNonNull(authType);
        return authenticationRepository.findByAuthValueAndAuthType(value, authType);
    }

    public List<UsrAuthentication> findAuthentications(UsrUser user) {
    	Objects.requireNonNull(user);
        return authenticationRepository.findByUser(user);
    }

    /**
     * Provede přenačtení oprávnění uživatele.
     *
     * @param user uživatel, kterému přepočítáváme práva
     */
    public void invalidateCache(@NotNull final UsrUser user) {
        invalidateUserCache(user.getUserId());
    }

    /**
     * Provede přenačtení oprávnění uživatele.
     *
     * @param userId id uživatele, kterému přepočítáváme práva
     */
    private void invalidateUserCache(@NotNull final Integer userId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                userPermissionsCache.invalidate(userId);
            }
        });
    }

    /**
     * Provede přenačtení oprávnění všech uživatelů skupiny.
     *
     * @param group skupina
     */
    public void invalidateCache(@NotNull final UsrGroup group) {
        userRepository.findByGroup(group)
                .forEach(x -> invalidateUserCache(x.getUserId()));
    }

    /**
     * Join User to groups
     *
     * After joining groups user should be invalidated in cache
     * 
     * @param user
     * @param users
     * @return
     */
    private List<UsrGroupUser> joinGroups(UsrUser user, final Collection<UsrGroup> groups) {
        List<UsrGroupUser> result = new ArrayList<>();
        for (UsrGroup group : groups) {
            List<UsrGroupUser> rels = groupUserRepository.findByGroupAndUser(group, user);

            if (CollectionUtils.isNotEmpty(rels)) {
                throw new BusinessException(
                        "User '" + user.getUsername() + "' is already member of the group '" + group.getName() + "'",
                        UserCode.ALREADY_IN_GROUP).set("user", user.getUsername()).set("group", group.getName());
            }

            result.add(joinGroup(user, group));
        }
        return result;
    }

    UsrGroupUser joinGroup(UsrUser user, UsrGroup group) {
        UsrGroupUser item = new UsrGroupUser();
        item.setGroup(group);
        item.setUser(user);

        return groupUserRepository.save(item);
    }

    /**
     * Přidání uživatelů do skupin.
     *
     * @param groups skupiny do které přidávám uživatele
     * @param users  přidávaní uživatelé
     */
    @AuthMethod(permission = {UsrPermission.Permission.USR_PERM})
    public void joinGroup(@NotEmpty final Collection<UsrGroup> groups,
                          @NotEmpty final Collection<UsrUser> users) {
        for (UsrUser user : users) {
            joinGroups(user, groups);

            invalidateCache(user);
        }

        changeUsersEvent(users);
        changeGroupsEvent(groups);
    }

    /**
     * Odebrání uživatele ze skupiny.
     *
     * @param group skupina ze které odebírám
     * @param user  odebíraný uživatel
     */
    @AuthMethod(permission = {UsrPermission.Permission.USR_PERM})
    public void leaveGroup(@NotNull final UsrGroup group,
                           @NotNull final UsrUser user) {
        List<UsrGroupUser> items = groupUserRepository.findByGroupAndUser(group, user);

        if (CollectionUtils.isEmpty(items)) {
            throw new BusinessException("User '" + user.getUsername() + "' is not memeber of the group '" + group.getName() + "'",
                    UserCode.NOT_IN_GROUP).set("user", user.getUsername()).set("group", group.getName());
        }

        // delete all relations
      	groupUserRepository.deleteAll(items);

        invalidateCache(user);
        changeUserEvent(user);
        changeGroupEvent(group);
    }

    /**
     * Vytvoření skupiny.
     *
     * @param name        název skupiny
     * @param code        kód skupiny
     * @param description popis skupiny
     * @return skupina
     */
    @AuthMethod(permission = {UsrPermission.Permission.USR_PERM})
    public UsrGroup createGroup(@NotEmpty final String name,
                                @NotEmpty final String code,
                                final String description) {
        UsrGroup group = groupRepository.findOneByCode(code);
        if (group != null) {
            throw new BusinessException("Skupina s kódem již existuje", UserCode.GROUP_CODE_EXISTS).set("code", code);
        }

        group = new UsrGroup();
        group.setName(name);
        group.setCode(code);
        group.setDescription(description);

        groupRepository.save(group);
        createGroupEvent(group);
        return group;
    }

    /**
     * Smazání skupiny.
     *
     * @return skupina
     */
    @AuthMethod(permission = {UsrPermission.Permission.USR_PERM, UsrPermission.Permission.GROUP_CONTROL_ENTITITY})
    public UsrGroup deleteGroup(@AuthParam(type = AuthParam.Type.GROUP) @NotNull final UsrGroup group) {

        List<UsrGroupUser> groupUserList = groupUserRepository.findByGroup(group);
        Set<UsrUser> users = groupUserList.stream()
                .map(UsrGroupUser::getUser)
                .collect(Collectors.toSet());

        List<UsrPermission> permissions = permissionRepository.findByGroupOrderByPermissionIdAsc(group);
        Set<UsrUser> usersByPermission = permissions.stream()
                .filter(permission -> permission.getUser() != null)
                .map(UsrPermission::getUser)
                .collect(Collectors.toSet());
        users.addAll(usersByPermission);

        changeUsersEvent(users);

        permissionRepository.deleteByGroup(group);
        groupUserRepository.deleteByGroup(group);
        groupRepository.delete(group);
        deleteGroupEvent(group);
        return group;
    }

    /**
     * Změna skupiny.
     *
     * @param group       měněná skupina
     * @param name        název skupiny
     * @param description popis skupiny
     * @return skupina
     */
    @AuthMethod(permission = {UsrPermission.Permission.USR_PERM, UsrPermission.Permission.GROUP_CONTROL_ENTITITY})
    public UsrGroup changeGroup(@AuthParam(type = AuthParam.Type.GROUP) @NotNull final UsrGroup group,
                                @NotEmpty final String name,
                                final String description) {
        group.setName(name);
        group.setDescription(description);
        groupRepository.save(group);
        changeGroupEvent(group);
        return group;
    }

    /**
     * Změna uživatele.
     *
     * @param user          upravovaný uživatel
     * @param accessPointId id přístupového bodu
     * @param username      uživatelské jméno
     * @param valuesMap     data autentizací
     * @return upravený uživatel
     */
    @AuthMethod(permission = {UsrPermission.Permission.USR_PERM, UsrPermission.Permission.USER_CONTROL_ENTITITY})
    public UsrUser changeUser(@AuthParam(type = AuthParam.Type.USER) @NotNull final UsrUser user,
                              @Nullable final Integer accessPointId,
                              @NotNull @NotEmpty final String username,
                              @NotNull final Map<UsrAuthentication.AuthType, String> valuesMap) {
    	Objects.requireNonNull(valuesMap);
    	Objects.requireNonNull(username);
    	Objects.requireNonNull(user);

        if (accessPointId != null && !accessPointId.equals(user.getAccessPoint().getAccessPointId())) {
            ApAccessPoint accessPoint = accessPointService.getAccessPoint(accessPointId);
            user.setAccessPoint(accessPoint);
        }

        Map<UsrAuthentication.AuthType, UsrAuthentication> authTypeMap = findAuthentications(user).stream()
                .collect(Collectors.toMap(UsrAuthentication::getAuthType, Function.identity()));
        Set<UsrAuthentication> removeAuthentications = new HashSet<>(authTypeMap.values());
        Set<UsrAuthentication> changeAuthentications = new HashSet<>();

        if (!Objects.equals(user.getUsername(), username)) {
            UsrAuthentication authType = authTypeMap.get(UsrAuthentication.AuthType.PASSWORD);
            if (authType != null) { // kontrola jen pokud uživatel má autentizaci heslem
                String newPassword = valuesMap.get(UsrAuthentication.AuthType.PASSWORD);
                if (newPassword == null) {
                    throw new BusinessException("Při změně jména se musí změnit i heslo", UserCode.NEED_CHANGE_PASSWORD)
                            .level(Level.WARNING);
                }
            }
            user.setUsername(username);
        }

        for (Map.Entry<UsrAuthentication.AuthType, String> entry : valuesMap.entrySet()) {
            UsrAuthentication.AuthType authType = entry.getKey();
            String value = entry.getValue();

            UsrAuthentication usrAuthentication = authTypeMap.get(authType);
            if (usrAuthentication == null) {
                usrAuthentication = new UsrAuthentication();
                if (StringUtils.isEmpty(value)) {
                    throw new BusinessException("Hodnota musí být vyplněna", BaseCode.PROPERTY_IS_INVALID)
                            .set("type", authType);
                }
            } else {
                removeAuthentications.remove(usrAuthentication);
            }
            if (value == null) {
                continue;
            }
            updateAuth(user, authType, value, usrAuthentication);
            changeAuthentications.add(usrAuthentication);
        }

        if (changeAuthentications.size() > 0) {
            authenticationRepository.saveAll(changeAuthentications);
        }

        if (removeAuthentications.size() > 0) {
            authenticationRepository.deleteAll(removeAuthentications);
        }

        userRepository.save(user);

        changeUserEvent(user);
        return user;
    }

    /**
     * Vytvoření uživatele.
     *
     * @param username uživatelské jméno
     * @param valuesMap mapa typů autentizace + hodnota
     * @param accessPointId  id přístupového bodu
     * @return vytvořený uživatel
     */
    @AuthMethod(permission = {UsrPermission.Permission.USR_PERM})
    public UsrUser createUser(@NotEmpty final String username,
                              @NotNull final Map<UsrAuthentication.AuthType, String> valuesMap,
                              @NotNull final Integer accessPointId) {

        ApAccessPoint accessPoint = accessPointService.getAccessPoint(accessPointId);

        if (accessPoint == null) {
            throw new BusinessException("Přístupový bod neexistuje", RegistryCode.PARTY_NOT_EXIST);
        }

        UsrUser user = findByUsername(username);
        if (user != null) {
            throw new BusinessException("Uživatelské jméno již existuje", UserCode.USERNAME_EXISTS);
        }

        user = new UsrUser();
        user.setActive(true);
        user.setAccessPoint(accessPoint);
        user.setUsername(username);

        user = userRepository.save(user);

        if (valuesMap != null) {
            for (Map.Entry<UsrAuthentication.AuthType, String> entry : valuesMap.entrySet()) {
                UsrAuthentication.AuthType authType = entry.getKey();
                String value = entry.getValue();
                if (StringUtils.isEmpty(value)) {
                    throw new BusinessException("Hodnota musí být vyplněna", BaseCode.PROPERTY_IS_INVALID)
                            .set("type", authType);
                }
                UsrAuthentication authentication = new UsrAuthentication();
                updateAuth(user, authType, value, authentication);
                authenticationRepository.save(authentication);
            }
        }

        createUserEvent(user);
        return user;
    }

    private void updateAuth(final UsrUser user,
                            final UsrAuthentication.AuthType authType,
                            final String value,
                            final UsrAuthentication authentication) {
        authentication.setUser(user);
        authentication.setAuthType(authType);
        if (authType == UsrAuthentication.AuthType.PASSWORD) {
            authentication.setAuthValue(encodePassword(value));
        } else {
            authentication.setAuthValue(value);
        }
    }

    /**
     * Změna hesla uživatele.
     *
     * @param user        uživate, kterému měním heslo
     * @param oldPassword původní heslo (v plaintextu)
     * @param newPassword nové heslo (v plaintextu)
     * @return uživatel
     */
    public UsrUser changePassword(@NotNull final UsrUser user,
                                  @NotEmpty final String oldPassword,
                                  @NotEmpty final String newPassword) {
        if (oldPassword != null) {
            UsrAuthentication authentication = findAuthentication(user, UsrAuthentication.AuthType.PASSWORD);
            if (authentication == null) {
                throw new BusinessException("Uživatel nemá povolené přihlášení heslem", BaseCode.INVALID_STATE);
            }

            if (!matchesPassword(oldPassword, authentication.getAuthValue(), user.getUsername())) {
                throw new BusinessException("Původní heslo se neshoduje", UserCode.PASSWORD_NOT_MATCH);
            }
        }

        return changePasswordPrivate(user, newPassword);
    }

    /**
     * Změna hesla uživatele - s ověřením oprávnění.
     *
     * @param user        uživate, kterému měním heslo
     * @param newPassword nové heslo (v plaintextu)
     * @return uživatel
     */
    @AuthMethod(permission = {UsrPermission.Permission.USR_PERM, UsrPermission.Permission.USER_CONTROL_ENTITITY})
    public UsrUser changePassword(@AuthParam(type = AuthParam.Type.USER) @NotNull final UsrUser user,
                                  @NotEmpty final String newPassword) {
        return changePasswordPrivate(user, newPassword);
    }

    /**
     * Změna hesla uživatele.
     *
     * @param user        uživate, kterému měním heslo
     * @param newPassword nové heslo (v plaintextu)
     * @return uživatel
     */
    private UsrUser changePasswordPrivate(@NotNull final UsrUser user,
                                          @NotEmpty final String newPassword) {
        UsrAuthentication authentication = findAuthentication(user, UsrAuthentication.AuthType.PASSWORD);
        if (authentication == null) {
            throw new BusinessException("Uživatel nemá povolené přihlášení heslem", BaseCode.INVALID_STATE);
        }
        authentication.setAuthValue(encodePassword(newPassword));
        authenticationRepository.save(authentication);
        changeUserEvent(user);
        return user;
    }

    /**
     * Vyhledání uživatele podle username.
     *
     * @param username uživatelské jméno
     * @return uživatel
     * @throws UsernameNotFoundException
     */
    public UsrUser findByUsername(final String username) throws UsernameNotFoundException {
        UsrUser user = userRepository.findByUsername(username);
        if (user == null) {
            if (allowDefaultUser && username.equals(defaultUsername)) {
                user = createDefaultUser();
            }
        }
        return user;
    }

    private UsrUser createDefaultUser() {
        // create default admin user
        UsrUser user = new UsrUser();
        user.setActive(true);
        user.setUsername(defaultUsername);
        return user;
    }

    /**
     * Zahashování hesla.
     *
     * @param password uživatelské heslo v plaintextu
     * @return zahashované heslo
     */
    public String encodePassword(final String password) {
        return encoder.encode(password);
    }

    /**
     * Ověření hesla.
     *
     * @param password       raw heslo
     * @param encodePassword očekáváný hash hesla
     * @param username       uživatel pro kterého hash ověřujeme
     * @return true - heslo je OK
     */
    public boolean matchesPassword(final String password, final String encodePassword, final String username) {
        if (!encodePassword.startsWith("{")) {
            // zpětná kompatibilita
            logger.warn("Uživatel {} používá starý mechanismus ukládání hashe hesla. Prosím, prověďte změnu hesla pro zvýšení bezpečnosti!", username);
            return Sha256Support.encodePassword(password, username + SALT).equalsIgnoreCase(encodePassword);
        }
        return encoder.matches(password, encodePassword);
    }

    /**
     * Vrací přihlášeného uživatele - DO.
     *
     * @return přihlášený uživatel (null pokud je přihlášený admin nebo je to akce bez přihlášení)
     */
    public UsrUser getLoggedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return null;
        }
		UserDetail details = (UserDetail) auth.getDetails();
		UsrUser user = null;
		Integer userId = details.getId();
		if (userId != null) {
			// userId is set -> user have to be in the repository
			user = userRepository.findById(userId)
                    .orElse(null);
		}
		return user;
    }

    /**
     * Vrací detail přihlášeního uživatele - VO.
     *
     * @return detail přihlášeného uživatele (null pokud není nikdo přihlášený)
     */
    public UserDetail getLoggedUserDetail() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null) {
			return null;
		}
		UserDetail details = (UserDetail) auth.getDetails();

		Integer userId = details.getId();
		// admin has no userId but has detail
		// pokud je null, jedná se o virtuální admin účet, který má nastaveno oprávnění ADMIN
		if (userId != null) {
			try {
				// get permission from cache, refresh its TTL
				Collection<UserPermission> perms = userPermissionsCache.get(userId);
				// refresh permissions in user detail
				// probably should be places elsewhere
				details.setUserPermission(perms);
			} catch (ExecutionException e) {
				throw new SystemException(e);
			}
		}
		return details;
    }

    /**
     * Vypočítá oprávnění pro uživatele.
     *
     * @param user uživatel
     * @return seznam oprávnění
     */
    public Collection<UserPermission> calcUserPermission(final UsrUser user) {
        Map<UsrPermission.Permission, UserPermission> userPermissions = new HashMap<>();

        // check default user
        if (allowDefaultUser && user.getUsername().equals(defaultUsername)) {
            return Collections.singletonList(new UserPermission(UsrPermission.Permission.ADMIN));
        }

        List<UsrPermission> permissions = permissionRepository.getAllPermissions(user);
        Map<Integer, UserPermission> issueListMap = null;

        for (UsrPermission permission : permissions) {
            UserPermission userPermission = userPermissions.get(permission.getPermission());
            if (userPermission == null) {
                userPermission = new UserPermission(permission.getPermission());
                userPermissions.put(permission.getPermission(), userPermission);
            }

            if (permission.getFundId() != null) {
                userPermission.addFundId(permission.getFundId());
            }

            if (permission.getScopeId() != null) {
                userPermission.addScopeId(permission.getScopeId());
            }

            if (permission.getGroupControlId() != null) {
                userPermission.addControlGroupId(permission.getGroupControlId());
            }

            if (permission.getUserControlId() != null) {
                userPermission.addControlUserId(permission.getUserControlId());
            }

            if (permission.getIssueListId() != null) {
                userPermission.addIssueListId(permission.getIssueListId());
                if (issueListMap == null) {
                    issueListMap = new HashMap<>();
                }
                // TODO: Extend issueListId with corresponding fundId
                issueListMap.put(permission.getIssueListId(), userPermission);
            }

            if (permission.getNodeId() != null && permission.getFundId() != null) {
                userPermission.addNodeId(permission.getFundId(), permission.getNodeId());
            }
        }
        // Read issue list mapping
        if (issueListMap != null) {
            List<WfIssueList> issueLists = this.issueListRepository.findAllById(issueListMap.keySet());
            if (issueLists.size() != issueListMap.size()) {
                logger.error("Failed to read all issue lists, user: {}, expected count: {}, received: {}",
                             user.getUserId(),
                             issueListMap.size(),
                             issueLists.size());
                throw new BusinessException("Failed to read all issue lists", BaseCode.DB_INTEGRITY_PROBLEM)
                        .set("Expected number", issueListMap.size())
                        .set("Received Count", issueLists.size())
                        .set("userId", user.getUserId());
            }
            for (WfIssueList issueList : issueLists) {
                UserPermission perms = issueListMap.get(issueList.getIssueListId());
                perms.addFundId(issueList.getFundId());
            }
        }

        return new HashSet<>(userPermissions.values());
    }

    /**
     * Vrátí oprávnění přihlášeného uživatele.
     * - oprávnění se počítá pouze při přihlášení uživatele
     *
     * @return seznam oprávnění
     */
	private Collection<UserPermission> getUserPermission() {
        UserDetail userDetail = getLoggedUserDetail();
        if (userDetail == null) {
            return new ArrayList<>();
        }
        return userDetail.getUserPermission();
    }


    /**
	 * Kontroluje oprávnění přihlášeného uživatele.
	 *
	 * Check have to be called inside existing transaction
	 *
	 * @param permission
	 *            typ oprávnění
	 * @param entityId
	 *            identifikátor entity, ke které se ověřuje oprávnění
	 * @return má oprávnění?
	 */
	@Transactional(value = Transactional.TxType.MANDATORY)
    public boolean hasPermission(final UsrPermission.Permission permission,
                                 final Integer entityId) {
		UserDetail userDetail = getLoggedUserDetail();
        if (userDetail == null) {
            // user not authorized
            return false;
        }
        return userDetail.hasPermission(permission, entityId);
    }

    /**
	 * Kontroluje oprávnění přihlášeného uživatele.
	 *
	 * Check have to be called inside existing transaction
	 *
	 * @param permission
	 *            typ oprávnění
	 * @return má oprávnění?
	 */
	@Transactional(value = Transactional.TxType.MANDATORY)
    public boolean hasPermission(final UsrPermission.Permission permission) {
		UserDetail userDetail = getLoggedUserDetail();
		if (userDetail == null) {
		    return false;
        }
		return userDetail.hasPermission(permission);
    }

    /**
     * Hledání uživatelů na základě podmínek.
     *
     * @param search      hledaný text
     * @param active      aktivní uživatelé
     * @param disabled    zakázaní uživatelé
     * @param firstResult od jakého záznamu
     * @param maxResults  maximální počet vrácených záznamů
     * @return výsledky hledání
     */
	public FilteredResult<UsrUser> findUser(final String search, final boolean active, final boolean disabled,
	        final int firstResult, final int maxResults, final Integer excludedGroupId, final SearchType searchTypeName, final SearchType searchTypeUsername ) {
        if (!active && !disabled) {
            throw new IllegalArgumentException("Musí být uveden alespoň jeden z parametrů: active, disabled.");
        }

		UserDetail userDetail = getLoggedUserDetail();
		if (userDetail.hasPermission(UsrPermission.Permission.USR_PERM)) {
			// return all users
			return userRepository.findUserByText(search, active, disabled, firstResult, maxResults, excludedGroupId, searchTypeName, searchTypeUsername);
		} else {
			return userRepository.findUserByTextAndStateCount(search, active, disabled, firstResult, maxResults,
			        excludedGroupId,
			        userDetail.getId(), false, searchTypeName, searchTypeUsername);
		}
    }

    /**
     * Hledání uživatelů na základě podmínek, kteří mají přiřazené nebo zděděné oprávnění na zakládání nových AS.
     *
     * @param search      hledaný text
     * @param firstResult od jakého záznamu
     * @param maxResults  maximální počet vrácených záznamů, pokud je -1 neomezuje se
     * @return výsledky hledání
     */
	@Transactional
	@AuthMethod(permission={UsrPermission.Permission.FUND_ADMIN, UsrPermission.Permission.FUND_CREATE,
			UsrPermission.Permission.USR_PERM})
	public FilteredResult<UsrUser> findUserWithFundCreate(final String search, final Integer firstResult,
                                                          final Integer maxResults, final SearchType searchTypeName, final SearchType searchTypeUsername) {
		// get current user
		UserDetail userDetail = getLoggedUserDetail();
    	// if has admin rights -> we can find any user
		AuthorizationRequest authRequest = AuthorizationRequest.hasPermission(UsrPermission.Permission.FUND_ADMIN)
				.or(UsrPermission.Permission.USR_PERM);
		if (authRequest.matches(userDetail)) {
			// find in all users
			return this.findUser(search, true, false, firstResult, maxResults, null, searchTypeName, searchTypeUsername);
		}

		// only create permission -> have to return himself + or any controlled user
        return userRepository.findUserByTextAndStateCount(search, true, false, firstResult, maxResults, null,
                userDetail.getId(), true, searchTypeName, searchTypeUsername);
    }

    /**
     * Hledání skupin na základě podmínek.
     *
     * @param search      hledaný text
     * @param firstResult od jakého záznamu
     * @param maxResults  maximální počet vrácených záznamů
     * @return výsledky hledání
     */
    public FilteredResult<UsrGroup> findGroup(final String search, final Integer firstResult, final Integer maxResults) {
        boolean filterByUser = !hasPermission(UsrPermission.Permission.USR_PERM);
        UsrUser user = getLoggedUser();
        return groupRepository.findGroupByTextCount(search, firstResult, maxResults, filterByUser && user != null ? user.getUserId() : null);
    }

    /**
     * Hledání skupin na základě podmínek, které mají přiřazené oprávnění na zakládání nových AS.
     *
     * @param search      hledaný text
     * @param firstResult od jakého záznamu
     * @param maxResults  maximální počet vrácených záznamů, pokud je -1 neomezuje se
     * @return výsledky hledání
     */
    @Transactional
    @AuthMethod(permission={UsrPermission.Permission.FUND_ADMIN, UsrPermission.Permission.FUND_CREATE,
            UsrPermission.Permission.USR_PERM})
    public FilteredResult<UsrGroup> findGroupWithFundCreate(final String search, final Integer firstResult, final Integer maxResults) {

        // get current user
        UserDetail userDetail = getLoggedUserDetail();
        // if has admin rights -> we can find any group
        AuthorizationRequest authRequest = AuthorizationRequest.hasPermission(UsrPermission.Permission.FUND_ADMIN)
                .or(UsrPermission.Permission.USR_PERM);
        if (authRequest.matches(userDetail)) {
            // find in all users
            return groupRepository.findGroupByTextCount(search, firstResult, maxResults, null);
        }

        // only create permission -> have to return list of controlled group
        return groupRepository.findGroupByTextCount(search, firstResult, maxResults, userDetail.getId());
    }

    /**
     * Načtení objektu uživatele dle id.
     *
     * @param userId id
     * @return objekt
     */
    @AuthMethod(permission = {UsrPermission.Permission.USR_PERM, UsrPermission.Permission.USER_CONTROL_ENTITITY})
	public UsrUser getUser(@AuthParam(type = AuthParam.Type.USER) final Integer userId) {
		Validate.notNull(userId, "Identifikátor uživatele musí být vyplněno");
        return userRepository.getOneCheckExist(userId);
    }

    /**
     * Načtení objektu uživatele dle id pro vnitřní použití.
     *
     * @param userId
     * @return objekt nebo null
     */
    public UsrUser getUserInternal(final Integer userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.getOneCheckExist(userId);
    }

    /**
     * Načtení objektu uživatele dle id.
     *
     * @param userIds ids
     * @return objekt
     */
    @AuthMethod(permission = {UsrPermission.Permission.USR_PERM})
    public Set<UsrUser> getUsers(final Set<Integer> userIds) {
        Assert.notNull(userIds, "Identifikátory musí být vyplněny");
        List<UsrUser> users = userRepository.findAllById(userIds);
        if (users.size() != userIds.size()) {
            throw new IllegalArgumentException("Některý uživatel neexistuje");
        }
        return new HashSet<>(users);
    }

    /**
     * Načtení objektu uživatele dle id.
     *
     * @param groupIds ids
     * @return objekt
     */
    @AuthMethod(permission = {UsrPermission.Permission.USR_PERM})
    public Set<UsrGroup> getGroups(final Set<Integer> groupIds) {
        Assert.notNull(groupIds, "Identifikátory musí být vyplněny");
        List<UsrGroup> groups = groupRepository.findAllById(groupIds);
        if (groups.size() != groupIds.size()) {
            throw new IllegalArgumentException("Některá skupina neexistuje");
        }
        return new HashSet<>(groups);
    }

    /**
     * Načtení objektu skupiny dle id.
     *
     * @param groupId id
     * @return objekt
     */
    @AuthMethod(permission = {UsrPermission.Permission.USR_PERM, UsrPermission.Permission.GROUP_CONTROL_ENTITITY})
    public UsrGroup getGroup(@AuthParam(type = AuthParam.Type.GROUP) final Integer groupId) {
        Assert.notNull(groupId, "Identifikátor skupiny musí být vyplněn");
        return groupRepository.getOneCheckExist(groupId);
    }

    /**
     * Aktivace/deaktivace uživatele.
     *
     * @param user   upravovaný uživatel
     * @param active je aktivní?
     * @return uživatel
     */
    @AuthMethod(permission = {UsrPermission.Permission.USR_PERM, UsrPermission.Permission.USER_CONTROL_ENTITITY})
    public UsrUser changeActive(@AuthParam(type = AuthParam.Type.USER) @NotNull final UsrUser user,
                                @NotNull final Boolean active) {
        user.setActive(active);
        userRepository.save(user);

        changeUserEvent(user);
        return user;
    }

    /**
     * Vyhledá list uživatelů podle osoby.
     *
     * @param accessPoint osoba
     * @return list uživatelů
     */
    public List<UsrUser> findUsersByAccessPoint(final ApAccessPoint accessPoint) {
        return userRepository.findByAccessPoint(accessPoint);
    }

    /**
     * Vyhledá list uživatelů podle AS.
     * @param fund AS
     * @return list uživatelů
     */
    public List<UsrUser> findUsersByFund(final ArrFund fund) {
		// get list of all users for fund
		List<UsrUser> users = userRepository.findByFund(fund);

		return filterUsersByAdminPermission(users);
	}

	/**
     * Vyhledá list uživatelů podle oprávnění typu všechny AS.
	 */
	public List<UsrUser> findUsersByFundAll() {
		List<UsrUser> users = userRepository.findByPermissions(UsrPermission.Permission.getFundAllPerms());

		return filterUsersByAdminPermission(users);
	}

	/**
	 * Filter list of users to contain only users which might be administered by
	 * logged user
	 *
	 * Method will run query for each user in the list.
	 *
	 * @param users
	 * @return
	 */
	private List<UsrUser> filterUsersByAdminPermission(List<UsrUser> users) {
    	// check permissions
    	UserDetail userDetail = this.getLoggedUserDetail();
		if (userDetail.hasPermission(UsrPermission.Permission.USR_PERM)) {
            return users;
        }

		// check each user if might be accessed
		List<UsrUser> result = new ArrayList<>(users.size());
		for (UsrUser checkedUser : users) {
			List<Integer> perms = userRepository.findPermissionAllowingUserAccess(userDetail.getId(),
			        checkedUser.getUserId());
			if (CollectionUtils.isNotEmpty(perms)) {
				result.add(checkedUser);
			}
		}
		return result;
	}

    /**
     * Vyhledá list skupin podle oprávnění typu všechny AS.
     * @return list skupin
     */
    public List<UsrGroup> findGroupsByFundAll() {
		List<UsrGroup> groups = groupRepository.findByPermissions(UsrPermission.Permission.getFundAllPerms());

		return filterGroupsByAdminPermission(groups);
    }

    /**
     * Vyhledá list uživatelů podle AS.
     * @param fund AS
     * @return list uživatelů
     */
    public List<UsrGroup> findGroupsByFund(final ArrFund fund) {
		List<UsrGroup> groups = groupRepository.findByFund(fund);

		return filterGroupsByAdminPermission(groups);
    }

	/**
     * Event změněného uživatele.
     *
     * @param groups
     */
	private List<UsrGroup> filterGroupsByAdminPermission(List<UsrGroup> groups) {
		// check permissions
		UserDetail userDetail = this.getLoggedUserDetail();
		if (userDetail.hasPermission(UsrPermission.Permission.USR_PERM)) {
            return groups;
        }

		// check each user if might be accessed
		List<UsrGroup> result = new ArrayList<>(groups.size());
		for (UsrGroup checkedGroup : groups) {
			List<Integer> perms = userRepository.findPermissionAllowingGroupAccess(userDetail.getId(),
			        checkedGroup.getGroupId());
			if (CollectionUtils.isNotEmpty(perms)) {
				result.add(checkedGroup);
			}
		}
		return result;
	}

	/**
	 * Event změněného uživatele.
	 *
	 * @param user
	 *            uživatel
	 */
    private void changeUserEvent(final UsrUser user) {
        eventNotificationService.publishEvent(new EventId(EventType.USER_CHANGE, user.getUserId()));
    }

    /**
     * Event změněných uživatelů.
     *
     * @param users uživatelé
     */
    private void changeUsersEvent(final Collection<UsrUser> users) {
        Set<Integer> userIds = users.stream().map(UsrUser::getUserId).collect(Collectors.toSet());
        eventNotificationService.publishEvent(new EventId(EventType.USER_CHANGE, userIds));
    }

    /**
     * Event vytvořeného uživatele.
     *
     * @param user uživatel
     */
    private void createUserEvent(final UsrUser user) {
        eventNotificationService.publishEvent(new EventId(EventType.USER_CREATE, user.getUserId()));
    }

    /**
     * Event změněného uživatele.
     *
     * @param group skupina
     */
    private void changeGroupEvent(final UsrGroup group) {
        eventNotificationService.publishEvent(new EventId(EventType.GROUP_CHANGE, group.getGroupId()));
    }

    /**
     * Event změněné skupiny.
     *
     * @param groups skupiny
     */
    private void changeGroupsEvent(final Collection<UsrGroup> groups) {
        Set<Integer> groupIds = groups.stream().map(UsrGroup::getGroupId).collect(Collectors.toSet());
        eventNotificationService.publishEvent(new EventId(EventType.GROUP_CHANGE, groupIds));
    }

    /**
     * Event vytvořené skupiny.
     *
     * @param group skupina
     */
    private void createGroupEvent(final UsrGroup group) {
        eventNotificationService.publishEvent(new EventId(EventType.GROUP_CREATE, group.getGroupId()));
    }

    /**
     * Event smazané skupiny.
     *
     * @param group skupina
     */
    private void deleteGroupEvent(final UsrGroup group) {
        eventNotificationService.publishEvent(new EventId(EventType.GROUP_DELETE, group.getGroupId()));
    }

    /**
     * Odstraní oprávnění podle AS.
     *
     * @param fund archivní soubor
     */
    @Transactional
    public void deletePermissionsByFund(final ArrFund fund) {
        permissionRepository.deleteByFund(fund);
    }

    /**
     * Odstraní oprávnění podle protokolu.
     *
     * @param issueList protokol
     */
    @Transactional
    public void deletePermissionsByIssueList(final WfIssueList issueList) {
        permissionRepository.deleteByIssueList(issueList);
    }

    /**
     * Odstraní oprávnění podle protokolu.
     *
     * @param issueList protokol
     * @param permission oprávnění
     */
    @Transactional
    public void deletePermissionsByIssueList(final WfIssueList issueList, Permission permission) {
        permissionRepository.deleteByIssueListAndPermission(issueList, permission);
    }

    /**
     * Vyhledá uživatele na základě id a vytvoří mapu.
     *
     * @param userIds hledaní uživatelé
     * @return mapa uživatelů
     */
    public Map<Integer, UsrUser> findUserMap(final Collection<Integer> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<UsrUser> users = userRepository.findAllById(userIds);
        return users.stream().collect(Collectors.toMap(UsrUser::getUserId, Function.identity()));
    }

    /**
     * Vrátí id scope na které ma uživatel právo. Nebere v úvahu právo {@link UsrPermission.Permission#AP_SCOPE_RD_ALL}.
     *
     * @return množina id scope na které ma uživatel právo
     */
	@Transactional(value = Transactional.TxType.MANDATORY)
    public Set<Integer> getUserScopeIds() {
        return getUserPermission()
                .stream()
                .filter(p -> p.getPermission() == UsrPermission.Permission.AP_SCOPE_RD)
                .findFirst()
                .map(p -> new HashSet<>(p.getScopeIds()))
                .orElse(new HashSet<>());
    }

	/**
	 * Authorize request or throw exception
	 */
	@Transactional(value = Transactional.TxType.MANDATORY)
	public void authorizeRequest(AuthorizationRequest authRequest) {
		UserDetail userDetail = getLoggedUserDetail();
        if (userDetail != null && authRequest.matches(userDetail)) {
			// request match permissions
			return;
		}

		// throw exception - authorization not granted
		UsrPermission.Permission deniedPermissions[] = authRequest.getPermissions();
		throw new AccessDeniedException("Missing permissions: " + Arrays.toString(deniedPermissions),
		        deniedPermissions);
	}

	/**
	 * Return detail information about logged user
	 *
	 * This method can be called by any user, no permissions are required
	 * @return
	 */
	@Transactional
	public UserInfoVO getLoggedUserInfo() {
        final UserDetail userDetail = getLoggedUserDetail();
		Integer userId = userDetail.getId();
		String preferredName = null;
		// if not admin
		if (userId != null) {
			// read user from db
			UsrUser user = userRepository.findOneWithDetail(userId);
			Validate.notNull(user, "Failed to get user: {}", userId);

			ApAccessPoint record = user.getAccessPoint();
			//TODO : smazáno - přiřazení preferovaného jména
		}

		return UserInfoVO.newInstance(userDetail, preferredName);
	}

	/**
	 * Add admin permissions to user/group for given fund
	 *
	 * Method is used only for newly created fund.
	 *
	 * @param userId
	 *            Can be null
	 * @param groupId
	 *            Can be null
	 * @param newFund
	 */
	@Transactional(value = Transactional.TxType.MANDATORY)
	public void addFundAdminPermissions(Integer userId, Integer groupId, ArrFund newFund) {
		UsrUser user = null;
		UsrGroup group = null;
		if (userId != null) {
			// only user or group might be set
			Validate.isTrue(groupId == null);

			user = userRepository.getOneCheckExist(userId);
		}
		if (groupId != null) {
			// only user or group might be set
			Validate.isTrue(userId == null);

			group = groupRepository.getOneCheckExist(groupId);
		}

		// if we do not have right ADMIN, FUND_ADMIN or USR_PERM
		// we have to have rights FUND_CREATE (In such case we can create it only for logged user)
		// or USER_CONTROL_ENTITITY (In such case we can create it only for managed entities).

        // check if user is same as logged user
        UserDetail userDetail = getLoggedUserDetail();

		boolean hasPermission = false;
        if (userDetail.hasPermission(UsrPermission.Permission.FUND_ADMIN)
                || userDetail.hasPermission(UsrPermission.Permission.USR_PERM)
                || userDetail.hasPermission(UsrPermission.Permission.FUND_CREATE)
                ) {
            hasPermission = true;
        } else
        if (userId!=null && userDetail.isControllsUser(userId))
        {
            hasPermission = true;
        } else
        if(groupId!=null && userDetail.isControllsGroup(groupId)) {
            hasPermission = true;
        }

		if (!hasPermission) {
			Permission[] perms = { UsrPermission.Permission.FUND_ADMIN, UsrPermission.Permission.USR_PERM,
			        UsrPermission.Permission.FUND_CREATE,
			        UsrPermission.Permission.USER_CONTROL_ENTITITY,
			        UsrPermission.Permission.GROUP_CONTROL_ENTITITY };
			throw new AccessDeniedException("Cannot set permissions for new fund.", perms);
		}

		// now we can add permission
		List<UsrPermission> usrPermissions = new ArrayList<>(FUND_ADMIN_PERMISSIONS.length);
		for (UsrPermission.Permission permission : FUND_ADMIN_PERMISSIONS) {
			UsrPermission usrPerm = new UsrPermission();
			usrPerm.setPermission(permission);
			usrPerm.setFund(newFund);
			usrPerm.setUser(user);
			usrPerm.setGroup(group);
			usrPermissions.add(usrPerm);
		}
		if (group != null) {
			addGroupPermission(group, usrPermissions, false);
		}
		if (user != null) {
			addUserPermission(user, usrPermissions, false);
		}
	}

    /**
     * Nastavení oprávnění k novému protokolu
     */
    @Transactional
    @AuthMethod(permission = {Permission.ADMIN, Permission.FUND_ISSUE_ADMIN_ALL, Permission.FUND_ISSUE_ADMIN})
    public void updateIssueListPermissions(@AuthParam(type = AuthParam.Type.ISSUE_LIST) @NotNull WfIssueList issueList, @Nullable Collection<UsrUser> rdUsers, @Nullable Collection<UsrUser> wrUsers) {

        Validate.notNull(issueList, "Issue list is null");

        Map<Integer, UsrUser> update = new HashMap<>();

        if (rdUsers != null) {
            update.putAll(updateIssueListPermissions(issueList, rdUsers, Permission.FUND_ISSUE_LIST_RD));
        }

        if (wrUsers != null) {
            update.putAll(updateIssueListPermissions(issueList, wrUsers, Permission.FUND_ISSUE_LIST_WR));
        }

        for (UsrUser user : update.values()) {
            invalidateCache(user);
            changeUserEvent(user);
        }

        permissionRepository.flush();
    }

    private Map<Integer, UsrUser> updateIssueListPermissions(@NotNull WfIssueList issueList,
                                                             @NotNull Collection<UsrUser> users,
                                                             @NotNull Permission permissionType) {

        Map<Integer, UsrUser> userMap = users.stream().collect(Collectors.toMap(user -> user.getUserId(), user -> user));

        Map<Integer, UsrUser> update = new HashMap<>();

        for (UsrPermission permissionDb : permissionRepository.findByIssueListAndPermission(issueList.getIssueListId(), permissionType)) {
            UsrUser user = permissionDb.getUser();
            if (userMap.remove(user.getUserId()) == null) {
                permissionRepository.delete(permissionDb);
                update.put(user.getUserId(), user);
            }
        }

        for (UsrUser user : userMap.values()) {
            UsrPermission permission = new UsrPermission();
            permission.setPermission(permissionType);
            permission.setUser(user);
            permission.setIssueList(issueList);
            permissionRepository.save(permission);
            update.put(user.getUserId(), user);
        }

        return update;
    }

    /**
     * Create user detail for security context
     *
     * @param user
     * @return
     */
    public UserDetail createUserDetail(UsrUser user) {
        Collection<UserPermission> perms = calcUserPermission(user);

        List<UsrAuthentication.AuthType> authTypes = new ArrayList<>();
        if (allowDefaultUser && user.getUsername().equalsIgnoreCase(defaultUsername)) {
            authTypes.add(UsrAuthentication.AuthType.PASSWORD);
        } else {
            authTypes.addAll(findAuthentications(user).stream()
                    .map(UsrAuthentication::getAuthType)
                    .collect(Collectors.toList()));
        }

        return new UserDetail(user, perms, levelTreeCacheService, authTypes);
    }

    public UserDetail createUserDetail(Integer userId) {
        UsrUser user;
        if (userId == null && allowDefaultUser) {
            // create for default user
            user = createDefaultUser();
        } else {
            user = userRepository.getOneCheckExist(userId);
        }
        if (user == null) {
            throw new ObjectNotFoundException("User not found", BaseCode.ID_NOT_EXIST);
        }

        return createUserDetail(user);
    }

    /**
     * Method to create admin detail
     *
     * This is temporary method and will be removed in future
     *
     * @return
     */
    private UserDetail createAdminUserDetail() {
        UsrUser user = createDefaultUser();
        Collection<UserPermission> perms = Collections.singletonList(new UserPermission(
                UsrPermission.Permission.ADMIN));

        List<UsrAuthentication.AuthType> authTypes = new ArrayList<>();
        authTypes.add(UsrAuthentication.AuthType.PASSWORD);

        return new UserDetail(user, perms, levelTreeCacheService, authTypes);
    }

    public boolean hasFullArrPerm(final Integer fundId) {
        UserDetail userDetail = getLoggedUserDetail();
        AuthorizationRequest authRequest = AuthorizationRequest.hasPermission(UsrPermission.Permission.FUND_ADMIN)
                .or(UsrPermission.Permission.FUND_ARR_ALL)
                .or(UsrPermission.Permission.FUND_ARR, fundId);
        return authRequest.matches(userDetail);
    }

    public SecurityContext createSecurityContext(Integer userId) {

        SecurityContext ctx = SecurityContextHolder.createEmptyContext();

        // read user from db
        String username = null, encodePassword = null;

        UserDetail userDetail = createUserDetail(userId);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(username, encodePassword,
                null);
        auth.setDetails(userDetail);
        ctx.setAuthentication(auth);

        return ctx;
    }

    /**
     * Create securoty context for admin
     *
     * @return
     */
    public SecurityContext createSecurityContextSystem() {
        SecurityContext secCtx = SecurityContextHolder.createEmptyContext();

        UserDetail userDetail = createAdminUserDetail();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(null, null, null);
        auth.setDetails(userDetail);
        secCtx.setAuthentication(auth);
        return secCtx;
    }

    public interface UserStats {

        int getActiveUserCount();

    }

    public UserStats getStats() {
        int activeUserCount = userRepository.countActive();
        return new UserStats() {

            @Override
            public int getActiveUserCount() {
                return activeUserCount;
            }

        };
    }

    /**
     * Append/copy permissions from one user to another
     * 
     * @param trgUserId
     *            Target userId
     * @param fromUserId
     *            Source user
     */
    @AuthMethod(permission = { Permission.USR_PERM, Permission.USER_CONTROL_ENTITITY })
    public void copyPermissions(Integer trgUserId, @NotNull Integer fromUserId) {
        UsrUser trgUser = userRepository.findOneWithDetail(trgUserId);
        UserDetail trgUserDetail = createUserDetail(trgUser);
        
        UsrUser fromUser = userRepository.findOneWithDetail(fromUserId);
        List<UsrPermission> srcPermissions = permissionRepository.findByUserOrderByPermissionIdAsc(fromUser);
        
        List<UsrPermission> addPermissions = new ArrayList<>();

        // iterate fromUserDetail permissions and check if exists equivalent in trgUserDetail
        for (UsrPermission srcPermission : srcPermissions) {
            if (trgUserDetail.hasPermission(srcPermission)) {
                continue;
            }
            UsrPermission copy = srcPermission.copy();
            copy.setPermissionId(null);
            copy.setUser(trgUser);
            addPermissions.add(copy);
        }
        if(CollectionUtils.isNotEmpty(addPermissions)) {
            ChangedPermissionResult result = changePermission(trgUser, null, addPermissions, ChangePermissionType.ADD,
                                                              true);
            logger.info("Copied permission from userId: {} to userId: {}, count: {}", fromUserId, trgUserId,
                     result.getNewPermissions().size());
        }

        // Copy group membership
        List<UsrGroupUser> srcUserGroups = groupUserRepository.findByUser(fromUser);
        List<UsrGroupUser> trgUserGroups = groupUserRepository.findByUser(trgUser);

        // Set of current membership
        Set<Integer> currentGroupMemebership = trgUserGroups.stream().map(UsrGroupUser::getGroupId)
                .collect(Collectors.toSet());
        for (UsrGroupUser srcUserGroup : srcUserGroups) {
            // check if not already member
            if (!currentGroupMemebership.contains(srcUserGroup.getGroupId())) {
                // create new group membership

                joinGroup(trgUser, srcUserGroup.getGroup());
                currentGroupMemebership.add(srcUserGroup.getGroupId());
            }
        }

        invalidateCache(trgUser);
        changeUserEvent(trgUser);
    }
}
