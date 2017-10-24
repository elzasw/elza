package cz.tacr.elza.bulkaction.generator;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.google.common.primitives.Ints;

import cz.tacr.elza.bulkaction.ActionRunContext;
import cz.tacr.elza.bulkaction.BulkAction;
import cz.tacr.elza.bulkaction.generator.result.Result;
import cz.tacr.elza.bulkaction.generator.result.TestDataGeneratorResult;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.vo.NodeTypeOperation;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.ArrangementServiceInternal;
import cz.tacr.elza.service.IEventNotificationService;
import cz.tacr.elza.service.RuleService;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.events.EventType;

/**
 * Bulk action to generate test data
 *
 * @author Petr Pytelka
 *
 */
public class TestDataGenerator extends BulkAction {

    /**
     * Action type
     */
    public static final String TYPE = "TEST_DATA_GENERATOR";

    /**
     * Number of units to generate on given level
     */
    int [] unitsToGenerate = {10};
    int activeLevel = 0;

    /**
     * Změna
     */
    private ArrChange change;

	private ArrFundVersion version;

	@Autowired
	ArrangementServiceInternal arrangementInternal;

    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private ArrangementService arrangementService;
    @Autowired
    private IEventNotificationService eventNotificationService;
    @Autowired
    private RuleService ruleService;

	TestDataConfig config;

	public TestDataGenerator(TestDataConfig testDataConfig) {
		Validate.notNull(testDataConfig);

		this.config = testDataConfig;
	}

	private void init() {
		List<Integer> unitsCount = config.getItemsToGenerate();
		unitsToGenerate = Ints.toArray(unitsCount);
		if(unitsToGenerate.length==0) {
			throw new BusinessException("empty configuration of items_to_generate", BaseCode.PROPERTY_NOT_EXIST)
			        .set("property", "itemsToGenerate");
		}
	}

	@Override
	@Transactional
	public void run(ActionRunContext runContext) {
		init();

		ArrBulkActionRun arrBulkActionRun = runContext.getBulkActionRun();

		this.change = arrBulkActionRun.getChange();
		this.version = arrBulkActionRun.getFundVersion();

		Validate.notNull(version);
        checkVersion(version);
		ArrNode rootNode = version.getRootNode();

		for (Integer nodeId : runContext.getInputNodeIds())
		{
			// get node
            ArrNode node = nodeRepository.findOne(nodeId);
            Assert.notNull(nodeId, "Node s nodeId=" + nodeId + " neexistuje");
            ArrLevel level = levelRepository.findNodeInRootTreeByNodeId(node, rootNode, null);
            Assert.notNull(level, "Level neexistuje, nodeId=" + node.getNodeId() + ", rootNodeId=" + rootNode.getNodeId());

            generate(level);
		}

		// prepare result
        Result resultBA = new Result();
        TestDataGeneratorResult result = new TestDataGeneratorResult();
        //result.setCountChanges(countChanges);
        resultBA.getResults().add(result);
		arrBulkActionRun.setResult(resultBA);
	}

	private void generate(ArrLevel parentLevel) {
		// prepare list of source nodes
		List<ArrLevel> childNodes = this.getChildren(parentLevel);

        if(childNodes.size()==0) {
        	throw new SystemException("Selected node has no sub items to copy.");
        }

        List<ArrLevel> createdLevels = makeCopies(parentLevel, childNodes);

        entityManager.flush(); //aktualizace verzí v nodech
        for(ArrLevel createdLevel: createdLevels) {
        	// Do final notifications to client
        	ruleService.conformityInfo(version.getFundVersionId(), Arrays.asList(createdLevel.getNode().getNodeId()),
                NodeTypeOperation.CONNECT_NODE, null, null, null);
        }
	}

	/**
	 * Function
	 * @param futureParent
	 * @param srcChildren
	 * @return
	 */
	private List<ArrLevel> makeCopies(ArrLevel futureParent, List<ArrLevel> srcChildren) {
		//
		int copiesCount = unitsToGenerate[activeLevel%unitsToGenerate.length];

		// get current children
		List<ArrLevel> currChildNodes = this.getChildren(futureParent);
		// positions are numbered from 1
		int pos = currChildNodes.size()+1;

		List<ArrLevel> result = new LinkedList<>();

		activeLevel++;
		for(int i = 0; i<copiesCount; i++)
		{
			// make single copy
			result.addAll(copyLevels(srcChildren, pos, futureParent));
			pos+=srcChildren.size();
		}
		activeLevel--;

		return result;
	}

	private Collection<? extends ArrLevel> copyLevels(List<ArrLevel> childLevels, int startPos, ArrLevel parentLevel) {

		List<ArrLevel> result = new LinkedList<>();

		int pos = startPos;
		// Copy child nodes
		for(ArrLevel srcLevel: childLevels)
		{
			ArrLevel newLevel = this.arrangementService.createLevel(this.change, parentLevel.getNode(), pos, version.getFund());

        	eventNotificationService
            .publishEvent(EventFactory.createAddNodeEvent(EventType.ADD_LEVEL_UNDER, version, parentLevel, newLevel));

			result.add(newLevel);

			// copy descr.items
			this.copyDescrItems(srcLevel, newLevel);

			// copy childLevels
			List<ArrLevel> subLevels = this.getChildren(srcLevel);
			if(subLevels.size()>0) {
				makeCopies(newLevel, subLevels);
			}
			pos++;
		}

		return result;
	}

	/**
	 * Copy all description items
	 * @param srcLevel
	 * @param trgLevel
	 */
	private void copyDescrItems(ArrLevel srcLevel, ArrLevel trgLevel) {
		List<ArrDescItem> sourceDescItems = arrangementInternal.getDescItems(version, srcLevel.getNode());
		descriptionItemService.copyDescItemWithDataToNode(trgLevel.getNode(), sourceDescItems, this.change, version);
	}

	@Override
	public String toString() {
		return "TestDataGenerator";
	}

}
