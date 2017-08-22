package cz.tacr.elza.deimport.sections;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import cz.tacr.elza.deimport.DEImportException;
import cz.tacr.elza.deimport.aps.context.RecordImportInfo;
import cz.tacr.elza.deimport.context.ImportContext;
import cz.tacr.elza.deimport.processor.ItemProcessor;
import cz.tacr.elza.deimport.sections.context.ContextNode;
import cz.tacr.elza.deimport.sections.context.ContextSection;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeRegister;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.schema.v2.AbstractDescriptionItem;
import cz.tacr.elza.schema.v2.AccessPointRefs;
import cz.tacr.elza.schema.v2.Level;

public class SectionLevelProcessor implements ItemProcessor {

    private final ImportContext context;

    private final ContextSection section;

    public SectionLevelProcessor(ImportContext context) {
        this.context = context;
        this.section = context.getSections().getCurrentSection();
    }

    @Override
    public void process(Object item) {
        Level level = (Level) item;
        validateLevel(level);
        ArrNode node = createNode(level);
        ContextNode contextNode = createContextNode(level, node);
        processSubEntities(level, contextNode);
    }

    private void validateLevel(Level item) {
        if (StringUtils.isEmpty(item.getId())) {
            throw new DEImportException("Fund level id is not set");
        }
    }

    private ArrNode createNode(Level item) {
        ArrNode node = new ArrNode();
        node.setFund(section.getFund());
        node.setLastUpdate(section.getCreateChange().getChangeDate());
        if (StringUtils.isBlank(item.getUuid())) {
            node.setUuid(section.generateNodeUuid());
        } else {
            node.setUuid(item.getUuid());
        }
        return node;
    }

    /**
     * Create context node and sets ArrLevel.
     */
    private ContextNode createContextNode(Level item, ArrNode node) {
        String importId = item.getId();
        String parentImportId = item.getPid();

        if (StringUtils.isEmpty(parentImportId)) {
            if (section.isRootSet()) {
                throw new DEImportException("Level parent id is not set, levelId:" + importId);
            }
            return section.addRootNode(node, importId);
        }
        ContextNode parentNode = section.getContextNode(parentImportId);
        if (parentNode == null) {
            throw new DEImportException("Parent for level not found, parentLevelId:" + parentImportId);
        }
        return parentNode.addChildNode(node, importId);
    }

    private void processSubEntities(Level item, ContextNode node) {
        try {
            processAccessPointRefs(item.getAprs(), node);
            processDescItems(item.getDeOrDiOrDd(), node);
        } catch (DEImportException e) {
            throw new DEImportException(
                    "Fund level cannot be processed, levelId:" + item.getId() + ", detail:" + e.getMessage());
        }
    }

    private void processAccessPointRefs(AccessPointRefs references, ContextNode node) {
        if (references == null) {
            return;
        }
        for (String apeId : references.getApid()) {
            RecordImportInfo recordInfo = context.getAccessPoints().getRecordInfo(apeId);
            if (recordInfo == null) {
                throw new DEImportException("Referenced access point not found, apeId:" + apeId);
            }
            ArrNodeRegister nodeRegister = new ArrNodeRegister();
            nodeRegister.setRecord(recordInfo.getEntityRef(context.getSession(), RegRecord.class));
            nodeRegister.setCreateChange(section.getCreateChange());
            node.addNodeRegister(nodeRegister);
        }
    }

    private void processDescItems(List<AbstractDescriptionItem> descItems, ContextNode node) {
        for (AbstractDescriptionItem descItem : descItems) {
            descItem.importItem(node, context);
        }
    }
}
