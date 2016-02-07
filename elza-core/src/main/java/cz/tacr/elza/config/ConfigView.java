package cz.tacr.elza.config;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
    public static final String TREE_ITEM = "tree-item";
    public static final String ACCORDION_LEFT = "accordion-left";
    public static final String ACCORDION_RIGHT = "accordion-right";
    public static final String ICON = "ZP2015_LEVEL_TYPE";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Nastavení zobrazení v UI.
     *
     * Kód pravidel / AP / TYP / seznam kódů
     */
    private Map<String, Map<String, Map<String, List<String>>>> findingAidView;

    @Value("${elza.treenode.title}")
    private String titleDescItemTypeCode = null;

    public ViewTitles getViewTitles(final String code, final Integer findingAidId) {
        Assert.notNull(code);
        Assert.notNull(findingAidId);

        if (findingAidView == null) {
            logger.warn("Nejsou definována pravidla pro zobrazení popisků v UI");
            return createDefaultViewTitles();
        }

        Map<String, Map<String, List<String>>> viewByCode = findingAidView.get(code);

        if (viewByCode == null) {
            logger.warn("Nejsou definována pravidla pro zobrazení popisků v UI s kódem " + code);
            return null;
        }

        Map<String, List<String>> viewByFa = viewByCode.get(FA_PREFIX + findingAidId);

        if (viewByFa == null) {
            viewByFa = viewByCode.get(DEFAULT);
            if (viewByFa == null) {
                logger.warn("Nejsou definována výchozí pravidla pro zobrazení popisků v UI s kódem " + code);
                return null;
            }
        }

        List<String> defaultTitleCode = null;
        if (CollectionUtils.isEmpty(viewByFa.get(TREE_ITEM)) || CollectionUtils.isEmpty(viewByFa.get(ACCORDION_LEFT))) {
            if (StringUtils.isBlank(titleDescItemTypeCode)) {
                logger.warn("Není nastaven typ atributu, jehož hodnota bude použita pro popisek uzlu."
                        + " Nastavte kód v konfiguraci pro hodnotu 'elza.treenode.title'");
            } else {
                defaultTitleCode = new ArrayList<String>(1);
                defaultTitleCode.add(titleDescItemTypeCode);
            }
        }

        List<String> treeNode = null;
        if (CollectionUtils.isEmpty(viewByFa.get(TREE_ITEM))) {
            treeNode = defaultTitleCode;
        } else {
            treeNode = viewByFa.get(TREE_ITEM);
        }

        List<String> accordionLeft = null;
        if (CollectionUtils.isEmpty(viewByFa.get(ACCORDION_LEFT))) {
            accordionLeft = defaultTitleCode;
        } else {
            accordionLeft = viewByFa.get(ACCORDION_LEFT);
        }

        return new ViewTitles(treeNode,
                accordionLeft,
                viewByFa.get(ACCORDION_RIGHT),
                ICON); // TODO: kde definovat?
    }

    private ViewTitles createDefaultViewTitles() {
        if (StringUtils.isBlank(titleDescItemTypeCode)) {
            logger.warn("Není nastaven typ atributu, jehož hodnota bude použita pro popisek uzlu."
                    + " Nastavte kód v konfiguraci pro hodnotu 'elza.treenode.title'");

            return new ViewTitles(null, null, null, null);
        }

        List<String> defaultTitleCode = new ArrayList<String>(1);
        defaultTitleCode.add(titleDescItemTypeCode);

        return new ViewTitles(defaultTitleCode, defaultTitleCode, null, null);
    }

    public Map<String, Map<String, Map<String, List<String>>>> getFindingAidView() {
        return findingAidView;
    }

    public void setFindingAidView(final Map<String, Map<String, Map<String, List<String>>>> findingAidView) {
        this.findingAidView = findingAidView;
    }

    public static class ViewTitles {

        private List<String> treeItem;

        private List<String> accordionLeft;

        private List<String> accordionRight;

        private String icon;

        public ViewTitles(final List<String> treeItem,
                          final List<String> accordionLeft,
                          final List<String> accordionRight,
                          final String icon) {
            this.treeItem = treeItem;
            this.accordionLeft = accordionLeft;
            this.accordionRight = accordionRight;
            this.icon = icon;
        }

        public List<String> getTreeItem() {
            return treeItem;
        }

        public List<String> getAccordionLeft() {
            return accordionLeft;
        }

        public List<String> getAccordionRight() {
            return accordionRight;
        }

        public String getIcon() {
            return icon;
        }
    }
}
