package cz.tacr.elza.config.view;

import java.util.HashMap;
import java.util.Map;

public class LevelConfig {

    private String icon;

    /**
     * Map of separators by parent specification
     */
    private Map<String, String> separsByParent = new HashMap<>();

    public String getIcon() {
        return icon;
    }

    public void setIcon(final String icon) {
        this.icon = icon;
    }

    void addSeparForParent(String parentSpecCode, String separator) {
        separsByParent.put(parentSpecCode, separator);
    }

    public String getSeparForParent(String parentSpecCode) {
        return separsByParent.get(parentSpecCode);
    }

}