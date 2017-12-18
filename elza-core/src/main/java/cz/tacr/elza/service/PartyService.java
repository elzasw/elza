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

import cz.tacr.elza.domain.*;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.*;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cz.tacr.elza.ElzaTools;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.core.security.AuthParam;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.Level;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.RegistryCode;
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
    private RegVariantRecordRepository variantRecordRepository;

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
    private DescriptionItemService descriptionItemService;

    @Autowired
    private ArrangementService arrangementService;

    @Autowired
    private DescItemRepository descItemRepository;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private ScopeRepository scopeRepository;

    /**
     * Najde osobu podle rejstříkového hesla.
     *
     * @param record rejstříkové heslo
     * @return osoba s daným rejstříkovým heslem nebo null
     */
    public ParParty findParPartyByRecord(final RegRecord record) {
        Assert.notNull(record, "Rejstříkové heslo musí být vyplněno");


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
     * @param scopeId scope, pokud je vyplněno hledají se osoby pouze s tímto scope
     */
    public List<ParParty> findPartyByTextAndType(final String searchRecord,
                                                 final Integer partyTypeId,
                                                 final Integer itemSpecId,
                                                 final Integer firstResult,
                                                 final Integer maxResults,
                                                 @Nullable final ArrFund fund,
                                                 @Nullable final Integer scopeId) {
        Set<Integer> scopeIdsForSearch = registryService.getScopeIdsForSearch(fund, scopeId);

        Set<Integer> registerTypesIds = null;
        if (itemSpecId != null) {
            registerTypesIds = this.find(itemSpecId);
        }

        return partyRepository.findPartyByTextAndType(searchRecord, partyTypeId, registerTypesIds, firstResult,
                maxResults, scopeIdsForSearch);
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
     *
     * @param searchRecord hledaný řetězec, může být null
     * @param partyTypeId typ osoby
     * @param itemSpecId specifikace
     * @param fund   AP, ze které se použijí třídy rejstříků
     * @param scopeId scope, pokud je vyplněno hledají se osoby pouze s tímto scope
     *
     * @return
     */
    public long findPartyByTextAndTypeCount(final String searchRecord,
                                            final Integer partyTypeId,
                                            final Integer itemSpecId,
                                            @Nullable final ArrFund fund,
                                            @Nullable final Integer scopeId){
        Set<Integer> scopeIdsForSearch = registryService.getScopeIdsForSearch(fund, scopeId);


        Set<Integer> registerTypesIds = null;
        if (itemSpecId != null) {
            registerTypesIds = this.find(itemSpecId);
        }

        return partyRepository.findPartyByTextAndTypeCount(searchRecord, partyTypeId, registerTypesIds,
                scopeIdsForSearch);
    }

    /**
     * Uložení osoby a všech navázaných dat, která musejí být při ukládání vyplněna.
     * @param newParty nová osoba s navázanými daty
     * @return uložená osoba
     */
    public ParParty saveParty(final ParParty newParty) {
        Assert.notNull(newParty, "Osoba musí být vyplněna");

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
            Assert.notNull(saveParty, "Osoba neexistuje");

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
        Assert.notNull(party, "Osoba nesmí být prázdná");

        Assert.notNull(party.getRecord(), "Osoba nemá zadané rejstříkové heslo.");
        Assert.notNull(party.getRecord().getRegisterType(), "Není vyplněný typ rejstříkového hesla.");
        Assert.notNull(party.getRecord().getScope(), "Není nastavena třída rejstříkového hesla");
        Assert.notNull(party.getRecord().getScope().getScopeId(), "Není nastaveno id třídy rejstříkového hesla");

        if (party.getRelations() != null) {
            party.getRelations().sort(new ParRelation.ParRelationComparator());
        }

        //vytvoření rejstříkového hesla v groovy
        List<ParComplementType> complementTypes = complementTypeRepository.findByPartyType(party.getPartyType());
        RegRecord recordFromGroovy = groovyScriptService.getRecordFromGroovy(party, complementTypes);
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
            Assert.notNull(newCreator.getCreatorParty().getPartyId(), "Nesmí být nulové");
            ParCreator oldCreator = creatorMap.get(newCreator.getCreatorParty().getPartyId());

            if (oldCreator == null) {
                oldCreator = newCreator;
                ParParty creatorParty = partyRepository.findOne(newCreator.getCreatorParty().getPartyId());
                Assert.notNull(creatorParty, "Osoba neexistuje");
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
        Assert.notNull(partyGroup, "Musí být vyplněno");


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

            Assert.notNull(newComplementType.getComplementType(), "Musí být nenulové");
            Assert.notNull(newComplementType.getComplementType().getComplementTypeId(), "Musí být nenulové");

            ParComplementType complementType = complementTypeRepository
                    .findOne(newComplementType.getComplementType().getComplementTypeId());
            Assert.notNull(complementType, "Musí být nenulové");

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
        Assert.notNull(partyName, "Musí být nenulové");
        partyNameComplementRepository.delete(partyName.getPartyNameComplements());
        partyNameRepository.delete(partyName);
        deleteUnitDates(partyName.getValidFrom(), partyName.getValidTo());
    }

    /**
     * Provede smazání osoby a navázaných entit.
     * @param party osoba ke smazání
     */
    public void deleteParty(final ParParty party){
        Assert.notNull(party, "Osoba nesmí být prázdná");

        checkUsagesBeforeDelete(party);

        if (canBeDeleted(party)) {

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
        } else {
            final RegRecord record = party.getRecord();
            record.setInvalid(true);
            registryService.saveRecord(record, true);
        }
    }

    private boolean canBeDeleted(ParParty party) {
        return registryService.canBeDeleted(party.getRecord()) &&
                CollectionUtils.isEmpty(dataPartyRefRepository.findByParty(party)) &&
                institutionRepository.findByParty(party) == null &&
                CollectionUtils.isEmpty(userRepository.findByParty(party)) &&
                CollectionUtils.isEmpty(partyCreatorRepository.findByCreatorParty(party));
    }

    private void checkUsagesBeforeDelete(ParParty party) {
        // rejstřík AS nebo arch. popis v otevřené verzi.(arr_node_register nebo arr_data_party_ref nebo arr_data_record_ref)

        // arr_node_register
        List<ArrNodeRegister> nodeRegisters = nodeRegisterRepository.findByRecordAndDeleteChangeIsNull(party.getRecord());
        if (CollectionUtils.isNotEmpty(nodeRegisters)) {
            throw new BusinessException("Nelze smazat/zneplatnit osobu, která má přiřazení rejstříkového hesla k jednotce archivního popisu.", RegistryCode.EXIST_FOREIGN_DATA)
                    .set("partyId", party.getPartyId())
                    .set("nodeRegisterIds", nodeRegisters.stream().map(ArrNodeRegister::getNodeRegisterId).collect(Collectors.toList()))
                    .set("nodeIds", nodeRegisters.stream().map(ArrNodeRegister::getNodeId).collect(Collectors.toList()));
        }
        // arr_data_party_ref
        List<ArrDescItem> arrPartyItems = descItemRepository.findArrItemByParty(party);
        if (CollectionUtils.isNotEmpty(arrPartyItems)) {
            throw new BusinessException("Nelze smazat/zneplatnit osobu, která má hodnotu osoby v jednotce archivního popisu.", RegistryCode.EXIST_FOREIGN_DATA)
                    .set("partyId", party.getPartyId())
                    .set("arrItems", arrPartyItems.stream().map(ArrItem::getItemId).collect(Collectors.toList()))
                    .set("fundIds", arrPartyItems.stream().map(ArrItem::getFundId).collect(Collectors.toList()));
        }
        // arr_data_record_ref
        List<ArrDescItem> arrRecordItems = descItemRepository.findArrItemByRecord(party.getRecord());
        if (CollectionUtils.isNotEmpty(arrRecordItems)) {
            throw new BusinessException("Nelze smazat/zneplatnit osobu, která má hodnotu osoby v jednotce archivního popisu.", RegistryCode.EXIST_FOREIGN_DATA)
                    .set("partyId", party.getPartyId())
                    .set("arrItems", arrPartyItems.stream().map(ArrItem::getItemId).collect(Collectors.toList()))
                    .set("fundIds", arrPartyItems.stream().map(ArrItem::getFundId).collect(Collectors.toList()));
        }
        // na uživatele (usr_user)
        List<UsrUser> users = userRepository.findByParty(party);
        if (CollectionUtils.isNotEmpty(users)) {
            throw new BusinessException("Nelze smazat/zneplatnit osobu, která má vazbu na uživatele.", RegistryCode.EXIST_FOREIGN_DATA)
                    .set("partyId", party.getPartyId())
                    .set("userIds", users.stream().map(UsrUser::getUserId).collect(Collectors.toList()));
        }
        // instituce (par_institution)
        ParInstitution institution = institutionRepository.findByParty(party);
        if (institution != null) {
            throw new BusinessException("Nelze smazat/zneplatnit osobu, která má vazbu na instituci.", RegistryCode.EXIST_FOREIGN_DATA)
                    .set("partyId", party.getPartyId())
                    .set("institutionId", institution.getInstitutionId());
        }
        // tvůrce osoby par_creator
        List<ParCreator> creators = partyCreatorRepository.findByCreatorParty(party);
        if (CollectionUtils.isNotEmpty(creators)) {
            throw new BusinessException("Nelze smazat/zneplatnit osobu, která je zakladatelem jiných osob.", RegistryCode.EXIST_FOREIGN_DATA)
                    .set("partyId", party.getPartyId())
                    .set("creatorsIds", creators.stream().map(ParCreator::getCreatorId).collect(Collectors.toList()))
                    .set("partyIds", creators.stream().map(ParCreator::getParty).map(ParParty::getPartyId).collect(Collectors.toList()));
        }
        // vztah osoby par_relation_entity
        List<ParRelationEntity> relationEntities = relationEntityRepository.findByRecord(party.getRecord());
        if (CollectionUtils.isNotEmpty(relationEntities)) {
            throw new BusinessException("Nelze smazat/zneplatnit osobu na kterou mají vazbu jiné osoby v relacích.", RegistryCode.EXIST_FOREIGN_DATA)
                    .set("partyId", party.getPartyId())
                    .set("relationEntities", relationEntities.stream().map(ParRelationEntity::getRelationEntityId).collect(Collectors.toList()))
                    .set("partyIds", relationEntities.stream().map(ParRelationEntity::getRelation).map(ParRelation::getParty).map(ParParty::getPartyId).collect(Collectors.toList()));
        }
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


        Assert.notNull(relationSource.getRelationType(), "Musí být nenulové");
        Assert.notNull(relationSource.getRelationType().getRelationTypeId(), "Musí být nenulové");

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
        Assert.notNull(party, "Osoba nesmí být prázdná");

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

    public void deleteRelationAndSync(final ParRelation relation) {
        ParParty parParty = deleteRelation(relation);
        synchRecord(parParty);
    }

    private ParParty deleteRelation(final ParRelation relation) {

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

        return party;
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


            Assert.notNull(newRelationEntity.getRoleType(), "Musí být nenulové");
            Assert.notNull(newRelationEntity.getRoleType().getRoleTypeId(), "Musí být nenulové");
            ParRelationRoleType relationRoleType = relationRoleTypeRepository
                    .findOne(newRelationEntity.getRoleType().getRoleTypeId());
            saveEntity.setRoleType(relationRoleType);

            Assert.notNull(newRelationEntity.getRecord(), "Musí být nenulové");
            Assert.notNull(newRelationEntity.getRecord().getRecordId(), "Musí být nenulové");
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
        Assert.notNull(relationEntity, "Musí být nenulové");
        Assert.notNull(relationEntity.getRoleType(), "Musí být nenulové");
        Assert.notNull(relationEntity.getRelation(), "Musí být nenulové");
        Assert.notNull(relationEntity.getRecord(), "Musí být nenulové");

        //typ role entity odpovídající typu vztahu dle par_relation_type_role_type
        ParRelationRoleType roleType = relationEntity.getRoleType();
        List<ParRelationType> possibleRelationTypes = relationTypeRepository.findByRelationRoleType(roleType);
        if (!possibleRelationTypes.contains(relationEntity.getRelation().getRelationType())) {
            throw new SystemException(
                    "Typ role entity " + roleType.getName() + " nespadá do typu vztahu " + relationEntity.getRelation()
                    .getRelationType().getName());
        }


        //navázaná entita stejné scope jako osoba sama
        RegScope entityScope = relationEntity.getRecord().getScope();
        if (!relationEntity.getRelation().getParty().getRecord().getScope().equals(entityScope)) {
            throw new BusinessException(
                    "Navázaná entita musí mít stejnou třídu rejstříkového hesla jako osoba, ke které entitu navazujeme.",
                    RegistryCode.FOREIGN_ENTITY_INVALID_SCOPE).level(Level.WARNING)
            .set("recordScope", entityScope.getCode())
            .set("entityScope", relationEntity.getRelation().getParty().getRecord().getScope().getCode());
        }

        //navázaná entita povoleného typu rejstříku dle par_registry_role (mělo by to ideálně i dědit)
        RegRegisterType entityRegisterType = relationEntity.getRecord().getRegisterType();
        Set<Integer> registerTypeIds = registerTypeRepository.findByRelationRoleType(roleType)
                .stream().map(RegRegisterType::getRegisterTypeId).collect(Collectors.toSet());
        registerTypeIds = registerTypeRepository.findSubtreeIds(registerTypeIds);
        if (!registerTypeIds.contains(entityRegisterType.getRegisterTypeId())) {
            throw new BusinessException(
                    "Navázaná entita musí mít typ rejstříku nebo podtyp, který je navázaný na roli entity.",
                    RegistryCode.FOREIGN_ENTITY_INVALID_SUBTYPE).level(Level.WARNING)
            .set("entityRegisterType", entityRegisterType.getCode())
            .set("roleType", roleType.getCode());
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
            throw new SystemException("Nalezeno použití party v tabulce ArrDataPartyRef.");
        }

        List<ArrDataRecordRef> dataRecordRefList = dataRecordRefRepository.findByRecord(party.getRecord());
        if (CollectionUtils.isNotEmpty(dataRecordRefList)) {
            throw new SystemException("Nalezeno použití hesla v tabulce ArrDataRecordRef.");
        }

        List<ArrNodeRegister> nodeRegisterList = nodeRegisterRepository.findByRecordId(party.getRecord());
        if (CollectionUtils.isNotEmpty(nodeRegisterList)) {
            throw new SystemException("Nalezeno použití hesla v tabulce ArrDataRecordRef.");
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
    public ParInstitution saveInstitution(final ParInstitution institution, final boolean notification) {
        Assert.notNull(institution, "Instituce musí být vyplněny");
        if (notification) {
            eventNotificationService.publishEvent(new ActionEvent(EventType.INSTITUTION_CHANGE));
        }
        return institutionRepository.save(institution);
    }

    @AuthMethod(permission = {UsrPermission.Permission.REG_SCOPE_RD_ALL, UsrPermission.Permission.REG_SCOPE_RD})
    public ParParty getParty(@AuthParam(type = AuthParam.Type.PARTY) final Integer partyId) {
        Assert.notNull(partyId, "Identifikátor osoby musí být vyplněna");
        return partyRepository.findOne(partyId);
    }

    /**
     * Replace party replaced by party replacement in all usages in JP, Party creators and ParRelation
     * @param replaced
     * @param replacement
     */
    public void replace(final ParParty replaced, final ParParty replacement) {

        // Arr
        final List<ArrDescItem> arrItems = descItemRepository.findArrItemByParty(replaced);
        if (!arrItems.isEmpty()) {

            final Collection<Integer> funds = arrItems.stream().map(ArrDescItem::getFundId).collect(Collectors.toSet());
            // Oprávnění
            if (!userService.hasPermission(UsrPermission.Permission.FUND_ARR_ALL)) {
                funds.forEach(i -> {
                    if (!userService.hasPermission(UsrPermission.Permission.FUND_ARR, i)) {
                        throw new SystemException("Uživatel nemá oprávnění na AS.", BaseCode.INSUFFICIENT_PERMISSIONS).set("fundId", i);
                    }
                });
            }

            final Map<Integer, ArrFundVersion> fundVersions = arrangementService.getOpenVersionsByFundIds(funds).stream()
                    .collect(Collectors.toMap(ArrFundVersion::getFundId, Function.identity()));
            // fund to scopes
            final Map<Integer, Set<Integer>> fundIdsToScopes = funds.stream()
                    .collect(Collectors.toMap(Function.identity(), scopeRepository::findIdsByFundId));

            final ArrChange change = arrangementService.createChange(ArrChange.Type.REPLACE_PARTY);
            arrItems.forEach(i -> {
                final ArrDataPartyRef data = new ArrDataPartyRef();
                data.setParty(replacement);
                ArrDescItem im = new ArrDescItem();
                im.setData(data);
                im.setNode(i.getNode());
                im.setCreateChange(i.getCreateChange());
                im.setDeleteChange(i.getDeleteChange());
                im.setDescItemObjectId(i.getDescItemObjectId());
                im.setItemId(i.getDescItemObjectId());
                im.setItemSpec(i.getItemSpec());
                im.setItemType(i.getItemType());
                im.setPosition(i.getPosition());


                Integer fundId = i.getNode().getFundId();
                Set<Integer> fundScopes = fundIdsToScopes.get(fundId);
                if (fundScopes == null) {
                    throw new SystemException("Pro AS neexistují žádné scope.", BaseCode.INVALID_STATE)
                            .set("fundId", fundId);
                } else {
                    if (!fundScopes.contains(replacement.getRegScope().getScopeId())) {
                        throw new BusinessException("Nelze nahradit osobu v AS jelikož AS nemá scope osoby pomcí které nahrazujeme.", BaseCode.INVALID_STATE)
                                .set("fundId", fundId)
                                .set("scopeId", replacement.getRegScope().getScopeId());
                    }
                }
                descriptionItemService.updateDescriptionItem(im, fundVersions.get(i.getFundId()), change, true);
            });
        }

        // creators
        final List<ParCreator> creatorsUsages = partyCreatorRepository.findByCreatorParty(replaced);


        boolean isScopeAdmin = userService.hasPermission(UsrPermission.Permission.REG_SCOPE_WR_ALL);
        creatorsUsages.forEach(i -> {
            if (!isScopeAdmin && !userService.hasPermission(UsrPermission.Permission.REG_SCOPE_WR, i.getParty().getRegScope().getScopeId())) {
                throw new SystemException("Uživatel nemá oprávnění na scope.", BaseCode.INSUFFICIENT_PERMISSIONS).set("scopeId", i.getParty().getRegScope().getScopeId());
            }
            i.setCreatorParty(replacement);
        });
        partyCreatorRepository.save(creatorsUsages);

        // Registry replace
        registryService.replace(replaced.getRecord(), replacement.getRecord());
    }
}
