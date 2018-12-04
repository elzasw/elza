package cz.tacr.elza.print;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.Iterator;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import cz.tacr.elza.AbstractServiceTest;
import cz.tacr.elza.controller.vo.ArrStructureDataVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemStructureVO;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.fund.FundTreeProvider;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.ArrStructuredObject;
import cz.tacr.elza.domain.ArrStructuredObject.State;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulOutputType;
import cz.tacr.elza.domain.RulStructuredType;
import cz.tacr.elza.repository.ApDescriptionRepository;
import cz.tacr.elza.repository.ApExternalIdRepository;
import cz.tacr.elza.repository.ApNameRepository;
import cz.tacr.elza.repository.OutputTypeRepository;
import cz.tacr.elza.repository.StructuredObjectRepository;
import cz.tacr.elza.service.FundLevelService;
import cz.tacr.elza.service.FundLevelService.AddLevelDirection;
import cz.tacr.elza.service.cache.NodeCacheService;
import cz.tacr.elza.service.output.OutputParams;
import cz.tacr.elza.service.output.generator.OutputGeneratorFactory;

public class OutputModelTest extends AbstractServiceTest {

    @Autowired
    FundLevelService fundLevelService;

    @Autowired
    OutputGeneratorFactory outputGenFactory;

    @Autowired
    FundTreeProvider fundTreeProvider;

    @Autowired
    NodeCacheService nodeCacheService;

    @Autowired
    ApDescriptionRepository apDescRepository;

    @Autowired
    ApNameRepository apNameRepository;

    @Autowired
    ApExternalIdRepository apEidRepository;

    @Autowired
    StructuredObjectRepository structObjRepos;

    @Autowired
    OutputTypeRepository outputTypeRepository;

    // test output with structObjs
    @Test
    @Transactional(TxType.REQUIRES_NEW)
    public void outputStructObjs() {
        authorizeAsAdmin();

        StaticDataProvider sdp = staticDataService.createProvider();
        FundInfo fi = this.createFund("F1");

        // Create struct objs
        RulStructuredType structureType = sdp.getStructuredTypeByCode("SRD_PACKET");
        assertNotNull(structureType);
        ArrStructuredObject structObj1 = structObjService.createStructObj(fi.getFund(), structureType, State.OK);
        assertNotNull(structObj1);
        ArrStructuredObject structObj2 = structObjService.createStructObj(fi.getFund(), structureType, State.OK);
        assertNotNull(structObj2);

        // Create levels
        ArrLevel level1 = fundLevelService.addNewLevel(fi.getFundVersion(), fi.getRootNode(), fi.getRootNode(),
                                                       AddLevelDirection.CHILD, "Série", null);
        assertNotNull(level1);

        ArrLevel level2 = fundLevelService.addNewLevel(fi.getFundVersion(), level1.getNode(), level1.getNode(),
                                                       AddLevelDirection.CHILD, "Série", null);
        assertNotNull(level2);

        // Output type SRD_INVENTORY
        RulOutputType outputType = outputTypeRepository.findByCode("SRD_INVENTORY");

        // Attach objs
        RulItemType itemType = itemTypeRepository.findOneByCode("SRD_STORAGE_ID");
        assertNotNull(itemType);

        // Insert item1 to level1
        ArrItemStructureVO itemSVO = new ArrItemStructureVO();
        ArrStructureDataVO svo1 = ArrStructureDataVO.newInstance(structObj1);
        itemSVO.setValue(structObj1.getStructuredObjectId());
        itemSVO.setStructureData(svo1);
        itemSVO.setItemTypeId(itemType.getItemTypeId());
        ArrDescItem descItemResult1 = createDescItem(itemSVO, level1.getNode(), fi.getFundVersionId());
        assertNotNull(descItemResult1);

        // Insert item2 to level1
        ArrItemStructureVO itemSVO2 = new ArrItemStructureVO();
        itemSVO2.setStructureData(svo1);
        itemSVO2.setValue(structObj1.getStructuredObjectId());
        itemSVO2.setItemTypeId(itemType.getItemTypeId());
        ArrDescItem descItemResult2 = createDescItem(itemSVO2, level2.getNode(), fi.getFundVersionId());
        assertNotNull(descItemResult2);

        OutputModel outputModel = new OutputModel(staticDataService, elzaLocale,
                fundTreeProvider, nodeCacheService, institutionRepository,
                apDescRepository, apNameRepository, apEidRepository,
                null, structObjRepos);

        ArrOutputDefinition od = new ArrOutputDefinition();
        od.setFund(fi.getFund());
        od.setOutputType(outputType);

        OutputParams params = new OutputParams(od, null, fi.getFundVersion(),
                Collections.singletonList(level1.getNodeId()),
                Collections.emptyList(),
                Paths.get("test"));
        outputModel.init(params);

        Iterator<Structured> it = outputModel.createStructObjIterator(structureType.getCode());
        assertNotNull(it);
        assertTrue(it.hasNext());
        Structured s = it.next();
        assertNotNull(s);

        assertTrue(!it.hasNext());

        // Flush any pending operations
        em.flush();

    }
}
