package cz.tacr.elza.repository;

import cz.tacr.elza.controller.vo.*;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.filter.AipFieldMapping;
import cz.tacr.elza.repository.filter.AipFieldMapping.AipJoin;
import cz.tacr.elza.repository.filter.AipFilterCapabilities;
import cz.tacr.elza.repository.filter.AipFilterValueType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 */
public class AipRepositoryImpl implements AipRepositoryCustom {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public FilteredResult<DaAip> findAipsByFilter(final SearchParams params) {
        int firstResult = params.getOffset() == null ? 0 : params.getOffset();
        int maxResults = params.getSize() == null ? 0 : params.getSize();

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<DaAip> query = cb.createQuery(DaAip.class);
        CriteriaQuery<Long> queryCount = cb.createQuery(Long.class);

        Root<DaAip> root = query.from(DaAip.class);
        Root<DaAip> aipRootCount = queryCount.from(DaAip.class);

        Joins joins = joins(cb, root);
        Joins joinsCount = joins(cb, aipRootCount);

        Predicate condition = prepareCondition(params.getFilters(), cb, joins);
        Predicate conditionCount = prepareCondition(params.getFilters(), cb, joinsCount);

        query.select(root);
        queryCount.select(cb.countDistinct(aipRootCount));

        query.where(condition).orderBy(prepareOrder(params.getSort(), cb, joins));
        queryCount.where(conditionCount);

        TypedQuery<DaAip> tq = entityManager.createQuery(query)
                .setFirstResult(firstResult);
        if (maxResults > 0) {
            tq.setMaxResults(maxResults);
        }
        List<DaAip> list = tq.getResultList();
        int count = entityManager.createQuery(queryCount).getSingleResult().intValue();

        return new FilteredResult<>(firstResult, maxResults, count, list);
    }

    /**
     * Joins of the AIP query, built once per query root and addressed by {@link AipJoin}.
     */
    private static final class Joins {
        private final Map<AipJoin, From<?, ?>> byJoin = new EnumMap<>(AipJoin.class);

        From<?, ?> get(final AipJoin join) {
            return byJoin.get(join);
        }
    }

    private Joins joins(final CriteriaBuilder cb, final Root<DaAip> aipRoot) {
        Join<DaAip, DaAipState> stateJoin = aipRoot.join("states", JoinType.LEFT);
        stateJoin.on(cb.isNull(stateJoin.get("deleteChange")));
        Join<DaAip, DaSyncQueueItem> importSyncJoin = aipRoot.join("syncQueueItems", JoinType.LEFT);
        importSyncJoin.on(cb.isTrue(importSyncJoin.get("active")), importSyncJoin.get("state").in(DaSyncQueueItem.QueueItemState.IMPORT_NEW,
                DaSyncQueueItem.QueueItemState.IMPORT_OK,
                DaSyncQueueItem.QueueItemState.IMPORT_ERROR,
                DaSyncQueueItem.QueueItemState.UPDATE));
        Join<DaAip, DaSyncQueueItem> exportSyncJoin = aipRoot.join("syncQueueItems", JoinType.LEFT);
        exportSyncJoin.on(cb.isTrue(exportSyncJoin.get("active")), exportSyncJoin.get("state").in(DaSyncQueueItem.QueueItemState.EXPORT_NEW,
                DaSyncQueueItem.QueueItemState.EXPORT_OK,
                DaSyncQueueItem.QueueItemState.EXPORT_ERROR));
        Join<DaAipState, ApAccessPoint> oApJoin = stateJoin.join("originatorAccessPoint", JoinType.LEFT);
        Join<DaAipState, ParInstitution> instJoin = stateJoin.join("institution", JoinType.LEFT);
        Join<ParInstitution, ApAccessPoint> instApJoin = instJoin.join("accessPoint", JoinType.LEFT);
        Join<DaAipState, ArrFund> fundJoin = stateJoin.join("fund", JoinType.LEFT);

        Joins joins = new Joins();
        joins.byJoin.put(AipJoin.AIP, aipRoot);
        joins.byJoin.put(AipJoin.STATE, stateJoin);
        joins.byJoin.put(AipJoin.IMPORT_SYNC, importSyncJoin);
        joins.byJoin.put(AipJoin.EXPORT_SYNC, exportSyncJoin);
        joins.byJoin.put(AipJoin.ORIGINATOR_AP, oApJoin);
        joins.byJoin.put(AipJoin.INSTITUTION_AP, instApJoin);
        joins.byJoin.put(AipJoin.FUND, fundJoin);
        return joins;
    }

    /**
     * Conditions of a search are joined by AND; nesting is expressed by LogicalFilter.
     */
    private Predicate prepareCondition(final List<AbstractFilter> filters, final CriteriaBuilder cb,
                                       final Joins joins) {
        if (filters == null || filters.isEmpty()) {
            return cb.conjunction();
        }
        List<Predicate> predicates = new ArrayList<>(filters.size());
        for (AbstractFilter filter : filters) {
            predicates.add(toPredicate(filter, cb, joins));
        }
        return cb.and(predicates.toArray(new Predicate[0]));
    }

    private Predicate toPredicate(final AbstractFilter filter, final CriteriaBuilder cb, final Joins joins) {
        AipFilterCapabilities.checkFilterSupported(filter.getFilterType());

        if (filter instanceof LogicalFilter logical) {
            List<Predicate> predicates = new ArrayList<>();
            for (AbstractFilter sub : logical.getFilters()) {
                predicates.add(toPredicate(sub, cb, joins));
            }
            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            Predicate[] array = predicates.toArray(new Predicate[0]);
            return logical.getOperation() == OperationLogicalType.OR ? cb.or(array) : cb.and(array);
        }
        if (filter instanceof TextValueFilter text) {
            return textPredicate(text, cb, joins);
        }
        if (filter instanceof NumberValueFilter number) {
            return numberPredicate(number, cb, joins);
        }
        if (filter instanceof BoolValueFilter bool) {
            return boolPredicate(bool, cb, joins);
        }
        if (filter instanceof DateValueFilter date) {
            return datePredicate(date, cb, joins);
        }
        if (filter instanceof EnumValueFilter enumFilter) {
            return enumPredicate(enumFilter, cb, joins);
        }
        if (filter instanceof RefValueFilter ref) {
            return refPredicate(ref, cb, joins);
        }
        throw new BusinessException("Filtr " + filter.getFilterType().getValue()
                + " není při vyhledávání AIP podporován", BaseCode.PROPERTY_IS_INVALID)
                        .set("filterType", filter.getFilterType().getValue());
    }

    // --- per value type -----------------------------------------------------------------

    private Predicate textPredicate(final TextValueFilter filter, final CriteriaBuilder cb, final Joins joins) {
        Resolved resolved = resolve(filter.getField(), filter.getFilterType());
        AipFilterCapabilities.checkOperationSupported(filter.getOperation(), resolved.fieldName);
        Path<?> path = resolved.path(joins);
        return switch (filter.getOperation()) {
            case IS_NULL -> cb.isNull(path);
            case NOT_NULL -> cb.isNotNull(path);
            case EQ -> cb.equal(path, AipFilterValueType.requireValue(filter.getValue(), resolved.fieldName));
            case CONTAINS -> cb.like(stringPath(path), contains(filter.getValue(), resolved.fieldName));
            case NOT_CONTAINS -> cb.notLike(stringPath(path), contains(filter.getValue(), resolved.fieldName));
            default -> throw unsupported(filter.getOperation().getValue(), resolved.fieldName);
        };
    }

    private Predicate numberPredicate(final NumberValueFilter filter, final CriteriaBuilder cb, final Joins joins) {
        Resolved resolved = resolve(filter.getField(), filter.getFilterType());
        AipFilterCapabilities.checkOperationSupported(filter.getOperation(), resolved.fieldName);
        Path<?> path = resolved.path(joins);
        return switch (filter.getOperation()) {
            case IS_NULL -> cb.isNull(path);
            case NOT_NULL -> cb.isNotNull(path);
            case EQ -> cb.equal(path, number(path, AipFilterValueType.requireValue(filter.getValue(), resolved.fieldName)));
            case BETWEEN -> between(cb, path,
                    number(path, AipFilterValueType.requireValue(filter.getFrom(), resolved.fieldName)),
                    number(path, AipFilterValueType.requireValue(filter.getTo(), resolved.fieldName)));
            default -> throw unsupported(filter.getOperation().getValue(), resolved.fieldName);
        };
    }

    private Predicate boolPredicate(final BoolValueFilter filter, final CriteriaBuilder cb, final Joins joins) {
        Resolved resolved = resolve(filter.getField(), filter.getFilterType());
        AipFilterCapabilities.checkOperationSupported(filter.getOperation(), resolved.fieldName);
        Path<?> path = resolved.path(joins);
        return switch (filter.getOperation()) {
            case IS_NULL -> cb.isNull(path);
            case NOT_NULL -> cb.isNotNull(path);
            case EQ -> cb.equal(path, AipFilterValueType.requireValue(filter.getValue(), resolved.fieldName));
            case NEQ -> notEqual(cb, path, AipFilterValueType.requireValue(filter.getValue(), resolved.fieldName));
            default -> throw unsupported(filter.getOperation().getValue(), resolved.fieldName);
        };
    }

    /**
     * A date range covers whole days, so an AIP dated on the boundary day is part of the result.
     * A field mapped onto a pair of columns must fit into the range with both of them.
     */
    private Predicate datePredicate(final DateValueFilter filter, final CriteriaBuilder cb, final Joins joins) {
        Resolved resolved = resolve(filter.getField(), filter.getFilterType());
        AipFilterCapabilities.checkOperationSupported(filter.getOperation(), resolved.fieldName);
        Path<?> path = resolved.path(joins);
        return switch (filter.getOperation()) {
            case IS_NULL -> resolved.mapping.isPair()
                    ? cb.and(cb.isNull(path), cb.isNull(resolved.secondPath(joins)))
                    : cb.isNull(path);
            case NOT_NULL -> resolved.mapping.isPair()
                    ? cb.and(cb.isNotNull(path), cb.isNotNull(resolved.secondPath(joins)))
                    : cb.isNotNull(path);
            case BETWEEN -> {
                LocalDateTime from = AipFilterValueType.rangeStart(
                        AipFilterValueType.requireValue(filter.getFrom(), resolved.fieldName), resolved.fieldName);
                LocalDateTime to = AipFilterValueType.rangeEnd(
                        AipFilterValueType.requireValue(filter.getTo(), resolved.fieldName), resolved.fieldName);
                Predicate first = between(cb, path,
                        AipFilterValueType.toDateBound(path.getJavaType(), from),
                        AipFilterValueType.toDateBound(path.getJavaType(), to));
                if (!resolved.mapping.isPair()) {
                    yield first;
                }
                Path<?> second = resolved.secondPath(joins);
                yield cb.and(first, between(cb, second,
                        AipFilterValueType.toDateBound(second.getJavaType(), from),
                        AipFilterValueType.toDateBound(second.getJavaType(), to)));
            }
            default -> throw unsupported(filter.getOperation().getValue(), resolved.fieldName);
        };
    }

    private Predicate enumPredicate(final EnumValueFilter filter, final CriteriaBuilder cb, final Joins joins) {
        Resolved resolved = resolve(filter.getField(), filter.getFilterType());
        AipFilterCapabilities.checkOperationSupported(filter.getOperation(), resolved.fieldName);
        Path<?> path = resolved.path(joins);
        return switch (filter.getOperation()) {
            case IS_NULL -> cb.isNull(path);
            case NOT_NULL -> cb.isNotNull(path);
            case EQ -> cb.equal(path,
                    AipFilterValueType.parseEnum(path.getJavaType(), filter.getValue(), resolved.fieldName));
            case NEQ -> notEqual(cb, path,
                    AipFilterValueType.parseEnum(path.getJavaType(), filter.getValue(), resolved.fieldName));
            default -> throw unsupported(filter.getOperation().getValue(), resolved.fieldName);
        };
    }

    private Predicate refPredicate(final RefValueFilter filter, final CriteriaBuilder cb, final Joins joins) {
        Resolved resolved = resolve(filter.getField(), filter.getFilterType());
        AipFilterCapabilities.checkOperationSupported(filter.getOperation(), resolved.fieldName);
        Path<?> path = resolved.path(joins);
        return switch (filter.getOperation()) {
            case IS_NULL -> cb.isNull(path);
            case NOT_NULL -> cb.isNotNull(path);
            case EQ -> cb.equal(path, number(path, AipFilterValueType.requireValue(filter.getValue(), resolved.fieldName)));
            case NEQ -> notEqual(cb, path, number(path, AipFilterValueType.requireValue(filter.getValue(), resolved.fieldName)));
            default -> throw unsupported(filter.getOperation().getValue(), resolved.fieldName);
        };
    }

    /**
     * Not-equal that also matches rows with no value: a row carrying nothing certainly does not
     * carry the given value, while plain SQL inequality would drop it.
     */
    private static Predicate notEqual(final CriteriaBuilder cb, final Path<?> path, final Object value) {
        return cb.or(cb.isNull(path), cb.notEqual(path, value));
    }

    // --- field resolution ---------------------------------------------------------------

    /**
     * A field of the contract resolved onto its mapping, with the filter model checked
     * against the value type of the field.
     */
    private record Resolved(AipFieldMapping mapping, String fieldName) {

        Path<?> path(final Joins joins) {
            return joins.get(mapping.getJoin()).get(mapping.getAttribute());
        }

        Path<?> secondPath(final Joins joins) {
            return joins.get(mapping.getJoin()).get(mapping.getSecondAttribute());
        }
    }

    private Resolved resolve(final AbstractField field, final FilterType filterType) {
        if (!(field instanceof AipField aipField)) {
            throw new BusinessException("Při vyhledávání AIP lze filtrovat pouze podle polí typu "
                    + FieldType.AIP_FIELD.getValue(), BaseCode.PROPERTY_IS_INVALID)
                            .set("fieldType", field == null ? null : field.getFieldType().getValue());
        }
        AipFieldName fieldName = aipField.getFieldName();
        AipFieldMapping mapping = AipFieldMapping.of(fieldName);
        mapping.getValueType().checkFilterType(filterType, fieldName.getValue());
        return new Resolved(mapping, fieldName.getValue());
    }

    // --- sorting ------------------------------------------------------------------------

    private List<Order> prepareOrder(final List<Sorting> sort, final CriteriaBuilder cb, final Joins joins) {
        if (sort == null || sort.isEmpty()) {
            return List.of(cb.asc(joins.get(AipJoin.AIP).get(DaAip.FIELD_CODE)));
        }
        List<Order> orders = new ArrayList<>(sort.size());
        for (Sorting sorting : sort) {
            AipFieldMapping mapping = AipFieldMapping.of(sortedField(sorting.getField()));
            Path<?> path = joins.get(mapping.getJoin()).get(mapping.getAttribute());
            orders.add(sorting.getOrder() == SortingOrder.DESC ? cb.desc(path) : cb.asc(path));
        }
        return orders;
    }

    private AipFieldName sortedField(final String field) {
        if (field == null || field.isEmpty()) {
            throw new BusinessException("Pole pro řazení není vyplněno", BaseCode.PROPERTY_NOT_EXIST);
        }
        try {
            return AipFieldName.fromValue(field);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Podle pole '" + field + "' nelze AIP řadit", e,
                    BaseCode.PROPERTY_IS_INVALID).set(BaseCode.PARAM_PROPERTY, field);
        }
    }

    // --- helpers ------------------------------------------------------------------------

    private static String contains(final String value, final String fieldName) {
        return "%" + AipFilterValueType.requireValue(value, fieldName) + "%";
    }

    /**
     * Path to a text column. Called only after the filter model has been checked against the
     * value type of the field, which guarantees the column really is of type String.
     */
    @SuppressWarnings("unchecked")
    private static Expression<String> stringPath(final Path<?> path) {
        return (Expression<String>) path;
    }

    /**
     * The contract carries whole numbers as int64; narrows to the type of the column so the
     * parameter is bound in it.
     */
    private static Object number(final Path<?> path, final Number value) {
        Class<?> javaType = path.getJavaType();
        if (Integer.class.equals(javaType) || int.class.equals(javaType)) {
            return value.intValue();
        }
        if (Short.class.equals(javaType) || short.class.equals(javaType)) {
            return value.shortValue();
        }
        return value.longValue();
    }

    /**
     * Range comparison over a column whose type is only known at runtime.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Predicate between(final CriteriaBuilder cb, final Path<?> path, final Object from,
                                     final Object to) {
        return cb.between((Expression<Comparable>) path, (Comparable) from, (Comparable) to);
    }

    private static BusinessException unsupported(final String operation, final String fieldName) {
        return (BusinessException) new BusinessException("Operace " + operation
                + " není při vyhledávání AIP podporována pro pole '" + fieldName + "'",
                BaseCode.PROPERTY_IS_INVALID)
                        .set(BaseCode.PARAM_PROPERTY, fieldName)
                        .set("operation", operation);
    }
}
