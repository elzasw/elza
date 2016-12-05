package cz.tacr.elza.controller.vo;

/**
 * VO pro externí systém - uložiště digitalizátů.
 *
 * @author Martin Šlapa
 * @since 05.12.2016
 */
public class ArrDigitalRepositoryVO extends SysExternalSystemVO {

    private String viewDaoUrl;

    private String viewFileUrl;

    private Boolean sendNotification;

    public String getViewDaoUrl() {
        return viewDaoUrl;
    }

    public void setViewDaoUrl(final String viewDaoUrl) {
        this.viewDaoUrl = viewDaoUrl;
    }

    public String getViewFileUrl() {
        return viewFileUrl;
    }

    public void setViewFileUrl(final String viewFileUrl) {
        this.viewFileUrl = viewFileUrl;
    }

    public Boolean getSendNotification() {
        return sendNotification;
    }

    public void setSendNotification(final Boolean sendNotification) {
        this.sendNotification = sendNotification;
    }
}
