package cz.tacr.elza.config;


import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;


/**
 * Nastavení popisků v UI.
 *
 * @author Martin Šlapa
 * @since 10.12.2015
 */
@Component
@ConfigurationProperties(prefix = "elza")
public class ConfigView {

    public static final String FA_PREFIX = "fa-";
    public static final String DEFAULT = "default";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Nastavení zobrazení v UI.
     */
    private Map<String, Map<String, ViewTitles>> fundView;

    public ViewTitles getViewTitles(final String code, final Integer fundId) {
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
        return fundView;
    }

    public void setFundView(final Map<String, Map<String, ViewTitles>> fundView) {
        this.fundView = fundView;
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

        private List<String> treeItem;

        private List<String> accordionLeft;

        private List<String> accordionRight;

        private Map<String, Map<String, ConfigViewTitlesHierarchy>> hierarchy;
        private Map<String, ConfigViewTitlesHierarchy> levelHierarchy;

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
    }
}
