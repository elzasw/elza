package cz.tacr.elza.service.eventnotification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

import cz.tacr.elza.service.IClientDataChangesService;
import cz.tacr.elza.service.IEventNotificationService;
import cz.tacr.elza.service.eventnotification.events.AbstractEventSimple;
import cz.tacr.elza.service.eventnotification.events.EventType;


/**
 * Servisní třída pro registraci události, která bude odeslána klientům.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 14.01.2016
 */
@Service
public class EventNotificationService implements IEventNotificationService {


    @Autowired
    private IClientDataChangesService clientDataChangesService;

    private Map<EventType, AbstractEventSimple> committedEventMap = new HashMap<>();


    @Override
    public void publishEvent(final AbstractEventSimple event) {
        Assert.notNull(event);

        AfterTransactionListener listener = null;
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            if (synchronization instanceof AfterTransactionListener) {
                listener = (AfterTransactionListener) synchronization;
                break;
            }
        }

        if (listener == null) {
            listener = new AfterTransactionListener();
            TransactionSynchronizationManager.registerSynchronization(listener);
        }
        listener.registerEvent(event);
    }

    /**
     * Provede odeslání událostí klientům.
     */
    private void flushEvents() {
        List<AbstractEventSimple> valuesCopy;
        synchronized (this) {
            valuesCopy = new ArrayList<>(committedEventMap.values());
            committedEventMap.clear();
        }

        clientDataChangesService.fireEvents(valuesCopy);
    }


    /**
     * Uloží dočasné události do komitnutých (připravených k odeslání klientům.)
     *
     * @param uncommittedEventMap mapa dočasných událostí
     */
    synchronized private void commitEvents(final Map<EventType, AbstractEventSimple> uncommittedEventMap) {
        for (AbstractEventSimple eventSimple : uncommittedEventMap.values()) {
            putEventIntoMap(committedEventMap, eventSimple);
        }
        //TODO neodesílat po každé transakci, ale po nějakém čase
        flushEvents();
    }


    /**
     * Najde v mapě událost stejného typu a sloučí její data, nebo vytvoří v mapě novo událost.
     *
     * @param eventMap mapa události
     * @param event    událost
     */
    private void putEventIntoMap(final Map<EventType, AbstractEventSimple> eventMap, final AbstractEventSimple event) {
        AbstractEventSimple commitEvent = eventMap.get(event.getEventType());
        if (commitEvent == null) {
            eventMap.put(event.getEventType(), event);
        } else {
            commitEvent.appendEventData(event);
        }
    }

    /**
     * Listener udržující připravená data, která v případě úspěšné transakce budou připravena k odeslání.
     */
    private class AfterTransactionListener extends TransactionSynchronizationAdapter {

        /**
         * Mapa připravených událostí.
         */
        private Map<EventType, AbstractEventSimple> uncommittedEventMap = new HashMap<>();

        /**
         * Přidá připravenou událost do mapy.
         *
         * @param event událost
         */
        public void registerEvent(final AbstractEventSimple event) {
            putEventIntoMap(uncommittedEventMap, event);
        }


        @Override
        public void afterCommit() {
            commitEvents(uncommittedEventMap);
        }
    }

}
