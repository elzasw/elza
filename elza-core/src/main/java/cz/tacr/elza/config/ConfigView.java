package cz.tacr.elza.config;


import com.google.common.eventbus.Subscribe;
import cz.tacr.elza.EventBusListener;
import cz.tacr.elza.domain.UISettings;
import cz.tacr.elza.packageimport.PackageService;
import cz.tacr.elza.packageimport.xml.SettingFundViews;
import cz.tacr.elza.repository.SettingsRepository;
import cz.tacr.elza.service.event.CacheInvalidateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Nastavení popisků v UI.
 *
 * @author Martin Šlapa
 * @since 10.12.2015
 */
@Component
@EventBusListener
public class ConfigView {

    public static final String FA_PREFIX = "fa-";
    public static final String DEFAULT = "default";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private SettingsRepository settingsRepository;

    @Autowired
    private PackageService packageService;

    /**
     * Nastavení zobrazení v UI.
     */
    private Map<String, Map<String, ViewTitles>> fundView;

    @Subscribe
    public synchronized void invalidateCache(final CacheInvalidateEvent cacheInvalidateEvent) {
        if (cacheInvalidateEvent.contains(CacheInvalidateEvent.Type.VIEW)) {
            fundView = null;
        }
    }

    public ViewTitles getViewTitles(final String code, final Integer fundId) {
        Map<String, Map<String, ViewTitles>> fundView = getFundView();
        Assert.notNull(code);
        Assert.notNull(fundId);

        if (fundView == null) {
            logger.warn("Nejsou definována pravidla pro zobrazení popisků v UI");
            return new ViewTitles();
        }

        Map<String, ViewTitles> viewByCode = fundView.get(code);

        if (viewByCode == null) {
            logger.warn("Nejsou definována pravidla pro zobrazení popisků v UI s kódem " + code);
            return new ViewTitles();
        }

        ViewTitles viewByFa = viewByCode.get(FA_PREFIX + fundId);

        if (viewByFa == null) {
            viewByFa = viewByCode.get(DEFAULT);
            if (viewByFa == null) {
                logger.warn("Nejsou definována výchozí pravidla pro zobrazení popisků v UI s kódem " + code);
                return new ViewTitles();
            }
        }

        return viewByFa;
    }

    public Map<String, Map<String, ViewTitles>> getFundView() {
        if (fundView == null) {
            List<UISettings> uiSettingsList = settingsRepository.findByUserAndSettingsTypeAndEntityType(null, UISettings.SettingsType.FUND_VIEW, UISettings.EntityType.RULE);
            fundView = new HashMap<>();
            if (uiSettingsList.size() > 0) {
                uiSettingsList.forEach(uiSettings -> {
                    SettingFundViews setting = (SettingFundViews) packageService.convertSetting(uiSettings);
                    Map<String, ViewTitles> viewByCode = fundView.get(setting.getCode());
                    if (viewByCode == null) {
                        viewByCode = new HashMap<>();
                        fundView.put(setting.getCode(), viewByCode);
                    }
                    List<SettingFundViews.Item> items = setting.getItems();
                    for (SettingFundViews.Item item : items) {

                        ViewTitles vt = convertViewTitles(item);
                        if (item instanceof SettingFundViews.Default) {
                            viewByCode.put(DEFAULT, vt);
                        } else if (item instanceof SettingFundViews.Fund) {
                            Integer fundId = ((SettingFundViews.Fund) item).getFundId();
                            viewByCode.put(FA_PREFIX + fundId, vt);
                        } else {
                            throw new IllegalStateException("Nedefinovaný stav pro třídu:" + item.getClass().getSimpleName());
                        }
                    }
                });
            }
        }
        return fundView;
    }

    private ViewTitles convertViewTitles(final SettingFundViews.Item item) {
        ViewTitles tv = new ViewTitles();

        tv.setDefaultTitle(item.getTitle());
        tv.setStrictMode(item.getStrictMode());
        tv.setAccordionLeft(item.getAccordionLeft() == null ? null : item.getAccordionLeft().getValues());
        tv.setAccordionRight(item.getAccordionRight() == null ? null : item.getAccordionRight().getValues());
        tv.setTreeItem(item.getTree() == null ? null : item.getTree().getValues());
        Map<String, Map<String, ConfigViewTitlesHierarchy>> heararchyMap = new HashMap<>();
        tv.setHierarchy(heararchyMap);
        if (!CollectionUtils.isEmpty(item.getHierarchyLevels())) {
            for (SettingFundViews.HierarchyLevel hierarchyLevel : item.getHierarchyLevels()) {
                Map<String, ConfigViewTitlesHierarchy> hierarchyLevelMap = new HashMap<>();
                heararchyMap.put(hierarchyLevel.getCode(), hierarchyLevelMap);
                if (!CollectionUtils.isEmpty(hierarchyLevel.getHierarchyItems())) {
                    for (SettingFundViews.HierarchyItem hierarchyItem : hierarchyLevel.getHierarchyItems()) {
                        ConfigViewTitlesHierarchy hierarchy = new ConfigViewTitlesHierarchy();
                        hierarchy.setIcon(hierarchyItem.getIcon());
                        hierarchy.setSeparatorFirst(hierarchyItem.getSeparatorFirst());
                        hierarchy.setSeparatorOther(hierarchyItem.getSeparatorOther());
                        hierarchyLevelMap.put(hierarchyItem.getCode(), hierarchy);
                    }
                }
            }
        }
        return tv;
    }

    public static class ConfigViewTitlesHierarchy {

        private String icon;

        private String separatorFirst;

        private String separatorOther;

        public String getIcon() {
            return icon;
        }

        public void setIcon(final String icon) {
            this.icon = icon;
        }

        public String getSeparatorFirst() {
            return separatorFirst;
        }

        public void setSeparatorFirst(final String separatorFirst) {
            this.separatorFirst = separatorFirst;
        }

        public String getSeparatorOther() {
            return separatorOther;
        }

        public void setSeparatorOther(final String separatorOther) {
            this.separatorOther = separatorOther;
        }
    }

    public static class ViewTitles {

        private String defaultTitle;

        private List<String> treeItem;

        private List<String> accordionLeft;

        private List<String> accordionRight;

        private Map<String, Map<String, ConfigViewTitlesHierarchy>> hierarchy;
        private Map<String, ConfigViewTitlesHierarchy> levelHierarchy;
        private Boolean strictMode;

        public List<String> getTreeItem() {
            return treeItem;
        }

        public void setTreeItem(final List<String> treeItem) {
            this.treeItem = treeItem;
        }

        public List<String> getAccordionLeft() {
            return accordionLeft;
        }

        public void setAccordionLeft(final List<String> accordionLeft) {
            this.accordionLeft = accordionLeft;
        }

        public List<String> getAccordionRight() {
            return accordionRight;
        }

        public void setAccordionRight(final List<String> accordionRight) {
            this.accordionRight = accordionRight;
        }

        public Map<String, Map<String, ConfigViewTitlesHierarchy>> getHierarchy() {
            return hierarchy;
        }

        public String getHierarchyLevelType() {
            if(hierarchy == null){
                return null;
            }


            for (Map.Entry<String, Map<String, ConfigViewTitlesHierarchy>> stringMapEntry : hierarchy.entrySet()) {
                return stringMapEntry.getKey();
            }

            return null;
        }

        public ConfigViewTitlesHierarchy getLevelTitlesHierarchy(final String levelType){
            if(levelHierarchy == null){
                levelHierarchy = hierarchy.get(getHierarchyLevelType());
            }

            return levelHierarchy.get(levelType);
        }

        public void setHierarchy(final Map<String, Map<String, ConfigViewTitlesHierarchy>> hierarchy) {
            this.hierarchy = hierarchy;
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
    }
}
