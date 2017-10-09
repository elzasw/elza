package cz.tacr.elza.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.encoding.ShaPasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import cz.tacr.elza.annotation.AuthMethod;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.RegScope;
import cz.tacr.elza.domain.UsrGroup;
import cz.tacr.elza.domain.UsrGroupUser;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.RegistryCode;
import cz.tacr.elza.exception.codes.UserCode;
import cz.tacr.elza.repository.FilteredResult;
import cz.tacr.elza.repository.FundRepository;
import cz.tacr.elza.repository.GroupRepository;
import cz.tacr.elza.repository.GroupUserRepository;
import cz.tacr.elza.repository.PermissionRepository;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.repository.UserRepository;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.security.UserPermission;
import cz.tacr.elza.service.eventnotification.events.EventId;
import cz.tacr.elza.service.eventnotification.events.EventType;

/**
 * Serviska pro uživatele.
 *
 * @author Martin Šlapa
 * @since 11.04.2016
 */
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private GroupUserRepository groupUserRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private PartyService partyService;

    @Autowired
    private IEventNotificationService eventNotificationService;

    @Autowired
    private FundRepository fundRepository;

    @Autowired
    private ScopeRepository scopeRepository;

    @Autowired
    private SessionRegistry sessionRegistry;

    @Value("${elza.security.salt:kdFss=+4Df_%}")
    private String SALT;

    private Object synchObj = new Object();

    private ShaPasswordEncoder encoder = new ShaPasswordEncoder(256);

    /** Cache pro nakešování oprávnění uživatele. */
    private LoadingCache<Integer, Collection<UserPermission>> userPermissionsCache;

    public UserService() {
        userPermissionsCache = CacheBuilder.newBuilder()
                .maximumSize(150)
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .build(new CacheLoader<Integer, Collection<UserPermission>>() {
                    @Override
                    public Collection<UserPermission> load(final Integer userId) throws Exception {
                        final UsrUser user = getUser(userId);
                        return calcUserPermission(user);
                    }
                });
    }

    private enum ChangePermissionType {
        SYNCHRONIZE,
        ADD,
        DELETE
    }

    private List<UsrPermission> changePermission(final UsrUser user,
                                  final UsrGroup group,
                                  final @NotNull List<UsrPermission> permissions,
                                  final @NotNull List<UsrPermission> permissionsDB,
                                  final @NotNull ChangePermissionType changePermissionType
                                  ) {
        Map<Integer, UsrPermission> permissionMap = permissionsDB.stream()
                .collect(Collectors.toMap(UsrPermission::getPermissionId, Function.identity()));

        List<UsrPermission> permissionsAdd = new ArrayList<>();
        List<UsrPermission> permissionsUpdate = new ArrayList<>();

        for (UsrPermission permission : permissions) {
            if (permission.getPermissionId() == null) {
                if (changePermissionType == ChangePermissionType.DELETE) {  // pokud se jedná o akci delete, nesmí být předán záznam bez id
                    throw new SystemException("V akci delete nelze předat oprávnění s nevyplněným id", UserCode.PERM_ILLEGAL_INPUT);
                }
                permission.setUser(user);
                permission.setGroup(group);
                setFundRelation(permission, permission.getFundId());
                setScopeRelation(permission, permission.getScopeId());
                setControlUserRelation(permission, permission.getUserControlId());
                setControlGroupRelation(permission, permission.getGroupControlId());
                permissionsAdd.add(permission);
            } else {
                if (changePermissionType == ChangePermissionType.ADD) { // pro akci add nelze předat vyplněné id
                    throw new SystemException("V akci add nelze předat oprávnění s vyplněným id", UserCode.PERM_ILLEGAL_INPUT);
                } else if (changePermissionType == ChangePermissionType.SYNCHRONIZE) {  // jen zde přidáváme, jinak se jedná o akci delete
                    UsrPermission permissionDB = permissionMap.get(permission.getPermissionId());
                    if (permissionDB == null) {
                        throw new SystemException("Oprávnění neexistuje a proto nemůže být upraveno", UserCode.PERM_NOT_EXIST);
                    }
                    permissionDB.setPermission(permission.getPermission());
                    setFundRelation(permissionDB, permission.getFundId());
                    setScopeRelation(permissionDB, permission.getScopeId());
                    setControlUserRelation(permissionDB, permission.getUserControlId());
                    setControlGroupRelation(permissionDB, permission.getGroupControlId());
                    permissionsUpdate.add(permissionDB);
                }
            }
        }

        List<UsrPermission> permissionsDelete;
        if (changePermissionType == ChangePermissionType.DELETE) {  // v delete budou ty, co jsou předané
            permissionsDelete = permissions.stream()
                    .map(permission -> {
                        UsrPermission permissionDB = permissionMap.get(permission.getPermissionId());
                        if (permissionDB == null) {
                            throw new SystemException("Oprávnění neexistuje a proto nemůže být upraveno", UserCode.PERM_NOT_EXIST);
                        }
                        return permissionDB;
                    })
                    .collect(Collectors.toList());
        } else if (changePermissionType == ChangePermissionType.ADD) {    // v delete nebude nic
            permissionsDelete = new ArrayList<>();
        } else if (changePermissionType == ChangePermissionType.SYNCHRONIZE) {    // v delete budou ty, co se neaktualizovaly
            permissionsDelete = new ArrayList<>(permissionsDB);
            permissionsDelete.removeAll(permissionsUpdate);
        } else {
            throw new IllegalStateException("Nepodporovaný typ změny oprávění: " + changePermissionType);
        }

        for (UsrPermission permission : permissions) {
            switch (permission.getPermission().getType()) {
                case ALL: {
                    if (permission.getScopeId() != null || permission.getFundId() != null || permission.getUserControlId() != null || permission.getGroupControlId() != null) {
                        throw new SystemException("Neplatný vstup oprávnění: ALL", UserCode.PERM_ILLEGAL_INPUT).set("type", "ALL");
                    }
                    break;
                }
                case SCOPE: {
                    if (permission.getScopeId() == null || permission.getFundId() != null || permission.getUserControlId() != null || permission.getGroupControlId() != null) {
                        throw new SystemException("Neplatný vstup oprávnění: SCOPE", UserCode.PERM_ILLEGAL_INPUT).set("type", "SCOPE");
                    }
                    break;
                }
                case FUND: {
                    if (permission.getScopeId() != null || permission.getFundId() == null || permission.getUserControlId() != null || permission.getGroupControlId() != null) {
                        throw new SystemException("Neplatný vstup oprávnění: FUND", UserCode.PERM_ILLEGAL_INPUT).set("type", "FUND");
                    }
                    break;
                }
                case USER:
                    if (permission.getScopeId() != null || permission.getFundId() != null || permission.getUserControlId() == null || permission.getGroupControlId() != null) {
                        throw new SystemException("Neplatný vstup oprávnění: USER", UserCode.PERM_ILLEGAL_INPUT).set("type", "USER");
                    }
                    break;
                case GROUP:
                    if (permission.getScopeId() != null || permission.getFundId() != null || permission.getUserControlId() != null || permission.getGroupControlId() == null) {
                        throw new SystemException("Neplatný vstup oprávnění: GROUP", UserCode.PERM_ILLEGAL_INPUT).set("type", "GROUP");
                    }
                    break;
                default:
                    throw new IllegalStateException("Nedefinovaný typ oprávnění");
            }
        }

        permissionRepository.delete(permissionsDelete);
        permissionRepository.save(permissionsAdd);
        permissionRepository.save(permissionsUpdate);

        List<UsrPermission> result = new ArrayList<>();
        result.addAll(permissionsAdd);
        result.addAll(permissionsUpdate);
        return result;
    }

    /**
     * Nastaví vazbu na soubor, pokud je předané id. Pokud předané není, je vazba odstraněna.
     * Kontroluje existenci objektu s daným id.
     * @param permission oprávnění
     * @param fundId id objektu, na který má být přidána vazba
     */
    private void setFundRelation(final UsrPermission permission, final Integer fundId) {
        if (fundId != null) {
            ArrFund fund = fundRepository.findOne(fundId);
            if (fund == null) {
                throw new SystemException("Neplatný archivní soubor", ArrangementCode.FUND_NOT_FOUND).set("id", fundId);
            }
            permission.setFund(fund);
        } else {
            permission.setFund(null);
        }
    }

    /**
     * Nastaví vazbu na scope, pokud je předané id. Pokud předané není, je vazba odstraněna.
     * Kontroluje existenci objektu s daným id.
     * @param permission oprávnění
     * @param scopeId id objektu, na který má být přidána vazba
     */
    private void setScopeRelation(final UsrPermission permission, final Integer scopeId) {
        if (scopeId != null) {
            RegScope scope = scopeRepository.findOne(scopeId);
            if (scope == null) {
                throw new SystemException("Neplatný scope", BaseCode.ID_NOT_EXIST);
            }
            permission.setScope(scope);
        } else {
            permission.setScope(null);
        }
    }

    /**
     * Nastaví vazbu na uživatele - spravovaná etita, pokud je předané id. Pokud předané není, je vazba odstraněna.
     * Kontroluje existenci objektu s daným id.
     * @param permission oprávnění
     * @param userId id objektu, na který má být přidána vazba
     */
    private void setControlUserRelation(final UsrPermission permission, final Integer userId) {
        if (userId != null) {
            UsrUser user = userRepository.findOne(userId);
            if (user == null) {
                throw new SystemException("Neplatný uživatel", BaseCode.ID_NOT_EXIST);
            }
            permission.setUserControl(user);
        } else {
            permission.setUserControl(null);
        }
    }

    /**
     * Nastaví vazbu na skupinu - spravovaná etita, pokud je předané id. Pokud předané není, je vazba odstraněna.
     * Kontroluje existenci objektu s daným id.
     * @param permission oprávnění
     * @param groupId id objektu, na který má být přidána vazba
     */
    private void setControlGroupRelation(final UsrPermission permission, final Integer groupId) {
        if (groupId != null) {
            UsrGroup group = groupRepository.findOne(groupId);
            if (group == null) {
                throw new SystemException("Neplatná skupina", BaseCode.ID_NOT_EXIST);
            }
            permission.setGroupControl(group);
        } else {
            permission.setGroupControl(null);
        }
    }

    @AuthMethod(permission = {UsrPermission.Permission.USR_PERM})
    public void changeGroupPermission(@NotNull final UsrGroup group,
                                      @NotNull final List<UsrPermission> permissions) {
        List<UsrPermission> permissionsDB = permissionRepository.findByGroupOrderByPermissionIdAsc(group);
        changePermission(null, group, permissions, permissionsDB, ChangePermissionType.SYNCHRONIZE);
        changeGroupEvent(group);
    }

    @AuthMethod(permission = {UsrPermission.Permission.USR_PERM})
    public void changeUserPermission(@NotNull final UsrUser user,
                                     @NotNull final List<UsrPermission> permissions) {
        List<UsrPermission> permissionsDB = permissionRepository.findByUserOrderByPermissionIdAsc(user);
        changePermission(user, null, permissions, permissionsDB, ChangePermissionType.SYNCHRONIZE);
        invalidateCache(user);
        changeUserEvent(user);
    }

    @AuthMethod(permission = {UsrPermission.Permission.USR_PERM})
    public List<UsrPermission> addUserPermission(@NotNull final UsrUser user,
                                     @NotNull final List<UsrPermission> permissions) {
        List<UsrPermission> permissionsDB = permissionRepository.findByUserOrderByPermissionIdAsc(user);
        List<UsrPermission> result = changePermission(user, null, permissions, permissionsDB, ChangePermissionType.ADD);
        invalidateCache(user);
        changeUserEvent(user);
        return result;
    }

    @AuthMethod(permission = {UsrPermission.Permission.USR_PERM})
    public List<UsrPermission> addGroupPermission(@NotNull final UsrGroup group,
                                     @NotNull final List<UsrPermission> permissions) {
        List<UsrPermission> permissionsDB = permissionRepository.findByGroupOrderByPermissionIdAsc(group);
        List<UsrPermission> result = changePermission(null, group, permissions, permissionsDB, ChangePermissionType.ADD);
        invalidateCache(group);
        changeGroupEvent(group);
        return result;
    }

    @AuthMethod(permission = {UsrPermission.Permission.USR_PERM})
    public void deleteUserPermission(@NotNull final UsrUser user,
                                     @NotNull final List<UsrPermission> permissions) {
        List<UsrPermission> permissionsDB = permissionRepository.findByUserOrderByPermissionIdAsc(user);
        changePermission(user, null, permissions, permissionsDB, ChangePermissionType.DELETE);
        invalidateCache(user);
        changeUserEvent(user);
    }

    @AuthMethod(permission = {UsrPermission.Permission.USR_PERM})
    public void deleteGroupPermission(@NotNull final UsrGroup group,
                                     @NotNull final List<UsrPermission> permissions) {
        List<UsrPermission> permissionsDB = permissionRepository.findByGroupOrderByPermissionIdAsc(group);
        changePermission(null, group, permissions, permissionsDB, ChangePermissionType.DELETE);
        invalidateCache(group);
        changeGroupEvent(group);
    }

    /**
     * Smaže všechna oprávnení uživatele na daný AS.
     * @param user uživatel
     * @param fundId id AS
     */
    @AuthMethod(permission = {UsrPermission.Permission.USR_PERM})
    public void deleteUserFundPermissions(@NotNull final UsrUser user,
                                          @NotNull final Integer fundId) {
        List<UsrPermission> permissionsDB = permissionRepository.findByUserOrderByPermissionIdAsc(user);
        List<UsrPermission> permissionsToDelete = permissionsDB.stream()
                .filter(x -> fundId.equals(x.getFundId()))
                .collect(Collectors.toList());
        deleteUserPermission(user, permissionsToDelete);
    }

    /**
     * Smaže všechna oprávnení skupiny na daný AS.
     * @param group skupina
     * @param fundId id AS
     */
    @AuthMethod(permission = {UsrPermission.Permission.USR_PERM})
    public void deleteGroupFundPermissions(@NotNull final UsrGroup group,
                                          @NotNull final Integer fundId) {
        List<UsrPermission> permissionsDB = permissionRepository.findByGroupOrderByPermissionIdAsc(group);
        List<UsrPermission> permissionsToDelete = permissionsDB.stream()
                .filter(x -> fundId.equals(x.getFundId()))
                .collect(Collectors.toList());
        deleteGroupPermission(group, permissionsToDelete);
    }

    /**
     * Smaže všechna oprávnení uživatel na daný typ rejstříku.
     * @param user uživatel
     * @param scopeId id typu rejstříku
     */
    @AuthMethod(permission = {UsrPermission.Permission.USR_PERM})
    public void deleteUserScopePermissions(@NotNull final UsrUser user,
                                           @NotNull final Integer scopeId) {
        List<UsrPermission> permissionsDB = permissionRepository.findByUserOrderByPermissionIdAsc(user);
        List<UsrPermission> permissionsToDelete = permissionsDB.stream()
                .filter(x -> scopeId.equals(x.getScopeId()))
                .collect(Collectors.toList());
        deleteUserPermission(user, permissionsToDelete);
    }

    /**
     * Smaže všechna oprávnení skupiny na daný typ rejstříku.
     * @param group skupina
     * @param scopeId id typu rejstříku
     */
    @AuthMethod(permission = {UsrPermission.Permission.USR_PERM})
    public void deleteGroupScopePermissions(@NotNull final UsrGroup group,
                                           @NotNull final Integer scopeId) {
        List<UsrPermission> permissionsDB = permissionRepository.findByGroupOrderByPermissionIdAsc(group);
        List<UsrPermission> permissionsToDelete = permissionsDB.stream()
                .filter(x -> scopeId.equals(x.getScopeId()))
                .collect(Collectors.toList());
        deleteGroupPermission(group, permissionsToDelete);
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
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
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
        userRepository.findByGroup(group).stream()
                .forEach(x -> invalidateUserCache(x.getUserId()));
    }

    /**
     * Přidání uživatelů do skupin.
     *
     * @param groups skupiny do které přidávám uživatele
     * @param users  přidávaní uživatelé
     */
    @AuthMethod(permission = {UsrPermission.Permission.USR_PERM})
    public void joinGroup(@NotEmpty final Set<UsrGroup> groups,
                          @NotEmpty final Set<UsrUser> users) {
        for (UsrUser user : users) {
            for (UsrGroup group : groups) {
                UsrGroupUser item = groupUserRepository.findOneByGroupAndUser(group, user);

                if (item != null) {
                    throw new BusinessException("Uživatel '" + user.getUsername() + "' je již členem skupiny '" + group.getName() + "'",
                            UserCode.ALREADY_IN_GROUP).set("user", user.getUsername()).set("group", group.getName());
                }

                item = new UsrGroupUser();
                item.setGroup(group);
                item.setUser(user);

                groupUserRepository.save(item);
            }
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
        UsrGroupUser item = groupUserRepository.findOneByGroupAndUser(group, user);

        if (item == null) {
            throw new BusinessException("Uživatel '" + user.getUsername() + "' není členem skupiny '" + group.getName() + "'",
                    UserCode.NOT_IN_GROUP).set("user", user.getUsername()).set("group", group.getName());
        }

        groupUserRepository.delete(item);
        invalidateCache(user);
        changeUserEvent(user);
        changeGroupEvent(group);
    }

    /**
     * Vytvoření skupiny.
     *
     * @param name název skupiny
     * @param code kód skupiny
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
    @AuthMethod(permission = {UsrPermission.Permission.USR_PERM})
    public UsrGroup deleteGroup(@NotNull final UsrGroup group) {

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
    @AuthMethod(permission = {UsrPermission.Permission.USR_PERM})
    public UsrGroup changeGroup(@NotNull final UsrGroup group,
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
     * @param user     upravovaný uživatel
     * @param username uživatelské jméno
     * @param password heslo (v plaintextu)
     * @return upravený uživatel
     */
    @AuthMethod(permission = {UsrPermission.Permission.USR_PERM})
    public UsrUser changeUser(@NotNull final UsrUser user,
                              @NotEmpty final String username,
                              @NotEmpty final String password) {

        /*String passwordDB = encodePassword(user.getUsername(), password);
        if (!passwordDB.equals(user.getPassword())) {
            throw new IllegalArgumentException("Neplatné heslo");
        }*/

        user.setUsername(username);
        user.setPassword(encodePassword(username, password));

        userRepository.save(user);

        changeUserEvent(user);
        return user;
    }

    /**
     * Vytvoření uživatele.
     *
     * @param username uživatelské jméno
     * @param password heslo (v plaintextu)
     * @param partyId  identifikátor osoby
     * @return vytvořený uživatel
     */
    @AuthMethod(permission = {UsrPermission.Permission.USR_PERM})
    public UsrUser createUser(@NotEmpty final String username,
                              @NotEmpty final String password,
                              @NotNull final Integer partyId) {

        ParParty party = partyService.getParty(partyId);
        if (party == null) {
            throw new BusinessException("Osoba neexistuje", RegistryCode.PARTY_NOT_EXIST);
        }

        UsrUser user = findByUsername(username);
        if (user != null) {
            throw new BusinessException("Uživatelské jméno již existuje", UserCode.USERNAME_EXISTS);
        }

        user = new UsrUser();
        user.setActive(true);
        user.setParty(party);
        user.setUsername(username);
        user.setPassword(encodePassword(username, password));

        userRepository.save(user);

        createUserEvent(user);
        return user;
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
            String oldPasswordHash = encodePassword(user.getUsername(), oldPassword);

            if (!oldPasswordHash.equals(user.getPassword())) {
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
    @AuthMethod(permission = {UsrPermission.Permission.USR_PERM})
    public UsrUser changePassword(@NotNull final UsrUser user,
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
        user.setPassword(encodePassword(user.getUsername(), newPassword));
        userRepository.save(user);
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
        return userRepository.findByUsername(username);
    }

    /**
     * Zahashování hesla.
     *
     * @param username uživatelské jméno
     * @param password uživatelské heslo v plaintextu
     * @return zahashované heslo
     */
    public String encodePassword(final String username, final String password) {
        return encoder.encodePassword(password, username + SALT);
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
        return userRepository.findByUsername(auth.getName());
    }

    /**
     * Vrací detail přihlášeního uživatele - VO.
     *
     * @return detail přihlášeného uživatele (null pokud není nikdo přihlášený)
     */
    public UserDetail getLoggedUserDetail() {
        synchronized (synchObj) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null) {
                return null;
            }
            UserDetail details = (UserDetail) auth.getDetails();
            UsrUser user = userRepository.findByUsername(details.getUsername());
            if (user != null) { // pokud je null, jedná se o virtuální admin účet, který má nastaveno oprávnění ADMIN
                try {
                    details.getUserPermission().clear();
                    details.getUserPermission().addAll(userPermissionsCache.get(user.getUserId()));
                } catch (ExecutionException e) {
                    throw new SystemException(e);
                }
            }
            return details;
        }
    }

    /**
     * Vypočítá oprávnění pro uživatele.
     *
     * @param user uživatel
     * @return seznam oprávnění
     */
    public Collection<UserPermission> calcUserPermission(final UsrUser user) {
        Map<UsrPermission.Permission, UserPermission> userPermissions = new HashMap<>();

        List<UsrPermission> permissions = permissionRepository.getAllPermissions(user);

        for (UsrPermission permission : permissions) {
            UserPermission userPermission = userPermissions.get(permission.getPermission());
            if (userPermission == null) {
                userPermission = new UserPermission(permission.getPermission());
                userPermissions.put(permission.getPermission(), userPermission);
            }

            if (permission.getFund() != null) {
                userPermission.addFundId(permission.getFund().getFundId());
            }

            if (permission.getScope() != null) {
                userPermission.addScopeId(permission.getScope().getScopeId());
            }

            if (permission.getGroupControl() != null) {
                userPermission.addControlGroupId(permission.getGroupControl().getGroupId());
            }

            if (permission.getUserControl() != null) {
                userPermission.addControlUserId(permission.getUserControl().getUserId());
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
    public Collection<UserPermission> getUserPermission() {
        UserDetail userDetail = getLoggedUserDetail();
        if (userDetail == null) {
            return new ArrayList<>();
        }
        return userDetail.getUserPermission();
    }


    /**
     * Kontroluje oprávnění přihlášeného uživatele.
     *
     * @param permission typ oprávnění
     * @param entityId   identifikátor entity, ke které se ověřuje oprávnění
     * @return má oprávnění?
     */
    public boolean hasPermission(final UsrPermission.Permission permission,
                                 final Integer entityId) {
        for (UserPermission userPermission : getUserPermission()) {
            if (userPermission.getPermission().equals(permission)) {
                switch (permission.getType()) {
                    case FUND:
                        if (userPermission.getFundIds().contains(entityId)) {
                            return true;
                        }
                        break;
                    case USER:
                        if (userPermission.getControlUserIds().contains(entityId)) {
                            return true;
                        }
                        break;
                    case GROUP:
                        if (userPermission.getControlGroupIds().contains(entityId)) {
                            return true;
                        }
                        break;
                    case SCOPE:
                        if (userPermission.getScopeIds().contains(entityId)) {
                            return true;
                        }
                        break;
                    default:
                        throw new IllegalStateException(permission.getType().toString());
                }
                break;
            }
        }
        return false;
    }

    /**
     * Kontroluje oprávnění přihlášeného uživatele.
     *
     * @param permission typ oprávnění
     * @return má oprávnění?
     */
    public boolean hasPermission(final UsrPermission.Permission permission) {
        for (UserPermission userPermission : getUserPermission()) {
            if (userPermission.getPermission().equals(permission) || userPermission.getPermission().equals(UsrPermission.Permission.ADMIN)) {
                return true;
            }
        }
        return false;
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
    @AuthMethod(permission = {UsrPermission.Permission.USR_PERM})
    public FilteredResult<UsrUser> findUser(final String search, final Boolean active, final Boolean disabled, final Integer firstResult, final Integer maxResults, final Integer excludedGroupId) {
        if (!active && !disabled) {
            throw new IllegalArgumentException("Musí být uveden alespoň jeden z parametrů: active, disabled.");
        }

        return userRepository.findUserByTextAndStateCount(search, active, disabled, firstResult, maxResults, excludedGroupId);
    }

    /**
     * Hledání skupin na základě podmínek.
     *
     * @param search      hledaný text
     * @param firstResult od jakého záznamu
     * @param maxResults  maximální počet vrácených záznamů
     * @return výsledky hledání
     */
    @AuthMethod(permission = {UsrPermission.Permission.USR_PERM})
    public FilteredResult<UsrGroup> findGroup(final String search, final Integer firstResult, final Integer maxResults) {
        return groupRepository.findGroupByTextCount(search, firstResult, maxResults);
    }

    /**
     * Načtení objektu uživatele dle id.
     *
     * @param userId id
     * @return objekt
     */
    @AuthMethod(permission = {UsrPermission.Permission.USR_PERM})
    public UsrUser getUser(final Integer userId) {
        Assert.notNull(userId, "Identifikátor uživatele musí být vyplněno");
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
        List<UsrUser> users = userRepository.findAll(userIds);
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
        List<UsrGroup> groups = groupRepository.findAll(groupIds);
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
    @AuthMethod(permission = {UsrPermission.Permission.USR_PERM})
    public UsrGroup getGroup(final Integer groupId) {
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
    @AuthMethod(permission = {UsrPermission.Permission.USR_PERM})
    public UsrUser changeActive(@NotNull final UsrUser user,
                                @NotNull final Boolean active) {
        user.setActive(active);
        userRepository.save(user);

        changeUserEvent(user);
        return user;
    }

    /**
     * Vyhledá list uživatelů podle osoby
     * @param party osoba
     * @return list uživatelů
     */
    public List<UsrUser> findUsersByParty(final ParParty party) {
        return userRepository.findByParty(party);
    }

    /**
     * Event změněného uživatele.
     *
     * @param user uživatel
     */
    private void changeUserEvent(final UsrUser user) {
        eventNotificationService.publishEvent(new EventId(EventType.USER_CHANGE, user.getUserId()));
    }

    /**
     * Event změněných uživatelů.
     *
     * @param users uživatelé
     */
    private void changeUsersEvent(final Set<UsrUser> users) {
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
    private void changeGroupsEvent(final Set<UsrGroup> groups) {
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
     * @param fund archivní souboru
     */
    public void deleteByFund(final ArrFund fund) {
        permissionRepository.deleteByFund(fund);
    }

    /**
     * Vyhledá uživatele na základě id a vytvoří mapu.
     *
     * @param userIds hledaní uživatelé
     * @return mapa uživatelů
     */
    public Map<Integer, UsrUser> findUserMap(final Collection<Integer> userIds) {
        List<UsrUser> users = userRepository.findAll(userIds);
        return users.stream().collect(Collectors.toMap(UsrUser::getUserId, Function.identity()));
    }

    /**
     * Vrátí id scope na které ma uživatel právo. Nebere v úvahu právo {@link UsrPermission.Permission#REG_SCOPE_RD_ALL}.
     *
     * @return množina id scope na které ma uživatel právo
     */
    public Set<Integer> getUserScopeIds() {
        return getUserPermission()
                .stream()
                .filter(p -> p.getPermission() == UsrPermission.Permission.REG_SCOPE_RD)
                .findFirst()
                .map(p -> new HashSet<>(p.getScopeIds()))
                .orElse(new HashSet<>());
    }
}
