package cz.tacr.elza.controller.factory;

import java.util.ArrayList;
import java.util.Arrays;
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
import cz.tacr.elza.controller.vo.ApScopeVO;
import cz.tacr.elza.controller.vo.ArrOutputRestrictionScopeVO;
import cz.tacr.elza.controller.vo.ArrOutputTemplateVO;
import cz.tacr.elza.controller.vo.FormItemType;
import cz.tacr.elza.controller.vo.NodeTreeData;
import cz.tacr.elza.controller.vo.OutputDef;
import cz.tacr.elza.controller.vo.OutputItem;
import cz.tacr.elza.controller.vo.OutputRestrictionScope;
import cz.tacr.elza.controller.vo.OutputState;
import cz.tacr.elza.controller.vo.OutputTemplate;
import cz.tacr.elza.controller.vo.OutputType;
import cz.tacr.elza.controller.vo.Scope;
import cz.tacr.elza.controller.vo.TreeNodeVO;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrNodeOutput;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ArrOutputItem;
import cz.tacr.elza.domain.ArrOutputResult;
import cz.tacr.elza.domain.ArrOutputTemplate;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.RulOutputType;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.service.LevelTreeCacheService;
import cz.tacr.elza.service.OutputServiceInternal;

@Component
public class OutputFactory {

    @Autowired
    private StaticDataService staticDataService;

    @Autowired
    private ClientFactoryDO clientFactoryDO;

    @Autowired
    private ClientFactoryVO clientFactoryVO;

    @Autowired
    private OutputServiceInternal outputServiceInternal;

    @Autowired
    private LevelTreeCacheService levelTreeCacheService;

    @Autowired
    private ApFactory apFactory;

    /**
     * Extended ArrOutput -> OutputDef. Fills additional fields (templateIds, nodes,
     * scopes, anonymizedAp) beyond {@link #createDef(ArrOutput)}.
     */
    public OutputDef createDefExt(final ArrOutput output, final ArrFundVersion fundVersion) {
        OutputDef def = createDef(output);

        List<ArrOutputTemplate> outputTemplates = outputServiceInternal.getOutputTemplates(output);
        if (CollectionUtils.isNotEmpty(outputTemplates)) {
            def.setTemplateIds(outputTemplates.stream()
                    .map(ArrOutputTemplate::getTemplateId)
                    .collect(Collectors.toList()));
        } else {
            def.setTemplateIds(Collections.emptyList());
        }

        List<ArrNodeOutput> outputNodes = outputServiceInternal.getOutputNodes(output, fundVersion.getLockChange());
        List<Integer> nodeIds = outputNodes.stream().map(ArrNodeOutput::getNodeId).collect(Collectors.toList());
        def.setNodes(mapNodes(levelTreeCacheService.getNodesByIds(nodeIds, fundVersion)));

        def.setScopes(mapScopes(outputServiceInternal.getRestrictedScopeVOs(output)));

        ApAccessPoint anonymizedAp = output.getAnonymizedAp();
        if (anonymizedAp != null) {
            def.setAnonymizedAp(apFactory.createVO(anonymizedAp));
        }
        return def;
    }

    private List<NodeTreeData> mapNodes(final List<TreeNodeVO> nodes) {
        if (nodes == null) {
            return Collections.emptyList();
        }
        return nodes.stream().map(src -> {
            NodeTreeData ntd = new NodeTreeData();
            ntd.setId(src.getId());
            ntd.setDepth(src.getDepth());
            ntd.setName(src.getName());
            ntd.setIcon(src.getIcon());
            ntd.setHasChildren(src.isHasChildren());
            ntd.setVersion(src.getVersion());
            ntd.setArrPerm(src.isArrPerm());
            if (src.getReferenceMark() != null) {
                ntd.setReferenceMark(Arrays.asList(src.getReferenceMark()));
            }
            return ntd;
        }).collect(Collectors.toList());
    }

    private List<Scope> mapScopes(final List<ApScopeVO> scopes) {
        if (scopes == null) {
            return Collections.emptyList();
        }
        return scopes.stream().map(src -> {
            Scope scope = new Scope();
            scope.setId(src.getId());
            scope.setCode(src.getCode());
            scope.setName(src.getName());
            scope.setLanguage(src.getLanguage());
            scope.setRuleSetCode(src.getRuleSetCode());
            return scope;
        }).collect(Collectors.toList());
    }

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
     * List<ArrOutput> -> List<OutputDef>
     */
    public List<OutputDef> createDefList(final List<ArrOutput> outputs) {
        if (outputs == null) {
            return Collections.emptyList();
        }
        return outputs.stream().map(this::createDef).collect(Collectors.toList());
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

    public OutputType createOutputType(final RulOutputType outputType) {
        Objects.requireNonNull(outputType, "Typ výstupu musí být vyplněn");
        OutputType vo = new OutputType();
        vo.setId(outputType.getOutputTypeId());
        vo.setCode(outputType.getCode());
        vo.setName(outputType.getName());
        return vo;
    }

    public List<OutputType> createOutputTypes(final List<RulOutputType> outputTypes) {
        if (outputTypes == null) {
            return Collections.emptyList();
        }
        return outputTypes.stream().map(this::createOutputType).collect(Collectors.toList());
    }

    public OutputRestrictionScope createRestrictionScope(final ArrOutputRestrictionScopeVO src) {
        Objects.requireNonNull(src, "Restrikce scope musí být vyplněna");
        OutputRestrictionScope vo = new OutputRestrictionScope();
        vo.setId(src.getId());
        vo.setOutputId(src.getOutputId());
        vo.setScopeId(src.getScopeId());
        return vo;
    }

    public OutputTemplate createTemplate(final ArrOutputTemplateVO src) {
        Objects.requireNonNull(src, "Šablona výstupu musí být vyplněna");
        OutputTemplate vo = new OutputTemplate();
        vo.setId(src.getId());
        vo.setOutputId(src.getOutputId());
        vo.setTemplateId(src.getTemplateId());
        return vo;
    }
}