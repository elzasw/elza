package cz.tacr.elza.service.eventnotification;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.google.common.eventbus.EventBus;

import cz.tacr.elza.service.ClientEventDispatcher;
import cz.tacr.elza.service.IEventNotificationService;
import cz.tacr.elza.service.eventnotification.events.AbstractEventSimple;
import cz.tacr.elza.service.eventnotification.events.EventChangeDescItem;
import cz.tacr.elza.service.eventnotification.events.EventIdsInVersion;
import cz.tacr.elza.service.eventnotification.events.EventType;
import cz.tacr.elza.service.eventnotification.events.EventVersion;

/**
 * Servisní třída pro registraci události, která bude odeslána klientům.
 */
@Service
public class EventNotificationService implements IEventNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(EventNotificationService.class);

    @Autowired
    private ClientEventDispatcher eventDispatcher;

    @Autowired
    private EventBus eventBus;

    private List<AbstractEventSimple> committedEvents = new LinkedList<>();

    @Override
    public void publishEvent(final AbstractEventSimple event) {
    	Objects.requireNonNull(event);
        logger.debug("Publish event: {}, {}", event.getEventType(), event.toString());

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
            valuesCopy = new LinkedList<>(committedEvents);
            committedEvents.clear();
        }

        //prozatím nejprve odešleme událost do kontextu aplikace a poté až klientovi
        eventBus.post(new EventChangeMessage(valuesCopy));
        valuesCopy.forEach(eventDispatcher::dispatchEvent);
    }


    /**
     * Uloží dočasné události do komitnutých (připravených k odeslání klientům.)
     *
     * @param uncommittedEvents dočasné události
     */
    synchronized private void commitEvents(final List<AbstractEventSimple> uncommittedEvents) {
        committedEvents.addAll(uncommittedEvents);

        //TODO neodesílat po každé transakci, ale po nějakém čase
        flushEvents();
    }



    /**
     * Listener udržující připravená data, která v případě úspěšné transakce budou
     * připravena k odeslání.
     * 
     * Priorita odeslání zpráv je nastavena na 0. Toto umožňuje odeslat ostatní
     * zprávy dříve či později.
     */
    private class AfterTransactionListener implements TransactionSynchronization {

        /**
         * Mapa připravených událostí.
         */
        private List<AbstractEventSimple> uncommittedEvents = new LinkedList<>();

        /**
         * Přidá připravenou událost do mapy.
         *
         * @param event událost
         */
        public void registerEvent(final AbstractEventSimple event) {
            switch (event.getEventType()) {

                case DESC_ITEM_CHANGE:
                    transformToNodesEvent((EventVersion) event);
                    break;

                default:
                    uncommittedEvents.add(event);
                    break;
            }
        }

        /**
         * Metoda grupuje požadované události.
         *
         * @param event událost
         */
        private void transformToNodesEvent(final EventVersion event) {
            EventIdsInVersion nodesEvent = null;
            for (AbstractEventSimple eventSimple : uncommittedEvents) {
                if (eventSimple.getEventType().equals(EventType.NODES_CHANGE)) {
                    nodesEvent = (EventIdsInVersion) eventSimple;
                    break;
                }
            }

            // pokud ještě neexistuje
            if (nodesEvent == null) {
                nodesEvent = new EventIdsInVersion(EventType.NODES_CHANGE, event.getVersionId(), null);
                uncommittedEvents.add(nodesEvent);
            }

            Integer nodeId;
            switch (event.getEventType()) {
                case DESC_ITEM_CHANGE:
                    nodeId = ((EventChangeDescItem) event).getNodeId();
                    Integer[] nodeIds = nodesEvent.getEntityIds();
                    nodeIds = nodeIds == null ? new Integer[0] : nodeIds;
                    nodesEvent.setEntityIds(append(nodeIds, nodeId));
                    break;
            }
        }

        /**
         * Přidání hodnoty do pole.
         * @param arr   pole
         * @param element přidávaná hodnota
         * @return nové pole
         */
        private <T> T[] append(T[] arr, T element) {
            final int N = arr.length;
            arr = Arrays.copyOf(arr, N + 1);
            arr[N] = element;
            return arr;
        }

        @Override
        public int getOrder() {
            // Zprávy o změnách jsou odesílány s prioritou 0
            // Výsledek volání přes WS je odesílán vždy s nejvyšší prioritou
            return 0;
        }

        @Override
        public void afterCommit() {
            logger.debug("AfterCommit: Publishing events: {}", uncommittedEvents.size());

            commitEvents(uncommittedEvents);
        }
    }

}
