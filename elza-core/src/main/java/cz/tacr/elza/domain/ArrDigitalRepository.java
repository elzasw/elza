package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import cz.tacr.elza.domain.enumeration.StringLength;

/**
 * Uložiště digitalizátů.
 *
 * @author Martin Šlapa
 * @since 05. 12. 2016
 */
@Entity(name = "arr_digital_repository")
@Table
public class ArrDigitalRepository extends SysExternalSystem {

    @Column(length = StringLength.LENGTH_1000)
    private String viewDaoUrl;

    @Column(length = StringLength.LENGTH_1000)
    private String viewFileUrl;

    @Column(nullable = false)
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
    public String toString() {
        return "ArrDigitalRepository pk=" + getExternalSystemId();
    }
}
