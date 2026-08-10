package cz.tacr.elza.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.domain.AiRequest;
import cz.tacr.elza.repository.AiRequestRepository;
import cz.tacr.elza.service.UserService;
import cz.tacr.elza.websocket.UserEventPushService;

/**
 * Renders the client snapshot of an AI request and pushes it to the
 * conversation owner's per-user WebSocket topic — the one delivery path shared
 * by the authoritative task poll ({@link AiRequestPoller}) and the advisory
 * event stream ({@link AiEventPoller}). Two rules, learned the hard way:
 *
 * <ul>
 * <li><b>Commit first, render after.</b> Rendering is derived, the persisted
 * request is authoritative: the snapshot is built in its own transaction from
 * the committed row, never inside the transaction that stores a result — a
 * mapper failure there rolled a received result back and the exchange hung
 * until its lifetime backstop (2026-08-07).</li>
 * <li><b>Render as the owner.</b> Block mappers run real permission checks
 * (the proposal block re-reads its level through {@code canRead}), and poller
 * threads carry no security context of their own — unauthenticated, the check
 * blew up, every completion push was skipped and the panel hung on its
 * progress state until a manual reload re-rendered on an authenticated
 * request thread (2026-08-10). Impersonating the owner also makes the checks
 * mean what they should: the pushed card shows exactly what the owner is
 * entitled to see.</li>
 * </ul>
 */
@Component
public class AiRequestPushService {

    private static final Logger logger = LoggerFactory.getLogger(AiRequestPushService.class);

    @Autowired
    private AiRequestRepository aiRequestRepository;

    @Autowired
    private AiRequestViewMapper requestViewMapper;

    @Autowired
    private UserEventPushService pushService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private UserService userService;

    /**
     * Builds the snapshot of the request as the owner and pushes it. Never
     * throws: a rendering failure is logged in full and the push is skipped —
     * the persisted request is unaffected and the client's next fetch
     * re-renders it. Failing to draw a result must not endanger the result.
     */
    public void pushUpdate(final Integer aiRequestId, final Integer userId) {
        if (userId == null) {
            return; // no owner — nobody to push to
        }
        AiRequestUpdateMessage message;
        // Snapshot + restore the thread's security context (the async-worker
        // idiom): poller threads are pooled, leaving the owner's context behind
        // would leak their permissions into whatever runs here next.
        SecurityContext originalSecCtx = SecurityContextHolder.getContext();
        try {
            SecurityContextHolder.setContext(userService.createSecurityContext(userId));
            message = transactionTemplate.execute(status -> {
                AiRequest request = aiRequestRepository.findById(aiRequestId).orElse(null);
                return request == null ? null : requestViewMapper.buildUpdateMessage(request);
            });
        } catch (RuntimeException e) {
            logger.error("Rendering the snapshot of AI request {} failed; the stored request is intact"
                         + " and the push is skipped", aiRequestId, e);
            return;
        } finally {
            SecurityContext emptyContext = SecurityContextHolder.createEmptyContext();
            if (emptyContext.equals(originalSecCtx)) {
                SecurityContextHolder.clearContext();
            } else {
                SecurityContextHolder.setContext(originalSecCtx);
            }
        }
        if (message != null) {
            pushService.push(userId, message);
        }
    }
}
