package cz.tacr.elza.domain;

import javax.persistence.*;

/**
 * Prvek popisu části přístupového bodu.
 *
 * @since 17.07.2018
 */
@Entity
@Table(name = "ap_access_point_item")
public class ApAccessPointItem extends ApItem {

    public static final String ACCESS_POINT_ID = "accessPointId";

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApAccessPoint.class)
    @JoinColumn(name = ACCESS_POINT_ID, nullable = false)
    private ApAccessPoint accessPoint;

    @Column(name = ACCESS_POINT_ID, nullable = false, updatable = false, insertable = false)
    private Integer accessPointId;

    public ApAccessPointItem() {
    }

    @Override
    public ApAccessPointItem copy() {
        return new ApAccessPointItem(this);
    }

    public ApAccessPointItem(final ApAccessPointItem other) {
        super(other);
        this.accessPoint = other.accessPoint;
        this.accessPointId = other.accessPointId;
    }

    public ApAccessPoint getAccessPoint() {
        return accessPoint;
    }

    public void setAccessPoint(final ApAccessPoint accessPoint) {
        this.accessPoint = accessPoint;
        this.accessPointId = accessPoint == null ? null : accessPoint.getAccessPointId();
    }
}
