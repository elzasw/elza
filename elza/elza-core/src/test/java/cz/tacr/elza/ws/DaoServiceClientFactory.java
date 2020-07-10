package cz.tacr.elza.ws;

import javax.xml.namespace.QName;

import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.elza.ws.core.v1.CoreService;
import cz.tacr.elza.ws.core.v1.DaoService;
import cz.tacr.elza.ws.core.v1.FundService;
import cz.tacr.elza.ws.core.v1.StructuredObjectService;
import cz.tacr.elza.ws.core.v1.UserService;

public class DaoServiceClientFactory {
    static Logger log = LoggerFactory.getLogger(DaoServiceClientFactory.class);

    public static DaoService createDaoService(String address, String username, String password) {
        return createWsdlService(DaoService.class, address, CoreService.DaoCoreService, username, password);
    }

    public static FundService createFundService(String address, String username, String password) {
        return createWsdlService(FundService.class, address, CoreService.FundService, username, password);
    }

    public static StructuredObjectService createStructuredObjectService(String address, String username,
                                                                        String password) {
        return createWsdlService(StructuredObjectService.class, address, CoreService.StructuredObjects, username,
                                 password);
    }

    public static UserService createUserService(String address, String username,
                                                            String password) {
        return createWsdlService(UserService.class, address, CoreService.UserService, username,
                                 password);
    }

    public static <T> T createWsdlService(Class<T> targetCls, String address, QName serviceName,
                                          String username,
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
