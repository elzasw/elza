package cz.tacr.elza.packageimport;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.apache.commons.lang.Validate;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import cz.tacr.elza.AbstractTest;
import cz.tacr.elza.controller.config.ClientFactoryDO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemStringVO;
import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.dataexchange.input.DEImportParams;
import cz.tacr.elza.dataexchange.input.DEImportService;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.other.SimpleDevRules;
import cz.tacr.elza.packageimport.xml.ItemSpec;
import cz.tacr.elza.packageimport.xml.ItemSpecs;
import cz.tacr.elza.packageimport.xml.ItemType;
import cz.tacr.elza.packageimport.xml.ItemTypes;
import cz.tacr.elza.repository.DataDateRepository;
import cz.tacr.elza.repository.DataDateRepository.OnlyValues;
import cz.tacr.elza.repository.InstitutionRepository;
import cz.tacr.elza.repository.ItemSpecRegisterRepository;
import cz.tacr.elza.repository.ItemTypeActionRepository;
import cz.tacr.elza.repository.RuleSetRepository;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.DescriptionItemService;

public class TypeUpdateTest extends AbstractTest {

    @Autowired
    ApplicationContext appCtx;

    @Autowired
    ArrangementService arrangementService;

    @Autowired
    AccessPointService apService;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private ItemSpecRegisterRepository itemSpecRegisterRepository;

    @Autowired
    private DescriptionItemService descriptionItemService;

    @Autowired
    private DataDateRepository dataDateRepository;

    @Autowired
    private ClientFactoryDO factoryDO;

    @Autowired
    private DEImportService deImportService;

    @Autowired
    private RuleSetRepository rulesetRepos;

    @Autowired
    private ResourcePathResolver resPathResolver;

    @Autowired
    private ItemTypeActionRepository itemTypeActionRepository;

    private List<ParInstitution> institutions;

    private ParInstitution firstInstitution;

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

    // drop all types and specs
    @Test
    @Transactional(TxType.REQUIRES_NEW)
    public void updateTypeTest1() {
        authorizeAsAdmin();

        List<RulRuleSet> rulesets = rulesetRepos.findAll();
        RulRuleSet ruleset = rulesets.get(0);
        RulPackage srcPackage = ruleset.getPackage();

        PackageContext puc = new PackageContext(resPathResolver);
        puc.setPackage(srcPackage);

        // drop foreign keys
        itemTypeActionRepository.deleteAll();

        // remove all types
        ItemTypeUpdater itu = appCtx.getBean(ItemTypeUpdater.class);
        itu.update(null, null, puc);

        long cnt = this.itemTypeRepository.count();
        Assert.assertEquals(0, cnt);

        // Flush any pending operations
        em.flush();
    }

    // test will migrate values from SRD_STRING2DATE to DATE
    @Test
    @Transactional(TxType.REQUIRES_NEW)
    public void updateTypeTest2() {
        authorizeAsAdmin();
        
        List<RulRuleSet> rulesets = rulesetRepos.findAll();
        RulRuleSet ruleset = rulesets.get(0);
        RulPackage srcPackage = ruleset.getPackage();

        ArrFund fund = arrangementService.createFundWithScenario("F1", ruleset, null, firstInstitution, "date-range");
        Validate.notNull(fund);
        ArrFundVersion fundVersion = arrangementService.getOpenVersionByFundId(fund.getFundId());
        Validate.notNull(fundVersion);
        ArrNode rootNode = fundVersion.getRootNode();

        ArrItemStringVO itemSVO = new ArrItemStringVO();
        itemSVO.setValue("13.10.2018");

        RulItemType itemType = itemTypeRepository.findOneByCode("SRD_STRING2DATE");
        Validate.notNull(itemType);

        // Insert items to convert
        ArrDescItem descItem = factoryDO.createDescItem(itemSVO, itemType.getItemTypeId());
        Validate.notNull(descItem);
        
        ArrDescItem descItemResult = descriptionItemService.createDescriptionItem(descItem, fundVersion.getRootNodeId(),
                                                     rootNode.getVersion(), fundVersion.getFundVersionId());
        
        List<ItemType> itemTypeList = new ArrayList<>(2);
        List<ItemSpec> itemSpecList = new ArrayList<>();
        // keep level and  SRD_STRING2DATE
        ItemType itemType1 = createItemTypeFor(SimpleDevRules.SRD_LEVEL_TYPE, itemSpecList);
        itemTypeList.add(itemType1);
        ItemType itemType2 = createItemTypeFor(SimpleDevRules.SRD_STRING2DATE, itemSpecList);
        itemType2.setDataType(DataType.DATE.getCode());
        itemTypeList.add(itemType2);

        ItemTypes its = new ItemTypes();
        its.setItemTypes(itemTypeList);
        ItemSpecs iss = new ItemSpecs();
        iss.setItemSpecs(itemSpecList);

        PackageContext puc = new PackageContext(resPathResolver);
        puc.setPackage(srcPackage);

        // drop foreign keys
        itemTypeActionRepository.deleteAll();

        // update (and also change type)
        ItemTypeUpdater itu = appCtx.getBean(ItemTypeUpdater.class);
        itu.update(its, iss, puc);

        // Flush any pending operations
        em.flush();

        // one node has to be mark as invalid
        Assert.assertEquals(1, itu.getNumDroppedCachedNode());

        // read values from repository 
        Collection<OnlyValues> ddc = dataDateRepository.findValuesByDataId(Collections.singletonList(descItemResult
                .getData()
                .getDataId()));
        Assert.assertNotNull(ddc);
        Assert.assertTrue(ddc.size() == 1);
        OnlyValues dd = ddc.iterator().next();
        Assert.assertTrue(dd.getValue().equals(LocalDate.of(2018, 10, 13)));
    }

    private ItemType createItemTypeFor(String itemTypeCode, List<ItemSpec> itemSpecList) {
        RulItemType dbItemType = itemTypeRepository.findOneByCode(itemTypeCode);

        ItemType itemType = ItemType.fromEntity(dbItemType);
        if (Boolean.TRUE.equals(dbItemType.getUseSpecification())) {
            // Copy specifications
            List<RulItemSpec> dbSpecs = this.itemSpecRepository.findByItemType(dbItemType);
            for (RulItemSpec dbSpec : dbSpecs) {
                ItemSpec itemSpec = ItemSpec.fromEntity(dbSpec, itemSpecRegisterRepository);
                itemSpecList.add(itemSpec);
            }
        }
        return itemType;
    }

    private void authorizeAsAdmin() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("", "", null);
        auth.setDetails(new UserDetail(""));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private List<ParInstitution> imporInsts() {
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
