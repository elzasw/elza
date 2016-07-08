package cz.tacr.elza.search;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.batchindexing.impl.SimpleIndexingProgressMonitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.EventBus;

import cz.tacr.elza.service.IClientDataChangesService;
import cz.tacr.elza.service.eventnotification.EventChangeMessage;
import cz.tacr.elza.service.eventnotification.events.AbstractEventSimple;
import cz.tacr.elza.service.eventnotification.events.ActionEvent;
import cz.tacr.elza.service.eventnotification.events.EventType;

/**
 * Monitor stavu indexování.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 25. 1. 2016
 */
@Component
public class IndexerProgressMonitor extends SimpleIndexingProgressMonitor {

    @Autowired
    private IClientDataChangesService clientDataChangesService;

    @Autowired
    private EventBus eventBus;

    @Override
    public void indexingCompleted() {
        List<AbstractEventSimple> events = new ArrayList<AbstractEventSimple>();
        events.add(new ActionEvent(EventType.INDEXING_FINISHED));
        eventBus.post(new EventChangeMessage(events));
        clientDataChangesService.fireEvents(events);
    }

}
