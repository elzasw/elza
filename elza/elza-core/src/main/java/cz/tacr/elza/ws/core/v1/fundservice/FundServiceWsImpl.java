package cz.tacr.elza.ws.core.v1.fundservice;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cz.tacr.elza.core.data.RuleSet;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.repository.InstitutionRepository;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.repository.UserRepository;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.ws.core.v1.CreateFundException;
import cz.tacr.elza.ws.core.v1.DeleteFundException;
import cz.tacr.elza.ws.core.v1.UpdateFundException;
import cz.tacr.elza.ws.core.v1.WSHelper;
import cz.tacr.elza.ws.types.v1.ErrorDescription;
import cz.tacr.elza.ws.types.v1.Fund;
import cz.tacr.elza.ws.types.v1.FundIdentifiers;
import cz.tacr.elza.ws.types.v1.IdentifierList;

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
    ScopeRepository scopeRepository;
    
    @Autowired
    UserRepository userRepository;

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

        List<ApScope> scopes = getScopes(fundInfo.getScopes());
        List<Integer> userIds = getUserIds(fundInfo.getAdminUsers());
        List<Integer> groupIds = getGroupIds(fundInfo.getAdminGroups());

        ArrFund fund = arrangementService.createFundWithScenario(fundInfo.getFundName(),
                                                                 ruleset.getEntity(),
                                                                 fundInfo.getInternalCode(),
                                                                 institution,
                                                                 fundNumber,
                                                                 fundInfo.getDateRange(),
                                                                 fundInfo.getMark(),
                                                                 uuid, Boolean.TRUE, scopes, userIds, groupIds);
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

        List<ApScope> scopes = getScopes(fundUpdate.getScopes());
        List<Integer> userIds = getUserIds(fundUpdate.getAdminUsers());
        List<Integer> groupIds = getGroupIds(fundUpdate.getAdminGroups());

        arrangementService.updateFund(fund, ruleSet.getEntity(), scopes, userIds, groupIds);
    }

    private FundIdentifiers getFundInfo(Fund fund) {
        FundIdentifiers fundInfo = new FundIdentifiers();
        fundInfo.setId(fund.getId());
        fundInfo.setUuid(fund.getUuid());
        return fundInfo;
    }

    /**
     * Získání seznamu ApScope
     * 
     * @param scopeIds nebo scope codes
     * @return List<ApScope>
     */
    private List<ApScope> getScopes(IdentifierList scopeIds) {
        if (scopeIds != null) {
            List<String> strings = scopeIds.getIdentifier();
            if (strings != null && !strings.isEmpty()) {
                List<ApScope> scopes;
                // pokud se jedná o seznamu id
                if (StringUtils.isNumeric(strings.get(0))) {
                    List<Integer> ids = strings.stream().map(i -> Integer.valueOf(i)).collect(Collectors.toList());
                    scopes = scopeRepository.findAllById(ids);
                } else {
                    scopes = scopeRepository.findByCodes(strings);
                }
                Validate.isTrue(strings.size() == scopes.size(), "Nebyly nalezeny všechny ApScope");
                return scopes;
            }
        }
        return null;
    }

    /**
     * Získání seznamu User ids
     * 
     * @param userIds nebo user names
     * @return List<Integer>
     */
    private List<Integer> getUserIds(IdentifierList userIds) {
        if (userIds != null) {
            List<String> strings = userIds.getIdentifier();
            if (strings != null && !strings.isEmpty()) {
                List<Integer> ids;
                // pokud se jedná o seznamu id
                if (StringUtils.isNumeric(strings.get(0))) {
                    ids = strings.stream().map(u -> Integer.valueOf(u)).collect(Collectors.toList());
                } else {
                    ids = userRepository.findIdsByUsername(strings);
                    Validate.isTrue(strings.size() == ids.size(), "Nebyly nalezeny všechny UsrUser");
                }
                return ids;
            }
        }
        return null;
    }

    /**
     * Získání seznamu Group ids
     * 
     * @param groupIds
     * @return List<Integer>
     */
    private List<Integer> getGroupIds(IdentifierList groupIds) {
        if (groupIds != null) {
            List<String> strings = groupIds.getIdentifier();
            if (strings != null && !strings.isEmpty()) {
                List<Integer> ids = strings.stream().map(g -> Integer.valueOf(g)).collect(Collectors.toList());
                return ids;
            }
        }
        return null;
    }
}
