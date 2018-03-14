package cz.tacr.elza.controller;

import cz.tacr.elza.controller.config.ClientFactoryDO;
import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.ArrStructureDataVO;
import cz.tacr.elza.controller.vo.FilteredResultVO;
import cz.tacr.elza.controller.vo.RulStructureTypeVO;
import cz.tacr.elza.controller.vo.nodes.ItemTypeLiteVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemVO;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrStructuredItem;
import cz.tacr.elza.domain.ArrStructuredObject;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.RulStructuredType;
import cz.tacr.elza.domain.RulStructuredTypeExtension;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.FilteredResult;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.RuleService;
import cz.tacr.elza.service.StructureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Map;


/**
 * Controller pro správu strukturovaných datových typů a jejich hodnot.
 *
 * @since 10.11.2017
 */
@RestController
@RequestMapping(value = "/api/structure",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
public class StructureController {

    private final StructureService structureService;
    private final ArrangementService arrangementService;
    private final RuleService ruleService;
    private final ClientFactoryDO factoryDO;
    private final ClientFactoryVO factoryVO;

    @Autowired
    public StructureController(final StructureService structureService,
                               final ArrangementService arrangementService,
                               final RuleService ruleService,
                               final ClientFactoryDO factoryDO,
                               final ClientFactoryVO factoryVO) {
        this.structureService = structureService;
        this.arrangementService = arrangementService;
        this.ruleService = ruleService;
        this.factoryDO = factoryDO;
        this.factoryVO = factoryVO;
    }

    /**
     * Vytvoření hodnoty strukturovaného datového typu.
     *
     * @param structureTypeCode kód strukturovaného datového typu
     * @param fundVersionId     identifikátor verze AS
     * @return vytvořená dočasná entita
     */
    @Transactional
    @RequestMapping(value = "/data/{fundVersionId}", method = RequestMethod.POST)
    public ArrStructureDataVO createStructureData(@RequestBody final String structureTypeCode,
                                                  @PathVariable(value = "fundVersionId") final Integer fundVersionId) {

        ArrFundVersion fundVersion = arrangementService.getFundVersionById(fundVersionId);
        RulStructuredType structureType = structureService.getStructureTypeByCode(structureTypeCode);
        validateRuleSet(fundVersion, structureType);
        ArrStructuredObject createStructureData = structureService.createStructureData(fundVersion.getFund(), structureType, ArrStructuredObject.State.TEMP);
        return factoryVO.createStructureData(createStructureData);
    }

    /**
     * Založení duplikátů strukturovaného datového typu a autoinkrementační.
     * Předloha musí být ve stavu {@link ArrStructuredObject.State#TEMP}.
     *
     * @param structureDataId    identifikátor předlohy hodnoty strukturovaného datového typu
     * @param fundVersionId      identifikátor verze AS
     * @param structureDataBatch data pro hromadné vytvoření hodnot
     */
    @Transactional
    @RequestMapping(value = "/data/{fundVersionId}/{structureDataId}/batch", method = RequestMethod.POST)
    public void duplicateStructureDataBatch(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                            @PathVariable(value = "structureDataId") final Integer structureDataId,
                                            @RequestBody StructureDataBatch structureDataBatch) {
        Assert.notNull(structureDataBatch.getCount(), "Počet položek musí být vyplněn");
        Assert.notEmpty(structureDataBatch.getItemTypeIds(), "Autoincrementující typ musí být alespoň jeden");
        ArrFundVersion fundVersion = arrangementService.getFundVersionById(fundVersionId);
        ArrStructuredObject structureData = structureService.getStructureDataById(structureDataId);
        structureService.duplicateStructureDataBatch(fundVersion, structureData, structureDataBatch.getCount(), structureDataBatch.getItemTypeIds());
    }

    /**
     * Hromadná úprava položek/hodnot strukt. typu.
     *
     * @param fundVersionId            identifikátor verze AS
     * @param structureTypeCode        kód strukturovaného datového typu
     * @param structureDataBatchUpdate data pro hromadnou úpravu hodnot
     */
    @Transactional
    @RequestMapping(value = "/data/{fundVersionId}/{structureTypeCode}/batchUpdate", method = RequestMethod.POST)
    public void updateStructureDataBatch(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                         @PathVariable(value = "structureTypeCode") final String structureTypeCode,
                                         @RequestBody final StructureDataBatchUpdate structureDataBatchUpdate) {
        ArrFundVersion fundVersion = arrangementService.getFundVersionById(fundVersionId);
        RulStructuredType structureType = structureService.getStructureTypeByCode(structureTypeCode);
        validateRuleSet(fundVersion, structureType);

        Assert.notNull(structureDataBatchUpdate.autoincrementItemTypeIds, "Identifikátory typů atributu pro autoincrement nesmí být null");
        Assert.notNull(structureDataBatchUpdate.deleteItemTypeIds, "Identifikátory typů atributu pro odstranění nesmí být null");
        Assert.notNull(structureDataBatchUpdate.items, "Položky nesmí být null");
        Assert.notEmpty(structureDataBatchUpdate.structureDataIds, "Musí být vyplněn alespoň jeden identifikátor hodnoty strukt. typu");

        List<ArrStructuredItem> structureItems = factoryDO.createStructureItem(structureDataBatchUpdate.getItems());
        structureService.updateStructureDataBatch(fundVersion,
                structureType,
                structureDataBatchUpdate.getStructureDataIds(),
                structureItems,
                structureDataBatchUpdate.getAutoincrementItemTypeIds(),
                structureDataBatchUpdate.getDeleteItemTypeIds());
    }

    /**
     * Potvrzení hodnoty strukturovaného datového typu. Provede nastavení hodnoty.
     *
     * @param fundVersionId   identifikátor verze AS
     * @param structureDataId identifikátor hodnoty strukturovaného datového typu
     * @return potvrzená entita
     */
    @Transactional
    @RequestMapping(value = "/data/{fundVersionId}/{structureDataId}/confirm", method = RequestMethod.POST)
    public ArrStructureDataVO confirmStructureData(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                                   @PathVariable(value = "structureDataId") final Integer structureDataId) {
        ArrFundVersion fundVersion = arrangementService.getFundVersionById(fundVersionId);
        ArrStructuredObject structureData = structureService.getStructureDataById(structureDataId);
        validateRuleSet(fundVersion, structureData.getStructuredType());
        ArrStructuredObject createStructureData = structureService.confirmStructureData(fundVersion.getFund(), structureData);
        return factoryVO.createStructureData(createStructureData);
    }

    /**
     * Nastavení přiřaditelnosti.
     *
     * @param fundVersionId    identifikátor verze AS
     * @param assignable       přiřaditelný
     * @param structureDataIds identifikátory hodnot strukturovaného datového typu
     */
    @Transactional
    @RequestMapping(value = "/data/{fundVersionId}/assignable/{assignable}", method = RequestMethod.POST)
    public void setAssignableStructureDataList(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                               @PathVariable(value = "assignable") final Boolean assignable,
                                               @RequestBody List<Integer> structureDataIds) {
        ArrFundVersion fundVersion = arrangementService.getFundVersionById(fundVersionId);
        List<ArrStructuredObject> structureDataList = structureService.getStructureDataByIds(structureDataIds);
        for (ArrStructuredObject structureData : structureDataList) {
            validateRuleSet(fundVersion, structureData.getStructuredType());
        }
        structureService.setAssignableStructureDataList(fundVersion.getFund(), structureDataList, assignable);
    }

    /**
     * Smazání hodnoty strukturovaného datového typu.
     *
     * @param fundVersionId   identifikátor verze AS
     * @param structureDataId identifikátor hodnoty strukturovaného datového typu
     * @return smazaná entita
     */
    @Transactional
    @RequestMapping(value = "/data/{fundVersionId}/{structureDataId}", method = RequestMethod.DELETE)
    public ArrStructureDataVO deleteStructureData(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                                  @PathVariable(value = "structureDataId") final Integer structureDataId) {
        ArrFundVersion fundVersion = arrangementService.getFundVersionById(fundVersionId);
        ArrStructuredObject structureData = structureService.getStructureDataById(structureDataId);
        validateRuleSet(fundVersion, structureData.getStructuredType());
        ArrStructuredObject deleteStructureData = structureService.deleteStructureData(structureData);
        return factoryVO.createStructureData(deleteStructureData);
    }

    /**
     * Získání hodnoty strukturovaného datového typu.
     *
     * @param fundVersionId   identifikátor verze AS
     * @param structureDataId identifikátor hodnoty strukturovaného datového typu
     * @return nalezená entita
     */
    @Transactional
    @RequestMapping(value = "/data/{fundVersionId}/{structureDataId}", method = RequestMethod.GET)
    public ArrStructureDataVO getStructureData(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                               @PathVariable(value = "structureDataId") final Integer structureDataId) {
        ArrFundVersion fundVersion = arrangementService.getFundVersionById(fundVersionId);
        return factoryVO.createStructureData(structureService.getStructureDataById(structureDataId, fundVersion));
    }

    /**
     * Vyhledání hodnot strukturovaného datového typu.
     *
     * @param structureTypeCode kód typu strukturovaného datového
     * @param fundVersionId     identifikátor verze AS
     * @param search            text pro filtrování (nepovinné)
     * @param assignable        přiřaditelnost
     * @param from              od položky
     * @param count             maximální počet položek
     * @return nalezené položky
     */
    @Transactional
    @RequestMapping(value = "/data/{fundVersionId}/{structureTypeCode}/search", method = RequestMethod.GET)
    public FilteredResultVO<ArrStructureDataVO> findStructureData(@PathVariable("fundVersionId") final Integer fundVersionId,
                                                                  @PathVariable("structureTypeCode") final String structureTypeCode,
                                                                  @RequestParam(value = "search", required = false) final String search,
                                                                  @RequestParam(value = "assignable", required = false) final Boolean assignable,
                                                                  @RequestParam(value = "from", required = false, defaultValue = "0") final Integer from,
                                                                  @RequestParam(value = "count", required = false, defaultValue = "200") final Integer count) {
        if (from < 0) {
            throw new SystemException("Hodnota nesmí být záporná", BaseCode.PROPERTY_IS_INVALID).set("property", "from");
        }
        if (count <= 0) {
            throw new SystemException("Hodnota musí být kladná", BaseCode.PROPERTY_IS_INVALID).set("property", "count");
        }
        ArrFundVersion fundVersion = arrangementService.getFundVersionById(fundVersionId);
        RulStructuredType structureType = structureService.getStructureTypeByCode(structureTypeCode);
        FilteredResult<ArrStructuredObject> filteredResult = structureService.findStructureData(structureType, fundVersion.getFund(), search, assignable, from, count);
        return new FilteredResultVO<>(factoryVO.createStructureDataList(filteredResult.getList()), filteredResult.getTotalCount());
    }

    /**
     * Vytvoření položky k hodnotě strukt. datového typu.
     *
     * @param itemVO          položka
     * @param fundVersionId   identifikátor verze AS
     * @param itemTypeId      identifikátor typu atributu
     * @param structureDataId identifikátor hodnoty strukturovaného datového typu
     * @return vytvořená entita
     */
    @Transactional
    @RequestMapping(value = "/item/{fundVersionId}/{structureDataId}/{itemTypeId}/create", method = RequestMethod.POST)
    public StructureItemResult createStructureItem(@RequestBody final ArrItemVO itemVO,
                                                   @PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                                   @PathVariable(value = "itemTypeId") final Integer itemTypeId,
                                                   @PathVariable(value = "structureDataId") final Integer structureDataId) {
        ArrStructuredItem structureItem = factoryDO.createStructureItem(itemVO, itemTypeId);
        ArrStructuredItem createStructureItem = structureService.createStructureItem(structureItem, structureDataId, fundVersionId);
        StructureItemResult result = new StructureItemResult();
        result.setItem(factoryVO.createItem(createStructureItem));
        result.setParent(factoryVO.createStructureData(createStructureItem.getStructuredObject()));
        return result;
    }

    /**
     * Upravení položky k hodnotě strukt. datového typu.
     *
     * @param itemVO           položka
     * @param fundVersionId    identifikátor verze AS
     * @param createNewVersion provést verzovanou změnu
     * @return upravená entita
     */
    @Transactional
    @RequestMapping(value = "/item/{fundVersionId}/update/{createNewVersion}", method = RequestMethod.PUT)
    public StructureItemResult updateStructureItem(@RequestBody final ArrItemVO itemVO,
                                                   @PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                                   @PathVariable(value = "createNewVersion") final Boolean createNewVersion) {
        ArrStructuredItem structureItem = factoryDO.createStructureItem(itemVO);
        ArrStructuredItem updateStructureItem = structureService.updateStructureItem(structureItem, fundVersionId, createNewVersion);
        StructureItemResult result = new StructureItemResult();
        result.setItem(factoryVO.createItem(updateStructureItem));
        result.setParent(factoryVO.createStructureData(updateStructureItem.getStructuredObject()));
        return result;
    }

    /**
     * Odstranení položky k hodnotě strukt. datového typu.
     *
     * @param itemVO        položka
     * @param fundVersionId identifikátor verze AS
     * @return smazaná entita
     */
    @Transactional
    @RequestMapping(value = "/item/{fundVersionId}/delete", method = RequestMethod.POST)
    public StructureItemResult deleteStructureItem(@RequestBody final ArrItemVO itemVO,
                                                   @PathVariable(value = "fundVersionId") final Integer fundVersionId) {
        ArrStructuredItem structureItem = factoryDO.createStructureItem(itemVO);
        ArrStructuredItem deleteStructureItem = structureService.deleteStructureItem(structureItem, fundVersionId);
        StructureItemResult result = new StructureItemResult();
        result.setItem(factoryVO.createItem(deleteStructureItem));
        result.setParent(factoryVO.createStructureData(deleteStructureItem.getStructuredObject()));
        return result;
    }

    /**
     * Odstranení položek k hodnotě strukt. datového typu podle typu atributu.
     * @param fundVersionId   identifikátor verze AS
     * @param structureDataId identifikátor hodnoty strukturovaného datového typu
     * @param itemTypeId      identifikátor typu atributu
     * @return smazaná entita
     */
    @Transactional
    @RequestMapping(value = "/item/{fundVersionId}/{structureDataId}/{itemTypeId}", method = RequestMethod.DELETE)
    public StructureItemResult deleteStructureItemsByType(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                                          @PathVariable(value = "structureDataId") final Integer structureDataId,
                                                          @PathVariable(value = "itemTypeId") final Integer itemTypeId) {
        ArrStructuredObject structureData = structureService.deleteStructureItemsByType(fundVersionId, structureDataId, itemTypeId);
        StructureItemResult result = new StructureItemResult();
        result.setItem(null);
        result.setParent(factoryVO.createStructureData(structureData));
        return result;
    }

    /**
     * Vyhledá možné typy strukt. datových typů, které lze v AS používat.
     *
     * @param fundVersionId identifikátor verze AS
     * @return nalezené entity
     */
    @Transactional
    @RequestMapping(value = "/type/{fundVersionId}", method = RequestMethod.GET)
    public List<RulStructureTypeVO> findStructureTypes(@PathVariable(value = "fundVersionId") final Integer fundVersionId) {
        ArrFundVersion fundVersion = arrangementService.getFundVersionById(fundVersionId);
        List<RulStructuredType> structureTypes = structureService.findStructureTypes(fundVersion);
        return factoryVO.createSimpleEntity(structureTypes, RulStructureTypeVO.class);
    }

    /**
     * Získání dat pro formulář strukt. datového typu.
     *
     * @param fundVersionId   identifikátor verze AS
     * @param structureDataId identifikátor hodnoty strukturovaného datového typu
     * @return data formuláře
     */
    @Transactional
    @RequestMapping(value = "/item/form/{fundVersionId}/{structureDataId}", method = RequestMethod.GET)
    public StructureDataFormDataVO getFormStructureItems(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                                         @PathVariable(value = "structureDataId") final Integer structureDataId) {
        ArrFundVersion fundVersion = arrangementService.getFundVersionById(fundVersionId);
        ArrStructuredObject structureData = structureService.getStructureDataById(structureDataId);

        validateRuleSet(fundVersion, structureData.getStructuredType());

        List<ArrStructuredItem> structureItems = structureService.findStructureItems(structureData);
        List<RulItemTypeExt> structureItemTypes = ruleService.getStructureItemTypes(structureData.getStructuredType(), fundVersion, structureItems);

        Integer fundId = fundVersion.getFund().getFundId();
        String ruleCode = fundVersion.getRuleSet().getCode();

        ArrStructureDataVO structureDataVO = factoryVO.createStructureData(structureData);
        List<ArrItemVO> descItems = factoryVO.createItems(structureItems);
        List<ItemTypeLiteVO> itemTypeLites = factoryVO.createItemTypes(ruleCode, fundId, structureItemTypes);
        return new StructureDataFormDataVO(structureDataVO, descItems, itemTypeLites);
    }

    /**
     * Vyhledá dostupná a aktivovaná rozšíření k AS.
     *
     * @param fundVersionId identifikátor verze AS
     * @return nalezené entity
     */
    @Transactional
    @RequestMapping(value = "/extension/{fundVersionId}/{structureTypeCode}", method = RequestMethod.GET)
    public List<StructureExtensionFundVO> findFundStructureExtension(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                                                     @PathVariable(value = "structureTypeCode") final String structureTypeCode) {
        ArrFundVersion fundVersion = arrangementService.getFundVersionById(fundVersionId);
        RulStructuredType structureType = structureService.getStructureTypeByCode(structureTypeCode);
        validateRuleSet(fundVersion, structureType);
        List<RulStructuredTypeExtension> allStructureExtensions = structureService.findAllStructureExtensions(structureType);
        List<RulStructuredTypeExtension> structureExtensions = structureService.findStructureExtensions(fundVersion.getFund(), structureType);
        return factoryVO.createStructureExtensionFund(allStructureExtensions, structureExtensions);
    }

    /**
     * Nastaví konkrétní rozšíření na AS.
     *
     * @param fundVersionId           identifikátor verze AS
     * @param structureExtensionCodes seznam kódů rozšíření, které mají být aktivovány na AS
     */
    @Transactional
    @RequestMapping(value = "/extension/{fundVersionId}/{structureTypeCode}", method = RequestMethod.PUT)
    public void setFundStructureExtensions(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                           @PathVariable(value = "structureTypeCode") final String structureTypeCode,
                                           @RequestBody final List<String> structureExtensionCodes) {
        ArrFundVersion fundVersion = arrangementService.getFundVersionById(fundVersionId);
        RulStructuredType structureType = structureService.getStructureTypeByCode(structureTypeCode);
        validateRuleSet(fundVersion, structureType);
        List<RulStructuredTypeExtension> structureExtensions = structureService.findStructureExtensionByCodes(structureExtensionCodes);
        structureService.setFundStructureExtensions(fundVersion, structureType, structureExtensions);
    }

    /**
     * Validace pravidel.
     *
     * @param fundVersion   verze AS
     * @param structureType strukturovaný typ
     */
    private void validateRuleSet(final ArrFundVersion fundVersion, final RulStructuredType structureType) {
        if (!structureType.getRuleSet().equals(fundVersion.getRuleSet())) {
            throw new BusinessException("Pravidla z AS se neshodují s pravidly ze strukturovaného typu", ArrangementCode.INVALID_RULE);
        }
    }

    public static class StructureDataFormDataVO extends ArrangementController.FormDataNewVO<ArrStructureDataVO> {
        private ArrStructureDataVO parent;

        public StructureDataFormDataVO() {
        }

        public StructureDataFormDataVO(final ArrStructureDataVO parent, final List<ArrItemVO> descItems, final List<ItemTypeLiteVO> itemTypeLites) {
            super(parent, descItems, itemTypeLites);
            this.parent = parent;
        }

        @Override
        public ArrStructureDataVO getParent() {
            return parent;
        }

        @Override
        public void setParent(final ArrStructureDataVO parent) {
            this.parent = parent;
        }
    }

    public static class StructureItemResult extends ArrangementController.ItemResult<ArrStructureDataVO> {
        private ArrStructureDataVO parent;

        @Override
        public ArrStructureDataVO getParent() {
            return parent;
        }

        @Override
        public void setParent(final ArrStructureDataVO parent) {
            this.parent = parent;
        }
    }

    public static class StructureDataBatchUpdate {

        /**
         * Identifikátory hodnot strukt. typu, pro které se bude provádět úprava.
         */
        private List<Integer> structureDataIds;

        /**
         * Identifikátory číselných typů atributu, které se budou incrementovat.
         */
        private List<Integer> autoincrementItemTypeIds;

        /**
         * Identifikátory typů atributu, které se mají smazat.
         */
        private List<Integer> deleteItemTypeIds;

        /**
         * Identifikátor typu atributu -> položky, které se mají nastavit na hodnotách strukt. typu.
         */
        private Map<Integer, List<ArrItemVO>> items;

        public List<Integer> getStructureDataIds() {
            return structureDataIds;
        }

        public void setStructureDataIds(final List<Integer> structureDataIds) {
            this.structureDataIds = structureDataIds;
        }

        public List<Integer> getAutoincrementItemTypeIds() {
            return autoincrementItemTypeIds;
        }

        public void setAutoincrementItemTypeIds(final List<Integer> autoincrementItemTypeIds) {
            this.autoincrementItemTypeIds = autoincrementItemTypeIds;
        }

        public List<Integer> getDeleteItemTypeIds() {
            return deleteItemTypeIds;
        }

        public void setDeleteItemTypeIds(final List<Integer> deleteItemTypeIds) {
            this.deleteItemTypeIds = deleteItemTypeIds;
        }

        public Map<Integer, List<ArrItemVO>> getItems() {
            return items;
        }

        public void setItems(final Map<Integer, List<ArrItemVO>> items) {
            this.items = items;
        }
    }

    public static class StructureDataBatch {

        /**
         * Počet položek, které se budou budou vytvářet (včetně zdrojové hodnoty strukt. typu).
         */
        private Integer count;

        /**
         * Identifikátory číselných typů atributu, které se budou incrementovat.
         */
        private List<Integer> itemTypeIds;

        public StructureDataBatch() {
        }

        public StructureDataBatch(final Integer count, final List<Integer> itemTypeIds) {
            this.count = count;
            this.itemTypeIds = itemTypeIds;
        }

        public Integer getCount() {
            return count;
        }

        public void setCount(final Integer count) {
            this.count = count;
        }

        public List<Integer> getItemTypeIds() {
            return itemTypeIds;
        }

        public void setItemTypeIds(final List<Integer> itemTypeIds) {
            this.itemTypeIds = itemTypeIds;
        }
    }
}
