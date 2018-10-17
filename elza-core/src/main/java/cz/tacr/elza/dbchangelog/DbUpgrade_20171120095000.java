package cz.tacr.elza.dbchangelog;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.DatabaseException;

/**
 * Migrace arr_packet & arr_data_packet_ref.
 *
 * @since 20.11.2017
 */
public class DbUpgrade_20171120095000 extends BaseTaskChange {

    private static final int DATA_TYPE_STRING = 2;
    private static final int DATA_TYPE_PACKET_REF = 11;
    private static final int DATA_TYPE_ENUM = 12;
    private static final int DATA_TYPE_SO_REF = 15;

    private List<RulRuleSet> ruleSets;
    private JdbcConnection dc;
    private Map<String, DbSequence> hibernateSequences;
    private Map<Integer, StructureTypePack> structureTypePacks = new HashMap<>();
    private Map<Integer, Integer> fundIdRuleSetIdMap;
    private Map<Integer, ArrStructureData> packetIdStructureDataMap = new HashMap<>();
    private List<RulItemType> packetRefItemTypes = new ArrayList<>();
    private Integer change;
    private Integer descItemObjectId;

    @Override
    public void execute(final Database db) throws CustomChangeException {
        dc = (JdbcConnection) db.getConnection();
        try {

            List<ArrPacket> packets = findAllPackets();
            if (!packets.isEmpty()) {
                migratePackets(packets);

            }
        } catch (DatabaseException | SQLException e) {
            throw new CustomChangeException(
                    "Chyba při vykonávání sql příkazu " + e.getLocalizedMessage(), e);
        }
    }

    private void migratePackets(List<ArrPacket> packets) throws DatabaseException, SQLException {
        // Prepare data
        ruleSets = findAllRuleSets();
        fundIdRuleSetIdMap = createFundIdRuleSetIdMap();
        hibernateSequences = findAllHibernateSequences().stream()
                .collect(Collectors.toMap(DbSequence::getTable, Function.identity()));
        packetRefItemTypes.addAll(findPacketItemTypes());
        descItemObjectId = getNextDescItemObjectId();
        change = createChange();

        // create new types
        for (RulRuleSet ruleSet : ruleSets) {
            StructureTypePack structureTypePack = createStructureTypePack(ruleSet);
            structureTypePacks.put(ruleSet.getRuleSetId(), structureTypePack);
        }

        // create structured types
        for (ArrPacket packet : packets) {
            Integer fundId = packet.getFundId();
            Integer ruleSetId = fundIdRuleSetIdMap.get(fundId);
            StructureTypePack structureTypePack = structureTypePacks.get(ruleSetId);

            boolean assignable = packet.getState() == ArrPacket.State.OPEN;
            ArrStructureData structureData = createStructureData(change,
                    structureTypePack.getStructureType().getStructureTypeId(), fundId, assignable,
                    ArrStructureData.State.ERROR);
            insertStructureData(structureData);

            createAndInsertStructureItems(structureData, packet, structureTypePack);

            packetIdStructureDataMap.put(packet.getPacketId(), structureData);
        }

        // update references to new structured types
        List<ArrDataPacketRef> packetRefs = findAllDataPacketRef();
        for (ArrDataPacketRef packetRef : packetRefs) {
            deleteDataPacketRef(packetRef);
            ArrStructureData structureData = packetIdStructureDataMap.get(packetRef.getPacketId());
            createDataStructureRef(packetRef.getDataId(), structureData.getStructureDataId());
        }

        // update data type in arr_data
        dataChangeDataType(DATA_TYPE_PACKET_REF, DATA_TYPE_SO_REF);

        // update item types from PACKET_REF to SO_REF
        for (RulItemType itemType : packetRefItemTypes) {
            // get type
            StructureTypePack structureTypePack = structureTypePacks.get(itemType.getRuleSetId());
            updateItemTypeWithStructureType(itemType.getItemTypeId(), DATA_TYPE_SO_REF,
                    structureTypePack.getStructureType().getStructureTypeId());
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

    private void createAndInsertStructureItems(final ArrStructureData structureData,
                                               final ArrPacket packet,
                                               final StructureTypePack structureTypePack) throws DatabaseException, SQLException {

        RulItemType itemTypePacketDescription = structureTypePack.getItemTypePacketDescription();
        Integer dataIdString = insertDataString(packet.getStorageNumber(), itemTypePacketDescription);
        insertStructureItem(structureData, itemTypePacketDescription, dataIdString);

        RulItemType itemTypePacketType = structureTypePack.getItemTypePacketType();
        RulItemSpec itemSpecPacketType = structureTypePack.getSpecPacketType(packet.getPacketTypeId());
        Integer dataIdNull = insertDataNull(itemTypePacketType);
        if (itemSpecPacketType == null) {
            insertStructureItem(structureData, itemTypePacketType, dataIdNull);
        } else {
            insertStructureItemWithSpec(structureData, itemTypePacketType, itemSpecPacketType, dataIdNull);
        }
    }

    private void insertStructureItemWithSpec(final ArrStructureData structureData, final RulItemType itemType, final RulItemSpec itemSpec, final Integer dataId) throws DatabaseException, SQLException {
        Integer itemId = nextId("arr_item", "item_id");
        PreparedStatement ps = dc.prepareStatement("INSERT INTO arr_item (item_id, create_change_id, desc_item_object_id, item_type_id, item_spec_id, position, version, data_id)" +
                " VALUES (?, ?, ?, ?, ?, ?, ?, ?);");
        int i = 1;
        ps.setInt(i++, itemId);
        ps.setInt(i++, change);
        ps.setInt(i++, descItemObjectId++);
        ps.setInt(i++, itemType.getItemTypeId());
        ps.setInt(i++, itemSpec.getItemSpecId());
        ps.setInt(i++, 1);
        ps.setInt(i++, 0);
        ps.setInt(i, dataId);
        ps.executeUpdate();

        insertStructureItem(structureData, itemId);
    }

    private void insertStructureItem(final ArrStructureData structureData, final Integer itemId) throws DatabaseException, SQLException {
        PreparedStatement ps;
        ps = dc.prepareStatement("INSERT INTO arr_structure_item (item_id, structure_data_id) VALUES (?, ?);");
        ps.setInt(1, itemId);
        ps.setInt(2, structureData.getStructureDataId());
        ps.executeUpdate();
    }

    private void insertStructureItem(final ArrStructureData structureData, final RulItemType itemType, final Integer dataId) throws DatabaseException, SQLException {
        Integer itemId = nextId("arr_item", "item_id");
        PreparedStatement ps = dc.prepareStatement("INSERT INTO arr_item (item_id, create_change_id, desc_item_object_id, item_type_id, position, version, data_id)" +
                " VALUES (?, ?, ?, ?, ?, ?, ?);");
        int i = 1;
        ps.setInt(i++, itemId);
        ps.setInt(i++, change);
        ps.setInt(i++, descItemObjectId++);
        ps.setInt(i++, itemType.getItemTypeId());
        ps.setInt(i++, 1);
        ps.setInt(i++, 0);
        ps.setInt(i, dataId);
        ps.executeUpdate();

        insertStructureItem(structureData, itemId);
    }

    private Integer insertDataString(final String storageNumber, final RulItemType itemType) throws DatabaseException, SQLException {

        Integer dataId = nextId("arr_data", "data_id");
        PreparedStatement ps = dc.prepareStatement("INSERT INTO arr_data (data_id, data_type_id) VALUES (?, ?);");
        ps.setInt(1, dataId);
        ps.setInt(2, itemType.getDataTypeId());
        ps.executeUpdate();

        ps = dc.prepareStatement("INSERT INTO arr_data_string (data_id, value) VALUES (?, ?);");
        ps.setInt(1, dataId);
        ps.setString(2, storageNumber);
        ps.executeUpdate();

        return dataId;
    }

    private Integer insertDataNull(final RulItemType itemType) throws DatabaseException, SQLException {

        Integer dataId = nextId("arr_data", "data_id");
        PreparedStatement ps = dc.prepareStatement("INSERT INTO arr_data (data_id, data_type_id) VALUES (?, ?);");
        ps.setInt(1, dataId);
        ps.setInt(2, itemType.getDataTypeId());
        ps.executeUpdate();

        ps = dc.prepareStatement("INSERT INTO arr_data_null (data_id) VALUES (?);");
        ps.setInt(1, dataId);
        ps.executeUpdate();

        return dataId;
    }

    private void updateItemTypeWithStructureType(int itemTypeId, int dataTypeSoRef, int structureTypeId)
            throws DatabaseException, SQLException {
        PreparedStatement ps = dc.prepareStatement(
                "UPDATE " + RulItemType.TABLE + " SET data_type_id = ?, " +
                        RulItemType.COL_STRUCTURE_TYPE_ID + " = ? WHERE item_type_id = ?;");
        ps.setInt(1, dataTypeSoRef);
        ps.setInt(2, structureTypeId);
        ps.setInt(3, itemTypeId);
        ps.executeUpdate();
    }

    private void dataChangeDataType(final int fromDataTypeId, final int toDataTypeId) throws SQLException, DatabaseException {
        PreparedStatement ps = dc.prepareStatement("UPDATE arr_data SET data_type_id = ? WHERE data_type_id = ?;");
        ps.setInt(1, toDataTypeId);
        ps.setInt(2, fromDataTypeId);
        ps.executeUpdate();
    }

    private void createDataStructureRef(final Integer dataId, final Integer structureDataId) throws SQLException, DatabaseException {
        PreparedStatement ps = dc.prepareStatement("INSERT INTO arr_data_structure_ref (data_id, structure_data_id) VALUES (?, ?);");
        ps.setInt(1, dataId);
        ps.setInt(2, structureDataId);
        ps.executeUpdate();
    }

    private void deleteDataPacketRef(final ArrDataPacketRef packetRef) throws DatabaseException, SQLException {
        PreparedStatement ps = dc.prepareStatement("DELETE FROM " + ArrDataPacketRef.TABLE + " WHERE " + ArrDataPacketRef.COL_DATA_ID + "=?;");
        ps.setInt(1, packetRef.getDataId());
        ps.executeUpdate();
    }

    private List<ArrDataPacketRef> findAllDataPacketRef() throws DatabaseException, SQLException {
        List<ArrDataPacketRef> dataPacketRefs = new ArrayList<>();
        PreparedStatement ps = dc.prepareStatement("SELECT * FROM arr_data_packet_ref");
        ps.execute();
        try (ResultSet rs = ps.getResultSet();) {
            while (rs.next()) {
                dataPacketRefs.add(createDataPacketRef(rs));
            }
        }
        return dataPacketRefs;
    }

    private ArrDataPacketRef createDataPacketRef(final ResultSet rs) throws SQLException {
        return new ArrDataPacketRef(rs.getInt(ArrDataPacketRef.COL_DATA_ID), rs.getInt(ArrDataPacketRef.COL_PACKET_ID));
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

    private ArrStructureData createStructureData(final Integer createChangeId,
                                                 final Integer structureTypeId,
                                                 final Integer fundId,
                                                 final boolean assignable,
                                                 final ArrStructureData.State state) throws DatabaseException, SQLException {
        return new ArrStructureData(nextId(ArrStructureData.TABLE, ArrStructureData.STRUCTURE_DATA_ID), createChangeId, structureTypeId, fundId, assignable, state);
    }

    private void insertStructureData(final ArrStructureData structureData) throws DatabaseException, SQLException {
        PreparedStatement ps = dc.prepareStatement("INSERT INTO " + ArrStructureData.TABLE + " (structure_data_id, create_change_id, structure_type_id, fund_id, assignable, state) " +
                "VALUES (?, ?, ?, ?, ?, ?);");
        int i = 1;
        ps.setInt(i++, structureData.getStructureDataId());
        ps.setInt(i++, structureData.getCreateChangeId());
        ps.setInt(i++, structureData.getStructureTypeId());
        ps.setInt(i++, structureData.getFundId());
        ps.setBoolean(i++, structureData.getAssignable());
        ps.setString(i, structureData.getState().name());
        ps.executeUpdate();
    }

    private Map<Integer, Integer> createFundIdRuleSetIdMap() throws SQLException, DatabaseException {
        Map<Integer, Integer> result = new HashMap<>();
        PreparedStatement ps = dc.prepareStatement("SELECT * FROM arr_fund_version fv WHERE lock_change_id IS NULL");
        ps.execute();
        try (ResultSet rs = ps.getResultSet();) {
            while (rs.next()) {
                result.put(rs.getInt("fund_id"), rs.getInt("rule_set_id"));
            }
        }
        return result;
    }

    private StructureTypePack createStructureTypePack(final RulRuleSet ruleSet) throws DatabaseException, SQLException {
        StructureTypePack result = new StructureTypePack();

        // create structured type
        RulStructureType structureType = createStructureType(ruleSet.getCode() + "_PACKET", "Obaly", ruleSet.getRuleSetId(), ruleSet.getPackageId());
        insertStructureType(structureType);
        result.setStructureType(structureType);

        // create packet_type
        RulItemType itemTypePacketType = createItemType(DATA_TYPE_ENUM, ruleSet.getCode() + "_PACKET_TYPE", "Typ obalu",
                "Typ obalu", "Typ obalu", true, nextItemTypeViewOrder(), ruleSet.getRuleSetId(), ruleSet.getPackageId(),
                null);
        insertItemType(itemTypePacketType);
        result.setItemTypePacketType(itemTypePacketType);

        RulItemType itemTypePacketDescription = createItemType(DATA_TYPE_STRING, ruleSet.getCode() + "_PACKET_PREFIX",
                "Popis obalu",
                "Popis obalu", "Popis obalu", false, nextItemTypeViewOrder(), ruleSet.getRuleSetId(),
                ruleSet.getPackageId(),
                null);
        insertItemType(itemTypePacketDescription);
        result.setItemTypePacketDescription(itemTypePacketDescription);

        // prepare specifications for packet types
        List<RulPacketType> packetTypes = findPacketTypeByRuleSet(ruleSet);
        List<RulItemSpec> itemSpecPacketTypes = new ArrayList<>(packetTypes.size());

        Map<Integer, RulItemSpec> packetTypeIdItemSpecMap = new HashMap<>();
        int viewOrder = 1;
        for (RulPacketType packetType : packetTypes) {
            String code = ruleSet.getCode() + "_PACKET_TYPE_" + packetType.getShortcut().toUpperCase();
            String name = packetType.getName();
            String shortcut = packetType.getShortcut();
            String description = name;
            Integer packageId = packetType.getPackageId();
            RulItemSpec itemSpec = createItemSpec(itemTypePacketType.getItemTypeId(), code, name, shortcut, description, viewOrder++, packageId);
            insertItemSpec(itemSpec);
            itemSpecPacketTypes.add(itemSpec);
            packetTypeIdItemSpecMap.put(packetType.getPacketTypeId(), itemSpec);
        }
        result.setPacketTypeIdItemSpecMap(packetTypeIdItemSpecMap);

        return result;
    }

    private void insertItemSpec(final RulItemSpec itemSpec) throws DatabaseException, SQLException {
        PreparedStatement ps = dc.prepareStatement("INSERT INTO " + RulItemSpec.TABLE + " (item_spec_id, item_type_id, code, name, shortcut, description, " +
                "view_order, package_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?);");
        int i = 1;
        ps.setInt(i++, itemSpec.getItemSpecId());
        ps.setInt(i++, itemSpec.getItemTypeId());
        ps.setString(i++, itemSpec.getCode());
        ps.setString(i++, itemSpec.getName());
        ps.setString(i++, itemSpec.getShortcut());
        ps.setString(i++, itemSpec.getDescription());
        ps.setInt(i++, itemSpec.getViewOrder());
        ps.setInt(i++, itemSpec.getPackageId());
        ps.executeUpdate();
    }

    private Integer createChange() throws DatabaseException, SQLException {
        PreparedStatement ps = dc.prepareStatement("INSERT INTO arr_change (change_id, change_date, type) " +
                "VALUES (?, ?, ?);");
        Integer changeId = nextId("arr_change", "change_id");
        ps.setInt(1, changeId);
        ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
        ps.setString(3, "ADD_STRUCTURE_DATA_BATCH");
        ps.executeUpdate();
        return changeId;
    }

    private RulItemSpec createItemSpec(final Integer itemTypeId,
                                       final String code,
                                       final String name,
                                       final String shortcut,
                                       final String description,
                                       final Integer viewOrder,
                                       final Integer packageId) throws DatabaseException, SQLException {
        return new RulItemSpec(nextId(RulItemSpec.TABLE, RulItemSpec.ITEM_SPEC_ID), itemTypeId, code, name, shortcut, description, viewOrder, packageId);
    }

    private RulStructureType createStructureType(final String code,
                                       final String name,
                                       final Integer ruleSetId,
                                       final Integer packageId) throws DatabaseException, SQLException {
        return new RulStructureType(nextId(RulStructureType.TABLE, RulStructureType.COL_STRUCTURE_TYPE_ID), code, name, packageId, ruleSetId);
    }

    private RulItemType createItemType(final Integer dataTypeId,
                                       final String code,
                                       final String name,
                                       final String shortcut,
                                       final String description,
                                       final boolean useSpecification,
                                       final Integer viewOrder,
                                       final Integer ruleSetId,
                                       final Integer packageId,
                                       final Integer structureTypeId) throws DatabaseException, SQLException {
        return new RulItemType(nextId(RulItemType.TABLE, RulItemType.COL_ITEM_TYPE_ID), dataTypeId, code, name, shortcut, description, false, false, useSpecification, viewOrder, ruleSetId, packageId, null, structureTypeId);
    }

    private Integer nextId(final String table, final String column) throws DatabaseException, SQLException {
        DbSequence dbSequence = hibernateSequences.get(table);
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

    private void insertItemType(final RulItemType itemType) throws DatabaseException, SQLException {
        PreparedStatement ps = dc.prepareStatement("INSERT INTO " + RulItemType.TABLE + " (item_type_id, data_type_id, code, name, shortcut, description, is_value_unique, " +
                "can_be_ordered, use_specification, view_order, package_id, columns_definition, rule_set_id, structure_type_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
        int i = 1;
        ps.setInt(i++, itemType.getItemTypeId());
        ps.setInt(i++, itemType.getDataTypeId());
        ps.setString(i++, itemType.getCode());
        ps.setString(i++, itemType.getName());
        ps.setString(i++, itemType.getShortcut());
        ps.setString(i++, itemType.getDescription());
        ps.setBoolean(i++, itemType.getIsValueUnique());
        ps.setBoolean(i++, itemType.getCanBeOrdered());
        ps.setBoolean(i++, itemType.getUseSpecification());
        ps.setInt(i++, itemType.getViewOrder());
        ps.setInt(i++, itemType.getPackageId());
        ps.setString(i++, itemType.getColumnsDefinition());
        ps.setInt(i++, itemType.getRuleSetId());

        Integer structTypeId = itemType.getStructureTypeId();
        if (structTypeId != null) {
            ps.setInt(i++, structTypeId);
        } else {
            ps.setNull(i++, java.sql.Types.INTEGER);
        }
        ps.executeUpdate();
    }

    private void insertStructureType(final RulStructureType structureType) throws DatabaseException, SQLException {
        PreparedStatement ps = dc.prepareStatement("INSERT INTO " + RulStructureType.TABLE + " (" + RulStructureType.COL_STRUCTURE_TYPE_ID + "," +
                RulStructureType.COL_CODE + "," +
                RulStructureType.COL_NAME + "," +
                RulStructureType.COL_PACKAGE_ID + "," +
                RulStructureType.COL_RULE_SET_ID + ") " +
                "VALUES (?, ?, ?, ?, ?);");
        int i = 1;
        ps.setInt(i++, structureType.getStructureTypeId());
        ps.setString(i++, structureType.getCode());
        ps.setString(i++, structureType.getName());
        ps.setInt(i++, structureType.getPackageId());
        ps.setInt(i++, structureType.getRuleSetId());
        ps.executeUpdate();
    }

    /**
     * Return all existing packets
     * 
     * @return
     * @throws DatabaseException
     * @throws SQLException
     */
    private List<ArrPacket> findAllPackets() throws DatabaseException, SQLException {
        List<ArrPacket> packets = new ArrayList<>();
        PreparedStatement ps = dc.prepareStatement("SELECT * FROM arr_packet");
        ps.execute();
        try (ResultSet rs = ps.getResultSet();) {
            while (rs.next()) {
                packets.add(createPacket(rs));
            }
        }
        return packets;
    }

    private Collection<RulItemType> findPacketItemTypes() throws DatabaseException, SQLException {
        List<RulItemType> items = new ArrayList<>();
        PreparedStatement ps = dc.prepareStatement("SELECT * FROM rul_item_type it WHERE it.data_type_id = ?");
        ps.setInt(1, DATA_TYPE_PACKET_REF);
        ps.execute();
        try (ResultSet rs = ps.getResultSet();) {
            while (rs.next()) {
                items.add(createItemType(rs));
            }
        }
        return items;
    }

    private RulItemType createItemType(ResultSet rs) throws SQLException {
        return new RulItemType(rs.getInt(RulItemType.COL_ITEM_TYPE_ID),
                rs.getInt(RulItemType.COL_DATA_TYPE_ID),
                rs.getString(RulItemType.COL_CODE),
                rs.getString(RulItemType.COL_NAME),
                rs.getString(RulItemType.COL_SHORTCUT),
                rs.getString(RulItemType.COL_DESCRIPTION),
                rs.getBoolean(RulItemType.COL_IS_VALUE_UNIQUE),
                rs.getBoolean(RulItemType.COL_CAN_BE_ORDERED),
                rs.getBoolean(RulItemType.COL_USE_SPECIFICATION),
                rs.getInt(RulItemType.COL_VIEW_ORDER),
                rs.getInt(RulItemType.COL_RULE_SET_ID),
                rs.getInt(RulItemType.COL_PACKAGE_ID),
                rs.getString(RulItemType.COL_COLUMNS_DEFINITION),
                null /*struct_type - not exists yet*/);
    }

    private Integer nextItemTypeViewOrder() throws DatabaseException, SQLException {
        PreparedStatement ps = dc.prepareStatement("SELECT MAX(" + RulItemType.COL_VIEW_ORDER  + ") FROM " + RulItemType.TABLE);
        ps.execute();
        try (ResultSet rs = ps.getResultSet();) {
            if (rs.next()) {
                return rs.getInt(1) + 1;
            }
        }
        return 1;
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

    private List<RulRuleSet> findAllRuleSets() throws DatabaseException, SQLException {
        List<RulRuleSet> ruleSets = new ArrayList<>();
        PreparedStatement ps = dc.prepareStatement("SELECT * FROM rul_rule_set");
        ps.execute();
        try (ResultSet rs = ps.getResultSet();) {
            while (rs.next()) {
                ruleSets.add(createRuleSet(rs));
            }
        }
        return ruleSets;
    }

    private List<RulPacketType> findPacketTypeByRuleSet(final RulRuleSet ruleSet) throws DatabaseException, SQLException {
        List<RulPacketType> packetTypes = new ArrayList<>();
        PreparedStatement ps = dc.prepareStatement("SELECT * FROM " + RulPacketType.TABLE + " WHERE " + RulPacketType.COL_PACKAGE_ID + "=? AND " + RulPacketType.COL_RULE_SET_ID + "=?");
        ps.setInt(1, ruleSet.getPackageId());
        ps.setInt(2, ruleSet.getRuleSetId());
        ps.execute();
        try (ResultSet rs = ps.getResultSet();) {
            while (rs.next()) {
                packetTypes.add(createPacketType(rs));
            }
        }
        return packetTypes;
    }

    private RulPacketType createPacketType(final ResultSet rs) throws SQLException {
        return new RulPacketType(rs.getInt(RulPacketType.COL_PACKET_TYPE_ID),
                rs.getString(RulPacketType.COL_CODE),
                rs.getString(RulPacketType.COL_NAME),
                rs.getString(RulPacketType.COL_SHORTCUT),
                rs.getInt(RulPacketType.COL_RULE_SET_ID),
                rs.getInt(RulPacketType.COL_PACKAGE_ID));
    }

    private RulRuleSet createRuleSet(final ResultSet rs) throws SQLException {
        return new RulRuleSet(rs.getInt(RulRuleSet.COL_RULE_SET_ID),
                rs.getString(RulRuleSet.COL_CODE),
                rs.getInt(RulRuleSet.COL_PACKAGE_ID));
    }

    private DbSequence createDbSequence(final ResultSet rs) throws SQLException {
        String sequenceName = rs.getString(DbSequence.SEQUENCE_NAME);
        String[] data = sequenceName.split("\\|");
        return new DbSequence(data[0], data[1], rs.getInt(DbSequence.NEXT_VAL));
    }

    private static class RulStructureType {

        private static String TABLE = "rul_structure_type";
        private static String COL_STRUCTURE_TYPE_ID = "structure_type_id";
        private static String COL_CODE = "code";
        private static String COL_NAME = "name";
        private static String COL_PACKAGE_ID = "package_id";
        private static String COL_RULE_SET_ID = "rule_set_id";

        private Integer structureTypeId;
        private String code;
        private String name;
        private Integer packageId;
        private Integer ruleSetId;

        public RulStructureType(final Integer structureTypeId, final String code, final String name, final Integer packageId, final Integer ruleSetId) {
            this.structureTypeId = structureTypeId;
            this.code = code;
            this.name = name;
            this.packageId = packageId;
            this.ruleSetId = ruleSetId;
        }

        public Integer getStructureTypeId() {
            return structureTypeId;
        }

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        public Integer getPackageId() {
            return packageId;
        }

        public Integer getRuleSetId() {
            return ruleSetId;
        }
    }

    private static class StructureTypePack {

        // packet type (kar, fas)
        private RulItemType itemTypePacketType;

        // old packet type -> specs for packet type
        private Map<Integer, RulItemSpec> packetTypeIdItemSpecMap = new HashMap<>();

        // packet number/prefix
        private RulItemType itemTypePacketDescription;

        // structured type
        private RulStructureType structureType;

        public RulItemType getItemTypePacketType() {
            return itemTypePacketType;
        }

        /**
         * Return specification for old packet type
         * 
         * @param packetTypeId
         * @return
         */
        public RulItemSpec getSpecPacketType(Integer packetTypeId) {
            RulItemSpec ret = null;
            if (packetTypeId != null) {
                ret = packetTypeIdItemSpecMap.get(packetTypeId);
                if (ret == null) {
                    throw new IllegalStateException("Unexpected packetTypeId = " + packetTypeId);
                }
            }
            return ret;
        }

        public void setItemTypePacketType(final RulItemType itemTypePacketType) {
            this.itemTypePacketType = itemTypePacketType;
        }

        public void setPacketTypeIdItemSpecMap(final Map<Integer, RulItemSpec> packetTypeIdItemSpecMap) {
            this.packetTypeIdItemSpecMap = packetTypeIdItemSpecMap;
        }

        public RulItemType getItemTypePacketDescription() {
            return itemTypePacketDescription;
        }

        public void setItemTypePacketDescription(final RulItemType itemTypePacketDescription) {
            this.itemTypePacketDescription = itemTypePacketDescription;
        }

        public void setStructureType(final RulStructureType structureType) {
            this.structureType = structureType;
        }

        public RulStructureType getStructureType() {
            return structureType;
        }
    }

    private static class RulItemSpec {

        public static final String TABLE = "rul_item_spec";
        public static final String ITEM_SPEC_ID = "item_spec_id";

        private Integer itemSpecId;
        private Integer itemTypeId;
        private String code;
        private String name;
        private String shortcut;
        private String description;
        private Integer viewOrder;
        private Integer packageId;

        public RulItemSpec(final Integer itemSpecId, final Integer itemTypeId, final String code, final String name,
                           final String shortcut, final String description, final Integer viewOrder, final Integer packageId) {

            this.itemSpecId = itemSpecId;
            this.itemTypeId = itemTypeId;
            this.code = code;
            this.name = name;
            this.shortcut = shortcut;
            this.description = description;
            this.viewOrder = viewOrder;
            this.packageId = packageId;
        }

        public Integer getItemSpecId() {
            return itemSpecId;
        }

        public Integer getItemTypeId() {
            return itemTypeId;
        }

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        public String getShortcut() {
            return shortcut;
        }

        public String getDescription() {
            return description;
        }

        public Integer getViewOrder() {
            return viewOrder;
        }

        public Integer getPackageId() {
            return packageId;
        }

    }

    private static class RulItemType {

        private static String TABLE = "rul_item_type";
        private static String COL_ITEM_TYPE_ID = "item_type_id";
        private static String COL_DATA_TYPE_ID = "data_type_id";
        private static String COL_CODE = "code";
        private static String COL_NAME = "name";
        private static String COL_SHORTCUT = "shortcut";
        private static String COL_DESCRIPTION = "description";
        private static String COL_IS_VALUE_UNIQUE = "is_value_unique";
        private static String COL_CAN_BE_ORDERED = "can_be_ordered";
        private static String COL_USE_SPECIFICATION = "use_specification";
        private static String COL_VIEW_ORDER = "view_order";
        private static String COL_PACKAGE_ID = "package_id";
        private static String COL_COLUMNS_DEFINITION = "columns_definition";
        private static String COL_RULE_SET_ID = "rule_set_id";
        private static String COL_STRUCTURE_TYPE_ID = "structure_type_id";

        private Integer itemTypeId;
        private Integer dataTypeId;
        private String code;
        private String name;
        private String shortcut;
        private String description;
        private Boolean isValueUnique;
        private Boolean canBeOrdered;
        private Boolean useSpecification;
        private Integer viewOrder;
        private Integer ruleSetId;
        private Integer packageId;
        private String columnsDefinition;
        private Integer structureTypeId;

        public RulItemType(final Integer itemTypeId, final Integer dataTypeId,
                           final String code, final String name, final String shortcut,
                           final String description, final Boolean isValueUnique,
                           final Boolean canBeOrdered, final Boolean useSpecification,
                           final Integer viewOrder, final Integer ruleSetId,
                           final Integer packageId,
                           final String columnsDefinition, final Integer structureTypeId) {
            this.itemTypeId = itemTypeId;
            this.dataTypeId = dataTypeId;
            this.code = code;
            this.name = name;
            this.shortcut = shortcut;
            this.description = description;
            this.isValueUnique = isValueUnique;
            this.canBeOrdered = canBeOrdered;
            this.useSpecification = useSpecification;
            this.viewOrder = viewOrder;
            this.ruleSetId = ruleSetId;
            this.packageId = packageId;
            this.columnsDefinition = columnsDefinition;
            this.structureTypeId = structureTypeId;
        }

        public Integer getItemTypeId() {
            return itemTypeId;
        }

        public Integer getDataTypeId() {
            return dataTypeId;
        }

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        public String getShortcut() {
            return shortcut;
        }

        public String getDescription() {
            return description;
        }

        public Boolean getIsValueUnique() {
            return isValueUnique;
        }

        public Boolean getCanBeOrdered() {
            return canBeOrdered;
        }

        public Boolean getUseSpecification() {
            return useSpecification;
        }

        public Integer getViewOrder() {
            return viewOrder;
        }

        public Integer getRuleSetId() {
            return ruleSetId;
        }

        public Integer getPackageId() {
            return packageId;
        }

        public String getColumnsDefinition() {
            return columnsDefinition;
        }

        public Integer getStructureTypeId() {
            return structureTypeId;
        }
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

    private static class RulRuleSet {
        private static String COL_RULE_SET_ID = "rule_set_id";
        private static String COL_CODE = "code";
        private static String COL_PACKAGE_ID = "package_id";

        private Integer ruleSetId;
        private String code;
        private Integer packageId;

        public RulRuleSet(final Integer ruleSetId, final String code, final Integer packageId) {
            this.ruleSetId = ruleSetId;
            this.code = code;
            this.packageId = packageId;
        }

        public Integer getRuleSetId() {
            return ruleSetId;
        }

        public String getCode() {
            return code;
        }

        public Integer getPackageId() {
            return packageId;
        }
    }

    private ArrPacket createPacket(final ResultSet rs) throws SQLException {
        Integer packetTypeId = rs.getInt(ArrPacket.COL_PACKET_TYPE_ID);
        if (rs.wasNull()) {
            packetTypeId = null;
        }
        return new ArrPacket(rs.getInt(ArrPacket.COL_PACKET_ID),
                rs.getString(ArrPacket.COL_STORAGE_NUMBER),
                packetTypeId,
                rs.getInt(ArrPacket.COL_FUND_ID),
                ArrPacket.State.valueOf(rs.getString(ArrPacket.COL_STATE).trim()));
    }

    private static class ArrPacket {

        private static String COL_PACKET_ID = "packet_id";
        private static String COL_STORAGE_NUMBER = "storage_number";
        private static String COL_PACKET_TYPE_ID = "packet_type_id";
        private static String COL_FUND_ID = "fund_id";
        private static String COL_STATE = "state";

        private Integer packetId;
        private String storageNumber;
        private Integer packetTypeId;
        private Integer fundId;
        private State state;

        public ArrPacket(final Integer packetId, final String storageNumber, final Integer packetTypeId, final Integer fundId, final State state) {
            this.packetId = packetId;
            this.storageNumber = storageNumber;
            this.packetTypeId = packetTypeId;
            this.fundId = fundId;
            this.state = state;
        }

        public enum State {
            OPEN,
            CLOSED,
            CANCELED;
        }

        public Integer getPacketId() {
            return packetId;
        }

        public String getStorageNumber() {
            return storageNumber;
        }

        public Integer getPacketTypeId() {
            return packetTypeId;
        }

        public Integer getFundId() {
            return fundId;
        }

        public State getState() {
            return state;
        }
    }

    private static class RulPacketType {

        private static String TABLE = "rul_packet_type";
        private static String COL_PACKET_TYPE_ID = "packet_type_id";
        private static String COL_CODE = "code";
        private static String COL_NAME = "name";
        private static String COL_SHORTCUT = "shortcut";
        private static String COL_RULE_SET_ID = "rule_set_id";
        private static String COL_PACKAGE_ID = "package_id";

        private Integer packetTypeId;
        private String code;
        private String name;
        private String shortcut;
        private Integer ruleSetId;
        private Integer packageId;

        public RulPacketType(final Integer packetTypeId, final String code, final String name, final String shortcut, final Integer ruleSetId, final Integer packageId) {
            this.packetTypeId = packetTypeId;
            this.code = code;
            this.name = name;
            this.shortcut = shortcut;
            this.ruleSetId = ruleSetId;
            this.packageId = packageId;
        }

        public Integer getPacketTypeId() {
            return packetTypeId;
        }

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        public String getShortcut() {
            return shortcut;
        }
        public Integer getRuleSetId() {
            return ruleSetId;
        }

        public Integer getPackageId() {
            return packageId;
        }
    }

    private static class ArrStructureData {

        private static String TABLE = "arr_structure_data";
        private static String STRUCTURE_DATA_ID = "structure_data_id";

        private Integer structureDataId;
        private Integer createChangeId;
        private Integer structureTypeId;
        private Integer fundId;
        private Boolean assignable;
        private State state;

        public ArrStructureData(final Integer structureDataId, final Integer createChangeId, final Integer structureTypeId, final Integer fundId, final Boolean assignable, final State state) {
            this.structureDataId = structureDataId;
            this.createChangeId = createChangeId;
            this.structureTypeId = structureTypeId;
            this.fundId = fundId;
            this.assignable = assignable;
            this.state = state;
        }

        public Integer getStructureDataId() {
            return structureDataId;
        }

        public Integer getCreateChangeId() {
            return createChangeId;
        }

        public Integer getStructureTypeId() {
            return structureTypeId;
        }

        public Integer getFundId() {
            return fundId;
        }

        public Boolean getAssignable() {
            return assignable;
        }

        public State getState() {
            return state;
        }

        public enum State {
            TEMP,
            OK,
            ERROR
        }

    }

    private static class ArrDataPacketRef {

        private static String TABLE = "arr_data_packet_ref";
        private static String COL_DATA_ID = "data_id";
        private static String COL_PACKET_ID = "packet_id";

        private Integer dataId;
        private Integer packetId;

        public ArrDataPacketRef(final Integer dataId, final Integer packetId) {
            this.dataId = dataId;
            this.packetId = packetId;
        }

        public Integer getDataId() {
            return dataId;
        }

        public Integer getPacketId() {
            return packetId;
        }
    }
}
