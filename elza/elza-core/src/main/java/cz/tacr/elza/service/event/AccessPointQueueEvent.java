package cz.tacr.elza.service.event;

import cz.tacr.elza.domain.ApAccessPoint;
import org.springframework.context.ApplicationEvent;

import java.util.List;

public class AccessPointQueueEvent extends ApplicationEvent {

    private List<ApAccessPoint> apAccessPoints;

    public AccessPointQueueEvent(List<ApAccessPoint> apAccessPoints) {
        super(apAccessPoints);
        this.apAccessPoints = apAccessPoints;
    }

    public List<ApAccessPoint> getApAccessPoints() {
        return apAccessPoints;
    }

    public void setApAccessPoints(List<ApAccessPoint> apAccessPoints) {
        this.apAccessPoints = apAccessPoints;
    }
}
