package cz.tacr.elza.ws.core.v1.fundservice;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cz.tacr.elza.core.data.RuleSet;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.repository.InstitutionRepository;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.ws.core.v1.CreateFundException;
import cz.tacr.elza.ws.core.v1.DeleteFundException;
import cz.tacr.elza.ws.core.v1.UpdateFundException;
import cz.tacr.elza.ws.core.v1.WSHelper;
import cz.tacr.elza.ws.types.v1.ErrorDescription;
import cz.tacr.elza.ws.types.v1.Fund;
import cz.tacr.elza.ws.types.v1.FundIdentifiers;

/**
 * Skutečná implementace WSDL služeb
 * 
 *
 */
@Service
public class FundServiceWsImpl {

    private Logger logger = LoggerFactory.getLogger(FundServiceWsImpl.class);

    @Autowired
    ArrangementService arrangementService;

    @Autowired
    InstitutionRepository instRepo;

    @Autowired
    StaticDataService staticDataService;

    @Autowired
    WSHelper wsHelper;

    @Transactional
    public FundIdentifiers createFund(Fund fundInfo) {
        StaticDataProvider sdp = staticDataService.getData();

        RuleSet ruleset = sdp.getRuleSetByCode(fundInfo.getRulesetCode());

        String uuid = fundInfo.getUuid();
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
        }

        if (fundInfo.getInstitutionIdentifier() == null) {
            logger.error("Missing institution identifier: {}", fundInfo.getInstitutionIdentifier());

            ErrorDescription errorDesc = WSHelper.prepareErrorDescription("Missing institution ID", null);
            throw new CreateFundException(errorDesc.getUserMessage(), errorDesc);
        }

        ParInstitution institution = instRepo.findByInternalCode(fundInfo.getInstitutionIdentifier());
        if (institution == null) {
            logger.error("Institution not found: {}", fundInfo.getInstitutionIdentifier());

            ErrorDescription errorDesc = WSHelper.prepareErrorDescription("Failed to find institution ID: "
                    + fundInfo.getInstitutionIdentifier(),
                                                                          null);
            throw new CreateFundException(errorDesc.getUserMessage(), errorDesc);
        }

        Integer fundNumber = null;
        if (StringUtils.isNotBlank(fundInfo.getFundNumber())) {
            fundNumber = Integer.valueOf(fundInfo.getFundNumber().trim());
        }
        ArrFund fund = arrangementService.createFundWithScenario(fundInfo.getFundName(),
                                                                 ruleset.getEntity(),
                                                                 fundInfo.getInternalCode(),
                                                                 institution,
                                                                 fundNumber,
                                                                 fundInfo.getDateRange(),
                                                                 fundInfo.getMark(),
                                                                 uuid, null);
        FundIdentifiers fi = new FundIdentifiers();
        fi.setId(fund.getFundId().toString());
        fi.setUuid(uuid);

        return fi;
    }

    @Transactional
    public void deleteFund(FundIdentifiers fundInfo) throws DeleteFundException {
        Integer fundId = wsHelper.getFundId(fundInfo);
        arrangementService.deleteFund(fundId);
    }

    @Transactional
    public void updateFund(Fund fundUpdate) throws UpdateFundException {
        ArrFund fund = wsHelper.getFund(getFundInfo(fundUpdate));

        StaticDataProvider sdp = staticDataService.getData();
        if (fundUpdate.getFundName() != null) {
            fund.setName(fundUpdate.getFundName());
        }
        RuleSet ruleSet = sdp.getRuleSetByCode(fundUpdate.getRulesetCode());

        if (fundUpdate.getDateRange() != null) {
            fund.setUnitdate(fundUpdate.getDateRange());
        }
        if (fundUpdate.getFundNumber() != null) {
            Integer fundNumber = Integer.valueOf(fundUpdate.getFundNumber());
            fund.setFundNumber(fundNumber);
        }
        if (fundUpdate.getInternalCode() != null) {
            fund.setInternalCode(fundUpdate.getInternalCode());
        }
        if (fundUpdate.getMark() != null) {
            fund.setMark(fundUpdate.getMark());
        }
        if (fundUpdate.getInstitutionIdentifier() != null) {
            ParInstitution institution = instRepo.findByInternalCode(fundUpdate.getInstitutionIdentifier());
            if (institution == null) {
                logger.error("Institution not found: {}", fundUpdate.getInstitutionIdentifier());

                ErrorDescription errorDesc = WSHelper.prepareErrorDescription("Failed to find institution ID: "
                        + fundUpdate.getInstitutionIdentifier(),
                                                                              null);
                throw new CreateFundException(errorDesc.getUserMessage(), errorDesc);
            }
            fund.setInstitution(institution);
        }

        arrangementService.updateFund(fund, ruleSet.getEntity(), null);
    }

    private FundIdentifiers getFundInfo(Fund fund) {
        FundIdentifiers fundInfo = new FundIdentifiers();
        fundInfo.setId(fund.getId());
        fundInfo.setUuid(fund.getUuid());
        return fundInfo;
    }

}
