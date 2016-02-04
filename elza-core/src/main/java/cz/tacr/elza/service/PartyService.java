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

import javax.annotation.Nullable;
import javax.persistence.EntityManager;

import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cz.tacr.elza.ElzaTools;
import cz.tacr.elza.domain.ArrCalendarType;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrFindingAid;
import cz.tacr.elza.domain.ArrNodeRegister;
import cz.tacr.elza.domain.ParComplementType;
import cz.tacr.elza.domain.ParCreator;
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
import cz.tacr.elza.domain.RegVariantRecord;
import cz.tacr.elza.repository.CalendarTypeRepository;
import cz.tacr.elza.repository.ComplementTypeRepository;
import cz.tacr.elza.repository.DataPartyRefRepository;
import cz.tacr.elza.repository.DataRecordRefRepository;
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
import cz.tacr.elza.repository.RelationEntityRepository;
import cz.tacr.elza.repository.RelationRepository;
import cz.tacr.elza.repository.RelationRoleTypeRepository;
import cz.tacr.elza.repository.RelationTypeRepository;
import cz.tacr.elza.repository.UnitdateRepository;
import cz.tacr.elza.repository.VariantRecordRepository;


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
            return Collections.EMPTY_MAP;
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
     * @param firstResult  první vrácená osoba
     * @param maxResults   max počet vrácených osob
     * @param findingAid   AP, ze které se použijí třídy rejstříků
     */
    public List<ParParty> findPartyByTextAndType(final String searchRecord, final Integer partyTypeId,
                                                 final Integer firstResult, final Integer maxResults,
                                                 @Nullable final ArrFindingAid findingAid) {

        Set<Integer> scopeIdsForRecord = registryService.getScopeIdsByFindingAid(findingAid);
        return partyRepository.findPartyByTextAndType(searchRecord, partyTypeId, firstResult, maxResults,
                scopeIdsForRecord);
    }

    /**
     * Vrátí počet osob vyhovující zadané frázi. Osobu vyhledává podle hesla v rejstříku včetně variantních hesel.
     * @param searchRecord hledaný řetězec, může být null
     * @param registerTypeId typ záznamu
     * @param findingAid   AP, ze které se použijí třídy rejstříků
     * @return
     */
    public long findPartyByTextAndTypeCount(final String searchRecord, final Integer registerTypeId,
                                            @Nullable final ArrFindingAid findingAid){

        Set<Integer> scopeIdsForRecord = registryService.getScopeIdsByFindingAid(findingAid);
        return partyRepository.findPartyByTextAndTypeCount(searchRecord, registerTypeId, scopeIdsForRecord);
    }

    /**
     * Uložení osoby a všech navázaných dat, která musejí být při ukládání vyplněna.
     * @param newParty nová osoba s navázanými daty
     * @return uložená osoba
     */
    public ParParty saveParty(final ParParty newParty) {
        Assert.notNull(newParty);

        ParPartyType partyType = partyTypeRepository.findOne(newParty.getPartyType().getPartyTypeId());

        ParParty saveParty;
        if (newParty.getPartyId() == null) {
            saveParty = newParty;
        } else {
            saveParty = partyRepository.findOne(newParty.getPartyId());
            Assert.notNull(saveParty);
            saveParty = ElzaTools.unproxyEntity(saveParty, ParParty.class);

            BeanUtils.copyProperties(newParty, saveParty, "partyGroupIdentifiers", "record",
                    "preferredName", "from", "to", "partyNames", "partyCreators");
        }
        saveParty.setPartyType(partyType);

        //synchronizace datace
        Set<ParUnitdate> removeUnitdate = new HashSet<>();
        removeUnitdate.add(synchUnitdate(saveParty, newParty.getFrom(), (g) -> g.getFrom(),
                (g, f) -> g.setFrom(f)));
        removeUnitdate.add(synchUnitdate(saveParty, newParty.getTo(), (g) -> g.getTo(),
                (g, f) -> g.setTo(f)));
        removeUnitdate.remove(null);
        unitdateRepository.delete(removeUnitdate);

        //synchronizace rejstříkového hesla
        synchRecord(newParty);


        //synchronizace jmen osoby
        ParPartyName newPrefferedName = newParty.getPreferredName();
        saveParty.setPreferredName(null);
        saveParty = partyRepository.save(saveParty);
        synchPartyNames(saveParty, newPrefferedName, newParty.getPartyNames());

        //synchronizace skupinových identifikátorů
        if (ParPartyGroup.class.isAssignableFrom(saveParty.getClass())) {
            ParPartyGroup savePartyGroup = (ParPartyGroup) saveParty;
            ParPartyGroup partyGroup = (ParPartyGroup) newParty;
            synchPartyGroupIdentifiers(savePartyGroup,
                    partyGroup.getPartyGroupIdentifiers() == null ? Collections.EMPTY_LIST
                                                                  : partyGroup.getPartyGroupIdentifiers());
        }


        //synchronizace tvůrců osoby
        synchCreators(saveParty, newParty.getPartyCreators() == null ?
                                 Collections.EMPTY_LIST : newParty.getPartyCreators());

        ParParty result = partyRepository.save(saveParty);
        entityManager.flush();

       return result;

    }

    /**
     * V groovy scriptu vytvoří rejstříkové heslo pro osobu a upraví jej v databázi.
     * @param party osoba
     */
    private void synchRecord(final ParParty party) {
        Assert.notNull(party);

        Assert.notNull(party.getRecord(), "Osoba nemá zadané rejstříkové heslo.");
        Assert.notNull(party.getRecord().getRegisterType(), "Není vyplněný typ rejstříkového helsa.");
        Assert.notNull(party.getRecord().getScope(), "Není nastavena třída rejstříkového hesla");
        Assert.notNull(party.getRecord().getScope().getScopeId(), "Není nastaveno id třídy rejstříkového hesla");

        //vytvoření rejstříkového hesla v groovy
        RegRecord recordFromGroovy = groovyScriptService.getRecordFromGroovy(party);
        List<RegVariantRecord> variantRecords = new ArrayList<>(recordFromGroovy.getVariantRecordList());

        //uložení hesla
        if (party.getPartyId() != null) {
            ParParty dbParty = partyRepository.findOne(party.getPartyId());
            recordFromGroovy.setRecordId(dbParty.getRecord().getRecordId());
            recordFromGroovy.setVersion(dbParty.getRecord().getVersion());
        }
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


        Map<Integer, ParPartyGroupIdentifier> dbIdentifiersMap = Collections.EMPTY_MAP;
        if(partyGroup.getPartyId() != null){
            dbIdentifiersMap = ElzaTools
                .createEntityMap(partyGroupIdentifierRepository.findByParty(partyGroup),
                        i -> i.getPartyGroupIdentifierId());
        }
        Set<ParPartyGroupIdentifier> removeIdentifiers = new HashSet<>(dbIdentifiersMap.values());

        Set<ParUnitdate> removeUnitdate = new HashSet<>();

        for (ParPartyGroupIdentifier newIdentifier : newPartyGroupIdentifiers) {
            ParPartyGroupIdentifier oldIdentifier = dbIdentifiersMap
                    .get(newIdentifier.getPartyGroupIdentifierId());

            if (oldIdentifier == null) {
                oldIdentifier = newIdentifier;
                oldIdentifier.setFrom(saveUnitDate(newIdentifier.getFrom()));
                oldIdentifier.setTo(saveUnitDate(newIdentifier.getTo()));
            } else {
                removeIdentifiers.remove(oldIdentifier);

                removeUnitdate.add(synchUnitdate(oldIdentifier, newIdentifier.getFrom(), (g) -> g.getFrom(),
                        (g, f) -> g.setFrom(f)));
                removeUnitdate.add(synchUnitdate(oldIdentifier, newIdentifier.getTo(), (g) -> g.getTo(),
                        (g, f) -> g.setTo(f)));
            }

            oldIdentifier.setPartyGroup(partyGroup);
            oldIdentifier.setSource(newIdentifier.getSource());
            oldIdentifier.setNote(newIdentifier.getNote());
            oldIdentifier.setIdentifier(newIdentifier.getIdentifier());

            partyGroupIdentifierRepository.save(oldIdentifier);
        }

        for (ParPartyGroupIdentifier removeIdentifier : removeIdentifiers) {
            deleteUnitDates(removeIdentifier.getFrom(), removeIdentifier.getTo());
        }

        removeUnitdate.remove(null);
        unitdateRepository.delete(removeUnitdate);
        partyGroupIdentifierRepository.delete(removeIdentifiers);
    }

    /**
     * Synchronizace stavu jmen osob. CRUD
     *
     * @param savedParty       uložená osoba
     * @param newPrefferedName preferované jméno osoby
     * @param newPartyNames    seznam všech jmen osoby (obsahuje i preferované jméno)
     */
    private void synchPartyNames(final ParParty savedParty,
                                 final ParPartyName newPrefferedName,
                                 final List<ParPartyName> newPartyNames) {
        Map<Integer, ParPartyName> dbPartyNameMap = ElzaTools
                .createEntityMap(partyNameRepository.findByParty(savedParty), p -> p.getPartyNameId());

        Set<ParPartyName> removePartyNames = new HashSet<>(dbPartyNameMap.values());
        Set<ParUnitdate> removeUnitdate = new HashSet<>();

        for (ParPartyName newPartyName : newPartyNames) {
            ParPartyName oldPartyName = dbPartyNameMap.get(newPartyName.getPartyNameId());

            Assert.notNull(newPartyName.getNameFormType());
            Assert.notNull(newPartyName.getNameFormType().getNameFormTypeId());
            ParPartyNameFormType nameFormType = partyNameFormTypeRepository
                    .findOne(newPartyName.getNameFormType().getNameFormTypeId());

            if (oldPartyName == null) {
                oldPartyName = newPartyName;
                oldPartyName.setValidFrom(saveUnitDate(newPartyName.getValidFrom()));
                oldPartyName.setValidTo(saveUnitDate(newPartyName.getValidTo()));
            } else {
                removePartyNames.remove(oldPartyName);
                removeUnitdate.add(synchUnitdate(oldPartyName, newPartyName.getValidFrom(), g -> g.getValidFrom(),
                        (n, from) -> n.setValidFrom(from)));
                removeUnitdate.add(synchUnitdate(oldPartyName, newPartyName.getValidTo(), g -> g.getValidTo(),
                        (n, to) -> n.setValidTo(to)));
            }

            oldPartyName.setNameFormType(nameFormType);
            oldPartyName.setMainPart(newPartyName.getMainPart());
            oldPartyName.setOtherPart(newPartyName.getOtherPart());
            oldPartyName.setNote(newPartyName.getNote());
            oldPartyName.setDegreeBefore(newPartyName.getDegreeBefore());
            oldPartyName.setDegreeAfter(newPartyName.getDegreeAfter());
            oldPartyName.setParty(savedParty);

            //nastavení preferovaného jména
            if (newPartyName == newPrefferedName) {
                savedParty.setPreferredName(oldPartyName);
            }

            partyNameRepository.save(oldPartyName);
            synchComplementTypes(oldPartyName, newPartyName.getPartyNameComplements() == null
                                               ? Collections.EMPTY_LIST : newPartyName.getPartyNameComplements());
        }

        removeUnitdate.remove(null);
        unitdateRepository.delete(removeUnitdate);

        for (ParPartyName removePartyName : removePartyNames) {
            deletePartyName(removePartyName);
        }
    }

    /**
     * Syncoronizace doplňků jména osoby. CRUD
     * @param partyName jméno osoby
     * @param newComplementTypes stav doplňků jména
     */
    private void synchComplementTypes(final ParPartyName partyName,
                                      final List<ParPartyNameComplement> newComplementTypes) {
        Map<Integer, ParPartyNameComplement> dbComplementMap = ElzaTools
                .createEntityMap(partyNameComplementRepository.findByPartyName(partyName),
                        c -> c.getPartyNameComplementId());

        Set<ParPartyNameComplement> removeComplements = new HashSet<>(dbComplementMap.values());

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

            partyNameComplementRepository.save(oldComplementType);
        }

        partyNameComplementRepository.delete(removeComplements);
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

        List<ParPartyName> partyNames = new ArrayList<>(partyNameRepository.findByParty(party));

        party.setPreferredName(null);
        for (ParPartyName parPartyName : partyNames) {
            deletePartyName(parPartyName);
        }

        partyCreatorRepository.deleteByPartyBoth(party);


        partyRelationRepository.findByParty(party).forEach((pr) -> {
                    deleteRelation(pr);
                }
        );

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

        partyRepository.delete(party);
        registryService.deleteRecord(party.getRecord(), false);
        deleteUnitDates(party.getFrom(), party.getTo());
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


        Assert.notNull(relationSource.getComplementType());
        Assert.notNull(relationSource.getComplementType().getRelationTypeId());

        Set<ParUnitdate> unitdateRemove = new HashSet<>();

        ParRelationType relationType = relationTypeRepository
                .findOne(relationSource.getComplementType().getRelationTypeId());


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
        }


        ParParty party = partyRepository.findOne(relationSource.getParty().getPartyId());
        Assert.notNull(party);

        relation.setParty(party);
        relation.setComplementType(relationType);
        relation.setDateNote(relationSource.getDateNote());
        relation.setNote(relationSource.getNote());

        ParRelation result = relationRepository.save(relation);
        relationRepository.flush();

        saveDeleteRelationEntities(result, relationEntities);


        for (ParUnitdate unitdate : unitdateRemove) {
            unitdateRepository.delete(unitdate);
        }

        return result;
    }

    public ParUnitdate saveUnitDate(@Nullable final ParUnitdate unitdateSource) {
        if(unitdateSource == null){
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
            ArrCalendarType calendarType = calendarTypeRepository
                    .findOne(unitdateSource.getCalendarType().getCalendarTypeId());
            unitdate.setCalendarType(calendarType);
        }

        return unitdateRepository.save(unitdate);
    }


    public void deleteRelation(final ParRelation relation) {


        ParUnitdate from = relation.getFrom();
        ParUnitdate to = relation.getTo();

        List<ParRelationEntity> relationEntities = relationEntityRepository.findByRelation(relation);
        if (!relationEntities.isEmpty()) {
            relationEntityRepository.delete(relationEntities);
        }

        relationRepository.delete(relation);

        deleteUnitDates(from, to);
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
            return Collections.EMPTY_LIST;
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

                saveEntity.setSource(newRelationEntity.getSource());
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

            result.add(relationEntityRepository.save(saveEntity));
        }

        if (!toRemoveEntities.isEmpty()) {
            relationEntityRepository.delete(toRemoveEntities);
        }

        return result;
    }


    /**
     * Prověří existenci vazeb na osobu. Pokud existují, vyhodí příslušnou výjimku, nelze mazat.
     * @param party osoba
     */
    private void checkPartyUsage(final ParParty party) {
        // vazby ( arr_node_register, ArrDataRecordRef, ArrDataPartyRef),
        Long pocet = dataPartyRefRepository.getCountByParty(party.getPartyId());
        if (pocet > 0) {
            throw new IllegalStateException("Nalezeno použití party v tabulce ArrDataPartyRef.");
        }

        List<ArrDataRecordRef> dataRecordRefList = dataRecordRefRepository.findByRecordId(party.getRecord().getRecordId());
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

}
