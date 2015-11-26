package cz.tacr.elza.xmlimport;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.math.RandomUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cz.tacr.elza.xmlimport.v1.vo.XmlImport;
import cz.tacr.elza.xmlimport.v1.vo.arrangement.AbstractDescItem;
import cz.tacr.elza.xmlimport.v1.vo.arrangement.DescItemCoordinates;
import cz.tacr.elza.xmlimport.v1.vo.arrangement.DescItemDecimal;
import cz.tacr.elza.xmlimport.v1.vo.arrangement.DescItemFormattedText;
import cz.tacr.elza.xmlimport.v1.vo.arrangement.DescItemInteger;
import cz.tacr.elza.xmlimport.v1.vo.arrangement.DescItemPartyRef;
import cz.tacr.elza.xmlimport.v1.vo.arrangement.DescItemRecordRef;
import cz.tacr.elza.xmlimport.v1.vo.arrangement.DescItemString;
import cz.tacr.elza.xmlimport.v1.vo.arrangement.DescItemText;
import cz.tacr.elza.xmlimport.v1.vo.arrangement.DescItemUnitDate;
import cz.tacr.elza.xmlimport.v1.vo.arrangement.DescItemUnitId;
import cz.tacr.elza.xmlimport.v1.vo.arrangement.FindingAid;
import cz.tacr.elza.xmlimport.v1.vo.arrangement.Level;
import cz.tacr.elza.xmlimport.v1.vo.date.ComplexDate;
import cz.tacr.elza.xmlimport.v1.vo.party.AbstractParty;
import cz.tacr.elza.xmlimport.v1.vo.party.Dynasty;
import cz.tacr.elza.xmlimport.v1.vo.party.Event;
import cz.tacr.elza.xmlimport.v1.vo.party.PartyGroup;
import cz.tacr.elza.xmlimport.v1.vo.party.PartyGroupId;
import cz.tacr.elza.xmlimport.v1.vo.party.PartyName;
import cz.tacr.elza.xmlimport.v1.vo.party.PartyNameComplement;
import cz.tacr.elza.xmlimport.v1.vo.party.PartyTimeRange;
import cz.tacr.elza.xmlimport.v1.vo.party.Person;
import cz.tacr.elza.xmlimport.v1.vo.party.Relation;
import cz.tacr.elza.xmlimport.v1.vo.party.RoleType;
import cz.tacr.elza.xmlimport.v1.vo.record.Coordinate;
import cz.tacr.elza.xmlimport.v1.vo.record.Record;
import cz.tacr.elza.xmlimport.v1.vo.record.RecordCoordinates;
import cz.tacr.elza.xmlimport.v1.vo.record.VariantRecord;

/**
 * Generátor dat pro testy xml imprtu.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 23. 11. 2015
 */
@Service
public class XmlDataGenerator {

    private int nodes = 0;

    /**
     * Vytvoření dat pro xml import.
     *
     * @param config nastavení generátoru
     *
     * @return data pro xml import
     */
    public XmlImport createXmlImportData(final XmlDataGeneratorConfig config) {
        Assert.notNull(config);

        XmlImport xmlImport = new XmlImport();

        List<Record> records = createRecords(config);
        xmlImport.setRecords(records);
        List<AbstractParty> parties = createParties(records, config);
        xmlImport.setParties(parties);

        if (config.getTreeDepth() > 0) {
            FindingAid findingAid = createFindingAid(config, records, parties);
            xmlImport.setFindingAid(findingAid);
        }

        System.out.println(nodes);

        return xmlImport;
    }

    /**
     * Vytvoření archivní pomůcky.
     *
     * @param config nastavení generátoru
     * @param parties osoby
     * @param records rejstříková hesla
     *
     * @return archivní pomůcka
     */
    private FindingAid createFindingAid(final XmlDataGeneratorConfig config, List<Record> records, List<AbstractParty> parties) {
        FindingAid fa = new FindingAid();
        fa.setName("Import z XML");
        fa.setRootLevel(createLevelTree(records, parties, config));

        return fa;
    }

    /**
     * Vytvoří strom archivní pomůcky.
     *
     * @param records rejstříková hesla
     * @param parties osoby
     * @param config nastavení generátoru
     *
     * @return kořenový uzel
     */
    private Level createLevelTree(List<Record> records, List<AbstractParty> parties, XmlDataGeneratorConfig config) {
        nodes++;
        Level rootLevel = new Level();
        rootLevel.setPosition(1);
        rootLevel.setUuid(UUID.randomUUID().toString());

        rootLevel.setSubLevels(createChildren(config.getTreeDepth(), records, parties, config));
        rootLevel.setDescItems(createDescItems(records, parties, config));

        return rootLevel;
    }

    /**
     * Vytvoří hodnoty uzlu.
     *
     * @param records rejstříková hesla
     * @param parties osoby
     * @param config nastavení generátoru
     *
     * @return hodnoty uzlu
     */
    private List<AbstractDescItem> createDescItems(List<Record> records, List<AbstractParty> parties, XmlDataGeneratorConfig config) {
        List<AbstractDescItem> values = new ArrayList<AbstractDescItem>(config.getDescItemsCount());

        for (int i = 0; i < config.getDescItemsCount(); i++) {
            int position = i % 10;

            switch (position) {
                case 0:
                    values.add(createValueCoordinates(i));
                    break;
                case 1:
                    values.add(createValueDecimal(i, config.getDescItemsCount()));
                    break;
                case 2:
                    values.add(createValueFormattedText(i));
                    break;
                case 3:
                    values.add(createValueInteger(i));
                    break;
                case 4:
                    values.add(createValuePartyRef(i, parties));
                    break;
                case 5:
                    values.add(createValueRecordRef(i, records));
                    break;
                case 6:
                    values.add(createValueString(i));
                    break;
                case 7:
                    values.add(createValueText(i));
                    break;
                case 8:
                    values.add(createValueUnitDate(i));
                    break;
                case 9:
                    values.add(createValueUnitId(i));
                    break;
            }
        }

        return values;
    }

    private DescItemCoordinates createValueCoordinates(int position) {
        DescItemCoordinates value = new DescItemCoordinates();
        fillCommonValueFields(value ,position);
        value.setValue("coordinates " + position);

        return value;
    }

    private DescItemDecimal createValueDecimal(int position, int descItemCount) {
        DescItemDecimal value = new DescItemDecimal();
        fillCommonValueFields(value ,position);
        value.setValue(new BigDecimal(descItemCount * position));

        return value;
    }

    private DescItemFormattedText createValueFormattedText(int position) {
        DescItemFormattedText value = new DescItemFormattedText();
        fillCommonValueFields(value ,position);
        value.setValue("formatted text " + position);

        return value;
    }

    private DescItemInteger createValueInteger(int position) {
        DescItemInteger value = new DescItemInteger();
        fillCommonValueFields(value ,position);
        value.setValue(position);

        return value;
    }

    private DescItemPartyRef createValuePartyRef(int position, List<AbstractParty> parties) {
        DescItemPartyRef value = new DescItemPartyRef();
        fillCommonValueFields(value ,position);

        value.setParty(parties.get(RandomUtils.nextInt(parties.size())));

        return value;
    }

    private DescItemRecordRef createValueRecordRef(int position, List<Record> records) {
        DescItemRecordRef value = new DescItemRecordRef();
        fillCommonValueFields(value ,position);
        value.setRecord(records.get(RandomUtils.nextInt(records.size())));

        return value;
    }

    private DescItemString createValueString(int position) {
        DescItemString value = new DescItemString();
        fillCommonValueFields(value ,position);
        value.setValue("string " + position);

        return value;
    }

    private DescItemText createValueText(int position) {
        DescItemText value = new DescItemText();
        fillCommonValueFields(value ,position);
        value.setValue("text " + position);

        return value;
    }

    private DescItemUnitDate createValueUnitDate(int position) {
        DescItemUnitDate value = new DescItemUnitDate();
        fillCommonValueFields(value ,position);
        value.setCalendarTypeCode("calendarTypeCode " + position);
        value.setValueFrom("valueFrom " + position);
        value.setValueFromEstimated(RandomUtils.nextBoolean());
        value.setValueTo("valueTo " + position);
        value.setValueToEstimated(RandomUtils.nextBoolean());

        return value;
    }

    private DescItemUnitId createValueUnitId(int position) {
        DescItemUnitId value = new DescItemUnitId();
        fillCommonValueFields(value ,position);
        value.setValue("unitId " + position);

        return value;
    }

    private void fillCommonValueFields(AbstractDescItem value, int position) {
        value.setDescItemSpecCode("descItemSpecCode");
        value.setDescItemTypeCode("descItemTypeCode");
        value.setPosition(position);
    }

    private List<Level> createChildren(int depth, List<Record> records, List<AbstractParty> parties, XmlDataGeneratorConfig config) {
        if (depth < 1) {
            return null;
        }
        List<Level> children = new ArrayList<Level>(config.getChildrenCount());
        for (int i = 0; i < config.getChildrenCount(); i++) {
            nodes++;
            Level child = new Level();
            child.setSubLevels(createChildren(depth - 1, records, parties, config));
            child.setPosition(i);
            child.setDescItems(createDescItems(records, parties, config));
            child.setUuid(UUID.randomUUID().toString());
            if (RandomUtils.nextBoolean()) {
                List<Record> levelRecords = new ArrayList<Record>(1);
                levelRecords.add(records.get(RandomUtils.nextInt(records.size())));
                child.setRecords(levelRecords);
            }

            children.add(child);
        }
        return children;
    }

    /**
     * Vytvoří seznam rejstříkových hesel.
     *
     * @param config nastavení generátoru
     *
     * @return seznam rejstříkových hesel
     */
    private List<Record> createRecords(final XmlDataGeneratorConfig config) {
        List<Record> records = new ArrayList<Record>(config.getRecordCount());

        for (int i = 0; i < config.getRecordCount(); i++) {
            Record record = createRecord(config, i, null);

            records.add(record);
        }

        return records;
    }

    /**
     * Vytvoří rejstříkové heslo.
     *
     * @param config nastavení generátoru
     * @param index číslo vytvářeného rejstříkového hesla
     * @param parentId id rodiče, null pro kořenový prvek
     *
     * @return rejstříkové heslo
     */
    private Record createRecord(XmlDataGeneratorConfig config, int index, String parentId) {
        Record record = new Record();
        record.setCharacteristics("characteristics " + index);
        record.setNote("comment " + index);
        record.setExternalId("externalId " + index);
        record.setExternalSourceCode("externalSourceCode " + index);
        record.setLocal(RandomUtils.nextBoolean());
        record.setPreferredName("record " + index);

        String idPrefix;
        if (parentId == null) {
            idPrefix = "recordId";
        } else {
            idPrefix = parentId;
        }
        if (config.isValid()) {
            record.setRecordId(idPrefix + "-" + index);
        } else {
            record.setRecordId(idPrefix + " " + index);
        }
        record.setRegisterTypeCode("registerTypeCode " + index);
        record.setVariantNames(createVariantRecords(config.getVariantRecordCount()));
        record.setRecordCoordinates(createRecordCoordinates());

        if (RandomUtils.nextBoolean()) {
            List<Record> subRecords = new ArrayList<Record>(1);
            record.setRecords(subRecords);

            subRecords.add(createRecord(config, 1, record.getRecordId()));
        }

        return record;
    }

    private RecordCoordinates createRecordCoordinates() {
        RecordCoordinates recordCoordinates = new RecordCoordinates();
        recordCoordinates.setAreaType("line");
        recordCoordinates.setNote("note");
        recordCoordinates.setSystem("WGS84");
        recordCoordinates.setCoordinates(createCoordinates(2));

        return recordCoordinates;
    }

    private List<Coordinate> createCoordinates(int count) {
        List<Coordinate> coordinates = new ArrayList<Coordinate>(2);

        for (int i = 0; i < count; i++) {
            Coordinate c = new Coordinate();
            c.setCoordinate("point " + i);
            c.setPosition(i);

            coordinates.add(c);
        }

        return coordinates;
    }

    /**
     * Vytvoří seznam variantních rejstříkových hesel.
     * @param variantRecordCount počet variantních hesel
     *
     * @return seznam variantních rejstříkových hesel
     */
    private List<VariantRecord> createVariantRecords(int variantRecordCount) {
        List<VariantRecord> variantRecords = new ArrayList<VariantRecord>(variantRecordCount);
        for (int i = 0; i < variantRecordCount; i++) {
            VariantRecord variantRecord = createVariantRecord(i);

            variantRecords.add(variantRecord);
        }
        return variantRecords;
    }

    /**
     * Vytvoří variantní rejstříkové heslo.
     *
     * @param index číslo vytvářeného variantního rejstříkového hesla
     *
     * @return variantní rejstříkové heslo
     */
    private VariantRecord createVariantRecord(int index) {
        VariantRecord variantRecord = new VariantRecord();
        variantRecord.setVariantName("variantRecord " + index);
        return variantRecord;
    }

    /**
     * Vytvoří seznam osob.
     *
     * @param records seznam rejstříků pro navázání na osoby
     * @param config nastavení generátoru
     *
     * @return seznam osob
     */
    private List<AbstractParty> createParties(List<Record> records, XmlDataGeneratorConfig config) {
        List<AbstractParty> parties = new ArrayList<AbstractParty>(config.getPartyCount());
        for (int i = 0; i < config.getPartyCount(); i++) {
            int position = i % 4;

            switch (position) {
                case 0:
                    parties.add(createPerson(records, i, config, parties));
                    break;
                case 1:
                    parties.add(createDynasty(records, i, config, parties));
                    break;
                case 2:
                    parties.add(createEvent(records, i, config, parties));
                    break;
                case 3:
                    parties.add(createPartyGroup(records, i, config, parties));
                    break;
            }
        }

        return parties;
    }

    private PartyGroup createPartyGroup(List<Record> records, int index, XmlDataGeneratorConfig config, List<AbstractParty> parties) {
        PartyGroup partyGroup = new PartyGroup();

        fillParentFields(records, index, partyGroup, config, parties);
        partyGroup.setPartyGroupIds(createPartyGroupIds(config));

        partyGroup.setScope("scope");
        partyGroup.setOrganization("organization");
        partyGroup.setScopeNorm("scopeNorm");
        partyGroup.setFoundingNorm("foundingNorm");

        return partyGroup;
    }

    private List<PartyGroupId> createPartyGroupIds(XmlDataGeneratorConfig config) {
        List<PartyGroupId> partyGroupIds = new ArrayList<PartyGroupId>(config.getPartyGroupIdCount());
        for (int i = 0; i < config.getPartyGroupIdCount(); i++) {
            partyGroupIds.add(createPartyGroupId(i));
        }

        return partyGroupIds;
    }

    private PartyGroupId createPartyGroupId(int i) {
        PartyGroupId partyGroupId = new PartyGroupId();

        partyGroupId.setId("id " + i);
        partyGroupId.setNote("note");
        partyGroupId.setSource("source");
        partyGroupId.setValidFrom(createComplexDate());
        partyGroupId.setValidTo(createComplexDate());

        return partyGroupId;
    }

    private Event createEvent(List<Record> records, int index, XmlDataGeneratorConfig config, List<AbstractParty> parties) {
        Event temporaryEvent = new Event();

        fillParentFields(records, index, temporaryEvent, config, parties);

        return temporaryEvent;
    }

    private Dynasty createDynasty(List<Record> records, int index, XmlDataGeneratorConfig config, List<AbstractParty> parties) {
        Dynasty dynasty = new Dynasty();

        fillParentFields(records, index, dynasty, config, parties);
        dynasty.setGenealogy("rod");

        return dynasty;
    }

    /**
     * Vytvoří osobu.
     *
     * @param records seznam rejstříků pro navázání na osoby
     * @param index číslo vytvářené osoby
     * @param parties již vytvořené osoby
     *
     * @return osoba
     */
    private Person createPerson(List<Record> records, int index, XmlDataGeneratorConfig config, List<AbstractParty> parties) {
        Person party = new Person();
        fillParentFields(records, index, party, config, parties);

        return party;
    }

    private void fillParentFields(List<Record> records, int index, AbstractParty party, XmlDataGeneratorConfig config,
            List<AbstractParty> parties) {
        party.setPartyId("partyId-" + index);
        party.setPartyTypeCode("partyTypeCode " + index);

        party.setRecord(records.get(RandomUtils.nextInt(records.size())));
        party.setPreferredName(createPartyName(index, config));

        if (RandomUtils.nextBoolean()) {
            int namesCount = RandomUtils.nextInt(5) + 1;
            List<PartyName> otherNames = new ArrayList<>(namesCount);
            for (int i = 0; i < namesCount; i++) {
                PartyName partyName = createPartyName(index, config);
                otherNames.add(partyName);
            }

            party.setVariantNames(otherNames);
        }

        if (RandomUtils.nextBoolean() && parties.size() > 0) {
            party.setCreators(createCreators(parties));
        }
        party.setEvents(createEvents(records, config));
        party.setHistory("history " + index);
        party.setSourceInformations("sourceInformations " + index);
        party.setPartyTimeRanges(createPartyTimeRanges(config));
    }

    private List<PartyTimeRange> createPartyTimeRanges(XmlDataGeneratorConfig config) {
        List<PartyTimeRange> partyTimeRanges = new ArrayList<PartyTimeRange>(config.getPartyTimeRangeCount());
        for (int i = 0; i < config.getPartyTimeRangeCount(); i++) {
            partyTimeRanges.add(createPartyTimeRange());
        }

        return partyTimeRanges;
    }

    private PartyTimeRange createPartyTimeRange() {
        PartyTimeRange partyTimeRange = new PartyTimeRange();

        partyTimeRange.setFromDate(createComplexDate());
        partyTimeRange.setToDate(createComplexDate());

        return partyTimeRange;
    }

    private List<Relation> createEvents(List<Record> records, XmlDataGeneratorConfig config) {
        List<Relation> events = new ArrayList<Relation>(config.getEventCount());
        for (int i = 0; i < config.getEventCount(); i++) {
            events.add(createEvent(records, i));
        }

        return events;
    }

    private Relation createEvent(List<Record> records, int index) {
        Relation event = new Relation();

        event.setClassTypeCode("classTypeCode ");
        event.setDateNote("dateNote " + index);
        event.setFromDate(createComplexDate());
        event.setNote("note " + index);
        event.setRelationTypeCode("relationTypeCode " + index);
        event.setRoleTypes(createRoleTypes(records));
        event.setToDate(createComplexDate());

        return event;
    }

    private List<RoleType> createRoleTypes(List<Record> records) {
        List<RoleType> roleTypes = new ArrayList<RoleType>(1);

        RoleType roleType = new RoleType();
        roleType.setRecord(records.get(RandomUtils.nextInt(records.size())));
        roleType.setRoleTypeCode("roleTypeCode");
        roleType.setSource("source");

        return roleTypes;
    }

    private ComplexDate createComplexDate() {
        ComplexDate complexDate = new ComplexDate();

        complexDate.setSpecificDateFrom(new Date());
        complexDate.setSpecificDateTo(new Date());

        return complexDate;
    }

    private List<AbstractParty> createCreators(List<AbstractParty> parties) {
        List<AbstractParty> creators = new ArrayList<AbstractParty>(1);

        creators.add(parties.get(RandomUtils.nextInt(parties.size())));

        return creators;
    }

    /**
     * Vytvoří jméno osoby.
     *
     * @param index pořadí jména osoby
     * @param config nastavení generátoru
     *
     * @return jméno osoby
     */
    private PartyName createPartyName(int index, XmlDataGeneratorConfig config) {
        PartyName partyName = new PartyName();

        partyName.setNote("anotation " + index);
        partyName.setDegreeAfter("degreeAfter " + index);
        partyName.setDegreeBefore("degreeBefore " + index);
        partyName.setMainPart("mainPart " + index);
        partyName.setOtherPart("otherPart " + index);
        partyName.setValidFrom(createComplexDate());
        partyName.setValidTo(createComplexDate());
        partyName.setPartyNameFormTypeCode("partyNameFormTypeCode " + index);
        partyName.setPartyNameComplements(createPartyNameComplements(config));

        return partyName;
    }

    private List<PartyNameComplement> createPartyNameComplements(XmlDataGeneratorConfig config) {
        List<PartyNameComplement> partyNameComplements = new ArrayList<PartyNameComplement>(config.getPartyNameComplementsCount());
        for (int i = 0; i < config.getEventCount(); i++) {
            partyNameComplements.add(createPartyNameComplement());
        }

        return partyNameComplements;
    }

    private PartyNameComplement createPartyNameComplement() {
        PartyNameComplement partyNameComplement = new PartyNameComplement();

        partyNameComplement.setComplement("complement");
        partyNameComplement.setPartyNameComplementTypeName("partyNameComplementTypeName");

        return partyNameComplement;
    }
}
