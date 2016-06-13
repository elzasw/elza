package cz.tacr.elza.repository;

import cz.tacr.elza.domain.*;
import cz.tacr.elza.utils.ObjectListIterator;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.hibernate.jpa.criteria.OrderImpl;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.Tuple;
import javax.persistence.criteria.*;
import java.util.*;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 03.02.2016
 */
public class DataRepositoryImpl implements DataRepositoryCustom {

    @Autowired
    private EntityManager entityManager;


    @Override
    public List<ArrData> findDescItemsByNodeIds(final Set<Integer> nodeIds,
                                                final Set<RulItemType> itemTypes,
                                                final ArrFundVersion version) {


        String hql = "SELECT d FROM arr_data d JOIN FETCH d.descItem di JOIN FETCH di.node n JOIN FETCH di.itemType dit LEFT JOIN FETCH di.itemSpec dis JOIN FETCH d.dataType dt WHERE ";
        if (version.getLockChange() == null) {
            hql += "di.deleteChange IS NULL ";
        } else {
            hql += "di.createChange < :lockChange AND (di.deleteChange IS NULL OR di.deleteChange > :lockChange) ";
        }

        hql += "AND n.nodeId IN (:nodeIds)";

        if (CollectionUtils.isNotEmpty(itemTypes)) {
            hql += " AND di.itemType IN (:itemTypes)";
        }


        Query query = entityManager.createQuery(hql);

        if (version.getLockChange() != null) {
            query.setParameter("lockChange", version.getLockChange());
        }

        if (CollectionUtils.isNotEmpty(itemTypes)) {
            query.setParameter("itemTypes", itemTypes);
        }

        List<ArrData> result = new LinkedList<>();
        ObjectListIterator<Integer> nodeIdsIterator = new ObjectListIterator<Integer>(nodeIds);
        while (nodeIdsIterator.hasNext()) {
            query.setParameter("nodeIds", nodeIdsIterator.next());

            result.addAll(query.getResultList());
        }

        return result;
    }


    @Override
    public List<ArrData> findByDataIdsAndVersionFetchSpecification(final Set<Integer> dataIds, final Set<RulItemType> itemTypes, final ArrFundVersion version) {
        String hql = "SELECT d FROM arr_data d JOIN FETCH d.descItem di JOIN FETCH di.node n JOIN FETCH di.itemType dit JOIN FETCH di.itemSpec dis JOIN FETCH d.dataType dt WHERE ";
        if (version.getLockChange() == null) {
            hql += "di.deleteChange IS NULL ";
        } else {
            hql += "di.createChange < :lockChange AND (di.deleteChange IS NULL OR di.deleteChange > :lockChange) ";
        }

        hql += "AND di.itemType IN (:itemTypes) AND d.dataId IN (:dataIds)";


        Query query = entityManager.createQuery(hql);

        if (version.getLockChange() != null) {
            query.setParameter("lockChange", version.getLockChange());
        }

        query.setParameter("itemTypes", itemTypes);

        List<ArrData> result = new LinkedList<>();
        ObjectListIterator<Integer> nodeIdsIterator = new ObjectListIterator<Integer>(dataIds);
        while (nodeIdsIterator.hasNext()) {
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
                + " JOIN FETCH d.descItem di "
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

            private Join packetTypeJoin;

            @Override
            public void init(final Root root, final Join descItemJoin) {
                packetTypeJoin = root.join(ArrDataPacketRef.PACKET, JoinType.INNER)
                        .join(ArrPacket.PACKET_TYPE, JoinType.LEFT);
            }

            @Override
            public boolean useSpec() {
                if (!dataTypeClass.equals(ArrDataPacketRef.class)) {
                    throw new IllegalArgumentException("Použitelné pouze pro data typu obalu.");
                }
                return true;
            }

            @Override
            public Predicate getPredicate(final CriteriaBuilder builder) {
                if (CollectionUtils.isEmpty(packetTypes) && !withoutType) {
                    throw new IllegalStateException("Musí být zadána alespoň jeden typ obalu.");
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
            public Path getSpecSelection() {
                return packetTypeJoin.get(RulPacketType.NAME);
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

            private Join specJoin;

            @Override
            public void init(final Root root, final Join descItemJoin) {
                specJoin = descItemJoin.join(ArrDescItem.DESC_ITEM_SPEC, JoinType.LEFT);
            }

            @Override
            public boolean useSpec() {
                return itemType.getUseSpecification();
            }

            @Override
            public Predicate getPredicate(final CriteriaBuilder builder) {
                if (CollectionUtils.isEmpty(specs) && !withoutSpec) {
                    throw new IllegalStateException("Musí být zadána alespoň jedna specifikace.");
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
            public Path getSpecSelection() {
                return specJoin.get("name");
            }
        };

        return findUniqueValuesInVersion(version, itemType, dataTypeClass, specHelper, fulltext, max, withoutSpec);
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

        Root data = query.from(dataTypeClass);
        AbstractDescItemDataTypeHelper typeHelper = getDataTypeHelper(dataTypeClass, data);

        Join descItem = data.join(ArrData.DESC_ITEM, JoinType.INNER);
        Join node = descItem.join(ArrDescItem.NODE, JoinType.INNER);


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
        andPredicates.add(builder.equal(node.get(ArrNode.FUND), version.getFund()));
        andPredicates.add(versionPredicate);
        andPredicates.add(builder.equal(descItem.get(ArrDescItem.DESC_ITEM_TYPE), itemType));
        if (specificationDataTypeHelper.useSpec()) {
            specificationDataTypeHelper.init(data, descItem);

            andPredicates.add(specificationDataTypeHelper.getPredicate(builder));
        }


        //seznam vracených sloupců
        List<Selection<?>> selections = new LinkedList<>();
        Expression valueSelection = typeHelper.getValueStringSelection(builder).as(String.class);    //hodnota pro select
        Expression valueExpression = createUniqueValueExpression(valueSelection, specificationDataTypeHelper, builder);
        selections.add(valueExpression);

        //oříznutá hodnota převedená na string, kvůli řazení
        Expression substringValue = builder.substring(valueSelection.as(String.class), 0, 100);
        selections.add(substringValue);


        if (StringUtils.isNotBlank(fulltext)) {
            String text = "%" + fulltext + "%";
            andPredicates.add(builder.like(valueExpression, text));
        }


        //sestavení dotazu
        query.multiselect(selections);
        query.where(andPredicates.toArray(new Predicate[andPredicates.size()]));

        query.orderBy(new OrderImpl(substringValue));
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
    private Expression createUniqueValueExpression(final Expression valuePath,
                                                   final SpecificationDataTypeHelper specHelper,
                                                   final CriteriaBuilder builder) {
        Expression result;
        if (specHelper.useSpec()) {
            Path specSelection = specHelper.getSpecSelection();
            Expression<String> concat = builder.concat(specSelection, ": ");
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
     * @return služba pro načtení hodnot konkrétního typu atributu
     */
    private AbstractDescItemDataTypeHelper getDataTypeHelper(final Class<? extends ArrData> dataClassType,
                                                             final Root dataRoot) {
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
                public Path getValueStringSelection(final CriteriaBuilder criteriaBuilder) {
                    return targetJoin.get("value");
                }
            };
        } else if (dataClassType.equals(ArrDataPartyRef.class)) {
            return new AbstractDescItemDataTypeHelper() {
                @Override
                protected void init() {
                    Join party = dataRoot.join(ArrDataPartyRef.PARTY, JoinType.INNER);
                    Join record = party.join(ParParty.RECORD, JoinType.INNER);
                    targetJoin = record;
                }

                @Override
                public Path getValueStringSelection(final CriteriaBuilder criteriaBuilder) {
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
                public Path getValueStringSelection(final CriteriaBuilder criteriaBuilder) {
                    return targetJoin.get(RegRecord.RECORD);
                }
            };
        } else if (dataClassType.equals(ArrDataPacketRef.class)) {
            return new AbstractDescItemDataTypeHelper() {
                @Override
                protected void init() {
                    targetJoin = dataRoot.join(ArrDataPacketRef.PACKET, JoinType.INNER);
                }

                @Override
                public Path getValueStringSelection(final CriteriaBuilder criteriaBuilder) {
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
        public abstract Path getValueStringSelection(final CriteriaBuilder criteriaBuilder);
    }

    private interface SpecificationDataTypeHelper{
        boolean useSpec();
         void init(Root root, Join descItemJoin);
        Predicate getPredicate(CriteriaBuilder builder);

        Path getSpecSelection();



    }

}
