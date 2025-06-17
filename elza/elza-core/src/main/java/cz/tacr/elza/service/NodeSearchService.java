package cz.tacr.elza.service;

import static cz.tacr.elza.domain.ArrDescItem.NORM_FROM;
import static cz.tacr.elza.domain.ArrDescItem.NORM_TO;
import static cz.tacr.elza.domain.ArrDescItem.REL_AP_ID;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateClausesStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.AbstractFilter;
import cz.tacr.elza.controller.vo.DescItemField;
import cz.tacr.elza.controller.vo.FieldValueFilter;
import cz.tacr.elza.controller.vo.Fund;
import cz.tacr.elza.controller.vo.FundSearchResult;
import cz.tacr.elza.controller.vo.LogicalFilter;
import cz.tacr.elza.controller.vo.MultimatchContainsFilter;
import cz.tacr.elza.controller.vo.NodeTreeData;
import cz.tacr.elza.controller.vo.OperationCompareType;
import cz.tacr.elza.controller.vo.SearchParams;
import cz.tacr.elza.controller.vo.TreeNodeVO;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ArrCachedNode;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.converter.UnitDateConverter;
import cz.tacr.elza.domain.vo.ArrFundToNodeList;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.service.cache.CachedNode;
import cz.tacr.elza.service.cache.NodeCacheService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
@Configuration
public class NodeSearchService {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private NodeCacheService nodeCacheService;

	@Autowired
	private ArrangementInternalService arrangementInternalService; 

	@Autowired
	private LevelTreeCacheService levelTreeCacheService;

	@Autowired
	private ClientFactoryVO clientFactory;

    @Autowired
    private StaticDataService staticDataService;

    /**
     * @return vrací session uživatele
     */
    @Bean
    @SessionScope
    public Holder<Collection<ArrFundToNodeList>> fundSearchSession() {
        return new Holder<>();
    }

    /**
	 * Seznam AS podle parametrů vyhledávání.
	 * 
	 * @param searchParams
	 * @return
	 */
	public List<FundSearchResult> nodeSearch(SearchParams searchParams) {
		SearchSession searchSession = Search.session(em);
		SearchPredicateFactory factory = searchSession.scope(ArrCachedNode.class).predicate();
		SearchPredicate predicate = createSearchPredicate(factory, searchParams);

        SearchResult<ArrCachedNode> resultList = searchSession.search(ArrCachedNode.class).where(predicate).fetchAll();

        // map: fundId -> ArrFundToNodeList
        Map<Integer, ArrFundToNodeList> fundToNodeListMap = new HashMap<>();

        resultList.hits().forEach(arrCachedNode -> {
        	CachedNode cachedNode = nodeCacheService.deserialize(arrCachedNode.getData());
        	ArrFundToNodeList fundToNodeList = fundToNodeListMap.get(cachedNode.getFundId());
        	if (fundToNodeList == null) {
        		fundToNodeList = new ArrFundToNodeList(cachedNode.getFundId(), new ArrayList<>());
        		fundToNodeListMap.put(cachedNode.getFundId(), fundToNodeList);
        	}
        	fundToNodeList.getNodeIdList().add(arrCachedNode.getNodeId());
        });

        Collection<ArrFundToNodeList> fundToNodeList = fundToNodeListMap.values();

        // uložit do session uživatele
        fundSearchSession().set(fundToNodeList);

        // read all ArrFundVersion by fundIds
        List<ArrFundVersion> fundVersions = arrangementInternalService.getOpenVersionsByFundIds(fundToNodeListMap.keySet());
        Map<Integer, ArrFundVersion> fundVersionsMap = fundVersions.stream().collect(Collectors.toMap(ArrFundVersion::getFundId, f -> f));
        
        List<FundSearchResult> result = new ArrayList<>(fundToNodeList.size());

        fundToNodeList.forEach(fund -> {
        	ArrFundVersion fundVersion = fundVersionsMap.get(fund.getFundId());
        	Objects.requireNonNull(fundVersion);

        	FundSearchResult fundSearch = new FundSearchResult();
        	fundSearch.setCount(fund.getNodeCount());
        	fundSearch.setId(fund.getFundId());
        	fundSearch.setFundVersionId(fundVersion.getFundVersionId());

        	// TODO vytvořit metodu pro transformace: ArrFundVersion -> FundSearchResult
        	Fund f = clientFactory.createFund(fundVersion);
        	fundSearch.setCreateDate(f.getCreateDate());
        	fundSearch.setFundNumber(f.getFundNumber());
        	fundSearch.setInstitutionIdentifier(f.getInstitutionIdentifier());
        	fundSearch.setInternalCode(f.getInternalCode());
        	fundSearch.setMark(f.getMark());
        	fundSearch.setName(f.getName());
        	fundSearch.setUnitdate(f.getUnitdate());
        	fundSearch.setUuid(f.getUuid());

        	result.add(fundSearch);
        });

        return result;
	}

	/**
	 * Seznam uzlů vybraného archivního souboru.
	 * 
	 * @param fundId
	 * @return
	 */
	public List<NodeTreeData> nodeGetSearchResult(Integer fundId) {
        ArrFundToNodeList fundToNodeList = getFundToNodeListFromSession(fundId);
        if (fundToNodeList != null) {
            List<Integer> nodeIdList = fundToNodeList.getNodeIdList();
            ArrFundVersion fundVersion = arrangementInternalService.getOpenVersionByFundId(fundToNodeList.getFundId());
            List<Integer> sortedList = levelTreeCacheService.sortNodesByTreePosition(nodeIdList, fundVersion);
            List<TreeNodeVO> treeNodes = levelTreeCacheService.getNodesByIds(sortedList, fundVersion);

            // TODO dočasné řešení do úplné výměny TreeNodeVO => NodeTree
            List<NodeTreeData> result = new ArrayList<>(treeNodes.size());
            treeNodes.forEach(node -> {
            	NodeTreeData nodeData = new NodeTreeData();
            	nodeData.setId(node.getId());
            	nodeData.setName(node.getName());
            	nodeData.setIcon(node.getIcon());
            	nodeData.setHasChildren(node.isHasChildren());
            	nodeData.setDepth(node.getDepth());
            	nodeData.setReferenceMark(Arrays.asList(node.getReferenceMark()));
            	nodeData.setVersion(node.getVersion());
            	nodeData.setArrPerm(node.isArrPerm());

            	result.add(nodeData);
            });

            return result;
        }
        return Collections.emptyList();
	}

	/**
	 * Vytvoření predikátu podle parametrů vyhledávání.
	 * 
	 * @param factory
	 * @param searchParams
	 * @return
	 */
	private SearchPredicate createSearchPredicate(SearchPredicateFactory factory, SearchParams searchParams) {

		BooleanPredicateClausesStep<?> bool = factory.bool();

		// zpracování filtru
    	for (AbstractFilter filter : searchParams.getFilters()) {
    		if (filter instanceof MultimatchContainsFilter) {
    	        bool.must(multimatchContainsPredicate(factory, (MultimatchContainsFilter) filter));

    		} else if (filter instanceof FieldValueFilter) {
    			bool.must(fieldValuePredicate(factory, (FieldValueFilter) filter));

    		} else if (filter instanceof LogicalFilter) {
    			throw new BusinessException("Filter type 'LogicalFilter' is not yet implemented", ArrangementCode.REQUEST_INVALID);

    		} else {
    			throw new BusinessException("Not specified filter in search request", ArrangementCode.REQUEST_INVALID);
    		}
    	}

    	return bool.toPredicate();
	}

	private SearchPredicate multimatchContainsPredicate(final SearchPredicateFactory factory, final MultimatchContainsFilter filter) {
        /* rozdělení zadaného výrazu podle mezer */
        String[] tokens = StringUtils.split(filter.getValue(), ' ');

        /* hledání výsledků pomocí AND (must) tak že každý obsahuje dané části zadaného výrazu */
        BooleanPredicateClausesStep<?> bool = factory.bool();
        for (String token : tokens) {
            String searchValue = "*" + token + "*";
            SearchPredicate predicate = factory.bool().should(factory.wildcard().field(ArrDescItem.FULLTEXT_ATT).matching(searchValue)).toPredicate();
            bool.must(predicate);
        }
        return bool.toPredicate();
	}

	private SearchPredicate fieldValuePredicate(final SearchPredicateFactory factory, final FieldValueFilter filter) {
    	String itemTypeCode = ((DescItemField) filter.getField()).getTypeCode();
    	String itemSpecCode = ((DescItemField) filter.getField()).getSpecCode();

	    StaticDataProvider sdp = staticDataService.getData();
	    ItemType itemType = sdp.getItemTypeByCode(itemTypeCode.toUpperCase());
	    Objects.requireNonNull(itemType);

	    String fieldName = itemTypeCode.toLowerCase();
	    String fieldSpecName = itemSpecCode != null ? fieldSpecName = fieldName + "_" + itemSpecCode.toLowerCase() : null;
	    OperationCompareType op = filter.getOperation();
	    DataType dataType = itemType.getDataType();
		switch (dataType) {
		case INT: {
		    Integer value = Integer.parseInt(filter.getValue());
			return getPredicateByNumber(factory, fieldName, fieldSpecName, op, value);
		}
		case DECIMAL: {
		    BigDecimal value = BigDecimal.valueOf(Double.parseDouble(filter.getValue()));
			return getPredicateByNumber(factory, fieldName, fieldSpecName, op, value);
		}
		case ENUM:
			return getPredicateByEnum(factory, fieldName, itemSpecCode, filter);
		case RECORD_REF:
			return getPredicateByRecordRef(factory, fieldName, fieldSpecName, filter);
		case STRING:
		case TEXT:
			return getPredicateByStringOrText(factory, fieldName, fieldSpecName, filter);
		case UNITDATE:
			return getPredicateByUnitdate(factory, fieldName, filter);
		default:
			throw new IllegalArgumentException("Unsupported dataType: " + dataType);
		}
	}

	private <T extends Number> SearchPredicate getPredicateByNumber(final SearchPredicateFactory factory, 
											  					 	final String fieldTypeName,
							                                        final String fieldTypeSpecName,
											  					 	final OperationCompareType op,
											  					 	final T value) {
		String fieldName = fieldTypeName + (fieldTypeSpecName != null ? "_" + fieldTypeSpecName : "");
		switch (op) {
		case EQ:
			return factory.match().field(fieldName).matching(value).toPredicate();
		case NEQ:
			return factory.bool().mustNot(factory.match().field(fieldName).matching(value)).toPredicate();
		case GT:
			return factory.range().field(fieldName).greaterThan(value).toPredicate();
		case LT:
			return factory.range().field(fieldName).lessThan(value).toPredicate();
		case GTE:
			return factory.range().field(fieldName).atLeast(value).toPredicate();
		case LTE:
			return factory.range().field(fieldName).atMost(value).toPredicate();
		default:
			throw new IllegalArgumentException("Unsupported comparison operation: " + op);
		}
	}

	private SearchPredicate getPredicateByRecordRef(final SearchPredicateFactory factory,
			                                        final String fieldTypeName,
			                                        final String fieldTypeSpecName,
			                                        final FieldValueFilter filter) {
	    OperationCompareType op = filter.getOperation();
	    String value = filter.getValue().toLowerCase();
	    // find by name of ap
	    if (!value.matches("-?\\d+")) {
	    	return getPredicateByStringOrText(factory, fieldTypeName, fieldTypeSpecName, filter);
	    }
	    // find by recordId
	    Integer intValue = Integer.parseInt(value);
		switch (op) {
		case EQ:
			return factory.match().field(REL_AP_ID).matching(intValue).toPredicate();
		case NEQ:
			return factory.bool().mustNot(factory.match().field(REL_AP_ID).matching(intValue)).toPredicate();
		default:
			throw new IllegalArgumentException("Unsupported comparison operation: " + op);
		}
	}

	private SearchPredicate getPredicateByEnum(final SearchPredicateFactory factory,
			   								   final String fieldTypeName,
			   								   final String specValue,
			   								   final FieldValueFilter filter) {
	    OperationCompareType op = filter.getOperation();
		switch (op) {
		case EQ:
			return factory.match().field(fieldTypeName).matching(specValue.toLowerCase()).toPredicate();
		case NEQ:
			return factory.bool().mustNot(factory.match().field(fieldTypeName).matching(specValue.toLowerCase())).toPredicate();
		default:
			throw new IllegalArgumentException("Unsupported comparison operation: " + op);
		}
	}

	private SearchPredicate getPredicateByStringOrText(final SearchPredicateFactory factory,
													   final String fieldTypeName,
													   final String fieldTypeSpecName,
											   		   final FieldValueFilter filter) {
	    OperationCompareType op = filter.getOperation();
	    String value = filter.getValue();
	    BooleanPredicateClausesStep<?> bool = factory.bool();
		switch (op) {
		case EQ:
			bool.must(factory.match().field(fieldTypeName).matching(value));
			if (fieldTypeSpecName != null) {
				bool.must(factory.match().field(fieldTypeSpecName).matching(value));
			}
			break;
		case NEQ:
			bool.mustNot(factory.match().field(fieldTypeName).matching(value));
			if (fieldTypeSpecName != null) {
				bool.mustNot(factory.match().field(fieldTypeSpecName).matching(value));
			}
			break;
		case GT:
			bool.must(factory.range().field(fieldTypeName).greaterThan(value));
			if (fieldTypeSpecName != null) {
				bool.must(factory.range().field(fieldTypeSpecName).greaterThan(value));
			}
			break;
		case LT:
			bool.must(factory.range().field(fieldTypeName).lessThan(value));
			if (fieldTypeSpecName != null) {
				bool.must(factory.range().field(fieldTypeSpecName).lessThan(value));
			}
			break;
		case GTE:
			bool.must(factory.range().field(fieldTypeName).atLeast(value));
			if (fieldTypeSpecName != null) {
				bool.must(factory.range().field(fieldTypeSpecName).atLeast(value));
			}
			break;
		case LTE:
			bool.must(factory.range().field(fieldTypeName).atMost(value));
			if (fieldTypeSpecName != null) {
				bool.must(factory.range().field(fieldTypeSpecName).atMost(value));
			}
			break;
		case STARTWITH:
			bool.must(factory.wildcard().field(fieldTypeName).matching(value + "*"));
			if (fieldTypeSpecName != null) {
				bool.must(factory.wildcard().field(fieldTypeSpecName).matching(value + "*"));
			}
			break;
		case ENDWITH:
			bool.must(factory.wildcard().field(fieldTypeName).matching("*" + value));
			if (fieldTypeSpecName != null) {
				bool.must(factory.wildcard().field(fieldTypeSpecName).matching("*" + value));
			}
			break;
		case CONTAINS:
			bool.must(factory.wildcard().field(fieldTypeName).matching("*" + value + "*"));
			if (fieldTypeSpecName != null) {
				bool.must(factory.wildcard().field(fieldTypeSpecName).matching("*" + value + "*"));
			}
			break;
		case IS_NULL:
			throw new BusinessException("Comparison of type IS_NULL is not implemented.", ArrangementCode.REQUEST_INVALID);
		case NOT_NULL:
			throw new BusinessException("Comparison of type NOT_NULL is not implemented..", ArrangementCode.REQUEST_INVALID);
		default:
			throw new IllegalArgumentException("Unsupported comparison operation: " + op);
		}

		return bool.toPredicate();
	}

	private SearchPredicate getPredicateByUnitdate(final SearchPredicateFactory factory, 
			                                       final String fieldName,
	   		   							           final FieldValueFilter filter) {
	    OperationCompareType op = filter.getOperation();
        ArrDataUnitdate value = new ArrDataUnitdate();
        UnitDateConverter.convertToUnitDate(filter.getValue(), value);

		String fieldNormalizedFrom = fieldName + "_" + NORM_FROM;
		String fieldNormalizedTo = fieldName + "_" + NORM_TO;        
        Long normalizedFrom = value.getNormalizedFrom();
        Long normalizedTo = value.getNormalizedTo();
        BooleanPredicateClausesStep<?> bool = factory.bool();
        switch (op) {
		case EQ:
			return bool
					.must(factory.match().field(fieldNormalizedFrom).matching(normalizedFrom))
					.must(factory.match().field(fieldNormalizedTo).matching(normalizedTo))
					.toPredicate();
		case NEQ:
			return bool
					.mustNot(factory.match().field(fieldNormalizedFrom).matching(normalizedFrom))
					.toPredicate();
		case GT:
			return factory.range().field(fieldNormalizedFrom).greaterThan(normalizedTo).toPredicate();
		case LT:
			return factory.range().field(fieldNormalizedTo).lessThan(normalizedFrom).toPredicate();
		case GTE:
			// (from1, to1), (from2, to2) -> from2 > to1 OR to2 > from1
			bool.should(factory.range().field(fieldNormalizedFrom).greaterThan(normalizedTo))
				.should(factory.range().field(fieldNormalizedTo).greaterThan(normalizedFrom));
			return bool.toPredicate();
		case LTE:
			// (from1, to1), (from2, to2) -> to2 < from1 OR from2 < to1 
			bool.should(factory.range().field(fieldNormalizedTo).lessThan(normalizedFrom))
				.should(factory.range().field(fieldNormalizedFrom).lessThan(normalizedTo));
			return bool.toPredicate();
		case STARTWITH:
			throw new BusinessException("Comparison of type STARTWITH is not implemented.", ArrangementCode.REQUEST_INVALID);
		case ENDWITH:
			throw new BusinessException("Comparison of type ENDWITH is not implemented.", ArrangementCode.REQUEST_INVALID);
		case CONTAINS:
			return bool
					.must(factory.range().field(fieldNormalizedFrom).lessThan(normalizedFrom))
					.must(factory.range().field(fieldNormalizedTo).greaterThan(normalizedTo))
					.toPredicate();
		case IS_NULL:
			throw new BusinessException("Comparison of type IS_NULL is not implemented.", ArrangementCode.REQUEST_INVALID);
		case NOT_NULL:
			throw new BusinessException("Comparison of type NOT_NULL is not implemented..", ArrangementCode.REQUEST_INVALID);
		default:
			throw new IllegalArgumentException("Unsupported comparison operation: " + op);
		}
	}

	protected ArrFundToNodeList getFundToNodeListFromSession(Integer fundId) {
        Holder<Collection<ArrFundToNodeList>> holder = fundSearchSession();
        Collection<ArrFundToNodeList> list = holder.get();
        if (list == null) {
            throw new SystemException("Nenalezena session data");
        }
        for (ArrFundToNodeList fundToNodeList : list) {
            if (fundId.equals(fundToNodeList.getFundId())) {
                return fundToNodeList;
            }
        }
        return null;
    }

    public static class Holder<T> {
        private T object;

        public T get() {
            return object;
        }

        public void set(T object) {
            this.object = object;
        }
    }
}
