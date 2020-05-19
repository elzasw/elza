package cz.tacr.elza.controller;

import cz.tacr.elza.common.FactoryUtils;
import cz.tacr.elza.controller.config.ClientFactoryDO;
import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.*;
import cz.tacr.elza.core.data.SearchType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.repository.FilteredResult;
import cz.tacr.elza.repository.FundRepository;
import cz.tacr.elza.repository.InstitutionRepository;
import cz.tacr.elza.repository.RuleSetRepository;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toSet;

@RestController
@RequestMapping("/api/v1")
public class FundController implements FundsApi {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private RuleSetRepository ruleSetRepository;

    @Autowired
    private ArrangementService arrangementService;

    @Autowired
    private FundRepository fundRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private ClientFactoryVO factoryVo;

    @Autowired
    private ClientFactoryDO factoryDO;

    @Override
    @Transactional
    public ResponseEntity<Fund> createFund(@RequestBody CreateFund createFund) {
        // Kontrola a vytvoření AS
        Assert.hasText(createFund.getName(), "Musí být vyplněn název");
        Assert.notNull(createFund.getInstitutionIdentifier(), "Identifikátor instituce musí být vyplněn");
        Assert.notNull(createFund.getRuleSetCode(), "Identifikátor pravidel musí být vyplněn");
        Assert.notNull(createFund.getScopes(), "Musí být zadána alespoň jedna oblast zařazení");
        Assert.notEmpty(createFund.getScopes(), "Musí být zadána alespoň jedna oblast zařazení");

        RulRuleSet ruleSet = ruleSetRepository.findByCode(createFund.getRuleSetCode());
        Assert.notNull(ruleSet, "Nebyla nalezena pravidla tvorby s kódem " + createFund.getRuleSetCode());

        ParInstitution institution = arrangementService.getInstitution(createFund.getInstitutionIdentifier());

        Assert.notNull(institution, "Nebyla nalezena instituce s identifikátorem " + createFund.getInstitutionIdentifier());

        ArrFund newFund = arrangementService
                .createFundWithScenario(createFund.getName(), ruleSet, createFund.getInternalCode(), institution, createFund.getDateRange(), createFund.getFundNumber(), createFund.getUnitdate(), createFund.getMark(), createFund.getUuid());

        // Kontrola na vyplněnost uživatele nebo skupiny jako správce, pokud není admin
        UserDetail userDetail = userService.getLoggedUserDetail();
        if (!userDetail.hasPermission(UsrPermission.Permission.FUND_ADMIN)) {
            if (ObjectUtils.isEmpty(createFund.getAdminUsers()) && ObjectUtils.isEmpty(createFund.getAdminGroups())) {
                Assert.isTrue(false, "Nebyl vybrán správce");
            }

            // Kontrola, zda daní uživatelé a skupiny mají oprávnění zakládat AS
            // pokud není admin, musí zadat je uživatele, kteří mají oprávnění (i zděděné) na zakládání nových AS
            if (createFund.getAdminUsers() != null && !createFund.getAdminUsers().isEmpty()) {
                // TODO: Remove stream and user more direct query
                final Set<Integer> userIds = userService.findUserWithFundCreate(null, 0, -1, SearchType.DISABLED, SearchType.FULLTEXT).getList().stream()
                        .map(x -> x.getUserId())
                        .collect(toSet());
                createFund.getAdminUsers()
                        .forEach(u -> {
                            if (!userIds.contains(u)) {
                                throw new BusinessException("Předaný správce (uživatel) nemá oprávnení zakládat AS", ArrangementCode.ADMIN_USER_MISSING_FUND_CREATE_PERM).set("id", u);
                            }
                        });
            }
            if (createFund.getAdminGroups() != null && !createFund.getAdminGroups().isEmpty()) {
                final Set<Integer> groupIds = userService.findGroupWithFundCreate(null, 0, -1).getList().stream()
                        .map(x -> x.getGroupId())
                        .collect(toSet());
                createFund.getAdminGroups()
                        .forEach(g -> {
                            if (!groupIds.contains(g)) {
                                throw new BusinessException("Předaný správce (skupina) nemá oprávnení zakládat AS", ArrangementCode.ADMIN_GROUP_MISSING_FUND_CREATE_PERM).set("id", g);
                            }
                        });
            }
        }

        // Oprávnění na uživatele a skupiny
        if (createFund.getAdminUsers() != null && !createFund.getAdminUsers().isEmpty()) {
            // add permissions to selectected users
            createFund.getAdminUsers().forEach(
                    u -> userService.addFundAdminPermissions(u, null, newFund));
        }
        if (createFund.getAdminGroups() != null && !createFund.getAdminGroups().isEmpty()) {
            // add permissions to selectected groups
            createFund.getAdminGroups().forEach(
                    g -> userService.addFundAdminPermissions(null, g, newFund));
        }

        return ResponseEntity.ok(factoryVo.createFund(newFund, userDetail));
    }

    @Override
    public ResponseEntity<FindFundsResult> findFunds(@RequestParam(value = "fulltext", required = false) String fulltext,
                                                     @RequestParam(value = "institutionIdentifier", required = false) String institutionIdentifier,
                                                     @RequestParam(value = "max", required = false, defaultValue="200") Integer max,
                                                     @RequestParam(value = "from", required = false, defaultValue="0") Integer from) {
        UserDetail userDetail = userService.getLoggedUserDetail();
        FilteredResult<ArrFund> funds;
        Integer institutionId = null;
        if (institutionIdentifier != null && !institutionIdentifier.isEmpty()) {
            ParInstitution institution = arrangementService.getInstitution(institutionIdentifier);
            if (institution != null) {
                institutionId = institution.getInstitutionId();
            }
            else {
                FindFundsResult fundsResult = new FindFundsResult();
                return ResponseEntity.ok(fundsResult);
            }
        }

        if (userDetail.hasPermission(UsrPermission.Permission.FUND_RD_ALL)) {
            // read all funds
            funds = fundRepository.findFunds(fulltext, institutionId, from, max);

        } else {
            Integer userId = userDetail.getId();
            funds = fundRepository.findFundsWithPermissions(fulltext, institutionId, from, max, userId);
        }

        List<ArrFund> fundList = funds.getList();
        FindFundsResult fundsResult = new FindFundsResult();
        fundsResult.setTotalCount(funds.getTotalCount());
        fundList.forEach(f -> {
            Fund fund = factoryVo.createFund(f.getFund(), userDetail);
            fundsResult.addFundsItem(fund);
        });

        return ResponseEntity.ok(fundsResult);

    }

    @Override
    public ResponseEntity<FundDetail> getFund(@PathVariable("id") String id) {
        Assert.notNull(id, "Musí být zadáno id AS");
        UserDetail userDetail = userService.getLoggedUserDetail();
        return ResponseEntity.ok(factoryVo.createFundDetail(arrangementService.getFund(Integer.valueOf(id)), userDetail));
    }

    @Override
    @Transactional
    public ResponseEntity<FundDetail> updateFund(@PathVariable("id") String id, @RequestBody UpdateFund updateFund) {
        Assert.notNull(updateFund, "AS musí být vyplněn");
        Assert.notNull(updateFund.getRuleSetCode(), "AS musí mít přiřazená pravidla");

        ParInstitution institution = arrangementService.getInstitution(updateFund.getInstitutionIdentifier());

        List<ApScope> apScopes = FactoryUtils.transformList(updateFund.getScopes(), s -> arrangementService.getApScope(s));

        return ResponseEntity.ok(factoryVo.createFundDetail(arrangementService.updateFund(
                factoryDO.createFund(updateFund, institution, id),
                ruleSetRepository.findByCode(updateFund.getRuleSetCode()),
                apScopes
        ), userService.getLoggedUserDetail()));
    }
}
