package cz.tacr.elza.ws.core.v1;

import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.repository.InstitutionRepository;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.ws.types.v1.ErrorDescription;
import cz.tacr.elza.ws.types.v1.Fund;
import cz.tacr.elza.ws.types.v1.FundIdentifiers;

@Component
@javax.jws.WebService(serviceName = "CoreService", portName = "FundService", targetNamespace = "http://elza.tacr.cz/ws/core/v1",
        //                      wsdlLocation = "file:elza-core-v1.wsdl",
        endpointInterface = "cz.tacr.elza.ws.core.v1.FundService")
public class FundServiceImpl implements FundService {

    @Autowired
    ArrangementService arrangementService;

    @Autowired
    InstitutionRepository instRepo;

    @Autowired
    StaticDataService staticDataService;

    @Autowired
    WSHelper wsHelper;

    @Override
    @Transactional
    public FundIdentifiers createFund(Fund fundInfo) throws CreateFundException {
        StaticDataProvider sdp = staticDataService.getData();

        String uuid = fundInfo.getUuid();
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
        }
        
        ArrChange change = arrangementService.createChange(ArrChange.Type.CREATE_AS);


        RulRuleSet ruleset = sdp.getRuleSetByCode(fundInfo.getRulesetCode());
        if (fundInfo.getInstitutionIdentifier() == null) {
            ErrorDescription errorDesc = WSHelper.prepareErrorDescription("Missing institution ID", null);
            throw new CreateFundException(errorDesc.getUserMessage(), errorDesc);
        }
        ParInstitution institution = instRepo.findByInternalCode(fundInfo.getInstitutionIdentifier());
        if (institution == null) {
            ErrorDescription errorDesc = WSHelper.prepareErrorDescription("Failed to find institution ID: "
                    + fundInfo.getInstitutionIdentifier(),
                                                                          null);
            throw new CreateFundException(errorDesc.getUserMessage(), errorDesc);
        }
        Integer fundNumber = null;
        if(StringUtils.isNotBlank(fundInfo.getId())) {
            fundNumber = Integer.valueOf(fundInfo.getId().trim());
        }
        ArrFund fund = arrangementService.createFund(
                                                     fundInfo.getFundName(),
                                                     ruleset,
                                                     change,
                                                     uuid,
                                                     fundInfo.getInternalCode(),
                                                     institution,
                                                     fundInfo.getDateRange(),
                                                     fundNumber,
                                                     null,
                                                     fundInfo.getMark());
        FundIdentifiers fi = new FundIdentifiers();
        fi.setId(fund.getFundId().toString());
        fi.setUuid(uuid);
        return fi;
    }

    @Override
    @Transactional
    public void deleteFund(FundIdentifiers fundInfo) throws DeleteFundException {
        Integer fundId = wsHelper.getFundId(fundInfo);
        arrangementService.deleteFund(fundId);
    }

    @Override
    public void updateFund(Fund fundUpdate) throws UpdateFundException {
        ArrFund fund = wsHelper.getFund(getFundInfo(fundUpdate));

        StaticDataProvider sdp = staticDataService.getData();
        if (fundUpdate.getFundName() != null) {
            fund.setName(fundUpdate.getFundName());
        }
        RulRuleSet ruleSet = sdp.getRuleSetByCode(fundUpdate.getRulesetCode());

        arrangementService.updateFund(fund, ruleSet, null);
    }

    private FundIdentifiers getFundInfo(Fund fund) {
        FundIdentifiers fundInfo = new FundIdentifiers();
        fundInfo.setId(fund.getId());
        fundInfo.setUuid(fund.getUuid());
        return fundInfo;
    }

}
