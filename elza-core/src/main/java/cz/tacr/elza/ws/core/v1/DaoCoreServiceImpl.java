
/**
 * Please modify this class to meet your needs
 * This class is not complete
 */

package cz.tacr.elza.ws.core.v1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import cz.tacr.elza.ws.core.v1.daoservice.DaoCoreServiceWsImpl;
import cz.tacr.elza.ws.types.v1.DaoImport;
import cz.tacr.elza.ws.types.v1.DaoLink;
import cz.tacr.elza.ws.types.v1.DaoPackage;
import cz.tacr.elza.ws.types.v1.Did;

@Component
@javax.jws.WebService(serviceName = "CoreService", portName = "DaoCoreService", targetNamespace = "http://elza.tacr.cz/ws/core/v1",
        //                      wsdlLocation = "file:elza-core-v1.wsdl",
        endpointInterface = "cz.tacr.elza.ws.core.v1.DaoService")
public class DaoCoreServiceImpl implements DaoService {

    private Logger logger = LoggerFactory.getLogger(DaoCoreServiceImpl.class);

    @Autowired
    private DaoCoreServiceWsImpl daoCoreServiceWsImpl;

    /*
     * @see cz.tacr.elza.ws.core.v1.DaoService#_import(cz.tacr.elza.ws.types.v1.DaoImport daoImport)
     */
    @Override
    public void _import(final DaoImport daoImport) throws CoreServiceException {
        try {
            logger.info("Executing operation daoImport");

            daoCoreServiceWsImpl.daoImport(daoImport);

            logger.info("Finished operation daoImport");
        } catch (Exception e) {
            logger.error("Fail operation daoImport", e);
            throw WSHelper.prepareException("DAO import failed", e);
        }
    }

    /*
     * @see cz.tacr.elza.ws.core.v1.DaoService#addPackage(cz.tacr.elza.ws.types.v1.DaoPackage daoPackage)*
     */
    @Override
    public String addPackage(final DaoPackage daoPackage) throws CoreServiceException {
        try {
            logger.info("Executing operation addPackage");
            String result = daoCoreServiceWsImpl.addPackage(daoPackage);
            logger.info("Ending operation addPackage, result: {}", result);
            return result;
        } catch (Exception e) {
            logger.error("Fail operation addPackage", e);
            throw WSHelper.prepareException("addPackage failed", e);
        }
    }

    /* (non-Javadoc)
     * @see cz.tacr.elza.ws.core.v1.DaoService#removePackage(java.lang.String packageIdentifier)
     */
    @Override
    @Transactional
    public void removePackage(final String packageIdentifier) throws CoreServiceException {
        try {
            logger.info("Executing operation removePackage");

            daoCoreServiceWsImpl.removePackage(packageIdentifier);

            logger.info("Ending operation removePackage");
        } catch (Exception e) {
            logger.error("Fail operation removePackage", e);
            throw WSHelper.prepareException("removePackage failed", e);
        }
    }

    /*
     * @see cz.tacr.elza.ws.core.v1.DaoService#link(cz.tacr.elza.ws.types.v1.DaoLink daoLink)*
     */
    @Override
    public void link(final DaoLink daoLink) throws CoreServiceException {
        try {
            logger.info("Executing operation link");

            daoCoreServiceWsImpl.link(daoLink);

            logger.info("Ending operation link");
        } catch (Exception e) {
            logger.error("Fail operation link", e);
            throw WSHelper.prepareException("link failed", e);
        }
    }

    /* (non-Javadoc)
     * @see cz.tacr.elza.ws.core.v1.DaoService#removeDao(java.lang.String packageIdentifier)*
     */
    @Override
    public void removeDao(final String daoIdentifier) throws CoreServiceException {
        try {
            logger.info("Executing operation removeDao");
            
            daoCoreServiceWsImpl.removeDao(daoIdentifier);

            logger.info("Ending operation removeDao");
        } catch (Exception e) {
            logger.error("Fail operation removeDao", e);
            throw WSHelper.prepareException("RemoveDao failed", e);
        }
    }

    /* (non-Javadoc)
     * @see cz.tacr.elza.ws.core.v1.DaoService#getDid(java.lang.String packageIdentifier)*
     */
    public Did getDid(String didIdentifier) throws CoreServiceException {
        try {
            logger.info("Executing operation getDid");

            Did result = daoCoreServiceWsImpl.getDid(didIdentifier);

            logger.info("Ending operation getDid");
            return result;
        } catch (Exception e) {
            logger.error("Fail operation getDid", e);
            throw WSHelper.prepareException("getDid failed", e);
        }
    }

}
