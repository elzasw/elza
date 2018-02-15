package cz.tacr.elza.dataexchange.input.parties;

import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.JAXBElement;

import org.apache.commons.lang3.StringUtils;

import cz.tacr.elza.core.data.CalendarType;
import cz.tacr.elza.core.data.PartyType;
import cz.tacr.elza.core.data.PartyTypeComplementTypes;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.common.timeinterval.TimeInterval;
import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.aps.context.AccessPointInfo;
import cz.tacr.elza.dataexchange.input.aps.context.AccessPointsContext;
import cz.tacr.elza.dataexchange.input.context.EntityIdHolder;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.parties.context.PartiesContext;
import cz.tacr.elza.dataexchange.input.parties.context.PartyInfo;
import cz.tacr.elza.dataexchange.input.parties.context.PartyNameWrapper;
import cz.tacr.elza.dataexchange.input.parties.context.PartyRelatedIdHolder;
import cz.tacr.elza.dataexchange.input.processor.ItemProcessor;
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

    private AccessPointInfo apInfo;

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
        PartyInfo partyInfo = addParty(entity, party.getId());
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
        String entryId = element.getValue().getApe().getId();
        apInfo = accessPointsContext.getAccessPointInfo(entryId);
        if (apInfo == null) {
            throw new IllegalStateException("Party access point entry must be processed first, entryId:" + entryId);
        }
    }

    protected void validateParty(P item) {
        if (StringUtils.isEmpty(item.getId())) {
            throw new DEImportException("Party id is empty");
        }
        ParPartyType recordPartyType = apInfo.getApType().getPartyType();
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
        party.setPartyType(apInfo.getApType().getPartyType());
        party.setSourceInformation(item.getSrc());
        return party;
    }

    protected PartyInfo addParty(E entity, String importId) {
        return partiesContext.addParty(entity, importId, apInfo, partyType);
    }

    protected void processSubEntities(P item, PartyInfo partyInfo) {
        processPartyName(item.getName(), partyInfo, true);
        if (item.getVnms() != null) {
            for (PartyName name : item.getVnms().getVnm()) {
                processPartyName(name, partyInfo, false);
            }
        }
    }

    protected final EntityIdHolder<ParUnitdate> processTimeInterval(TimeIntervalExt interval, PartyInfo partyInfo) {
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
                    "Conversion of time interval failed, partyId:" + partyInfo.getImportId() + ", detail:" + e.getMessage());
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
        return partiesContext.addUnitDate(unitDate, partyInfo);
    }

    private void processPartyName(PartyName partyName, PartyInfo partyInfo, boolean preferred) {
        if (StringUtils.isBlank(partyName.getMain())) {
            throw new DEImportException("Main part of party name not set, partyId:" + partyInfo.getImportId());
        }
        ParPartyNameFormType formType = null;
        if (StringUtils.isNotEmpty(partyName.getFt())) {
            formType = staticData.getPartyNameFormTypeByCode(partyName.getFt());
            if (formType == null) {
                throw new DEImportException("Form type of party name not found, partyId:" + partyInfo.getImportId() + ", formType: "
                        + partyName.getFt());
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
        wrapper.setValidFrom(processTimeInterval(partyName.getVf(), partyInfo));
        wrapper.setValidTo(processTimeInterval(partyName.getVto(), partyInfo));
        processNameComplements(partyName.getNcs(), wrapper.getIdHolder());
    }

    private void processNameComplements(NameComplements nameComplements, PartyRelatedIdHolder<ParPartyName> partyNameIdHolder) {
        if (nameComplements == null) {
            return;
        }
        String partyImportId = partyNameIdHolder.getPartyInfo().getImportId();
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
