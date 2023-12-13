package cz.tacr.elza.repository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

import cz.tacr.elza.common.ObjectListIterator;
import cz.tacr.elza.controller.vo.UniqueValue;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataBit;
import cz.tacr.elza.domain.ArrDataCoordinates;
import cz.tacr.elza.domain.ArrDataDate;
import cz.tacr.elza.domain.ArrDataDecimal;
import cz.tacr.elza.domain.ArrDataFileRef;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataJsonTable;
import cz.tacr.elza.domain.ArrDataNull;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataStructureRef;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.ArrDataUnitid;
import cz.tacr.elza.domain.ArrDataUriRef;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrStructuredObject;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.repository.vo.DataResult;
import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;


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
    public List<UniqueValue> findUniqueSpecValuesInVersion(final ArrFundVersion version,
                                                           final RulItemType itemType,
                                                           final Class<? extends ArrData> dataTypeClass,
                                                           @Nullable final Set<RulItemSpec> specs,
                                                           final boolean withoutSpec,
                                                           @Nullable final String fulltext,
                                                           final int max) {

        SpecificationDataTypeHelper specHelper = new SpecificationDataTypeHelper() {

            private Join<ArrDescItem, RulItemSpec> specJoin;

            @Override
            public void init(final Root<ArrDescItem> descItemJoin) {
                specJoin = descItemJoin.join(ArrDescItem.FIELD_ITEM_SPEC, JoinType.LEFT);
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

        };

        return findUniqueValuesInVersion(version, itemType, dataTypeClass, specHelper, fulltext, max, withoutSpec);
    }

    @Override
    public List<Integer> findUniqueSpecIdsInVersion(final ArrFundVersion version,
                                                    final RulItemType itemType,
                                                    final List<Integer> nodeIds) {
        Set<Integer> result = new HashSet<>();
        for (List<Integer> partOfNodeIds : Lists.partition(nodeIds, ObjectListIterator.getMaxBatchSize())) {
            CriteriaBuilder builder = entityManager.getCriteriaBuilder();
            CriteriaQuery<RulItemSpec> query = builder.createQuery(RulItemSpec.class);

            Root<ArrDescItem> descItem = query.from(ArrDescItem.class);

            Predicate versionPredicate = prepareVersionPredicate(version, builder, descItem);

            //seznam AND podmínek
            List<Predicate> andPredicates = new LinkedList<>();
            andPredicates.add(builder.equal(descItem.get(ArrDescItem.FIELD_NODE).get(ArrNode.FIELD_FUND), version.getFund()));
            andPredicates.add(descItem.get(ArrDescItem.FIELD_NODE_ID).in(partOfNodeIds));
            andPredicates.add(versionPredicate);
            andPredicates.add(builder.equal(descItem.get(ArrDescItem.FIELD_ITEM_TYPE), itemType));

            query.select(descItem.get(ArrItem.FIELD_ITEM_SPEC));
            query.where(andPredicates.toArray(new Predicate[andPredicates.size()]));

            //query.orderBy(builder.asc(substringValue));
            query.distinct(true);

            List<RulItemSpec> resultList = entityManager.createQuery(query).getResultList();

            result.addAll(resultList.stream().map(RulItemSpec::getItemSpecId).collect(Collectors.toList()));
        }
        return new ArrayList<>(result);
    }

    private Predicate prepareVersionPredicate(final ArrFundVersion version,
                                              CriteriaBuilder builder, Root<ArrDescItem> descItem) {
        if (version.getLockChange() == null) {
            return builder.isNull(descItem.get(ArrDescItem.FIELD_DELETE_CHANGE_ID));
        } else {
            Integer lockChangeId = version.getLockChange().getChangeId();

            Predicate createPred = builder.lt(descItem.get(ArrDescItem.FIELD_CREATE_CHANGE_ID), lockChangeId);
            Predicate deletePred = builder.or(
                    builder.isNull(descItem.get(ArrDescItem.FIELD_DELETE_CHANGE_ID)),
                    builder.gt(descItem.get(ArrDescItem.FIELD_DELETE_CHANGE_ID),
                            lockChangeId));

            return builder.and(createPred, deletePred);
        }
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
    private List<UniqueValue> findUniqueValuesInVersion(final ArrFundVersion version,
                                                   final RulItemType itemType,
                                                   final Class<? extends ArrData> dataTypeClass,
                                                   final SpecificationDataTypeHelper specificationDataTypeHelper,
                                                   @Nullable final String fulltext,
                                                   final int max, final boolean withoutSpec) {


        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = builder.createTupleQuery();

        Root<? extends ArrData> data = query.from(dataTypeClass);

        Root<ArrDescItem> descItem = query.from(ArrDescItem.class);

        Predicate versionPredicate = prepareVersionPredicate(version, builder, descItem);

        //seznam AND podmínek
        List<Predicate> andPredicates = new LinkedList<>();
        andPredicates.add(builder.equal(descItem.get(ArrItem.FIELD_DATA), data));
        andPredicates.add(builder.equal(descItem.get(ArrDescItem.FIELD_NODE).get(ArrNode.FIELD_FUND), version.getFund()));
        andPredicates.add(versionPredicate);
        andPredicates.add(builder.equal(descItem.get(ArrDescItem.FIELD_ITEM_TYPE), itemType));
        if (specificationDataTypeHelper.useSpec()) {
            specificationDataTypeHelper.init(descItem);

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

        //id odkazovaného objektu
        Path<Integer> idPath = typeHelper.getIdSelection(builder);
        if (idPath != null) {
            Expression<Integer> idSelection = idPath.as(Integer.class);
            selections.add(idSelection);
        }


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
        List<UniqueValue> result = new ArrayList<>(resultList.size());
        for (Tuple tuple : resultList) {
            UniqueValue uniqueValue = new UniqueValue();

            Object fullValue = tuple.get(0);
            Object onlyValue = tuple.get(1);
            if (withoutSpec) {
                if (fullValue == null) {
                    uniqueValue.setValue(onlyValue == null ? "" : onlyValue.toString());
                } else {
                    uniqueValue.setValue(fullValue.toString());
                }
            } else {
                uniqueValue.setValue(fullValue == null ? "" : fullValue.toString());
            }

            try {
                Object id = tuple.get(2);
                uniqueValue.setId(id == null ? null : (int) id);
            } catch (IllegalArgumentException e) {

            }

            result.add(uniqueValue);

        }

        return result;
    }

    @Override
    public void findAllDataByDataResults(List<DataResult> dataResults) {
        Map<RulDataType, List<Integer>> dataIdsMap = dataResults.stream()
                .collect(Collectors.groupingBy(DataResult::getRulDataType,
                        Collectors.mapping(DataResult::getDataId, Collectors.toList())));

        for (Map.Entry<RulDataType, List<Integer>> dataIdsEntry : dataIdsMap.entrySet()) {
            Class<? extends ArrData> dataClass = getDataClass(dataIdsEntry.getKey());

            List<Integer> dataIds = dataIdsEntry.getValue();
            String storageTable = dataIdsEntry.getKey().getStorageTable();

            entityManager.createQuery("SELECT d FROM " + storageTable + " d WHERE d.dataId in :dataIds", dataClass)
                    .setParameter("dataIds", dataIds)
                    .getResultList();
        }
    }

    public Class<? extends ArrData> getDataClass(final RulDataType dataType) {
        switch (dataType.getCode()) {
            case "INT":
                return ArrDataInteger.class;
            case "STRING":
                return ArrDataString.class;
            case "TEXT":
            case "FORMATTED_TEXT":
                return ArrDataText.class;
            case "DATE":
                return ArrDataDate.class;
            case "UNITDATE":
                return ArrDataUnitdate.class;
            case "UNITID":
                return ArrDataUnitid.class;
            case "COORDINATES":
                return ArrDataCoordinates.class;
            case "RECORD_REF":
                return ArrDataRecordRef.class;
            case "DECIMAL":
                return ArrDataDecimal.class;
            case "STRUCTURED":
                return ArrDataStructureRef.class;
            case "URI_REF":
                return ArrDataUriRef.class;
            case "BIT":
                return ArrDataBit.class;
            case "ENUM":
                return ArrDataNull.class;
            case "JSON_TABLE":
                return ArrDataJsonTable.class;
            case "FILE_REF":
                return ArrDataFileRef.class;
            default:
                throw new org.apache.commons.lang3.NotImplementedException("Nebyl namapován datový typ");
        }
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
     *
     * @param dataClassType               typ třídy ukládající hodnotu atributu
     * @param dataRoot                    kořen vehledání (dataClassType)
     * @param specificationDataTypeHelper
     * @return služba pro načtení hodnot konkrétního typu atributu
     */
    private AbstractDescItemDataTypeHelper getDataTypeHelper(final Class<? extends ArrData> dataClassType,
                                                             final Root dataRoot,
                                                             final SpecificationDataTypeHelper specificationDataTypeHelper) {
        if (dataClassType.equals(ArrDataCoordinates.class) ||
                dataClassType.equals(ArrDataDecimal.class) ||
                dataClassType.equals(ArrDataDate.class)) {
            return new AbstractDescItemDataTypeHelper() {

                @Override
                protected void init() {
                    targetJoin = dataRoot;
                }

                @Override
                public Path<String> getValueStringSelection(final CriteriaBuilder criteriaBuilder) {
                    return targetJoin.get("value");
                }

                @Override
                public Path<Integer> getIdSelection(CriteriaBuilder criteriaBuilder) {
                    return null;
                }
            };
        } else if (dataClassType.equals(ArrDataText.class)) {
            return new AbstractDescItemDataTypeHelper() {

                @Override
                protected void init() {
                    targetJoin = dataRoot;
                }

                @Override
                public Path<String> getValueStringSelection(final CriteriaBuilder criteriaBuilder) {
                    return targetJoin.get(ArrDataText.TEXT_VALUE);
                }

                @Override
                public Path<Integer> getIdSelection(CriteriaBuilder criteriaBuilder) {
                    return null;
                }
            };
        } else if (dataClassType.equals(ArrDataString.class)) {
            return new AbstractDescItemDataTypeHelper() {

                @Override
                protected void init() {
                    targetJoin = dataRoot;
                }

                @Override
                public Path<String> getValueStringSelection(final CriteriaBuilder criteriaBuilder) {
                    return targetJoin.get(ArrDataString.STRING_VALUE);
                }

                @Override
                public Path<Integer> getIdSelection(CriteriaBuilder criteriaBuilder) {
                    return null;
                }
            };
        } else if (dataClassType.equals(ArrDataUriRef.class)) {
            return new AbstractDescItemDataTypeHelper() {

                @Override
                protected void init() {
                    targetJoin = dataRoot;
                }

                @Override
                public Path<String> getValueStringSelection(final CriteriaBuilder criteriaBuilder) {
                    return targetJoin.get(ArrDataUriRef.URI_REF_VALUE);
                }

                @Override
                public Path<Integer> getIdSelection(CriteriaBuilder criteriaBuilder) {
                    return null;
                }
            };
        } else if (dataClassType.equals(ArrDataInteger.class)) {
            return new AbstractDescItemDataTypeHelper() {

                @Override
                protected void init() {
                    targetJoin = dataRoot;
                }

                @Override
                public Path<String> getValueStringSelection(final CriteriaBuilder criteriaBuilder) {
                    return targetJoin.get(ArrDataInteger.INTEGER_VALUE);
                }

                @Override
                public Path<Integer> getIdSelection(CriteriaBuilder criteriaBuilder) {
                    return null;
                }
            };
        } else if (dataClassType.equals(ArrDataBit.class)) {
            return new AbstractDescItemDataTypeHelper() {

                @Override
                protected void init() {
                    targetJoin = dataRoot;
                }

                @Override
                public Path<String> getValueStringSelection(final CriteriaBuilder criteriaBuilder) {
                    return targetJoin.get(ArrDataBit.BIT_VALUE);
                }

                @Override
                public Path<Integer> getIdSelection(CriteriaBuilder criteriaBuilder) {
                    return null;
                }
            };
        } else if (dataClassType.equals(ArrDataUnitid.class)) {
            return new AbstractDescItemDataTypeHelper() {

                @Override
                protected void init() {
                    targetJoin = dataRoot;
                }

                @Override
                public Path<String> getValueStringSelection(final CriteriaBuilder criteriaBuilder) {
                    return targetJoin.get(ArrDataUnitid.FIELD_UNITID);
                }

                @Override
                public Path<Integer> getIdSelection(CriteriaBuilder criteriaBuilder) {
                    return null;
                }
            };
        } else if (dataClassType.equals(ArrDataRecordRef.class)) {
            return new AbstractDescItemDataTypeHelper() {
                @Override
                protected void init() {
                    targetJoin = dataRoot.join(ArrDataRecordRef.FIELD_RECORD, JoinType.INNER);
                }

                @Override
                public Path<String> getValueStringSelection(final CriteriaBuilder criteriaBuilder) {
                    return targetJoin.get(ApAccessPoint.FIELD_ACCESS_POINT_ID);
                }

                @Override
                public Path<Integer> getIdSelection(CriteriaBuilder criteriaBuilder) {
                    return null;
                }
            };
        } else if (dataClassType.equals(ArrDataStructureRef.class)) {
            return new AbstractDescItemDataTypeHelper() {
                @Override
                protected void init() {
                    targetJoin = dataRoot.join(ArrDataStructureRef.STRUCTURED_OBJECT, JoinType.INNER);
                }

                @Override
                public Path<String> getValueStringSelection(final CriteriaBuilder criteriaBuilder) {
                    return targetJoin.get(ArrStructuredObject.FIELD_VALUE);
                }

                @Override
                public Path<Integer> getIdSelection(CriteriaBuilder criteriaBuilder) {
                    return targetJoin.get(ArrStructuredObject.FIELD_STRUCTURED_OBJECT_ID);
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
         *
         * @return výraz
         */
        public abstract Path<String> getValueStringSelection(final CriteriaBuilder criteriaBuilder);

        /**
         * Vrací výraz pro získání id atributu z tabulky.
         *
         * @return výraz
         */
        public abstract Path<Integer> getIdSelection(final CriteriaBuilder criteriaBuilder);
    }

    private interface SpecificationDataTypeHelper {
        boolean useSpec();

        void init(Root<ArrDescItem> descItemJoin);

        Predicate getPredicate(CriteriaBuilder builder);

        Path<String> getSpecSelection();
    }
}
