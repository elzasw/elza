package cz.tacr.elza.daoimport.service.vo;

import java.util.List;

/**
 * Nastavení importu.
 */
public class ImportConfig {

    private String mainDir;
    private List<String> supportedMimeTypes;
    private List<String> mimeTypesForConversion;
    private String conversionCommand;
    private String mimeTypeAferConversion;
    private String fileExtensionAferConversion;
    private String daoNameExpression;

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

    public List<String> getMimeTypesForConversion() {
        return mimeTypesForConversion;
    }

    public void setMimeTypesForConversion(List<String> mimeTypesForConversion) {
        this.mimeTypesForConversion = mimeTypesForConversion;
    }

    public String getConversionCommand() {
        return conversionCommand;
    }

    public void setConversionCommand(String conversionCommand) {
        this.conversionCommand = conversionCommand;
    }

    public String getMimeTypeAferConversion() {
        return mimeTypeAferConversion;
    }

    public void setMimeTypeAferConversion(String mimeTypeAferConversion) {
        this.mimeTypeAferConversion = mimeTypeAferConversion;
    }

    public String getFileExtensionAferConversion() {
        return fileExtensionAferConversion;
    }

    public void setFileExtensionAferConversion(String fileExtensionAferConversion) {
        this.fileExtensionAferConversion = fileExtensionAferConversion;
    }

    public String getDaoNameExpression() {
        return daoNameExpression;
    }

    public void setDaoNameExpression(String daoNameExpression) {
        this.daoNameExpression = daoNameExpression;
    }
}
