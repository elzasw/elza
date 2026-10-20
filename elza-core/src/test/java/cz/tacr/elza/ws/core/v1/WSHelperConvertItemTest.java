package cz.tacr.elza.ws.core.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.ws.types.v1.ItemEnum;
import cz.tacr.elza.ws.types.v1.ItemString;

/**
 * Item type and specification resolution of items received over the web services.
 *
 * A digital archive re-sends DAOs whose stored items may use codes that the rules
 * no longer know; the fault it gets back has to name the code and carry a stable
 * error code.
 */
class WSHelperConvertItemTest {

    private WSHelper wsHelper;

    private StaticDataProvider sdp;

    @BeforeEach
    void setUp() {
        sdp = mock(StaticDataProvider.class);
        StaticDataService staticDataService = mock(StaticDataService.class);
        when(staticDataService.getData()).thenReturn(sdp);

        wsHelper = new WSHelper();
        wsHelper.staticDataService = staticDataService;
    }

    @Test
    void unknownItemTypeNamesTheCodeInTheFault() {
        ItemString item = stringItem("ZP2015_TITLE", null);

        BusinessException e = assertThrows(BusinessException.class,
                () -> wsHelper.convertItem(new ArrDescItem(), item));

        assertSame(ArrangementCode.ITEM_TYPE_NOT_FOUND, e.getErrorCode());
        assertEquals("ZP2015_TITLE", e.getProperties().get("id"));

        String detail = WSHelper.prepareException("DAO import failed", e).getFaultInfo().getDetail();
        assertTrue(detail.startsWith("ITEM_TYPE_NOT_FOUND: "), detail);
        assertTrue(detail.contains("ZP2015_TITLE"), detail);
    }

    @Test
    void missingItemTypeCode() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> wsHelper.convertItem(new ArrDescItem(), stringItem("", null)));

        assertSame(ArrangementCode.ITEM_TYPE_NOT_FOUND, e.getErrorCode());
    }

    @Test
    void missingSpecification() {
        registerType("ZP2015_LEVEL_TYPE", true);

        BusinessException e = assertThrows(BusinessException.class,
                () -> wsHelper.convertItem(new ArrDescItem(), enumItem("ZP2015_LEVEL_TYPE", null)));

        assertSame(ArrangementCode.ITEM_SPEC_NOT_FOUND, e.getErrorCode());
        assertTrue(e.getMessage().contains("ZP2015_LEVEL_TYPE"), e.getMessage());
    }

    @Test
    void unknownSpecificationNamesBothCodes() {
        registerType("ZP2015_LEVEL_TYPE", true);

        BusinessException e = assertThrows(BusinessException.class,
                () -> wsHelper.convertItem(new ArrDescItem(), enumItem("ZP2015_LEVEL_TYPE", "ZP2015_LEVEL_GONE")));

        assertSame(ArrangementCode.ITEM_SPEC_NOT_FOUND, e.getErrorCode());
        assertTrue(e.getMessage().contains("ZP2015_LEVEL_TYPE"), e.getMessage());
        assertTrue(e.getMessage().contains("ZP2015_LEVEL_GONE"), e.getMessage());
    }

    @Test
    void specificationOnTypeWithoutSpecifications() {
        registerType("ZP2015_NAME", false);

        BusinessException e = assertThrows(BusinessException.class,
                () -> wsHelper.convertItem(new ArrDescItem(), stringItem("ZP2015_NAME", "SOME_SPEC")));

        assertSame(ArrangementCode.ITEM_SPEC_FOUND, e.getErrorCode());
    }

    @Test
    void knownTypeAndSpecificationAreSet() {
        ItemType itemType = registerType("ZP2015_LEVEL_TYPE", true);
        RulItemSpec spec = new RulItemSpec();
        when(itemType.getItemSpecByCode("ZP2015_LEVEL_ITEM")).thenReturn(spec);

        ArrDescItem target = new ArrDescItem();
        // prepareItem directly: building the data needs the initialized DataType cache
        ItemType result = wsHelper.prepareItem(target, "ZP2015_LEVEL_TYPE", "ZP2015_LEVEL_ITEM", true);

        assertSame(itemType, result);
        assertSame(itemType.getEntity(), target.getItemType());
        assertSame(spec, target.getItemSpec());
        assertTrue(target.getReadOnly());
    }

    private ItemType registerType(String code, boolean useSpecification) {
        ItemType itemType = mock(ItemType.class);
        when(itemType.getEntity()).thenReturn(new RulItemType());
        when(itemType.hasSpecifications()).thenReturn(useSpecification);
        when(sdp.getItemTypeByCode(code)).thenReturn(itemType);
        return itemType;
    }

    private static ItemString stringItem(String type, String spec) {
        ItemString item = new ItemString();
        item.setType(type);
        item.setSpec(spec);
        item.setValue("value");
        return item;
    }

    private static ItemEnum enumItem(String type, String spec) {
        ItemEnum item = new ItemEnum();
        item.setType(type);
        item.setSpec(spec);
        return item;
    }
}
