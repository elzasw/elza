package cz.tacr.elza.controller.arrangement;

import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.nodes.ItemTypeLiteVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemVO;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.service.LevelTreeCacheService;

import java.util.List;

/**
 * Result of update operation
 *
 */
public class UpdateItemResult {
	/**
	 * Change Id (transaction) connected with the update
	 */
	protected int changeId;

	/**
	 * Node which was updated and new version
	 */
	protected ArrNodeVO node;

	/**
	 * Updated item
	 */
	protected ArrItemVO item;

	protected NodeTreeInfo treeInfo;

	protected NodeFormTitle formTitle;

    protected List<ItemTypeLiteVO> itemTypes;

    public UpdateItemResult() {

    }

    public UpdateItemResult(ArrDescItem descItem, ArrItemVO descItemVo,
                            List<ItemTypeLiteVO> itemTypes, LevelTreeCacheService.Node node) {
		this.changeId = descItem.getCreateChangeId();
		this.node = ArrNodeVO.valueOf(descItem.getNode());
		this.item = descItemVo;
		this.itemTypes = itemTypes;
		this.treeInfo = new NodeTreeInfo(node.getIcon(), node.getName());
		this.formTitle = new NodeFormTitle(node.getAccordionLeft(), node.getAccordionRight());
	}

	public int getChangeId() {
		return changeId;
	}

	public ArrNodeVO getNode() {
		return node;
	}

	public ArrItemVO getItem() {
		return item;
	}

    public List<ItemTypeLiteVO> getItemTypes() {
        return itemTypes;
    }

    public NodeTreeInfo getTreeInfo() {
		return treeInfo;
	}

	public NodeFormTitle getFormTitle() {
		return formTitle;
	}

}
