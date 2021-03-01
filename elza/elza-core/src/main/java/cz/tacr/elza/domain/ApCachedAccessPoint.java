package cz.tacr.elza.domain;

import cz.tacr.elza.domain.bridge.ApCachedAccessPointClassBridge;
import org.hibernate.annotations.Type;
import org.hibernate.search.annotations.ClassBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Table
@Indexed
@ClassBridge(name = "data",
        impl = ApCachedAccessPointClassBridge.class,
        store = Store.YES)
@Entity(name = "ap_cached_access_point")
public class ApCachedAccessPoint {

    public static final String DATA = "data";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer cachedAccessPointId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApAccessPoint.class)
    @JoinColumn(name = "accessPointId", nullable = false)
    private ApAccessPoint accessPoint;

    @Column(nullable = false, updatable = false, insertable = false)
    private Integer accessPointId;

    @Lob
    @Type(type = "org.hibernate.type.TextType")
    @Column
    private String data;

    public Integer getCachedAccessPointId() {
        return cachedAccessPointId;
    }

    public void setCachedAccessPointId(Integer cachedAccessPointId) {
        this.cachedAccessPointId = cachedAccessPointId;
    }

    public ApAccessPoint getAccessPoint() {
        return accessPoint;
    }

    public void setAccessPoint(ApAccessPoint accessPoint) {
        this.accessPoint = accessPoint;
        if (accessPoint != null) {
            this.accessPointId = accessPoint.getAccessPointId();
        }
    }

    public Integer getAccessPointId() {
        return accessPointId;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
