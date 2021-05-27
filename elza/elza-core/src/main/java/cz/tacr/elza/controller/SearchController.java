package cz.tacr.elza.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Functions;

import cz.tacr.elza.common.FactoryUtils;
import cz.tacr.elza.common.db.QueryResults;
import cz.tacr.elza.controller.factory.ApFactory;
import cz.tacr.elza.controller.vo.AbstractFilter;
import cz.tacr.elza.controller.vo.EntityRef;
import cz.tacr.elza.controller.vo.FieldValueFilter;
import cz.tacr.elza.controller.vo.LogicalFilter;
import cz.tacr.elza.controller.vo.MultimatchContainsFilter;
import cz.tacr.elza.controller.vo.ResultEntityRef;
import cz.tacr.elza.controller.vo.SearchFilterVO;
import cz.tacr.elza.controller.vo.SearchParams;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApIndex;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.RulPartType;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.vo.ArrFundToNodeList;
import cz.tacr.elza.drools.model.PartType;
import cz.tacr.elza.groovy.GroovyResult;
import cz.tacr.elza.repository.ApIndexRepository;
import cz.tacr.elza.repository.ApPartRepository;
import cz.tacr.elza.repository.FundRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.NodeRepositoryCustom.ArrDescItemInfo;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.LevelTreeCacheService;
import cz.tacr.elza.service.UserService;
import cz.tacr.elza.service.arr_search.ResponseBuilder;
import cz.tacr.elza.service.cache.AccessPointCacheService;
import cz.tacr.elza.service.cache.CachedAccessPoint;
import cz.tacr.elza.service.cache.CachedPart;

@RestController
public class SearchController implements SearchApi {
	
	static final Logger log = LoggerFactory.getLogger(SearchController.class);
	
	@Autowired
	AccessPointService apService;
	
	@Autowired
	StaticDataService staticDataService;
	
	@Autowired
	ApPartRepository apPartRepository;
	
	@Autowired
	ApIndexRepository apIndexRepository;
	
    @Autowired
    AccessPointCacheService apCacheService;

    /**
	 * Maximal count of items in response
	 */
	static final int MAX_RESPONSE_COUNT = 10000;
	
	static final String FIELD_APTYPE = "APTYPE";

	
	static class ApSearchParams {
		int firstResult = 0;
	    int maxResults = 200;
	    
        List<Integer> scopeIds;

		List<Integer> apTypeIds = null;
		private StaticDataProvider sdp;
		
        SearchFilterVO searchFilterVO = new SearchFilterVO();

		public ApSearchParams(final StaticDataProvider sdp) {
			this.sdp = sdp;
		}

		public boolean prepare(SearchParams searchParams) {
			// read offset
			Integer offset = searchParams.getOffset();
			if(offset!=null) {
				if(offset<0) {
					log.warn("Offset is out of range, received value: {}", offset);
					return false;					
				}
				firstResult = offset;			
			}
			Integer size = searchParams.getSize();
			if(size!=null) {
				if(size>0&&size<MAX_RESPONSE_COUNT) {
					maxResults = size;
				} else {
					log.warn("Size is out of range, received value: {}", size);
					return false;
				}
			}
			
			List<AbstractFilter> filters = searchParams.getFilters();
            if (filters != null) {
                if (!processAndFilters(filters)) {
                    return false;
                }
			}
			return true;			
		}
		
        private boolean processAndFilters(List<AbstractFilter> filters) {
			for(AbstractFilter filter: filters) {
				if(filter instanceof MultimatchContainsFilter) {
					MultimatchContainsFilter mcf = (MultimatchContainsFilter)filter;
                    if (StringUtils.isNotBlank(searchFilterVO.getSearch())) {
						// multiple fulltext values
						log.warn("Multiple fulltext fields");
						return false; 
					}
                    searchFilterVO.setSearch(mcf.getValue());
					continue;
				}
				
				if(filter instanceof FieldValueFilter) {
					FieldValueFilter fvf = (FieldValueFilter)filter;
					if(FIELD_APTYPE.equals(fvf.getField())) {
						if(!addApType(fvf.getValue())) {
							return false;
						}
					} else {
						// unrecognized field type
						log.warn("Unknown field type: {}", fvf.getField());
						return false;
					}
					continue;
				}
				
				if(filter instanceof LogicalFilter) {
					LogicalFilter lf = (LogicalFilter) filter;
					// and filters can be processed recursively
					if(LogicalFilter.OperationEnum.AND==lf.getOperation()) {
						if(!processAndFilters(lf.getFilters())) {
							return false;
						}
					} else
					if(LogicalFilter.OperationEnum.OR==lf.getOperation()) {
						if(!processOrFilters(lf.getFilters())) {
							return false;
						}						
					} else {
						log.warn("Unsupported logical operation: {}", lf.getOperation());
						return false;						
					}					
				}
			}
			return true;
		}
		private boolean addApType(String value) {
			ApType apType = sdp.getApTypeByCode(value);
			if(apType==null) {
				// unrecognized ap type
				log.warn("Unknown aptype: {}", value);
				return false;
				
			}
			if(apTypeIds!=null) {
				log.warn("AP has only one type and cannot has multiple types, unexpected value: {}", value);
				return false;							
			}
			addApType(apType);
			return true;
		}

		private boolean processOrFilters(List<AbstractFilter> filters) {
			for(AbstractFilter filter: filters) {
				if(filter instanceof MultimatchContainsFilter) {
					// unrecognized field type
					log.warn("Fulltext cannot have multiple or values");
					return false;
				}
				if(filter instanceof FieldValueFilter) {
					FieldValueFilter fvf = (FieldValueFilter)filter;
					if(FIELD_APTYPE.equals(fvf.getField())) {
						if(!addApType(fvf.getValue())) {
							return false;
						}
					} else {
						// unrecognized field type
						log.warn("Unknown field type: {}", fvf.getField());
						return false;
					}
					continue;
				}
				if(filter instanceof LogicalFilter) {
					LogicalFilter lf = (LogicalFilter) filter;
					if(LogicalFilter.OperationEnum.OR==lf.getOperation()) {
						if(!processOrFilters(lf.getFilters())) {
							return false;
						}
					} else 
					if(LogicalFilter.OperationEnum.AND==lf.getOperation()) {
						log.warn("Cannot user AND under OR");
						return false;
					} else {
						log.warn("Unsupported logical operation: {}", lf.getOperation());
						return false;						
					}					

				}
				
			}
			return true;
		}

		private void addApType(ApType apType) {
			if(apTypeIds==null) {
				apTypeIds = new ArrayList<>();
			}
			apTypeIds.add(apType.getApTypeId());
		}

		public int getFirstResult() {
			return firstResult;
		}
		public int getMaxResults() {
			return maxResults;
		}
		public List<Integer> getApTypeIds() {
			return apTypeIds;
		}

        public List<Integer> getScopeIds() {
            return scopeIds;
		}

        public SearchFilterVO getSearchFilter() {
            return searchFilterVO;
        }
		
	};

	/**
	 * Vyhledani AP
	 * 
	 * Podporovano jen fulltextove vyhledavani s moznosti limitovani tridy
	 * 
	 * Moznosti vstupu jsou:
	 * - jeden textovy filter
	 * - jeden filter na typ
	 * - filter na typ a textovy filter
	 */
	@Override
    @RequestMapping(value = { "/cuni-ais-api/search-ap",
            "/cuni-ais-api/search-ap/search",
            "/api/v1/search-ap" })
    @Transactional
	public ResponseEntity<ResultEntityRef> searchEntity(@RequestBody SearchParams searchParams) {

        log.debug("Received request on: /api/v1/search-ap, query: {}", searchParams);
		
		StaticDataProvider sdp = staticDataService.createProvider();
		final RulPartType bodyType = sdp.getPartTypeByCode(StaticDataProvider.DEFAULT_BODY_PART_TYPE);
		Validate.notNull(bodyType);
		final RulPartType nameType = sdp.getPartTypeByCode(StaticDataProvider.DEFAULT_PART_TYPE);
		
		ApSearchParams apsp = new ApSearchParams(sdp);
		if(!apsp.prepare(searchParams)) {
            log.error("Failed to prepare search parameters: {}", searchParams);
			return ResponseEntity.badRequest().build();
		}
		
		SearchFilterVO searchFilter = apsp.getSearchFilter();
		
        QueryResults<CachedAccessPoint> searchResult = apCacheService.search(searchFilter,
		                      apsp.getApTypeIds(),
                              apsp.getScopeIds(),
                              null,
                              apsp.getFirstResult(),
                              apsp.getMaxResults(),
                              sdp);

		ResultEntityRef rer = new ResultEntityRef();
        rer.setCount((long) searchResult.getRecordCount());
        if (CollectionUtils.isNotEmpty(searchResult.getRecords())) {
            List<EntityRef> rList = searchResult.getRecords().stream().map(entity -> {
                EntityRef er = new EntityRef();
                er.setId(entity.getUuid());
                
                List<CachedPart> parts = entity.getParts();
                for (CachedPart part : parts) {
                    if (part.getPartId().equals(entity.getPreferredPartId())) {
                        er.setLabel(ApFactory.findDisplayIndexValue(part.getIndices()));
                    } else {
                        if (part.getPartTypeCode().equals(PartType.PT_BODY)) {
                            er.setNote(ApFactory.findDisplayIndexValue(part.getIndices()));
                        }
                    }
                }
                
                if (StringUtils.isBlank(er.getLabel())) {
                    er.setLabel("id=" + entity.getAccessPointId());
                }
                return er;
            }).collect(Collectors.toList());
            rer.setItems(rList);
        }
		
		return ResponseEntity.ok(rer);
	}

    @Autowired
    UserService userService;

    @Autowired
    FundRepository fundRepository;

    @Autowired
    FundVersionRepository fundVersionRepository;

    @Autowired
    NodeRepository nodeRepository;

    @Autowired
    LevelTreeCacheService levelTreeCacheService;

    @Override
    @RequestMapping(value = { "/cuni-ais-api/search-arr",
            "/cuni-ais-api/search-arr/search",
            "/api/v1/search-arr" })
    @Transactional
    public ResponseEntity<ResultEntityRef> searchArchDesc(@RequestBody SearchParams searchParams) {

        log.debug("Received request on: /api/v1/search-arr, query: {}", searchParams);

        // get funds to search in
        UserDetail userDetail = userService.getLoggedUserDetail();

        // If user can read all funds then list of fund should not be prepared here at all
        List<ArrFund> fundList;
        if(userDetail.hasPermission(UsrPermission.Permission.FUND_RD_ALL) ||
                userDetail.hasPermission(UsrPermission.Permission.FUND_ADMIN) ||
                userDetail.hasPermission(UsrPermission.Permission.ADMIN)) {
            fundList = null;
            log.debug("Searching in all funds");
        } else {
            Integer userId = userDetail.getId();
            fundList = fundRepository.findFundByFulltext(null, userId);
            if (fundList.isEmpty()) {
                log.debug("No matching funds");
                ResultEntityRef rer = new ResultEntityRef();
                rer.setCount((long) 0);
                return ResponseEntity.ok(rer);
            }
            log.debug("Searching funds: {}", fundList);
        }

        // full text query
        List<AbstractFilter> filters = searchParams.getFilters();
        String searchedText = null;
        if (filters != null && filters.size() > 0) {
            if(filters.size() == 1) {                
                if (filters.get(0) instanceof MultimatchContainsFilter) {                    
                    MultimatchContainsFilter mcf = (MultimatchContainsFilter) filters.get(0);
                    searchedText = mcf.getValue();
                } else {
                    log.debug("Received unexpected search request, query: {}", searchParams);
                    return ResponseEntity.badRequest().build();                    
                }
            } else {
                log.debug("Received unexpected search request, query: {}", searchParams);
                return ResponseEntity.badRequest().build();
            }

        } else {
            // filter not specified
        }
        return searchEntityFulltext(fundList, searchParams.getOffset(), searchParams.getSize(), searchedText);
    }

    private ResponseEntity<ResultEntityRef> searchEntityFulltext(List<ArrFund> fundList,
                                                                 Integer offset, Integer size,
                                                                 String value) {
        QueryResults<ArrDescItemInfo> results = nodeRepository.findFundIdsByFulltext(value, fundList, size, offset);

        ResponseBuilder rb = new ResponseBuilder(fundVersionRepository,
                levelTreeCacheService, nodeRepository);
        ResultEntityRef rer = rb.build(results);

        return ResponseEntity.ok(rer);
    }

}
