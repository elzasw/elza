package cz.tacr.elza.service.eventnotification;

import com.google.common.eventbus.EventBus;
import cz.tacr.elza.service.IClientDataChangesService;
import cz.tacr.elza.service.IEventNotificationService;
import cz.tacr.elza.service.eventnotification.events.AbstractEventSimple;
import cz.tacr.elza.service.eventnotification.events.EventChangeDescItem;
import cz.tacr.elza.service.eventnotification.events.EventIdsInVersion;
import cz.tacr.elza.service.eventnotification.events.EventType;
import cz.tacr.elza.service.eventnotification.events.EventVersion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


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

    @Autowired
    private EventBus eventBus;


    private List<AbstractEventSimple> committedEvents = new LinkedList<>();

    public void forcePublish(final AbstractEventSimple event) {
        this.clientDataChangesService.fireEvents(Arrays.asList(event));
    }

    @Override
    public void publishEvent(final AbstractEventSimple event) {
        this.publishEvent(event, false);
    }

    public void publishEvent(final AbstractEventSimple event, final boolean onRollBack) {
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
        if (onRollBack) {
            listener.registerRollBackEvent(event);
        }
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
        clientDataChangesService.fireEvents(valuesCopy);
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
        private List<AbstractEventSimple> uncommittedRollBackEvents = new LinkedList<>();

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

        public void registerRollBackEvent(final AbstractEventSimple event) {
            uncommittedRollBackEvents.add(event);
        }


        @Override
        public void afterCommit() {
            commitEvents(uncommittedEvents);
        }

        @Override
        public void afterCompletion(int status) {
            if (status == STATUS_ROLLED_BACK) {
                commitEvents(uncommittedRollBackEvents);
            }
            super.afterCompletion(status);
        }
    }

}
