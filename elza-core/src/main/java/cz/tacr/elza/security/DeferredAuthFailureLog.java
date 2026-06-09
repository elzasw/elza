package cz.tacr.elza.security;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

/**
 * Collects authentication-provider failures for the duration of a single
 * authentication attempt and decides how to log them based on the overall outcome.
 *
 * In a multi-provider chain a single provider failing is normal - the next
 * provider may still authenticate the request. Logging every such failure at
 * ERROR floods the log (e.g. Kerberos password validation failing for users that
 * exist only in the local database and log in with a password every minute).
 * Failures are therefore buffered and flushed once the whole chain has finished:
 * <ul>
 *   <li>the chain authenticated the request - failures are logged at DEBUG, so
 *       they stay available for Kerberos debugging but are silent in normal operation,</li>
 *   <li>the chain rejected the request - failures are logged at ERROR with the full
 *       stack trace, because the user could not be authenticated by any means.</li>
 * </ul>
 *
 * Buffering is bound to the current thread and is active only between {@link #start()}
 * and {@link #clear()}, which {@link DeferredFailureAuthenticationManager} brackets
 * around the provider chain. When no buffer is active (authentication triggered
 * outside the wrapped manager) failures are logged immediately at ERROR.
 */
public final class DeferredAuthFailureLog {

    private record DeferredFailure(Logger log, String message, Throwable cause) {
    }

    private static final ThreadLocal<List<DeferredFailure>> BUFFER = new ThreadLocal<>();

    private DeferredAuthFailureLog() {
    }

    /**
     * Open a buffer for the current thread. Failures reported afterwards are deferred
     * until {@link #flushAsSuccess()} or {@link #flushAsFailure()} is called.
     */
    public static void start() {
        BUFFER.set(new ArrayList<>());
    }

    /**
     * Report a provider failure. If a buffer is active the failure is deferred until
     * the overall outcome is known, otherwise it is logged immediately at ERROR.
     */
    public static void defer(final Logger log, final String message, final Throwable cause) {
        List<DeferredFailure> buffer = BUFFER.get();
        if (buffer == null) {
            log.error(message, cause);
        } else {
            buffer.add(new DeferredFailure(log, message, cause));
        }
    }

    /**
     * The authentication chain succeeded - buffered failures are only interesting for
     * debugging, so they are logged at DEBUG.
     */
    public static void flushAsSuccess() {
        List<DeferredFailure> buffer = BUFFER.get();
        if (buffer == null) {
            return;
        }
        for (DeferredFailure f : buffer) {
            if (f.log().isDebugEnabled()) {
                f.log().debug(f.message(), f.cause());
            }
        }
    }

    /**
     * The authentication chain failed - buffered failures are surfaced at ERROR with the
     * full stack trace.
     */
    public static void flushAsFailure() {
        List<DeferredFailure> buffer = BUFFER.get();
        if (buffer == null) {
            return;
        }
        for (DeferredFailure f : buffer) {
            f.log().error(f.message(), f.cause());
        }
    }

    /**
     * Discard the buffer for the current thread. Must be called once the attempt is
     * finished to avoid leaking state across pooled request threads.
     */
    public static void clear() {
        BUFFER.remove();
    }
}
