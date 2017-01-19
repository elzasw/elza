package cz.tacr.elza.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cz.tacr.elza.ElzaTools;
import cz.tacr.elza.annotation.AuthMethod;
import cz.tacr.elza.annotation.AuthParam;
import cz.tacr.elza.domain.ArrCalendarType;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrNodeRegister;
import cz.tacr.elza.domain.ParComplementType;
import cz.tacr.elza.domain.ParCreator;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.ParInstitutionType;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyGroup;
import cz.tacr.elza.domain.ParPartyGroupIdentifier;
import cz.tacr.elza.domain.ParPartyName;
import cz.tacr.elza.domain.ParPartyNameComplement;
import cz.tacr.elza.domain.ParPartyNameFormType;
import cz.tacr.elza.domain.ParPartyType;
import cz.tacr.elza.domain.ParRelation;
import cz.tacr.elza.domain.ParRelationEntity;
import cz.tacr.elza.domain.ParRelationRoleType;
import cz.tacr.elza.domain.ParRelationType;
import cz.tacr.elza.domain.ParUnitdate;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.RegScope;
import cz.tacr.elza.domain.RegVariantRecord;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.repository.CalendarTypeRepository;
import cz.tacr.elza.repository.ComplementTypeRepository;
import cz.tacr.elza.repository.DataPartyRefRepository;
import cz.tacr.elza.repository.DataRecordRefRepository;
import cz.tacr.elza.repository.InstitutionRepository;
import cz.tacr.elza.repository.ItemSpecRegisterRepository;
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.NodeRegisterRepository;
import cz.tacr.elza.repository.PartyCreatorRepository;
import cz.tacr.elza.repository.PartyGroupIdentifierRepository;
import cz.tacr.elza.repository.PartyGroupRepository;
import cz.tacr.elza.repository.PartyNameComplementRepository;
import cz.tacr.elza.repository.PartyNameFormTypeRepository;
import cz.tacr.elza.repository.PartyNameRepository;
import cz.tacr.elza.repository.PartyRelationRepository;
import cz.tacr.elza.repository.PartyRepository;
import cz.tacr.elza.repository.PartyTypeRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.repository.RegisterTypeRepository;
import cz.tacr.elza.repository.RelationEntityRepository;
import cz.tacr.elza.repository.RelationRepository;
import cz.tacr.elza.repository.RelationRoleTypeRepository;
import cz.tacr.elza.repository.RelationTypeRepository;
import cz.tacr.elza.repository.UnitdateRepository;
import cz.tacr.elza.repository.VariantRecordRepository;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.ActionEvent;
import cz.tacr.elza.service.eventnotification.events.EventId;
import cz.tacr.elza.service.eventnotification.events.EventType;


/**
 * Servisní třídy pro osoby.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 21.12.2015
 */
@Service
public class PartyService {

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private RegRecordRepository recordRepository;

    @Autowired
    private CalendarTypeRepository calendarTypeRepository;

    @Autowired
    private PartyNameFormTypeRepository partyNameFormTypeRepository;

    @Autowired
    private PartyNameRepository partyNameRepository;

    @Autowired
    private UnitdateRepository unitdateRepository;

    @Autowired
    private PartyGroupRepository partyGroupRepository;

    @Autowired
    private RelationTypeRepository relationTypeRepository;

    @Autowired
    private RelationRepository relationRepository;

    @Autowired
    private RelationEntityRepository relationEntityRepository;

    @Autowired
    private RelationRoleTypeRepository relationRoleTypeRepository;

    @Autowired
    private DataPartyRefRepository dataPartyRefRepository;
    @Autowired
    private DataRecordRefRepository dataRecordRefRepository;

    @Autowired
    private NodeRegisterRepository nodeRegisterRepository;
    @Autowired
    private PartyNameComplementRepository partyNameComplementRepository;

    @Autowired
    private ComplementTypeRepository complementTypeRepository;

    @Autowired
    private PartyCreatorRepository partyCreatorRepository;
    @Autowired
    private PartyRelationRepository partyRelationRepository;
    @Autowired
    private PartyGroupIdentifierRepository partyGroupIdentifierRepository;
    @Autowired
    private PartyTypeRepository partyTypeRepository;

    @Autowired
    private RegistryService registryService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private VariantRecordRepository variantRecordRepository;

    @Autowired
    private GroovyScriptService groovyScriptService;

    @Autowired
    private EventNotificationService eventNotificationService;

    @Autowired
    private RegisterTypeRepository registerTypeRepository;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private ItemSpecRepository itemSpecRepository;

    @Autowired
    private ItemSpecRegisterRepository itemSpecRegisterRepository;

    @Autowired
    private UserService userService;

    /**
     * Najde osobu podle rejstříkového hesla.
     *
     * @param record rejstříkové heslo
     * @return osoba s daným rejstříkovým heslem nebo null
     */
    public ParParty findParPartyByRecord(final RegRecord record) {
        Assert.notNull(record);


        List<ParParty> recordParties = partyRepository.findParPartyByRecordId(record.getRecordId());
        return recordParties.isEmpty() ? null : recordParties.get(0);
    }

    /**
     * Najde id osob podle rejstříkových hesel.
     *
     * @param records seznam rejstříkových hesel
     *
     * @return mapa id rejstříku -> id osoby
     */
    public Map<Integer, Integer> findParPartyIdsByRecords(final Collection<RegRecord> records) {
        if (CollectionUtils.isEmpty(records)) {
            return Collections.emptyMap();
        }

        List<Object[]> recordIdsAndPartyIds = partyRepository.findRecordIdAndPartyIdByRecords(records);
        Map<Integer, Integer> recordIdPartyIdMap = new HashMap<>(recordIdsAndPartyIds.size());
        for (Object[] row : recordIdsAndPartyIds) {
            recordIdPartyIdMap.put((Integer) row[1], (Integer) row[0]);
        }

        return recordIdPartyIdMap;
    }


    /**
     * Osobu vyhledává podle hesla v rejstříku včetně variantních hesel.
     *
     * @param searchRecord hledaný řetězec, může být null
     * @param partyTypeId  typ záznamu
     * @param itemSpecId specifikace
     * @param firstResult  první vrácená osoba
     * @param maxResults   max počet vrácených osob
     * @param fund   AP, ze které se použijí třídy rejstříků
     */
    public List<ParParty> findPartyByTextAndType(final String searchRecord,
                                                 final Integer partyTypeId,
                                                 final Integer itemSpecId,
                                                 final Integer firstResult,
                                                 final Integer maxResults,
                                                 @Nullable final ArrFund fund) {
        UsrUser user = userService.getLoggedUser();
        boolean readAllScopes = userService.hasPermission(UsrPermission.Permission.REG_SCOPE_RD_ALL);
        Set<Integer> scopeIdsForRecord = registryService.getScopeIdsByFund(fund);

        Set<Integer> registerTypesIds = null;
        if (itemSpecId != null) {
            registerTypesIds = this.find(itemSpecId);
        }

        return partyRepository.findPartyByTextAndType(searchRecord, partyTypeId, registerTypesIds, firstResult, maxResults, scopeIdsForRecord, readAllScopes, user);
    }

    private Set<Integer> find(final Integer itemSpecId) {

        Set<Integer> registerTypeIds = new HashSet<>();
        if (itemSpecId != null) {
            RulItemSpec spec = itemSpecRepository.getOneCheckExist(itemSpecId);
            registerTypeIds.addAll(itemSpecRegisterRepository.findIdsByItemSpecId(spec));
        }
        return registerTypeRepository.findSubtreeIds(registerTypeIds);
    }

    /**
     * Vrátí počet osob vyhovující zadané frázi. Osobu vyhledává podle hesla v rejstříku včetně variantních hesel.
     * @param searchRecord hledaný řetězec, může být null
     * @param partyTypeId typ osoby
     * @param itemSpecId specifikace
     * @param fund   AP, ze které se použijí třídy rejstříků
     * @return
     */
    public long findPartyByTextAndTypeCount(final String searchRecord,
                                            final Integer partyTypeId,
                                            final Integer itemSpecId,
                                            @Nullable final ArrFund fund){
        UsrUser user = userService.getLoggedUser();
        boolean readAllScopes = userService.hasPermission(UsrPermission.Permission.REG_SCOPE_RD_ALL);
        Set<Integer> scopeIdsForRecord = registryService.getScopeIdsByFund(fund);


        Set<Integer> registerTypesIds = null;
        if (itemSpecId != null) {
            registerTypesIds = this.find(itemSpecId);
        }

        return partyRepository.findPartyByTextAndTypeCount(searchRecord, partyTypeId, registerTypesIds, scopeIdsForRecord, readAllScopes, user);
    }

    /**
     * Uložení osoby a všech navázaných dat, která musejí být při ukládání vyplněna.
     * @param newParty nová osoba s navázanými daty
     * @return uložená osoba
     */
    public ParParty saveParty(final ParParty newParty) {
        Assert.notNull(newParty);

        ParPartyType partyType = partyTypeRepository.findOne(newParty.getPartyType().getPartyTypeId());

        boolean isNewParty = newParty.getPartyId() == null;

        ParParty saveParty;
        if (isNewParty) {
            saveParty = newParty;
            saveParty.setPartyType(partyType);
            // Rejstříkové heslo pro založení
            synchRecord(newParty);
        } else {
            saveParty = partyRepository.findOne(newParty.getPartyId());
            Assert.notNull(saveParty);

            BeanUtils.copyProperties(newParty, saveParty, "partyGroupIdentifiers", "record",
                    "preferredName", "from", "to", "partyNames", "partyCreators", "relations");
        }

        //synchronizace jmen osoby
        ParPartyName newPrefferedName = newParty.getPreferredName();
        saveParty.setPreferredName(null);
        saveParty = partyRepository.save(saveParty);
        saveParty = synchPartyNames(saveParty, newPrefferedName, newParty.getPartyNames());
        //synchronizace skupinových identifikátorů
        if (ParPartyGroup.class.isAssignableFrom(saveParty.getClass())) {
            ParPartyGroup savePartyGroup = (ParPartyGroup) saveParty;
            ParPartyGroup partyGroup = (ParPartyGroup) newParty;
            synchPartyGroupIdentifiers(savePartyGroup,
                    partyGroup.getPartyGroupIdentifiers() == null ? Collections.emptyList() : partyGroup.getPartyGroupIdentifiers());
        }


        //synchronizace tvůrců osoby
        synchCreators(saveParty, newParty.getPartyCreators() == null ? Collections.emptyList() : newParty.getPartyCreators());

        //synchronizace rejstříkového hesla
        synchRecord(saveParty);

        ParParty result = partyRepository.save(saveParty);
        entityManager.flush();

        EventType eventType = isNewParty ? EventType.PARTY_CREATE : EventType.PARTY_UPDATE;
        eventNotificationService.publishEvent(EventFactory.createIdEvent(eventType, result.getPartyId()));

        return result;

    }

    /**
     * V groovy scriptu vytvoří rejstříkové heslo pro osobu a upraví jej v databázi.
     * @param party osoba
     */
    private void synchRecord(final ParParty party) {
        Assert.notNull(party);

        Assert.notNull(party.getRecord(), "Osoba nemá zadané rejstříkové heslo.");
        Assert.notNull(party.getRecord().getRegisterType(), "Není vyplněný typ rejstříkového hesla.");
        Assert.notNull(party.getRecord().getScope(), "Není nastavena třída rejstříkového hesla");
        Assert.notNull(party.getRecord().getScope().getScopeId(), "Není nastaveno id třídy rejstříkového hesla");

        if (party.getRelations() != null) {
            party.getRelations().sort(new ParRelation.ParRelationComparator());
        }

        //vytvoření rejstříkového hesla v groovy
        RegRecord recordFromGroovy = groovyScriptService.getRecordFromGroovy(party);
        List<RegVariantRecord> variantRecords = new ArrayList<>(recordFromGroovy.getVariantRecordList());

        //uložení hesla
        if (party.getPartyId() != null) {
            ParParty dbParty = partyRepository.findOne(party.getPartyId());
            recordFromGroovy.setRecordId(dbParty.getRecord().getRecordId());
            recordFromGroovy.setVersion(dbParty.getRecord().getVersion());
            recordFromGroovy.setUuid(dbParty.getRecord().getUuid());
        }
        recordFromGroovy.setExternalId(party.getRecord().getExternalId());
        recordFromGroovy.setExternalSystem(party.getRecord().getExternalSystem());
        RegRecord savedRecord = registryService.saveRecord(recordFromGroovy, true);
        party.setRecord(savedRecord);

        //smazání a uložení nových variantních hesel
        List<RegVariantRecord> oldVariants = variantRecordRepository.findByRegRecordId(savedRecord.getRecordId());
        variantRecordRepository.delete(oldVariants);

        for (RegVariantRecord variantRecord : variantRecords) {
            variantRecord.setRegRecord(savedRecord);
            registryService.saveVariantRecord(variantRecord);
        }
    }



    /**
     * Provede synchronizaci tvůrců osoby. CRUD.
     *
     * @param party       osoba
     * @param newCreators nový stav tvůrců
     */
    private void synchCreators(final ParParty party, final List<ParCreator> newCreators) {
        partyCreatorRepository.findByParty(party);
        Map<Integer, ParCreator> creatorMap = ElzaTools
                .createEntityMap(partyCreatorRepository.findByParty(party), c -> c.getCreatorParty().getPartyId());

        Set<ParCreator> remove = new HashSet<>(creatorMap.values());

        for (ParCreator newCreator : newCreators) {
            Assert.notNull(newCreator.getCreatorParty().getPartyId());
            ParCreator oldCreator = creatorMap.get(newCreator.getCreatorParty().getPartyId());

            if (oldCreator == null) {
                oldCreator = newCreator;
                ParParty creatorParty = partyRepository.findOne(newCreator.getCreatorParty().getPartyId());
                Assert.notNull(creatorParty);
                oldCreator.setCreatorParty(creatorParty);
            } else {
                remove.remove(oldCreator);
            }
            oldCreator.setParty(party);
            partyCreatorRepository.save(oldCreator);
        }

        partyCreatorRepository.delete(remove);
    }

    /**
     * Pokud se jedná o typ osoby group, dojde k synchronizaci identifikátorů osoby. CRUD.
     * @param partyGroup osoba
     * @param newPartyGroupIdentifiers nový stav identifikátorů
     */
    private void synchPartyGroupIdentifiers(final ParPartyGroup partyGroup,
                                            final Collection<ParPartyGroupIdentifier> newPartyGroupIdentifiers) {
        Assert.notNull(partyGroup);


        Map<Integer, ParPartyGroupIdentifier> dbIdentifiersMap = Collections.emptyMap();
        if(partyGroup.getPartyId() != null){
            dbIdentifiersMap = ElzaTools
                .createEntityMap(partyGroupIdentifierRepository.findByParty(partyGroup), ParPartyGroupIdentifier::getPartyGroupIdentifierId);
        }
        Set<ParPartyGroupIdentifier> removeIdentifiers = new HashSet<>(dbIdentifiersMap.values());

        Set<ParUnitdate> removeUnitdate = new HashSet<>();
        HashSet<ParPartyGroupIdentifier> saved = new HashSet<>();
        for (ParPartyGroupIdentifier newIdentifier : newPartyGroupIdentifiers) {
            ParPartyGroupIdentifier oldIdentifier = dbIdentifiersMap
                    .get(newIdentifier.getPartyGroupIdentifierId());

            if (oldIdentifier == null) {
                oldIdentifier = newIdentifier;
                oldIdentifier.setFrom(saveUnitDate(newIdentifier.getFrom()));
                oldIdentifier.setTo(saveUnitDate(newIdentifier.getTo()));
            } else {
                removeIdentifiers.remove(oldIdentifier);

                removeUnitdate.add(synchUnitdate(oldIdentifier, newIdentifier.getFrom(), ParPartyGroupIdentifier::getFrom, ParPartyGroupIdentifier::setFrom));
                removeUnitdate.add(synchUnitdate(oldIdentifier, newIdentifier.getTo(), ParPartyGroupIdentifier::getTo, ParPartyGroupIdentifier::setTo));
            }

            oldIdentifier.setPartyGroup(partyGroup);
            oldIdentifier.setSource(newIdentifier.getSource());
            oldIdentifier.setNote(newIdentifier.getNote());
            oldIdentifier.setIdentifier(newIdentifier.getIdentifier());

            ParPartyGroupIdentifier save = partyGroupIdentifierRepository.save(oldIdentifier);
            saved.add(save);
        }

        for (ParPartyGroupIdentifier removeIdentifier : removeIdentifiers) {
            deleteUnitDates(removeIdentifier.getFrom(), removeIdentifier.getTo());
        }

        removeUnitdate.remove(null);
        unitdateRepository.delete(removeUnitdate);
        partyGroupIdentifierRepository.delete(removeIdentifiers);
        partyGroup.setPartyGroupIdentifiers(new ArrayList<>(saved));
        partyRepository.save(partyGroup);
    }

    /**
     * Synchronizace stavu jmen osob. CRUD
     *  @param party       uložená osoba
     * @param newPrefferedName preferované jméno osoby
     * @param newPartyNames    seznam všech jmen osoby (obsahuje i preferované jméno)
     */
    private ParParty synchPartyNames(final ParParty party,
                                     final ParPartyName newPrefferedName,
                                     final List<ParPartyName> newPartyNames) {
        Map<Integer, ParPartyName> dbPartyNameMap = ElzaTools
                .createEntityMap(partyNameRepository.findByParty(party), ParPartyName::getPartyNameId);

        Set<ParPartyName> removePartyNames = new HashSet<>(dbPartyNameMap.values());
        Set<ParUnitdate> removeUnitdate = new HashSet<>();
        Set<ParPartyName> saved = new HashSet<>();
        for (ParPartyName newPartyName : newPartyNames) {
            ParPartyName oldPartyName = dbPartyNameMap.get(newPartyName.getPartyNameId());

            ParPartyNameFormType nameFormType = null;
            ParPartyNameFormType partyNameFormType = newPartyName.getNameFormType();
            if (partyNameFormType != null && partyNameFormType.getNameFormTypeId() != null) {
                nameFormType = partyNameFormTypeRepository.findOne(partyNameFormType.getNameFormTypeId());
            }

            if (oldPartyName == null) {
                oldPartyName = newPartyName;
                oldPartyName.setValidFrom(saveUnitDate(newPartyName.getValidFrom()));
                oldPartyName.setValidTo(saveUnitDate(newPartyName.getValidTo()));
            } else {
                removePartyNames.remove(oldPartyName);
                removeUnitdate.add(synchUnitdate(oldPartyName, newPartyName.getValidFrom(), ParPartyName::getValidFrom, ParPartyName::setValidFrom));
                removeUnitdate.add(synchUnitdate(oldPartyName, newPartyName.getValidTo(), ParPartyName::getValidTo, ParPartyName::setValidTo));
            }

            oldPartyName.setNameFormType(nameFormType);
            oldPartyName.setMainPart(newPartyName.getMainPart());
            oldPartyName.setOtherPart(newPartyName.getOtherPart());
            oldPartyName.setNote(newPartyName.getNote());
            oldPartyName.setDegreeBefore(newPartyName.getDegreeBefore());
            oldPartyName.setDegreeAfter(newPartyName.getDegreeAfter());
            oldPartyName.setParty(party);

            //nastavení preferovaného jména
            if (newPartyName == newPrefferedName) {
                party.setPreferredName(oldPartyName);
            }

            ParPartyName save = partyNameRepository.save(oldPartyName);
            save = synchComplementTypes(save, newPartyName.getPartyNameComplements() == null
                                               ? Collections.emptyList() : newPartyName.getPartyNameComplements());
            saved.add(save);
        }

        removeUnitdate.remove(null);
        unitdateRepository.delete(removeUnitdate);

        removePartyNames.forEach(this::deletePartyName);

        party.setPartyNames(new ArrayList<>(saved));
        return partyRepository.save(party);
    }

    /**
     * Syncoronizace doplňků jména osoby. CRUD
     * @param partyName jméno osoby
     * @param newComplementTypes stav doplňků jména
     */
    private ParPartyName synchComplementTypes(final ParPartyName partyName,
                                              final List<ParPartyNameComplement> newComplementTypes) {
        Map<Integer, ParPartyNameComplement> dbComplementMap = ElzaTools
                .createEntityMap(partyNameComplementRepository.findByPartyName(partyName),
                        ParPartyNameComplement::getPartyNameComplementId);

        Set<ParPartyNameComplement> removeComplements = new HashSet<>(dbComplementMap.values());
        Set<ParPartyNameComplement> saved = new HashSet<>();
        for (ParPartyNameComplement newComplementType : newComplementTypes) {
            ParPartyNameComplement oldComplementType = dbComplementMap
                    .get(newComplementType.getPartyNameComplementId());

            Assert.notNull(newComplementType.getComplementType());
            Assert.notNull(newComplementType.getComplementType().getComplementTypeId());

            ParComplementType complementType = complementTypeRepository
                    .findOne(newComplementType.getComplementType().getComplementTypeId());
            Assert.notNull(complementType);

            if (oldComplementType == null) {
                oldComplementType = newComplementType;
            } else {
                removeComplements.remove(oldComplementType);
                oldComplementType.setComplement(newComplementType.getComplement());
            }

            oldComplementType.setComplementType(complementType);
            oldComplementType.setPartyName(partyName);

            ParPartyNameComplement save = partyNameComplementRepository.save(oldComplementType);
            saved.add(save);
        }

        partyNameComplementRepository.delete(removeComplements);
        partyName.setPartyNameComplements(new ArrayList<>(saved));
        return partyNameRepository.save(partyName);
    }


    /**
     * Provede synchronizaci datace entity.
     *
     * @param entity      entita, na kterou je datace navázaná
     * @param newUnitdate nová datace
     * @param getter      getter datace z entity
     * @param setter      setter datace do entity
     * @param <T>         entita s datací
     * @return datace, která by měla být smazaná
     */
    public <T> ParUnitdate synchUnitdate(final T entity,
                                         final ParUnitdate newUnitdate,
                                         final Function<T, ParUnitdate> getter,
                                         final BiConsumer<T, ParUnitdate> setter) {

        ParUnitdate toRemove = null;
        ParUnitdate oldUnitDate = getter.apply(entity);

        Integer oldId = oldUnitDate == null ? null : oldUnitDate.getUnitdateId();
        Integer newId = newUnitdate == null ? null : newUnitdate.getUnitdateId();

        //pokud měl nastavenou dataci a nyní došlo k jejímu smazání, bude původní datace smazána
        if (oldId != null && (newId == null || !newId.equals(oldId))) {
            toRemove = oldUnitDate;
        }

        setter.accept(entity, saveUnitDate(newUnitdate));
        return toRemove;
    }


    /**
     * Smazání jména osoby.
     * @param partyName jméno ke smazání
     */
    public void deletePartyName(final ParPartyName partyName) {
        Assert.notNull(partyName);
        partyNameComplementRepository.delete(partyName.getPartyNameComplements());
        partyNameRepository.delete(partyName);
        deleteUnitDates(partyName.getValidFrom(), partyName.getValidTo());
    }

    /**
     * Provede smazání osoby a navázaných entit.
     * @param party osoba ke smazání
     */
    public void deleteParty(final ParParty party){
        Assert.notNull(party);

        checkPartyUsage(party);

        partyRelationRepository.findByParty(party).forEach(this::deleteRelation);

        List<ParPartyName> partyNames = new ArrayList<>(partyNameRepository.findByParty(party));

        party.setPreferredName(null);
        for (ParPartyName parPartyName : partyNames) {
            deletePartyName(parPartyName);
        }

        partyCreatorRepository.deleteByPartyBoth(party);



        ParPartyGroup partyGroup = partyGroupRepository.findOne(party.getPartyId());
        if (partyGroup != null) {
            partyGroupIdentifierRepository.findByParty(partyGroup).forEach((pg) -> {
                        ParUnitdate from = pg.getFrom();
                        ParUnitdate to = pg.getTo();
                        partyGroupIdentifierRepository.delete(pg);
                        deleteUnitDates(from, to);
                    }
            );
        }

        partyRepository.flush();

        eventNotificationService.publishEvent(new EventId(EventType.PARTY_DELETE, party.getPartyId()));
        partyRepository.delete(party);
        registryService.deleteRecord(party.getRecord(), false);
    }


    /**
     * Provede uložení vztahu a jeho vazeb.
     *
     * @param relationSource   zdrojový objakt vztahu
     * @param relationEntities seznam vazeb vztahu (Pokud je null, nedojde k aktualizaci vazeb)
     * @return uložený objekt
     */
    public ParRelation saveRelation(final ParRelation relationSource,
                                    @Nullable final Collection<ParRelationEntity> relationEntities) {


        Assert.notNull(relationSource.getRelationType());
        Assert.notNull(relationSource.getRelationType().getRelationTypeId());

        Set<ParUnitdate> unitdateRemove = new HashSet<>();

        ParRelationType relationType = relationTypeRepository
                .getOneCheckExist(relationSource.getRelationType().getRelationTypeId());


        ParRelation relation;
        if (relationSource.getRelationId() == null) {
            relation = relationSource;

            relation.setFrom(saveUnitDate(relationSource.getFrom()));
            relation.setTo(saveUnitDate(relationSource.getTo()));

        } else {
            relation = relationRepository.findOne(relationSource.getRelationId());
            relation.setVersion(relationSource.getVersion());


            Integer fromId = relation.getFrom() == null ? null : relation.getFrom().getUnitdateId();
            Integer toId = relation.getTo() == null ? null : relation.getTo().getUnitdateId();

            //pokud měl nastavenou dataci a nyní došlo k jejímu smazání, bude původní datace smazána
            if (fromId != null && (relationSource.getFrom() == null || relationSource.getFrom().getUnitdateId() == null
                    || !relationSource.getFrom().getUnitdateId().equals(fromId))) {
                unitdateRemove.add(relation.getFrom());
            }
            relation.setFrom(saveUnitDate(relationSource.getFrom()));

            //pokud měl nastavenou dataci a nyní došlo k jejímu smazání, bude původní datace smazána
            if (toId != null && (relationSource.getTo() == null || relationSource.getTo().getUnitdateId() == null
                    || !relationSource.getTo().getUnitdateId().equals(toId))) {
                unitdateRemove.add(relation.getTo());
            }

            relation.setTo(saveUnitDate(relationSource.getTo()));
            relation.setSource(relationSource.getSource());
        }


        ParParty party = partyRepository.findOne(relationSource.getParty().getPartyId());
        Assert.notNull(party);

        relation.setParty(party);
        relation.setRelationType(relationType);
        relation.setNote(relationSource.getNote());

        ParRelation result = relationRepository.save(relation);
        relationRepository.flush();

        saveDeleteRelationEntities(result, relationEntities);


        for (ParUnitdate unitdate : unitdateRemove) {
            unitdateRepository.delete(unitdate);
        }

        entityManager.flush(); //aktualizace seznamu vztahů v osobě
        synchRecord(party);

        return result;
    }

    public ParUnitdate saveUnitDate(@Nullable final ParUnitdate unitdateSource) {
        if (unitdateSource == null) {
            return null;
        }


        ParUnitdate unitdate;

        Integer calendarTypeId =
                unitdateSource.getCalendarType() == null || unitdateSource.getCalendarType().getCalendarTypeId() == null
                ? null : unitdateSource.getCalendarType().getCalendarTypeId();


        if (unitdateSource.getUnitdateId() == null) {
            unitdate = unitdateSource;

        } else {
            unitdate = unitdateRepository.findOne(unitdateSource.getUnitdateId());

            unitdate.setValueFrom(unitdateSource.getValueFrom());
            unitdate.setValueFromEstimated(unitdateSource.getValueFromEstimated());
            unitdate.setValueTo(unitdateSource.getValueTo());
            unitdate.setValueToEstimated(unitdateSource.getValueToEstimated());
            unitdate.setFormat(unitdateSource.getFormat());
            unitdate.setTextDate(unitdateSource.getTextDate());
        }

        if (calendarTypeId != null) {
            ArrCalendarType calendarType = calendarTypeRepository.findOne(unitdateSource.getCalendarType().getCalendarTypeId());
            unitdate.setCalendarType(calendarType);
        }

        return unitdateRepository.save(unitdate);
    }


    public void deleteRelation(final ParRelation relation) {

        ParParty party = relation.getParty();

        ParUnitdate from = relation.getFrom();
        ParUnitdate to = relation.getTo();

        List<ParRelationEntity> relationEntities = relationEntityRepository.findByRelation(relation);
        if (!relationEntities.isEmpty()) {
            relationEntityRepository.delete(relationEntities);
        }

        relationRepository.delete(relation);

        deleteUnitDates(from, to);
        entityManager.flush();      //aktualizace seznamu vztahů

        synchRecord(party);
    }


    /**
     * Provede nastavení stavu vazeb u vztahu. Dojde k vytvoření, aktualizaci a smazání přebytečných vazeb.
     *
     * @param relation            vztah
     * @param newRelationEntities seznam vazeb vztahu (pokud je null, nedojde k žádné změně)
     * @return nový seznam vazeb
     */
    private List<ParRelationEntity> saveDeleteRelationEntities(final ParRelation relation,
                                                               @Nullable final Collection<ParRelationEntity> newRelationEntities) {
        if (newRelationEntities == null) {
            return Collections.emptyList();
        }

        Map<Integer, ParRelationEntity> relationEntityMap = ElzaTools
                .createEntityMap(relationEntityRepository.findByRelation(relation), (r) -> r.getRelationEntityId());

        Set<ParRelationEntity> toRemoveEntities = new HashSet<>(relationEntityMap.values());

        List<ParRelationEntity> result = new ArrayList<>(newRelationEntities.size());

        for (ParRelationEntity newRelationEntity : newRelationEntities) {
            ParRelationEntity saveEntity;
            if (newRelationEntity.getRelationEntityId() == null) {
                saveEntity = newRelationEntity;

            } else {
                saveEntity = relationEntityMap.get(newRelationEntity.getRelationEntityId());
                Assert.notNull(saveEntity,
                        "Nebyla nalezena entita vztahu s id " + newRelationEntity.getRelationEntityId());
                toRemoveEntities.remove(saveEntity);
            }


            Assert.notNull(newRelationEntity.getRoleType());
            Assert.notNull(newRelationEntity.getRoleType().getRoleTypeId());
            ParRelationRoleType relationRoleType = relationRoleTypeRepository
                    .findOne(newRelationEntity.getRoleType().getRoleTypeId());
            saveEntity.setRoleType(relationRoleType);

            Assert.notNull(newRelationEntity.getRecord());
            Assert.notNull(newRelationEntity.getRecord().getRecordId());
            RegRecord record = recordRepository.findOne(newRelationEntity.getRecord().getRecordId());
            saveEntity.setRecord(record);

            saveEntity.setRelation(relation);
            checkRelationEntitySave(saveEntity);

            result.add(relationEntityRepository.save(saveEntity));
        }

        if (!toRemoveEntities.isEmpty()) {
            relationEntityRepository.delete(toRemoveEntities);
        }

        return result;
    }


    /**
     * Zvaliduje možnost vytvoření navázené entity vztahu k osobě.
     *
     * @param relationEntity navázaná entita
     */
    private void checkRelationEntitySave(final ParRelationEntity relationEntity) {
        Assert.notNull(relationEntity);
        Assert.notNull(relationEntity.getRoleType());
        Assert.notNull(relationEntity.getRelation());
        Assert.notNull(relationEntity.getRecord());

        //typ role entity odpovídající typu vztahu dle par_relation_type_role_type
        ParRelationRoleType roleType = relationEntity.getRoleType();
        List<ParRelationType> possibleRelationTypes = relationTypeRepository.findByRelationRoleType(roleType);
        if (!possibleRelationTypes.contains(relationEntity.getRelation().getRelationType())) {
            throw new IllegalArgumentException(
                    "Typ role entity " + roleType.getName() + " nespadá do typu vztahu " + relationEntity.getRelation()
                            .getRelationType().getName());
        }


        //navázaná entita stejné scope jako osoba sama
        RegScope entityScope = relationEntity.getRecord().getScope();
        if (!relationEntity.getRelation().getParty().getRecord().getScope().equals(entityScope)) {
            throw new IllegalArgumentException(
                    "Navázaná entita musí mít stejnou třídu rejstříkového hesla jako osoba, ke které entitu navazujeme.");
        }

        //navázaná entita povoleného typu rejstříku dle par_registry_role (mělo by to ideálně i dědit)
        RegRegisterType entityRegisterType = relationEntity.getRecord().getRegisterType();
        Set<Integer> registerTypeIds = registerTypeRepository.findByRelationRoleType(roleType)
                .stream().map(RegRegisterType::getRegisterTypeId).collect(Collectors.toSet());
        registerTypeIds = registerTypeRepository.findSubtreeIds(registerTypeIds);
        if (!registerTypeIds.contains(entityRegisterType.getRegisterTypeId())) {
            throw new IllegalArgumentException(
                    "Navázaná entita musí mít typ rejstříku nebo podtyp, který je navázaný na roli entity.");
        }

    }

    /**
     * Prověří existenci vazeb na osobu. Pokud existují, vyhodí příslušnou výjimku, nelze mazat.
     * @param party osoba
     */
    private void checkPartyUsage(final ParParty party) {
        // vazby ( arr_node_register, ArrDataRecordRef, ArrDataPartyRef),
        Long pocet = dataPartyRefRepository.getCountByParty(party);
        if (pocet > 0) {
            throw new IllegalStateException("Nalezeno použití party v tabulce ArrDataPartyRef.");
        }

        List<ArrDataRecordRef> dataRecordRefList = dataRecordRefRepository.findByRecord(party.getRecord());
        if (CollectionUtils.isNotEmpty(dataRecordRefList)) {
            throw new IllegalStateException("Nalezeno použití hesla v tabulce ArrDataRecordRef.");
        }

        List<ArrNodeRegister> nodeRegisterList = nodeRegisterRepository.findByRecordId(party.getRecord());
        if (CollectionUtils.isNotEmpty(nodeRegisterList)) {
            throw new IllegalStateException("Nalezeno použití hesla v tabulce ArrDataRecordRef.");
        }
    }

    /**
     * Promazání dvojice datumů od kterékoliv entity.
     * @param from  od
     * @param to    do
     */
    private void deleteUnitDates(final ParUnitdate from, final ParUnitdate to) {
        if (from != null) {
            unitdateRepository.delete(from);
        }
        if (to != null) {
            unitdateRepository.delete(to);
        }
    }

    /**
     * Vytvoří instituci.
     *
     * @param internalCode kód
     * @param institutionType typ instituce
     * @param party osoba
     *
     * @return neuložená instituce
     */
    public ParInstitution createInstitution(final String internalCode, final ParInstitutionType institutionType, final ParParty party) {
        ParInstitution institution = new ParInstitution();
        institution.setInternalCode(internalCode);
        institution.setInstitutionType(institutionType);
        institution.setParty(party);

        return institution;
    }

    /**
     * Uloží instituci.
     *
     * @param institution instituce
     *
     * @return uložená instituce
     */
    public ParInstitution saveInstitution(final ParInstitution institution) {
        Assert.notNull(institution);
        eventNotificationService.publishEvent(new ActionEvent(EventType.INSTITUTION_CHANGE));
        return institutionRepository.save(institution);
    }

    @AuthMethod(permission = {UsrPermission.Permission.REG_SCOPE_RD_ALL, UsrPermission.Permission.REG_SCOPE_RD})
    public ParParty getParty(@AuthParam(type = AuthParam.Type.PARTY) final Integer partyId) {
        Assert.notNull(partyId);
        return partyRepository.findOne(partyId);
    }
}
