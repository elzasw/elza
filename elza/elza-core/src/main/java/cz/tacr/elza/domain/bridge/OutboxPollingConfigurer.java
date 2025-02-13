package cz.tacr.elza.domain.bridge;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.hibernate.Session;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.AgentRepositoryProvider;
import org.hibernate.search.mapper.orm.outboxpolling.event.impl.DefaultOutboxEventFinder;
import org.hibernate.search.mapper.orm.outboxpolling.event.impl.DefaultOutboxEventFinder.Provider;
import org.hibernate.search.mapper.orm.outboxpolling.event.impl.OutboxEvent;
import org.hibernate.search.mapper.orm.outboxpolling.event.impl.OutboxEventFinder;
import org.hibernate.search.mapper.orm.outboxpolling.event.impl.OutboxEventFinderProvider;
import org.hibernate.search.mapper.orm.outboxpolling.event.impl.OutboxEventPredicate;
import org.hibernate.search.mapper.orm.outboxpolling.impl.OutboxPollingInternalConfigurer;
import org.hibernate.search.util.common.spi.ToStringTreeAppender;

public class OutboxPollingConfigurer implements OutboxPollingInternalConfigurer {
	
	private static boolean indexingEnabled = false;
	
	class PausableOutboxEventFinder implements OutboxEventFinder {
		
		private DefaultOutboxEventFinder delegate;

		PausableOutboxEventFinder(final DefaultOutboxEventFinder finder) {
    		this.delegate = finder;
		}
        
        @Override
		public List<OutboxEvent> findOutboxEvents(Session session, int maxResults) {
        	if(!indexingEnabled) {
        		return Collections.emptyList();
        	}
        	return delegate.findOutboxEvents(session, maxResults);
		}		
    }
	
	
	public static void setIndexingEnabled(boolean enabled) {
        indexingEnabled = enabled;
    }
		
	public OutboxPollingConfigurer() {
		
	}

	@Override
	public OutboxEventFinderProvider wrapEventFinder(Provider delegate) {
		return new OutboxEventFinderProvider() {

			@Override
			public void appendTo(ToStringTreeAppender appender) {
				delegate.appendTo(appender);				
			}

			@Override
			public OutboxEventFinder create(Optional<OutboxEventPredicate> predicate) {
				DefaultOutboxEventFinder finder = delegate.create(predicate);
				if(!indexingEnabled) {
					return new PausableOutboxEventFinder(finder);
				}
				return finder;
			}
    		
		};
	}

	@Override
	public AgentRepositoryProvider wrapAgentRepository(AgentRepositoryProvider delegate) {
		return OutboxPollingInternalConfigurer.DEFAULT.wrapAgentRepository(delegate);
	}

}
