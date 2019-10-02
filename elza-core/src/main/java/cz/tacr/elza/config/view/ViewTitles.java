package cz.tacr.elza.config.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.commons.collections4.CollectionUtils;

import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.packageimport.xml.SettingFundViews;
import cz.tacr.elza.packageimport.xml.SettingFundViews.FundView;
import cz.tacr.elza.packageimport.xml.SettingFundViews.HierarchyItem;
import cz.tacr.elza.packageimport.xml.SettingFundViews.HierarchyXml;
import cz.tacr.elza.packageimport.xml.SettingFundViews.Separator;

/**
 * Title codes for view
 * 
 *
 */
public class ViewTitles {

    private String defaultTitle;

    /**
     * Item type ids for tree level
     */
    private List<Integer> treeItemIds = new ArrayList<>();

    /**
     * Item type ids for accordion left
     */
    private List<Integer> accordionLeftIds = new ArrayList<>();

    /**
     * Item type ids for accordion right
     */
    private List<Integer> accordionRightIds = new ArrayList<>();

    /**
     * Item type if for level code
     */
    private Integer levelTypeId;

    /**
     * List of configuration for each level
     */
    private final List<LevelConfig> levelHierarchy = new ArrayList<>();
    private final Map<String, LevelConfig> levelHierarchyLookup = new HashMap<>();
    private Boolean strictMode;

    private ViewTitles() {

    }

    public Integer getLevelTypeId() {
        return levelTypeId;
    }

    public void setLevelTypeId(final Integer levelTypeId) {
        this.levelTypeId = levelTypeId;
    }

    private String defaultLevelSeparator;

    public String getDefaultLevelSeparator() {
        return defaultLevelSeparator;
    }

    public void setDefaultLevelSeparator(String defaultLevelSeparator) {
        this.defaultLevelSeparator = defaultLevelSeparator;
    }

    public void addLevelHierarchy(String specCode, LevelConfig level) {
        levelHierarchy.add(level);
        levelHierarchyLookup.put(specCode, level);
    }

    public List<Integer> getTreeItemIds() {
        return treeItemIds;
    }

    public List<Integer> getAccordionLeftIds() {
        return accordionLeftIds;
    }

    public List<Integer> getAccordionRightIds() {
        return accordionRightIds;
    }

    public String getDefaultTitle() {
        return defaultTitle;
    }

    public void setDefaultTitle(final String defaultTitle) {
        this.defaultTitle = defaultTitle;
    }

    public void setStrictMode(final Boolean strictMode) {
        this.strictMode = strictMode;
    }

    public Boolean getStrictMode() {
        return strictMode;
    }

    public LevelConfig getLevelHierarchy(String nodeTypeSpec) {
        return levelHierarchyLookup.get(nodeTypeSpec);
    }

    /**
     * Create collection of all used item types by given ViewTitles
     * 
     * 
     * @param viewTitles
     * @return
     */
    public Set<Integer> getAllItemTypeIds() {
        Set<Integer> result = new HashSet<>();
        result.addAll(accordionLeftIds);
        result.addAll(accordionRightIds);
        result.addAll(treeItemIds);
        if (levelTypeId != null) {
            result.add(levelTypeId);
        }

        return result;
    }

    static Integer getItemId(String code, StaticDataProvider sdp) {
        ItemType itemType = sdp.getItemTypeByCode(code);
        if (itemType == null) {
            throw new SystemException("Item type not found", BaseCode.INVALID_STATE)
                    .set("itemType.code", code);
        }
        return itemType.getItemTypeId();
    }

    static void addItemIds(@Nullable List<String> src, List<Integer> trg, StaticDataProvider sdp) {
        if (src == null) {
            return;
        }
        src.forEach(s -> {
            trg.add(getItemId(s, sdp));
        });
    }

    static public ViewTitles valueOf(FundView fundView, StaticDataProvider sdp) {
        ViewTitles result = new ViewTitles();

        result.setDefaultTitle(fundView.getTitle());
        result.setStrictMode(fundView.getStrictMode());
        
        if(fundView.getAccordionLeft()!=null) {
            addItemIds(fundView.getAccordionLeft().getValues(), result.accordionLeftIds, sdp);
        }
        if (fundView.getAccordionRight() != null) {
            addItemIds(fundView.getAccordionRight().getValues(), result.accordionRightIds, sdp);
        }
        if (fundView.getTree() != null) {
            addItemIds(fundView.getTree().getValues(), result.treeItemIds, sdp);
        }

        HierarchyXml hierXml = fundView.getHierarchy();
        if (hierXml == null) {
            throw new SystemException("Incorrect fund view definition", BaseCode.INVALID_STATE);
        }
        Integer levelTypeId = getItemId(hierXml.getTypeCode(), sdp);
        result.setLevelTypeId(levelTypeId);
        result.setDefaultLevelSeparator(hierXml.getDefaultSeparator());

        // prepare levels
        List<HierarchyItem> levels = hierXml.getLevels();
        if (!CollectionUtils.isEmpty(levels)) {
            for (SettingFundViews.HierarchyItem hierarchyItem : levels) {
                LevelConfig levelInfo = new LevelConfig();
                levelInfo.setIcon(hierarchyItem.getIcon());

                // prepare separators
                List<Separator> separs = hierarchyItem.getSeparators();
                if (separs != null) {
                    for (Separator separ : separs) {
                        levelInfo.addSeparForParent(separ.getParent(), separ.getValue());
                    }
                }

                result.addLevelHierarchy(hierarchyItem.getCode(), levelInfo);
            }
        }
        return result;
    }
}