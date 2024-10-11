package cz.tacr.elza.config.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.vo.TitleValue;
import cz.tacr.elza.domain.vo.TitleValues;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.packageimport.xml.SettingFundViews.ItemSpec;
import cz.tacr.elza.packageimport.xml.SettingFundViews.TitleConfig;
import cz.tacr.elza.service.vo.TitleItemsByType;

public class TitleBuilder {

    private String separator = " ";
    
    class TitlePart {
    	Integer itemTypeId;
    	
    	Integer maxCount;
		
		List<Integer> specs;
		
		public TitlePart(final Integer itemTypeId, final Integer maxCount, final List<Integer> specs) {
			this.itemTypeId = itemTypeId;
			this.maxCount = maxCount;
			this.specs = specs;
		}

		public Integer getItemTypeId() {
			return itemTypeId;
		}

		public Integer getMaxCount() {
			return maxCount;
		}

		public List<Integer> getSpecs() {
			return specs;
		}
    };
    
    /**
     * List of {@link TitlePart}.
     */
    private List<TitlePart> titleParts = new ArrayList<>();
    
    private Set<Integer> itemTypeIds = new HashSet<>();

    public Set<Integer> getItemTypeIds() {
        return itemTypeIds;
    }

    public void setSeparator(String separator) {
        if (separator != null) {
            this.separator = separator;
        }
    }

    public void addItem(Integer itemTypeId, Integer maxCount, List<Integer> specs) {
        itemTypeIds.add(itemTypeId);
   
        TitlePart tp = new TitlePart(itemTypeId, maxCount, specs);
		titleParts.add(tp);		
    }

    static private Integer getItemId(String code, StaticDataProvider sdp) {
        ItemType itemType = sdp.getItemTypeByCode(code);
        if (itemType == null) {
            throw new SystemException("Item type not found", BaseCode.INVALID_STATE)
                    .set("itemType.code", code);
        }
        return itemType.getItemTypeId();
    }

    static private Integer getItemSpecId(String code, StaticDataProvider sdp) {
        RulItemSpec itemSpec = sdp.getItemSpecByCode(code);
        if (itemSpec == null) {
            throw new SystemException("Item spec not found", BaseCode.INVALID_STATE)
                    .set("itemSpec.code", code);
        }
        return itemSpec.getItemSpecId();
    }

    static private List<Integer> getItemSpecIds(List<ItemSpec> specs, StaticDataProvider sdp) {
        if (CollectionUtils.isEmpty(specs)) {
            return null;
        }
        return specs.stream().map(p -> getItemSpecId(p.getType(), sdp)).collect(Collectors.toList());
    }

    public void configure(TitleConfig titleConfig, StaticDataProvider sdp) {
        setSeparator(titleConfig.getSeparator());
        if (CollectionUtils.isEmpty(titleConfig.getItems())) {
            return;
        }
        titleConfig.getItems().forEach(s -> {
            addItem(getItemId(s.getType(), sdp), s.getMaxCount(), getItemSpecIds(s.getSpecs(), sdp));
        });
    }

    public String build(TitleItemsByType itemsValueMap, ArrDao dao, String defaultTitle) {
        List<String> titleParts = new ArrayList<>();
        if(itemsValueMap != null) {
	        // iterate all title parts
	        for (TitlePart tp : this.titleParts) {
	        	Integer itemTypeId = tp.getItemTypeId();
	        	TitleValues titleValues = itemsValueMap.getTitles(itemTypeId);
	        	if (titleValues != null) {
                    // filtrace podle spec, pokud jsou uvedeny
	        		List<Integer> specs = tp.getSpecs();
	        		Integer maxCount = tp.getMaxCount();
	        		for (TitleValue titleValue : titleValues.getValues()) {
						Integer specId = titleValue.getSpecId();
						if (!CollectionUtils.isEmpty(specs) && specId != null && !specs.contains(specId)) {
							continue;
						}
						String value = titleValue.getValue();
						if (StringUtils.isNotBlank(value)) {
							titleParts.add(value);
						}
						// kontrola hodnoty maxCount, pokud je nastavena
						if (maxCount != null) {
							maxCount--;
							if (maxCount == 0) {
								break;
							}
						}
	        		}
	        	}
	        }
        }
        if (dao != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("(");
            if (StringUtils.isNotBlank(dao.getLabel())) {
                sb.append("dao: ");
                sb.append(dao.getLabel());
            } else if (StringUtils.isNotBlank(dao.getCode())) {
                sb.append("dao: ");
                sb.append(dao.getCode());
            } else {
                sb.append("daoId: ");
                sb.append(dao.getDaoId().toString());
            }
            sb.append(")");
            titleParts.add(sb.toString());
        }
        if (CollectionUtils.isEmpty(titleParts))
            return defaultTitle;
        else {
            return StringUtils.join(titleParts, separator);
        }
    }

}
