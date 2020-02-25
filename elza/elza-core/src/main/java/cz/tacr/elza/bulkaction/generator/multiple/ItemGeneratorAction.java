package cz.tacr.elza.bulkaction.generator.multiple;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import cz.tacr.elza.bulkaction.generator.LevelWithItems;
import cz.tacr.elza.bulkaction.generator.multiple.ItemGeneratorConfig.CreateItem;
import cz.tacr.elza.bulkaction.generator.multiple.ItemGeneratorConfig.StructuredObjectConfig;
import cz.tacr.elza.bulkaction.generator.multiple.ItemGeneratorConfig.StructuredObjectItemConfig;
import cz.tacr.elza.bulkaction.generator.result.ActionResult;
import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StructType;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataStructureRef;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrStructuredItem;
import cz.tacr.elza.domain.ArrStructuredObject;
import cz.tacr.elza.domain.ArrStructuredObject.State;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BulkActionCode;
import cz.tacr.elza.repository.StructuredObjectRepository;
import cz.tacr.elza.repository.StructuredObjectRepository.MaximalItemValues;
import cz.tacr.elza.service.DescriptionItemService;
import cz.tacr.elza.service.StructObjService;

@Component
@Scope("prototype")
public class ItemGeneratorAction extends Action {
    
    private final ItemGeneratorConfig config;

    @Autowired
    private DescriptionItemService descriptionItemService;

    WhenCondition excludeWhen;

    WhenCondition when;
    
    private class CreateAction {
        private ArrFund fund;
        private ArrFundVersion fundVersion;
        private ArrChange change;

        private Map<String, Integer> maxValues;

        private CreateItem createConfig;
        private ItemType trgItemType;
        
        private ItemType trgSOPrefixType;
        private ItemType prefixValueSource;
        private ItemType trgSOValueType;
        private ItemType countSource;
        private ItemType trgSOStartValueType;

        private StructType structType;

        public CreateAction(final CreateItem createConfig) {
            this.createConfig = createConfig; 
        }

        /**
         * Initialize create action
         * 
         * @param bulkActionRun
         */
        public void init(ArrBulkActionRun bulkActionRun) {
            fund = bulkActionRun.getFundVersion().getFund();
            fundVersion = bulkActionRun.getFundVersion();
            change = bulkActionRun.getChange();

            StaticDataProvider sdp = getStaticDataProvider();
            
            // read target type info
            trgItemType = sdp.getItemTypeByCode(createConfig.getItemType());
            if(trgItemType==null) {
                throw new SystemException("Missing item type", BulkActionCode.INCORRECT_CONFIG)
                    .set("itemType", createConfig.getItemType());
            }
            if(trgItemType.getDataType()!=DataType.STRUCTURED) {
                throw new SystemException("Only structured types can be created", BulkActionCode.INCORRECT_CONFIG)
                    .set("itemType", createConfig.getItemType());                
            } else {
                Integer structTypeId = trgItemType.getEntity().getStructuredTypeId();
                structType = sdp.getStructuredTypeById(structTypeId);
                Validate.notNull(structType);
            }
            
            StructuredObjectConfig soc = createConfig.getStructuredObject();
            if(soc==null) {
                throw new SystemException("Missing structure type config", BulkActionCode.INCORRECT_CONFIG);
            }
            init(sdp, soc);
            
            // read max values for prefixes
            List<MaximalItemValues> maxValuesList = structObjRepository.countMaximalItemValues(bulkActionRun
                    .getFundVersion().getFundId(), trgSOPrefixType.getItemTypeId(), trgSOValueType.getItemTypeId());
            // Convert to map
            maxValues = maxValuesList.stream().collect(Collectors.toMap(MaximalItemValues::getPrefix,
                                                                        MaximalItemValues::getValue));
        }

        /**
         * Initialize struct. obj specific info 
         * @param sdp 
         * @param soc
         */
        private void init(StaticDataProvider sdp, StructuredObjectConfig soc) {
            // read prefix info
            StructuredObjectItemConfig prefixConfig = soc.getPrefix();
            if(prefixConfig==null) {
                throw new SystemException("Missing prefix configuration for structured type", BulkActionCode.INCORRECT_CONFIG);
            }
            // 
            trgSOPrefixType = sdp.getItemTypeByCode(prefixConfig.getItemType());
            if(trgSOPrefixType==null) {
                throw new SystemException("Incorrect prefix configuration for structured type", BulkActionCode.INCORRECT_CONFIG);
            }
            prefixValueSource = sdp.getItemTypeByCode(prefixConfig.getValueFrom());
            if(prefixValueSource==null) {
                throw new SystemException("Incorrect prefix configuration for structured type", BulkActionCode.INCORRECT_CONFIG);
            }
            
            // main part
            StructuredObjectItemConfig mainValueConfig = soc.getMainValue();
            if(mainValueConfig==null) {
                throw new SystemException("Missing mainValue configuration for structured type", BulkActionCode.INCORRECT_CONFIG);
            }
            trgSOValueType = sdp.getItemTypeByCode(mainValueConfig.getItemType());
            if(trgSOValueType==null) {
                throw new SystemException("Incorrect configuration for structured type", BulkActionCode.INCORRECT_CONFIG);
            }
            countSource = sdp.getItemTypeByCode(mainValueConfig.getValueFrom());
            if(countSource==null) {
                throw new SystemException("Incorrect configuration for structured type", BulkActionCode.INCORRECT_CONFIG);
            }
            if (countSource.getDataType() != DataType.INT) {
                throw new SystemException("Only structured types can be created", BulkActionCode.INCORRECT_CONFIG)
                        .set("itemType", createConfig.getItemType());
            }
            trgSOStartValueType = sdp.getItemTypeByCode(mainValueConfig.getStartItemType());
            if(trgSOStartValueType==null) {
                throw new SystemException("Incorrect configuration for structured type", BulkActionCode.INCORRECT_CONFIG);
            }
            
        }

        public void apply(LevelWithItems level, TypeLevel typeLevel) {
            String prefix = null;
            // read values
            List<ArrDescItem> prefixItemList = level.getDescItems(prefixValueSource, null);
            if (prefixItemList != null && prefixItemList.size() == 1) {
                // read prefix if we have only one
                ArrDescItem prefixItem = prefixItemList.get(0);
                // 
                Integer specId = prefixItem.getItemSpecId();
                RulItemSpec spec = staticDataService.getData().getItemSpecById(specId);
                prefix = spec.getShortcut();

            }
            // read count
            List<ArrDescItem> cntItemList = level.getDescItems(countSource, null);
            int cnt = 1;
            if (cntItemList != null) {
                for (ArrDescItem adi : cntItemList) {
                    ArrData data = adi.getData();
                    if (data != null) {
                        data = HibernateUtils.unproxy(data);
                        ArrDataInteger di = (ArrDataInteger) data;
                        if (di.getValue() > cnt) {
                            cnt = di.getValue();
                        }
                    }
                }
            }
            // create struct obj
            createStructObj(level, prefix, cnt);
        }

        private void createStructObj(LevelWithItems level, String prefix, int cnt) {
            List<ArrStructuredItem> items = new ArrayList<>(3);

            // create prefix
            Validate.isTrue(trgSOPrefixType.getDataType() == DataType.STRING);
            ArrStructuredItem prefixItem = new ArrStructuredItem();
            prefixItem.setPosition(1);
            prefixItem.setItemType(trgSOPrefixType.getEntity());

            ArrDataString prefixData = new ArrDataString();
            prefixData.setDataType(trgSOPrefixType.getDataType().getEntity());
            prefixData.setValue(prefix);

            prefixItem.setData(prefixData);
            items.add(prefixItem);

            // calculate new value
            Integer curMaxValue = this.maxValues.get(prefix);
            if (curMaxValue == null) {
                curMaxValue = 0;
            }
            curMaxValue += cnt;
            maxValues.put(prefix, curMaxValue);

            if (cnt > 1) {
                // add lower band
                Validate.isTrue(trgSOStartValueType.getDataType() == DataType.INT);
                ArrStructuredItem startItem = new ArrStructuredItem();
                startItem.setPosition(1);
                startItem.setItemType(trgSOStartValueType.getEntity());

                ArrDataInteger startData = new ArrDataInteger();
                startData.setDataType(trgSOStartValueType.getDataType().getEntity());
                startData.setValue(curMaxValue - cnt + 1);

                startItem.setData(startData);
                items.add(startItem);
            }

            // create main part
            Validate.isTrue(trgSOValueType.getDataType() == DataType.INT);
            ArrStructuredItem mainItem = new ArrStructuredItem();
            mainItem.setPosition(1);
            mainItem.setItemType(trgSOValueType.getEntity());

            ArrDataInteger mainData = new ArrDataInteger();
            mainData.setDataType(trgSOValueType.getDataType().getEntity());
            mainData.setValue(curMaxValue);

            mainItem.setData(mainData);
            items.add(mainItem);

            // Create struct obj
            ArrStructuredObject structObj = structObjService.createStructObj(fund, change,
                                                                             structType.getStructuredType(),
                                                                             State.OK,
                                                                             items);

            // Connect struct obj to the level
            ArrDataStructureRef ds = new ArrDataStructureRef();
            ds.setDataType(DataType.STRUCTURED.getEntity());
            ds.setStructuredObject(structObj);

            ArrDescItem descItem = new ArrDescItem();
            descItem.setItemType(trgItemType.getEntity());
            descItem.setData(ds);
            descriptionItemService.createDescriptionItem(descItem, level.getNodeId(), fundVersion, change);
        }
        
    }
    
    List<CreateAction> actions = new ArrayList<>();

    @Autowired
    protected StructuredObjectRepository structObjRepository;
    
    @Autowired
    protected StructObjService structObjService;

    @Autowired
    public ItemGeneratorAction(final ItemGeneratorConfig itemGeneratorConfig) {
        Validate.notNull(itemGeneratorConfig);
        
        this.config = itemGeneratorConfig;
    }

    @Override
    public void init(ArrBulkActionRun bulkActionRun) {
        StaticDataProvider sdp = getStaticDataProvider();        
        
        // initialize exclude configuration
        WhenConditionConfig excludeWhenConfig = config.getExcludeWhen();
        if (excludeWhenConfig != null) {
            excludeWhen = new WhenCondition(excludeWhenConfig, sdp);
        }

        WhenConditionConfig whenConfig = config.getWhen();
        if (whenConfig != null) {
            when = new WhenCondition(whenConfig, sdp);
        }
        
        if(config.getCreate()!=null) {
            for(CreateItem createConfig: config.getCreate()) {
                CreateAction ca = new CreateAction(createConfig);
                ca.init(bulkActionRun);
                actions.add(ca);
            }
        }
        
        // find current max values
    }

    @Override
    public void apply(LevelWithItems level, TypeLevel typeLevel) {
        // check exclude condition
        if (excludeWhen != null) {
            if (excludeWhen.isTrue(level)) {
                return;
            }
        }

        // check when condition
        if (when != null) {
            if (!when.isTrue(level)) {
                return;
            }
        }

        // create items
        actions.forEach(c -> c.apply(level, typeLevel));
        
    }

    @Override
    public ActionResult getResult() {
        return null;
    }

}
