package cz.tacr.elza.controller;

import java.util.Collection;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import cz.tacr.elza.controller.vo.ArrFundVO;
import cz.tacr.elza.other.SimpleClientEventDispatcher;
import cz.tacr.elza.service.ClientEventDispatcher;
import cz.tacr.elza.service.eventnotification.events.AbstractEventSimple;
import cz.tacr.elza.service.eventnotification.events.EventId;
import cz.tacr.elza.service.eventnotification.events.EventType;
import cz.tacr.elza.test.ApiException;
import cz.tacr.elza.test.controller.vo.Fund;


/**
 * Test vyvolání událostí.
 */
public class ClientEventTest extends AbstractControllerTest {

    @Autowired
    private ClientEventDispatcher clientEventDispatcher;

    @Test
    public void testEventPublish() throws ApiException {
        SimpleClientEventDispatcher testDispatcher = (SimpleClientEventDispatcher) clientEventDispatcher;
        testDispatcher.clearFiredEvents();

        Fund test_publish = createFund("test_publish", "IC4");

        Collection<AbstractEventSimple> firedEvents = testDispatcher.getFiredEvents();

        Assert.assertTrue(firedEvents.size() > 0);

        AbstractEventSimple event = firedEvents.iterator().next();

        Assert.assertTrue(event.getEventType().equals(EventType.FUND_CREATE));
        Assert.assertTrue(event instanceof EventId);
        Assert.assertTrue(((EventId) event).getIds().contains(test_publish.getId()));
    }
}
