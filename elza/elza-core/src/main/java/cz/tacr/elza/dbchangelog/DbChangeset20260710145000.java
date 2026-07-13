package cz.tacr.elza.dbchangelog;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.elza.cam.SyncConfig;
import cz.tacr.elza.cam.SyncConfig.SynchronizationInfo;
import cz.tacr.elza.service.SpringContext;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;

public class DbChangeset20260710145000 extends BaseTaskChange {

    private static final Logger log = LoggerFactory.getLogger(DbChangeset20260710145000.class);

    @Override
    public void execute(Database database) throws CustomChangeException {
        SyncConfig cfg = SpringContext.getBean(SyncConfig.class);
        if (cfg == null || cfg.getSync() == null || cfg.getSync().isEmpty()) {
            return;
        }
        try {
            Connection conn = ((JdbcConnection) database.getConnection()).getUnderlyingConnection();
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE ap_external_system SET sync_delay = ?"
                    + " WHERE external_system_id = (SELECT external_system_id FROM sys_external_system WHERE code = ?)")) {
                for (SynchronizationInfo si : cfg.getSync()) {
                    if (si.getCode() == null || si.getSyncDelay() == null) {
                        continue;
                    }
                    ps.setInt(1, si.getSyncDelay());
                    ps.setString(2, si.getCode());
                    int updated = ps.executeUpdate();
                    if (updated == 0) {
                        log.warn("AP external system with code '{}' not found, syncDelay migration skipped",
                                si.getCode());
                    }
                }
            }
        } catch (Exception e) {
            throw new CustomChangeException(e);
        }
    }
}