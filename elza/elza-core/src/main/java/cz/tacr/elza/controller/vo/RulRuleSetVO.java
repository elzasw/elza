package cz.tacr.elza.controller.vo;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import cz.tacr.elza.core.data.RuleSet;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.packageimport.xml.SettingGridView;
import cz.tacr.elza.packageimport.xml.SettingGridView.ItemType;


/**
 * VO pro pravidla s typy výstupů.
 *
 * @since 7. 1. 2016
 */
public class RulRuleSetVO {

    private Integer id;

    private String code;

    private String name;

    private RulRuleSet.RuleType ruleType;

    /** Kódy atributů pro zobrazení v gridu hromadných úprav */
    private List<GridView> gridViews;

    public RulRuleSetVO() {

    }

    public RulRuleSetVO(RulRuleSet ruleSet) {
        this.id = ruleSet.getRuleSetId();
        this.code = ruleSet.getCode();
        this.name = ruleSet.getName();
        this.ruleType = ruleSet.getRuleType();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RulRuleSet.RuleType getRuleType() {
        return ruleType;
    }

    public void setRuleType(final RulRuleSet.RuleType ruleType) {
        this.ruleType = ruleType;
    }

    public List<GridView> getGridViews() {
        return gridViews;
    }

    public void setGridViews(final List<GridView> gridViews) {
        this.gridViews = gridViews;
    }

    public static class GridView {

        /**
         * ID type
         */
        private Integer id;

        /**
         * Zobrazit ve výchozím zobrazení?
         */
        private Boolean showDefault;

        /**
         * Výchozí šířka.
         */
        private Integer width;

        public Integer getId() {
            return id;
        }

        public void setId(Integer itemTypeId) {
            this.id = itemTypeId;
        }

        public Boolean getShowDefault() {
            return showDefault;
        }

        public void setShowDefault(final Boolean showDefault) {
            this.showDefault = showDefault;
        }

        public Integer getWidth() {
            return width;
        }

        public void setWidth(final Integer width) {
            this.width = width;
        }

        public static GridView newInstance(ItemType gv, StaticDataProvider sdp) {
            GridView result = new GridView();
            cz.tacr.elza.core.data.ItemType itemType = sdp.getItemTypeByCode(gv.getCode());
            if (itemType == null) {
                throw new BusinessException("Missing itemType", BaseCode.INVALID_STATE)
                        .set("code", gv.getCode());
            }
            result.setId(itemType.getItemTypeId());
            result.setShowDefault(gv.getShowDefault());
            result.setWidth(gv.getWidth());
            return result;
        }

    }

    public static RulRuleSetVO newInstance(RuleSet ruleSet,
                                           List<SettingGridView.ItemType> gridViewItemTypes,
                                           StaticDataProvider sdp) {
        RulRuleSetVO result = new RulRuleSetVO(ruleSet.getEntity());
        if (CollectionUtils.isNotEmpty(gridViewItemTypes)) {
            List<GridView> gridViews = gridViewItemTypes.stream().map(gv -> GridView.newInstance(gv, sdp))
                    .collect(Collectors.toList());

            result.setGridViews(gridViews);
        }

        return result;
    }
}
