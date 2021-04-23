package cz.tacr.elza.repository.vo;

import cz.tacr.elza.domain.ApCachedAccessPoint;

import java.util.List;

public class ApCachedAccessPointResult {

    private int count;

    private List<ApCachedAccessPoint> apCachedAccessPoints;

    public ApCachedAccessPointResult(int count, List<ApCachedAccessPoint> apCachedAccessPoints) {
        this.count = count;
        this.apCachedAccessPoints = apCachedAccessPoints;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<ApCachedAccessPoint> getApCachedAccessPoints() {
        return apCachedAccessPoints;
    }

    public void setApCachedAccessPoints(List<ApCachedAccessPoint> apCachedAccessPoints) {
        this.apCachedAccessPoints = apCachedAccessPoints;
    }
}
