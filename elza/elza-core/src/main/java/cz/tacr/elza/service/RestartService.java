package cz.tacr.elza.service;

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.security.UserDetail;

/**
 * Restart of the application from the administration.
 *
 * A Java process cannot restart itself, and the application cannot rebuild its Spring context in
 * one process, so the restart is an exit with the configured code: the service manager (systemd
 * with {@code Restart=on-failure}, a Windows service wrapper) starts the application again. The
 * exit closes the context, so the schedulers and queues stop as at a normal shutdown. Without the
 * key the restart is not offered.
 */
@Service
public class RestartService {

    private static final Logger logger = LoggerFactory.getLogger(RestartService.class);

    public static final String EXIT_CODE_KEY = "elza.restart.exitCode";

    /** Time for the response of the request to reach the client before the context closes. */
    private static final long EXIT_DELAY_MS = 1000;

    @Value("${elza.restart.exitCode:#{null}}")
    private Integer exitCode;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private UserService userService;

    private final AtomicBoolean requested = new AtomicBoolean();

    /** The restart can be requested: the exit code is configured. */
    public boolean isAvailable() {
        return exitCode != null;
    }

    /**
     * Exits the application with the configured code after a short delay. A second request while
     * the exit is pending does nothing.
     */
    public void restart() {
        if (exitCode == null) {
            throw new BusinessException("Restart z administrace není nastaven, chybí klíč " + EXIT_CODE_KEY,
                    BaseCode.INVALID_STATE).set("key", EXIT_CODE_KEY);
        }
        if (!requested.compareAndSet(false, true)) {
            logger.warn("Restart already requested, the application is exiting");
            return;
        }
        UserDetail userDetail = userService.getLoggedUserDetail();
        String user = userDetail != null ? userDetail.getUsername() : null;
        logger.warn("Restart requested by user {}: the application exits with code {} in {} ms", user, exitCode,
                    EXIT_DELAY_MS);
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(EXIT_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            int code = SpringApplication.exit(applicationContext, () -> exitCode);
            System.exit(code);
        }, "elza-restart");
        thread.setDaemon(false);
        thread.start();
    }
}
