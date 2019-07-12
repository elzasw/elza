package cz.tacr.elza.dataexchange.input.parties;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.xml.bind.JAXBElement;

import org.apache.commons.lang3.StringUtils;

import cz.tacr.elza.core.data.CalendarType;
import cz.tacr.elza.core.data.PartyType;
import cz.tacr.elza.core.data.PartyTypeCmplType;
import cz.tacr.elza.core.data.PartyTypeCmplTypes;
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
import cz.tacr.elza.dataexchange.input.reader.ItemProcessor;
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
import cz.tacr.elza.schema.v2.PartyNames;
import cz.tacr.elza.schema.v2.TimeIntervalExt;

/**
 * Implementation is not thread-safe.
 */
public class PartyProcessor<P extends Party, E extends ParParty> implements ItemProcessor {

    protected final Class<E> partyClass;

    protected final PartiesContext partiesContext;

    protected final AccessPointsContext apContext;

    protected final StaticDataProvider staticData;

    protected P party;

    protected PartyInfo info;

    public PartyProcessor(ImportContext context, Class<E> partyClass) {
        this.partyClass = partyClass;
        this.partiesContext = context.getParties();
        this.apContext = context.getAccessPoints();
        this.staticData = context.getStaticData();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void process(Object item) {
        processInternal((JAXBElement<P>) item);
        info.onProcessed();
    }

    protected void processInternal(JAXBElement<P> element) {
        processParty(element);
        processNames(party.getNms());
    }

    private void processParty(JAXBElement<P> element) {
        party = element.getValue();
        PartyType type = resolvePartyType(element);
        AccessPointInfo apInfo = apContext.getApInfo(party.getApe().getId());
        E entity = createParty(party, type, apInfo);
        info = partiesContext.addParty(entity, party.getId(), apInfo, type);
    }

    protected E createParty(P party, PartyType type, AccessPointInfo apInfo) {
        if (StringUtils.isEmpty(party.getId())) {
            throw new DEImportException("Party id is empty");
        }
        ParPartyType apPartyType = apInfo.getState().getApType().getPartyType();
        // references should match (both initialized from static data)
        if (apPartyType != type.getEntity()) {
            throw new DEImportException("Party type does not match with AP type, partyId=" + party.getId());
        }
        // invoke no-arg constructor (should be implemented for Hibernate)
        E entity;
        try {
            entity = partyClass.newInstance();
        } catch (Exception e) {
            throw new SystemException("Failed to intialized no arg constructor, entity: " + partyClass, e);
        }
        entity.setCharacteristics(party.getChr());
        entity.setHistory(party.getHst());
        entity.setOriginator(true);
        entity.setPartyType(apPartyType);
        entity.setSourceInformation(party.getSrc());
        return entity;
    }

    private void processNames(PartyNames names) {
        if (names == null || names.getNm().isEmpty()) {
            throw new DEImportException("Preferred party name not found, partyId=" + party.getId());
        }
        Iterator<PartyName> it = names.getNm().iterator();
        processName(it.next(), true);
        while (it.hasNext()) {
            processName(it.next(), false);
        }
    }

    private void processName(PartyName name, boolean preferred) {
        if (StringUtils.isEmpty(name.getMain())) {
            throw new DEImportException("Main part of party name not set, partyId=" + party.getId());
        }
        ParPartyNameFormType formType = null;
        if (StringUtils.isNotEmpty(name.getFt())) {
            formType = staticData.getPartyNameFormTypeByCode(name.getFt());
            if (formType == null) {
                throw new DEImportException(
                        "Form type of party name not found, partyId=" + party.getId() + ", name=" + name.getFt());
            }
        }
        ParPartyName entity = new ParPartyName();
        entity.setDegreeAfter(name.getDga());
        entity.setDegreeBefore(name.getDgb());
        entity.setMainPart(name.getMain());
        entity.setNameFormType(formType);
        entity.setNote(name.getNote());
        entity.setOtherPart(name.getOth());
        PartyNameWrapper wrapper = partiesContext.addName(entity, info, preferred);

        wrapper.setValidFrom(processTimeInterval(name.getVf()));
        wrapper.setValidTo(processTimeInterval(name.getVto()));

        processNameCmpls(name.getNcs(), wrapper.getIdHolder());

    }

    protected final EntityIdHolder<ParUnitdate> processTimeInterval(TimeIntervalExt strInterval) {
        if (strInterval == null) {
            return null;
        }
        ParUnitdate entity = new ParUnitdate();
        entity.setNote(strInterval.getNote());
        entity.setTextDate(strInterval.getTf());
        TimeInterval interval = null;
        try {
            interval = TimeInterval.create(strInterval);
        } catch (IllegalArgumentException e) {
            throw new DEImportException(
                    "Conversion of time interval failed, partyId=" + party.getId() + ", detail: " + e.getMessage());
        }
        CalendarType ct = interval.getCalendarType();
        if (ct == null) {
            throw new IllegalArgumentException("Calendar type for time interval not found, code=" + party.getId());
        }
        entity.setCalendarType(ct.getEntity());
        entity.setFormat(interval.getFormat());
        entity.setValueFrom(interval.getFormattedFrom());
        entity.setValueTo(interval.getFormattedTo());
        entity.setValueFromEstimated(interval.isFromEst());
        entity.setValueToEstimated(interval.isToEst());
        return partiesContext.addUnitDate(entity, info);
    }

    private void processNameCmpls(NameComplements nameCmpls, EntityIdHolder<ParPartyName> nameIdHolder) {
        if (nameCmpls == null || nameCmpls.getNc().isEmpty()) {
            return;
        }
        Set<String> foundTypes = new HashSet<>();
        for (NameComplement cmpl : nameCmpls.getNc()) {
            if (StringUtils.isBlank(cmpl.getV())) {
                throw new DEImportException("Value of party name complement is not set, partyId=" + party.getId());
            }
            PartyTypeCmplTypes types = staticData.getCmplTypesByPartyTypeCode(info.getPartyType().getCode());
            PartyTypeCmplType type = types.getCmplTypeByCode(cmpl.getCt());
            if (type == null) {
                throw new DEImportException(
                        "Type of party name complement not found, partyId=" + party.getId() + ", code=" + cmpl.getCt());
            }
            if (!foundTypes.add(type.getCode()) && !type.isRepeatable()) {
                throw new DEImportException("Type of party name complement is not repeatable, partyId=" + party.getId()
                        + ", code=" + type.getCode());
            }
            ParPartyNameComplement entity = new ParPartyNameComplement();
            entity.setComplement(cmpl.getV());
            entity.setComplementType(type.getEntity());
            partiesContext.addNameComplement(entity, nameIdHolder, info);
        }
    }

    public static PartyType resolvePartyType(JAXBElement<? extends Party> element) {
        String localName = element.getName().getLocalPart();
        switch (localName) {
        case "per":
            return PartyType.PERSON;
        case "famy":
            return PartyType.DYNASTY;
        case "pg":
            return PartyType.GROUP_PARTY;
        case "evnt":
            return PartyType.EVENT;
        default:
            throw new IllegalStateException("Uknown party element, name:" + element.getName());
        }
    }
}
