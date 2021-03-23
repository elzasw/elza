package cz.tacr.elza.service.arr_search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.stream.Collectors;

import cz.tacr.elza.controller.vo.EntityRef;
import cz.tacr.elza.controller.vo.ResultEntityRef;
import cz.tacr.elza.controller.vo.TreeNodeVO;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.vo.ArrFundToNodeList;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.service.LevelTreeCacheService;

public class ResponseBuilder {
	List<ArrFundToNodeList> itemList = new ArrayList<>();
	
	final FundVersionRepository fundVersionRepository;
	
	final LevelTreeCacheService levelTreeCacheService;
	
	final NodeRepository nodeRepository;
	
	int cnt = 0;
	
	final int offset;
	final int pageSize;
	
	public ResponseBuilder(final FundVersionRepository fundVersionRepository, 
			final LevelTreeCacheService levelTreeCacheService,
			final NodeRepository nodeRepository,
			final Integer offset, final Integer size) {
		if(offset!=null) {
			this.offset = offset.intValue();
		} else {
			this.offset = 0;
		}
		if(size!=null) {
			this.pageSize = size.intValue();
		} else {
			this.pageSize = 200;
		}
		this.fundVersionRepository = fundVersionRepository;
		this.levelTreeCacheService = levelTreeCacheService;
		this.nodeRepository = nodeRepository;
	}

	/**
	 * Build result
	 * @param results
	 * @return
	 */
	public ResultEntityRef build(List<ArrFundToNodeList> results) {
		prepareItems(results);
		
		ResultEntityRef rer = new ResultEntityRef();
		rer.setCount(Long.valueOf(cnt));
				
		// set data
		for(ArrFundToNodeList item: this.itemList) {
			// read fund data
			ArrFundVersion fundVer = fundVersionRepository.findByFundIdAndLockChangeIsNull(item.getFundId());
			
			List<Integer> nodeIds = item.getNodeIdList();
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

	private void prepareItems(List<ArrFundToNodeList> results) {
		if(results.size()==0) {
			return;
		}
		// Move to offset		
		ListIterator<ArrFundToNodeList> iter = results.listIterator();
		
		ArrFundToNodeList activeNodeList = iter.next();
		// prepare prefix
		int numSkip = offset;
		int listPos = 0;
		int nodeCnt=activeNodeList.getNodeCount();
		while(numSkip>0) {
			// prepare pos
			if(nodeCnt>numSkip) {
				listPos = numSkip;
				break;
			}
			// skip all
			numSkip-=nodeCnt;
			
			if(!iter.hasNext()) {
				// not enough items -> return back
				return;
			}
			activeNodeList = iter.next();
			nodeCnt = activeNodeList.getNodeCount();
		}
		
		// do body
		//int startFromActList = nodeCnt-numAddFromList;
		int numShouldAdd = pageSize;
		int toIndex=nodeCnt;
		while(numShouldAdd>0) {
			// add items
			int numAvailable = nodeCnt-listPos;
			if(numAvailable>numShouldAdd) {
				toIndex = listPos+numShouldAdd;
			} else {
				toIndex = nodeCnt;
			}

			// add sub list
			List<Integer> sublist = activeNodeList.getNodeIdList().subList(listPos, toIndex);
			add(activeNodeList.getFundId(), sublist);
				
			int numProcessed=toIndex-listPos;

			cnt+=numProcessed;
			numShouldAdd-=numProcessed;
			
			// stop if not all element were processed
			listPos = 0;
			if(toIndex<nodeCnt) {
				break;
			}
			
			// prepare next list
			if(!iter.hasNext()) {
				// not enough items -> return back
				return;
			}
			activeNodeList = iter.next();
			nodeCnt = activeNodeList.getNodeCount();
		}
		
		// prepare postfix
		while(true) {
			// add non processed nodes
			cnt+=nodeCnt-toIndex;
			// prepare next list
			if(!iter.hasNext()) {
				// not enough items -> return back
				return;
			}
			activeNodeList = iter.next();
			nodeCnt = activeNodeList.getNodeCount();
		}
	}

	private void add(Integer fundId, List<Integer> sublist) {
		ArrFundToNodeList afnl = new ArrFundToNodeList(fundId, sublist);
	
		itemList.add(afnl);
	}
}
