package cz.tacr.elza.ws.core.v1;

import cz.tacr.elza.ws.types.v1.Entities;
import cz.tacr.elza.ws.types.v1.EntitiesRequest;

public class EntitiesServiceImpl implements EntitiesService {

    @Override
    public Entities getEntities(EntitiesRequest request) throws GetEntitiesException {
        String format = request.getRequiredFormat();
        Entities ents = new Entities();
        return ents;
    }

}
