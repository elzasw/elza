package cz.tacr.elza.bulkaction.generator.multiple;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import cz.tacr.elza.bulkaction.BulkAction;
import cz.tacr.elza.bulkaction.generator.LevelWithItems;
import cz.tacr.elza.bulkaction.generator.result.ActionResult;
import cz.tacr.elza.bulkaction.generator.result.DateRangeActionResult;
import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrDataDecimal;
import cz.tacr.elza.domain.ArrDataStructureRef;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrStructuredItem;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.StructuredItemRepository;

/**
 * Akce na zjištění rozsahu metraze
 *
 */
@Component
@Scope("prototype")
public class SizeCounterAction extends Action {

    final private SizeCounterConfig config;
    private WhenCondition excludeWhen;
    private ItemType outputItemType;
    private ItemType storageType;
    private ItemType sizeItemType;

    private BigDecimal sum = new BigDecimal(0);

    /**
     * Skip subtree
     */
    LevelWithItems skipSubtree;

    Set<Integer> countedStructObjs = new HashSet<>();

    @Autowired
    private StructuredItemRepository structureItemRepository;

    /**
     * Flag if exists packet without size
     */
    private int packetWithoutSizeCounter = 0;
    private int packetWithSizeCounter = 0;

    public SizeCounterAction(SizeCounterConfig config) {
        this.config = config;
    }

    @Override
    public void init(BulkAction bulkAction, ArrBulkActionRun bulkActionRun) {
        super.init(bulkAction, bulkActionRun);

        StaticDataProvider sdp = this.getStaticDataProvider();

        // initialize exclude configuration
        WhenConditionConfig excludeWhenConfig = config.getExcludeWhen();
        if (excludeWhenConfig != null) {
            excludeWhen = new WhenCondition(excludeWhenConfig, sdp);
        }

        // prepare output type
        String outputType = config.getOutputType();
        if (outputType == null) {
            throw new BusinessException("Není vyplněn parametr 'output_type' v akci.", BaseCode.PROPERTY_NOT_EXIST)
                    .set(BaseCode.PARAM_PROPERTY, "outputType");
        }
        outputItemType = sdp.getItemTypeByCode(outputType);
        if (outputItemType.getDataType() != DataType.STRING) {
            throw new BusinessException(
                    "Datový typ atributu musí být " + DataType.STRING + " (item type " + outputType + ")",
                    BaseCode.ID_NOT_EXIST);
        }

        String storageType = config.getStorageType();
        if (storageType == null) {
            throw new BusinessException("Není vyplněn parametr 'storageType' v akci.", BaseCode.PROPERTY_NOT_EXIST)
                    .set(BaseCode.PARAM_PROPERTY, "storageType");
        }
        this.storageType = sdp.getItemTypeByCode(storageType);
        checkValidDataType(this.storageType, DataType.STRUCTURED);

        String sizeType = config.getSizeType();
        if (sizeType == null) {
            throw new BusinessException("Není vyplněn parametr 'sizeType' v akci.", BaseCode.PROPERTY_NOT_EXIST)
                    .set(BaseCode.PARAM_PROPERTY, "sizeType");
        }
        this.sizeItemType = sdp.getItemTypeByCode(sizeType);
        checkValidDataType(this.sizeItemType, DataType.DECIMAL);
    }

    /**
     * Mark level to be skipped
     * 
     * @param level
     */
    public void setSkipSubtree(LevelWithItems level) {
        this.skipSubtree = level;
    }

    @Override
    public void apply(LevelWithItems level, TypeLevel typeLevel) {
        // Check if node stopped
        if (skipSubtree != null) {
            if (isInTree(skipSubtree, level)) {
                return;
            }
            // reset limit
            skipSubtree = null;
        }

        // check exclude condition
        if (excludeWhen != null) {
            if (excludeWhen.isTrue(level)) {
                // set as skip
                setSkipSubtree(level);
                return;
            }
        }

        List<ArrDescItem> descItems = level.getDescItems();
        if (CollectionUtils.isEmpty(descItems)) {
            return;
        }
        for (ArrDescItem item : descItems) {
            if (!item.getItemTypeId().equals(storageType.getItemTypeId())) {
                continue;
            }
            // fetch valid items from packet
            ArrDataStructureRef dataStructObjRef = HibernateUtils.unproxy(item.getData());
            Integer packetId = dataStructObjRef.getStructuredObjectId();
            if (countedStructObjs.contains(packetId)) {
                continue;
            }
            countStructObj(packetId, level);
            countedStructObjs.add(packetId);
        }

    }

    private void countStructObj(Integer packetId, LevelWithItems level) {
        // TODO: Do filtering and counting in DB
        List<ArrStructuredItem> structObjItems = this.structureItemRepository
                .findByStructObjIdAndDeleteChangeIsNullFetchData(packetId);
        // filter only our item types
        boolean found = false;
        for (ArrStructuredItem structObjItem : structObjItems) {
            if (structObjItem.getItemTypeId().equals(this.sizeItemType.getItemTypeId())) {
                ArrDataDecimal dataDec = HibernateUtils.unproxy(structObjItem.getData());
                if (dataDec == null) {
                    continue;
                }
                sum = sum.add(dataDec.getValue());
                found = true;
            }
        }

        if (!found) {
            packetWithoutSizeCounter++;
        } else {
            packetWithSizeCounter++;
        }

    }

    @Override
    public ActionResult getResult() {
        if (packetWithSizeCounter == 0) {
            return null;
        }
        DateRangeActionResult drar = new DateRangeActionResult();
        drar.setItemType(this.outputItemType.getCode());

        StringBuilder sb = new StringBuilder();
        sb.append(sum.toString());
        if (this.packetWithoutSizeCounter == 0) {
            if (this.config.getOutputPostfix() != null) {
                sb.append(this.config.getOutputPostfix());
            }
        } else {
            if (this.config.getOutputPostfixMissing() != null) {
                sb.append(this.config.getOutputPostfixMissing());
            }
        }

        drar.setText(sb.toString());
        return drar;
    }

}
