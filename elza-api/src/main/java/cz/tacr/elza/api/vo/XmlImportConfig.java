package cz.tacr.elza.api.vo;

import java.io.File;

/**
 *
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 21. 12. 2015
 */
@Deprecated
public class XmlImportConfig {

    private boolean stopOnError;

    private boolean updateRecords;

    private File xmlFile;

    private File transformationFile;

    private XmlImportType importDataFormat;

    private Integer ruleSetId;

    private Integer recordScopeId;

    public boolean isStopOnError() {
        return stopOnError;
    }

    public void setStopOnError(boolean stopOnError) {
        this.stopOnError = stopOnError;
    }

    public boolean isUpdateRecords() {
        return updateRecords;
    }

    public void setUpdateRecords(boolean updateRecords) {
        this.updateRecords = updateRecords;
    }

    public File getXmlFile() {
        return xmlFile;
    }

    public void setXmlFile(File xmlFile) {
        this.xmlFile = xmlFile;
    }

    public File getTransformationFile() {
        return transformationFile;
    }

    public void setTransformationFile(File transformationFile) {
        this.transformationFile = transformationFile;
    }

    public XmlImportType getImportDataFormat() {
        return importDataFormat;
    }

    public void setImportDataFormat(XmlImportType importDataFormat) {
        this.importDataFormat = importDataFormat;
    }

    public Integer getRuleSetId() {
        return ruleSetId;
    }

    public void setRuleSetId(Integer ruleSetId) {
        this.ruleSetId = ruleSetId;
    }

    public Integer getRecordScopeId() {
        return recordScopeId;
    }

    public void setRecordScopeId(Integer recordScopeId) {
        this.recordScopeId = recordScopeId;
    }
}
