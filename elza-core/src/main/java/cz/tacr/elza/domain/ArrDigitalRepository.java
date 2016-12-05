package cz.tacr.elza.domain;

import cz.tacr.elza.domain.enumeration.StringLength;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

/**
 * Uložiště digitalizátů.
 *
 * @author Martin Šlapa
 * @since 05. 12. 2016
 */
@Entity(name = "arr_digital_repository")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table
public class ArrDigitalRepository extends SysExternalSystem implements cz.tacr.elza.api.ArrDigitalRepository {

    @Column(length = StringLength.LENGTH_1000)
    private String viewDaoUrl;

    @Column(length = StringLength.LENGTH_1000)
    private String viewFileUrl;

    @Column(nullable = false)
    private Boolean sendNotification;

    @Override
    public String getViewDaoUrl() {
        return viewDaoUrl;
    }

    @Override
    public void setViewDaoUrl(final String viewDaoUrl) {
        this.viewDaoUrl = viewDaoUrl;
    }

    @Override
    public String getViewFileUrl() {
        return viewFileUrl;
    }

    @Override
    public void setViewFileUrl(final String viewFileUrl) {
        this.viewFileUrl = viewFileUrl;
    }

    @Override
    public Boolean getSendNotification() {
        return sendNotification;
    }

    @Override
    public void setSendNotification(final Boolean sendNotification) {
        this.sendNotification = sendNotification;
    }

    @Override
    public String toString() {
        return "ArrDigitalRepository pk=" + getExternalSystemId();
    }
}
