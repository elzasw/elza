package cz.tacr.elza.domain.bridge;

import static cz.tacr.elza.domain.ArrDescItem.FIELD_DELETE_CHANGE;

import org.hibernate.search.mapper.pojo.bridge.RoutingBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.RoutingBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingBridgeRouteContext;
import org.hibernate.search.mapper.pojo.route.DocumentRoutes;

import cz.tacr.elza.domain.ArrDescItem;

public class ArrDescItemRoutingBinder implements RoutingBinder {

	@Override
	public void bind(RoutingBindingContext context) {
        context.dependencies().use(FIELD_DELETE_CHANGE);
        context.bridge(ArrDescItem.class, new Bridge());
	}

	public static class Bridge implements RoutingBridge<ArrDescItem> {

		@Override
		public void route(DocumentRoutes routes, Object entityIdentifier, ArrDescItem indexedEntity, RoutingBridgeRouteContext context) {
			if (indexedEntity.getDeleteChange() == null) {
				routes.addRoute();
				return;
			}
			routes.notIndexed();
		}

		@Override
		public void previousRoutes(DocumentRoutes routes, Object entityIdentifier, ArrDescItem indexedEntity, RoutingBridgeRouteContext context) {
			routes.addRoute();
		}
		
	}
}
