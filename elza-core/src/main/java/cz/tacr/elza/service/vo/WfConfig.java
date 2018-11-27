package cz.tacr.elza.service.vo;

import java.util.Map;

public class WfConfig {

    private Map<String, String> colors;

    private Map<String, String> icons;

    public WfConfig(final Map<String, String> colors, final Map<String, String> icons) {
        this.colors = colors;
        this.icons = icons;
    }

    public Map<String, String> getColors() {
        return colors;
    }

    public Map<String, String> getIcons() {
        return icons;
    }

}
