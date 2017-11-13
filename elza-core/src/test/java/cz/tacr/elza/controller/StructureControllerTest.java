package cz.tacr.elza.controller;

import cz.tacr.elza.controller.vo.ArrFundVO;
import cz.tacr.elza.controller.vo.ArrFundVersionVO;
import cz.tacr.elza.controller.vo.ArrStructureDataVO;
import cz.tacr.elza.controller.vo.FilteredResultVO;
import cz.tacr.elza.domain.ArrStructureData;
import org.junit.Test;

import java.util.Objects;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


/**
 * Test pro {@link StructureController}.
 */
public class StructureControllerTest extends AbstractControllerTest {

    private final String NAME_AS = "Test AS1";
    private final String CODE_AS = "TST1";
    private final String STRUCTURE_TYPE_CODE = "ZP2015_OBAL";

    @Test
    public void structureDataTest() {
        ArrFundVO fund = createFund(NAME_AS, CODE_AS);
        ArrFundVersionVO fundVersion = getOpenVersion(fund);

        ArrStructureDataVO structureData = createStructureData(STRUCTURE_TYPE_CODE, fundVersion.getId());
        assertNotNull(structureData);
        assertNotNull(structureData.id);
        assertNotNull(structureData.assignable);
        assertTrue(structureData.state == ArrStructureData.State.TEMP);

        ArrStructureDataVO structureDataConfirmed = confirmStructureData(fundVersion.getId(), structureData.id);
        assertTrue(Objects.equals(structureDataConfirmed.id, structureDataConfirmed.id));
        assertTrue(structureDataConfirmed.state == ArrStructureData.State.OK);
        assertNotNull(structureDataConfirmed.value);
        assertNull(structureDataConfirmed.errorDescription);

        FilteredResultVO<ArrStructureDataVO> structureDataResult1 = findStructureData(STRUCTURE_TYPE_CODE, fundVersion.getId(), null, null, null, null);
        assertTrue(structureDataResult1.getCount() == 1);
        assertTrue(structureDataResult1.getRows().size() == 1);

        FilteredResultVO<ArrStructureDataVO> structureDataResult2 = findStructureData(STRUCTURE_TYPE_CODE, fundVersion.getId(), null, false, null, null);
        assertTrue(structureDataResult2.getCount() == 0);
        assertTrue(structureDataResult2.getRows().size() == 0);

        ArrStructureDataVO structureDataDeleted = deleteStructureData(fundVersion.getId(), structureData.id);
        assertNotNull(structureDataDeleted);

        FilteredResultVO<ArrStructureDataVO> structureDataResult3 = findStructureData(STRUCTURE_TYPE_CODE, fundVersion.getId(), null, null, null, null);
        assertTrue(structureDataResult3.getCount() == 0);
        assertTrue(structureDataResult3.getRows().size() == 0);

    }

}
