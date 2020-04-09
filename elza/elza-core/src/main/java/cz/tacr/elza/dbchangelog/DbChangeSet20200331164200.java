package cz.tacr.elza.dbchangelog;

import cz.tacr.elza.asynchactions.UpdateConformityInfoWorker;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.DatabaseException;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DbChangeSet20200331164200 extends BaseTaskChange {


    /**
     * <changeSet id="20200331164201" author="gotzy">
     * <customChange class="cz.tacr.elza.dbchangelog.DbChangeSet20200331164200" />
     * </changeSet>
     */
    private JdbcConnection conn;
    private List<RulItemType> rulItemTypes;
    private List<RulDataType> rulDataTypes;
    private List<RulStructuredType> rulStructuredTypes;
    private List<RulItemSpec> rulItemSpecs;
    private Map<String, Integer> rulDataTypeMap;
    private Map<String, Integer> rulItemTypeMap;
    private Map<String, Integer> rulStructuredTypeMap;
    private Map<String, Integer> rulItemSpecMap;
    private Map<String, String> convertItemSpecMap;
    private Map<String, String> convertComplementMap;
    private Map<String, String> convertRelRoleTypeMap;
    private Map<String, String> convertRelEventMap;
    private Map<String, String> convertPersonPartyTypeItemSpec;
    private Map<String, String> convertExtinctionPartyTypeItemSpec;
    private RulPackage currentPackage;
    private ApChange apChange;
    private Map<String, DbSequence> hibernateSequences;

    private static final Logger logger = LoggerFactory.getLogger(UpdateConformityInfoWorker.class);

    @Override
    public void execute(final Database database) throws CustomChangeException {
        logger.info("Running DB Migration to Fragments");
        conn = (JdbcConnection) database.getConnection();
        try {
            beginApMigration();
        } catch (DatabaseException | SQLException e) {
            throw new CustomChangeException(
                    "Chyba při vykonávání sql příkazu " + e.getLocalizedMessage(), e);
        }
    }

    private void beginApMigration() throws DatabaseException, SQLException {
        hibernateSequences = findAllHibernateSequences().stream()
                .collect(Collectors.toMap(ds -> ds.getTable() + "|" + ds.getColumn(), Function.identity()));
        createApChange();
        createConvertSpecMap();
        createConvertComplementMap();
        createConvertRelEventMap();
        createConvertRoleTypeMap();
        createPersonConvertPartyTypeItemSpec();
        createExtinctionConvertPartyTypeItemSpec();
        getCurrentPackage();
        getRulItemTypes();
        getRulItemSpecs();
        getRulDataTypes();
        getRulStructuredTypes();

        //migrace přístupových bodů
        List<ApAccessPoint> accessPointList = getAccessPointsForMigration();
        for (ApAccessPoint accessPoint : accessPointList) {
            migrateExternalSystem(accessPoint);
            migrateDescription(accessPoint);
            migrateName(accessPoint);

        }

        //migrace "osob"
        accessPointList = getPersonsForMigration();
        for (ApAccessPoint accessPoint : accessPointList) {
            migrateExternalSystem(accessPoint);
            migrateParParty(accessPoint);

        }

        saveHibernateSequences();
    }

    private List<DbSequence> findAllHibernateSequences() throws DatabaseException, SQLException {
        List<DbSequence> dbSequences = new ArrayList<>();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM db_hibernate_sequences");
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

    private void createApChange() throws DatabaseException, SQLException {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        apChange = new ApChange(nextId("ap_change", ApChange.COL_CHANGE_ID),
                timestamp, null, "AP_IMPORT");
        PreparedStatement ps = conn.prepareStatement("INSERT INTO ap_change(change_id, change_date, user_id, type, external_system_id) " +
                " VALUES (?,?,?,?,?)");
        int i = 1;
        ps.setInt(i++, apChange.getChangeId());
        ps.setTimestamp(i++, apChange.getChangeDate());
        ps.setNull(i++, Types.INTEGER);
        ps.setString(i++, apChange.getType());
        ps.setNull(i++, Types.INTEGER);
        ps.executeUpdate();
    }

    private void getCurrentPackage() throws DatabaseException, SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT package_id, code, version FROM rul_package WHERE code = 'ZP2015'");
        ps.execute();
        try (ResultSet rs = ps.getResultSet();) {
            while (rs.next()) {
                currentPackage = new RulPackage(rs.getInt(RulPackage.COL_PACKAGE_ID),
                        rs.getString(RulPackage.COL_CODE),
                        rs.getInt(RulPackage.COL_VERSION));
            }
        }
    }

    private void getRulItemTypes() throws DatabaseException, SQLException {
        rulItemTypes = new ArrayList<>();
        rulItemTypeMap = new HashMap<>();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM rul_item_type ORDER BY item_type_id");
        ps.execute();
        try (ResultSet rs = ps.getResultSet();) {
            while (rs.next()) {
                RulItemType rulItemType = createRulItemType(rs);
                rulItemTypes.add(rulItemType);
                rulItemTypeMap.put(rulItemType.getCode(), rulItemType.getItemTypeId());
            }
        }
    }

    private RulItemType createRulItemType(final ResultSet rs) throws DatabaseException, SQLException {
        RulItemType result = new RulItemType();
        result.setItemTypeId(rs.getInt(RulItemType.COL_ITEM_TYPE_ID));
        result.setDataTypeId(rs.getInt(RulItemType.COL_DATA_TYPE_ID));
        result.setCode(rs.getString(RulItemType.COL_CODE));
        result.setName(rs.getString(RulItemType.COL_NAME));
        result.setShortcut(rs.getString(RulItemType.COL_SHORTCUT));
        result.setDescription(rs.getString(RulItemType.COL_DESCRIPTION));
        result.setValueUnique(rs.getBoolean(RulItemType.COL_IS_VALUE_UNIQUE));
        result.setCanBeOrdered(rs.getBoolean(RulItemType.COL_CAN_BE_ORDERED));
        result.setUseSpecification(rs.getBoolean(RulItemType.COL_USE_SPECIFICATION));
        result.setViewOrder(rs.getInt(RulItemType.COL_VIEW_ORDER));
        result.setViewDefinition(rs.getString(RulItemType.COL_VIEW_DEFINITION));
        result.setStructuredTypeId(rs.getInt(RulItemType.COL_STRUCTURED_TYPE_ID));
        result.setFragmentTypeId(rs.getInt(RulItemType.COL_FRAGMENT_TYPE_ID));
        return result;
    }

    private void getRulItemSpecs() throws DatabaseException, SQLException {
        rulItemSpecs = new ArrayList<>();
        rulItemSpecMap = new HashMap<>();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM rul_item_spec ORDER BY item_spec_id");
        ps.execute();
        try (ResultSet rs = ps.getResultSet();) {
            while (rs.next()) {
                RulItemSpec rulItemSpec = createRulItemSpec(rs);
                rulItemSpecs.add(rulItemSpec);
                rulItemSpecMap.put(rulItemSpec.getCode(), rulItemSpec.getItemSpecId());
            }
        }
    }

    private RulItemSpec createRulItemSpec(ResultSet rs) throws DatabaseException, SQLException {
        RulItemSpec result = new RulItemSpec(rs.getInt(RulItemSpec.COL_ITEM_SPEC_ID),
                rs.getString(RulItemSpec.COL_CODE),
                rs.getString(RulItemSpec.COL_NAME),
                rs.getString(RulItemSpec.COL_SHORTCUT),
                rs.getString(RulItemSpec.COL_DESCRIPTION),
                rs.getInt(RulItemSpec.COL_PACKAGE_ID),
                rs.getString(RulItemSpec.COL_CATEGORY));
        return result;

    }

    private void getRulDataTypes() throws DatabaseException, SQLException {
        rulDataTypes = new ArrayList<>();
        rulDataTypeMap = new HashMap<>();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM rul_data_type ORDER BY data_type_id");
        ps.execute();
        try (ResultSet rs = ps.getResultSet();) {
            while (rs.next()) {
                RulDataType dataType = createRulDataType(rs);
                rulDataTypes.add(dataType);
                rulDataTypeMap.put(dataType.getCode(), dataType.getDataTypeId());
            }
        }
    }

    private RulDataType createRulDataType(final ResultSet rs) throws DatabaseException, SQLException {
        RulDataType result = new RulDataType();
        result.setDataTypeId(rs.getInt(RulDataType.COL_DATA_TYPE_ID));
        result.setCode(rs.getString(RulDataType.COL_CODE));
        result.setName(rs.getString(RulDataType.COL_NAME));
        result.setDescription(rs.getString(RulDataType.COL_DESCRIPTION));
        result.setRegexpUse(rs.getBoolean(RulDataType.COL_REGEXP_USE));
        result.setTextLengthLimitUse(rs.getBoolean(RulDataType.COL_TEXT_LENGTH_LIMIT_USE));
        result.setStorageTable(rs.getString(RulDataType.COL_STORAGE_TABLE));
        result.setTextLengthLimit(rs.getInt(RulDataType.COL_TEXT_LENGTH_LIMIT));
        return result;
    }

    private void getRulStructuredTypes() throws DatabaseException, SQLException {
        rulStructuredTypes = new ArrayList<>();
        rulStructuredTypeMap = new HashMap<>();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM rul_structured_type ORDER BY structured_type_id");
        ps.execute();
        try (ResultSet rs = ps.getResultSet();) {
            while (rs.next()) {
                RulStructuredType structuredType = createStructuredDataType(rs);
                rulStructuredTypes.add(structuredType);
                rulStructuredTypeMap.put(structuredType.getCode(), structuredType.getStructuredTypeId());
            }
        }
    }

    private RulStructuredType createStructuredDataType(ResultSet rs) throws DatabaseException, SQLException {
        RulStructuredType result = new RulStructuredType(rs.getInt(RulStructuredType.COL_STRUCTURED_TYPE_ID),
                rs.getInt(RulStructuredType.COL_PACKAGE_ID),
                rs.getString(RulStructuredType.COL_NAME),
                rs.getString(RulStructuredType.COL_CODE),
                rs.getBoolean(RulStructuredType.COL_ANONYMOUS));
        return result;
    }

    private List<ApAccessPoint> getPersonsForMigration() throws DatabaseException, SQLException {
        List<ApAccessPoint> accessPointList = new ArrayList<>();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM ap_access_point WHERE access_point_id IN (SELECT access_point_id FROM par_party WHERE access_point_id IS NOT NULL)");
        ps.execute();
        try (ResultSet rs = ps.getResultSet()) {
            while (rs.next()) {
                accessPointList.add(createAccessPoint(rs));
            }
        }
        return accessPointList;
    }

    private List<ApAccessPoint> getAccessPointsForMigration() throws DatabaseException, SQLException {
        List<ApAccessPoint> accessPointList = new ArrayList<>();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM ap_access_point WHERE access_point_id NOT IN (SELECT access_point_id FROM par_party WHERE access_point_id IS NOT NULL)");
        ps.execute();
        try (ResultSet rs = ps.getResultSet();) {
            while (rs.next()) {
                accessPointList.add(createAccessPoint(rs));
            }
        }
        return accessPointList;
    }

    private ApAccessPoint createAccessPoint(final ResultSet rs) throws SQLException {
        return new ApAccessPoint(rs.getInt(ApAccessPoint.COL_ACCESS_POINT_ID),
                rs.getInt(ApAccessPoint.COL_UUID),
                rs.getInt(ApAccessPoint.COL_RULE_SYSTEM_ID),
                rs.getString(ApAccessPoint.COL_ERROR_DESCRIPTION),
                rs.getString(ApAccessPoint.COL_STATE),
                rs.getInt(ApAccessPoint.COL_VERSION),
                rs.getTimestamp(ApAccessPoint.COL_LAST_UPDATE),
                rs.getInt(ApAccessPoint.COL_PREFERRED_NAME_ITEM_ID));
    }

    private void migrateExternalSystem(ApAccessPoint accessPoint) throws DatabaseException, SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT value FROM ap_external_id WHERE delete_change_id IS NULL AND access_point_id = " + accessPoint.getAccessPointId());
        ps.execute();
        try (ResultSet rs = ps.getResultSet()) {
            while(rs.next()) {
                String value = rs.getString("value");
                if (value != null && !value.isEmpty()) {
                    Integer dataId = createArrData(rulDataTypeMap.get(DataTypeCode.APFRAG_REF.code));
                    Integer itemId = createApItem(accessPoint.getAccessPointId(), null, dataId, rulItemTypeMap.get(ItemTypeCode.PT_IDENT.code), null);
                    Integer fragmentId = createApFragment(dataId, rulStructuredTypeMap.get(StructuredTypeCode.PT_IDENT.code), null);

                    //zpracování typu - nenačítá se z DB, jenom staticky INTERPI
                    String itemTypeCode = ItemTypeCode.IDN_TYPE.code;
                    Integer dataTypeId = getRulItemTypeDataTypeId(itemTypeCode);
                    dataId = createArrData(dataTypeId);
                    itemId = createApItem(null, fragmentId, dataId, rulItemTypeMap.get(itemTypeCode), rulItemSpecMap.get("INTERPI"));
                    storeNullValue(dataId, dataTypeId);

                    //zpracování value
                    itemTypeCode = ItemTypeCode.IDN_VALUE.code;
                    dataTypeId = getRulItemTypeDataTypeId(itemTypeCode);
                    dataId = createArrData(dataTypeId);
                    itemId = createApItem(null, fragmentId, dataId, rulItemTypeMap.get(itemTypeCode), null);
                    storeStringValue(dataId, dataTypeId, value);
                }
            }
        }
    }

    private void migrateDescription(ApAccessPoint accessPoint) throws DatabaseException, SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT description FROM ap_description WHERE access_point_id = " + accessPoint.getAccessPointId() + " AND delete_change_id IS NULL");
        ps.execute();
        logger.info("Migrating ap_description");
        try (ResultSet rs = ps.getResultSet()) {
            while (rs.next()) {
                Integer dataId = createArrData(rulDataTypeMap.get(DataTypeCode.APFRAG_REF.code));
                Integer itemId = createApItem(accessPoint.getAccessPointId(), null, dataId, rulItemTypeMap.get(ItemTypeCode.PT_BODY.code), null);
                Integer fragmentId = createApFragment(dataId, rulStructuredTypeMap.get(StructuredTypeCode.PT_BODY.code), null);
                Integer dataTypeId = getRulItemTypeDataTypeId(ItemTypeCode.IDN_TYPE.code);
                dataId = createArrData(dataTypeId);
                itemId = createApItem(null, fragmentId, dataId, rulItemTypeMap.get(ItemTypeCode.BRIEF_DESC.code), null);
                storeStringValue(dataId, dataTypeId, rs.getString("description"));
            }
        }
    }

    private void migrateName(ApAccessPoint accessPoint) throws DatabaseException, SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT name, complement, preferred_name FROM ap_name WHERE access_point_id = " + accessPoint.getAccessPointId() + " AND delete_change_id IS NULL");
        ps.execute();
        logger.info("Migrating ap_name");
        try (ResultSet rs = ps.getResultSet()) {
            while (rs.next()) {
                //vytvoreni fragmentu
                Integer dataId = createArrData(rulDataTypeMap.get(DataTypeCode.APFRAG_REF.code));
                Integer itemId = createApItem(accessPoint.getAccessPointId(), null, dataId, rulItemTypeMap.get(ItemTypeCode.PT_NAME.code), null);
                if (rs.getBoolean("preferred_name")) {
                    updateAccessPoint(accessPoint, accessPoint.COL_PREFERRED_NAME_ITEM_ID, itemId);
                }
                Integer fragmentId = createApFragment(dataId, rulStructuredTypeMap.get(StructuredTypeCode.PT_NAME.code), null);
                //zpracování name
                Integer dataTypeId = getRulItemTypeDataTypeId(ItemTypeCode.NM_MAIN.code);
                dataId = createArrData(dataTypeId);
                itemId = createApItem(null, fragmentId, dataId, rulItemTypeMap.get(ItemTypeCode.NM_MAIN.code), null);
                storeStringValue(dataId, dataTypeId, rs.getString("name"));

                //zpracování complement
                dataTypeId = getRulItemTypeDataTypeId(ItemTypeCode.NM_SUP_GEN.code);
                dataId = createArrData(dataTypeId);
                itemId = createApItem(null, fragmentId, dataId, rulItemTypeMap.get(ItemTypeCode.NM_SUP_GEN.code), null);
                storeStringValue(dataId, dataTypeId, rs.getString("complement"));
            }
        }
    }

    private void migrateParParty(ApAccessPoint accessPoint) throws DatabaseException, SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT party_id, history, source_information, characteristics, code FROM par_party CROSS JOIN par_party_type WHERE par_party.party_type_id = par_party_type.party_type_id AND access_point_id = " + accessPoint.getAccessPointId() );
        ps.execute();
        logger.info("Migrating par_party");
        try (ResultSet rs = ps.getResultSet()) {
            while (rs.next()) {
                logger.info("Migrating par_party : " + rs.getInt("party_id"));
                //vytvoreni fragmentu
                Integer dataId = createArrData(rulDataTypeMap.get(DataTypeCode.APFRAG_REF.code));
                Integer itemId = createApItem(accessPoint.getAccessPointId(), null, dataId, rulItemTypeMap.get(ItemTypeCode.PT_BODY.code), null);
                Integer fragmentId = createApFragment(dataId, rulStructuredTypeMap.get(StructuredTypeCode.PT_BODY.code), null);
                //zpracování history
                String history = rs.getString("history");
                if (history != null && !history.isEmpty()) {
                    String itemTypeCode;
                    if (rs.getString("code").equals("PERSON")) {
                        itemTypeCode = ItemTypeCode.BIOGRAPHY.code;
                    } else {
                        itemTypeCode = ItemTypeCode.HISTORY.code;
                    }
                    Integer dataTypeId = getRulItemTypeDataTypeId(itemTypeCode);
                    dataId = createArrData(dataTypeId);
                    itemId = createApItem(null, fragmentId, dataId, rulItemTypeMap.get(itemTypeCode), null);
                    storeStringValue(dataId, dataTypeId, history); //TEXT
                }

                //zpracování source information
                String sourceInformation = rs.getString("source_information");
                if (sourceInformation != null && !sourceInformation.isEmpty()) {
                    Integer dataTypeId = getRulItemTypeDataTypeId(ItemTypeCode.SOURCE_INFO.code);
                    dataId = createArrData(dataTypeId);
                    itemId = createApItem(null, fragmentId, dataId, rulItemTypeMap.get(ItemTypeCode.SOURCE_INFO.code), null);
                    storeStringValue(dataId, dataTypeId, sourceInformation); //TEXT
                }

                //zpracování charakteristiky
                String characteristics = rs.getString("characteristics");
                if (characteristics != null && !characteristics.isEmpty()) {
                    Integer dataTypeId = getRulItemTypeDataTypeId(ItemTypeCode.BRIEF_DESC.code);
                    dataId = createArrData(dataTypeId);
                    itemId = createApItem(null, fragmentId, dataId, rulItemTypeMap.get(ItemTypeCode.BRIEF_DESC.code), null);
                    storeStringValue(dataId, dataTypeId, characteristics);
                }

                //migrate parties
                Integer partyId = rs.getInt("party_id");
                migrateParPartyGroup(accessPoint, partyId);
                migrateParDynasty(accessPoint, partyId);
                migrateParPartyName(accessPoint, partyId);
                migrateParPartyGroupIdentifier(accessPoint, partyId);
                migrateParRelation(accessPoint, partyId);
            }
        }
    }

    private void migrateParPartyGroup(ApAccessPoint accessPoint, Integer partyId) throws DatabaseException, SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT scope, founding_norm, scope_norm, organization FROM par_party_group WHERE party_id = " + partyId);
        ps.execute();

        try (ResultSet rs = ps.getResultSet()) {
            while (rs.next()) {
                //vytvoreni fragmentu
                Integer dataId = createArrData(rulDataTypeMap.get(DataTypeCode.APFRAG_REF.code));
                Integer itemId = createApItem(accessPoint.getAccessPointId(), null, dataId, rulItemTypeMap.get(ItemTypeCode.PT_BODY.code), null);
                Integer fragmentId = createApFragment(dataId, rulStructuredTypeMap.get(StructuredTypeCode.PT_BODY.code), null);
                //zpracování scope
                String text = rs.getString("scope");
                if (text != null && !text.isEmpty()) {
                    Integer dataTypeId = getRulItemTypeDataTypeId(ItemTypeCode.CORP_PURPOSE.code);
                    dataId = createArrData(dataTypeId);
                    itemId = createApItem(null, fragmentId, dataId, rulItemTypeMap.get(ItemTypeCode.CORP_PURPOSE.code), null);
                    storeStringValue(dataId, dataTypeId, text); //TEXT
                }
                //zpracování founding norm
                text = rs.getString("founding_norm");
                if (text != null && !text.isEmpty()) {
                    Integer dataTypeId = getRulItemTypeDataTypeId(ItemTypeCode.FOUNDING_NORMS.code);
                    dataId = createArrData(dataTypeId);
                    itemId = createApItem(null, fragmentId, dataId, rulItemTypeMap.get(ItemTypeCode.FOUNDING_NORMS.code), null);
                    storeStringValue(dataId, dataTypeId, text); //TEXT
                }
                //zpracování scope norm
                text = rs.getString("scope_norm");
                if (text != null && !text.isEmpty()) {
                    Integer dataTypeId = getRulItemTypeDataTypeId(ItemTypeCode.SCOPE_NORMS.code);
                    dataId = createArrData(dataTypeId);
                    itemId = createApItem(null, fragmentId, dataId, rulItemTypeMap.get(ItemTypeCode.SCOPE_NORMS.code), null);
                    storeStringValue(dataId, dataTypeId, text); //TEXT
                }
                //zpracování organization
                text = rs.getString("organization");
                if (text != null && !text.isEmpty()) {
                    Integer dataTypeId = getRulItemTypeDataTypeId(ItemTypeCode.CORP_STRUCTURE.code);
                    dataId = createArrData(dataTypeId);
                    itemId = createApItem(null, fragmentId, dataId, rulItemTypeMap.get(ItemTypeCode.CORP_STRUCTURE.code), null);
                    storeStringValue(dataId, dataTypeId, text);//TEXT
                }
            }
        }
    }

    private void migrateParDynasty(ApAccessPoint accessPoint, Integer partyId) throws DatabaseException, SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT party_id, genealogy FROM par_dynasty WHERE party_id = " + partyId);
        ps.execute();
        try (ResultSet rs = ps.getResultSet();) {
            while (rs.next()) {
                //vytvoreni fragmentu
                Integer dataId = createArrData(rulDataTypeMap.get(DataTypeCode.APFRAG_REF.code));
                Integer itemId = createApItem(accessPoint.getAccessPointId(), null, dataId, rulItemTypeMap.get(ItemTypeCode.PT_BODY.code), null);
                Integer fragmentId = createApFragment(dataId, rulStructuredTypeMap.get(StructuredTypeCode.PT_BODY.code), null);

                //zpracování genealogy
                String itemTypeCode = ItemTypeCode.GENEALOGY.code;
                Integer dataTypeId = getRulItemTypeDataTypeId(itemTypeCode);
                dataId = createArrData(dataTypeId);
                itemId = createApItem(null, fragmentId, dataId, rulItemTypeMap.get(itemTypeCode), null);
                storeStringValue(dataId, dataTypeId, rs.getString("genealogy")); //TEXT
            }
        }
    }

    private void migrateParPartyName(ApAccessPoint accessPoint, Integer partyId) throws DatabaseException, SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT party_name_id, valid_from_unitdate_id, valid_to_unitdate_id, " +
                "code, main_part, other_part, note, degree_before, degree_after " +
                "FROM par_party_name " +
                "CROSS JOIN par_party_name_form_type " +
                "WHERE par_party_name.name_form_type_id = par_party_name_form_type.name_form_type_id " +
                "AND party_id = " + partyId);
        ps.execute();
        try (ResultSet rs = ps.getResultSet()) {
            while (rs.next()) {
                //vytvoreni fragmentu
                Integer dataId = createArrData(rulDataTypeMap.get(DataTypeCode.APFRAG_REF.code));
                Integer itemId = createApItem(accessPoint.getAccessPointId(), null, dataId, rulItemTypeMap.get(ItemTypeCode.PT_NAME.code), null);
                Integer fragmentId = createApFragment(dataId, rulStructuredTypeMap.get(StructuredTypeCode.PT_NAME.code), null);

                //zpracování main_part
                String itemTypeCode = ItemTypeCode.NM_MAIN.code;
                Integer nmMainDataTypeId = getRulItemTypeDataTypeId(itemTypeCode);
                Integer nmMainDataId = createArrData(nmMainDataTypeId);
                itemId = createApItem(null, fragmentId, nmMainDataId, rulItemTypeMap.get(itemTypeCode), null);
                storeStringValue(nmMainDataId, nmMainDataTypeId, rs.getString("main_part"));

                //zpracování other_part
                String text = rs.getString("other_part");
                if (text != null && !text.isEmpty()) {
                    itemTypeCode = ItemTypeCode.NM_MINOR.code;
                    Integer dataTypeId = getRulItemTypeDataTypeId(itemTypeCode);
                    dataId = createArrData(dataTypeId);
                    itemId = createApItem(null, fragmentId, dataId, rulItemTypeMap.get(itemTypeCode), null);
                    storeStringValue(dataId, dataTypeId, text);
                }

                //zpracování degree_before
                text = rs.getString("degree_before");
                if (text != null && !text.isEmpty()) {
                    itemTypeCode = ItemTypeCode.NM_DEGREE_PRE.code;
                    Integer dataTypeId = getRulItemTypeDataTypeId(itemTypeCode);
                    dataId = createArrData(dataTypeId);
                    itemId = createApItem(null, fragmentId, dataId, rulItemTypeMap.get(itemTypeCode), null);
                    storeStringValue(dataId, dataTypeId, text);
                }

                //zpracování degree_after
                text = rs.getString("degree_after");
                if (text != null && !text.isEmpty()) {
                    itemTypeCode = ItemTypeCode.NM_DEGREE_POST.code;
                    Integer dataTypeId = getRulItemTypeDataTypeId(itemTypeCode);
                    dataId = createArrData(dataTypeId);
                    itemId = createApItem(null, fragmentId, dataId, rulItemTypeMap.get(itemTypeCode), null);
                    storeStringValue(dataId, dataTypeId, text);
                }

                //zpracování note
                text = rs.getString("note");
                if (text != null && !text.isEmpty()) {
                    itemTypeCode = ItemTypeCode.NOTE.code;
                    Integer dataTypeId = getRulItemTypeDataTypeId(itemTypeCode);
                    dataId = createArrData(dataTypeId);
                    itemId = createApItem(null, fragmentId, dataId, rulItemTypeMap.get(itemTypeCode), null);
                    storeStringValue(dataId, dataTypeId, text); //TEXT
                }
                //zpracování name_form_type_id
                itemTypeCode = ItemTypeCode.NM_TYPE.code;
                Integer dataTypeId = getRulItemTypeDataTypeId(itemTypeCode);
                dataId = createArrData(dataTypeId);
                itemId = createApItem(null, fragmentId, dataId, rulItemTypeMap.get(itemTypeCode), rulItemSpecMap.get(convertItemSpecMap.get(rs.getString("code"))));
                storeNullValue(dataId, dataTypeId);

                //zpracování valid_from_unitdate_id
                Integer validFromUnitdateId = rs.getInt("valid_from_unitdate_id");
                if (validFromUnitdateId != null) {
                    itemTypeCode = ItemTypeCode.NM_USED_FROM.code;
                    dataTypeId = getRulItemTypeDataTypeId(itemTypeCode);
                    dataId = createArrData(dataTypeId);
                    itemId = createApItem(null, fragmentId, dataId, rulItemTypeMap.get(itemTypeCode), null);
                    storeUnitdateValue(dataId, dataTypeId, validFromUnitdateId);
                }
                //zpracování valid_to_unitdate_id
                Integer validToUnitdateId = rs.getInt("valid_to_unitdate_id");
                if (validToUnitdateId != null) {
                    itemTypeCode = ItemTypeCode.NM_USED_TO.code;
                    dataTypeId = getRulItemTypeDataTypeId(itemTypeCode);
                    dataId = createArrData(dataTypeId);
                    itemId = createApItem(null, fragmentId, dataId, rulItemTypeMap.get(itemTypeCode), null);
                    storeUnitdateValue(dataId, dataTypeId, validToUnitdateId);
                }

                migrateParPartyNameComplement(accessPoint, rs.getInt("party_name_id"), nmMainDataId, nmMainDataTypeId);
            }
        }
    }

    private void migrateParPartyNameComplement(ApAccessPoint accessPoint, Integer partyNameId, Integer nmMainDataId, Integer nmMainDataTypeId) throws DatabaseException, SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT code, complement " +
                "FROM par_party_name_complement " +
                "CROSS JOIN par_complement_type " +
                "WHERE par_party_name_complement.complement_type_id = par_complement_type.complement_type_id " +
                "AND party_name_id = " + partyNameId);
        ps.execute();
        try (ResultSet rs = ps.getResultSet()) {
            while (rs.next()) {
                String code = rs.getString("code");
                if (code.equals("INITIALS") || code.equals("ROMAN_NUM")) {
                    String arrTableName = getDataStorageTable(nmMainDataTypeId);
                    ps = conn.prepareStatement("UPDATE " + arrTableName + " SET value = value || ' ' || " + dbString(rs.getString("complement")));
                    ps.executeUpdate();
                } else {

                    //vytvoreni fragmentu
                    Integer dataId = createArrData(rulDataTypeMap.get(DataTypeCode.APFRAG_REF.code));
                    Integer itemId = createApItem(accessPoint.getAccessPointId(), null, dataId, rulItemTypeMap.get(ItemTypeCode.PT_NAME.code), null);
                    Integer fragmentId = createApFragment(dataId, rulStructuredTypeMap.get(StructuredTypeCode.PT_NAME.code), null);

                    //zpracování complement
                    String itemTypeCode = convertComplementMap.get(code);
                    Integer dataTypeId = getRulItemTypeDataTypeId(itemTypeCode);
                    dataId = createArrData(dataTypeId);
                    itemId = createApItem(null, fragmentId, dataId, rulItemTypeMap.get(itemTypeCode), null);
                    storeStringValue(dataId, dataTypeId, rs.getString("complement"));
                }
            }
        }
    }

    private void migrateParPartyGroupIdentifier(ApAccessPoint accessPoint, Integer partyId) throws DatabaseException, SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT party_group_identifier_id, to_unitdate_id, from_unitdate_id, " +
                "party_id, source, note, identifier" +
                " FROM par_party_group_identifier WHERE party_id = " + partyId);
        ps.execute();
        try (ResultSet rs = ps.getResultSet();) {
            while (rs.next()) {
                //zpracování complement
                Integer dataId = createArrData(rulDataTypeMap.get(DataTypeCode.APFRAG_REF.code));
                Integer itemId = createApItem(accessPoint.getAccessPointId(), null, dataId, rulItemTypeMap.get(ItemTypeCode.PT_IDENT.code), null);
                Integer fragmentId = createApFragment(dataId, rulStructuredTypeMap.get(StructuredTypeCode.PT_IDENT.code), null);

                //zpracování to_unitdate_id
                int validToUnitdateId = rs.getInt("to_unitdate_id");
                if (validToUnitdateId > 0) {
                    String itemTypeCode = ItemTypeCode.IDN_VALID_TO.code;
                    Integer dataTypeId = getRulItemTypeDataTypeId(itemTypeCode);
                    dataId = createArrData(dataTypeId);
                    itemId = createApItem(null, fragmentId, dataId, rulItemTypeMap.get(itemTypeCode), null);
                    storeUnitdateValue(dataId, dataTypeId, validToUnitdateId);
                }

                //zpracování from_unitdate_id
                int validFromUnitdateId = rs.getInt("from_unitdate_id");
                if (validFromUnitdateId > 0) {
                    String itemTypeCode = ItemTypeCode.IDN_VALID_FROM.code;
                    Integer dataTypeId = getRulItemTypeDataTypeId(itemTypeCode);
                    dataId = createArrData(dataTypeId);
                    itemId = createApItem(null, fragmentId, dataId, rulItemTypeMap.get(itemTypeCode), null);
                    storeUnitdateValue(dataId, dataTypeId, validFromUnitdateId);
                }

                //zpracování source
                String text = rs.getString("source");
                String noteText = null;
                if(convertItemSpecMap.containsKey(rs.getString("source"))) {
                    if (text != null && !text.isEmpty()) {
                        String itemTypeCode = ItemTypeCode.IDN_TYPE.code;
                        Integer dataTypeId = getRulItemTypeDataTypeId(itemTypeCode);
                        dataId = createArrData(dataTypeId);
                        itemId = createApItem(null, fragmentId, dataId, rulItemTypeMap.get(itemTypeCode), rulItemSpecMap.get(convertItemSpecMap.get(rs.getString("source"))));
                        storeNullValue(dataId, dataTypeId);
                    }
                } else {
                    noteText = rs.getString("source");
                }

                //zpracování note
                text = rs.getString("note");
                if ((text != null && !text.isEmpty()) || (noteText != null && !noteText.isEmpty())) {
                    String itemTypeCode = ItemTypeCode.NOTE.code;
                    Integer dataTypeId = getRulItemTypeDataTypeId(itemTypeCode);
                    dataId = createArrData(dataTypeId);
                    itemId = createApItem(null, fragmentId, dataId, rulItemTypeMap.get(itemTypeCode), null);
                    if((noteText != null && !noteText.isEmpty())) {
                        if(text == null) {
                            text = "";
                        }
                        text += " Nerozpoznaný identifikátor : " + noteText;
                    }
                    storeStringValue(dataId, dataTypeId, text);
                }

                //zpracování identifier
                text = rs.getString("identifier");
                if (text != null && !text.isEmpty()) {
                    String itemTypeCode = ItemTypeCode.IDN_VALUE.code;
                    Integer dataTypeId = getRulItemTypeDataTypeId(itemTypeCode);
                    dataId = createArrData(dataTypeId);
                    itemId = createApItem(null, fragmentId, dataId, rulItemTypeMap.get(itemTypeCode), null);
                    storeStringValue(dataId, dataTypeId, text);
                }
            }
        }
    }

    private void migrateParRelation(ApAccessPoint accessPoint, Integer partyId) throws DatabaseException, SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT relation_id, par_relation.party_id, par_relation_type.code AS relation_type_code, from_unitdate_id, to_unitdate_id, " +
                "note, par_relation.version, source,  par_party_type.code AS party_type_code " +
                "FROM par_relation " +
                "CROSS JOIN par_relation_type " +
                "CROSS JOIN par_party " +
                "CROSS JOIN par_party_type " +
                "WHERE par_relation_type.relation_type_id = par_relation.relation_type_id " +
                "AND par_party.party_id = par_relation.party_id " +
                "AND par_party.party_type_id = par_party_type.party_type_id " +
                "AND par_relation.party_id = " + partyId);
        ps.execute();
        try (ResultSet rs = ps.getResultSet()) {
            while (rs.next()) {
                if (convertRelEventMap.get(rs.getString("relation_type_code")).equals(StructuredTypeCode.PT_REL.code)) {
                    createPTRelFragment(accessPoint, rs);
                } else {
                    createOtherRelFragment(accessPoint, rs);
                }
            }
        }
    }

    private void createPTRelFragment(ApAccessPoint accessPoint, ResultSet rs) throws DatabaseException, SQLException {
        Integer parRelationId = rs.getInt("relation_id");
        PreparedStatement ps = conn.prepareStatement("SELECT record_id, note, code " +
                "FROM par_relation_entity " +
                "CROSS JOIN par_relation_role_type " +
                "WHERE par_relation_entity.role_type_id = par_relation_role_type.role_type_id " +
                "AND relation_id = " + parRelationId);
        ps.execute();
        try (ResultSet rsEntity = ps.getResultSet()) {
            while (rsEntity.next()) {
                //zpracování complement
                Integer dataId = createArrData(rulDataTypeMap.get(DataTypeCode.APFRAG_REF.code));
                Integer itemId = createApItem(accessPoint.getAccessPointId(), null, dataId, rulItemTypeMap.get(ItemTypeCode.PT_REL.code),
                        rulItemSpecMap.get(convertRelRoleTypeMap.get(rsEntity.getString("code"))));
                Integer fragmentId = createApFragment(dataId, rulStructuredTypeMap.get(StructuredTypeCode.PT_REL.code), null);

                //zpracování from_unitdate_id
                int fromUnitdateId = rs.getInt("from_unitdate_id");
                String noteUnitDateFrom = null;
                if (fromUnitdateId > 0) {
                    String itemTypeCode = ItemTypeCode.REL_BEGIN.code;
                    Integer dataTypeId = getRulItemTypeDataTypeId(itemTypeCode);
                    dataId = createArrData(dataTypeId);
                    itemId = createApItem(null, fragmentId, dataId, rulItemTypeMap.get(itemTypeCode), null);
                    noteUnitDateFrom = storeUnitdateValue(dataId, dataTypeId, fromUnitdateId);
                }

                //zpracování to_unitdate_id
                int validToUnitdateId = rs.getInt("to_unitdate_id");
                String noteUnitDateTo = null;
                if (validToUnitdateId > 0) {
                    String itemTypeCode = ItemTypeCode.REL_END.code;
                    Integer dataTypeId = getRulItemTypeDataTypeId(itemTypeCode);
                    dataId = createArrData(dataTypeId);
                    itemId = createApItem(null, fragmentId, dataId, rulItemTypeMap.get(itemTypeCode), null);
                    noteUnitDateTo = storeUnitdateValue(dataId, dataTypeId, validToUnitdateId);
                }

                //zpracování note
                String text = rs.getString("note");
                String textEntity = rsEntity.getString("note");
                if ((text != null && !text.isEmpty()) || (textEntity != null && !textEntity.isEmpty())
                        || (noteUnitDateFrom != null && !noteUnitDateFrom.isEmpty()) || (noteUnitDateTo != null && !noteUnitDateTo.isEmpty())) {
                    String itemTypeCode = ItemTypeCode.NOTE.code;
                    Integer dataTypeId = getRulItemTypeDataTypeId(itemTypeCode);
                    dataId = createArrData(dataTypeId);
                    itemId = createApItem(null, fragmentId, dataId, rulItemTypeMap.get(itemTypeCode), null);
                    if (textEntity != null && !textEntity.isEmpty()) {
                        if(text == null) {
                            text = "";
                        }
                        text += " Poznámka z entity :" + textEntity;
                    }
                    if (noteUnitDateFrom != null && !noteUnitDateFrom.isEmpty()) {
                        if(text == null) {
                            text = "";
                        }
                        text += " Poznámka k dataci : " + noteUnitDateFrom;
                    }
                    if (noteUnitDateTo != null && !noteUnitDateTo.isEmpty()) {
                        if(text == null) {
                            text = "";
                        }
                        text += " Poznámka k dataci : " + noteUnitDateFrom;
                    }
                    storeStringValue(dataId, dataTypeId, text);
                }

                //zpracování recordId
                String itemTypeCode = ItemTypeCode.REL_ENTITY.code;
                Integer dataTypeId = getRulItemTypeDataTypeId(itemTypeCode);
                dataId = createArrData(dataTypeId);
                itemId = createApItem(null, fragmentId, dataId, rulItemTypeMap.get(itemTypeCode), null);
                storeRecordRefValue(dataId, dataTypeId, rsEntity.getInt("record_id"));
            }
        }
    }

    private void createOtherRelFragment(ApAccessPoint accessPoint, ResultSet rs) throws DatabaseException, SQLException {
        Integer parRelationId = rs.getInt("relation_id");
        String itemSpecCode = null;
        String fromUnitdateTypeCode = null;
        String toUnitdateTypeCode = null;
        if (rs.getString("relation_type_code").equals("CREATION")) {
            itemSpecCode = convertPersonPartyTypeItemSpec.get(rs.getString("party_type_code"));
        } else if (rs.getString("relation_type_code").equals("EXTINCTION")) {
            itemSpecCode = convertExtinctionPartyTypeItemSpec.get(rs.getString("party_type_code"));
        } else {
            itemSpecCode = convertItemSpecMap.get(rs.getString("relation_type_code"));
        }

        //zpracování complement
        Integer dataId = createArrData(rulDataTypeMap.get(DataTypeCode.APFRAG_REF.code));
        Integer itemId = createApItem(accessPoint.getAccessPointId(), null, dataId, rulItemTypeMap.get(convertRelEventMap.get(rs.getString("relation_type_code"))), rulItemSpecMap.get(itemSpecCode));
        Integer fragmentId = createApFragment(dataId, rulStructuredTypeMap.get(convertRelEventMap.get(rs.getString("relation_type_code"))), null);

        if (convertRelEventMap.get(rs.getString("relation_type_code")).equals("PT_CRE")) {
            fromUnitdateTypeCode = "CRE_DATE";
        } else if (convertRelEventMap.get(rs.getString("relation_type_code")).equals("PT_EVENT")) {
            fromUnitdateTypeCode = "EV_BEGIN";
            toUnitdateTypeCode = "EV_END";
        } else if (convertRelEventMap.get(rs.getString("relation_type_code")).equals("PT_EXT")) {
            toUnitdateTypeCode = "EXT_DATE";
        }
        //zpracování from_unitdate_id
        int fromUnitdateId = rs.getInt("from_unitdate_id");
        String noteUnitDateFrom = null;
        if (fromUnitdateId > 0 && fromUnitdateTypeCode != null) {
            String itemTypeCode = fromUnitdateTypeCode;
            Integer dataTypeId = getRulItemTypeDataTypeId(itemTypeCode);
            dataId = createArrData(dataTypeId);
            itemId = createApItem(null, fragmentId, dataId, rulItemTypeMap.get(itemTypeCode), null);
            noteUnitDateFrom = storeUnitdateValue(dataId, dataTypeId, fromUnitdateId);
        }

        //zpracování to_unitdate_id
        int validToUnitdateId = rs.getInt("to_unitdate_id");
        String noteUnitDateTo = null;
        if (validToUnitdateId > 0 && toUnitdateTypeCode != null) {
            String itemTypeCode = toUnitdateTypeCode;
            Integer dataTypeId = getRulItemTypeDataTypeId(itemTypeCode);
            dataId = createArrData(dataTypeId);
            itemId = createApItem(null, fragmentId, dataId, rulItemTypeMap.get(itemTypeCode), null);
            noteUnitDateTo = storeUnitdateValue(dataId, dataTypeId, validToUnitdateId);
        }

        //zpracování note
        String text = rs.getString("note");
        if ((text != null && !text.isEmpty()) || (noteUnitDateFrom != null && !noteUnitDateFrom.isEmpty()) || (noteUnitDateTo != null && !noteUnitDateTo.isEmpty())) {
            String itemTypeCode = ItemTypeCode.NOTE.code;
            Integer dataTypeId = getRulItemTypeDataTypeId(itemTypeCode);
            dataId = createArrData(dataTypeId);
            itemId = createApItem(null, fragmentId, dataId, rulItemTypeMap.get(itemTypeCode), null);
            if (noteUnitDateFrom != null && !noteUnitDateFrom.isEmpty()) {
                if(text == null) {
                    text = "";
                }
                text += " Poznámka k dataci : " + noteUnitDateFrom;
            }
            if (noteUnitDateTo != null && !noteUnitDateTo.isEmpty()) {
                if(text == null) {
                    text = "";
                }
                text += " Poznámka k dataci : " + noteUnitDateFrom;
            }
            storeStringValue(dataId, dataTypeId, text);
        }

        //zpracování source
        text = rs.getString("source");
        if (text != null && !text.isEmpty()) {
            String itemTypeCode = ItemTypeCode.SOURCE_INFO.code;
            Integer dataTypeId = getRulItemTypeDataTypeId(itemTypeCode);
            dataId = createArrData(dataTypeId);
            itemId = createApItem(null, fragmentId, dataId, rulItemTypeMap.get(itemTypeCode), null);
            storeStringValue(dataId, dataTypeId, text);
        }
        createRelationEntities(accessPoint.getAccessPointId(), parRelationId, fragmentId);
    }

    private void createRelationEntities(Integer accessPointId, Integer parRelationId, Integer parentFragmentId) throws DatabaseException, SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT record_id, note, code " +
                "FROM par_relation_entity " +
                "CROSS JOIN par_relation_role_type " +
                "WHERE par_relation_entity.role_type_id = par_relation_role_type.role_type_id " +
                "AND relation_id = " + parRelationId);
        ps.execute();
        try (ResultSet rs = ps.getResultSet()) {
            while (rs.next()) {
                Integer dataId = createArrData(rulDataTypeMap.get(DataTypeCode.APFRAG_REF.code));
                Integer itemId = createApItem(accessPointId, null, dataId, rulItemTypeMap.get(ItemTypeCode.PT_REL.code), null);
                Integer fragmentId = createApFragment(dataId, rulStructuredTypeMap.get(StructuredTypeCode.PT_REL.code), parentFragmentId);

                //zpracování note
                String text = rs.getString("note");
                if (text != null && !text.isEmpty()) {
                    String itemTypeCode = ItemTypeCode.NOTE.code;
                    Integer dataTypeId = getRulItemTypeDataTypeId(itemTypeCode);
                    dataId = createArrData(dataTypeId);
                    itemId = createApItem(null, fragmentId, dataId, rulItemTypeMap.get(itemTypeCode), null);
                    storeStringValue(dataId, dataTypeId, text);
                }

                //zpracování recordId
                String itemTypeCode = ItemTypeCode.REL_ENTITY.code;
                Integer dataTypeId = getRulItemTypeDataTypeId(itemTypeCode);
                dataId = createArrData(dataTypeId);
                itemId = createApItem(null, fragmentId, dataId, rulItemTypeMap.get(itemTypeCode), null);
                storeRecordRefValue(dataId, dataTypeId, rs.getInt("record_id"));
            }
        }

    }

    private void updateAccessPoint(ApAccessPoint accessPoint, String column, Integer value) throws DatabaseException, SQLException {
        PreparedStatement ps = conn.prepareStatement("UPDATE ap_access_point SET " + column + " = " + value +
                " WHERE " + accessPoint.COL_ACCESS_POINT_ID + " = " + accessPoint.getAccessPointId());
        ps.executeUpdate();
    }

    private Integer getRulItemTypeDataTypeId(String code) {
        for (RulItemType rulItemType : rulItemTypes) {
            if (rulItemType.getCode().equals(code)) {
                return rulItemType.getDataTypeId();
            }
        }
        return null;
    }

    private String getDataStorageTable(Integer dataTypeId) {
        for (RulDataType rulDataType : rulDataTypes) {
            if (rulDataType.getDataTypeId().equals(dataTypeId)) {
                return rulDataType.getStorageTable();
            }
        }
        return null;
    }

    private Integer createApItem(Integer accessPointId, Integer fragmentId, Integer dataId, Integer itemTypeId, Integer itemSpecId) throws DatabaseException, SQLException {
        Integer itemId = nextId("ap_item", "item_id");
        PreparedStatement ps = conn.prepareStatement("INSERT INTO ap_item(item_id, data_id, item_type_id, item_spec_id, create_change_id, delete_change_id, object_id, \"position\")" +
                " VALUES (?, ?, ?, ?, ?, ?, ?, ?);");
        int i = 1;
        ps.setInt(i++, itemId);
        ps.setInt(i++, dataId);
        ps.setInt(i++, itemTypeId);
        if (itemSpecId == null) {
            ps.setNull(i++, Types.INTEGER);
        } else {
            ps.setInt(i++, itemSpecId);
        }
        ps.setInt(i++, apChange.changeId);
        ps.setNull(i++, Types.INTEGER);
        ps.setInt(i++, nextId("ap_item", "object_id"));
        ps.setInt(i++, 1);
        ps.executeUpdate();

        if (accessPointId != null) {
            ps = conn.prepareStatement("INSERT INTO ap_access_point_item(item_id, access_point_id)" +
                    " VALUES (?, ?);");
            i = 1;
            ps.setInt(i++, itemId);
            ps.setInt(i++, accessPointId);
            ps.executeUpdate();
        }

        if (fragmentId != null) {
            ps = conn.prepareStatement("INSERT INTO ap_fragment_item(item_id, fragment_id)" +
                    " VALUES (?, ?);");
            i = 1;
            ps.setInt(i++, itemId);
            ps.setInt(i++, fragmentId);
            ps.executeUpdate();
        }

        return itemId;
    }

    private Integer createApFragment(Integer dataId, Integer fragmentTypeId, Integer fragmentParentId) throws DatabaseException, SQLException {
        Integer fragmentId = nextId("ap_fragment", "fragment_id");
        PreparedStatement ps = conn.prepareStatement("INSERT INTO ap_fragment(fragment_id, value, error_description, state, fragment_type_id, ap_fragment_parent_id)" +
                " VALUES (?, ?, ?, ?, ?, ?);");
        int i = 1;
        ps.setInt(i++, fragmentId);
        ps.setString(i++, "Bude generováno");
        ps.setNull(i++, Types.VARCHAR);
        ps.setString(i++, "OK");
        ps.setInt(i++, fragmentTypeId);
        if (fragmentParentId == null) {
            ps.setNull(i++, Types.INTEGER);
        } else {
            ps.setInt(i++, fragmentParentId);
        }
        ps.executeUpdate();

        ps = conn.prepareStatement("INSERT INTO arr_data_apfrag_ref(data_id, fragment_id)" +
                " VALUES (?, ?);");
        i = 1;
        ps.setInt(i++, dataId);
        ps.setInt(i++, fragmentId);
        ps.executeUpdate();

        return fragmentId;

    }

    private Integer createArrData(Integer dataTypeId) throws DatabaseException, SQLException {
        Integer dataId = nextId("arr_data", "data_id");
        PreparedStatement ps = conn.prepareStatement("INSERT INTO arr_data(data_id, data_type_id, version) " +
                "VALUES(?,?,?)");
        int i = 1;
        ps.setInt(i++, dataId);
        ps.setInt(i++, dataTypeId);
        ps.setInt(i++, 0);
        ps.executeUpdate();
        return dataId;
    }

    private void storeStringValue(Integer dataId, Integer dataTypeId, String value) throws DatabaseException, SQLException {
        if(value != null) {
            String table = getDataStorageTable(dataTypeId);
            PreparedStatement ps = conn.prepareStatement("INSERT INTO " + table + "(data_id, value) " +
                    "VALUES(?,?)");
            int i = 1;
            ps.setInt(i++, dataId);
            ps.setString(i++, value);
            ps.executeUpdate();
        }
    }

    private void storeRecordRefValue(Integer dataId, Integer dataTypeId, Integer value) throws DatabaseException, SQLException {
        String table = getDataStorageTable(dataTypeId);

        PreparedStatement ps = conn.prepareStatement("INSERT INTO " + table + "(data_id, record_id) " +
                "VALUES(?,?)");
        int i = 1;
        ps.setInt(i++, dataId);
        ps.setInt(i++, value);
        ps.executeUpdate();
    }

    private String storeUnitdateValue(Integer dataId, Integer dataTypeId, Integer unitdateId) throws DatabaseException, SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT unitdate_id, par_unitdate.calendar_type_id, code, value_from, value_from_estimated, value_to, value_to_estimated, " +
                "format, text_date, note FROM par_unitdate CROSS JOIN arr_calendar_type " +
                "WHERE arr_calendar_type.calendar_type_id = par_unitdate.calendar_type_id " +
                "AND unitdate_id = " + unitdateId);
        ps.execute();
        String dateNote = null;
        try (ResultSet rs = ps.getResultSet()) {
            while (rs.next()) {
                dateNote = rs.getString("note");

                Integer calendarTypeId = rs.getInt("calendar_type_id");
                //CalendarType calType = CalendarType.fromId(calendarTypeId);
                String valueFrom = rs.getString("value_from");
                Boolean valueFromEstimated = rs.getBoolean("value_from_estimated");
                // prepare normalized values - from
                Long normalizedFrom;
                if (valueFrom != null && !valueFrom.isEmpty()) {
                    LocalDateTime locDateTime = LocalDateTime.parse(valueFrom, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    CalendarConverter calendarConverter = new CalendarConverter();
                    normalizedFrom = calendarConverter.toSeconds(rs.getString("code"), locDateTime);
                } else {
                    normalizedFrom = Long.MIN_VALUE;
                }

                // prepare normalized values - to
                String valueTo = rs.getString("value_to");
                Boolean valueToEstimated = rs.getBoolean("value_to_estimated");
                Long normalizedTo;
                if (valueTo != null && !valueTo.isEmpty()) {
                    LocalDateTime locDateTime = LocalDateTime.parse(valueTo, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    CalendarConverter calendarConverter = new CalendarConverter();
                    normalizedTo = calendarConverter.toSeconds(rs.getString("code"), locDateTime);
                } else {
                    normalizedTo = Long.MAX_VALUE;
                }
                String format = rs.getString("format");
                if(format != null && normalizedFrom != null && normalizedTo != null) {
                    PreparedStatement psu = conn.prepareStatement("INSERT INTO arr_data_unitdate(data_id, calendar_type_id, value_from, value_from_estimated, " +
                            "value_to, value_to_estimated, format, normalized_from, normalized_to) " +
                            " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
                    int i = 1;
                    psu.setInt(i++, dataId);
                    psu.setInt(i++, calendarTypeId);
                    psu.setString(i++, valueFrom);
                    psu.setBoolean(i++, valueFromEstimated);
                    psu.setString(i++, valueTo);
                    psu.setBoolean(i++, valueToEstimated);
                    psu.setString(i++, format);
                    psu.setLong(i++, normalizedFrom);
                    psu.setLong(i++, normalizedTo);
                    psu.executeUpdate();
                }
            }
        }
        return dateNote;
    }

   /*private void storeTextValue(Integer dataId, Integer dataTypeId, String value) throws DatabaseException, SQLException {
        String table = getDataStorageTable(dataTypeId);
        PreparedStatement ps = conn.prepareStatement("INSERT INTO " + table + "(data_id, value) " +
                "VALUES(?,?)");
        int i = 1;
        ps.setInt(i++, dataId);
        ps.setString(i++, value);
        ps.executeUpdate();
    }*/

    private void storeNullValue(Integer dataId, Integer dataTypeId) throws DatabaseException, SQLException {
        String table = getDataStorageTable(dataTypeId);
        PreparedStatement ps = conn.prepareStatement("INSERT INTO " + table + "(data_id) " +
                "VALUES(?)");
        int i = 1;
        ps.setInt(i++, dataId);
        ps.executeUpdate();
    }

    private Integer nextId(final String table, final String column) throws DatabaseException, SQLException {
        DbSequence dbSequence = hibernateSequences.get(table + "|" + column);
        if (dbSequence == null) {
            dbSequence = new DbSequence(table, column, 1);
            PreparedStatement ps = conn.prepareStatement("INSERT INTO db_hibernate_sequences (" + DbSequence.SEQUENCE_NAME + "," + DbSequence.NEXT_VAL + ") " +
                    "VALUES (?, ?);");
            ps.setString(1, table + "|" + column);
            ps.setInt(2, 1);
            ps.executeUpdate();
            hibernateSequences.put(table + "|" + column, dbSequence);
        }
        return dbSequence.nextVal();
    }

    private void saveHibernateSequences() throws SQLException, DatabaseException {
        for (DbSequence dbSequence : hibernateSequences.values()) {
            if (dbSequence.isChange()) {
                PreparedStatement ps = conn.prepareStatement("UPDATE db_hibernate_sequences SET " + DbSequence.NEXT_VAL + "=? WHERE " + DbSequence.SEQUENCE_NAME + "=?;");
                // append safety constant to sequence generator
                ps.setInt(1, dbSequence.getNextVal() + 20);
                ps.setString(2, dbSequence.getTable() + "|" + dbSequence.getColumn());
                ps.executeUpdate();
            }
        }
    }

    private String dbString(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        } else {
            return "'" + input + "'";
        }
    }

    private void createConvertSpecMap() {
        convertItemSpecMap = new HashMap<>();
        convertItemSpecMap.put("ANTONYM", "NT_ANTONYMUM");
        convertItemSpecMap.put("AUTHOR", "NT_AUTHORCIPHER");
        convertItemSpecMap.put("CHURCH", "NT_RELIGIOUS");
        convertItemSpecMap.put("HISTORICAL", "NT_HISTORICAL");
        convertItemSpecMap.put("HISTORICAL2", "NT_FORMER");
        convertItemSpecMap.put("HISTORICAL4", "");
        convertItemSpecMap.put("HOMONYM", "NT_HOMONYMUM");
        convertItemSpecMap.put("INVERTED", "NT_INVERTED");
        convertItemSpecMap.put("HISTORICAL3", "NT_ONLYKNOWN");
        convertItemSpecMap.put("INAPPROPRIATE", "NT_INAPPROPRIATE");
        convertItemSpecMap.put("SPECIAL", "NT_TERM");
        convertItemSpecMap.put("PLURAL", "NT_PLURAL");
        convertItemSpecMap.put("TERM", "NT_TAKEN");
        convertItemSpecMap.put("MARRIAGE", "NT_ACCEPTED");
        convertItemSpecMap.put("ORDER", "NT_DIRECT");
        convertItemSpecMap.put("PSEUDONYM", "NT_PSEUDONYM");
        convertItemSpecMap.put("SINGULAR", "NT_SINGULAR");
        convertItemSpecMap.put("USED", "NT_ACTUAL");
        convertItemSpecMap.put("LEGAL", "NT_SECULAR");
        convertItemSpecMap.put("ARTIFICIAL", "NT_ARTIFICIAL");
        convertItemSpecMap.put("LEGAL2", "NT_OFFICIAL");
        convertItemSpecMap.put("NARROW", "NT_NARROWER");
        convertItemSpecMap.put("IC", "IC");
        convertItemSpecMap.put("IČ", "IC");
        convertItemSpecMap.put("IČO", "IC");
        convertItemSpecMap.put("ICO", "IC");
        convertItemSpecMap.put("DIČ", "VAT");
        convertItemSpecMap.put("DIC", "DIC");
        convertItemSpecMap.put("ACTIVE_FROM", "CRC_BEGINSCOPE");
        convertItemSpecMap.put("MEMBERSHIP", "ET_MEMBERSHIP");
        convertItemSpecMap.put("APPRECIATION", "ET_AWARD");
        convertItemSpecMap.put("STUDY", "ET_STUDY");
        convertItemSpecMap.put("MARRIAGE", "ET_MARRIAGE");
        convertItemSpecMap.put("EMPLOYMENT", "ET_JOB");
        convertItemSpecMap.put("ACTIVE_TO", "EXC_ENDSCOPE");
    }

    private void createConvertComplementMap() {
        convertComplementMap = new HashMap<>();
        convertComplementMap.put("GENERAL", "NM_SUP_GEN");
        convertComplementMap.put("GEO", "NM_SUP_GEO");
        convertComplementMap.put("TIME", "NM_SUP_CHRO");
        convertComplementMap.put("ORDER", "NM_ORDER");
    }

    private void createConvertRoleTypeMap() {
        convertRelRoleTypeMap = new HashMap<>();
        convertRelRoleTypeMap.put("BROTHER", "RT_BROTHER");
        convertRelRoleTypeMap.put("UNIT", "RT_WHOLE");
        convertRelRoleTypeMap.put("PARTY_GROUP_MEMBER", "RT_ISMEMBER");
        convertRelRoleTypeMap.put("FAMILY_MEMBER", "RT_ISMEMBER");
        convertRelRoleTypeMap.put("MEMBER_OF", "RT_MEMBERORG");
        convertRelRoleTypeMap.put("DAUGHTER", "RT_RELATIONS");
        convertRelRoleTypeMap.put("CREATION", "RT_RELATED");
        convertRelRoleTypeMap.put("OTHER_IDENTITY", "RT_OTHERNAME");
        convertRelRoleTypeMap.put("OTHER_RELATIVE", "RT_RELATIONS");
        convertRelRoleTypeMap.put("COLLEAGUE", "RT_COLLABORATOR");
        convertRelRoleTypeMap.put("LIQUIDATOR", "RT_LIQUIDATOR");
        convertRelRoleTypeMap.put("SPOUSE", "RT_HUSBAND");
        convertRelRoleTypeMap.put("MOTHER", "RT_MOTHER");
        convertRelRoleTypeMap.put("PLACE", "RT_PLACE");
        convertRelRoleTypeMap.put("SUPERIOR", "RT_RELATED");
        convertRelRoleTypeMap.put("SUPERIOR_PERSON", "RT_SENIOR");
        convertRelRoleTypeMap.put("SUCCESSOR", "RT_SUCCESSOR");
        convertRelRoleTypeMap.put("ORGANIZER", "RT_ORGANIZER");
        convertRelRoleTypeMap.put("FATHER", "RT_FATHER");
        convertRelRoleTypeMap.put("SUBORDINATE", "RT_RELATED");
        convertRelRoleTypeMap.put("SUBORDINATE_PERSON", "RT_RELATED");
        convertRelRoleTypeMap.put("NAMED_AFTER", "RT_NAMEDAFTER");
        convertRelRoleTypeMap.put("LAST_MEMBER", "RT_LASTKMEMBER");
        convertRelRoleTypeMap.put("WORKPLACE", "RT_RELATED");
        convertRelRoleTypeMap.put("FIRST_MEMBER", "RT_FIRSTKMEMBER");
        convertRelRoleTypeMap.put("PREDECESSOR", "RT_PREDECESSOR");
        convertRelRoleTypeMap.put("RECEIVER", "RT_RELATED");
        convertRelRoleTypeMap.put("DOCUMENT", "RT_DOCUMENT");
        convertRelRoleTypeMap.put("SISTER", "RT_SISTER");
        convertRelRoleTypeMap.put("RESIDENCY", "RT_RESIDENCE");
        convertRelRoleTypeMap.put("RELATED", "RT_RELATED");
        convertRelRoleTypeMap.put("RELATED_FAMILY", "RT_RELATED");
        convertRelRoleTypeMap.put("SCHOOLMATE", "RT_SCHOOLMATE");
        convertRelRoleTypeMap.put("SON", "RT_RELATIONS");
        convertRelRoleTypeMap.put("SCHOOL", "RT_SCHOOL");
        convertRelRoleTypeMap.put("TEACHER", "RT_TEACHER");
        convertRelRoleTypeMap.put("AWARD", "RT_AWARD");
        convertRelRoleTypeMap.put("CEREMONY", "RT_CEREMONY");
        convertRelRoleTypeMap.put("AWARDED_BY", "RT_GRANTAUTH");
        convertRelRoleTypeMap.put("FAMILY_BRANCH", "RT_RELATED");
        convertRelRoleTypeMap.put("OWNER", "RT_OWNER");
        convertRelRoleTypeMap.put("FOUNDER", "RT_FOUNDER");
        convertRelRoleTypeMap.put("EMPLOYEE", "RT_RELATED");
        convertRelRoleTypeMap.put("EMPLOYER", "RT_EMPLOYER");
        convertRelRoleTypeMap.put("FRIEND", "RT_RELATED");
    }

    private void createConvertRelEventMap() {
        convertRelEventMap = new HashMap<>();
        convertRelEventMap.put("ACTIVE_FROM", "PT_CRE");
        convertRelEventMap.put("CREATION", "PT_CRE");
        convertRelEventMap.put("MEMBERSHIP", "PT_EVENT");
        convertRelEventMap.put("APPRECIATION", "PT_EVENT");
        convertRelEventMap.put("STUDY", "PT_EVENT");
        convertRelEventMap.put("MARRIAGE", "PT_EVENT");
        convertRelEventMap.put("EMPLOYMENT", "PT_EVENT");
        convertRelEventMap.put("ACTIVE_TO", "PT_EXT");
        convertRelEventMap.put("EXTINCTION", "PT_EXT");
        convertRelEventMap.put("ACTIVITIES", "PT_REL");
        convertRelEventMap.put("SCOPE", "PT_REL");
        convertRelEventMap.put("PLACE", "PT_REL");
        convertRelEventMap.put("SUPERIORITY", "PT_REL");
        convertRelEventMap.put("POSITION", "PT_REL");
        convertRelEventMap.put("OTHER", "PT_REL");
        convertRelEventMap.put("SUBORDINATION", "PT_REL");
        convertRelEventMap.put("DESIGNATION", "PT_REL");
        convertRelEventMap.put("FAMILY_RELATIONS", "PT_REL");
        convertRelEventMap.put("RESIDENCY", "PT_REL");
        convertRelEventMap.put("EVENT", "PT_REL");
        convertRelEventMap.put("OWNERSHIP", "PT_REL");
        convertRelEventMap.put("SAME_IDENTITY", "PT_REL");
    }

    private void createPersonConvertPartyTypeItemSpec() {
        convertPersonPartyTypeItemSpec = new HashMap<>();
        convertPersonPartyTypeItemSpec.put("PERSON", "CRC_BIRTH");
        convertPersonPartyTypeItemSpec.put("PARTY_GROUP", "CRC_RISE");
        convertPersonPartyTypeItemSpec.put("EVENT", "CRC_ORIGIN");
        convertPersonPartyTypeItemSpec.put("DYNASTY", "CRC_FIRSTMBIRTH");
    }

    private void createExtinctionConvertPartyTypeItemSpec() {
        convertExtinctionPartyTypeItemSpec = new HashMap<>();
        convertExtinctionPartyTypeItemSpec.put("PERSON", "EXC_DEATH");
        convertExtinctionPartyTypeItemSpec.put("PARTY_GROUP", "EXC_EXTINCTION");
        convertExtinctionPartyTypeItemSpec.put("EVENT", "EXC_END");
        convertExtinctionPartyTypeItemSpec.put("DYNASTY", "EXC_LASTMDEATH");
    }

    private static class ApAccessPoint {
        private static String COL_ACCESS_POINT_ID = "access_point_id";
        private static String COL_UUID = "access_point_id";
        private static String COL_RULE_SYSTEM_ID = "rule_system_id";
        private static String COL_ERROR_DESCRIPTION = "error_description";
        private static String COL_STATE = "state";
        private static String COL_VERSION = "version";
        private static String COL_LAST_UPDATE = "last_update";
        private static String COL_PREFERRED_NAME_ITEM_ID = "preferred_name_item_id";

        private Integer accessPointId;
        private Integer uuid;
        private Integer ruleSystemId;
        private String errorDescription;
        private String state;
        private Integer version;
        private Timestamp lastUpdate;
        private Integer preferredNameItemId;

        public ApAccessPoint(Integer accessPointId, Integer uuid, Integer ruleSystemId, String errorDescription, String state, Integer version, Timestamp lastUpdate, Integer preferredNameItemId) {
            this.accessPointId = accessPointId;
            this.uuid = uuid;
            this.ruleSystemId = ruleSystemId;
            this.errorDescription = errorDescription;
            this.state = state;
            this.version = version;
            this.lastUpdate = lastUpdate;
            this.preferredNameItemId = preferredNameItemId;
        }

        public Integer getAccessPointId() {
            return accessPointId;
        }

        public Integer getUuid() {
            return uuid;
        }

        public Integer getRuleSystemId() {
            return ruleSystemId;
        }

        public String getErrorDescription() {
            return errorDescription;
        }

        public String getState() {
            return state;
        }

        public Integer getVersion() {
            return version;
        }

        public Timestamp getLastUpdate() {
            return lastUpdate;
        }

        public Integer getPreferredNameItemId() {
            return preferredNameItemId;
        }
    }

    private static class RulItemType {
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
        private static String COL_VIEW_DEFINITION = "view_definition";
        private static String COL_STRUCTURED_TYPE_ID = "structured_type_id";
        private static String COL_FRAGMENT_TYPE_ID = "fragment_type_id";

        private Integer itemTypeId;
        private Integer dataTypeId;
        private String code;
        private String name;
        private String shortcut;
        private String description;
        private boolean isValueUnique;
        private boolean canBeOrdered;
        private boolean useSpecification;
        private Integer viewOrder;
        private Integer packageId;
        private String viewDefinition;
        private Integer structuredTypeId;
        private Integer fragmentTypeId;

        public RulItemType() {

        }

        public Integer getItemTypeId() {
            return itemTypeId;
        }

        public void setItemTypeId(Integer itemTypeId) {
            this.itemTypeId = itemTypeId;
        }

        public Integer getDataTypeId() {
            return dataTypeId;
        }

        public void setDataTypeId(Integer dataTypeId) {
            this.dataTypeId = dataTypeId;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getShortcut() {
            return shortcut;
        }

        public void setShortcut(String shortcut) {
            this.shortcut = shortcut;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public boolean isValueUnique() {
            return isValueUnique;
        }

        public void setValueUnique(boolean valueUnique) {
            isValueUnique = valueUnique;
        }

        public boolean isCanBeOrdered() {
            return canBeOrdered;
        }

        public void setCanBeOrdered(boolean canBeOrdered) {
            this.canBeOrdered = canBeOrdered;
        }

        public boolean isUseSpecification() {
            return useSpecification;
        }

        public void setUseSpecification(boolean useSpecification) {
            this.useSpecification = useSpecification;
        }

        public Integer getViewOrder() {
            return viewOrder;
        }

        public void setViewOrder(Integer viewOrder) {
            this.viewOrder = viewOrder;
        }

        public Integer getPackageId() {
            return packageId;
        }

        public void setPackageId(Integer packageId) {
            this.packageId = packageId;
        }

        public String getViewDefinition() {
            return viewDefinition;
        }

        public void setViewDefinition(String viewDefinition) {
            this.viewDefinition = viewDefinition;
        }

        public Integer getStructuredTypeId() {
            return structuredTypeId;
        }

        public void setStructuredTypeId(Integer structuredTypeId) {
            this.structuredTypeId = structuredTypeId;
        }

        public Integer getFragmentTypeId() {
            return fragmentTypeId;
        }

        public void setFragmentTypeId(Integer fragmentTypeId) {
            this.fragmentTypeId = fragmentTypeId;
        }
    }

    private static class RulItemSpec {
        private static String COL_ITEM_SPEC_ID = "item_spec_id";
        private static String COL_CODE = "code";
        private static String COL_NAME = "name";
        private static String COL_SHORTCUT = "shortcut";
        private static String COL_DESCRIPTION = "description";
        private static String COL_PACKAGE_ID = "package_id";
        private static String COL_CATEGORY = "category";

        private Integer itemSpecId;
        private String code;
        private String name;
        private String shortcut;
        private String description;
        private Integer packageId;
        private String category;

        public RulItemSpec() {

        }

        public RulItemSpec(Integer itemSpecId, String code, String name, String shortcut, String description, Integer packageId, String category) {
            this.itemSpecId = itemSpecId;
            this.code = code;
            this.name = name;
            this.shortcut = shortcut;
            this.description = description;
            this.packageId = packageId;
            this.category = category;
        }

        public Integer getItemSpecId() {
            return itemSpecId;
        }

        public void setItemSpecId(Integer itemSpecId) {
            this.itemSpecId = itemSpecId;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getShortcut() {
            return shortcut;
        }

        public void setShortcut(String shortcut) {
            this.shortcut = shortcut;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Integer getPackageId() {
            return packageId;
        }

        public void setPackageId(Integer packageId) {
            this.packageId = packageId;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }
    }

    private static class RulPackage {
        private Integer packageId;
        private String name;
        private String code;
        private String description;
        private Integer version;

        private static String COL_PACKAGE_ID = "package_id";
        private static String COL_CODE = "code";
        private static String COL_VERSION = "version";

        public RulPackage(Integer packageId, String code, Integer version) {
            this.packageId = packageId;
            this.code = code;
            this.version = version;
        }

        public Integer getPackageId() {
            return packageId;
        }

        public String getCode() {
            return code;
        }

        public Integer getVersion() {
            return version;
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

    private static class ApChange {
        private static String COL_CHANGE_ID = "change_id";
        private static String COL_CHANGE_DATE = "change_date";
        private static String COL_USER_ID = "user_id";
        private static String COL_TYPE = "type";

        private Integer changeId;
        private Timestamp changeDate;
        private Integer userId;
        private String type;

        public ApChange(Integer changeId, Timestamp changeDate, Integer userId, String type) {
            this.changeId = changeId;
            this.changeDate = changeDate;
            this.userId = userId;
            this.type = type;
        }

        public Integer getChangeId() {
            return changeId;
        }

        public Timestamp getChangeDate() {
            return changeDate;
        }

        public Integer getUserId() {
            return userId;
        }

        public String getType() {
            return type;
        }
    }

    private static class RulDataType {
        private static String COL_DATA_TYPE_ID = "data_type_id";
        private static String COL_CODE = "code";
        private static String COL_NAME = "name";
        private static String COL_DESCRIPTION = "description";
        private static String COL_REGEXP_USE = "regexp_use";
        private static String COL_TEXT_LENGTH_LIMIT_USE = "text_length_limit_use";
        private static String COL_STORAGE_TABLE = "storage_table";
        private static String COL_TEXT_LENGTH_LIMIT = "text_length_limit";

        private Integer dataTypeId;
        private String code;
        private String name;
        private String description;
        private boolean regexpUse;
        private boolean textLengthLimitUse;
        private String storageTable;
        private Integer textLengthLimit;

        public RulDataType() {
        }

        public static String getColDataTypeId() {
            return COL_DATA_TYPE_ID;
        }

        public static void setColDataTypeId(String colDataTypeId) {
            COL_DATA_TYPE_ID = colDataTypeId;
        }

        public static String getColCode() {
            return COL_CODE;
        }

        public static void setColCode(String colCode) {
            COL_CODE = colCode;
        }

        public static String getColName() {
            return COL_NAME;
        }

        public static void setColName(String colName) {
            COL_NAME = colName;
        }

        public static String getColDescription() {
            return COL_DESCRIPTION;
        }

        public static void setColDescription(String colDescription) {
            COL_DESCRIPTION = colDescription;
        }

        public static String getColRegexpUse() {
            return COL_REGEXP_USE;
        }

        public static void setColRegexpUse(String colRegexpUse) {
            COL_REGEXP_USE = colRegexpUse;
        }

        public static String getColTextLengthLimitUse() {
            return COL_TEXT_LENGTH_LIMIT_USE;
        }

        public static void setColTextLengthLimitUse(String colTextLengthLimitUse) {
            COL_TEXT_LENGTH_LIMIT_USE = colTextLengthLimitUse;
        }

        public static String getColStorageTable() {
            return COL_STORAGE_TABLE;
        }

        public static void setColStorageTable(String colStorageTable) {
            COL_STORAGE_TABLE = colStorageTable;
        }

        public static String getColTextLengthLimit() {
            return COL_TEXT_LENGTH_LIMIT;
        }

        public static void setColTextLengthLimit(String colTextLengthLimit) {
            COL_TEXT_LENGTH_LIMIT = colTextLengthLimit;
        }

        public Integer getDataTypeId() {
            return dataTypeId;
        }

        public void setDataTypeId(Integer dataTypeId) {
            this.dataTypeId = dataTypeId;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public boolean isRegexpUse() {
            return regexpUse;
        }

        public void setRegexpUse(boolean regexpUse) {
            this.regexpUse = regexpUse;
        }

        public boolean isTextLengthLimitUse() {
            return textLengthLimitUse;
        }

        public void setTextLengthLimitUse(boolean textLengthLimitUse) {
            this.textLengthLimitUse = textLengthLimitUse;
        }

        public String getStorageTable() {
            return storageTable;
        }

        public void setStorageTable(String storageTable) {
            this.storageTable = storageTable;
        }

        public Integer getTextLengthLimit() {
            return textLengthLimit;
        }

        public void setTextLengthLimit(Integer textLengthLimit) {
            this.textLengthLimit = textLengthLimit;
        }
    }

    private static class RulStructuredType {
        private static String COL_STRUCTURED_TYPE_ID = "structured_type_id";
        private static String COL_PACKAGE_ID = "package_id";
        private static String COL_NAME = "name";
        private static String COL_CODE = "code";
        private static String COL_ANONYMOUS = "anonymous";

        private Integer structuredTypeId;
        private Integer packageId;
        private String name;
        private String code;
        private boolean anonymous;

        public RulStructuredType(Integer structuredTypeId, Integer packageId, String name, String code, boolean anonymous) {
            this.structuredTypeId = structuredTypeId;
            this.packageId = packageId;
            this.name = name;
            this.code = code;
            this.anonymous = anonymous;
        }

        public Integer getStructuredTypeId() {
            return structuredTypeId;
        }

        public Integer getPackageId() {
            return packageId;
        }

        public String getName() {
            return name;
        }

        public String getCode() {
            return code;
        }

        public boolean isAnonymous() {
            return anonymous;
        }
    }

    private enum ItemTypeCode {
        BRIEF_DESC("BRIEF_DESC"),
        PT_BODY("PT_BODY"),
        PT_NAME("PT_NAME"),
        PT_IDENT("PT_IDENT"),
        PT_REL("PT_REL"),
        PT_CRE("PT_CRE"),
        PT_EVENT("PT_EVENT"),
        PT_EXT("PT_EXT"),
        NM_MAIN("NM_MAIN"),
        NM_MINOR("NM_MINOR"),
        NM_DEGREE_PRE("NM_DEGREE_PRE"),
        NM_DEGREE_POST("NM_DEGREE_POST"),
        NOTE("NOTE"),
        NM_USED_FROM("NM_USED_FROM"),
        NM_USED_TO("NM_USED_TO"),
        NM_TYPE("NM_TYPE"),
        NM_SUP_GEN("NM_SUP_GEN"),
        IDN_VALID_FROM("IDN_VALID_FROM"),
        IDN_VALID_TO("IDN_VALID_TO"),
        IDN_VALUE("IDN_VALUE"),
        IDN_TYPE("IDN_TYPE"),
        HISTORY("HISTORY"),
        BIOGRAPHY("BIOGRAPHY"),
        SOURCE_INFO("SOURCE_INFO"),
        CORP_PURPOSE("CORP_PURPOSE"),
        FOUNDING_NORMS("FOUNDING_NORMS"),
        SCOPE_NORMS("SCOPE_NORMS"),
        CORP_STRUCTURE("CORP_STRUCTURE"),
        GENEALOGY("GENEALOGY"),
        REL_BEGIN("REL_BEGIN"),
        REL_END("REL_END"),
        REL_ENTITY("REL_ENTITY");

        private String code;

        ItemTypeCode(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    private enum StructuredTypeCode {
        PT_BODY("PT_BODY"),
        PT_IDENT("PT_IDENT"),
        PT_NAME("PT_NAME"),
        PT_REL("PT_REL"),
        PT_CRE("PT_CRE"),
        PT_EVENT("PT_EVENT"),
        PT_EXT("PT_EXT");

        private String code;

        StructuredTypeCode(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    private enum DataTypeCode {
        APFRAG_REF("APFRAG_REF");

        private String code;

        DataTypeCode(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    private enum ItemSpecCode {
        NT_ANTONYMUM("NT_ANTONYMUM"),
        NT_AUTHORCIPHER("NT_AUTHORCIPHER"),
        NT_RELIGIOUS("NT_RELIGIOUS"),
        NT_EQUIV("NT_EQUIV"),
        NT_HISTORICAL("NT_HISTORICAL"),
        NT_FORMER("NT_FORMER"),
        NT_HOMONYMUM("NT_HOMONYMUM"),
        NT_INVERTED("NT_INVERTED"),
        NT_ONLYKNOWN("NT_ONLYKNOWN"),
        NT_ORIGINAL("NT_ORIGINAL"),
        NT_INAPPROPRIATE("NT_INAPPROPRIATE"),
        NT_TERM("NT_TERM"),
        NT_PLURAL("NT_PLURAL"),
        NT_OTHERRULES("NT_OTHERRULES"),
        NT_HONOR("NT_HONOR"),
        NT_TAKEN("NT_TAKEN"),
        NT_TRANSLATED("NT_TRANSLATED"),
        NT_ALIAS("NT_ALIAS"),
        NT_ACCEPTED("NT_ACCEPTED"),
        NT_DIRECT("NT_DIRECT"),
        NT_PSEUDONYM("NT_PSEUDONYM"),
        NT_NATIV("NT_NATIV"),
        NT_SINGULAR("NT_SINGULAR"),
        NT_ACTUAL("NT_ACTUAL"),
        NT_SECULAR("NT_SECULAR"),
        NT_ARTIFICIAL("NT_ARTIFICIAL"),
        NT_OFFICIAL("NT_OFFICIAL"),
        NT_NARROWER("NT_NARROWER"),
        NT_SIMPLIFIED("NT_SIMPLIFIED"),
        NT_GARBLED("NT_GARBLED"),
        NT_ACRONYM("NT_ACRONYM"),
        NT_FOLK("NT_FOLK");

        private String code;

        ItemSpecCode(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

    }

    private enum StoreMethods {
        STRING, TEXT
    }

    private enum CalendarType {
        JULIAN, GREGORIAN;

        /**
         * Case sensitive search by code (used by <code>valueOf</code>).
         *
         * @param code not-null
         * @return Null when not found.
         */
        public CalendarType fromCode(String code) {
            Validate.notEmpty(code);
            try {
                return valueOf(code);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    private class CalendarConverter {
        public static final long UNIX_EPOCH_START_POS = 62135596800l;
        public static final long UNIX_EPOCH_START_NEG = 62167219200l;

        private final ICalendarConverter gcc = new GregorianCalendarConverter();
        private final ICalendarConverter jcc = new JulianCalendarConverter();


        public long toSeconds(final String calType, final LocalDateTime dateTime) {
            ICalendarConverter converter = getCalendarConverter(calType);
            LocalDateTime dateTimeNormalized = converter.fromCalendar(dateTime);
            return toSeconds(dateTimeNormalized);
        }

        private ICalendarConverter getCalendarConverter(String type) {
            ICalendarConverter converter;
            switch (type) {
                case "GREGORIAN" :
                    converter = gcc;
                    break;
                case "JULIAN":
                    converter = jcc;
                    break;
                default:
                    throw new IllegalStateException("Neimplementovaný typ kalendáře: " + type);
            }
            return converter;
        }

        /**
         * Převede datum na počet sekund od roku 1.
         * <p>
         * Datum 1. 1. 1 00:00:01 odpovídá 1
         * Datum 1. 1. 1 00:00:00 odpovídá 0
         * Datum 31. 12. -1 23:59:59 odpovídá -1
         *
         * @param dateTime normalizovaný datum
         * @return počet sekund
         */
        public long toSeconds(final LocalDateTime dateTime) {
            if (dateTime.getYear() == 0) {
                throw new IllegalArgumentException("Year 0 is not valid");
            }
            Instant instant = dateTime.toInstant(ZoneOffset.UTC);
            long sec;
            if (dateTime.getYear() < 0) {
                sec = instant.getEpochSecond() + UNIX_EPOCH_START_NEG;
            } else {
                sec = instant.getEpochSecond() + UNIX_EPOCH_START_POS;
            }
            return sec;
        }
    }

    interface ICalendarConverter {

        /**
         * Převedení juliánského datumu na normalizačního.
         *
         * @param julianDate juliánský datum
         * @return normalizačního datum
         */
        LocalDateTime fromCalendar(final LocalDateTime julianDate);

        /**
         * Převedení normalizačního datumu na juliánského.
         *
         * @param normalizedDate normalizačního datum
         * @return juliánský datum
         */
        LocalDateTime toCalendar(final LocalDateTime normalizedDate);

    }

    private class JulianCalendarConverter implements ICalendarConverter {

        @Override
        public LocalDateTime fromCalendar(final LocalDateTime julianDate) {
            long day = fromJulianDayDiff(julianDate);
            LocalDateTime gregorianDate = julianDate.plusDays(day);
            return gregorianDate;
        }

        @Override
        public LocalDateTime toCalendar(final LocalDateTime normalizedDate) {
            long day = toJulianDayDiff(normalizedDate);
            LocalDateTime julianDate = normalizedDate.minusDays(day);
            return julianDate;
        }

        /**
         * Vypočtení rozdílu dní mezi juliánským a gregoriánským kalendářem při J->G
         *
         * @param date juliánský datum
         * @return počet dní
         */
        private long fromJulianDayDiff(final LocalDateTime date) {
            LocalDateTime refDate = LocalDateTime.of(date.getYear(), 3, 12, 0, 0);
            int yearX = date.getYear() / 100 * 100;
            int y100 = (yearX - 3) / 100;
            int y400 = yearX / 400;
            long tmpDayDiff = y100 - y400 - 1L;

            LocalDateTime dateTime = refDate.minusDays(tmpDayDiff - 1);
            if (date.isBefore(dateTime)) {
                tmpDayDiff -= 1;
            }

            return tmpDayDiff;
        }

        /**
         * Vypočtení rozdílu dní mezi juliánským a gregoriánským kalendářem při G->J
         *
         * @param date gregoriánský datum
         * @return počet dní
         */
        private long toJulianDayDiff(final LocalDateTime date) {
            LocalDateTime refDate = LocalDateTime.of(date.getYear(), 3, 1, 0, 0);
            int yearX = date.getYear() / 100 * 100;
            int y100 = (yearX - 3) / 100;
            int y400 = yearX / 400;
            long tmpDayDiff = y100 - y400 - 1L;

            LocalDateTime dateTime = refDate.plusDays(tmpDayDiff - 1);
            if (date.isBefore(dateTime)) {
                tmpDayDiff -= 1;
            }
            return tmpDayDiff;
        }
    }

    private class GregorianCalendarConverter implements ICalendarConverter {

        @Override
        public LocalDateTime fromCalendar(final LocalDateTime gregorianDate) {
            return gregorianDate;
        }

        @Override
        public LocalDateTime toCalendar(final LocalDateTime normalizedDate) {
            return normalizedDate;
        }

    }
}
