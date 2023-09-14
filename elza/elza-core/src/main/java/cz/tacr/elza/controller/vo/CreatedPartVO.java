package cz.tacr.elza.controller.vo;

public class CreatedPartVO {

    final int partId;

    final int apVersion;

    public CreatedPartVO(int partId, int apVersion) {
        this.partId = partId;
        this.apVersion = apVersion;
    }

    public int getPartId() {
        return partId;
    }

    public int getApVersion() {
        return apVersion;
    }
}
