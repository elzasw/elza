package cz.tacr.elza.dbchangelog;

import liquibase.database.Database;

/**
 * Originally the migration of the DMS storage to the new layout (3.4.9). It
 * parked every legacy file with a four-digit id in {@code _migrating/} and
 * never took it from there. The migration now runs in
 * {@link DbChangeset20261012120000}, which also repairs installations where
 * this one already ran. Kept as a no-op so that the changeset stays valid.
 */
public class DbChangeset20260918120000 extends BaseTaskChange {

    @Override
    public void execute(Database database) {
        // superseded by DbChangeset20261012120000
    }
}
