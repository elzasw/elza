package cz.tacr.elza.controller.factory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.controller.config.ClientFactoryDO;
import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.FormItemType;
import cz.tacr.elza.controller.vo.OutputDef;
import cz.tacr.elza.controller.vo.OutputItem;
import cz.tacr.elza.controller.vo.OutputState;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ArrOutputItem;
import cz.tacr.elza.domain.ArrOutputResult;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;

@Component
public class OutputFactory {

    @Autowired
    private StaticDataService staticDataService;

    @Autowired
    private ClientFactoryDO clientFactoryDO;

    @Autowired
    private ClientFactoryVO clientFactoryVO;

    /**
     * ArrOutput -> OutputDef.
     */
    public OutputDef createDef(final ArrOutput output) {
        Objects.requireNonNull(output, "Výstup musí být vyplněn");

        OutputDef def = new OutputDef();
        def.setId(output.getOutputId());
        def.setInternalCode(output.getInternalCode());
        def.setName(output.getName());
        def.setState(OutputState.valueOf(output.getState().name()));
        def.setError(output.getError());
        def.setOutputTypeId(output.getOutputType().getOutputTypeId());
        def.setVersion(output.getVersion());
        def.setOutputSettings(output.getOutputSettings());

        def.setCreateDate(output.getCreateChange().getChangeDate());
        if (output.getDeleteChange() != null) {
            def.setDeleteDate(output.getDeleteChange().getChangeDate());
        }

        if (CollectionUtils.isNotEmpty(output.getOutputResults())) {
            def.setOutputResultIds(output.getOutputResults().stream()
                    .map(ArrOutputResult::getOutputResultId)
                    .collect(Collectors.toList()));
            def.setGeneratedDate(output.getOutputResults().get(0).getChange().getChangeDate());
        } else {
            def.setOutputResultIds(Collections.emptyList());
        }

        if (output.getOutputFilter() != null) {
            def.setOutputFilterId(output.getOutputFilter().getOutputFilterId());
        }
        return def;
    }

    /**
     * ArrOutputItem (resp. libovolný ArrItem) -> OutputItem (OpenAPI VO).
     */
    public <T extends ArrItem> OutputItem createOutputItem(final T item) {
        Assert.notNull(item, "Hodnota musí být vyplněna");

        OutputItem outputItem = new OutputItem();
        outputItem.setId(item.getItemId());
        outputItem.setItemTypeId(item.getItemTypeId());
        outputItem.setItemSpecId(item.getItemSpecId());
        outputItem.setItemObjectId(item.getDescItemObjectId());
        outputItem.setPosition(item.getPosition());
        outputItem.setReadOnly(item.getReadOnly());

        ArrData arrData = HibernateUtils.unproxy(item.getData());
        if (arrData == null) {
            outputItem.setUndefined(true);
        } else {
            StaticDataProvider sdp = staticDataService.getData();
            RulItemType itemType = sdp.getItemType(item.getItemTypeId());
            DataType dataType = DataType.fromId(itemType.getDataTypeId());
            outputItem.setData(clientFactoryVO.convertData(arrData, dataType));
        }
        return outputItem;
    }

    public List<OutputItem> createOutputItems(final List<ArrOutputItem> items) {
        if (items == null) {
            return null;
        }
        List<OutputItem> result = new ArrayList<>(items.size());
        for (ArrOutputItem item : items) {
            result.add(createOutputItem(item));
        }
        return result;
    }

    /**
     * OutputItem (OpenAPI VO) -> ArrOutputItem. itemTypeId se bere z VO.
     */
    public ArrOutputItem createOutputItem(final OutputItem outputItem) {
        Assert.notNull(outputItem, "Výstup musí být vyplněn");

        ArrOutputItem entity = new ArrOutputItem();
        entity.setItemId(outputItem.getId());
        entity.setDescItemObjectId(outputItem.getItemObjectId());
        entity.setPosition(outputItem.getPosition());

        StaticDataProvider sdp = staticDataService.getData();
        var itemType = sdp.getItemTypeById(outputItem.getItemTypeId());
        if (itemType == null) {
            throw new BusinessException("Failed to get item type, itemTypeId: " + outputItem.getItemTypeId(),
                    BaseCode.ID_NOT_EXIST);
        }
        entity.setItemType(itemType.getEntity());

        if (outputItem.getItemSpecId() != null) {
            var itemSpec = itemType.getItemSpecById(outputItem.getItemSpecId());
            if (itemSpec == null) {
                throw new BusinessException("Failed to get item spec, itemTypeId: " + outputItem.getItemTypeId()
                        + ", itemSpecId: " + outputItem.getItemSpecId(), BaseCode.ID_NOT_EXIST);
            }
            entity.setItemSpec(itemSpec);
        }

        if (!Boolean.TRUE.equals(outputItem.getUndefined())) {
            entity.setData(clientFactoryDO.createArrData(outputItem.getData()));
        }
        return entity;
    }

    public List<FormItemType> createFormItemTypes(final String ruleCode, 
    		                                      final Integer fundId,
                                                  final List<RulItemTypeExt> itemTypes) {
        return clientFactoryVO.createFormItemTypes(ruleCode, fundId, itemTypes);
    }
}