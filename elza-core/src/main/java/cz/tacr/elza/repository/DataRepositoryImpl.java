package cz.tacr.elza.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.Tuple;
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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.hibernate.jpa.criteria.OrderImpl;
import org.springframework.beans.factory.annotation.Autowired;

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
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.RulPacketType;
import cz.tacr.elza.utils.ObjectListIterator;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 03.02.2016
 */
public class DataRepositoryImpl implements DataRepositoryCustom {

    @Autowired
    private EntityManager entityManager;


    @Override
    public List<ArrData> findDescItemsByNodeIds(final Set<Integer> nodeIds,
                                                final Set<RulDescItemType> descItemTypes,
                                                final ArrFundVersion version) {


        String hql = "SELECT d FROM arr_data d JOIN FETCH d.descItem di JOIN FETCH di.node n JOIN FETCH di.descItemType dit JOIN FETCH d.dataType dt WHERE ";
        if (version.getLockChange() == null) {
            hql += "di.deleteChange IS NULL ";
        } else {
            hql += "di.createChange < :lockChange AND (di.deleteChange IS NULL OR di.deleteChange > :lockChange) ";
        }

        hql += "AND di.descItemType IN (:descItemTypes) AND n.nodeId IN (:nodeIds)";


        Query query = entityManager.createQuery(hql);

        if (version.getLockChange() != null) {
            query.setParameter("lockChange", version.getLockChange());
        }

        query.setParameter("descItemTypes", descItemTypes);

        List<ArrData> result = new LinkedList<>();
        ObjectListIterator<Integer> nodeIdsIterator = new ObjectListIterator<Integer>(nodeIds);
        while (nodeIdsIterator.hasNext()) {
            query.setParameter("nodeIds", nodeIdsIterator.next());

            result.addAll(query.getResultList());
        }

        return result;
    }


    @Override
    public List<ArrData> findByDataIdsAndVersionFetchSpecification(Set<Integer> dataIds, final Set<RulDescItemType> descItemTypes, ArrFundVersion version) {
        String hql = "SELECT d FROM arr_data d JOIN FETCH d.descItem di JOIN FETCH di.node n JOIN FETCH di.descItemType dit JOIN FETCH di.descItemSpec dis JOIN FETCH d.dataType dt WHERE ";
        if (version.getLockChange() == null) {
            hql += "di.deleteChange IS NULL ";
        } else {
            hql += "di.createChange < :lockChange AND (di.deleteChange IS NULL OR di.deleteChange > :lockChange) ";
        }

        hql += "AND di.descItemType IN (:descItemTypes) AND d.dataId IN (:dataIds)";


        Query query = entityManager.createQuery(hql);

        if (version.getLockChange() != null) {
            query.setParameter("lockChange", version.getLockChange());
        }

        query.setParameter("descItemTypes", descItemTypes);

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
                                                                 final RulDescItemType descItemType,
                                                                 final String text) {

        if(StringUtils.isBlank(text)){
            throw new IllegalArgumentException("Parametr text nesmí mít prázdnou hodnotu.");
        }


        String searchText = "%" + text + "%";

        String tableName;
        switch (descItemType.getDataType().getCode()){
            case "STRING":
                tableName = descItemType.getDataType().getStorageTable();
                break;
            case "TEXT":
                tableName = descItemType.getDataType().getStorageTable();
                break;
            default:
                throw new IllegalStateException(
                        "Není zatím implementováno pro typ " + descItemType.getDataType().getCode());
        }

        String hql = "SELECT d FROM " + tableName +" d"
                + " JOIN FETCH d.descItem di "
                + " JOIN FETCH di.node n WHERE di.descItemType = :descItemType AND di.node IN (:nodes) AND d.value like :text";

        Query query = entityManager.createQuery(hql);
        query.setParameter("descItemType", descItemType);
        query.setParameter("nodes", nodes);
        query.setParameter("text", searchText);

        return query.getResultList();
    }


    @Override
    public List<String> findUniquePacketValuesInVersion(final ArrFundVersion version,
                                                         final RulDescItemType descItemType,
                                                         final Class<? extends ArrData> dataTypeClass,
                                                         @Nullable final Set<RulPacketType> packetTypes,
                                                         @Nullable final String fulltext,
                                                         final int max){

        SpecificationDataTypeHelper specHelper = new SpecificationDataTypeHelper() {

            private Join packetTypeJoin;

            @Override
            public void init(final Root root, final Join descItemJoin) {
                packetTypeJoin = root.join(ArrDataPacketRef.PACKET, JoinType.INNER)
                        .join(ArrPacket.PACKET_TYPE, JoinType.INNER);
            }

            @Override
            public boolean useSpec() {
                if (!dataTypeClass.equals(ArrDataPacketRef.class)) {
                    throw new IllegalArgumentException("Použitelné pouze pro data typu obalu.");
                }
                return true;
            }

            @Override
            public Predicate getPredicate() {
                if (CollectionUtils.isEmpty(packetTypes)) {
                    throw new IllegalStateException("Musí být zadána alespoň jeden typ obalu.");
                } else {
                    return packetTypeJoin.in(packetTypes);
                }
            }

            @Override
            public Selection getSpecSelection() {
                return packetTypeJoin.get(RulPacketType.NAME);
            }
        };

        return findUniqueValuesInVersion(version, descItemType, dataTypeClass, specHelper, fulltext, max);

    }

    @Override
    public List<String> findUniqueSpecValuesInVersion(final ArrFundVersion version,
                                                       final RulDescItemType descItemType,
                                                       final Class<? extends ArrData> dataTypeClass,
                                                       @Nullable final Set<RulDescItemSpec> specs,
                                                       @Nullable final String fulltext,
                                                       final int max){

        SpecificationDataTypeHelper specHelper = new SpecificationDataTypeHelper() {

            private Join specJoin;

            @Override
            public void init(final Root root, final Join descItemJoin) {
                specJoin = descItemJoin.join(ArrDescItem.DESC_ITEM_SPEC, JoinType.INNER);
            }

            @Override
            public boolean useSpec() {
                return descItemType.getUseSpecification();
            }

            @Override
            public Predicate getPredicate() {
                if (CollectionUtils.isEmpty(specs)) {
                    throw new IllegalStateException("Musí být zadána alespoň jedna specifikace.");
                } else {
                    return specJoin.in(specs);
                }
            }

            @Override
            public Selection getSpecSelection() {
                return specJoin.get("name");
            }
        };

        return findUniqueValuesInVersion(version, descItemType, dataTypeClass, specHelper, fulltext, max);
    }

    /**
     * Provede načtení unikátních hodnot atributů.
     *
     * @param version                     id verze stromu
     * @param descItemType                typ atributu
     * @param dataTypeClass               třída hodnot atributu
     * @param specificationDataTypeHelper obsluha načtení specifikací / obalů
     * @param fulltext                    fulltext
     * @param max                         maximální počet hodnot
     * @return seznam unikátních hodnot
     */
    private List<String> findUniqueValuesInVersion(final ArrFundVersion version,
                                                   final RulDescItemType descItemType,
                                                   final Class<? extends ArrData> dataTypeClass,
                                                   final SpecificationDataTypeHelper specificationDataTypeHelper,
                                                   @Nullable final String fulltext,
                                                   final int max) {


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
        andPredicates.add(builder.equal(descItem.get(ArrDescItem.DESC_ITEM_TYPE), descItemType));
        if (StringUtils.isNotBlank(fulltext)) {
            andPredicates.add(typeHelper.getFulltextPredicate(fulltext, builder));
        }


        //seznam vracených sloupců
        List<Selection<?>> selections = new LinkedList<>();


        Path valueSelection = typeHelper.getValueStringSelection(builder);    //hodnota pro select
        selections.add(valueSelection);

        if (specificationDataTypeHelper.useSpec()) {
            specificationDataTypeHelper.init(data, descItem);
            andPredicates.add(specificationDataTypeHelper.getPredicate());
            selections.add(specificationDataTypeHelper.getSpecSelection());
        }

        //oříznutá hodnota převedená na string, kvůli řazení

        Expression substringValue = builder.substring(valueSelection.as(String.class), 0, 100);
        selections.add(substringValue);

        query.multiselect(selections);
        query.where(andPredicates.toArray(new Predicate[andPredicates.size()]));

        query.orderBy(new OrderImpl(substringValue));
        query.distinct(true);

        List<Tuple> resultList = entityManager.createQuery(query).setMaxResults(max).getResultList();

        //převedení na text
        List<String> result = new ArrayList<>(resultList.size());
        if (specificationDataTypeHelper.useSpec()) {
            for (Tuple tuple : resultList) {
                Object value = tuple.get(0);
                result.add(tuple.get(1) + ": " + value == null ? "" : value.toString());
            }
        } else {
            for (Tuple tuple : resultList) {
                Object value = tuple.get(0);
                result.add(value == null ? "" : value.toString());
            }
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
                dataClassType.equals(ArrDataUnitid.class)) {
            return new AbstractDescItemDataTypeHelper() {

                @Override
                protected void init() {
                    targetJoin = dataRoot;
                }

                @Override
                public Path getValueStringSelection(final CriteriaBuilder criteriaBuilder) {
                    return targetJoin.get("value");
                }

                @Override
                public Predicate getFulltextPredicate(final String fulltext, final CriteriaBuilder criteriaBuilder) {
                    String fulltextString = "%" + fulltext + "%";
                    return criteriaBuilder.like(targetJoin.get("value"), fulltextString);
                }
            };
        } else if (dataClassType.equals(ArrDataDecimal.class) ||
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

                @Override
                public Predicate getFulltextPredicate(final String fulltext, final CriteriaBuilder criteriaBuilder) {
                    String fulltextString = "%" + fulltext + "%";

                    return criteriaBuilder.like(targetJoin.get("value").as(String.class), fulltextString);
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

                @Override
                public Predicate getFulltextPredicate(final String fulltext, final CriteriaBuilder criteriaBuilder) {
                    String fulltextString = "%" + fulltext + "%";

                    return criteriaBuilder.like(targetJoin.get(RegRecord.RECORD), fulltextString);
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

                @Override
                public Predicate getFulltextPredicate(final String fulltext, final CriteriaBuilder criteriaBuilder) {
                    String fulltextString = "%" + fulltext + "%";

                    return criteriaBuilder.like(targetJoin.get(RegRecord.RECORD), fulltextString);
                }
            };
        }else if(dataClassType.equals(ArrDataPacketRef.class)){
            return new AbstractDescItemDataTypeHelper() {
                @Override
                protected void init() {
                    targetJoin = dataRoot.join(ArrDataPacketRef.PACKET, JoinType.INNER);
                }

                @Override
                public Path getValueStringSelection(final CriteriaBuilder criteriaBuilder) {
                    return targetJoin.get(ArrPacket.STORAGE_NUMBER);
                }

                @Override
                public Predicate getFulltextPredicate(final String fulltext, final CriteriaBuilder criteriaBuilder) {
                    String fulltextString = "%" + fulltext + "%";

                    return criteriaBuilder.like(targetJoin.get(ArrPacket.STORAGE_NUMBER), fulltextString);
                }
            };
        }
        else  {
            throw new NotImplementedException();
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


        /**
         * Vrací podmínku pro vyhledání hodnoty podle fulltextu.
         * @param fulltext fulltext
         * @param criteriaBuilder
         * @return
         */
        public abstract Predicate getFulltextPredicate(final String fulltext, final CriteriaBuilder criteriaBuilder);
    }

    private interface SpecificationDataTypeHelper{
        boolean useSpec();
         void init(Root root, Join descItemJoin);
        Predicate getPredicate();

        Selection getSpecSelection();



    }

}
