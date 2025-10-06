package cz.tacr.elza.controller;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.controller.vo.DataString;
import cz.tacr.elza.controller.vo.DataText;
import cz.tacr.elza.controller.vo.DataType;
import cz.tacr.elza.controller.vo.ItemData;
import cz.tacr.elza.controller.vo.ItemDataResult;
import cz.tacr.elza.controller.vo.NodeItem;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.service.ArrangementFormService;
import cz.tacr.elza.service.DescriptionItemService;
import jakarta.transaction.Transactional;

@RestController
@RequestMapping("/api/v1")
public class DescItemController implements DescitemsApi {

	@Autowired
    private ArrangementFormService formService;

    @Autowired
    private DescriptionItemService descriptionItemService;

    /**
     * Vytvoření hodnoty atributu (nová).
     *
     * @param fundVersionId  identfikátor verze AS
     * @param nodeItem       hodnota atributu
     * @return hodnota atributu
     */
    @Override
    @Transactional
    // @RequestMapping "/descItems/{fundVersionId}/create"
    public ResponseEntity<ItemDataResult> descItemCreateDescItem(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                                                 @RequestBody final NodeItem nodeItem) {
        Validate.notNull(nodeItem, "Hodnota atributu musí být vyplněna");
        Validate.notNull(nodeItem.getNodeId(), "Nebyl vyplněn identifikátor uzlu JP");
        Validate.notNull(nodeItem.getNodeVersion(), "Nebyla vyplněna verze uzlu JP");
        Validate.notNull(fundVersionId, "Nebyl vyplněn identifikátor verze AS");

        // nepovolujeme prázdné řádky pro DataText i DataString
        if (nodeItem.getData() != null) {
			ItemData data = nodeItem.getData();
        	if (data.getDataType() == DataType.TEXT) {
        		Validate.isTrue(StringUtils.isNotBlank(((DataText) data).getTextValue()), "Textové pole nesmí být prázdné");
        	}
        	if (data.getDataType() == DataType.STRING) {
        		Validate.isTrue(StringUtils.isNotBlank(((DataString) data).getStringValue()), "Stringové pole nesmí být prázdné");
        	}
        }

        ArrDescItem descItemCreated = descriptionItemService.createDescriptionItem(nodeItem, nodeItem.getNodeId(), fundVersionId);

        return ResponseEntity.ok(formService.createItemDataResult(descItemCreated));
    }

    /**
     * Aktualizace hodnoty atributu (nová).
     *
     * @param fundVersionId    identfikátor verze AS
     * @param createNewVersion vytvořit novou verzi?
     * @param nodeItem         hodnota atributu
     */
    @Override
    @Transactional
    // @RequestMapping "/descItems/{fundVersionId}/{createNewVersion}/update
    public ResponseEntity<ItemDataResult> descItemUpdateDescItem(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                         					     @PathVariable(value = "createNewVersion") final Boolean createNewVersion,
                                         					     @RequestBody final NodeItem nodeItem) {
        Validate.notNull(nodeItem, "Hodnota atributu musí být vyplněna");
        Validate.notNull(nodeItem.getNodeId(), "Nebyl vyplněn identifikátor uzlu JP");
        Validate.notNull(nodeItem.getNodeVersion(), "Nebyla vyplněna verze uzlu JP");
        Validate.notNull(nodeItem.getPosition(), "Pozice musí být vyplněna");
        Validate.notNull(nodeItem.getItemObjectId(), "Identifikátor hodnoty atributu musí být vyplněn");
        Validate.notNull(fundVersionId, "Nebyl vyplněn identifikátor verze AS");
        Validate.notNull(createNewVersion, "Vytvořit novou verzi musí být vyplněno");

		ArrDescItem descItemUpdated = descriptionItemService.updateDescriptionItem(nodeItem, nodeItem.getNodeVersion(), nodeItem.getNodeId(), fundVersionId, createNewVersion, false);

        return ResponseEntity.ok(formService.createItemDataResult(descItemUpdated));
    }

    /**
     * Smazání hodnoty atributu (nová).
     *
     * @param fundVersionId identfikátor verze AS
     * @param nodeId        node id
     * @param nodeVersion   node version
     */
    @Override
    @Transactional
    // DELETE /descItems/{fundVersionId}
    public ResponseEntity<ItemDataResult> descItemDeleteDescItem(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                          					     @RequestBody final NodeItem nodeItem) {
        Validate.notNull(nodeItem, "Hodnota atributu musí být vyplněna");
        Validate.notNull(nodeItem.getNodeId(), "Nebyl vyplněn identifikátor uzlu JP");
        Validate.notNull(nodeItem.getNodeVersion(), "Nebyla vyplněna verze uzlu JP");
        Validate.notNull(nodeItem.getItemObjectId(), "Identifikátor hodnoty atributu musí být vyplněn");
        Validate.notNull(fundVersionId, "Nebyl vyplněn identifikátor verze AS");

        ArrDescItem descItemDeleted = descriptionItemService.deleteDescriptionItem(nodeItem.getItemObjectId(), nodeItem.getNodeVersion(), nodeItem.getNodeId(), fundVersionId, false);

        return ResponseEntity.ok(formService.createItemDataResult(descItemDeleted));
    }
}
