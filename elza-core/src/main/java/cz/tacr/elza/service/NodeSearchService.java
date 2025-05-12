package cz.tacr.elza.service;

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
import cz.tacr.elza.controller.vo.FieldValueFilter;
import cz.tacr.elza.controller.vo.Fund;
import cz.tacr.elza.controller.vo.FundSearchResult;
import cz.tacr.elza.controller.vo.LogicalFilter;
import cz.tacr.elza.controller.vo.MultimatchContainsFilter;
import cz.tacr.elza.controller.vo.NodeSearchResult;
import cz.tacr.elza.controller.vo.SearchParams;
import cz.tacr.elza.controller.vo.TreeNodeVO;
import cz.tacr.elza.domain.ArrCachedNode;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFundVersion;
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
	public List<NodeSearchResult> nodeGetSearchResult(Integer fundId) {
        ArrFundToNodeList fundToNodeList = getFundToNodeListFromSession(fundId);
        if (fundToNodeList != null) {
            List<Integer> nodeIdList = fundToNodeList.getNodeIdList();
            ArrFundVersion fundVersion = arrangementInternalService.getOpenVersionByFundId(fundToNodeList.getFundId());
            List<Integer> sortedList = levelTreeCacheService.sortNodesByTreePosition(nodeIdList, fundVersion);
            List<TreeNodeVO> treeNodes = levelTreeCacheService.getNodesByIds(sortedList, fundVersion);
            
            List<NodeSearchResult> result = new ArrayList<>(treeNodes.size());
            treeNodes.forEach(node -> {
            	NodeSearchResult nodeSearch = new NodeSearchResult();
            	nodeSearch.setId(node.getId());
            	nodeSearch.setName(node.getName());
            	nodeSearch.setIcon(node.getIcon());
            	nodeSearch.setHasChildren(node.isHasChildren());
            	nodeSearch.setDepth(node.getDepth());
            	nodeSearch.setReferenceMark(Arrays.asList(node.getReferenceMark()));
            	nodeSearch.setReferenceMarkInt(Arrays.asList(node.getReferenceMarkInt()));
            	nodeSearch.setVersion(node.getVersion());
            	nodeSearch.setArrPerm(node.isArrPerm());

            	result.add(nodeSearch);
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

		// TODO add the possibility of accumulating predicates in a loop

		// zpracování filtru
    	for (AbstractFilter filter : searchParams.getFilters()) {
    		if (filter instanceof MultimatchContainsFilter) {
    	        /* rozdělení zadaného výrazu podle mezer */
    	        String[] tokens = StringUtils.split(((MultimatchContainsFilter) filter).getValue(), ' ');

    	        /* hledání výsledků pomocí AND (must) tak že každý obsahuje dané části zadaného výrazu */
    	        BooleanPredicateClausesStep<?> bool = factory.bool();
    	        for (String token : tokens) {
    	            String searchValue = "*" + token + "*";
    	            SearchPredicate predicate = factory.bool().should(factory.wildcard().field(ArrDescItem.FULLTEXT_ATT).matching(searchValue)).toPredicate();
    	            bool.must(predicate);
    	        }
    	        return bool.toPredicate();

    		} else if (filter instanceof FieldValueFilter) {
    			throw new BusinessException("Filter type 'FieldValueFilter' is not yet implemented", ArrangementCode.REQUEST_INVALID);

    		} else if (filter instanceof LogicalFilter) {
    			throw new BusinessException("Filter type 'LogicalFilter' is not yet implemented", ArrangementCode.REQUEST_INVALID);

    		} else {
    			throw new BusinessException("Not specified filter in search request", ArrangementCode.REQUEST_INVALID);
    		}
    	}

    	return null;
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
