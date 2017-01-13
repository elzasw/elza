package cz.tacr.elza.controller.vo;

import org.springframework.web.multipart.MultipartFile;

import cz.tacr.elza.domain.vo.XmlImportType;

public class XmlImportConfigVO {

    private boolean stopOnError;

    private MultipartFile xmlFile;

    private String transformationName;

    private XmlImportType importDataFormat;

    private Integer ruleSetId;

    private RegScopeVO regScope;

    public XmlImportConfigVO() {
    }

    public boolean isStopOnError() {
        return stopOnError;
    }

    public void setStopOnError(boolean stopOnError) {
        this.stopOnError = stopOnError;
    }

    public MultipartFile getXmlFile() {
        return xmlFile;
    }

    public void setXmlFile(MultipartFile xmlFile) {
        this.xmlFile = xmlFile;
    }

    public String getTransformationName() {
        return transformationName;
    }

    public void setTransformationName(String transformationName) {
        this.transformationName = transformationName;
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

    public RegScopeVO getRegScope() {
        return regScope;
    }

    public void setRegScope(RegScopeVO regScope) {
        this.regScope = regScope;
    }

}
