package cz.tacr.elza.ws;

import javax.xml.namespace.QName;

import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.elza.ws.core.v1.CoreService;
import cz.tacr.elza.ws.core.v1.DaoService;

public class DaoServiceClientFactory {
    static Logger log = LoggerFactory.getLogger(DaoServiceClientFactory.class);

    public static DaoService createDaoService(String address, String username, String password) {
        return createDaoService(DaoService.class, address, CoreService.DaoCoreService, username, password);
    }

    public static <T> T createDaoService(Class<T> targetCls, String address, QName serviceName, String username,
                                         String password) {
        try {
            JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
            factory.setServiceName(serviceName);
            factory.setAddress(address);
            if (username != null) {
                log.debug("Setting basic authorization for: {} / {}", username, password);

                factory.setUsername(username);
                factory.setPassword(password);
            }
            T t = factory.create(targetCls);
            return t;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
