package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.SysExternalSystem;

/**
 * VO pro externí systém - uložiště digitalizátů.
 *
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

    @Override
    public SysExternalSystem createEntity() {
        ArrDigitalRepository entity = new ArrDigitalRepository();
        this.fillEntity(entity);

        entity.setViewDaoUrl(viewDaoUrl);
        entity.setViewFileUrl(viewFileUrl);
        entity.setSendNotification(sendNotification);

        return entity;
    }
}
