package cz.tacr.elza.dataexchange.output.writer.xml;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.elza.common.XmlUtils;
import cz.tacr.elza.core.data.CalendarType;
import cz.tacr.elza.core.data.PartyType;
import cz.tacr.elza.dataexchange.common.CalendarTypeConvertor;
import cz.tacr.elza.dataexchange.output.writer.PartiesOutputStream;
import cz.tacr.elza.dataexchange.output.writer.xml.nodes.FileNode;
import cz.tacr.elza.dataexchange.output.writer.xml.nodes.RootNode;
import cz.tacr.elza.dataexchange.output.writer.xml.nodes.RootNode.ChildNodeType;
import cz.tacr.elza.domain.ParDynasty;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyGroup;
import cz.tacr.elza.domain.ParPartyGroupIdentifier;
import cz.tacr.elza.domain.ParPartyName;
import cz.tacr.elza.domain.ParPartyNameComplement;
import cz.tacr.elza.domain.ParUnitdate;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.schema.v2.Event;
import cz.tacr.elza.schema.v2.Family;
import cz.tacr.elza.schema.v2.NameComplement;
import cz.tacr.elza.schema.v2.NameComplements;
import cz.tacr.elza.schema.v2.Party;
import cz.tacr.elza.schema.v2.PartyGroup;
import cz.tacr.elza.schema.v2.PartyIdentifier;
import cz.tacr.elza.schema.v2.PartyIdentifiers;
import cz.tacr.elza.schema.v2.PartyName;
import cz.tacr.elza.schema.v2.PartyNames;
import cz.tacr.elza.schema.v2.Person;
import cz.tacr.elza.schema.v2.TimeIntervalExt;

/**
 * XML output stream for parties export.
 */
public class XmlPartiesOutputStream implements PartiesOutputStream {

    private final static Logger logger = LoggerFactory.getLogger(XmlPartiesOutputStream.class);

    private final JAXBContext jaxbContext = XmlUtils.createJAXBContext(Person.class, PartyGroup.class, Family.class, Event.class);

    private final RootNode rootNode;

    private final XmlFragment fragment;

    private boolean processed;

    public XmlPartiesOutputStream(RootNode rootNode, Path tempDirectory) {
        this.rootNode = rootNode;
        this.fragment = new XmlFragment(tempDirectory);
    }

    @Override
    public void addParty(ParParty party) {
        Validate.isTrue(!processed);

        Party element = createParty(party);
        element.setApe(XmlAccessPointOutputStream.createEntry(party.getRecord()));
        element.setChr(party.getCharacteristics());
        element.setHst(party.getHistory());
        element.setId(party.getPartyId().toString());
        element.setName(createPartyName(party.getPreferredName()));
        element.setSrc(party.getSourceInformation());
        element.setVnms(createPartyNames(party));

        try {
            writeParty(element);
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    @Override
    public void processed() {
        Validate.isTrue(!processed);

        try {
            fragment.close();
        } catch (XMLStreamException | IOException e) {
            throw new SystemException(e);
        }

        if (fragment.isExist()) {
            FileNode node = new FileNode(fragment.getPath());
            rootNode.setNode(ChildNodeType.PARTIES, node);
        }
        processed = true;
    }

    @Override
    public void close() {
        if (processed) {
            return;
        }
        try {
            fragment.delete();
        } catch (IOException e) {
            throw new SystemException(e);
        }
    }

    private void writeParty(Party party) throws Exception {
        if (!fragment.isOpen()) {
            XMLStreamWriter sw = fragment.openStreamWriter();
            sw.writeStartDocument();
            sw.writeStartElement(XmlNameConsts.PARTIES);
        }

        XMLStreamWriter sw = fragment.getStreamWriter();
        String partyName = XmlNameConsts.getPartyName(party);
        JAXBElement<?> jaxbElement = XmlUtils.wrapElement(partyName, party);
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        marshaller.marshal(jaxbElement, sw);
    }

    private static Party createParty(ParParty party) {
        PartyType partyType = PartyType.fromId(party.getPartyTypeId());
        switch (partyType) {
            case PERSON:
                return new Person();
            case GROUP_PARTY:
                ParPartyGroup group = (ParPartyGroup) party;
                return createPartyGroup(group);
            case DYNASTY:
                ParDynasty dynasty = (ParDynasty) party;
                Family family = new Family();
                family.setGen(dynasty.getGenealogy());
                return family;
            case EVENT:
                return new Event();
        }
        throw new IllegalStateException("Unknown party type:" + partyType);
    }

    private static PartyName createPartyName(ParPartyName partyName) {
        PartyName element = new PartyName();
        element.setDga(partyName.getDegreeAfter());
        element.setDgb(partyName.getDegreeBefore());
        element.setMain(partyName.getMainPart());
        element.setNote(partyName.getNote());
        element.setOth(partyName.getOtherPart());
        element.setVf(createTimeIntervalEx(partyName.getValidFrom()));
        element.setVto(createTimeIntervalEx(partyName.getValidTo()));

        if (partyName.getNameFormType() != null) {
            element.setFt(partyName.getNameFormType().getCode());
        }
        List<ParPartyNameComplement> complements = partyName.getPartyNameComplements();
        if (complements != null && complements.size() > 0) {

            NameComplements listElement = new NameComplements();
            List<NameComplement> list = listElement.getNc();

            complements.forEach(source -> {
                NameComplement target = new NameComplement();
                target.setCt(source.getComplementType().getCode());
                target.setV(source.getComplement());
                list.add(target);
            });

            element.setNcs(listElement);
        }

        return element;
    }

    private static PartyNames createPartyNames(ParParty party) {
        List<ParPartyName> names = party.getPartyNames();
        if (names == null || names.isEmpty()) {
            return null;
        }
        PartyNames listElement = new PartyNames();
        List<PartyName> list = listElement.getVnm();

        names.forEach(source -> {
            PartyName target = createPartyName(source);
            list.add(target);
        });

        return listElement;
    }

    private static Party createPartyGroup(ParPartyGroup partyGroup) {
        PartyGroup element = new PartyGroup();
        element.setScp(partyGroup.getScope());
        element.setFn(partyGroup.getFoundingNorm());
        element.setSn(partyGroup.getScopeNorm());
        element.setStr(partyGroup.getOrganization());

        List<ParPartyGroupIdentifier> indetifiers = partyGroup.getPartyGroupIdentifiers();
        if (indetifiers != null && indetifiers.size() > 0) {

            PartyIdentifiers listElement = new PartyIdentifiers();
            List<PartyIdentifier> list = listElement.getPi();

            indetifiers.forEach(source -> {
                PartyIdentifier target = new PartyIdentifier();
                target.setNote(source.getNote());
                target.setSrc(source.getSource());
                target.setV(source.getIdentifier());
                target.setVf(createTimeIntervalEx(source.getFrom()));
                target.setVto(createTimeIntervalEx(source.getTo()));
                list.add(target);
            });

            element.setPis(listElement);
        }

        return element;
    }

    private static TimeIntervalExt createTimeIntervalEx(ParUnitdate partyUnitdate) {
        if (partyUnitdate == null) {
            return null;
        }

        String valueFrom = partyUnitdate.getValueFrom();
        String valueTo = partyUnitdate.getValueTo();
        if (valueFrom == null && valueTo == null) {
            logger.warn("Ignored unitdate without value, parUnitdateId:{}", partyUnitdate.getUnitdateId());
            return null;
        }
        if (valueFrom == null || valueTo == null) {
            throw new SystemException("Unitdate without from/to value").set("parUnitdateId", partyUnitdate.getUnitdateId());
        }

        TimeIntervalExt element = new TimeIntervalExt();
        if (partyUnitdate.getCalendarTypeId() != null) {
            CalendarType ct = CalendarType.fromId(partyUnitdate.getCalendarTypeId());
            element.setCt(CalendarTypeConvertor.convert(ct));
        }
        element.setF(valueFrom);
        element.setFe(partyUnitdate.getValueFromEstimated());
        element.setTo(valueTo);
        element.setToe(partyUnitdate.getValueToEstimated());
        element.setFmt(partyUnitdate.getFormat());
        element.setNote(partyUnitdate.getNote());
        element.setTf(partyUnitdate.getTextDate());

        return element;
    }
}
