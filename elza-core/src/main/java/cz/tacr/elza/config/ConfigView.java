package cz.tacr.elza.config;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
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

    private final Logger logger = LoggerFactory.getLogger(getClass());

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

    /**
     * Nastavení zobrazení v UI.
     *
     * Kód pravidel / AP / TYP / seznam kódů
     */
    private Map<String, Map<String, Map<String, List<String>>>> findingAidView;


    public ViewTitles getViewTitles(final String code, final Integer findingAidId) {
        Assert.notNull(code);
        Assert.notNull(findingAidId);

        if (findingAidView == null) {
            logger.warn("Nejsou definována pravidla pro zobrazení popisků v UI");
            return null;
        }

        Map<String, Map<String, List<String>>> viewByCode = findingAidView.get(code);

        if (viewByCode == null) {
            logger.warn("Nejsou definována pravidla pro zobrazení popisků v UI s kódem " + code);
            return null;
        }

        Map<String, List<String>> viewByFa = viewByCode.get("fa-" + findingAidId);

        if (viewByFa == null) {
            viewByFa = viewByCode.get("default");
            if (viewByFa == null) {
                logger.warn("Nejsou definována výchozí pravidla pro zobrazení popisků v UI s kódem " + code);
                return null;
            }
        }

        return new ViewTitles(viewByFa.get("tree-item"),
                viewByFa.get("accordion-left"),
                viewByFa.get("accordion-right"),
                "ZP2015_LEVEL_TYPE"); // TODO: kde definovat?
    }

    public Map<String, Map<String, Map<String, List<String>>>> getFindingAidView() {
        return findingAidView;
    }

    public void setFindingAidView(final Map<String, Map<String, Map<String, List<String>>>> findingAidView) {
        this.findingAidView = findingAidView;
    }

}
