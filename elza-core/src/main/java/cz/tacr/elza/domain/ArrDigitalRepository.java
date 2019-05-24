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

    @Column(length = StringLength.LENGTH_1000)
    private String viewThumbnailUrl;

    @Column(nullable = false)
    private Boolean sendNotification;

    /**
     * @return url k dao
     */
    public String getViewDaoUrl() {
        return viewDaoUrl;
    }

    /**
     * @param viewDaoUrl url k dao
     */
    public void setViewDaoUrl(final String viewDaoUrl) {
        this.viewDaoUrl = viewDaoUrl;
    }

    /**
     * @return url k souboru
     */
    public String getViewFileUrl() {
        return viewFileUrl;
    }

    /**
     * @param viewFileUrl url k souboru
     */
    public void setViewFileUrl(final String viewFileUrl) {
        this.viewFileUrl = viewFileUrl;
    }

    /**
     * @return url k náhledu
     */
    public String getViewThumbnailUrl() {
        return viewThumbnailUrl;
    }

    /**
     * @param viewThumbnailUrl k náhledu
     */
    public void setViewThumbnailUrl(String viewThumbnailUrl) {
        this.viewThumbnailUrl = viewThumbnailUrl;
    }

    /**
     * @return odeslat notifikaci?
     */
    public Boolean getSendNotification() {
        return sendNotification;
    }

    /**
     * @param sendNotification odeslat notifikaci?
     */
    public void setSendNotification(final Boolean sendNotification) {
        this.sendNotification = sendNotification;
    }

    @Override
    public String toString() {
        return "ArrDigitalRepository pk=" + getExternalSystemId();
    }
}
