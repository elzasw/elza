package cz.tacr.elza.controller.arrangement;

import java.util.List;

import cz.tacr.elza.controller.vo.TreeNodeClient;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ItemTypeGroupVO;
import cz.tacr.elza.domain.ArrDescItem;

/**
 * Result of update operation
 *
 */
public class UpdateItemResult {
	/**
	 * Change Id (transaction) connected with the update
	 */
	protected final int changeId;

	/**
	 * Node which was updated and new version
	 */
	protected final ArrNodeVO node;

	/**
	 * Updated item
	 */
	protected final ArrItemVO item;

	/**
	 * Groups of items in the form
	 */
	protected final List<ItemTypeGroupVO> groups;

	protected final NodeTreeInfo treeInfo;

	protected final NodeFormTitle formTitle;

	public UpdateItemResult(ArrDescItem descItem, ArrItemVO descItemVo,
	        List<ItemTypeGroupVO> descItemTypeGroupsVO, TreeNodeClient tnc) {
		this.changeId = descItem.getCreateChangeId();
		this.node = new ArrNodeVO(descItem.getNode());
		this.item = descItemVo;
		this.groups = descItemTypeGroupsVO;
		this.treeInfo = new NodeTreeInfo(tnc.getIcon(), tnc.getName());
		this.formTitle = new NodeFormTitle(tnc.getAccordionLeft(), tnc.getAccordionRight());
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

	public List<ItemTypeGroupVO> getGroups() {
		return groups;
	}

	public NodeTreeInfo getTreeInfo() {
		return treeInfo;
	}

	public NodeFormTitle getFormTitle() {
		return formTitle;
	}

}
