package cz.tacr.elza.dataexchange.input.sections.context;

import java.util.List;

import com.vividsolutions.jts.util.Assert;

import cz.tacr.elza.dataexchange.input.DEImportParams.ImportDirection;
import cz.tacr.elza.dataexchange.input.context.ImportInitHelper;
import cz.tacr.elza.dataexchange.input.context.SimpleIdHolder;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.repository.LevelRepository;

/**
 * Adapter to import sections into existing fund
 * 
 *
 */
class SubsectionRootAdapter implements SectionRootAdapter {

    private final ImportPosition importPosition;

    private final ArrChange createChange;

    private final LevelRepository levelRepository;

    public SubsectionRootAdapter(ImportPosition importPosition,
                                 ArrChange createChange,
                                 ImportInitHelper initHelper) {
        this.importPosition = importPosition;
        this.createChange = createChange;
        this.levelRepository = initHelper.getLevelRepository();
    }


    @Override
    public ArrFund getFund() {
        return importPosition.getFundVersion().getFund();
    }

    @Override
    public void onSectionClose() {
        // NOP
    }

    private int getNextLevelPosition() {
        int levelPosition;
        if (importPosition.getLevelPosition() == null) {
            levelPosition = 0;
        } else {
            levelPosition = importPosition.getLevelPosition();
        }
        ArrNode parentNode = importPosition.getParentLevel().getNode();

        if (importPosition.getTargetLevel() != null) {
            if (levelPosition == 0) {
                levelPosition = importPosition.getTargetLevel().getPosition();
                Assert.isTrue(levelPosition > 0);
                if (importPosition.getDirection() == ImportDirection.BEFORE) {
                    levelPosition--;
                }
            }
            List<ArrLevel> moveLevels = levelRepository.findByParentNodeAndPositionGreaterThanOrderByPositionAsc(parentNode,
                    levelPosition);
            moveLevels(moveLevels);
            levelPosition++;
        } else {
            if (importPosition.getDirection() == ImportDirection.AFTER) {
                if (levelPosition == 0) {
                    // first iteration -> add last from DB
                    Integer max = levelRepository.findMaxPositionUnderParent(parentNode);
                    levelPosition = max == null ? 1 : max + 1;
                } else {
                    levelPosition++;
                }
            } else {
                // This is probably broken for adding multiple items without root
                // This code will work for adding single item
                // add first
                List<ArrLevel> moveLevels = levelRepository.findByParentNodeAndPositionGreaterThanOrderByPositionAsc(parentNode,
                        levelPosition);
                moveLevels(moveLevels);
                levelPosition++;
            }
        }
        // update position for future iterations
        importPosition.setLevelPosition(levelPosition);

        return levelPosition;
    }

    private void moveLevels(List<ArrLevel> levels) {
        for (ArrLevel level : levels) {
            level.setPosition(level.getPosition() + 1);
            levelRepository.save(level);
        }
    }

	@Override
	public NodeContext createRoot(SectionContext contextSection, ArrNode rootNode, String importNodeId) {
		ArrNodeWrapper nodeWrapper = new ArrNodeWrapper(rootNode);

		// set existing node from importPosition as parent 
		SimpleIdHolder<ArrNode> parentNodeIdHolder = new SimpleIdHolder<>(ArrNode.class);
		parentNodeIdHolder.setEntityId(importPosition.getParentLevel().getNodeId());
		ArrLevelWrapper levelWrapper = NodeContext.createLevelWrapper(nodeWrapper.getIdHolder(), parentNodeIdHolder,
		        getNextLevelPosition(), createChange);

		return contextSection.addNode(nodeWrapper, levelWrapper, importNodeId, 0);
	}
}
