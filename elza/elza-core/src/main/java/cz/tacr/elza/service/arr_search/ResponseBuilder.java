package cz.tacr.elza.service.arr_search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.common.db.QueryResults;
import cz.tacr.elza.controller.vo.EntityRef;
import cz.tacr.elza.controller.vo.ResultEntityRef;
import cz.tacr.elza.controller.vo.TreeNodeVO;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.NodeRepositoryCustom.ArrDescItemInfo;
import cz.tacr.elza.service.LevelTreeCacheService;

public class ResponseBuilder {
    List<ArrDescItemInfo> itemList = new ArrayList<>();
	
	final FundVersionRepository fundVersionRepository;
	
	final LevelTreeCacheService levelTreeCacheService;
	
	final NodeRepository nodeRepository;
	
	int cnt = 0;

	public ResponseBuilder(final FundVersionRepository fundVersionRepository, 
                           final LevelTreeCacheService levelTreeCacheService,
                           final NodeRepository nodeRepository) {
		this.fundVersionRepository = fundVersionRepository;
		this.levelTreeCacheService = levelTreeCacheService;
		this.nodeRepository = nodeRepository;
	}

	/**
	 * Build result
	 * @param results
	 * @return
	 */
    public ResultEntityRef build(QueryResults<ArrDescItemInfo> results) {
        cnt = results.getRecordCount();
        itemList.addAll(results.getRecords());
		
		ResultEntityRef rer = new ResultEntityRef();
		rer.setCount(Long.valueOf(cnt));
				
		// set data
        for (ArrDescItemInfo item : this.itemList) {
			// read fund data
			ArrFundVersion fundVer = fundVersionRepository.findByFundIdAndLockChangeIsNull(item.getFundId());
            Validate.notNull(fundVer, "Fund not found, foundId: " + item.getFundId());
			
            List<Integer> nodeIds = Collections.singletonList(item.getNodeId()); // item.getNodeIdList();
			Collection<TreeNodeVO> details = levelTreeCacheService.getFaTreeNodes(fundVer.getFundVersionId(), nodeIds);
			
			// map IDS to UUID
			List<ArrNode> nodes = this.nodeRepository.findAllById(nodeIds);
			Map<Integer, String> idUUIDMap = nodes.stream().collect(Collectors.toMap(ArrNode::getNodeId, 
					ArrNode::getUuid));
			
			for(TreeNodeVO detail: details) {
				EntityRef er = new EntityRef();
				er.setId(idUUIDMap.get(detail.getId()));
				er.setLabel(detail.getName());
				er.setNote(fundVer.getFund().getName());
				rer.addItemsItem(er);
			}
		}
		return rer;
	}
}
