package cz.tacr.elza.xmlimport.v1.utils;

import org.springframework.web.multipart.MultipartFile;

import cz.tacr.elza.api.vo.XmlImportType;
import cz.tacr.elza.domain.RegScope;

/**
 *
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 21. 12. 2015
 */
public class XmlImportConfig {

    private boolean stopOnError;

    private MultipartFile xmlFile;

    private String transformationName;

    private XmlImportType importDataFormat;

    private Integer ruleSetId;

    private RegScope regScope;

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

    public XmlImportType getXmlImportType() {
        return importDataFormat;
    }

    public void setXmlImportType(XmlImportType importDataFormat) {
        this.importDataFormat = importDataFormat;
    }

    public Integer getRuleSetId() {
        return ruleSetId;
    }

    public void setRuleSetId(Integer ruleSetId) {
        this.ruleSetId = ruleSetId;
    }

    public RegScope getRegScope() {
        return regScope;
    }

    public void setRegScope(RegScope regScope) {
        this.regScope = regScope;
    }

    public String getTransformationName() {
        return transformationName;
    }

    public void setTransformationName(String transformationName) {
        this.transformationName = transformationName;
    }
}
