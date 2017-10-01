package cz.tacr.elza.deimport.parties;

import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.JAXBElement;

import org.apache.commons.lang3.StringUtils;

import cz.tacr.elza.core.data.CalendarType;
import cz.tacr.elza.core.data.PartyType;
import cz.tacr.elza.core.data.PartyTypeComplementTypes;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.deimport.DEImportException;
import cz.tacr.elza.deimport.aps.context.AccessPointsContext;
import cz.tacr.elza.deimport.aps.context.RecordImportInfo;
import cz.tacr.elza.deimport.context.ImportContext;
import cz.tacr.elza.deimport.context.StatefulIdHolder;
import cz.tacr.elza.deimport.parties.context.PartiesContext;
import cz.tacr.elza.deimport.parties.context.PartyImportInfo;
import cz.tacr.elza.deimport.parties.context.PartyNameWrapper;
import cz.tacr.elza.deimport.processor.ItemProcessor;
import cz.tacr.elza.deimport.timeinterval.TimeInterval;
import cz.tacr.elza.domain.ParComplementType;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyName;
import cz.tacr.elza.domain.ParPartyNameComplement;
import cz.tacr.elza.domain.ParPartyNameFormType;
import cz.tacr.elza.domain.ParPartyType;
import cz.tacr.elza.domain.ParUnitdate;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.schema.v2.NameComplement;
import cz.tacr.elza.schema.v2.NameComplements;
import cz.tacr.elza.schema.v2.Party;
import cz.tacr.elza.schema.v2.PartyName;
import cz.tacr.elza.schema.v2.TimeIntervalExt;

/**
 * Implementation is not thread-safe.
 */
public class PartyProcessor<P extends Party, E extends ParParty> implements ItemProcessor {

    protected final Class<E> partyClass;

    protected final PartiesContext partiesContext;

    protected final AccessPointsContext accessPointsContext;

    protected final StaticDataProvider staticData;

    private PartyType partyType;

    private RecordImportInfo recordInfo;

    public PartyProcessor(ImportContext context, Class<E> partyClass) {
        this.partyClass = partyClass;
        this.partiesContext = context.getParties();
        this.accessPointsContext = context.getAccessPoints();
        this.staticData = context.getStaticData();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void process(Object item) {
        JAXBElement<P> element = (JAXBElement<P>) item;
        prepareCachedReferences(element);
        P party = element.getValue();
        validateParty(party);
        E entity = createEntity(party);
        PartyImportInfo partyInfo = addParty(entity, party.getId());
        processSubEntities(party, partyInfo);
    }

    protected void prepareCachedReferences(JAXBElement<P> element) {
        // party type by element name
        switch (element.getName().getLocalPart()) {
            case "per":
                partyType = PartyType.PERSON;
                break;
            case "famy":
                partyType = PartyType.DYNASTY;
                break;
            case "pg":
                partyType = PartyType.GROUP_PARTY;
                break;
            case "evnt":
                partyType = PartyType.EVENT;
                break;
            default:
                throw new IllegalStateException("Uknown party element, name:" + element.getName());
        }
        // access point entry by id
        String apeId = element.getValue().getApe().getId();
        recordInfo = accessPointsContext.getRecordInfo(apeId);
        if (recordInfo == null) {
            throw new IllegalStateException("Party access point entry must be processed first, apeId:" + apeId);
        }
    }

    protected void validateParty(P item) {
        if (StringUtils.isEmpty(item.getId())) {
            throw new DEImportException("Party id is empty");
        }
        ParPartyType recordPartyType = recordInfo.getRegisterType().getPartyType();
        if (partyType.getEntity() != recordPartyType) {
            throw new DEImportException("Party type does not match with registry type, partyId:" + item.getId());
        }
    }

    protected E createEntity(P item) {
        E party;
        try {
            party = partyClass.newInstance();
        } catch (Exception e) {
            throw new SystemException("Failed to intialized no args constructor, entity: " + partyClass, e);
        }
        party.setCharacteristics(item.getChr());
        party.setHistory(item.getHst());
        party.setOriginator(true);
        party.setPartyType(recordInfo.getRegisterType().getPartyType());
        party.setSourceInformation(item.getSrc());
        return party;
    }

    protected PartyImportInfo addParty(E entity, String importId) {
        return partiesContext.addParty(entity, importId, recordInfo, partyType);
    }

    protected void processSubEntities(P item, PartyImportInfo partyInfo) {
        processPartyName(item.getName(), partyInfo, true);
        if (item.getVnms() != null) {
            for (PartyName name : item.getVnms().getVnm()) {
                processPartyName(name, partyInfo, false);
            }
        }
    }

    protected final StatefulIdHolder processTimeInterval(TimeIntervalExt interval, String partyImportId) {
        if (interval == null) {
            return null;
        }
        ParUnitdate unitDate = new ParUnitdate();
        unitDate.setNote(interval.getNote());
        unitDate.setTextDate(interval.getTf());
        TimeInterval it = null;
        try {
            it = TimeInterval.create(interval);
        } catch (IllegalArgumentException e) {
            throw new DEImportException(
                    "Conversion of time interval failed, partyId:" + partyImportId + ", detail:" + e.getMessage());
        }
        CalendarType calendarType = it.getCalendarType();
        if (calendarType == null) {
            throw new IllegalArgumentException("Calendar type for time interval not found, code:" + it.getCalendarType());
        }
        unitDate.setCalendarType(calendarType.getEntity());
        unitDate.setFormat(it.getFormat());
        unitDate.setValueFrom(it.getFormattedFrom());
        unitDate.setValueTo(it.getFormattedTo());
        unitDate.setValueFromEstimated(it.isFromEst());
        unitDate.setValueToEstimated(it.isToEst());
        return partiesContext.addUnitDate(unitDate, recordInfo);
    }

    private void processPartyName(PartyName partyName, PartyImportInfo partyInfo, boolean preferred) {
        if (StringUtils.isBlank(partyName.getMain())) {
            throw new DEImportException("Main part of party name not set, partyId:" + partyInfo.getImportId());
        }
        ParPartyNameFormType formType = null;
        if (StringUtils.isNotEmpty(partyName.getFt())) {
            formType = staticData.getPartyNameFormTypeByCode(partyName.getFt());
            if (formType == null) {
                throw new DEImportException("Form type of party name not found, partyId:" + partyInfo.getImportId()+", formType: "+partyName.getFt());
            }
        }
        ParPartyName name = new ParPartyName();
        name.setDegreeAfter(partyName.getDga());
        name.setDegreeBefore(partyName.getDgb());
        name.setMainPart(partyName.getMain());
        name.setNameFormType(formType);
        name.setNote(partyName.getNote());
        name.setOtherPart(partyName.getOth());
        PartyNameWrapper wrapper = partiesContext.addName(name, partyInfo, preferred);
        String partyImportId = partyInfo.getImportId();
        wrapper.setValidFrom(processTimeInterval(partyName.getVf(), partyImportId));
        wrapper.setValidTo(processTimeInterval(partyName.getVto(), partyImportId));
        processNameComplements(partyName.getNcs(), wrapper.getIdHolder(), partyImportId);
    }

    private void processNameComplements(NameComplements nameComplements,
                                        StatefulIdHolder partyNameIdHolder,
                                        String partyImportId) {
        if (nameComplements == null) {
            return;
        }
        Set<String> foundTypes = new HashSet<>();
        for (NameComplement nc : nameComplements.getNc()) {
            if (StringUtils.isBlank(nc.getV())) {
                throw new DEImportException("Value of party name complement is not set, partyId:" + partyImportId);
            }
            PartyTypeComplementTypes typeGroup = staticData.getComplementTypesByPartyTypeCode(partyType.getCode());
            String typeCode = nc.getCt();
            ParComplementType complementType = typeGroup.getTypeByCode(typeCode);
            if (complementType == null) {
                throw new DEImportException("Type of party name complement not found, partyId:" + partyImportId);
            }
            if (!foundTypes.add(typeCode) && !typeGroup.getRepeatableByCode(typeCode)) {
                throw new DEImportException("Type of party name complement is not repeatable, partyId:" + partyImportId);
            }
            ParPartyNameComplement complement = new ParPartyNameComplement();
            complement.setComplement(nc.getV());
            complement.setComplementType(complementType);
            partiesContext.addNameComplement(complement, partyNameIdHolder);
        }
    }
}
