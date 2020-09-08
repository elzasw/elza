package cz.tacr.elza.domain;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity(name = "ap_access_point_queue_item")
public class ApAccessPointQueueItem {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer accessPointQueueItemId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApAccessPoint.class)
    @JoinColumn(name = "access_point_id", nullable = false)
    private ApAccessPoint accessPoint;

    public Integer getAccessPointQueueItemId() {
        return accessPointQueueItemId;
    }

    public void setAccessPointQueueItemId(Integer accessPointQueueItemId) {
        this.accessPointQueueItemId = accessPointQueueItemId;
    }

    public ApAccessPoint getAccessPoint() {
        return accessPoint;
    }

    public void setAccessPoint(ApAccessPoint accessPoint) {
        this.accessPoint = accessPoint;
    }
}
