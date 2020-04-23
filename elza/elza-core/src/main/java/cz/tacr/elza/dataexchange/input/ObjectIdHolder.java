package cz.tacr.elza.dataexchange.input;

import cz.tacr.elza.service.AccessPointItemService;
import org.hibernate.Session;

public class ObjectIdHolder {

    private final AccessPointItemService apItemService;

    private Integer objectId;

    private Session session;

    public ObjectIdHolder(AccessPointItemService apItemService, Session session) {
        this.apItemService = apItemService;
        this.session = session;
    }

    public Integer getObjectId() {
        if(objectId == null) {
            objectId = apItemService.nextItemObjectId();
            session.flush();
        }
        return objectId;
    }
}
