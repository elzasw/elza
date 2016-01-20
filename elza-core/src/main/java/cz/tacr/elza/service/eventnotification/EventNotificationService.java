package cz.tacr.elza.service.eventnotification;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

import com.google.common.eventbus.EventBus;

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

    @Autowired
    private EventBus eventBus;


    private List<AbstractEventSimple> committedEvents = new LinkedList<>();


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

        /**
         * Přidá připravenou událost do mapy.
         *
         * @param event událost
         */
        public void registerEvent(final AbstractEventSimple event) {
            uncommittedEvents.add(event);
        }


        @Override
        public void afterCommit() {
            commitEvents(uncommittedEvents);
        }
    }

}
