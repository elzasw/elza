package cz.tacr.elza.dbchangelog;

import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.DatabaseException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DbChangeset20190708133002 extends BaseTaskChange {

    private static final String RECORD_REF = "RECORD_REF";
    private static final String ZP2015_AP_REF = "ZP2015_AP_REF";

    private JdbcConnection dc;
    private Map<String, DbSequence> hibernateSequences;
    private Integer descItemObjectId;
    private Integer dataTypeRecordRefId;
    private Integer itemTypeApRefId;

    private int getDataTypeRecordRefId() throws DatabaseException, SQLException {
        PreparedStatement ps = dc.prepareStatement("SELECT data_type_id FROM rul_data_type WHERE code = '" + RECORD_REF + "'");
        ps.execute();
        try (ResultSet rs = ps.getResultSet()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private int getItemTypeApRefId() throws DatabaseException, SQLException {
        PreparedStatement ps = dc.prepareStatement("SELECT item_type_id FROM rul_item_type WHERE code = '" + ZP2015_AP_REF + "'");
        ps.execute();
        try (ResultSet rs = ps.getResultSet()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    @Override
    public void execute(final Database db) throws CustomChangeException {
        dc = (JdbcConnection) db.getConnection();
        try {

            List<ArrNodeRegister> nodeRegisters = findAllNodeRegisters();
            if (!nodeRegisters.isEmpty()) {
                migrateArrNodeRegister(nodeRegisters);

            }
        } catch (DatabaseException | SQLException e) {
            throw new CustomChangeException(
                    "Chyba při vykonávání sql příkazu " + e.getLocalizedMessage(), e);
        }
    }

    private void migrateArrNodeRegister(List<ArrNodeRegister> nodeRegisters) throws DatabaseException, SQLException {
        // Prepare data
        hibernateSequences = findAllHibernateSequences().stream()
                .collect(Collectors.toMap(ds -> ds.getTable() + "|" + ds.getColumn(), Function.identity()));
        descItemObjectId = getNextDescItemObjectId();
        dataTypeRecordRefId = getDataTypeRecordRefId();
        itemTypeApRefId = getItemTypeApRefId();

        Map<Integer, Integer> nodePositionMap = new HashMap<>();
        for (ArrNodeRegister nodeRegister : nodeRegisters) {
            Integer nodeId = nodeRegister.getNodeId();
            Integer recordId = nodeRegister.getRecordId();
            Integer createChangeId = nodeRegister.getCreateChangeId();
            Integer deleteChangeId = nodeRegister.getDeleteChangeId();

            Integer position = nodePositionMap.get(nodeId);
            if (position == null) {
                position = 1;
            } else {
                position++;
            }
            nodePositionMap.put(nodeId, position);
            insertDescItem(nodeId, position, createChangeId, deleteChangeId, recordId);
        }
        saveHibernateSequences();
    }

    private Integer getNextDescItemObjectId() throws DatabaseException, SQLException {
        PreparedStatement ps = dc.prepareStatement("SELECT MAX(desc_item_object_id) AS max FROM arr_item");
        ps.execute();
        try (ResultSet rs = ps.getResultSet()) {
            if (rs.next()) {
                return rs.getInt("max") + 1;
            } else {
                return 1;
            }
        }
    }

    private void insertDescItem(final Integer itemId, final Integer nodeId) throws DatabaseException, SQLException {
        PreparedStatement ps;
        ps = dc.prepareStatement("INSERT INTO arr_desc_item (item_id, node_id) VALUES (?, ?);");
        ps.setInt(1, itemId);
        ps.setInt(2, nodeId);
        ps.executeUpdate();
    }

    private void insertDescItem(final Integer nodeId,
                                final Integer position,
                                final Integer createChangeId,
                                final Integer deleteChangeId,
                                final Integer recordId) throws DatabaseException, SQLException {

        Integer dataId = insertDataRecordRef(recordId);

        Integer itemId = nextId("arr_item", "item_id");
        PreparedStatement ps = dc.prepareStatement("INSERT INTO arr_item (item_id, create_change_id, delete_change_id, desc_item_object_id, item_type_id, position, version, data_id)" +
                " VALUES (?, ?, ?, ?, ?, ?, ?, ?);");
        int i = 1;
        ps.setInt(i++, itemId);
        ps.setInt(i++, createChangeId);
        if (deleteChangeId == null) {
            ps.setNull(i++, Types.INTEGER);
        } else {
            ps.setInt(i++, deleteChangeId);
        }
        ps.setInt(i++, descItemObjectId++);
        ps.setInt(i++, itemTypeApRefId);
        ps.setInt(i++, position);
        ps.setInt(i++, 0);
        ps.setInt(i, dataId);
        ps.executeUpdate();

        insertDescItem(itemId, nodeId);
    }

    private Integer insertDataRecordRef(final Integer recordId) throws DatabaseException, SQLException {

        Integer dataId = nextId("arr_data", "data_id");
        PreparedStatement ps = dc.prepareStatement("INSERT INTO arr_data (data_id, data_type_id) VALUES (?, ?);");
        ps.setInt(1, dataId);
        ps.setInt(2, dataTypeRecordRefId);
        ps.executeUpdate();

        ps = dc.prepareStatement("INSERT INTO arr_data_record_ref (data_id, record_id) VALUES (?, ?);");
        ps.setInt(1, dataId);
        ps.setInt(2, recordId);
        ps.executeUpdate();

        return dataId;
    }

    private void saveHibernateSequences() throws SQLException, DatabaseException {
        for (DbSequence dbSequence : hibernateSequences.values()) {
            if (dbSequence.isChange()) {
                PreparedStatement ps = dc.prepareStatement("UPDATE db_hibernate_sequences SET " + DbSequence.NEXT_VAL + "=? WHERE " + DbSequence.SEQUENCE_NAME + "=?;");
                // append safety constant to sequence generator
                ps.setInt(1, dbSequence.getNextVal() + 20);
                ps.setString(2, dbSequence.getTable() + "|" + dbSequence.getColumn());
                ps.executeUpdate();
            }
        }
    }

    private Integer nextId(final String table, final String column) throws DatabaseException, SQLException {
        DbSequence dbSequence = hibernateSequences.get(table + "|" + column);
        if (dbSequence == null) {
            dbSequence = new DbSequence(table, column, 1);
            PreparedStatement ps = dc.prepareStatement("INSERT INTO db_hibernate_sequences (" + DbSequence.SEQUENCE_NAME + "," + DbSequence.NEXT_VAL + ") " +
                    "VALUES (?, ?);");
            ps.setString(1, table + "|" + column);
            ps.setInt(2, 1);
            ps.executeUpdate();
            hibernateSequences.put(table, dbSequence);
        }
        return dbSequence.nextVal();
    }

    private List<ArrNodeRegister> findAllNodeRegisters() throws DatabaseException, SQLException {
        List<ArrNodeRegister> nodeRegisters = new ArrayList<>();
        PreparedStatement ps = dc.prepareStatement("SELECT * FROM arr_node_register ORDER BY " + ArrNodeRegister.COL_NODE_ID);
        ps.execute();
        try (ResultSet rs = ps.getResultSet()) {
            while (rs.next()) {
                nodeRegisters.add(createNodeRegister(rs));
            }
        }
        return nodeRegisters;
    }

    private List<DbSequence> findAllHibernateSequences() throws DatabaseException, SQLException {
        List<DbSequence> dbSequences = new ArrayList<>();
        PreparedStatement ps = dc.prepareStatement("SELECT * FROM db_hibernate_sequences");
        ps.execute();
        try (ResultSet rs = ps.getResultSet();) {
            while (rs.next()) {
                dbSequences.add(createDbSequence(rs));
            }
        }
        return dbSequences;
    }

    private DbSequence createDbSequence(final ResultSet rs) throws SQLException {
        String sequenceName = rs.getString(DbSequence.SEQUENCE_NAME);
        String[] data = sequenceName.split("\\|");
        return new DbSequence(data[0], data[1], rs.getInt(DbSequence.NEXT_VAL));
    }

    private static class DbSequence {

        private static String SEQUENCE_NAME = "sequence_name";
        private static String NEXT_VAL = "next_val";

        private String table;
        private String column;
        private Integer nextVal;
        private boolean change = false;

        public DbSequence(final String table, final String column, final Integer nextVal) {
            this.table = table;
            this.column = column;
            this.nextVal = nextVal;
        }

        public String getTable() {
            return table;
        }

        public String getColumn() {
            return column;
        }

        public Integer getNextVal() {
            return nextVal;
        }

        /**
         * Prepare next value
         *
         * @return
         */
        public Integer nextVal() {
            change = true;
            int result = nextVal;
            nextVal++;
            return result;
        }

        public boolean isChange() {
            return change;
        }
    }

    private ArrNodeRegister createNodeRegister(final ResultSet rs) throws SQLException {
        int dcid = rs.getInt(ArrNodeRegister.COL_DELETE_CHANGE_ID);
        return new ArrNodeRegister(rs.getInt(ArrNodeRegister.COL_NODE_REGISTER_ID),
                rs.getInt(ArrNodeRegister.COL_RECORD_ID),
                rs.getInt(ArrNodeRegister.COL_NODE_ID),
                rs.getInt(ArrNodeRegister.COL_CREATE_CHANGE_ID),
                dcid == 0 ? null : dcid);
    }

    private static class ArrNodeRegister {

        private static String COL_NODE_REGISTER_ID = "node_register_id";
        private static String COL_RECORD_ID = "record_id";
        private static String COL_NODE_ID = "node_id";
        private static String COL_CREATE_CHANGE_ID = "create_change_id";
        private static String COL_DELETE_CHANGE_ID = "delete_change_id";

        private Integer nodeRegisterId;
        private Integer recordId;
        private Integer nodeId;
        private Integer createChangeId;
        private Integer deleteChangeId;

        public ArrNodeRegister(final Integer nodeRegisterId, final Integer recordId, final Integer nodeId, final Integer createChangeId, final Integer deleteChangeId) {
            this.nodeRegisterId = nodeRegisterId;
            this.recordId = recordId;
            this.nodeId = nodeId;
            this.createChangeId = createChangeId;
            this.deleteChangeId = deleteChangeId;
        }

        public Integer getNodeRegisterId() {
            return nodeRegisterId;
        }

        public Integer getRecordId() {
            return recordId;
        }

        public Integer getNodeId() {
            return nodeId;
        }

        public Integer getCreateChangeId() {
            return createChangeId;
        }

        public Integer getDeleteChangeId() {
            return deleteChangeId;
        }
    }

}
