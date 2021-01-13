package cz.tacr.elza.dbchangelog;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.DatabaseException;

public class DbChangeset20200807100100 extends BaseTaskChange {

    private final Map<String, String> archyMap = new HashMap<String, String>() {
        {
            put("000000010", "b50776b9-8d76-4688-ad15-9358b59d057b");
            put("100000010", "9f783015-b9af-42fc-bff4-11ff57cdb072");
            put("100000020", "c4b13fa0-89a2-44a2-954f-e281934c3dcf");
            put("211000010", "e1ad171f-aee0-4e6e-b692-8bfb09f44eaf");
            put("212000010", "921ee434-ef97-4b65-86d9-22313513d43b");
            put("213000010", "c0471c34-4dbd-4fa0-8929-ccfe8e8cb325");
            put("214000010", "252edbbc-f893-4083-8702-c0e1432dbe89");
            put("215000010", "3d392a72-6ad4-4fa3-a237-e5a4d7a7f91d");
            put("216000010", "32eff1a7-4a2f-4462-a6de-29c9fa39ead5");
            put("217000010", "1a381845-785f-4cb2-8290-fa1e0c54e1bd");
            put("221201010", "9668ae9f-e3fa-402e-9817-d1fcd730110e");
            put("221202010", "0d158984-9b53-4660-b08b-f23fdf39e764");
            put("221203010", "d865cc77-fbb3-4224-9eee-1a8f2ebcea29");
            put("221204010", "64364a53-fa2c-4e28-bef3-8693b00f6776");
            put("221205010", "32b92f0c-046f-4d7d-b04e-6cee2862ee99");
            put("221206010", "8f8c422f-deda-4445-b640-931c2d6b116a");
            put("221207010", "08b9e050-4821-41eb-98f8-160022099243");
            put("221208010", "21b0f0da-9113-4146-bc85-18bbd5df17c0");
            put("221209010", "2dab13a8-437d-49ed-9e04-37433790a8d3");
            put("221210010", "b1b0d00b-233e-497b-b7ea-66343a1600d6");
            put("221211010", "2d2ef1c1-f137-49a0-b9cf-a800b58c530d");
            put("221212010", "8b8ae293-2f56-426f-89d5-0c98ac2ba1ea");
            put("222101010", "9c9365db-435a-4b4b-a2d2-73862fd686d5");
            put("222102010", "e52ca1a4-b155-40c5-a2d9-130173832f96");
            put("222103010", "290efa39-f5d3-4bbe-a4e2-d7e246a99af5");
            put("222104010", "90aae591-dcb9-4e53-9c28-2971121009ec");
            put("222105010", "d037558f-8715-4ac5-9908-e40efb5d61ba");
            put("222106010", "18ed12b1-e272-4721-a86c-c50a6eb60949");
            put("222107010", "68a37692-15c5-4c11-afb2-b9b91d850258");
            put("223101010", "5c37b075-f144-4cd6-8025-dc3a11ed393b");
            put("223102010", "69328285-debe-4f87-bc8f-bdd28905e322");
            put("223104010", "e4015348-52fa-4f41-92ea-df72f6a9833f");
            put("223105010", "d0abb221-fb6f-4acb-b2a0-a57799fe7d3c");
            put("223106010", "4e81142a-5b99-4c17-a27a-687f578f6016");
            put("223107010", "46d706f2-7fe1-4e63-8ab3-f8152166011a");
            put("223201010", "4bb423a4-c930-4060-91d1-13934f15fb42");
            put("223202010", "079f9a24-707c-473b-bedc-721a0ff64372");
            put("223203010", "3b0c1dd8-3fac-4d75-990f-4f2ee31e0906");
            put("224101010", "d7fafd5e-92d7-47e7-b7f4-348a36827ca3");
            put("224102010", "8fa7fe8e-6793-4c09-a8d0-f44ddfb5115d");
            put("224103010", "0f60c827-b614-40c8-b2e7-dec00e77d291");
            put("224104010", "5d3eda10-4c1d-4333-b842-5fa4b0766ceb");
            put("224105010", "3e537690-41ff-41a3-aac9-5fd0e7c955b8");
            put("224106010", "9061b7f2-4bdd-4cc2-9677-6133539be02e");
            put("224201010", "dafd0a3e-e1b4-438f-9775-e4cb742c02dd");
            put("224202010", "28c86428-cffe-40de-bab9-c4148c964000");
            put("224203010", "77bd7659-bc7f-482f-82e5-04c933948116");
            put("224204010", "1a1f10dc-d3e7-493a-95da-3a0eda108a61");
            put("225101010", "8a3f1154-74af-4144-a3c1-cecba7329bd8");
            put("225102010", "f9c1aa26-830f-4ad1-ae92-575e932c7c1c");
            put("225103010", "2e8b81ae-830b-4d2e-9f22-50fccf9caeab");
            put("225104010", "0c1c929f-9479-4af9-9834-675751a9f37a");
            put("225105010", "d84a5237-4ac0-4cc1-85b1-8188e1ae705a");
            put("225201010", "48c5571b-dbf5-48e5-b615-5cf7cef71489");
            put("225202010", "0db5911a-7718-4465-9807-b4ac537dfe20");
            put("225203010", "19c88d80-2946-4093-901a-1949f53746af");
            put("225204010", "63ec306f-df57-4db4-b0c7-da43b0323f26");
            put("226101010", "7497d383-dec5-44ce-8362-bf3932ebcde5");
            put("226102010", "9b468443-026c-44c6-8c6f-00f0ce1e59cc");
            put("226103010", "8def92e9-7788-4d6c-a6e2-aebe0a0155c3");
            put("226104010", "134eba54-096d-4279-9469-a987b7f44eec");
            put("226105010", "e0d15f0c-f383-4b2f-b9f4-9daf288c6cda");
            put("226201010", "96c7c9a4-0a0c-4a9c-af97-6e09a51e0ceb");
            put("226203010", "a3c6f569-4a57-4871-8697-9767831451c0");
            put("226204010", "2ca2e335-387c-4e6a-a9b1-5e08ff48f4d5");
            put("226205010", "95b1d4ab-4419-4a86-a28f-25fe52572c1f");
            put("226206010", "afea7996-b878-428d-839f-4e7b4a1c4774");
            put("226207010", "fb6e5fa7-8baa-4922-8a25-b5c45ca2eeca");
            put("226301010", "68243149-e5d2-440d-9c6e-2f93baa015c2");
            put("226302010", "0dd05a49-fdcf-4c63-8af1-8889b7a969c8");
            put("226303010", "d97deea9-49e7-45f3-917d-ad7a14e721e6");
            put("226304010", "2ecebed8-aae6-4981-a423-b15f4dcffe17");
            put("227101010", "47d8bfed-8ebe-4e92-a351-82c04a9cd359");
            put("227102010", "d4b0e75f-2d69-4e70-b3c7-0038a992e91e");
            put("227103010", "c52330bd-3faa-4fed-9dcc-70185ccbd805");
            put("227104010", "65d82539-3c69-4b9b-8be1-f2482a3bbc13");
            put("227105010", "0d549f67-6861-4fac-b2b6-3b4e68016997");
            put("227201010", "1b3efb79-bdad-4a1a-a6c6-16b56e949dcc");
            put("227202010", "d2218aa2-c114-4bf7-af36-61736df2b689");
            put("227203010", "c1b00bfb-65db-4c39-8f4a-e0f93c0c5d79");
            put("227204010", "ba4772f7-e4f7-4430-8a9c-9a13fe377ed5");
            put("227205010", "5fa204fe-490c-430e-b05a-202c283bdfc7");
            put("321100010", "f6a50fd5-d939-4445-8317-eb4ff3b46f9c");
            put("323103010", "e211d416-3d04-4fae-8225-0eb0e722e24b");
            put("324107010", "2bb1d13a-3a61-4d42-b395-bf10048ff97b");
            put("326202010", "c99dd0e7-e473-43da-8228-fc8607606f1b");
            put("327206010", "38762000-44c2-4f19-98ef-7a2335aa68d5");
            put("511207010", "84df4233-2d2b-4f66-9c95-47a7d55f1883");
            put("513103010", "01629fda-572a-4a18-8001-301c318fa7bc");
            put("513203010", "1f3979e5-dccd-426f-87ca-ffedea5b6816");
            put("517206010", "295416a7-06bc-47dc-a19e-487c5dd469a9");
            put("517206020", "3410a550-21eb-40d5-90c4-cc3f8b0b3091");
            put("517206040", "644a40b9-cddd-459a-8dba-05d3cb9c722d");
            put("521100010", "ff5b6542-772a-4f91-84c1-2c39bd660151");
            put("521100020", "f9d334fd-fd98-4f9e-b88e-42740d1c6773");
            put("521100030", "ca2fcd2a-f4d2-460d-9c06-e8000bc7e070");
            put("526101010", "574efc6e-b581-4c8e-833c-f04bf6e80cdb");
            put("536000010", "6ea5bf19-4533-4fed-9c0f-3787bd302eb9");
            put("610000010", "451dd601-78b9-4ec3-9f2d-e9cb939a8a75");
            put("610000020", "8416538c-90df-4758-9b04-53d7f99e4d99");
            put("610000030", "16208eaa-e391-453f-8d47-f20ea9a2e394");
            put("610000040", "7048bede-352d-4e2f-9e2f-6191c2b7e5bd");
            put("610000050", "62eec208-29b5-44f0-af64-a705213efb3c");
            put("610000060", "9923ea86-57de-4828-b4f3-fe442a0c2b68");
            put("610000070", "ff4d292a-4574-4dc7-8ae5-387832e4f8db");
            put("610000080", "f43a45aa-23e0-4871-bbe1-c2df7870154c");
            put("610000090", "9f6428bd-7931-4543-ae44-05958d51b130");
            put("610000100", "a2e3130d-7ad4-4a9b-a84a-4bcbf1e6b92a");
            put("610000110", "9e4ef863-969c-4dfc-ab82-4a2a980a8e8d");
            put("610000130", "de5924a1-e2fb-47c4-b1b7-e29f7715c794");
            put("610000140", "c24d82c2-ed61-4e55-9fa8-57623b46f8b6");
            put("620000010", "fe5fccc4-9fb1-43ec-9a89-4ef017c7b8a9");
            put("620000020", "dceb7449-711d-4911-8248-626dbd6cc658");
            put("620000030", "e8b6df17-ec75-4767-b92c-f29f38d4a9c5");
            put("620000040", "21eff654-9e88-477e-8e25-d5ec1bbee7f6");
            put("620000050", "48d63566-44ce-425e-870c-0255573f3788");
            put("620000060", "fd747aeb-21ac-4141-b3e7-67765cb200da");
            put("620000070", "f163517e-2a83-4cc3-b84f-3a69a15d2d39");
            put("620000080", "9ad6c9ce-b07b-4e37-8662-2bf091f97375");
            put("620000090", "a0a9af28-3460-44a2-a916-3aa1385d2bf3");
            put("620000100", "7a7b7d01-b450-4816-9940-bfc1333c4509");
            put("620000110", "553be9d2-ccc1-40fb-a8e6-176cc5b3a4cc");
            put("630000010", "0fc3fa38-b5f0-44f6-a0ce-462a2a614c2d");
            put("630000020", "ee1a3cbb-f6b0-412e-860f-65e3930afa6f");
            put("630000040", "e9ceb79e-7679-4741-89f6-8834fa231e46");
            put("630000050", "886df522-5987-4191-a528-445ce8fb199b");
            put("630000060", "1d4013f3-a6af-4425-b9fe-b50483e5a785");
            put("630000070", "35fd090b-3186-4f11-88a6-951255c8557d");
            put("640000010", "88617219-51c2-4941-9426-39858e5a0769");
            put("700000020", "211e6b1d-99b7-4664-a1b8-1ae658d6fc78");
            put("700000030", "0756fe68-3ff1-49ec-a7a1-c6a7eac7d662");
            put("700000040", "60071f26-a5ea-49bd-bd1d-7ddf2d43d68c");
            put("700000050", "a14e5d7b-83fe-46e8-a81b-e3fd513073cc");
            put("700000060", "ee700a25-6195-4a80-b2a5-c43b691b36ca");
            put("700000070", "1ec5ea44-cf34-4652-a70c-9d72ec2a9aa5");
        }
    };

    private final String GET_DATA_LIST = "SELECT pi.access_point_id, pi.internal_code, ap.uuid "
            + "FROM par_institution pi "
            + "JOIN ap_access_point ap ON pi.access_point_id = ap.access_point_id";

    private final String UPDATE_POSTGRE_SQL = "UPDATE ap_access_point SET uuid = ?"
            + "WHERE access_point_id = ? "
            + "AND NOT EXISTS( SELECT 1 FROM ap_access_point WHERE uuid = ? )";

    private int numChanged = 0;

    @Override
    public String getConfirmationMessage() {
        return "Changed UUIDs for table ap_access_point, count: " + numChanged;
    }

    @Override
    public void execute(Database database) throws CustomChangeException {
        JdbcConnection dc = (JdbcConnection) database.getConnection();

        try (ResultSet resultSet = dc.createStatement().executeQuery(GET_DATA_LIST)) {
            if (resultSet.next()) {
                try (PreparedStatement ps = dc.prepareStatement(UPDATE_POSTGRE_SQL)) {
                    do {
                        String internalCode = resultSet.getString("internal_code");
                        String currUuid = resultSet.getString("uuid");
                        String newUuid = archyMap.get(internalCode);
                        if (newUuid != null && !newUuid.equals(currUuid)) {
                            Integer accessPointId = resultSet.getInt("access_point_id");

                            ps.setString(1, newUuid);
                            ps.setInt(2, accessPointId);
                            ps.setString(3, newUuid);
                            ps.execute();
                            int updateCount = ps.getUpdateCount();
                            Validate.isTrue(updateCount >= 0 && updateCount <= 1,
                                            "Unexpected update count: ", updateCount);
                            numChanged += updateCount;
                        }
                    } while (resultSet.next());
                }
            }
        } catch (DatabaseException | SQLException e) {
            throw new CustomChangeException("Chyba při vykonávání sql příkazu: " + e.getLocalizedMessage(), e);
        }
    }
}
