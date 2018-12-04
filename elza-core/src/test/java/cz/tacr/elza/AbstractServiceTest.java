package cz.tacr.elza;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import cz.tacr.elza.controller.config.ClientFactoryDO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemVO;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.dataexchange.input.DEImportParams;
import cz.tacr.elza.dataexchange.input.DEImportService;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.repository.InstitutionRepository;
import cz.tacr.elza.repository.RuleSetRepository;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.DescriptionItemService;
import cz.tacr.elza.service.StructObjService;

public abstract class AbstractServiceTest extends AbstractTest {
    @Autowired
    protected AccessPointService apService;

    @Autowired
    protected DEImportService deImportService;

    @Autowired
    protected InstitutionRepository institutionRepository;

    protected List<ParInstitution> institutions;

    protected ParInstitution firstInstitution;

    @Autowired
    protected RuleSetRepository rulesetRepos;

    @Autowired
    protected ArrangementService arrangementService;

    @Autowired
    protected DescriptionItemService descriptionItemService;

    @Autowired
    protected ClientFactoryDO factoryDO;

    @Autowired
    protected StructObjService structObjService;

    @Autowired
    protected StaticDataService staticDataService;

    public class FundInfo {

        List<RulRuleSet> rulesets;
        RulRuleSet firstRuleset;
        private ArrFund fund;
        private ArrFundVersion openVersion;
        private ArrNode rootNode;

        public void setRulesets(List<RulRuleSet> rulesets) {
            this.rulesets = rulesets;
            this.firstRuleset = rulesets.get(0);
        }

        public RulRuleSet getFirstRuleset() {
            return firstRuleset;
        }

        public void setFund(ArrFund fund) {
            this.fund = fund;
        }

        public void setOpenVersion(ArrFundVersion fundVersion) {
            this.openVersion = fundVersion;
            rootNode = openVersion.getRootNode();
        }

        public Integer getRootNodeId() {
            return rootNode.getNodeId();
        }

        public ArrNode getRootNode() {
            return rootNode;
        }

        public Integer getFundVersionId() {
            return openVersion.getFundVersionId();
        }

        public ArrFund getFund() {
            return fund;
        }

        public ArrFundVersion getFundVersion() {
            return openVersion;
        }

    }

    protected FundInfo createFund(String fundName) {
        FundInfo fi = new FundInfo();

        List<RulRuleSet> rulesets = rulesetRepos.findAll();
        fi.setRulesets(rulesets);

        ArrFund fund = arrangementService.createFundWithScenario(fundName, fi.getFirstRuleset(), null, firstInstitution,
                                                                 "date-range");
        Validate.notNull(fund);
        fi.setFund(fund);
        
        ArrFundVersion fundVersion = arrangementService.getOpenVersionByFundId(fund.getFundId());
        Validate.notNull(fundVersion);
        fi.setOpenVersion(fundVersion);

        return fi;
    }

    /**
     * Create description item
     * 
     * @param itemVO
     * @param node
     * @param fundVersionId
     * @return
     */
    protected ArrDescItem createDescItem(ArrItemVO itemVO, ArrNode node, Integer fundVersionId) {

        ArrDescItem descItem = factoryDO.createDescItem(itemVO, itemVO.getItemTypeId());
        assertNotNull(descItem);

        ArrDescItem descItemResult = descriptionItemService.createDescriptionItem(descItem, node.getNodeId(),
                                                                                  node.getVersion(),
                                                                                  fundVersionId);
        assertNotNull(descItemResult);

        return descItemResult;
    }

    @Before
    @Override
    public void setUp() throws Exception {
        authorizeAsAdmin();
        super.setUp();
        institutions = imporInsts();
        firstInstitution = institutions.get(0);
        // Flush any pending operations
        em.flush();
    }

    @After
    @Override
    public void tearDown() {
        // Flush any pending operations
        em.flush();

        super.tearDown();
    }

    protected void authorizeAsAdmin() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("", "", null);
        auth.setDetails(new UserDetail(""));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    /**
     * Import institutions from XML
     * 
     * @return
     */
    protected List<ParInstitution> imporInsts() {
        ApScope scope = apService.getScope(1);
        Validate.notNull(scope);

        File instFile = getResourceFile(XML_INSTITUTION);
        try (FileInputStream fis = new FileInputStream(instFile)) {
            DEImportParams params = new DEImportParams(scope.getScopeId(), 1000, 10000, null, null);
            deImportService.importData(fis, params);
        } catch (IOException e) {
            Assert.fail(e.fillInStackTrace().toString());
        }
        return institutionRepository.findAll();
    }
}
