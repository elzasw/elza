package cz.tacr.elza.packageimport;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import cz.tacr.elza.domain.*;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import cz.tacr.elza.AbstractServiceTest;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemStringVO;
import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.other.SimpleDevRules;
import cz.tacr.elza.packageimport.xml.ItemSpec;
import cz.tacr.elza.packageimport.xml.ItemSpecs;
import cz.tacr.elza.packageimport.xml.ItemType;
import cz.tacr.elza.packageimport.xml.ItemTypes;
import cz.tacr.elza.repository.DataDateRepository;
import cz.tacr.elza.repository.DataDateRepository.OnlyValues;
import cz.tacr.elza.repository.ItemAptypeRepository;
import cz.tacr.elza.repository.ItemTypeActionRepository;

import static org.junit.Assert.assertNotNull;

public class TypeUpdateTest extends AbstractServiceTest {

    @Autowired
    ApplicationContext appCtx;

    @Autowired
    private ItemAptypeRepository itemAptypeRepository;

    @Autowired
    private DataDateRepository dataDateRepository;

    @Autowired
    private ResourcePathResolver resPathResolver;

    @Autowired
    private ItemTypeActionRepository itemTypeActionRepository;

    // drop all types and specs
    //@Test TODO: gotzy vratit zpatky
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
    @Ignore //TODO: problém s FK ApItem na specifikaci
    @Test
    @Transactional(TxType.REQUIRES_NEW)
    public void updateTypeTest2() {
        authorizeAsAdmin();

        FundInfo fi = createFund("F1");
        RulPackage srcPackage = fi.getFirstRuleset().getPackage();

        RulItemType itemType = itemTypeRepository.findOneByCode("SRD_STRING2DATE");
        assertNotNull(itemType);

        // Insert items to convert
        ArrItemStringVO itemSVO = new ArrItemStringVO();
        itemSVO.setValue("13.10.2018");
        itemSVO.setItemTypeId(itemType.getItemTypeId());

        ArrNode node = fi.getRootNode();
        ArrDescItem descItemResult = createDescItem(itemSVO, node, fi.getFundVersionId());

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
        Collection<OnlyValues> ddc = dataDateRepository.findValuesByDataIdIn(Collections.singletonList(descItemResult
                .getData()
                .getDataId()));
        Assert.assertNotNull(ddc);
        Assert.assertEquals("Neplatný počet položek", 1, ddc.size());
        OnlyValues dd = ddc.iterator().next();
        // zde může nastat někdy problém s kompatibilitou JAVA/Locale z metody cz.tacr.elza.packageimport.ItemTypeUpdater.changeString2Date
        Assert.assertEquals("Neplatný očekávaný datum", LocalDate.of(2018, 10, 13), dd.getValue());
    }

    private ItemType createItemTypeFor(String itemTypeCode, List<ItemSpec> itemSpecList) {
        RulItemType dbItemType = itemTypeRepository.findOneByCode(itemTypeCode);

        ItemType itemType = ItemType.fromEntity(dbItemType, itemAptypeRepository);
        if (Boolean.TRUE.equals(dbItemType.getUseSpecification())) {
            // Copy specifications
            List<RulItemTypeSpecAssign> dbSpecs = this.itemTypeSpecAssignRepository.findByItemType(dbItemType);
            for (RulItemTypeSpecAssign dbSpec : dbSpecs) {
                ItemSpec itemSpec = ItemSpec.fromEntity(dbSpec.getItemSpec(), itemAptypeRepository);
                itemSpecList.add(itemSpec);
            }
        }
        return itemType;
    }
}
