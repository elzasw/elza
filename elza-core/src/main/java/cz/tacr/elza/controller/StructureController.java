package cz.tacr.elza.controller;

import cz.tacr.elza.controller.config.ClientFactoryDO;
import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.ArrStructureDataVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemVO;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrStructureData;
import cz.tacr.elza.domain.ArrStructureItem;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.RulStructureType;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.StructureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.transaction.Transactional;
import java.util.List;


/**
 * TODO
 */
@RestController
@RequestMapping("/api/structure")
public class StructureController {

    private static final Logger logger = LoggerFactory.getLogger(StructureController.class);

    private final StructureService structureService;
    private final ArrangementService arrangementService;
    private final ClientFactoryDO factoryDO;
    private final ClientFactoryVO factoryVO;

    @Autowired
    public StructureController(final StructureService structureService,
                               final ArrangementService arrangementService,
                               final ClientFactoryDO factoryDO,
                               final ClientFactoryVO factoryVO) {
        this.structureService = structureService;
        this.arrangementService = arrangementService;
        this.factoryDO = factoryDO;
        this.factoryVO = factoryVO;
    }

    @Transactional
    @RequestMapping(value = "/data/{structureTypeCode}/{fundVersionId}/create",
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ArrStructureDataVO createStructureData(@PathVariable(value = "structureTypeCode") final String structureTypeCode,
                                                  @PathVariable(value = "fundVersionId") final Integer fundVersionId) {

        ArrFundVersion fundVersion = arrangementService.getFundVersionById(fundVersionId);
        RulStructureType structureType = structureService.getStructureTypeByCode(structureTypeCode);

        if (!structureType.getRuleSet().equals(fundVersion.getRuleSet())) {
            throw new BusinessException("Pravidla z AS se neshodují s pravidly ze strukturovaného typu", ArrangementCode.INVALID_RULE);
        }

        ArrStructureData createStructureData = structureService.createStructureData(fundVersion.getFund(), structureType, ArrStructureData.State.TEMP);
        return factoryVO.createStructureData(createStructureData);
    }

    @Transactional
    @RequestMapping(value = "/data/{structureTypeCode}/{fundVersionId}/confirm",
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ArrStructureDataVO confirmStructureData(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                                   @PathVariable(value = "structureDataId") final Integer structureDataId) {

        ArrFundVersion fundVersion = arrangementService.getFundVersionById(fundVersionId);
        ArrStructureData structureData = structureService.getStructureDataById(structureDataId);

        if (!structureData.getStructureType().getRuleSet().equals(fundVersion.getRuleSet())) {
            throw new BusinessException("Pravidla z AS se neshodují s pravidly ze strukturovaného typu", ArrangementCode.INVALID_RULE);
        }

        ArrStructureData createStructureData = structureService.confirmStructureData(structureData);
        return factoryVO.createStructureData(createStructureData);
    }

    @Transactional
    @RequestMapping(value = "/data/{fundVersionId}/{structureDataId}//delete",
            method = RequestMethod.DELETE,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ArrStructureDataVO deleteStructureData(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                                  @PathVariable(value = "structureDataId") final Integer structureDataId) {
        ArrFundVersion fundVersion = arrangementService.getFundVersionById(fundVersionId);
        ArrStructureData structureData = structureService.getStructureDataById(structureDataId);

        if (!structureData.getStructureType().getRuleSet().equals(fundVersion.getRuleSet())) {
            throw new BusinessException("Pravidla z AS se neshodují s pravidly ze strukturovaného typu", ArrangementCode.INVALID_RULE);
        }

        ArrStructureData deleteStructureData = structureService.deleteStructureData(structureData);
        return factoryVO.createStructureData(deleteStructureData);
    }

    @Transactional
    @RequestMapping(value = "/item/{fundVersionId}/{structureDataId}/{itemTypeId}/create",
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
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

    @Transactional
    @RequestMapping(value = "/item/{fundVersionId}/update/{createNewVersion}",
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public StructureItemResult updateDescItem(@RequestBody final ArrItemVO itemVO,
                                              @PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                              @PathVariable(value = "createNewVersion") final Boolean createNewVersion) {
        ArrStructureItem structureItem = factoryDO.createStructureItem(itemVO);
        ArrStructureItem updateStructureItem = structureService.updateStructureItem(structureItem, fundVersionId, createNewVersion);
        StructureItemResult result = new StructureItemResult();
        result.setItem(factoryVO.createDescItem(updateStructureItem));
        result.setParent(factoryVO.createStructureData(updateStructureItem.getStructureData()));
        return result;
    }

    @Transactional
    @RequestMapping(value = "/item/{fundVersionId}/delete",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public StructureItemResult deleteDescItem(@RequestBody final ArrItemVO itemVO,
                                              @PathVariable(value = "fundVersionId") final Integer fundVersionId) {
        ArrStructureItem structureItem = factoryDO.createStructureItem(itemVO);
        ArrStructureItem deleteStructureItem = structureService.deleteStructureItem(structureItem, fundVersionId);
        StructureItemResult result = new StructureItemResult();
        result.setItem(factoryVO.createDescItem(deleteStructureItem));
        result.setParent(factoryVO.createStructureData(deleteStructureItem.getStructureData()));
        return result;
    }

    @Transactional
    @RequestMapping(value = "/item/{fundVersionId}/{structureDataId}/{itemTypeId}",
            method = RequestMethod.DELETE,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public void deleteItemsByType(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                  @PathVariable(value = "structureDataId") final Integer structureDataId,
                                  @PathVariable(value = "itemTypeId") final Integer itemTypeId) {
        structureService.deleteStructureItemsByType(fundVersionId, structureDataId, itemTypeId);
    }

    @Transactional
    @RequestMapping(value = "/item/form/{fundVersionId}/{structureDataId}",
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public void getFormStructureItems(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                      @PathVariable(value = "structureDataId") final Integer structureDataId) {
        ArrFundVersion fundVersion = arrangementService.getFundVersionById(fundVersionId);
        ArrStructureData structureData = structureService.getStructureDataById(structureDataId);

        if (!structureData.getStructureType().getRuleSet().equals(fundVersion.getRuleSet())) {
            throw new BusinessException("Pravidla z AS se neshodují s pravidly ze strukturovaného typu", ArrangementCode.INVALID_RULE);
        }

        List<RulItemTypeExt> structureItemTypes = structureService.getStructureItemTypes(structureData.getStructureType());
        List<ArrStructureItem> structureItems = structureService.findStructureItems(structureData);
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

}
