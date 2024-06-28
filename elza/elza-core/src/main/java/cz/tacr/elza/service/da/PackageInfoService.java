package cz.tacr.elza.service.da;

import com.lightcomp.kads.premis.PremisReaderWriter;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaAipState;
import cz.tacr.elza.domain.DaChange;
import cz.tacr.elza.domain.DaChangeType;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.repository.AipRepository;
import cz.tacr.elza.repository.AipStateRepository;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.DaChangeRepository;
import cz.tacr.elza.repository.FundRepository;
import cz.tacr.elza.repository.InstitutionRepository;
import cz.tacr.elza.service.vo.Agent;
import cz.tacr.elza.service.vo.Event;
import cz.tacr.elza.service.vo.IntellectualObject;
import cz.tacr.elza.service.vo.PackageObject;
import cz.tacr.elza.service.vo.RepresentationObject;
import gov.loc.premis.v3.AgentComplexType;
import gov.loc.premis.v3.AgentIdentifierComplexType;
import gov.loc.premis.v3.EventComplexType;
import gov.loc.premis.v3.IntellectualEntity;
import gov.loc.premis.v3.LinkingAgentIdentifierComplexType;
import gov.loc.premis.v3.LinkingObjectIdentifierComplexType;
import gov.loc.premis.v3.ObjectComplexType;
import gov.loc.premis.v3.ObjectIdentifierComplexType;
import gov.loc.premis.v3.PremisComplexType;
import gov.loc.premis.v3.Representation;
import gov.loc.premis.v3.SignificantPropertiesComplexType;
import gov.loc.premis.v3.StringPlusAuthority;
import jakarta.transaction.Transactional;
import jakarta.xml.bind.JAXBException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PackageInfoService {

    @Autowired
    private AipRepository aipRepository;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private DaChangeRepository changeRepository;

    @Autowired
    private ApAccessPointRepository accessPointRepository;
    @Autowired
    private AipStateRepository aipStateRepository;
    @Autowired
    private FundRepository fundRepository;

    @Transactional
    public DaAipState processPackageInfo(ArrDigitalRepository digitalRepository, File file) throws FileNotFoundException, JAXBException {
        FileInputStream is = new FileInputStream(file);
        DaAipState aipState = new DaAipState();
        PremisComplexType premisComplexType = PremisReaderWriter.unmarshal(is);

        List<Agent> agentList = readAgents(premisComplexType.getAgent());
        Map<String, Agent> agentMap = agentList.stream().collect(Collectors.toMap(Agent::getLocalIdentifier, Function.identity()));
        List<PackageObject> objectList = readObjects(premisComplexType.getObject());
        List<Event> eventList = readEvents(agentMap, premisComplexType.getEvent());
        DaAip daAip = null;
        DaChange daChange = new DaChange();
        String aipCode = null;
        String fundCode = null;
        for (PackageObject packageObject : objectList) {
            if (packageObject instanceof IntellectualObject intellectualObject) {
                if (intellectualObject.getAipId() != null) {
                    aipCode = intellectualObject.getAipId();
                }
                if (intellectualObject.getFondsId() != null) {
                    fundCode = intellectualObject.getFondsId();
                }
                aipState.setInstitutionCode(intellectualObject.getInstituitionId());
                ParInstitution parInstitution = institutionRepository.findByInternalCode(intellectualObject.getInstituitionId());
                aipState.setInstitution(parInstitution);
                if (intellectualObject.getAipSize() != null) {
                    aipState.setAipSize(Long.valueOf(intellectualObject.getAipSize()));
                }
                aipState.setAipVersion(intellectualObject.getAipVersion());
            }
        }
        daAip = aipRepository.findByCode(aipCode);
        if (daAip == null) {
            daChange.setType(DaChangeType.AIP_CREATE);
            daAip = new DaAip();
            daAip.setCode(aipCode);
            daAip.setDigitalRepository(digitalRepository);
            aipRepository.save(daAip);
        } else {
            DaAipState oldAipState = aipStateRepository.findByDaAipAndDeleteChangeIsNull(daAip);
            if (oldAipState != null) {
                oldAipState.setDeleteChange(daChange);
                aipStateRepository.save(oldAipState);
            }
            daChange.setType(DaChangeType.AIP_UPDATE);
        }
        if (fundCode != null) {
            ArrFund arrFund = fundRepository.findByInternalCode(fundCode);
            aipState.setFund(arrFund);
        }

        aipState.setDaAip(daAip);
        daChange.setDaAip(daAip);
        daChange.setChangeDate(LocalDateTime.now());
        changeRepository.save(daChange);
        aipState.setCreateChange(daChange);
        for (Event event : eventList) {
            if (event.getOriginator() != null) {
                List<String> nameList = event.getOriginator().getNameList();
                if (!nameList.isEmpty()) {
                    aipState.setOriginator(nameList.get(0));
                }
                accessPointRepository.findById(Integer.valueOf(event.getOriginator().getCamId())).ifPresent(aipState::setOriginatorAccessPoint);
            }
        }
        return aipStateRepository.save(aipState);
    }

    public String getFundCodeFromPackageInfoFile(File file) throws FileNotFoundException, JAXBException {
        FileInputStream is = new FileInputStream(file);
        PremisComplexType premisComplexType = PremisReaderWriter.unmarshal(is);
        List<PackageObject> objectList = readObjects(premisComplexType.getObject());
        for (PackageObject packageObject : objectList) {
            if (packageObject instanceof IntellectualObject intellectualObject && intellectualObject.getFondsId() != null) {
                return intellectualObject.getFondsId();
            }
        }
        return null;
    }


    private List<Event> readEvents(Map<String, Agent> agentMap, List<EventComplexType> eventComplexTypeList) {
        List<Event> eventList = new ArrayList<>();
        for (EventComplexType eventComplexType : eventComplexTypeList) {
            Event event = new Event();
            if (eventComplexType.getEventIdentifier().getEventIdentifierType().getValue().equals("local")) {
                event.setLocalId(eventComplexType.getEventIdentifier().getEventIdentifierValue());
            }
            loadEventAgents(event, agentMap,eventComplexType.getLinkingAgentIdentifier());
            loadIdentifiers(event, eventComplexType.getLinkingObjectIdentifier());
            eventList.add(event);
        }
        return eventList;
    }

    private void loadIdentifiers(Event event, List<LinkingObjectIdentifierComplexType> linkingObjectIdentifierComplexTypeList) {
        for (LinkingObjectIdentifierComplexType linkingAgentIdentifierComplexType : linkingObjectIdentifierComplexTypeList) {
            String type = linkingAgentIdentifierComplexType.getLinkingObjectIdentifierType().getValue();
            String value = linkingAgentIdentifierComplexType.getLinkingObjectIdentifierValue();
            switch (type) {
                case "INGESTION_ID" -> event.setIngestionId(value);
                case "REFERENCE_NUMBER" -> event.setReferenceNumber(value);
                case "CZ_NAD_VNEZ" -> event.setNadChangeCode(value);
            }
        }
    }

    private void loadEventAgents(Event event, Map<String, Agent> agentMap, List<LinkingAgentIdentifierComplexType> linkingAgentIdentifierComplexTypeList) {
        for (LinkingAgentIdentifierComplexType linkingAgentIdentifierComplexType : linkingAgentIdentifierComplexTypeList) {
            String identifierType = linkingAgentIdentifierComplexType.getLinkingAgentIdentifierType().getValue();
            switch (identifierType) {
                case "local" -> {
                    if (linkingAgentIdentifierComplexType.getLinkingAgentRole().stream().anyMatch(r -> r.getValue().equals("SUBMITTER"))) {
                        event.setSubmitter(agentMap.get(linkingAgentIdentifierComplexType.getLinkingAgentIdentifierValue()));
                    }
                    if (linkingAgentIdentifierComplexType.getLinkingAgentRole().stream().anyMatch(r -> r.getValue().equals("CURATOR"))) {
                        event.setCurator(agentMap.get(linkingAgentIdentifierComplexType.getLinkingAgentIdentifierValue()));
                    }
                    if (linkingAgentIdentifierComplexType.getLinkingAgentRole().stream().anyMatch(r -> r.getValue().equals("ORIGINATOR"))) {
                        event.setOriginator(agentMap.get(linkingAgentIdentifierComplexType.getLinkingAgentIdentifierValue()));
                    }
                }
                case "INGESTION_ID" ->
                        event.setIngestionId(linkingAgentIdentifierComplexType.getLinkingAgentIdentifierValue());
                case "REFERENCE_NUMBER" ->
                        event.setReferenceNumber(linkingAgentIdentifierComplexType.getLinkingAgentIdentifierValue());
                case "CZ_NAD_VNEZ" ->
                        event.setNadChangeCode(linkingAgentIdentifierComplexType.getLinkingAgentIdentifierValue());
            }
        }
    }

    private List<PackageObject> readObjects(List<ObjectComplexType> object) {
        List<PackageObject> objectList = new ArrayList<>();
        for (ObjectComplexType objectComplexType : object) {
            if (objectComplexType instanceof IntellectualEntity intellectualEntity) {
                IntellectualObject intellectualObject = new IntellectualObject();
                loadIdentifiers(intellectualObject, intellectualEntity.getObjectIdentifier());
                loadSignificantProperties(intellectualObject, intellectualEntity.getSignificantProperties());
                objectList.add(intellectualObject);
            } else if (objectComplexType instanceof Representation representation) {
                RepresentationObject representationObject = new RepresentationObject();
                representationObject.setName(representation.getOriginalName().getValue());
                loadIdentifiers(representationObject, representation.getObjectIdentifier());
                objectList.add(representationObject);
            }
        }
        return objectList;
    }

    private void loadIdentifiers(RepresentationObject representationObject, List<ObjectIdentifierComplexType> objectIdentifier) {
        for (ObjectIdentifierComplexType objectIdentifierComplexType : objectIdentifier) {
            String type = objectIdentifierComplexType.getObjectIdentifierType().getValue();
            String value = objectIdentifierComplexType.getObjectIdentifierValue();
            if (type.equals("local")) {
                representationObject.setLocalId(value);
            }
        }
    }

    private void loadSignificantProperties(IntellectualObject intellectualObject, List<SignificantPropertiesComplexType> significantProperties) {
        for (SignificantPropertiesComplexType significantProperty : significantProperties) {
            StringPlusAuthority typeAuth = (StringPlusAuthority) significantProperty.getContent().get(0).getValue();
            String type = typeAuth.getValue();
            String value = (String) significantProperty.getContent().get(1).getValue();
            switch (type) {
                case "AIP_VERSION" -> intellectualObject.setAipVersion(value);
                case "AIP_SIZE" -> intellectualObject.setAipSize(value);
                case "INSTITUTION_ID" -> intellectualObject.setInstituitionId(value);
            }
        }
    }

    private void loadIdentifiers(IntellectualObject intellectualObject, List<ObjectIdentifierComplexType> objectIdentifier) {
        for (ObjectIdentifierComplexType objectIdentifierComplexType : objectIdentifier) {
            String type = objectIdentifierComplexType.getObjectIdentifierType().getValue();
            String value = objectIdentifierComplexType.getObjectIdentifierValue();
            switch (type) {
                case "local" -> intellectualObject.setLocalId(value);
                case "FONDS_ID" -> intellectualObject.setFondsId(value);
                case "AIP_ID" -> intellectualObject.setAipId(value);
            }
        }
    }

    private List<Agent> readAgents(List<AgentComplexType> agentComplexTypeList) {
        List<Agent> agentList = new ArrayList<>();
        for (AgentComplexType agentComplexType : agentComplexTypeList) {
            Agent agent = new Agent();
            loadIdentifiers(agent, agentComplexType.getAgentIdentifier());
            agent.setType(agentComplexType.getAgentType().getValue());
            agent.setNameList(agentComplexType.getAgentName().stream().map(StringPlusAuthority::getValue).toList());
            agentList.add(agent);
        }
        return agentList;
    }

    private void loadIdentifiers(Agent agent, List<AgentIdentifierComplexType> agentIdentifier) {
        for (AgentIdentifierComplexType agentIdentifierComplexType : agentIdentifier) {
            String type = agentIdentifierComplexType.getAgentIdentifierType().getValue();
            String value = agentIdentifierComplexType.getAgentIdentifierValue();
            switch (type) {
                case "INSTITUTION_ID" -> agent.setInstitutionId(value);
                case "CAM_ID" -> agent.setCamId(value);
                case "local" -> agent.setLocalIdentifier(value);
            }
        }
    }
}
