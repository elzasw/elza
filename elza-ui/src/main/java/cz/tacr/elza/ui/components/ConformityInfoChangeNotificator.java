package cz.tacr.elza.ui.components;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.EventBus;
import com.vaadin.event.UIEvents;

import cz.tacr.elza.events.ConformityInfoUpdatedEvent;
import cz.tacr.elza.ui.utils.ElzaNotifications;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 2.12.2015
 */
@Component
@Scope("prototype")
public class ConformityInfoChangeNotificator implements UIEvents.PollListener{

    @Autowired
    private EventBus eventBus;

    private Integer selectedNodeID;

    private Set<Integer> recentlyUpdatedInfoIds = new HashSet<>();


    public ConformityInfoChangeNotificator() {
       // eventBus.register(this);
    }

    public void setSelectedNodeID(final Integer selectedNodeID) {
        this.selectedNodeID = selectedNodeID;
    }

    @Override
    public void poll(final UIEvents.PollEvent event) {
        if (recentlyUpdatedInfoIds.contains(selectedNodeID)) {
            ElzaNotifications.showWarn("Stav uzlu nodeId=" + selectedNodeID + " byl aktualizován.");
        }
        synchronized (this) {
            recentlyUpdatedInfoIds.clear();
        }
    }


    public void onConformityInfoChange(final ConformityInfoUpdatedEvent event) {
        synchronized (this) {
            recentlyUpdatedInfoIds.add(event.getNodeId());
        }
    }
}
