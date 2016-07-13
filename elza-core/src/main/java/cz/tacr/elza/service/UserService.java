package cz.tacr.elza.service;

import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.UsrGroup;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.repository.FilteredResult;
import cz.tacr.elza.repository.GroupRepository;
import cz.tacr.elza.repository.PermissionRepository;
import cz.tacr.elza.repository.UserRepository;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.security.UserPermission;
import cz.tacr.elza.service.eventnotification.events.EventId;
import cz.tacr.elza.service.eventnotification.events.EventType;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.encoding.ShaPasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private PermissionRepository permissionRepository;

    @Autowired
    private PartyService partyService;

    @Autowired
    private IEventNotificationService eventNotificationService;

    @Value("${elza.security.salt:kdFss=+4Df_%}")
    private String SALT;

    private ShaPasswordEncoder encoder = new ShaPasswordEncoder(256);

    /**
     * Vytvoření uživatele.
     *
     * @param username uživatelské jméno
     * @param password heslo (v plaintextu)
     * @param partyId  identifikátor osoby
     * @return vytvořený uživatel
     */
    public UsrUser createUser(@NotEmpty final String username,
                              @NotEmpty final String password,
                              @NotNull final Integer partyId) {

        ParParty party = partyService.getParty(partyId);
        if (party == null) {
            throw new IllegalArgumentException("Osoba neexistuje");
        }

        UsrUser user = findByUsername(username);
        if (user != null) {
            throw new IllegalArgumentException("Uživatelské jméno již existuje");
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
                                  @Nullable final String oldPassword,
                                  @NotEmpty final String newPassword) {
        if (oldPassword != null) {
            String oldPasswordHash = encodePassword(user.getUsername(), oldPassword);

            if (!oldPasswordHash.equals(user.getPassword())) {
                throw new IllegalArgumentException("Původní heslo se neshoduje");
            }
        }

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
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return null;
        }
        return (UserDetail) auth.getDetails();
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
        }

        return userPermissions.values();
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
    public boolean hasPermission(UsrPermission.Permission permission) {
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
    public FilteredResult<UsrUser> findUser(final String search, final Boolean active, final Boolean disabled, final Integer firstResult, final Integer maxResults) {
        if (!active && !disabled) {
            throw new IllegalArgumentException("Musí být uveden alespoň jeden z parametrů: active, disabled.");
        }

        return userRepository.findUserByTextAndStateCount(search, active, disabled, firstResult, maxResults);
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
        return groupRepository.findGroupByTextCount(search, firstResult, maxResults);
    }

    /**
     * Načtení objektu uživatele dle id.
     *
     * @param userId id
     * @return objekt
     */
    public UsrUser getUser(final Integer userId) {
        Assert.notNull(userId);
        return userRepository.getOneCheckExist(userId);
    }

    /**
     * Načtení objektu skupiny dle id.
     *
     * @param groupId id
     * @return objekt
     */
    public UsrGroup getGroup(final Integer groupId) {
        Assert.notNull(groupId);
        return groupRepository.getOneCheckExist(groupId);
    }

    /**
     * Aktivace/deaktivace uživatele.
     *
     * @param user   upravovaný uživatel
     * @param active je aktivní?
     * @return uživatel
     */
    public UsrUser changeActive(@NotNull final UsrUser user,
                                @NotNull final Boolean active) {
        user.setActive(active);
        userRepository.save(user);

        changeUserEvent(user);
        return user;
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
     * Event vytvořeného uživatele.
     *
     * @param user uživatel
     */
    private void createUserEvent(final UsrUser user) {
        eventNotificationService.publishEvent(new EventId(EventType.USER_CREATE, user.getUserId()));
    }

}
