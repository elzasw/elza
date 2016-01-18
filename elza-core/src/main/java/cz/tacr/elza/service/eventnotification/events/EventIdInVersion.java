package cz.tacr.elza.service.eventnotification.events;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.util.Assert;


/**
 * Událost, která která nastala nad entitou konkrétní verze stromu a je potřeba její cache stromu přenačíst.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 15.01.2016
 */
public class EventIdInVersion extends AbstractEventSimple<EventIdInVersion> implements VersionTreeChange {

    /**
     * Mapa id verze stromu -> množina změněných nodů
     */
    private Map<Integer, Set<Integer>> values = new HashMap<>();


    /**
     * @param eventType typ události
     * @param versionId id verze
     * @param ids       množina nodů
     */
    public EventIdInVersion(final EventType eventType, final Integer versionId, final Integer... ids) {
        super(eventType);
        Assert.notNull(versionId);
        Assert.notNull(ids);


        Set<Integer> idsSet = new HashSet<>();
        for (Integer id : ids) {
            idsSet.add(id);
        }
        values.put(versionId, idsSet);
    }

    public EventIdInVersion(final EventType eventType, final Integer versionId, final Set<Integer> ids) {
        super(eventType);
        Assert.notNull(versionId);
        Assert.notNull(ids);

        values.put(versionId, ids);
    }


    public Map<Integer, Set<Integer>> getValues() {
        return values;
    }

    @Override
    public void appendEventData(final EventIdInVersion event) {
        Map<Integer, Set<Integer>> eventValues = event.getValues();
        Assert.notNull(eventValues);


        for (Map.Entry<Integer, Set<Integer>> eventValueEntry : eventValues.entrySet()) {
            Set<Integer> thisIds = values.get(eventValueEntry.getKey());
            if (thisIds == null) {
                thisIds = new HashSet<>();
                values.put(eventValueEntry.getKey(), thisIds);
            }
            thisIds.addAll(eventValueEntry.getValue());
        }
    }

    @Override
    public Set<Integer> getChangedVersionIds() {
        return values.keySet();
    }
}
