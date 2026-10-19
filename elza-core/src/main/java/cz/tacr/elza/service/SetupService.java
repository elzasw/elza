package cz.tacr.elza.service;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.Level;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.UserCode;
import cz.tacr.elza.repository.UserRepository;
import cz.tacr.elza.security.OverrideProperties;
import cz.tacr.elza.security.OverrideProperties.OverrideUser;
import cz.tacr.elza.security.SiemAuditLogger;
import cz.tacr.elza.security.SiemAuditLogger.ConfigAdminAction;

/**
 * First-run setup and the users from the configuration.
 *
 * While the database contains no user, the setup dialog creates the first administrator;
 * it is available without login, to anyone, until the first user exists. Independently,
 * the users configured in elza.security.override are applied at every startup.
 */
@Service
public class SetupService {

    private static final Logger logger = LoggerFactory.getLogger(SetupService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    @Qualifier("transactionManager")
    private PlatformTransactionManager txManager;

    @Autowired
    private SiemAuditLogger siemAuditLogger;

    @Autowired
    private OverrideProperties overrideProperties;

    @Autowired
    private PasswordPolicyService passwordPolicyService;

    // only to detect the removed configuration
    @Value("${elza.security.admin.username:#{null}}")
    private String legacyAdminUsername;

    @Value("${elza.security.admin.password:#{null}}")
    private String legacyAdminPassword;

    /**
     * What applying a user from the configuration changed.
     */
    private record OverrideResult(boolean created, boolean activated) {
    }

    /**
     * Applies elza.security.override at every startup: a missing user is created as an
     * administrator without a password in the database, a deactivated one is activated.
     * A configuration error (also the removed elza.security.admin.*) stops the startup;
     * a failure of one user is logged and the others are applied.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void applyOverrideFromConfiguration() {
        if (legacyAdminUsername != null || legacyAdminPassword != null) {
            throw new IllegalStateException("Volba elza.security.admin již není podporována, "
                    + "použijte elza.security.override (seznam username/password).");
        }
        overrideProperties.validate();
        for (OverrideUser user : overrideProperties.getOverride()) {
            applyOverrideUser(user.getUsername().trim(), user.getPassword());
        }
    }

    private void applyOverrideUser(final String username, final String password) {
        logger.warn("Pro uživatele {} platí heslo z konfigurace (elza.security.override).", username);
        siemAuditLogger.overrideActive(username);
        try {
            OverrideResult result;
            // same lock as createAdmin, so it cannot race with the setup dialog
            synchronized (this) {
                result = new TransactionTemplate(txManager).execute(tx -> {
                    warnIfPolicyViolated(username, password);
                    UsrUser user = userRepository.findByUsername(username);
                    if (user == null) {
                        userService.createInitialAdmin(username, null);
                        return new OverrideResult(true, false);
                    }
                    boolean activated = !Boolean.TRUE.equals(user.getActive());
                    if (activated) {
                        user.setActive(true);
                        userRepository.save(user);
                    }
                    return new OverrideResult(false, activated);
                });
            }
            if (result.created()) {
                logger.info("Administrátor {} byl vytvořen z konfigurace (elza.security.override).", username);
                siemAuditLogger.configAdminApplied(username, ConfigAdminAction.USER_CREATED);
            }
            if (result.activated()) {
                logger.info("Uživatel {} byl aktivován podle konfigurace (elza.security.override).", username);
                siemAuditLogger.configAdminApplied(username, ConfigAdminAction.USER_ACTIVATED);
            }
        } catch (RuntimeException e) {
            logger.error("Uživatele {} z konfigurace (elza.security.override) se nepodařilo použít: {}",
                         username, e.getMessage(), e);
            siemAuditLogger.configAdminFailed(username, e.getMessage());
        }
    }

    /** The password is not stored, so the policy is not enforced; a violation is only reported. */
    private void warnIfPolicyViolated(final String username, final String password) {
        try {
            passwordPolicyService.validate(password);
        } catch (BusinessException e) {
            logger.warn("Heslo uživatele {} v elza.security.override nesplňuje pravidla pro hesla.", username);
        }
    }

    /**
     * Whether the first administrator has to be created: the database contains no user.
     */
    public boolean isSetupRequired() {
        return userRepository.count() == 0;
    }

    /**
     * Creates the first administrator.
     *
     * The check that no user exists and the creation run under one lock and in one
     * transaction committed before the lock is released, so concurrent requests cannot
     * create two administrators.
     *
     * @param username username of the administrator
     * @param password password of the administrator (plaintext)
     */
    public synchronized void createAdmin(final String username, final String password) {
        new TransactionTemplate(txManager).executeWithoutResult(tx -> {
            if (!isSetupRequired()) {
                throw new BusinessException("Úvodní nastavení již bylo dokončeno", UserCode.SETUP_NOT_AVAILABLE)
                        .level(Level.WARNING);
            }
            if (StringUtils.isBlank(username)) {
                throw new BusinessException("Je nutné zadat uživatelské jméno", BaseCode.PROPERTY_NOT_EXIST)
                        .set("property", "username");
            }
            userService.createInitialAdmin(username.trim(), password);
        });
        logger.info("Úvodní nastavení: vytvořen první administrátor {}.", username.trim());
        siemAuditLogger.initialAdminCreated(username.trim());
    }
}
