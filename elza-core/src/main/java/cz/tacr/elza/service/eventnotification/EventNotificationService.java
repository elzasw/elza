package cz.tacr.elza.service.eventnotification;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.google.common.eventbus.EventBus;

import cz.tacr.elza.service.ClientEventDispatcher;
import cz.tacr.elza.service.IEventNotificationService;
import cz.tacr.elza.service.eventnotification.events.AbstractEventSimple;
import cz.tacr.elza.service.eventnotification.events.EventChangeDescItem;
import cz.tacr.elza.service.eventnotification.events.EventId;
import cz.tacr.elza.service.eventnotification.events.EventIdsInVersion;
import cz.tacr.elza.service.eventnotification.events.EventType;
import cz.tacr.elza.service.eventnotification.events.EventVersion;


/**
 * Servisní třída pro registraci události, která bude odeslána klientům.
 */
@Service
public class EventNotificationService implements IEventNotificationService {

    @Autowired
    private ClientEventDispatcher eventDispatcher;

    @Autowired
    private EventBus eventBus;

    private List<AbstractEventSimple> committedEvents = new LinkedList<>();

    @Override
    public void publishEvent(final AbstractEventSimple event) {
        Validate.notNull(event);

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

        //prozatím nejpreve odešleme událost do kontextu aplikace a poté až klientovi
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
     * Listener udržující připravená data, která v případě úspěšné transakce budou připravena k odeslání.
     */
    private class AfterTransactionListener extends TransactionSynchronizationAdapter {

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

                case PARTY_CREATE:
                    transformToPartiesEvent((EventId) event);
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
        private void transformToPartiesEvent(final EventId event) {
            EventId partiesEvent = null;
            for (AbstractEventSimple eventSimple : uncommittedEvents) {
                if (eventSimple.getEventType().equals(EventType.PARTIES_CREATE)) {
                    partiesEvent = (EventId) eventSimple;
                    break;
                }
            }

            // pokud ještě neexistuje
            if (partiesEvent == null) {
                partiesEvent = new EventId(EventType.PARTIES_CREATE, event.getIds());
                uncommittedEvents.add(partiesEvent);
            }

            partiesEvent.getIds().addAll(event.getIds());
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
        public void afterCommit() {
            commitEvents(uncommittedEvents);
        }
    }

}
