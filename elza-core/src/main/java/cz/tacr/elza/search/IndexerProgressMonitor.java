package cz.tacr.elza.search;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.mapper.pojo.massindexing.impl.PojoMassIndexingLoggingMonitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.EventBus;

import cz.tacr.elza.service.ClientEventDispatcher;
import cz.tacr.elza.service.eventnotification.EventChangeMessage;
import cz.tacr.elza.service.eventnotification.events.AbstractEventSimple;
import cz.tacr.elza.service.eventnotification.events.ActionEvent;
import cz.tacr.elza.service.eventnotification.events.EventType;

/**
 * Monitor stavu indexování.
 */
@Component
public class IndexerProgressMonitor extends PojoMassIndexingLoggingMonitor {

    @Autowired
    private ClientEventDispatcher eventDispatcher;

    @Autowired
    private EventBus eventBus;

    @Override
    public void indexingCompleted() {
        List<AbstractEventSimple> events = new ArrayList<>();
        events.add(new ActionEvent(EventType.INDEXING_FINISHED));
        eventBus.post(new EventChangeMessage(events));
        events.forEach(eventDispatcher::dispatchEvent);
    }

}
