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
import cz.tacr.elza.security.SiemAuditLogger;
import cz.tacr.elza.security.SiemAuditLogger.ConfigAdminAction;

/**
 * First-run setup and the administrator from the configuration.
 *
 * While the database contains no user, the setup dialog creates the first administrator;
 * it is available without login, to anyone, until the first user exists. Independently,
 * the administrator configured in elza.security.admin.* is applied at every startup.
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

    @Value("${elza.security.admin.username:#{null}}")
    private String configAdminUsername;

    @Value("${elza.security.admin.password:#{null}}")
    private String configAdminPassword;

    /**
     * What applying the configured administrator changed.
     */
    private record ConfigAdminResult(boolean created, boolean passwordChanged, boolean activated) {
    }

    /**
     * Applies the administrator from the configuration (elza.security.admin.*) at every
     * startup: a missing user is created as an administrator; an existing user gets the
     * configured password when it differs and is activated when it is inactive.
     *
     * Every outcome is logged (never the password); the changes and a failure also go to
     * the security audit log. A failure does not stop the startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void applyAdminFromConfiguration() {
        boolean hasUsername = StringUtils.isNotBlank(configAdminUsername);
        boolean hasPassword = StringUtils.isNotEmpty(configAdminPassword);
        if (!hasUsername && !hasPassword) {
            return;
        }
        if (!hasUsername || !hasPassword) {
            logger.warn("Volba elza.security.admin není úplná (chybí {}), nepoužije se.",
                        hasUsername ? "password" : "username");
            return;
        }
        String username = configAdminUsername.trim();
        logger.info("Používá se administrátor z konfigurace (elza.security.admin): {}.", username);
        try {
            ConfigAdminResult result;
            // same lock as createAdmin, so it cannot race with the setup dialog
            synchronized (this) {
                result = new TransactionTemplate(txManager).execute(tx -> {
                    UsrUser user = userRepository.findByUsername(username);
                    if (user == null) {
                        userService.createInitialAdmin(username, configAdminPassword);
                        return new ConfigAdminResult(true, false, false);
                    }
                    boolean passwordChanged = userService.applyConfiguredPassword(user, configAdminPassword);
                    boolean activated = !Boolean.TRUE.equals(user.getActive());
                    if (activated) {
                        user.setActive(true);
                        userRepository.save(user);
                    }
                    return new ConfigAdminResult(false, passwordChanged, activated);
                });
            }
            if (result.created()) {
                logger.info("Administrátor {} byl vytvořen z konfigurace (elza.security.admin).", username);
                siemAuditLogger.configAdminApplied(username, ConfigAdminAction.USER_CREATED);
            }
            if (result.passwordChanged()) {
                logger.info("Heslo uživatele {} bylo nastaveno z konfigurace (elza.security.admin).", username);
                siemAuditLogger.configAdminApplied(username, ConfigAdminAction.PASSWORD_SET);
            }
            if (result.activated()) {
                logger.info("Uživatel {} byl aktivován podle konfigurace (elza.security.admin).", username);
                siemAuditLogger.configAdminApplied(username, ConfigAdminAction.USER_ACTIVATED);
            }
            if (!result.created() && !result.passwordChanged() && !result.activated()) {
                logger.info("Uživatel {} odpovídá konfiguraci (elza.security.admin), beze změny.", username);
            }
        } catch (RuntimeException e) {
            logger.error("Administrátora {} z konfigurace (elza.security.admin) se nepodařilo použít: {}",
                         username, e.getMessage(), e);
            siemAuditLogger.configAdminFailed(username, e.getMessage());
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
    }
}
