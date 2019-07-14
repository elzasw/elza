package cz.tacr.elza.daoimport.service.vo;

import java.util.List;

/**
 * Nastavení importu.
 */
public class ImportConfig {

    private String mainDir;
    private List<String> supportedMimeTypes;

    public String getMainDir() {
        return mainDir;
    }

    public void setMainDir(String mainDir) {
        this.mainDir = mainDir;
    }

    public List<String> getSupportedMimeTypes() {
        return supportedMimeTypes;
    }

    public void setSupportedMimeTypes(List<String> supportedMimeTypes) {
        this.supportedMimeTypes = supportedMimeTypes;
    }
}
