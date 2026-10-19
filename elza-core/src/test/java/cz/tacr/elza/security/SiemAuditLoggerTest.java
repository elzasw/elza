package cz.tacr.elza.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * {@link SiemAuditLogger}: a success of a stateless authentication is logged once per interval,
 * and writes deferred to the commit of the transaction.
 */
class SiemAuditLoggerTest {

    @Test
    void statelessSuccessIsLoggedOncePerCredentialAndAddress() {
        SiemAuditLogger logger = new SiemAuditLogger();

        assertThat(logger.firstStatelessSuccess("API_KEY:k1", "10.0.0.1")).isTrue();
        assertThat(logger.firstStatelessSuccess("API_KEY:k1", "10.0.0.1")).isFalse();
        // another address or another key is logged again
        assertThat(logger.firstStatelessSuccess("API_KEY:k1", "10.0.0.2")).isTrue();
        assertThat(logger.firstStatelessSuccess("API_KEY:k2", "10.0.0.1")).isTrue();
    }

    @Test
    void writeWaitsForCommit() {
        AtomicBoolean written = new AtomicBoolean();
        TransactionSynchronizationManager.initSynchronization();
        try {
            SiemAuditLogger.afterCommit(() -> written.set(true));
            assertThat(written).isFalse();

            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
            assertThat(written).isTrue();
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void writeWithoutTransactionIsImmediate() {
        AtomicBoolean written = new AtomicBoolean();
        SiemAuditLogger.afterCommit(() -> written.set(true));
        assertThat(written).isTrue();
    }
}
