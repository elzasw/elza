package cz.tacr.elza.service.vo;

public class IntellectualObject extends PackageObject{
    private String fondsId;
    private String instituitionId;
    private String aipId;
    private String aipVersion;
    private String aipSize;

    public String getFondsId() {
        return fondsId;
    }

    public void setFondsId(String fondsId) {
        this.fondsId = fondsId;
    }

    public String getInstituitionId() {
        return instituitionId;
    }

    public void setInstituitionId(String instituitionId) {
        this.instituitionId = instituitionId;
    }

    public String getAipId() {
        return aipId;
    }

    public void setAipId(String aipId) {
        this.aipId = aipId;
    }

    public String getAipVersion() {
        return aipVersion;
    }

    public void setAipVersion(String aipVersion) {
        this.aipVersion = aipVersion;
    }

    public String getAipSize() {
        return aipSize;
    }

    public void setAipSize(String aipSize) {
        this.aipSize = aipSize;
    }

}
