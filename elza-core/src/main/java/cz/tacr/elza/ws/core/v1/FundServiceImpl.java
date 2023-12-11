package cz.tacr.elza.ws.core.v1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import cz.tacr.elza.ws.core.v1.fundservice.FundServiceWsImpl;
import cz.tacr.elza.ws.types.v1.Fund;
import cz.tacr.elza.ws.types.v1.FundIdentifiers;

/**
 * Implementace WSDL/FundService
 * 
 * Volání jsou delegována do FundServiceWsImpl
 * pro zapouzdření transakcí.
 *
 */
@Component
@jakarta.jws.WebService(serviceName = "CoreService", portName = "FundService", targetNamespace = "http://elza.tacr.cz/ws/core/v1",
        //                      wsdlLocation = "file:elza-core-v1.wsdl",
        endpointInterface = "cz.tacr.elza.ws.core.v1.FundService")
public class FundServiceImpl implements FundService {

    private Logger logger = LoggerFactory.getLogger(FundServiceImpl.class);

    @Autowired
    protected FundServiceWsImpl fundServiceWsImpl;

    @Override
    @Transactional
    public FundIdentifiers createFund(Fund fundInfo) throws CreateFundException {
        logger.debug("Received createFund");

        try {
            FundIdentifiers result = fundServiceWsImpl.createFund(fundInfo);
            logger.debug("Finished createFund, fundId: {}", result.getId());
            return result;
        } catch (Exception e) {
            logger.debug("Failed to create fund", e);
            throw WSHelper.prepareException("Failed to create fund", e);
        }
    }

    @Override
    @Transactional
    public void deleteFund(FundIdentifiers fundInfo) throws DeleteFundException {
        logger.debug("Received deleteFund");

        try {
            fundServiceWsImpl.deleteFund(fundInfo);
            logger.debug("Finished deleteFund");
        } catch (Exception e) {
            logger.debug("Failed to delete fund", e);
            throw WSHelper.prepareException("Failed to delete fund", e);
        }
    }

    @Override
    @Transactional
    public void updateFund(Fund fundUpdate) throws UpdateFundException {
        logger.debug("Received updateFund");

        try {
            fundServiceWsImpl.updateFund(fundUpdate);
            logger.debug("Finished updateFund");
        } catch (Exception e) {
            logger.debug("Failed to delete fund", e);
            throw WSHelper.prepareException("Failed to delete fund", e);
        }

    }

}
