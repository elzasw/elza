package cz.tacr.elza.controller;

import cz.tacr.elza.controller.config.ClientFactoryDO;
import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.ArrStructureDataVO;
import cz.tacr.elza.controller.vo.FilteredResultVO;
import cz.tacr.elza.controller.vo.RulStructureTypeVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ItemGroupVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ItemTypeGroupVO;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrStructureData;
import cz.tacr.elza.domain.ArrStructureItem;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.RulStructureExtension;
import cz.tacr.elza.domain.RulStructureType;
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
        RulStructureType structureType = structureService.getStructureTypeByCode(structureTypeCode);
        validateRuleSet(fundVersion, structureType);
        ArrStructureData createStructureData = structureService.createStructureData(fundVersion.getFund(), structureType, ArrStructureData.State.TEMP);
        return factoryVO.createStructureData(createStructureData);
    }

    /**
     * Založení duplikátů strukturovaného datového typu a autoinkrementační.
     * Předloha musí být ve stavu {@link ArrStructureData.State#TEMP}.
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
        ArrStructureData structureData = structureService.getStructureDataById(structureDataId);
        structureService.duplicateStructureDataBatch(fundVersion, structureData, structureDataBatch.getCount(), structureDataBatch.getItemTypeIds());
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
        ArrStructureData structureData = structureService.getStructureDataById(structureDataId);
        validateRuleSet(fundVersion, structureData.getStructureType());
        ArrStructureData createStructureData = structureService.confirmStructureData(structureData);
        return factoryVO.createStructureData(createStructureData);
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
        ArrStructureData structureData = structureService.getStructureDataById(structureDataId);
        validateRuleSet(fundVersion, structureData.getStructureType());
        ArrStructureData deleteStructureData = structureService.deleteStructureData(structureData);
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
        RulStructureType structureType = structureService.getStructureTypeByCode(structureTypeCode);
        FilteredResult<ArrStructureData> filteredResult = structureService.findStructureData(structureType, fundVersion.getFund(), search, assignable, from, count);
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
        ArrStructureItem structureItem = factoryDO.createStructureItem(itemVO, itemTypeId);
        ArrStructureItem createStructureItem = structureService.createStructureItem(structureItem, structureDataId, fundVersionId);
        StructureItemResult result = new StructureItemResult();
        result.setItem(factoryVO.createDescItem(createStructureItem));
        result.setParent(factoryVO.createStructureData(createStructureItem.getStructureData()));
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
        ArrStructureItem structureItem = factoryDO.createStructureItem(itemVO);
        ArrStructureItem updateStructureItem = structureService.updateStructureItem(structureItem, fundVersionId, createNewVersion);
        StructureItemResult result = new StructureItemResult();
        result.setItem(factoryVO.createDescItem(updateStructureItem));
        result.setParent(factoryVO.createStructureData(updateStructureItem.getStructureData()));
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
        ArrStructureItem structureItem = factoryDO.createStructureItem(itemVO);
        ArrStructureItem deleteStructureItem = structureService.deleteStructureItem(structureItem, fundVersionId);
        StructureItemResult result = new StructureItemResult();
        result.setItem(factoryVO.createDescItem(deleteStructureItem));
        result.setParent(factoryVO.createStructureData(deleteStructureItem.getStructureData()));
        return result;
    }

    /**
     * Odstranení položek k hodnotě strukt. datového typu podle typu atributu.
     *
     * @param fundVersionId   identifikátor verze AS
     * @param structureDataId identifikátor hodnoty strukturovaného datového typu
     * @param itemTypeId      identifikátor typu atributu
     */
    @Transactional
    @RequestMapping(value = "/item/{fundVersionId}/{structureDataId}/{itemTypeId}", method = RequestMethod.DELETE)
    public void deleteStructureItemsByType(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                           @PathVariable(value = "structureDataId") final Integer structureDataId,
                                           @PathVariable(value = "itemTypeId") final Integer itemTypeId) {
        structureService.deleteStructureItemsByType(fundVersionId, structureDataId, itemTypeId);
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
        List<RulStructureType> structureTypes = structureService.findStructureTypes(fundVersion.getRuleSet());
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
        ArrStructureData structureData = structureService.getStructureDataById(structureDataId);

        validateRuleSet(fundVersion, structureData.getStructureType());

        List<RulItemTypeExt> structureItemTypes = ruleService.getStructureItemTypes(structureData.getStructureType(), fundVersion);
        List<ArrStructureItem> structureItems = structureService.findStructureItems(structureData);

        Integer fundId = fundVersion.getFund().getFundId();
        String ruleCode = fundVersion.getRuleSet().getCode();

        ArrStructureDataVO structureDataVO = factoryVO.createStructureData(structureData);
        List<ItemGroupVO> itemGroupsVO = factoryVO.createItemGroupsNew(ruleCode, fundId, structureItems);
        List<ItemTypeGroupVO> itemTypeGroupsVO = factoryVO.createItemTypeGroupsNew(ruleCode, fundId, structureItemTypes);
        return new StructureDataFormDataVO(structureDataVO, itemGroupsVO, itemTypeGroupsVO);
    }

    /**
     * Vyhledá dostupná a aktivovaná rozšíření k AS.
     *
     * @param fundVersionId identifikátor verze AS
     * @return nalezené entity
     */
    @Transactional
    @RequestMapping(value = "/extension/{fundVersionId}", method = RequestMethod.GET)
    public List<StructureExtensionFundVO> findFundStructureExtension(@PathVariable(value = "fundVersionId") final Integer fundVersionId) {
        ArrFundVersion fundVersion = arrangementService.getFundVersionById(fundVersionId);
        List<RulStructureExtension> allStructureExtensions = structureService.findAllStructureExtensions(fundVersion);
        List<RulStructureExtension> structureExtensions = structureService.findStructureExtensions(fundVersion);
        return factoryVO.createStructureExtensionFund(allStructureExtensions, structureExtensions);
    }

    /**
     * Aktivuje rozšíření u archivního souboru.
     *
     * @param fundVersionId          identifikátor verze AS
     * @param structureExtensionCode kód rozšíření strukturovaného typu
     */
    @Transactional
    @RequestMapping(value = "/extension/{structureExtensionCode}/{fundVersionId}/add", method = RequestMethod.POST)
    public void addFundStructureExtension(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                          @PathVariable(value = "structureExtensionCode") final String structureExtensionCode) {
        ArrFundVersion fundVersion = arrangementService.getFundVersionById(fundVersionId);
        RulStructureExtension structureExtension = structureService.getStructureExtensionByCode(structureExtensionCode);
        structureService.addFundStructureExtension(fundVersion, structureExtension);
    }

    /**
     * Deaktivuje rozšíření u archivního souboru.
     *
     * @param fundVersionId          identifikátor verze AS
     * @param structureExtensionCode kód rozšíření strukturovaného typu
     */
    @Transactional
    @RequestMapping(value = "/extension/{structureExtensionCode}/{fundVersionId}/delete", method = RequestMethod.POST)
    public void deleteFundStructureExtension(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                             @PathVariable(value = "structureExtensionCode") final String structureExtensionCode) {
        ArrFundVersion fundVersion = arrangementService.getFundVersionById(fundVersionId);
        RulStructureExtension structureExtension = structureService.getStructureExtensionByCode(structureExtensionCode);
        structureService.deleteFundStructureExtension(fundVersion, structureExtension);
    }

    /**
     * Nastaví konkrétní rozšíření na AS.
     *
     * @param fundVersionId           identifikátor verze AS
     * @param structureExtensionCodes seznam kódů rozšíření, které mají být aktivovány na AS
     */
    @Transactional
    @RequestMapping(value = "/extension/{fundVersionId}/set", method = RequestMethod.POST)
    public void setFundStructureExtensions(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                           @RequestBody final List<String> structureExtensionCodes) {
        ArrFundVersion fundVersion = arrangementService.getFundVersionById(fundVersionId);
        List<RulStructureExtension> structureExtensions = structureService.findStructureExtensionByCodes(structureExtensionCodes);
        structureService.setFundStructureExtensions(fundVersion, structureExtensions);
    }

    /**
     * Validace pravidel.
     *
     * @param fundVersion   verze AS
     * @param structureType strukturovaný typ
     */
    private void validateRuleSet(final ArrFundVersion fundVersion, final RulStructureType structureType) {
        if (!structureType.getRuleSet().equals(fundVersion.getRuleSet())) {
            throw new BusinessException("Pravidla z AS se neshodují s pravidly ze strukturovaného typu", ArrangementCode.INVALID_RULE);
        }
    }

    public static class StructureDataFormDataVO extends ArrangementController.FormDataNewVO<ArrStructureDataVO> {
        private ArrStructureDataVO parent;

        public StructureDataFormDataVO() {
        }

        public StructureDataFormDataVO(final ArrStructureDataVO parent, final List<ItemGroupVO> groups, final List<ItemTypeGroupVO> typeGroups) {
            super(parent, groups, typeGroups);
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
