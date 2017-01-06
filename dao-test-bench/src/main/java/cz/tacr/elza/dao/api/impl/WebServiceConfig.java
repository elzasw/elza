package cz.tacr.elza.dao.api.impl;

import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import cz.tacr.elza.dao.DCStorageConfig;
import cz.tacr.elza.ws.dao_service.v1.DaoNotifications;
import cz.tacr.elza.ws.dao_service.v1.DaoRequests;

@Configuration
@ImportResource({ "classpath:META-INF/cxf/cxf.xml" })
public class WebServiceConfig {

	@Autowired
	private DaoNotifications daoNotifications;

	@Autowired
	private DCStorageConfig storageConfig;

	@Autowired
	private DaoRequests daoRequests;

	@Autowired
	private Bus bus;

	@Bean
	public EndpointImpl notificationsEndpoint() {
		EndpointImpl endpoint = new EndpointImpl(bus, daoNotifications);
		endpoint.publish("/notifications");
		return endpoint;
	}

	@Bean
	public EndpointImpl requestsEndpoint() {
		EndpointImpl endpoint = new EndpointImpl(bus, daoRequests);
		endpoint.publish("/requests");
		return endpoint;
	}

	@Bean
	public ServletRegistrationBean CXFServlet() {
		ServletRegistrationBean servlet = new ServletRegistrationBean(new CXFServlet(),
				"/" + storageConfig.getRepositoryIdentifier() + "/*");
		servlet.setLoadOnStartup(1);
		return servlet;
	}
}