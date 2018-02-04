package cz.tacr.elza.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import javax.persistence.criteria.Subquery;

import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.exception.SystemException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import cz.tacr.elza.common.ObjectListIterator;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataCoordinates;
import cz.tacr.elza.domain.ArrDataDecimal;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataPacketRef;
import cz.tacr.elza.domain.ArrDataPartyRef;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDataUnitid;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulPacketType;


/**
 * Implementation of DataRepositoryCustom
 *
 * @since 03.02.2016
 */
public class DataRepositoryImpl implements DataRepositoryCustom {

    public static final String SPEC_SEPARATOR = ": ";

    @Autowired
    private EntityManager entityManager;


    @Override
    public List<ArrData> findDescItemsByNodeIds(final Set<Integer> nodeIds,
                                                final Set<RulItemType> itemTypes,
                                                final ArrFundVersion version) {
        return findDescItemsByNodeIds(nodeIds, itemTypes, version.getLockChange() == null ? null : version.getLockChange().getChangeId());
    }

    @Override
    public List<ArrData> findDescItemsByNodeIds(final Set<Integer> nodeIds, final Set<RulItemType> itemTypes, final Integer changeId) {
        String hql = "SELECT d FROM arr_data d JOIN FETCH d.item di JOIN FETCH di.node n JOIN FETCH di.itemType dit LEFT JOIN FETCH di.itemSpec dis JOIN FETCH d.dataType dt WHERE ";
        if (changeId == null) {
            hql += "di.deleteChange IS NULL ";
        } else {
            hql += "di.createChange.changeId <= :changeId AND (di.deleteChange IS NULL OR di.deleteChange.changeId >= :changeId) ";
        }

        hql += "AND n.nodeId IN (:nodeIds)";

        if (CollectionUtils.isNotEmpty(itemTypes)) {
            hql += " AND di.itemType IN (:itemTypes)";
        }

        List<ArrData> result = new LinkedList<>();
        ObjectListIterator<Integer> nodeIdsIterator = new ObjectListIterator<Integer>(nodeIds);
        while (nodeIdsIterator.hasNext()) {

            Query query = entityManager.createQuery(hql);
            if (changeId != null) {
                query.setParameter("changeId", changeId);
            }
            if (CollectionUtils.isNotEmpty(itemTypes)) {
                query.setParameter("itemTypes", itemTypes);
            }
            query.setParameter("nodeIds", nodeIdsIterator.next());

            result.addAll(query.getResultList());
        }

        return result;
    }


    @Override
    public List<ArrData> findByDataIdsAndVersionFetchSpecification(final Set<Integer> dataIds, final Set<RulItemType> itemTypes, final ArrFundVersion version) {
        return findByDataIdsAndVersionFetchSpecification(dataIds, itemTypes, version.getLockChange() == null ? null : version.getLockChange().getChangeId());
    }

    @Override
    public List<ArrData> findByDataIdsAndVersionFetchSpecification(final Set<Integer> dataIds, final Set<RulItemType> itemTypes, final Integer changeId) {
        String hql = "SELECT d FROM arr_data d JOIN FETCH d.item di JOIN FETCH di.node n JOIN FETCH di.itemType dit JOIN FETCH di.itemSpec dis JOIN FETCH d.dataType dt WHERE ";
        if (changeId == null) {
            hql += "di.deleteChange IS NULL ";
        } else {
            hql += "di.createChange.changeId <= :changeId AND (di.deleteChange IS NULL OR di.deleteChange.changeId >= :changeId) ";
        }

        hql += "AND di.itemType IN (:itemTypes) AND d.dataId IN (:dataIds)";

        List<ArrData> result = new LinkedList<>();
        ObjectListIterator<Integer> nodeIdsIterator = new ObjectListIterator<Integer>(dataIds);
        while (nodeIdsIterator.hasNext()) {

            Query query = entityManager.createQuery(hql);
            if (changeId != null) {
                query.setParameter("changeId", changeId);
            }
            query.setParameter("itemTypes", itemTypes);
        	query.setParameter("dataIds", nodeIdsIterator.next());

            result.addAll(query.getResultList());
        }

        return result;
    }


    @Override
    public <T extends ArrData> List<T> findByNodesContainingText(final Collection<ArrNode> nodes,
                                                                 final RulItemType itemType,
                                                                 final Set<RulItemSpec> specifications,
                                                                 final String text) {

        if(StringUtils.isBlank(text)){
            throw new IllegalArgumentException("Parametr text nesmí mít prázdnou hodnotu.");
        }

        if(itemType.getUseSpecification() && CollectionUtils.isEmpty(specifications)){
            throw new IllegalArgumentException("Musí být zadána alespoň jedna filtrovaná specifikace.");
        }


        String searchText = "%" + text + "%";

        String tableName;
        switch (itemType.getDataType().getCode()){
            case "STRING":
                tableName = itemType.getDataType().getStorageTable();
                break;
            case "TEXT":
                tableName = itemType.getDataType().getStorageTable();
                break;
            default:
                throw new IllegalStateException(
                        "Není zatím implementováno pro typ " + itemType.getDataType().getCode());
        }

        String hql = "SELECT d FROM " + tableName +" d"
                + " JOIN FETCH d.item di "
                + " JOIN FETCH di.node n WHERE di.itemType = :itemType";

        if(itemType.getUseSpecification()){
            hql+= " AND di.itemSpec IN (:specs)";
        }

        hql += " AND di.node IN (:nodes) AND d.value like :text AND di.deleteChange IS NULL";

        Query query = entityManager.createQuery(hql);
        query.setParameter("itemType", itemType);
        query.setParameter("nodes", nodes);
        query.setParameter("text", searchText);
        if (itemType.getUseSpecification()) {
            query.setParameter("specs", specifications);
        }


        return query.getResultList();
    }


    @Override
    public List<String> findUniquePacketValuesInVersion(final ArrFundVersion version,
                                                         final RulItemType itemType,
                                                         final Class<? extends ArrData> dataTypeClass,
                                                         @Nullable final Set<RulPacketType> packetTypes,
                                                         final boolean withoutType,
                                                         @Nullable final String fulltext,
                                                         final int max){

        SpecificationDataTypeHelper specHelper = new SpecificationDataTypeHelper() {

            private Join<ArrPacket, RulPacketType> packetTypeJoin;
            private Join<ArrDataPacketRef, ArrPacket> packetJoin;

            @Override
            public void init(final Root<ArrDescItem> descItemJoin, final Join<ArrDescItem, ArrData> dataJoin) {
                Join<ArrDescItem, ArrDataPacketRef> treat = entityManager.getCriteriaBuilder().treat(dataJoin, ArrDataPacketRef.class);
                packetJoin = treat.join(ArrDataPacketRef.PACKET, JoinType.INNER);
                packetTypeJoin = packetJoin.join(ArrPacket.PACKET_TYPE, JoinType.LEFT);
            }

            @Override
            public boolean useSpec() {
                if (!dataTypeClass.equals(ArrDataPacketRef.class)) {
                    throw new SystemException("Použitelné pouze pro data typu obalu.");
                }
                return true;
            }

            @Override
            public Predicate getPredicate(final CriteriaBuilder builder) {
                if (CollectionUtils.isEmpty(packetTypes) && !withoutType) {
                    throw new SystemException("Musí být zadána alespoň jeden typ obalu.");
                }

                if (!withoutType) {
                    return packetTypeJoin.in(packetTypes);
                }

                if (CollectionUtils.isEmpty(packetTypes)) {
                    return packetTypeJoin.isNull();
                }

                return builder.or(packetTypeJoin.in(packetTypes), packetTypeJoin.isNull());
            }

            @Override
            public Path<String> getSpecSelection() {
                return packetTypeJoin.get(RulPacketType.NAME);
            }

            @Override
            public Join<ArrDataPacketRef, ArrPacket> getPacketJoin() {
                return packetJoin;
            }
        };

        return findUniqueValuesInVersion(version, itemType, dataTypeClass, specHelper, fulltext, max, withoutType);

    }

    @Override
    public List<String> findUniqueSpecValuesInVersion(final ArrFundVersion version,
                                                       final RulItemType itemType,
                                                       final Class<? extends ArrData> dataTypeClass,
                                                       @Nullable final Set<RulItemSpec> specs,
                                                       final boolean withoutSpec,
                                                       @Nullable final String fulltext,
                                                       final int max){

        SpecificationDataTypeHelper specHelper = new SpecificationDataTypeHelper() {

            private Join<ArrDescItem, RulItemSpec> specJoin;

            @Override
            public void init(final Root<ArrDescItem> descItemJoin, final Join<ArrDescItem, ArrData> dataJoin) {
                specJoin = descItemJoin.join(ArrDescItem.ITEM_SPEC, JoinType.LEFT);
            }

            @Override
            public boolean useSpec() {
                return itemType.getUseSpecification();
            }

            @Override
            public Predicate getPredicate(final CriteriaBuilder builder) {
                if (CollectionUtils.isEmpty(specs) && !withoutSpec) {
                    throw new SystemException("Musí být zadána alespoň jedna specifikace.");
                }

                if (!withoutSpec) {
                    return specJoin.in(specs);
                }

                if (CollectionUtils.isEmpty(specs)) {
                    return specJoin.isNull();
                }

                return builder.or(specJoin.in(specs), specJoin.isNull());
            }

            @Override
            public Path<String> getSpecSelection() {
                return specJoin.get("name");
            }

            @Override
            public Join<ArrDataPacketRef, ArrPacket> getPacketJoin() {
                return null;
            }
        };

        return findUniqueValuesInVersion(version, itemType, dataTypeClass, specHelper, fulltext, max, withoutSpec);
    }

    @Override
    public List<Integer> findUniqueSpecIdsInVersion(final ArrFundVersion version,
                                                    final RulItemType itemType) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<RulItemSpec> query = builder.createQuery(RulItemSpec.class);

        Root<ArrDescItem> descItem = query.from(ArrDescItem.class);

        Predicate versionPredicate;
        if (version.getLockChange() == null) {
            versionPredicate = builder.isNull(descItem.get(ArrDescItem.DELETE_CHANGE_ID));
        } else {
            Integer lockChangeId = version.getLockChange().getChangeId();

            Predicate createPred = builder.lt(descItem.get(ArrDescItem.CREATE_CHANGE_ID), lockChangeId);
            Predicate deletePred = builder.or(
                    builder.isNull(descItem.get(ArrDescItem.DELETE_CHANGE_ID)),
                    builder.gt(descItem.get(ArrDescItem.DELETE_CHANGE_ID), lockChangeId)
            );

            versionPredicate = builder.and(createPred, deletePred);
        }

        //seznam AND podmínek
        List<Predicate> andPredicates = new LinkedList<>();
        andPredicates.add(builder.equal(descItem.get(ArrDescItem.NODE).get(ArrNode.FUND), version.getFund()));
        andPredicates.add(versionPredicate);
        andPredicates.add(builder.equal(descItem.get(ArrDescItem.ITEM_TYPE), itemType));

        query.select(descItem.get(ArrItem.ITEM_SPEC));
        query.where(andPredicates.toArray(new Predicate[andPredicates.size()]));

        //query.orderBy(builder.asc(substringValue));
        query.distinct(true);

        List<RulItemSpec> resultList = entityManager.createQuery(query).getResultList();

        return resultList.stream().map(RulItemSpec::getItemSpecId).collect(Collectors.toList());
    }

    @Override
    public List<Integer> findUniquePacketTypeIdsInVersion(final ArrFundVersion version, final RulItemType itemType) {

        // TODO slapa: optimalizovat, načítat pouze typy bez dat

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ArrDescItem> c = cb.createQuery(ArrDescItem.class);
        Root<ArrDescItem> descItem = c.from(ArrDescItem.class);
        Join<ArrData, ArrDescItem> data = descItem.join(ArrItem.DATA);

        Subquery<Integer> sq = c.subquery(Integer.class);
        Root<ArrDataPacketRef> dataPacketRef = sq.from(ArrDataPacketRef.class);
        Join<ArrPacket, ArrDataPacketRef> sqDataRef = dataPacketRef.join(ArrDataPacketRef.PACKET);

        sq.select(dataPacketRef.get(ArrData.ID));

        Predicate versionPredicate;
        if (version.getLockChange() == null) {
            versionPredicate = cb.isNull(descItem.get(ArrDescItem.DELETE_CHANGE_ID));
        } else {
            Integer lockChangeId = version.getLockChange().getChangeId();

            Predicate createPred = cb.lt(descItem.get(ArrDescItem.CREATE_CHANGE_ID), lockChangeId);
            Predicate deletePred = cb.or(
                    cb.isNull(descItem.get(ArrDescItem.DELETE_CHANGE_ID)),
                    cb.gt(descItem.get(ArrDescItem.DELETE_CHANGE_ID), lockChangeId)
            );

            versionPredicate = cb.and(createPred, deletePred);
        }

        //seznam AND podmínek
        List<Predicate> andPredicates = new LinkedList<>();
        andPredicates.add(cb.equal(descItem.get(ArrDescItem.NODE).get(ArrNode.FUND), version.getFund()));
        andPredicates.add(versionPredicate);
        andPredicates.add(cb.equal(descItem.get(ArrDescItem.ITEM_TYPE), itemType));
        andPredicates.add(cb.in(descItem.get(ArrItem.DATA)).value(data));

        sq.where(andPredicates.toArray(new Predicate[andPredicates.size()]));

        c.select(descItem).where(andPredicates.toArray(new Predicate[andPredicates.size()]));

        TypedQuery<ArrDescItem> q = entityManager.createQuery(c);

        List<ArrDescItem> descItems = q.getResultList();

        Set<Integer> result = new HashSet<>();
        boolean addNull = false;
        for (ArrDescItem di : descItems) {
            ArrDataPacketRef d = (ArrDataPacketRef) di.getData();
            RulPacketType packetType = d.getPacket().getPacketType();
            if (packetType != null) {
                result.add(packetType.getPacketTypeId());
            } else {
                addNull = true;
            }
        }

        ArrayList<Integer> integers = new ArrayList<>(result);
        if (addNull) {
            integers.add(null);
        }
        return integers;
    }

    /**
     * Provede načtení unikátních hodnot atributů.
     *
     * @param version                     id verze stromu
     * @param itemType                    typ atributu
     * @param dataTypeClass               třída hodnot atributu
     * @param specificationDataTypeHelper obsluha načtení specifikací / obalů
     * @param fulltext                    fulltext
     * @param max                         maximální počet hodnot
     * @param withoutSpec
     * @return seznam unikátních hodnot
     */
    private List<String> findUniqueValuesInVersion(final ArrFundVersion version,
                                                   final RulItemType itemType,
                                                   final Class<? extends ArrData> dataTypeClass,
                                                   final SpecificationDataTypeHelper specificationDataTypeHelper,
                                                   @Nullable final String fulltext,
                                                   final int max, final boolean withoutSpec) {


        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = builder.createTupleQuery();

        Root<? extends ArrData> data = query.from(dataTypeClass);

        Root<ArrDescItem> descItem = query.from(ArrDescItem.class);

        Join<ArrDescItem, ArrData> dataJoin = null;
        if (dataTypeClass.equals(ArrDataPacketRef.class)) {
            dataJoin = descItem.join(ArrItem.DATA, JoinType.INNER);
        }

        Predicate versionPredicate;
        if (version.getLockChange() == null) {
            versionPredicate = builder.isNull(descItem.get(ArrDescItem.DELETE_CHANGE_ID));
        } else {
            Integer lockChangeId = version.getLockChange().getChangeId();

            Predicate createPred = builder.lt(descItem.get(ArrDescItem.CREATE_CHANGE_ID), lockChangeId);
            Predicate deletePred = builder.or(
                    builder.isNull(descItem.get(ArrDescItem.DELETE_CHANGE_ID)),
                    builder.gt(descItem.get(ArrDescItem.DELETE_CHANGE_ID), lockChangeId)
            );

            versionPredicate = builder.and(createPred, deletePred);
        }

        //seznam AND podmínek
        List<Predicate> andPredicates = new LinkedList<>();
        andPredicates.add(builder.equal(descItem.get(ArrItem.DATA), data));
        andPredicates.add(builder.equal(descItem.get(ArrDescItem.NODE).get(ArrNode.FUND), version.getFund()));
        andPredicates.add(versionPredicate);
        andPredicates.add(builder.equal(descItem.get(ArrDescItem.ITEM_TYPE), itemType));
        if (specificationDataTypeHelper.useSpec()) {
            specificationDataTypeHelper.init(descItem, dataJoin);

            andPredicates.add(specificationDataTypeHelper.getPredicate(builder));
        }

        AbstractDescItemDataTypeHelper typeHelper = getDataTypeHelper(dataTypeClass, data, specificationDataTypeHelper);

        //seznam vracených sloupců
        List<Selection<?>> selections = new LinkedList<>();
        Expression<String> valueSelection = typeHelper.getValueStringSelection(builder).as(String.class);    //hodnota pro select
        Expression<String> valueExpression = createUniqueValueExpression(valueSelection, specificationDataTypeHelper, builder);
        selections.add(valueExpression);

        //oříznutá hodnota převedená na string, kvůli řazení
        Expression<String> substringValue = builder.substring(valueSelection.as(String.class), 0, 100);
        selections.add(substringValue);


        if (StringUtils.isNotBlank(fulltext)) {
            String text = "%" + fulltext + "%";
            andPredicates.add(builder.like(valueExpression, text));
        }


        //sestavení dotazu
        query.multiselect(selections);
        query.where(andPredicates.toArray(new Predicate[andPredicates.size()]));

        query.orderBy(builder.asc(substringValue));
        query.distinct(true);

        List<Tuple> resultList = entityManager.createQuery(query).setMaxResults(max).getResultList();

        //převedení na text
        List<String> result = new ArrayList<>(resultList.size());
        for (Tuple tuple : resultList) {
            Object fullValue = tuple.get(0);
            Object onlyValue = tuple.get(1);
            if (withoutSpec) {
                if (fullValue == null) {
                    result.add(onlyValue == null ? "" : onlyValue.toString());
                } else {
                    result.add(fullValue.toString());
                }
            } else {
                result.add(fullValue == null ? "" : fullValue.toString());
            }

        }

        return result;
    }


    /**
     * Vytvoření selectu pro hodnotu atributu.
     *
     * @param valuePath  výraz pro získání hodnoty atributu
     * @param specHelper použití specifikace
     * @param builder    criteria builder
     * @return výraz pro výběr hodnoty atributu. Pokud se jedná o specifikaci, je výraz spojen do název specifikace:
     * hodnota.
     */
    private Expression<String> createUniqueValueExpression(final Expression<String> valuePath,
                                                   final SpecificationDataTypeHelper specHelper,
                                                   final CriteriaBuilder builder) {
        Expression<String> result;
        if (specHelper.useSpec()) {
            Path<String> specSelection = specHelper.getSpecSelection();
            Expression<String> concat = builder.concat(specSelection, SPEC_SEPARATOR);
            result = builder.concat(concat, valuePath);
        } else {
            result = valuePath;
        }

        return result;
    }

    /**
     * Podle typu atributu vrací informace pro načtení a filtrování konkrétních dat.
     * @param dataClassType typ třídy ukládající hodnotu atributu
     * @param dataRoot kořen vehledání (dataClassType)
     * @param specificationDataTypeHelper
     * @return služba pro načtení hodnot konkrétního typu atributu
     */
    private AbstractDescItemDataTypeHelper getDataTypeHelper(final Class<? extends ArrData> dataClassType,
                                                             final Root dataRoot,
                                                             final SpecificationDataTypeHelper specificationDataTypeHelper) {
        if (dataClassType.equals(ArrDataString.class) ||
                dataClassType.equals(ArrDataText.class) ||
                dataClassType.equals(ArrDataCoordinates.class) ||
                dataClassType.equals(ArrDataUnitid.class) ||
                dataClassType.equals(ArrDataDecimal.class) ||
                dataClassType.equals(ArrDataInteger.class)) {
            return new AbstractDescItemDataTypeHelper() {

                @Override
                protected void init() {
                    targetJoin = dataRoot;
                }

                @Override
                public Path<String> getValueStringSelection(final CriteriaBuilder criteriaBuilder) {
                    return targetJoin.get("value");
                }
            };
        } else if (dataClassType.equals(ArrDataPartyRef.class)) {
            return new AbstractDescItemDataTypeHelper() {
                @Override
                protected void init() {
                    Join<ArrDataPartyRef, ParParty> party = dataRoot.join(ArrDataPartyRef.PARTY, JoinType.INNER);
                    Join<ParParty, RegRecord> record = party.join(ParParty.RECORD, JoinType.INNER);
                    targetJoin = record;
                }

                @Override
                public Path<String> getValueStringSelection(final CriteriaBuilder criteriaBuilder) {
                    return targetJoin.get(RegRecord.RECORD);
                }
            };
        } else if (dataClassType.equals(ArrDataRecordRef.class)) {
            return new AbstractDescItemDataTypeHelper() {
                @Override
                protected void init() {
                    targetJoin = dataRoot.join(ArrDataRecordRef.RECORD, JoinType.INNER);
                }

                @Override
                public Path<String> getValueStringSelection(final CriteriaBuilder criteriaBuilder) {
                    return targetJoin.get(RegRecord.RECORD);
                }
            };
        } else if (dataClassType.equals(ArrDataPacketRef.class)) {
            return new AbstractDescItemDataTypeHelper() {
                @Override
                protected void init() {
                    targetJoin = specificationDataTypeHelper.getPacketJoin() == null ? dataRoot.join(ArrDataPacketRef.PACKET, JoinType.INNER) : specificationDataTypeHelper.getPacketJoin();
                }

                @Override
                public Path<String> getValueStringSelection(final CriteriaBuilder criteriaBuilder) {
                    return targetJoin.get(ArrPacket.STORAGE_NUMBER);
                }
            };
        } else {
            throw new NotImplementedException("Data class: " + dataClassType);
        }
    }

    /**
     * Služba pro načtení hodnot konkrétního typu atributů.
     */
    private static abstract class AbstractDescItemDataTypeHelper {

        protected From targetJoin;

        public AbstractDescItemDataTypeHelper() {
            init();
        }

        protected abstract void init();

        /**
         * Vrací výraz pro získání hodnoty atributu z tabulky.
         * @return výraz
         */
        public abstract Path<String> getValueStringSelection(final CriteriaBuilder criteriaBuilder);
    }

    private interface SpecificationDataTypeHelper {
        boolean useSpec();

        void init(Root<ArrDescItem> descItemJoin, Join<ArrDescItem, ArrData> dataJoin);

        Predicate getPredicate(CriteriaBuilder builder);

        Path<String> getSpecSelection();

        Join<ArrDataPacketRef, ArrPacket> getPacketJoin();
    }
}
