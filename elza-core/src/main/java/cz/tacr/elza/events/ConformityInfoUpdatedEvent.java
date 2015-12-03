package cz.tacr.elza.events;

import java.util.Set;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 2.12.2015
 */
public class ConformityInfoUpdatedEvent {

    private Set<Integer> nodeIds;

    public ConformityInfoUpdatedEvent(final Set<Integer> nodeIds) {
        this.nodeIds = nodeIds;
    }

    public Set<Integer> getNodeIds() {
        return nodeIds;
    }
}
