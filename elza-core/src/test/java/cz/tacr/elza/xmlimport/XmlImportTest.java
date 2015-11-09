package cz.tacr.elza.xmlimport;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.MarshalException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import cz.tacr.elza.ElzaCore;
import cz.tacr.elza.xmlimport.v1.vo.AbstractDescItem;
import cz.tacr.elza.xmlimport.v1.vo.DescItemCoordinates;
import cz.tacr.elza.xmlimport.v1.vo.DescItemDecimal;
import cz.tacr.elza.xmlimport.v1.vo.DescItemFormattedText;
import cz.tacr.elza.xmlimport.v1.vo.DescItemInteger;
import cz.tacr.elza.xmlimport.v1.vo.DescItemPartyRef;
import cz.tacr.elza.xmlimport.v1.vo.DescItemRecordRef;
import cz.tacr.elza.xmlimport.v1.vo.DescItemString;
import cz.tacr.elza.xmlimport.v1.vo.DescItemText;
import cz.tacr.elza.xmlimport.v1.vo.DescItemUnitDate;
import cz.tacr.elza.xmlimport.v1.vo.DescItemUnitId;
import cz.tacr.elza.xmlimport.v1.vo.FindingAid;
import cz.tacr.elza.xmlimport.v1.vo.FindingAidImport;
import cz.tacr.elza.xmlimport.v1.vo.Level;
import cz.tacr.elza.xmlimport.v1.vo.Party;
import cz.tacr.elza.xmlimport.v1.vo.PartyName;
import cz.tacr.elza.xmlimport.v1.vo.Record;
import cz.tacr.elza.xmlimport.v1.vo.VariantRecord;

/**
 * Testy na suzap.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 27. 10. 2015
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = ElzaCore.class)
public class XmlImportTest implements ApplicationContextAware {

    private static final int RECORD_COUNT = 10;

    private static final int VARIANT_RECORD_COUNT = 1;

    private static final int PARTY_COUNT = 10;

    private static final int CHILD_COUNT = 3;

    private static final int TREE_DEPTH_COUNT = 1;

    private static final int DESC_ITEMS_COUNT = 2;

    private ApplicationContext applicationContext;

    /** Test na zápis dat do xml, jejich načtení a porovnání. */
    @Test
    public void testDataIntegrity() throws JAXBException, IOException {
        FindingAidImport fa = createFindingAid(true);

        File out = File.createTempFile("fa-export", ".xml");
        out.deleteOnExit();

        createMarshaller().marshal(fa, out);

        FindingAidImport faFromFile = (FindingAidImport) createUnmarshaller().unmarshal(out);

        Assert.isTrue(fa.equals(faFromFile));
    }

    /** Test na validaci dat oproti vygenerovanému xsd. */
    @Test
    public void testValidity() throws JAXBException, SAXException, IOException {
        FindingAidImport fa = createFindingAid(true);

        Marshaller marshaller = createMarshaller();
        marshaller.setSchema(createSchema());
        marshaller.marshal(fa, new DefaultHandler());

        Assert.isTrue(true);
    }

    /** Test na validaci dat oproti vygenerovanému xsd. Data nejsou validní. */
    @Test(expected = MarshalException.class)
    public void testValidityWithInvalidData() throws JAXBException, SAXException, IOException {
        FindingAidImport fa = createFindingAid(false);

        Marshaller marshaller = createMarshaller();
        marshaller.setSchema(createSchema());
        marshaller.marshal(fa, new DefaultHandler());

        Assert.isTrue(false);
    }

    /**
     * Vytvoření archivní pomůcky.
     *
     * @param valid příznak zda mají být data validní, kvůli porovnání s xsd
     *
     * @return archivní pomůcka
     */
    private FindingAidImport createFindingAid(boolean valid) {
        FindingAidImport findingAidImport = new FindingAidImport();
        FindingAid fa = new FindingAid();
        fa.setName("Import ze SUZAP");

        findingAidImport.setFindingAid(fa);
        List<Record> records = createRecords(valid);
        findingAidImport.setRecords(records);
        List<Party> parties = createParties(records);
        findingAidImport.setParties(parties);
        fa.setRootLevel(createLevelTree(records, parties));

        return findingAidImport;
    }

    /**
     * Vytvoří strom archivní pomůcky.
     *
     * @param records rejstříková hesla
     * @param parties osoby
     *
     * @return kořenový uzel
     */
    private Level createLevelTree(List<Record> records, List<Party> parties) {
        Level rootLevel = new Level();
        rootLevel.setPosition(1);

        rootLevel.setSubLevels(createChildren(TREE_DEPTH_COUNT, records, parties));
        rootLevel.setDescItems(createDescItems(records, parties));

        return rootLevel;
    }

    /**
     * Vytvoří hodnoty uzlu.
     *
     * @param records rejstříková hesla
     * @param parties osoby
     *
     * @return hodnoty uzlu
     */
    private List<AbstractDescItem> createDescItems(List<Record> records, List<Party> parties) {
        List<AbstractDescItem> values = new ArrayList<AbstractDescItem>(DESC_ITEMS_COUNT);
        int position = 1;
        values.add(createValueCoordinates(position++));
        values.add(createValueDecimal(position++));
        values.add(createValueFormattedText(position++));
        values.add(createValueInteger(position++));
        values.add(createValuePartyRef(position++, parties));
        values.add(createValueRecordRef(position++, records));
        values.add(createValueString(position++));
        values.add(createValueText(position++));
        values.add(createValueUnitDate(position++));
        values.add(createValueUnitId(position++));

        return values;
    }

    private DescItemCoordinates createValueCoordinates(int position) {
        DescItemCoordinates value = new DescItemCoordinates();
        fillCommonValueFields(value ,position);
        value.setValue("coordinates " + position);

        return value;
    }

    private DescItemDecimal createValueDecimal(int position) {
        DescItemDecimal value = new DescItemDecimal();
        fillCommonValueFields(value ,position);
        value.setValue(new BigDecimal(DESC_ITEMS_COUNT * position));

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

    private DescItemPartyRef createValuePartyRef(int position, List<Party> parties) {
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

    private List<Level> createChildren(int depth, List<Record> records, List<Party> parties) {
        if (depth < 1) {
            return null;
        }
        List<Level> children = new ArrayList<Level>(CHILD_COUNT);
        for (int i = 0; i < CHILD_COUNT; i++) {
            Level child = new Level();
            child.setSubLevels(createChildren(--depth, records, parties));
            child.setPosition(i);
            child.setDescItems(createDescItems(records, parties));
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
     * @param valid příznak zda mají být hesla validní nebo ne, kvůli testu oproti xsd
     *
     * @return seznam rejstříkových hesel
     */
    private List<Record> createRecords(boolean valid) {
        List<Record> records = new ArrayList<Record>(RECORD_COUNT);

        for (int i = 0; i < RECORD_COUNT; i++) {
            Record record = createRecord(valid, i, 0);

            records.add(record);
        }

        return records;
    }

    /**
     * Vytvoří rejstříkové heslo.
     *
     * @param valid příznak zda má být heslo validní nebo ne, kvůli testu oproti xsd
     * @param index číslo vytvářeného rejstříkového hesla
     * @param depth hloubka zanoření v hierarchii hesel
     *
     * @return rejstříkové heslo
     */
    private Record createRecord(boolean valid, int index, int depth) {
        Record record = new Record();
        record.setCharacteristics("characteristics " + index);
        record.setComment("comment " + index);
        record.setExternalId("externalId " + index);
        record.setExternalSourceCode("externalSourceCode " + index);
        record.setLocal(RandomUtils.nextBoolean());
        record.setRecord("record " + index);
        String idPrefix = StringUtils.repeat("sub-", depth);
        if (valid) {
            record.setRecordId(idPrefix + "recordId-" + index);
        } else {
            record.setRecordId(idPrefix + "recordId " + index);
        }
        record.setRegisterTypeCode("registerTypeCode " + index);
        record.setVariantRecords(createVariantRecords());

        if (RandomUtils.nextBoolean()) {
            List<Record> subRecords = new ArrayList<Record>(1);
            record.setRecords(subRecords);

            subRecords.add(createRecord(valid, 1, ++depth));
        }
        return record;
    }

    /**
     * Vytvoří seznam variantních rejstříkových hesel.
     *
     * @return seznam variantních rejstříkových hesel
     */
    private List<VariantRecord> createVariantRecords() {
        List<VariantRecord> variantRecords = new ArrayList<VariantRecord>(VARIANT_RECORD_COUNT);
        for (int i = 0; i < VARIANT_RECORD_COUNT; i++) {
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
        variantRecord.setRecord("variantRecord " + index);
        return variantRecord;
    }

    /**
     * Vytvoří seznam osob.
     *
     * @param records seznam rejstříků pro navázání na osoby
     *
     * @return seznam osob
     */
    private List<Party> createParties(List<Record> records) {
        List<Party> parties = new ArrayList<Party>(PARTY_COUNT);
        for (int i = 0; i < PARTY_COUNT; i++) {
            Party party = createParty(records, i);

            parties.add(party);
        }
        return parties;
    }

    /**
     * Vytvoří osobu.
     *
     * @param records seznam rejstříků pro navázání na osoby
     * @param index číslo vytvářené osoby
     *
     * @return osoba
     */
    private Party createParty(List<Record> records, int index) {
        Party party = new Party();
        party.setPartyId("partyId-" + index);
        party.setPartyTypeCode("partyTypeCode " + index);

        party.setRecord(records.get(RandomUtils.nextInt(records.size())));

        if (RandomUtils.nextBoolean()) {
            PartyName partyName = new PartyName();
            partyName.setAnotation("anotation " + index);
            partyName.setDegreeAfter("degreeAfter " + index);
            partyName.setDegreeBefore("degreeBefore " + index);
            partyName.setMainPart("mainPart " + index);
            partyName.setOtherPart("otherPart " + index);
            partyName.setValidFrom(new Date());
            partyName.setValidTo(new Date());
//                partyName.setValidFrom(LocalDateTime.now());
//                partyName.setValidTo(LocalDateTime.now());

            party.setPrefferedName(partyName);
        }
        return party;
    }

    /**
     * Vytvoří marshaller pro zápis dat do xml.
     *
     * @return marshaller
     */
    private Marshaller createMarshaller() throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(FindingAidImport.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        return marshaller;
    }

    /**
     * Vytvoří unmarshaller pro načtení dat do xml.
     *
     * @return unmarshaller
     */
    private Unmarshaller createUnmarshaller() throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(FindingAidImport.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        return unmarshaller;
    }

    /**
     * Vytvoří schéma.
     *
     * @return schéma
     */
    private Schema createSchema() throws SAXException, IOException {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Resource resource = applicationContext.getResource("classpath:xsd/fa-import.xsd");
        Schema schema = schemaFactory.newSchema(resource.getFile());
        return schema;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
