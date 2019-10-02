package cz.tacr.elza.dataexchange.output.writer.xml;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.elza.common.XmlUtils;
import cz.tacr.elza.core.data.CalendarType;
import cz.tacr.elza.core.data.PartyType;
import cz.tacr.elza.dataexchange.common.CalendarTypeConvertor;
import cz.tacr.elza.dataexchange.output.parties.PartyInfo;
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
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
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
public class XmlPartiesOutputStream extends BaseFragmentStream
        implements PartiesOutputStream {

    private final static Logger logger = LoggerFactory.getLogger(XmlPartiesOutputStream.class);

    private final JAXBContext jaxbContext = XmlUtils.createJAXBContext(Person.class, PartyGroup.class, Family.class, Event.class);

    private final RootNode rootNode;

    public XmlPartiesOutputStream(RootNode rootNode, Path tempDirectory) {
        super(tempDirectory);
        this.rootNode = rootNode;
    }

    @Override
    public void addParty(PartyInfo partyInfo) {
        Validate.isTrue(!isProcessed());

        Party element = createParty(partyInfo);
        try {
            writeParty(element);
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    @Override
    public void processed() {
        finishFragment();

        if (fragment.isExist()) {
            FileNode node = new FileNode(fragment.getPath());
            rootNode.setNode(ChildNodeType.PARTIES, node);
        }
    }

    @Override
    public void close() {
        closeFragment();
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

    private static Party createParty(PartyInfo partyInfo) {
        PartyType partyType = PartyType.fromId(partyInfo.getParty().getPartyTypeId());
        switch (partyType) {
        case PERSON:
            Person person = new Person();
            initCommonParty(person, partyInfo);
            return person;
        case GROUP_PARTY:
            PartyGroup partyGroup = new PartyGroup();
            initPartyGroup(partyGroup, partyInfo);
            return partyGroup;
        case DYNASTY:
            Family family = new Family();
            initFamily(family, partyInfo);
            return family;
        case EVENT:
            Event event = new Event();
            initCommonParty(event, partyInfo);
            return event;
        }
        throw new IllegalStateException("Unknown party type, name=" + partyType.name());
    }

    private static void initCommonParty(Party element, PartyInfo partyInfo) {
        ParParty party = partyInfo.getParty();
        element.setChr(party.getCharacteristics());
        element.setHst(party.getHistory());
        element.setId(party.getPartyId().toString());
        element.setNms(createPartyNames(party));
        element.setSrc(party.getSourceInformation());
        // init entry
        element.setApe(XmlApOutputStream.createEntry(partyInfo.getApState(), partyInfo.getExternalIds()));
    }

    private static void initPartyGroup(PartyGroup element, PartyInfo partyInfo) {
        initCommonParty(element, partyInfo);
        // init group
        ParPartyGroup partyGroup = (ParPartyGroup) partyInfo.getParty();
        element.setScp(partyGroup.getScope());
        element.setFn(partyGroup.getFoundingNorm());
        element.setSn(partyGroup.getScopeNorm());
        element.setStr(partyGroup.getOrganization());
        // init identifiers
        PartyIdentifiers identifiers = createPartyGroupIdentifiers(partyGroup.getPartyGroupIdentifiers());
        element.setPis(identifiers);
    }

    private static void initFamily(Family element, PartyInfo partyInfo) {
        initCommonParty(element, partyInfo);
        // init family
        ParDynasty dynasty = (ParDynasty) partyInfo.getParty();
        element.setGen(dynasty.getGenealogy());
    }

    private static PartyIdentifiers createPartyGroupIdentifiers(Collection<ParPartyGroupIdentifier> identifiers) {
        if (CollectionUtils.isEmpty(identifiers)) {
            return null;
        }
        PartyIdentifiers listElement = new PartyIdentifiers();
        List<PartyIdentifier> list = listElement.getPi();

        for (ParPartyGroupIdentifier indetifier : identifiers) {
            PartyIdentifier element = new PartyIdentifier();
            element.setNote(indetifier.getNote());
            element.setSrc(indetifier.getSource());
            element.setV(indetifier.getIdentifier());
            element.setVf(createTimeIntervalExt(indetifier.getFrom()));
            element.setVto(createTimeIntervalExt(indetifier.getTo()));
            list.add(element);
        }
        return listElement;
    }

    private static PartyName createPartyName(ParPartyName partyName) {
        PartyName element = new PartyName();
        element.setDga(partyName.getDegreeAfter());
        element.setDgb(partyName.getDegreeBefore());
        element.setMain(partyName.getMainPart());
        element.setNote(partyName.getNote());
        element.setOth(partyName.getOtherPart());
        element.setVf(createTimeIntervalExt(partyName.getValidFrom()));
        element.setVto(createTimeIntervalExt(partyName.getValidTo()));
        // init form type (loaded from static data)
        if (partyName.getNameFormType() != null) {
            element.setFt(partyName.getNameFormType().getCode());
        }
        // init complements
        NameComplements cmpls = createNameCmpls(partyName.getPartyNameComplements());
        element.setNcs(cmpls);
        return element;
    }

    private static NameComplements createNameCmpls(Collection<ParPartyNameComplement> nameCmpls) {
        if (CollectionUtils.isEmpty(nameCmpls)) {
            return null;
        }
        NameComplements listElement = new NameComplements();
        List<NameComplement> list = listElement.getNc();

        for (ParPartyNameComplement nameCmpl : nameCmpls) {
            NameComplement element = new NameComplement();
            // init complement type (loaded from static data)
            element.setCt(nameCmpl.getComplementType().getCode());
            element.setV(nameCmpl.getComplement());
            list.add(element);
        }
        return listElement;
    }

    private static PartyNames createPartyNames(ParParty party) {
        PartyNames listElement = new PartyNames();
        List<PartyName> list = listElement.getNm();

        ParPartyName prefName = party.getPreferredName();
        list.add(createPartyName(prefName));

        List<ParPartyName> partyNames = party.getPartyNames();
        if (partyNames == null) {
            throw new BusinessException("Party without names", BaseCode.DB_INTEGRITY_PROBLEM)
                    .set("partyId", party.getPartyId())
                    .set("prefNameId", prefName.getPartyNameId());
        }
        for (ParPartyName name : partyNames) {
            if (prefName.getPartyNameId().equals(name.getPartyNameId())) {
                continue; // preferred already added as first
            }
            list.add(createPartyName(name));
        }
        return listElement;
    }

    private static TimeIntervalExt createTimeIntervalExt(ParUnitdate partyUnitdate) {
        if (partyUnitdate == null) {
            return null;
        }
        String valueFrom = partyUnitdate.getValueFrom();
        String valueTo = partyUnitdate.getValueTo();
        if (valueFrom == null && valueTo == null) {
            logger.warn("Ignored unitdate without value, parUnitdateId={}", partyUnitdate.getUnitdateId());
            return null;
        }
        if (valueFrom == null || valueTo == null) {
            throw new SystemException("Unitdate without from/to value").set("parUnitdateId",
                                                                            partyUnitdate.getUnitdateId());
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
