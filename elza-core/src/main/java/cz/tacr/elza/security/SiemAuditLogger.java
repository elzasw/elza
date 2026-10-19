package cz.tacr.elza.security;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import jakarta.annotation.PostConstruct;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * Security audit log for a SIEM system. Field names follow the OSSEC/Wazuh schema
 * (srcuser, dstuser, srcip, ...) so that the built-in SIEM rules can use them.
 */
@Component
public class SiemAuditLogger {
	private static final Logger LOG = LoggerFactory.getLogger("siem.audit");

	/** Authentication events. */
	private static final String LOG_TYPE_SECURE = "secure";
	/** Changes of user accounts and their access. */
	private static final String LOG_TYPE_AUDIT = "audit";

	private static final String RESULT_SUCCESS = "success";
	private static final String RESULT_FAILED = "failed";

	/** srcuser of changes made at startup from the configuration (elza.security.override). */
	private static final String CONFIGURATION_USER = "configuration";

	/** srcuser of an action of the system itself, e.g. a user created on the first JWT login. */
	public static final String SYSTEM_USER = "system";
	/** srcuser of the first administrator created in the initial setup dialog. */
	private static final String SETUP_USER = "setup";

	/** How long a repeated success of a stateless authentication (JWT, API key) is not logged again. */
	private static final long STATELESS_SUCCESS_INTERVAL_HOURS = 1;

	/**
	 * Credentials with the client address whose success was logged within the interval. Stateless
	 * authentication runs on every request; without this every request of an integration would be
	 * a login event. Kept in memory, per node.
	 */
	private final Cache<String, Boolean> loggedStatelessSuccesses = CacheBuilder.newBuilder()
			.expireAfterWrite(STATELESS_SUCCESS_INTERVAL_HOURS, TimeUnit.HOURS)
			.maximumSize(10_000)
			.build();

	// Get the property, defaulting to an empty string if not found
    @Value("${elza.siemLogFile:}")
    private String siemLogPath;

    public enum AuthenticationType {
		PASSWORD,
		JWT,
		SSO_HEADER,
		// MS Active Directory - direct password authentication
		ACTIVE_DIRECTORY,
		// Kerberos based authentication - suitable for SSO
		KERBEROS,
		// Personal API key sent in the X-API-Key header
		API_KEY,
		// Password from elza.security.override, accepted next to the user's own
		OVERRIDE
	}

	@PostConstruct
    public void init() {
        if (StringUtils.isEmpty(siemLogPath)) {
            // Programmatically disable the "siem.audit" logger
        	var ctx = LoggerFactory.getILoggerFactory();
        	if(ctx instanceof LoggerContext) {
                LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
                ch.qos.logback.classic.Logger logbackLogger = context.getLogger("siem.audit");
                logbackLogger.setLevel(Level.OFF);

        	}
        }
    }

	public void loginSuccess(String user, String sourceIp, AuthenticationType authenticationType) {
		if (authenticationType == AuthenticationType.JWT && !firstStatelessSuccess("JWT:" + user, sourceIp)) {
			return;
		}
		var builder = event(LOG_TYPE_SECURE, "user_login", "login", RESULT_SUCCESS,
				"User " + user + " logged in"
						+ (authenticationType == AuthenticationType.OVERRIDE ? " with the configuration password" : "")
						+ fromIp(sourceIp) + ".", user, user, sourceIp);
		addIfNotNull(builder, "authentication_type", authenticationType);
		builder.log();
	}

	public void loginFailed(String username, String sourceIp, AuthenticationType authenticationType,
			String reason, String details) {
		var builder = event(LOG_TYPE_SECURE, "user_login", "login", RESULT_FAILED,
				"User " + username + " failed to log in" + fromIp(sourceIp) + ".", username, username, sourceIp);
		addIfNotNull(builder, "authentication_type", authenticationType);
		addIfNotNull(builder, "reason", reason);
		addIfNotNull(builder, "details", details);
		builder.log();
	}

	public void logout(String user, String sourceIp) {
		event(LOG_TYPE_SECURE, "user_logout", "logout", RESULT_SUCCESS,
				"User " + user + " logged out" + fromIp(sourceIp) + ".", user, user, sourceIp)
				.log();
	}

	/** Password was changed by the user themselves or by an administrator (actor). */
	public void passwordChanged(String actor, String user) {
		String msg = Objects.equals(actor, user)
				? "User " + user + " changed their password."
				: actor + " changed the password of user " + user + ".";
		event(LOG_TYPE_SECURE, "password_change", "change_password", RESULT_SUCCESS, msg, actor, user, null)
				.log();
	}

	/** The user failed to change their own password, e.g. the old password did not match. */
	public void passwordChangeFailed(String user, String reason) {
		event(LOG_TYPE_SECURE, "password_change", "change_password", RESULT_FAILED,
				"User " + user + " failed to change their password.", user, user, null)
				.addArgument(kv("reason", reason))
				.log();
	}

	/** A user was created by an administrator, an integration or the system (actor). */
	public void userCreated(String actor, String user) {
		event(LOG_TYPE_AUDIT, "user_create", "create_user", RESULT_SUCCESS,
				actor + " created user " + user + ".", actor, user, null)
				.log();
	}

	/** The first administrator was created in the initial setup dialog (nobody is logged in yet). */
	public void initialAdminCreated(String user) {
		event(LOG_TYPE_AUDIT, "user_create", "create_user", RESULT_SUCCESS,
				"Initial setup created administrator " + user + ".", SETUP_USER, user, null)
				.log();
	}

	/** A user was renamed by an administrator (actor); dstuser is the new name. */
	public void userRenamed(String actor, String oldUsername, String newUsername) {
		event(LOG_TYPE_AUDIT, "user_rename", "rename_user", RESULT_SUCCESS,
				actor + " renamed user " + oldUsername + " to " + newUsername + ".", actor, newUsername, null)
				.addArgument(kv("old_username", oldUsername))
				.log();
	}

	/** A user was activated or deactivated by an administrator or an integration (actor). */
	public void userActiveChanged(String actor, String user, boolean active) {
		event(LOG_TYPE_AUDIT, active ? "user_activate" : "user_disable",
				active ? "activate_user" : "disable_user", RESULT_SUCCESS,
				actor + (active ? " activated user " : " deactivated user ") + user + ".", actor, user, null)
				.log();
	}

	/** Kind of a change of a permission of a user or a group. */
	public enum PermissionAction {
		GRANT("permission_grant", "grant_permission", "granted", "to"),
		REVOKE("permission_revoke", "revoke_permission", "revoked", "from"),
		CHANGE("permission_change", "change_permission", "changed", "of");

		private final String eventType;
		private final String action;
		private final String verb;
		private final String preposition;

		PermissionAction(final String eventType, final String action, final String verb, final String preposition) {
			this.eventType = eventType;
			this.action = action;
			this.verb = verb;
			this.preposition = preposition;
		}
	}

	/**
	 * A permission of a user (user set) or of a group (groupCode set) was changed; target is null
	 * for a global permission.
	 */
	public void permissionChanged(String actor, PermissionAction action, String user, String groupCode,
			String permission, String target) {
		String holder = user != null ? "user " + user : "group " + groupCode;
		var builder = event(LOG_TYPE_AUDIT, action.eventType, action.action, RESULT_SUCCESS,
				actor + " " + action.verb + " " + permission + (target != null ? " on " + target : "")
						+ " " + action.preposition + " " + holder + ".", actor, user, null)
				.addArgument(kv("permission", permission));
		addIfNotNull(builder, "dstgroup", groupCode);
		addIfNotNull(builder, "permission_target", target);
		builder.log();
	}

	public void groupMemberAdded(String actor, String user, String groupCode) {
		event(LOG_TYPE_AUDIT, "group_member_add", "add_group_member", RESULT_SUCCESS,
				actor + " added user " + user + " to group " + groupCode + ".", actor, user, null)
				.addArgument(kv("dstgroup", groupCode))
				.log();
	}

	public void groupMemberRemoved(String actor, String user, String groupCode) {
		event(LOG_TYPE_AUDIT, "group_member_remove", "remove_group_member", RESULT_SUCCESS,
				actor + " removed user " + user + " from group " + groupCode + ".", actor, user, null)
				.addArgument(kv("dstgroup", groupCode))
				.log();
	}

	public void groupCreated(String actor, String groupCode) {
		event(LOG_TYPE_AUDIT, "group_create", "create_group", RESULT_SUCCESS,
				actor + " created group " + groupCode + ".", actor, null, null)
				.addArgument(kv("dstgroup", groupCode))
				.log();
	}

	/** A group was deleted; its members lose the permissions of the group. */
	public void groupDeleted(String actor, String groupCode) {
		event(LOG_TYPE_AUDIT, "group_delete", "delete_group", RESULT_SUCCESS,
				actor + " deleted group " + groupCode + ".", actor, null, null)
				.addArgument(kv("dstgroup", groupCode))
				.log();
	}

	/** A fund was created, by hand (UI, SOAP) or by an import; written after the transaction commits. */
	public void fundCreated(String actor, Integer fundId, String fundName, boolean imported) {
		afterCommit(() -> event(LOG_TYPE_AUDIT, "fund_create", "create_fund", RESULT_SUCCESS,
				actor + (imported ? " imported fund " : " created fund ") + fundId + " (" + fundName + ").",
				actor, null, null)
				.addArgument(kv("fund_id", fundId))
				.addArgument(kv("fund_name", fundName))
				.addArgument(kv("fund_import", imported))
				.log());
	}

	/** A fund was deleted with all its data; written after the transaction commits. */
	public void fundDeleted(String actor, Integer fundId, String fundName) {
		afterCommit(() -> event(LOG_TYPE_AUDIT, "fund_delete", "delete_fund", RESULT_SUCCESS,
				actor + " deleted fund " + fundId + " (" + fundName + ").", actor, null, null)
				.addArgument(kv("fund_id", fundId))
				.addArgument(kv("fund_name", fundName))
				.log());
	}

	/** The history (old versions and changes) of a fund was deleted; written after the transaction commits. */
	public void fundHistoryDeleted(String actor, Integer fundId, String fundName) {
		afterCommit(() -> event(LOG_TYPE_AUDIT, "fund_history_delete", "delete_fund_history", RESULT_SUCCESS,
				actor + " deleted the history of fund " + fundId + " (" + fundName + ").", actor, null, null)
				.addArgument(kv("fund_id", fundId))
				.addArgument(kv("fund_name", fundName))
				.log());
	}

	/** Runs the write after the current transaction commits; at once when there is no transaction. */
	static void afterCommit(Runnable write) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					write.run();
				}
			});
		} else {
			write.run();
		}
	}

	/** Successful API-key authentication — includes the public key id. */
	public void apiKeyLoginSuccess(String user, String sourceIp, String keyId) {
		if (!firstStatelessSuccess("API_KEY:" + keyId, sourceIp)) {
			return;
		}
		event(LOG_TYPE_SECURE, "user_login", "login", RESULT_SUCCESS,
				"User " + user + " logged in with API key " + keyId + fromIp(sourceIp) + ".", user, user, sourceIp)
				.addArgument(kv("authentication_type", AuthenticationType.API_KEY))
				.addArgument(kv("key_id", keyId))
				.log();
	}

	/** Failed API-key authentication — the user is unknown; keyId is null for MALFORMED_TOKEN. */
	public void apiKeyLoginFailed(String sourceIp, String keyId, String detail) {
		var builder = event(LOG_TYPE_SECURE, "user_login", "login", RESULT_FAILED,
				"API key authentication failed" + fromIp(sourceIp) + ".", null, null, sourceIp)
				.addArgument(kv("authentication_type", AuthenticationType.API_KEY));
		addIfNotNull(builder, "key_id", keyId);
		addIfNotNull(builder, "reason", detail);
		builder.log();
	}

	/** JWT could not be decoded or validated (bad signature, expired, ...); the user is unknown. */
	public void jwtLoginFailed(String sourceIp, String reason, String details) {
		var builder = event(LOG_TYPE_SECURE, "user_login", "login", RESULT_FAILED,
				"JWT authentication failed" + fromIp(sourceIp) + ".", null, null, sourceIp)
				.addArgument(kv("authentication_type", AuthenticationType.JWT))
				.addArgument(kv("reason", reason));
		addIfNotNull(builder, "details", details);
		builder.log();
	}

	/** API key was created. Actor is the person who logged in; owner is the key's user. */
	public void apiKeyCreated(String actor, String owner, String keyId, OffsetDateTime expireDate) {
		event(LOG_TYPE_AUDIT, "api_key_create", "create_api_key", RESULT_SUCCESS,
				actor + " created API key " + keyId + " for user " + owner + ".", actor, owner, null)
				.addArgument(kv("key_id", keyId))
				.addArgument(kv("expire_date", expireDate.toString()))
				.log();
	}

	/** API key was revoked. Actor is the person who revoked it (owner or an admin). */
	public void apiKeyRevoked(String actor, String owner, String keyId) {
		event(LOG_TYPE_AUDIT, "api_key_revoke", "revoke_api_key", RESULT_SUCCESS,
				actor + " revoked API key " + keyId + " of user " + owner + ".", actor, owner, null)
				.addArgument(kv("key_id", keyId))
				.log();
	}

	/**
	 * Change of a user made at startup from the configuration (elza.security.override).
	 */
	public enum ConfigAdminAction {
		USER_CREATED(LOG_TYPE_AUDIT, "user_create", "create_user", "created user"),
		USER_ACTIVATED(LOG_TYPE_AUDIT, "user_activate", "activate_user", "activated user");

		private final String logType;
		private final String eventType;
		private final String action;
		private final String description;

		ConfigAdminAction(final String logType, final String eventType, final String action,
				final String description) {
			this.logType = logType;
			this.eventType = eventType;
			this.action = action;
			this.description = description;
		}
	}

	/** A user was changed from the configuration; the actor is the configuration itself. */
	public void configAdminApplied(String user, ConfigAdminAction action) {
		event(action.logType, action.eventType, action.action, RESULT_SUCCESS,
				"Configuration " + action.description + " " + user + ".", CONFIGURATION_USER, user, null)
				.log();
	}

	/** The configuration (elza.security.override) gives the user a password valid next to their own. */
	public void overrideActive(String user) {
		event(LOG_TYPE_SECURE, "override_active", "enable_override", RESULT_SUCCESS,
				"User " + user + " has a configuration password.",
				CONFIGURATION_USER, user, null)
				.log();
	}

	/** A user from the configuration (elza.security.override) could not be applied. */
	public void configAdminFailed(String user, String detail) {
		event(LOG_TYPE_AUDIT, "config_admin", "apply_config_admin", RESULT_FAILED,
				"Configuration failed to set up user " + user + ".", CONFIGURATION_USER, user, null)
				.addArgument(kv("reason", detail))
				.log();
	}

	/**
	 * Starts an event with the fields common to all events; null srcuser/dstuser are omitted.
	 * Without sourceIp the address of the current HTTP request is used, if there is one.
	 */
	private static LoggingEventBuilder event(String logType, String eventType, String action, String result,
			String msg, String srcUser, String dstUser, String sourceIp) {
		var builder = LOG.atInfo().setMessage(msg)
				.addArgument(kv("log_type", logType))
				.addArgument(kv("event_type", eventType))
				.addArgument(kv("action", action))
				.addArgument(kv("result", result));
		addIfNotNull(builder, "srcuser", srcUser);
		addIfNotNull(builder, "dstuser", dstUser);
		addIfNotNull(builder, "srcip", sourceIp != null ? sourceIp : currentRequestIp());
		return builder;
	}

	/** Client address of the current HTTP request; null outside a request (startup, background job). */
	private static String currentRequestIp() {
		return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs
				? attrs.getRequest().getRemoteAddr()
				: null;
	}

	/** True when this credential has not been logged from this address within the interval. */
	boolean firstStatelessSuccess(String credential, String sourceIp) {
		return loggedStatelessSuccesses.asMap().putIfAbsent(credential + "|" + sourceIp, Boolean.TRUE) == null;
	}

	private static void addIfNotNull(LoggingEventBuilder builder, String key, Object value) {
		if (value != null) {
			builder.addArgument(kv(key, value));
		}
	}

	private static String fromIp(String sourceIp) {
		return sourceIp != null ? " from IP " + sourceIp : "";
	}
}
