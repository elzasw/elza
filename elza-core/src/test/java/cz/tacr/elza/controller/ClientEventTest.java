package cz.tacr.elza.controller;

import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import cz.tacr.elza.controller.vo.ArrFindingAidVO;
import cz.tacr.elza.service.ClientDataChangesServiceTest;
import cz.tacr.elza.service.IClientDataChangesService;
import cz.tacr.elza.service.eventnotification.events.AbstractEventSimple;
import cz.tacr.elza.service.eventnotification.events.EventId;
import cz.tacr.elza.service.eventnotification.events.EventType;


/**
 * Test vyvolání událostí.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 14.01.2016
 */
public class ClientEventTest extends AbstractRestTest {


    @Autowired
    private IClientDataChangesService clientDataChangesService;

    @Test
    public void testEventPublish() {
        ArrFindingAidVO test_publish = createFindingAidRestV2("test_publish");


        Collection<AbstractEventSimple> lastEvents = ((ClientDataChangesServiceTest) clientDataChangesService)
                .getLastFiredEvents();

        Assert.assertTrue(lastEvents.size() > 0);

        EventId createFAevent = null;
        for (AbstractEventSimple lastEvent : lastEvents) {
            if (lastEvent.getEventType().equals(EventType.FINDING_AID_CREATE)) {
                createFAevent = (EventId) lastEvent;
                break;
            }
        }

        Assert.assertNotNull(createFAevent);
        Assert.assertTrue(createFAevent.getIds().contains(test_publish.getId()));
    }

}
