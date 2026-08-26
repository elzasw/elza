package cz.tacr.elza.controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.transaction.Transactional;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.controller.config.ClientFactoryDO;
import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.ArrStructureDataVO;
import cz.tacr.elza.controller.vo.FilteredResultVO;
import cz.tacr.elza.controller.vo.RulPartTypeVO;
import cz.tacr.elza.controller.vo.RulStructureTypeVO;
import cz.tacr.elza.controller.vo.StructureExtensionFundVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemVO;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrStructuredItem;
import cz.tacr.elza.domain.ArrStructuredObject;
import cz.tacr.elza.domain.RulPartType;
import cz.tacr.elza.domain.RulStructuredType;
import cz.tacr.elza.domain.RulStructuredTypeExtension;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.FilteredResult;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.StructObjService;

/**
 * Controller pro správu strukturovaných datových typů a jejich hodnot.
 *
 * @since 10.11.2017
 */
@RestController
@RequestMapping(value = "/api/structure",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
public class StructureOldController {

    private final StructObjService structureService;
    private final ArrangementService arrangementService;
    private final ClientFactoryDO factoryDO;
    private final ClientFactoryVO factoryVO;

    @Autowired
    public StructureOldController(final StructObjService structureService,
                               final ArrangementService arrangementService,
                               final ClientFactoryDO factoryDO,
                               final ClientFactoryVO factoryVO) {
        this.structureService = structureService;
        this.arrangementService = arrangementService;
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
    @Deprecated
    @Transactional
    @RequestMapping(value = "/data/{fundVersionId}", method = RequestMethod.POST)
    public ArrStructureDataVO createStructureData(@RequestBody final String structureTypeCode,
                                                  @PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                                  @RequestParam(value = "value", required = false) final String value) {

        ArrFundVersion fundVersion = arrangementService.getFundVersionById(fundVersionId);
        RulStructuredType structureType = structureService.getStructureTypeByCode(structureTypeCode);
        ArrStructuredObject createStructureData = structureService.createStructObj(fundVersion.getFund(), structureType, ArrStructuredObject.State.TEMP);
        if (StringUtils.isNotEmpty(value)) {
            structureService.addItemsFromValue(createStructureData, value);
        }
        return ArrStructureDataVO.newInstance(createStructureData);
    }

    /**
     * Založení duplikátů strukturovaného datového typu a autoinkrementační.
     * Předloha musí být ve stavu {@link ArrStructuredObject.State#TEMP}.
     *
     * @param structureDataId    identifikátor předlohy hodnoty strukturovaného datového typu
     * @param fundVersionId      identifikátor verze AS
     * @param structureDataBatch data pro hromadné vytvoření hodnot
     */
    @Deprecated
    @Transactional
    @RequestMapping(value = "/data/{fundVersionId}/{structureDataId}/batch", method = RequestMethod.POST)
    public void duplicateStructureDataBatch(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                            @PathVariable(value = "structureDataId") final Integer structureDataId,
                                            @RequestBody StructureDataBatch structureDataBatch) {
        Integer count = structureDataBatch.getCount();
        Validate.notNull(count, "Počet položek musí být vyplněn");

        List<Integer> incrementedTypeIds = structureDataBatch.getIncrementedTypeIds();
        Validate.notEmpty(incrementedTypeIds, "Autoincrementující typ musí být alespoň jeden");

        ArrFundVersion fundVersion = arrangementService.getFundVersionById(fundVersionId);
        ArrStructuredObject structureData = structureService.getStructObjById(structureDataId);
        structureService.duplicateStructureDataBatch(fundVersion, structureData, count,
                                                     incrementedTypeIds);
    }

    /**
     * Hromadná úprava položek/hodnot strukt. typu.
     *
     * @param fundVersionId            identifikátor verze AS
     * @param structureTypeCode        kód strukturovaného datového typu
     * @param structureDataBatchUpdate data pro hromadnou úpravu hodnot
     */
    @Deprecated
    @Transactional
    @RequestMapping(value = "/data/{fundVersionId}/{structureTypeCode}/batchUpdate", method = RequestMethod.POST)
    public void updateStructureDataBatch(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                         @PathVariable(value = "structureTypeCode") final String structureTypeCode,
                                         @RequestBody final StructureDataBatchUpdate structureDataBatchUpdate) {
        ArrFundVersion fundVersion = arrangementService.getFundVersionById(fundVersionId);
        RulStructuredType structureType = structureService.getStructureTypeByCode(structureTypeCode);

        Assert.notNull(structureDataBatchUpdate.autoincrementItemTypeIds, "Identifikátory typů atributu pro autoincrement nesmí být null");
        Assert.notNull(structureDataBatchUpdate.deleteItemTypeIds, "Identifikátory typů atributu pro odstranění nesmí být null");
        Assert.notNull(structureDataBatchUpdate.items, "Položky nesmí být null");
        Assert.notEmpty(structureDataBatchUpdate.structureDataIds, "Musí být vyplněn alespoň jeden identifikátor hodnoty strukt. typu");

        List<ArrStructuredItem> structureItems = factoryDO.createStructureItem(structureDataBatchUpdate.getItems());
        structureService.updateStructObjBatch(fundVersion,
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
    @Deprecated
    @Transactional
    @RequestMapping(value = "/data/{fundVersionId}/{structureDataId}/confirm", method = RequestMethod.POST)
    public ArrStructureDataVO confirmStructureData(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                                   @PathVariable(value = "structureDataId") final Integer structureDataId) {
        ArrFundVersion fundVersion = arrangementService.getFundVersionById(fundVersionId);
        ArrStructuredObject structureData = structureService.getStructObjById(structureDataId);
        ArrStructuredObject createStructureData = structureService.confirmStructureData(fundVersion.getFund(), structureData);
        return ArrStructureDataVO.newInstance(createStructureData);
    }

    /**
     * Nastavení přiřaditelnosti.
     *
     * @param fundVersionId    identifikátor verze AS
     * @param assignable       přiřaditelný
     * @param structureDataIds identifikátory hodnot strukturovaného datového typu
     */
    @Deprecated
    @Transactional
    @RequestMapping(value = "/data/{fundVersionId}/assignable/{assignable}", method = RequestMethod.POST)
    public void setAssignableStructObjList(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                               @PathVariable(value = "assignable") final Boolean assignable,
                                               @RequestBody List<Integer> structureDataIds) {
        ArrFundVersion fundVersion = arrangementService.getFundVersionById(fundVersionId);
        List<ArrStructuredObject> structureDataList = structureService.getStructObjByIds(structureDataIds);
        structureService.setAssignableStructureDataList(fundVersion.getFund(), structureDataList, assignable);
    }

    /**
     * Smazání hodnoty strukturovaného datového typu.
     *
     * @param fundVersionId   identifikátor verze AS
     * @param structureDataId identifikátor hodnoty strukturovaného datového typu
     * @return smazaná entita
     */
    @Deprecated
    @Transactional
    @RequestMapping(value = "/data/{fundVersionId}/{structureDataId}", method = RequestMethod.DELETE)
    public ArrStructureDataVO deleteStructureData(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                                  @PathVariable(value = "structureDataId") final Integer structureDataId) {
        ArrFundVersion fundVersion = arrangementService.getFundVersionById(fundVersionId);
        ArrStructuredObject structObj = structureService.getStructObjById(structureDataId);
        structureService.deleteStructObj(fundVersion.getFundId(), Collections.singletonList(structObj));
        return ArrStructureDataVO.newInstance(structObj);
    }

    /**
     * Získání hodnoty strukturovaného datového typu.
     *
     * @param fundVersionId   identifikátor verze AS
     * @param structureDataId identifikátor hodnoty strukturovaného datového typu
     * @return nalezená entita
     */
    @Deprecated
    @Transactional
    @RequestMapping(value = "/data/{fundVersionId}/{structureDataId}", method = RequestMethod.GET)
    public ArrStructureDataVO getStructureData(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                               @PathVariable(value = "structureDataId") final Integer structureDataId) {
        ArrFundVersion fundVersion = arrangementService.getFundVersionById(fundVersionId);
        return ArrStructureDataVO.newInstance(structureService.getStructObjById(structureDataId, fundVersion));
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
    @Deprecated
    @Transactional
    @RequestMapping(value = "/data/{fundVersionId}/{structureTypeCode}/search", method = RequestMethod.GET)
    public FilteredResultVO<ArrStructureDataVO> findStructObj(@PathVariable("fundVersionId") final Integer fundVersionId,
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
        return new FilteredResultVO<>(filteredResult.getList(), ArrStructureDataVO::newInstance,
                filteredResult.getTotalCount());
    }

    /**
     * Vyhledá možné typy strukt. datových typů, které lze v AS používat.
     *
     * @return nalezené entity
     */
    @Deprecated
    @Transactional
    @RequestMapping(value = "/type", method = RequestMethod.GET)
    public List<RulStructureTypeVO> findStructureTypes(@RequestParam(value = "fundVersionId", required = false) final Integer fundVersionId) {
        List<RulStructuredType> structureTypes;
        if (fundVersionId == null) {
            structureTypes = structureService.findStructureTypes();
        } else {
            ArrFundVersion fundVersion = arrangementService.getFundVersionById(fundVersionId);
            structureTypes = structureService.findStructureTypes(fundVersion);
        }
        return structureTypes.stream().map(i -> RulStructureTypeVO.newInstance(i)).collect(Collectors.toList());
    }

    @Deprecated
    @Transactional
    @RequestMapping(value = "/part-type", method = RequestMethod.GET)
    public List<RulPartTypeVO> findPartTypes() {
        List<RulPartType> partTypes = structureService.findPartTypes();
        return partTypes.stream().map(i -> RulPartTypeVO.newInstance(i)).collect(Collectors.toList());
    }

    /**
     * Vyhledá dostupná a aktivovaná rozšíření k AS.
     *
     * @param fundVersionId identifikátor verze AS
     * @return nalezené entity
     */
    @Deprecated
    @Transactional
    @RequestMapping(value = "/extension/{fundVersionId}/{structureTypeCode}", method = RequestMethod.GET)
    public List<StructureExtensionFundVO> findFundStructureExtension(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                                                     @PathVariable(value = "structureTypeCode") final String structureTypeCode) {
        ArrFundVersion fundVersion = arrangementService.getFundVersionById(fundVersionId);
        RulStructuredType structureType = structureService.getStructureTypeByCode(structureTypeCode);
        List<RulStructuredTypeExtension> allStructureExtensions = structureService.findAllStructureExtensions(structureType);
        List<RulStructuredTypeExtension> structureExtensions = structureService.findStructureExtensions(fundVersion.getFund(), structureType);
        return factoryVO.createStructureExtensionFundDeprecated(allStructureExtensions, structureExtensions);
    }

    /**
     * Nastaví konkrétní rozšíření na AS.
     *
     * @param fundVersionId           identifikátor verze AS
     * @param structureExtensionCodes seznam kódů rozšíření, které mají být aktivovány na AS
     */
    @Deprecated
    @Transactional
    @RequestMapping(value = "/extension/{fundVersionId}/{structureTypeCode}", method = RequestMethod.PUT)
    public void setFundStructureExtensions(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                           @PathVariable(value = "structureTypeCode") final String structureTypeCode,
                                           @RequestBody final List<String> structureExtensionCodes) {
        ArrFundVersion fundVersion = arrangementService.getFundVersionById(fundVersionId);
        RulStructuredType structureType = structureService.getStructureTypeByCode(structureTypeCode);
        List<RulStructuredTypeExtension> structureExtensions = structureService.findStructureExtensionByCodes(structureExtensionCodes);
        structureService.setFundStructureExtensions(fundVersion, structureType, structureExtensions);
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
        private List<Integer> incrementedTypeIds;

        public StructureDataBatch() {
        }

        public StructureDataBatch(final Integer count, final List<Integer> itemTypeIds) {
            this.count = count;
            this.incrementedTypeIds = itemTypeIds;
        }

        public Integer getCount() {
            return count;
        }

        public void setCount(final Integer count) {
            this.count = count;
        }

        public List<Integer> getIncrementedTypeIds() {
            return incrementedTypeIds;
        }

        public void setIncrementedTypeIds(final List<Integer> itemTypeIds) {
            this.incrementedTypeIds = itemTypeIds;
        }
    }
}
