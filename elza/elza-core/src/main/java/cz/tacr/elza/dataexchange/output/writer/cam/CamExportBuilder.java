package cz.tacr.elza.dataexchange.output.writer.cam;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.xml.sax.SAXException;

import cz.tacr.cam._2019.Entities;
import cz.tacr.cam._2019.Entity;
import cz.tacr.cam._2019.EntityRecordState;
import cz.tacr.cam._2019.ObjectFactory;
import cz.tacr.cam._2019.Part;
import cz.tacr.cam._2019.PartType;
import cz.tacr.cam._2019.RevInfo;
import cz.tacr.elza.common.XmlUtils;
import cz.tacr.elza.dataexchange.output.aps.ApInfo;
import cz.tacr.elza.dataexchange.output.parties.PartyInfo;
import cz.tacr.elza.dataexchange.output.sections.SectionContext;
import cz.tacr.elza.dataexchange.output.writer.ApOutputStream;
import cz.tacr.elza.dataexchange.output.writer.ExportBuilder;
import cz.tacr.elza.dataexchange.output.writer.PartiesOutputStream;
import cz.tacr.elza.dataexchange.output.writer.SectionOutputStream;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApName;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ParComplementType;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyName;
import cz.tacr.elza.domain.ParPartyNameComplement;
import cz.tacr.elza.domain.ParPartyNameFormType;
import cz.tacr.elza.domain.SysLanguage;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.exception.SystemException;

public class CamExportBuilder implements ExportBuilder {

    final protected static ObjectFactory objectcFactory = CamUtils.getObjectFactory();

    static Schema camSchema;
    {
        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try (InputStream is = CamExportBuilder.class.getClassLoader().getResourceAsStream("cam/cam-2019.xsd")) {
            camSchema = sf.newSchema(new StreamSource(is));
        } catch (IOException | SAXException e) {
            throw new RuntimeException("Failed to load internal XSD", e);
        }
    }

    private final JAXBContext jaxbContext = XmlUtils.createJAXBContext(Entities.class);

    private Entities entities;

    private ApStream apStream;
    private PartiesStream partiesStream;

    protected CamExportBuilder getExportBuilder() {
        return this;
    }

    class ApStream implements ApOutputStream {

        @Override
        public void addAccessPoint(ApInfo apInfo) {
            CamExportBuilder expBuilder = getExportBuilder();
            expBuilder.addAccessPoint(apInfo);
        }

        @Override
        public void processed() {
            // nop
        }

        @Override
        public void close() {
            CamExportBuilder expBuilder = getExportBuilder();
            Validate.notNull(expBuilder.apStream);
            expBuilder.apStream = null;
        }

    };

    class PartiesStream implements PartiesOutputStream {

        @Override
        public void addParty(PartyInfo partyInfo) {
            CamExportBuilder expBuilder = getExportBuilder();
            expBuilder.addParty(partyInfo);
        }

        @Override
        public void processed() {
            // nop
        }

        @Override
        public void close() {
            CamExportBuilder expBuilder = getExportBuilder();
            Validate.notNull(expBuilder.partiesStream);
            expBuilder.partiesStream = null;
        }
    };

    private final void initBuilder() {
        this.entities = objectcFactory.createEntities();
        Validate.isTrue(apStream == null);
        this.apStream = null;
        Validate.isTrue(partiesStream == null);
        this.partiesStream = null;
    }

    public void addParty(PartyInfo partyInfo) {
        ApState apState = partyInfo.getApState();
        Entity ent = createEntity(apState);

        // add names
        ParParty party = partyInfo.getParty();
        //ApAccessPoint ap = party.getAccessPoint();
        ParPartyName prefName = party.getPreferredName();
        addEntityName(ent, prefName);
        for (ParPartyName partyName : party.getPartyNames()) {
            if (!prefName.getPartyNameId().equals(partyName.getPartyNameId())) {
                addEntityName(ent, partyName);
            }
        }

        this.entities.getEnt().add(ent);
    }

    private void addEntityName(Entity ent, ParPartyName prefName) {
        
        Part part = objectcFactory.createPart();
        part.setPid(UUID.randomUUID().toString());
        part.setT(PartType.PT_NAME);

        part.setItms(objectcFactory.createItems());

        saveName(prefName, part);

        ent.getPrts().getP().add(part);
    }

    private void saveAPName(ApName apName, Part part) {
        CamUtils.addItemString(part, CamItemType.NM_MAIN, apName.getName());
        CamUtils.addItemString(part, CamItemType.NM_SUP_GEN, apName.getComplement());
        SysLanguage sysLang = apName.getLanguage();
        if (sysLang != null) {
            String code = sysLang.getCode();
            if (code != null) {
                CamUtils.addItemEnum(part, CamItemType.NM_LANG, "LNG_" + code);
            }
        }
    }

    private void saveName(ParPartyName partyName, Part part) {
        CamUtils.addItemString(part, CamItemType.NM_MAIN, partyName.getMainPart());
        CamUtils.addItemString(part, CamItemType.NM_MINOR, partyName.getOtherPart());
        CamUtils.addItemString(part, CamItemType.NM_DEGREE_PRE, partyName.getDegreeBefore());
        CamUtils.addItemString(part, CamItemType.NM_DEGREE_POST, partyName.getDegreeAfter());

        // NM_USED_FROM - datace uziti jmena od
        CamUtils.addItemUnitDate(part, CamItemType.NM_USED_FROM, partyName.getValidFrom());
        // NM_USED_TO - datace uziti jmena do
        CamUtils.addItemUnitDate(part, CamItemType.NM_USED_TO, partyName.getValidTo());

        // NM_TYPE -> typ formy jmena
        ParPartyNameFormType nameType = partyName.getNameFormType();
        String nmType = partyNameType2NMType(nameType.getCode());
        CamUtils.addItemEnum(part, CamItemType.NM_TYPE, nmType);

        // NM_LANG -> jazyk jmena
        // u party byl jen v DB a nikdy v UI, nemusi se prevadet

        // NM_ORDER -> poradi
        Long order = null;
        // NM_SUP_GEN -> obecny doplnek
        // NM_SUP_CHRO -> chronologicky doplnek
        // NM_SUP_AUTH -> autor tvurce
        // NM_SUP_GEO -> geograficky doplnek
        List<String> sbSupGen = new ArrayList<>(),
                sbSupChro = new ArrayList<>(), sbSupAuth = new ArrayList<>(),
                sbSupGeo = new ArrayList<>();

        List<ParPartyNameComplement> complements = partyName.getPartyNameComplements();
        if (complements != null) {
            for (ParPartyNameComplement compl : complements) {
                ParComplementType complType = compl.getComplementType();
                switch(complType.getCode()) {
                case "INITIALS":
                    // rozpis iniciál
                    sbSupGen.add(compl.getComplement());
                    break;
                case "GENERAL":
                    // obecný doplněk
                    sbSupGen.add(compl.getComplement());
                    break;
                case "ROMAN_NUM":
                    // římské číslice
                    sbSupGen.add(compl.getComplement());
                    break;
                case "GEO":
                    // geografický doplněk
                    sbSupGeo.add(compl.getComplement());
                    break;
                case "TIME":
                    // chronologický doplněk
                    sbSupChro.add(compl.getComplement());
                    break;
                case "ORDER":
                    // pořadí události
                    if (order == null) {
                        String v = compl.getComplement();
                        try {
                            order = Long.parseLong(v);
                        } catch (NumberFormatException nfe) {
                            sbSupGen.add(v);
                        }
                    } else {
                        sbSupGen.add(compl.getComplement());
                    }
                    break;
                }
            }
        }
        // store supplements
        CamUtils.addItemNumber(part, CamItemType.NM_ORDER, order);
        CamUtils.addItemString(part, CamItemType.NM_SUP_GEN, StringUtils.join(sbSupGen, ","));
        CamUtils.addItemString(part, CamItemType.NM_SUP_CHRO, StringUtils.join(sbSupChro, ","));
        CamUtils.addItemString(part, CamItemType.NM_SUP_AUTH, StringUtils.join(sbSupAuth, ","));
        CamUtils.addItemString(part, CamItemType.NM_SUP_GEO, StringUtils.join(sbSupGeo, ","));
    }

    private String partyNameType2NMType(String code) {
        // Problemy:
        // - jak prevest: "LEGAL2", "PREFERED", "USED"?  
        
        switch(code) {
        case "LEGAL":
            // <name>úřední / skutečné / světské jméno / jméno za svobodna</name>
            // -> úřední
            return "NT_OFFICIAL";
        case "ABBRV":
            // <name>akronym / zkratka</name>
            // -> zkratka/akronym
            return "NT_ACRONYM";
        case "AUTHOR":
            // <name>autorská šifra</name>
            // -> autorská šifra
            return "NT_AUTHORCIPHER";
        case "PSEUDONYM":
            // <name>pseudonym</name>
            // -> pseudonym
            return "NT_PSEUDONYM";
        case "CHURCH":
            // <name>církevní jméno</name>
            // -> církevní
            return "NT_RELIGIOUS";
        case "MARRIAGE":
            // <name>jméno získané sňatkem</name>
            // ->  přijaté
            return "NT_ACCEPTED";
        case "HISTORICAL":
            // <name>historická podoba jména</name>
            // -> historická podoba
            return "NT_HISTORICAL";
        case "ORDER":
            // <name>přímé pořadí</name>
            // -> přímé pořadí
            return "NT_DIRECT";
        case "LEGAL2":
            // <name>jméno úřední</name>
            // -> úřední
            return "NT_OFFICIAL";
        case "PREFERED":
            // <name>jméno preferované entitou</name>
            // -> současná podoba
            return "NT_ACTUAL";
        case "HISTORICAL2":
            // <name>historická / dřívější forma jména</name>
            // -> historická/dřívější podoba
            return "NT_FORMER";
        case "HISTORICAL3":
            // <name>jediný známý tvar jména v daném období</name>
            // -> jediný známý tvar
            return "NT_ONLYKNOWN";    
        case "ARTIFICIAL":
            // <name>uměle vytvořené označení</name>
            // -> uměle vytvořené
            return "NT_ARTIFICIAL";
        case "SINGULAR":
            // <name>singulár</name>
            // -> singulár
            return "NT_SINGULAR";
        case "PLURAL":
            // <name>plurál</name>
            // -> plurál
            return "NT_PLURAL";
        case "TERM":
            // <name>přejatý termín</name>
            // -> přejatý termín
            return "NT_TAKEN";
        case "HISTORICAL4":
            // <name>zastaralý, historický termín</name>
            // -> historická podoba
            return "NT_HISTORICAL";
        case "INAPPROPRIATE":
            // <name>nevhodný termín</name>
            // -> nevhodný termín
            return "NT_INAPPROPRIATE";
        case "NARROW":
            // <name>užší termín</name>
            // -> užší termín
            return "NT_NARROWER";    
        case "SPECIAL":
            // <name>odborný termín</name>
            // -> odborný termín
            return "NT_TERM";
        case "INVERTED":
            // <name>invertovaná podoba</name>
            // -> invertovaná podoba
            return "NT_INVERTED";
        case "ANTONYM":
            // <name>antonymum</name>
            // -> antonymum
            return "NT_ANTONYMUM";
        case "HOMONYM":
            // <name>homonymum</name>
            // -> homonymum
            return "NT_HOMONYMUM";
        case "USED":
            // <name>jméno, pod nímž je entita nejvíce známá</name>
            // -> současná podoba
            return "NT_ACTUAL";
        default:
            throw new SystemException("Cannot convert name type: " + code);
        }

        // Nevyuzite typy pri prevodu:
        /*
        ekvivalent,NT_EQUIV    
        překlad,NT_TRANSLATED        
        přezdívka/zlidovělá podoba,NT_ALIAS
        zjednodušená podoba,NT_SIMPLIFIED
        zkomolená podoba,NT_GARBLED
        "podoba, s, čestným, názvem",NT_HONOR
        podle jiných pravidel,NT_OTHERRULES    
        rodné,NT_NATIV
        světské,NT_SECULAR
        současná podoba,NT_ACTUAL
        zlidovělá podoba,NT_FOLK
        název originálu,NT_ORIGINA
        */
    }

    public void addAccessPoint(ApInfo apInfo) {
        Entity ent = createEntity(apInfo.getApState());
        Collection<ApName> names = apInfo.getNames();
        for (ApName name : names) {
            addAPName(ent, name);
        }
        this.entities.getEnt().add(ent);
    }

    private void addAPName(Entity ent, ApName apName) {
        Part part = objectcFactory.createPart();
        part.setPid(UUID.randomUUID().toString());
        part.setT(PartType.PT_NAME);

        part.setItms(objectcFactory.createItems());

        saveAPName(apName, part);

        ent.getPrts().getP().add(part);
    }

    private Entity createEntity(ApState apState) {
        Entity ent = new Entity();
        ent.setEid(apState.getAccessPointId());
        ent.setEuid(apState.getAccessPoint().getUuid());
        // entity class
        ent.setEnt(apState.getApType().getCode());

        // set state
        EntityRecordState ens;
        switch (apState.getStateApproval()) {
        case NEW:
        case TO_AMEND:
        case TO_APPROVE:
            ens = EntityRecordState.ERS_NEW;
        case APPROVED:
            ens = EntityRecordState.ERS_APPROVED;
            break;
        default:
            throw new SystemException("Missing mapping of internal state to CAM state");
        }
        ent.setEns(ens);

        RevInfo revInfo = createRevInfo(apState);
        ent.setRevi(revInfo);

        // Prepare empty parts
        ent.setPrts(objectcFactory.createParts());

        return ent;
    }

    private RevInfo createRevInfo(ApState apState) {
        RevInfo revInfo = objectcFactory.createRevInfo();

        // Set revision id to UUID of accesspoint
        // TODO: User proper UUID of revision (when will be available)
        revInfo.setRid(UUID.randomUUID().toString());

        ApChange createChange = apState.getCreateChange();
        revInfo.setModt(XmlUtils.convertDate(createChange.getChangeDate().toLocalDateTime()));

        // User info
        String usr = "system";
        UsrUser user = createChange.getUser();
        if (user != null) {
            // TODO: Improve user info
            usr = user.getUsername();
        }
        revInfo.setUsr(usr);
        return revInfo;
    }

    public CamExportBuilder() {
        initBuilder();
    }

    @Override
    public SectionOutputStream openSectionOutputStream(SectionContext sectionContext) {
        throw new SystemException("CAM format does not support sections");
    }

    @Override
    public ApOutputStream openAccessPointsOutputStream() {
        Validate.isTrue(apStream == null);
        if (apStream == null) {
            apStream = new ApStream();
        }
        return apStream;
    }

    @Override
    public PartiesOutputStream openPartiesOutputStream() {
        Validate.isTrue(partiesStream == null);
        if (partiesStream == null) {
            partiesStream = new PartiesStream();
        }
        return partiesStream;
    }

    @Override
    public void build(OutputStream os) throws XMLStreamException {
        JAXBElement<Entities> jaxbEnts = objectcFactory.createEnts(this.entities);
        
        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setSchema(camSchema);
            marshaller.marshal(jaxbEnts, os);
        } catch (JAXBException e) {
            throw new XMLStreamException("Failed to save with JAXB", e);
        }
    }

    @Override
    public void clear() {
        initBuilder();
    }

}
