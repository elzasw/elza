package cz.tacr.elza.service.vo;

public class Event {

    private String localId;
    private Agent submitter;
    private Agent curator;
    private Agent originator;
    private String ingestionId;
    private String referenceNumber;
    private String nadChangeCode;
    private PackageObject souObject;

    public Agent getSubmitter() {
        return submitter;
    }

    public void setSubmitter(Agent submitter) {
        this.submitter = submitter;
    }

    public Agent getCurator() {
        return curator;
    }

    public void setCurator(Agent curator) {
        this.curator = curator;
    }

    public String getIngestionId() {
        return ingestionId;
    }

    public void setIngestionId(String ingestionId) {
        this.ingestionId = ingestionId;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getNadChangeCode() {
        return nadChangeCode;
    }

    public void setNadChangeCode(String nadChangeCode) {
        this.nadChangeCode = nadChangeCode;
    }

    public PackageObject getSouObject() {
        return souObject;
    }

    public void setSouObject(PackageObject souObject) {
        this.souObject = souObject;
    }

    public String getLocalId() {
        return localId;
    }

    public void setLocalId(String localId) {
        this.localId = localId;
    }

    public Agent getOriginator() {
        return originator;
    }

    public void setOriginator(Agent originator) {
        this.originator = originator;
    }
}
