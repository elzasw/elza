package cz.tacr.elza.xmlimport.v1.utils;

import java.io.File;

import org.springframework.web.multipart.MultipartFile;

import cz.tacr.elza.api.vo.ImportDataFormat;

/**
 *
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 21. 12. 2015
 */
public class XmlImportConfig {

    private boolean stopOnError;

    private boolean updateRecords;

    private MultipartFile xmlMultipartFile;

    private File xmlFile;

    private File transformationFile;

    private String transformationName;

    private ImportDataFormat importDataFormat;

    private Integer ruleSetId;

    private Integer arrangementTypeId;

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

    public ImportDataFormat getImportDataFormat() {
        return importDataFormat;
    }

    public void setImportDataFormat(ImportDataFormat importDataFormat) {
        this.importDataFormat = importDataFormat;
    }

    public Integer getRuleSetId() {
        return ruleSetId;
    }

    public void setRuleSetId(Integer ruleSetId) {
        this.ruleSetId = ruleSetId;
    }

    public Integer getArrangementTypeId() {
        return arrangementTypeId;
    }

    public void setArrangementTypeId(Integer arrangementTypeId) {
        this.arrangementTypeId = arrangementTypeId;
    }

    public Integer getRecordScopeId() {
        return recordScopeId;
    }

    public void setRecordScopeId(Integer recordScopeId) {
        this.recordScopeId = recordScopeId;
    }

    public String getTransformationName() {
        return transformationName;
    }

    public void setTransformationName(String transformationName) {
        this.transformationName = transformationName;
    }

    public MultipartFile getXmlMultipartFile() {
        return xmlMultipartFile;
    }

    public void setXmlMultipartFile(MultipartFile xmlMultipartFile) {
        this.xmlMultipartFile = xmlMultipartFile;
    }
}
