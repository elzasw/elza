package cz.tacr.elza.suzap;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.lang.math.RandomUtils;
import org.springframework.util.Assert;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import cz.tacr.elza.suzap.v1.xml.AbstractValue;
import cz.tacr.elza.suzap.v1.xml.FindingAid;
import cz.tacr.elza.suzap.v1.xml.Level;
import cz.tacr.elza.suzap.v1.xml.Party;
import cz.tacr.elza.suzap.v1.xml.PartyName;
import cz.tacr.elza.suzap.v1.xml.Record;
import cz.tacr.elza.suzap.v1.xml.ValueCoordinates;
import cz.tacr.elza.suzap.v1.xml.ValueDecimal;
import cz.tacr.elza.suzap.v1.xml.ValueFormattedText;
import cz.tacr.elza.suzap.v1.xml.ValueInteger;
import cz.tacr.elza.suzap.v1.xml.ValuePartyRef;
import cz.tacr.elza.suzap.v1.xml.ValueRecordRef;
import cz.tacr.elza.suzap.v1.xml.ValueString;
import cz.tacr.elza.suzap.v1.xml.ValueText;
import cz.tacr.elza.suzap.v1.xml.ValueUnitDate;
import cz.tacr.elza.suzap.v1.xml.ValueUnitId;
import cz.tacr.elza.suzap.v1.xml.VariantRecord;

/**
 * Testy na suzap.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 27. 10. 2015
 */
public class SuzapTest {

    private static final int RECORD_COUNT = 10;

    private static final int VARIANT_RECORD_COUNT = 1;

    private static final int PARTY_COUNT = 10;

    private static final int CHILD_COUNT = 3;

    private static final int TREE_DEPTH_COUNT = 1;

    private static final int VALUE_COUNT = 2;

//    @Test
    public void marshall() throws JAXBException {
        FindingAid fa = new FindingAid();
        fa.setName("Import ze SUZAP");

        List<Record> records = createRecords();
        fa.setRecords(records);
        List<Party> parties = createParties(records);
        fa.setParties(parties);
        fa.setRootLevel(createLevelTree(records, parties));

        JAXBContext jaxbContext = JAXBContext.newInstance(FindingAid.class);
        Marshaller marshaller = jaxbContext.createMarshaller();

        File out = new File("/tmp/suzapi-out.xml");
        marshaller.marshal(fa, out);

        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

        FindingAid faFromFile = (FindingAid) unmarshaller.unmarshal(out);
        Assert.isTrue(fa.equals(faFromFile));
    }

//    @Test
    public void validation() throws JAXBException, SAXException {
        FindingAid fa = new FindingAid();
        fa.setName("Import ze SUZAP");

        List<Record> records = createRecords();
        fa.setRecords(records);
        List<Party> parties = createParties(records);
        fa.setParties(parties);
        fa.setRootLevel(createLevelTree(records, parties));

        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = schemaFactory.newSchema(new File("d:\\marbes\\projekty\\elza\\elza-core\\target\\xsd\\schema1.xsd"));

        JAXBContext jaxbContext = JAXBContext.newInstance(FindingAid.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setSchema(schema);
        marshaller.marshal(fa, new DefaultHandler());
    }

    private Level createLevelTree(List<Record> records, List<Party> parties) {
        Level rootLevel = new Level();
        rootLevel.setPosition(1);

        rootLevel.setLevels(createChildren(TREE_DEPTH_COUNT, records, parties));
        rootLevel.setValues(createValues(records, parties));

        return rootLevel;
    }

    private List<AbstractValue> createValues(List<Record> records, List<Party> parties) {
        List<AbstractValue> values = new ArrayList<AbstractValue>(VALUE_COUNT);
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

    private ValueCoordinates createValueCoordinates(int position) {
        ValueCoordinates value = new ValueCoordinates();
        fillCommonValueFields(value ,position);
        value.setValue("coordinates-" + position);

        return value;
    }

    private ValueDecimal createValueDecimal(int position) {
        ValueDecimal value = new ValueDecimal();
        fillCommonValueFields(value ,position);
        value.setValue(new BigDecimal(VALUE_COUNT * position));

        return value;
    }

    private ValueFormattedText createValueFormattedText(int position) {
        ValueFormattedText value = new ValueFormattedText();
        fillCommonValueFields(value ,position);
        value.setValue("formatted text-" + position);

        return value;
    }

    private ValueInteger createValueInteger(int position) {
        ValueInteger value = new ValueInteger();
        fillCommonValueFields(value ,position);
        value.setValue(position);

        return value;
    }

    private ValuePartyRef createValuePartyRef(int position, List<Party> parties) {
        ValuePartyRef value = new ValuePartyRef();
        fillCommonValueFields(value ,position);

        value.setParty(parties.get(RandomUtils.nextInt(parties.size())));

        return value;
    }

    private ValueRecordRef createValueRecordRef(int position, List<Record> records) {
        ValueRecordRef value = new ValueRecordRef();
        fillCommonValueFields(value ,position);
        value.setRecord(records.get(RandomUtils.nextInt(records.size())));

        return value;
    }

    private ValueString createValueString(int position) {
        ValueString value = new ValueString();
        fillCommonValueFields(value ,position);
        value.setValue("string-" + position);

        return value;
    }

    private ValueText createValueText(int position) {
        ValueText value = new ValueText();
        fillCommonValueFields(value ,position);
        value.setValue("text-" + position);

        return value;
    }

    private ValueUnitDate createValueUnitDate(int position) {
        ValueUnitDate value = new ValueUnitDate();
        fillCommonValueFields(value ,position);
        value.setCalendarTypeCode("calendarTypeCode " + position);
        value.setValueFrom("valueFrom-" + position);
        value.setValueFromEstimated(RandomUtils.nextBoolean());
        value.setValueTo("valueTo-" + position);
        value.setValueToEstimated(RandomUtils.nextBoolean());

        return value;
    }

    private ValueUnitId createValueUnitId(int position) {
        ValueUnitId value = new ValueUnitId();
        fillCommonValueFields(value ,position);
        value.setValue("unitId-" + position);

        return value;
    }

    private void fillCommonValueFields(AbstractValue value, int position) {
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
            child.setLevels(createChildren(--depth, records, parties));
            child.setPosition(i);
            child.setValues(createValues(records, parties));

            children.add(child);
        }
        return children;
    }

    private List<Record> createRecords() {
        List<Record> records = new ArrayList<Record>(RECORD_COUNT);

        for (int i = 0; i < RECORD_COUNT; i++) {
            Record record = new Record();
            record.setCharacteristics("characteristics-" + i);
            record.setComment("comment-" + i);
            record.setExternalId("externalId-" + i);
            record.setExternalSourceCode("externalSourceCode-" + i);
            record.setLocal(RandomUtils.nextBoolean());
            record.setRecord("record-" + i);
            record.setRecordId("recordId-" + i);
            record.setRegisterTypeCode("registerTypeCode-" + i);
            record.setVariantRecords(createVariantRecords());

            records.add(record);
        }

        return records;
    }

    private List<VariantRecord> createVariantRecords() {
        List<VariantRecord> variantRecords = new ArrayList<VariantRecord>(VARIANT_RECORD_COUNT);
        for (int i = 0; i < VARIANT_RECORD_COUNT; i++) {
            VariantRecord variantRecord = new VariantRecord();
            variantRecord.setRecord("variantRecord-" + i);

            variantRecords.add(variantRecord);
        }
        return variantRecords;
    }

    private List<Party> createParties(List<Record> records) {
        List<Party> parties = new ArrayList<Party>(PARTY_COUNT);
        for (int i = 0; i < PARTY_COUNT; i++) {
            Party party = new Party();
            party.setPartyId("partyId-" + i);
            party.setPartyTypeCode("partyTypeCode-" + i);

            party.setRecord(records.get(RandomUtils.nextInt(records.size())));

            if (RandomUtils.nextBoolean()) {
                PartyName partyName = new PartyName();
                partyName.setAnotation("anotation-" + i);
                partyName.setDegreeAfter("degreeAfter-" + i);
                partyName.setDegreeBefore("degreeBefore-" + i);
                partyName.setMainPart("mainPart-" + i);
                partyName.setOtherPart("otherPart-" + i);
                partyName.setValidFrom(new Date());
                partyName.setValidTo(new Date());
//                partyName.setValidFrom(LocalDateTime.now());
//                partyName.setValidTo(LocalDateTime.now());

                party.setPrefferedName(partyName);
            }

            parties.add(party);
        }
        return parties;
    }
}
