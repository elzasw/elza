package cz.tacr.elza.dataexchange.input.sections.context;

import java.util.List;

import com.vividsolutions.jts.util.Assert;

import cz.tacr.elza.dataexchange.input.DEImportParams.ImportDirection;
import cz.tacr.elza.dataexchange.input.context.EntityIdHolder;
import cz.tacr.elza.dataexchange.input.context.ImportInitHelper;
import cz.tacr.elza.dataexchange.input.sections.context.ContextSection.SectionRootAdapter;
import cz.tacr.elza.dataexchange.input.sections.context.SectionsContext.ImportPosition;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.repository.LevelRepository;

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
    public ArrNodeWrapper createNodeWrapper(ArrNode rootNode) {
        return new ArrNodeWrapper(rootNode);
    }

    @Override
    public ArrLevelWrapper createLevelWrapper(EntityIdHolder<ArrNode> rootNodeIdHolder) {
        EntityIdHolder<ArrNode> parentNodeIdHolder = new EntityIdHolder<>(ArrNode.class);
        parentNodeIdHolder.setEntityId(importPosition.getParentLevel().getNodeId());
        return ContextNode.createLevelWrapper(rootNodeIdHolder, parentNodeIdHolder, getNextLevelPosition(), createChange);
    }

    @Override
    public void onSectionClose() {
        // NOP
    }

    private int getNextLevelPosition() {
        int levelPosition = importPosition.getLevelPosition() == null ? 0 : importPosition.getLevelPosition();
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
                // add last
                Integer max = levelRepository.findMaxPositionUnderParent(parentNode);
                levelPosition = max == null ? 1 : max + 1;
            } else {
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
}
