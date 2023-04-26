package cz.tacr.elza.controller;

import java.io.InputStream;
import java.util.List;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import cz.tacr.elza.common.FactoryUtils;
import cz.tacr.elza.controller.config.ClientFactoryDO;
import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.CreateFund;
import cz.tacr.elza.controller.vo.FindFundsResult;
import cz.tacr.elza.controller.vo.Fund;
import cz.tacr.elza.controller.vo.FundDetail;
import cz.tacr.elza.controller.vo.UpdateFund;
import cz.tacr.elza.core.data.RuleSet;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrStructuredObject;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.repository.FilteredResult;
import cz.tacr.elza.repository.FundRepository;
import cz.tacr.elza.repository.RuleSetRepository;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.StructObjService;
import cz.tacr.elza.service.UserService;

@RestController
@RequestMapping("/api/v1")
public class FundController implements FundsApi {

    private static final Logger logger = LoggerFactory.getLogger(FundController.class);

    @Autowired
    private RuleSetRepository ruleSetRepository;

    @Autowired
    private ArrangementService arrangementService;

    @Autowired
    private AccessPointService accessPointService;

    @Autowired
    private FundRepository fundRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private ClientFactoryVO factoryVo;

    @Autowired
    private ClientFactoryDO factoryDO;

    @Autowired
    private StaticDataService staticDataService;

    @Autowired
    private ScopeRepository scopeRepository;

    @Autowired
    private StructObjService structureService;

    @Override
    @Transactional
    public ResponseEntity<Fund> createFund(@RequestBody CreateFund createFund) {
        // Kontrola a vytvoření AS
        Validate.isTrue(StringUtils.isNotBlank(createFund.getName()), "Musí být vyplněn název");
        Validate.notNull(createFund.getInstitutionIdentifier(), "Identifikátor instituce musí být vyplněn");
        Validate.notNull(createFund.getRuleSetCode(), "Identifikátor pravidel musí být vyplněn");
        Validate.notNull(createFund.getScopes(), "Musí být zadána alespoň jedna oblast zařazení");
        Validate.notEmpty(createFund.getScopes(), "Musí být zadána alespoň jedna oblast zařazení");

        StaticDataProvider sdp = staticDataService.getData();

        // prepare ruleset
        RuleSet ruleSet = sdp.getRuleSetByCode(createFund.getRuleSetCode());
        Validate.notNull(ruleSet, "Nebyla nalezena pravidla tvorby s kódem " + createFund.getRuleSetCode());

        // prepare institution
        ParInstitution institution = arrangementService.getInstitution(createFund.getInstitutionIdentifier());
        Validate.notNull(institution, "Nebyla nalezena instituce s identifikátorem " + createFund
                .getInstitutionIdentifier());

        // prepare collection of scopes
        List<ApScope> scopes = scopeRepository.findByCodes(createFund.getScopes());
        Validate.isTrue(scopes.size() == createFund.getScopes().size(),
                      "Některá oblast archivních entit nebyla nalezena");

        ArrFund newFund = arrangementService
                .createFundWithScenario(createFund.getName(), ruleSet.getEntity(), createFund.getInternalCode(),
                                        institution, createFund.getFundNumber(),
                                        createFund.getUnitdate(), createFund.getMark(),
                                        createFund.getUuid(),
                                        scopes, createFund.getAdminUsers(), createFund.getAdminGroups());

        UserDetail userDetail = userService.getLoggedUserDetail();

        return ResponseEntity.ok(factoryVo.createFund(newFund, userDetail));
    }

    @Override
    public ResponseEntity<FindFundsResult> findFunds(@RequestParam(value = "fulltext", required = false) String fulltext,
                                                     @RequestParam(value = "institutionIdentifier", required = false) String institutionIdentifier,
                                                     @RequestParam(value = "max", required = false, defaultValue = "200") Integer max,
                                                     @RequestParam(value = "from", required = false, defaultValue = "0") Integer from) {
        UserDetail userDetail = userService.getLoggedUserDetail();
        FilteredResult<ArrFund> funds;
        Integer institutionId = null;
        if (institutionIdentifier != null && !institutionIdentifier.isEmpty()) {
            ParInstitution institution = arrangementService.getInstitution(institutionIdentifier);
            if (institution != null) {
                institutionId = institution.getInstitutionId();
            } else {
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
        Validate.notNull(id, "Musí být zadáno id AS");
        UserDetail userDetail = userService.getLoggedUserDetail();
        ArrFund fund = arrangementService.getFund(Integer.valueOf(id));
        return ResponseEntity.ok(factoryVo.createFundDetail(fund,
                                                            userDetail));
    }

    @Override
    @Transactional
    public ResponseEntity<Void> importFundData(@PathVariable("id") String id,
                                               @Valid @RequestPart(value = "importType", required = true) String importType,
                                               @Valid @RequestPart(value = "dataFile", required = true) MultipartFile dataFile) {
        Validate.notNull(id, "Musí být zadáno id AS");

        ArrFund fund = arrangementService.getFund(Integer.valueOf(id));
        try (InputStream is = dataFile.getInputStream()) {
            arrangementService.importFundData(fund, importType, is);
            return ResponseEntity.ok(null);
        } catch (Exception e) {
            logger.error("Failed to import data", e);
            return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @Override
    @Transactional
    public ResponseEntity<FundDetail> updateFund(@PathVariable("id") String id, @RequestBody UpdateFund updateFund) {
        Validate.notNull(updateFund, "AS musí být vyplněn");
        Validate.notNull(updateFund.getRuleSetCode(), "AS musí mít přiřazená pravidla");

        ParInstitution institution = arrangementService.getInstitution(updateFund.getInstitutionIdentifier());

        List<ApScope> apScopes = FactoryUtils.transformList(updateFund.getScopes(), s -> accessPointService.getApScope(s));

        ArrFund arrFund = factoryDO.createFund(updateFund, institution, id);
        RulRuleSet ruleSet = ruleSetRepository.findByCode(updateFund.getRuleSetCode());
        Validate.notNull(ruleSet);
        ArrFund updatedFund = arrangementService.updateFund(arrFund, ruleSet, apScopes, null, null);

        return ResponseEntity.ok(factoryVo.createFundDetail(updatedFund, userService.getLoggedUserDetail()));
    }

    /**
     * Smazání hodnot strukturovaného datového typu.
     *
     * @param fundVersionId    identifikátor verze AS
     * @param structureDataIds identifikátory hodnot strukturovaného datového typu
     * @return smazané entity
     */
    @Override
    @Transactional
    public ResponseEntity<List<Integer>> deleteStructureData(final Integer fundVersionId, final List<Integer> structureDataIds) {
        ArrFundVersion fundVersion = arrangementService.getFundVersionById(fundVersionId);
        List<ArrStructuredObject> structObjList = structureService.getStructObjByIds(structureDataIds);
        List<Integer> deletedIds = structureService.deleteStructObj(fundVersion.getFundId(), structObjList);

        return ResponseEntity.ok(deletedIds);
    }
}
