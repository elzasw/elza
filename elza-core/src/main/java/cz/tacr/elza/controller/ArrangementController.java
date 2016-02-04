package cz.tacr.elza.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.transaction.Transactional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.api.exception.ConcurrentUpdateException;
import cz.tacr.elza.controller.config.ClientFactoryDO;
import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.ArrCalendarTypeVO;
import cz.tacr.elza.controller.vo.ArrFindingAidVO;
import cz.tacr.elza.controller.vo.ArrFindingAidVersionVO;
import cz.tacr.elza.controller.vo.ArrNodeRegisterVO;
import cz.tacr.elza.controller.vo.ArrPacketVO;
import cz.tacr.elza.controller.vo.RulPacketTypeVO;
import cz.tacr.elza.controller.vo.ScenarioOfNewLevelVO;
import cz.tacr.elza.controller.vo.TreeData;
import cz.tacr.elza.controller.vo.TreeNodeClient;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeDescItemsVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrDescItemGroupVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrDescItemTypeGroupVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrDescItemVO;
import cz.tacr.elza.domain.ArrCalendarType;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFindingAid;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeConformity;
import cz.tacr.elza.domain.ArrNodeConformityError;
import cz.tacr.elza.domain.ArrNodeConformityMissing;
import cz.tacr.elza.domain.ArrNodeRegister;
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.domain.RulArrangementType;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.RulDescItemTypeExt;
import cz.tacr.elza.domain.RulPacketType;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.factory.DescItemFactory;
import cz.tacr.elza.drools.DirectionLevel;
import cz.tacr.elza.repository.ArrangementTypeRepository;
import cz.tacr.elza.repository.CalendarTypeRepository;
import cz.tacr.elza.repository.DescItemTypeRepository;
import cz.tacr.elza.repository.FindingAidVersionRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.RuleSetRepository;
import cz.tacr.elza.service.ArrMoveLevelService;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.DescriptionItemService;
import cz.tacr.elza.service.LevelTreeCacheService;
import cz.tacr.elza.service.PacketService;
import cz.tacr.elza.service.RegistryService;
import cz.tacr.elza.service.RuleService;


/**
 * Kontroler pro pořádání.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 7. 1. 2016
 */
@RestController
@RequestMapping("/api/arrangementManagerV2")
public class ArrangementController {

    @Autowired
    private FindingAidVersionRepository findingAidVersionRepository;

    @Autowired
    private LevelTreeCacheService levelTreeCacheService;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private ArrangementService arrangementService;

    @Autowired
    private CalendarTypeRepository calendarTypeRepository;

    @Autowired
    private RuleService ruleService;

    @Autowired
    private RuleSetRepository ruleSetRepository;

    @Autowired
    private ArrangementTypeRepository arrangementTypeRepository;

    @Autowired
    private DescItemTypeRepository descItemTypeRepository;

    @Autowired
    private ClientFactoryDO factoryDO;

    @Autowired
    private ClientFactoryVO factoryVo;

    @Autowired
    private DescriptionItemService descriptionItemService;

    @Autowired
    private ArrMoveLevelService moveLevelService;

    @Autowired
    private PacketService packetService;

    @Autowired
    private RegistryService registryService;

    @Autowired
    private DescItemFactory descItemFactory;

    @RequestMapping(value = "/packets/types",
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public List<RulPacketTypeVO> getPacketTypes() {
        List<RulPacketType> packetTypes = packetService.getPacketTypes();
        return factoryVo.createPacketTypeList(packetTypes);
    }

    @RequestMapping(value = "/packets/{findingAidId}",
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ArrPacketVO> getPackets(@PathVariable(value = "findingAidId") final Integer findingAidId) {
        Assert.notNull(findingAidId);
        List<ArrPacket> packets = packetService.getPackets(findingAidId);
        return factoryVo.createPacketList(packets);
    }

    @Transactional
    @RequestMapping(value = "/packets/{findingAidId}",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ArrPacketVO insertPacket(@PathVariable(value = "findingAidId") final Integer findingAidId,
                                          @RequestBody final ArrPacketVO packetVO) {
        Assert.notNull(findingAidId);
        Assert.notNull(packetVO);

        ArrPacket packet = factoryDO.createPacket(packetVO, findingAidId);
        return factoryVo.createPacket(packetService.insertPacket(packet));
    }

    @Transactional
    @RequestMapping(value = "/packets/{findingAidId}/{packetId}",
            method = RequestMethod.DELETE,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ArrPacketVO deactivatePacket(@PathVariable(value = "findingAidId") final Integer findingAidId,
                                        @PathVariable(value = "packetId") final Integer packetId) {
        Assert.notNull(findingAidId);
        Assert.notNull(packetId);

        ArrPacket packet = packetService.getPacket(findingAidId, packetId);
        return factoryVo.createPacket(packetService.deactivatePacket(packet));
    }

    @Transactional
    @RequestMapping(value = "/packets/{findingAidId}",
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ArrPacketVO updatePacket(@PathVariable(value = "findingAidId") final Integer findingAidId,
                                    @RequestBody final ArrPacketVO packetVO) {
        Assert.notNull(findingAidId);
        Assert.notNull(packetVO);

        ArrPacket packet = factoryDO.createPacket(packetVO, findingAidId);
        return factoryVo.createPacket(packetService.updatePacket(packet));
    }

    @Transactional
    @RequestMapping(value = "/descItems/{findingAidVersionId}/{nodeId}/{nodeVersion}/{descItemTypeId}",
            method = RequestMethod.DELETE,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public DescItemResult deleteDescItemsByType(@PathVariable(value = "findingAidVersionId") final Integer findingAidVersionId,
                                                @PathVariable(value = "nodeId") final Integer nodeId,
                                                @PathVariable(value = "nodeVersion") final Integer nodeVersion,
                                                @PathVariable(value = "descItemTypeId") final Integer descItemTypeId) {

        Assert.notNull(findingAidVersionId);
        Assert.notNull(nodeVersion);
        Assert.notNull(descItemTypeId);
        Assert.notNull(nodeId);

        ArrNode node = descriptionItemService
                .deleteDescriptionItemsByType(findingAidVersionId, nodeId, nodeVersion, descItemTypeId);

        DescItemResult descItemResult = new DescItemResult();
        descItemResult.setDescItem(null);
        descItemResult.setNode(factoryVo.createArrNode(node));

        return descItemResult;
    }

    @Transactional
    @RequestMapping(value = "/descItems/{findingAidVersionId}/{nodeVersion}/delete",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public DescItemResult deleteDescItem(@RequestBody final ArrDescItemVO descItemVO,
                                         @PathVariable(value = "findingAidVersionId") final Integer findingAidVersionId,
                                         @PathVariable(value = "nodeVersion") final Integer nodeVersion) {
        Assert.notNull(descItemVO);
        Assert.notNull(findingAidVersionId);
        Assert.notNull(nodeVersion);

        ArrDescItem descItemDeleted = descriptionItemService
                .deleteDescriptionItem(descItemVO.getDescItemObjectId(), nodeVersion, findingAidVersionId);

        DescItemResult descItemResult = new DescItemResult();
        descItemResult.setDescItem(null);
        descItemResult.setNode(factoryVo.createArrNode(descItemDeleted.getNode()));

        return descItemResult;
    }

    @Transactional
    @RequestMapping(value = "/descItems/{findingAidVersionId}/{nodeVersion}/update/{createNewVersion}",
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public DescItemResult updateDescItem(@RequestBody final ArrDescItemVO descItemVO,
                                         @PathVariable(value = "findingAidVersionId") final Integer findingAidVersionId,
                                         @PathVariable(value = "nodeVersion") final Integer nodeVersion,
                                         @PathVariable(value = "createNewVersion") final Boolean createNewVersion) {
        Assert.notNull(descItemVO);
        Assert.notNull(findingAidVersionId);
        Assert.notNull(nodeVersion);
        Assert.notNull(createNewVersion);

        ArrDescItem descItem = factoryDO.createDescItem(descItemVO);

        ArrDescItem descItemUpdated = descriptionItemService
                .updateDescriptionItem(descItem, nodeVersion, findingAidVersionId, createNewVersion);

        DescItemResult descItemResult = new DescItemResult();
        descItemResult.setDescItem(factoryVo.createDescItem(descItemUpdated));
        descItemResult.setNode(factoryVo.createArrNode(descItemUpdated.getNode()));

        return descItemResult;
    }

    @Transactional
    @RequestMapping(value = "/descItems/{findingAidVersionId}/{nodeId}/{nodeVersion}/{descItemTypeId}/create",
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public DescItemResult createDescItem(@RequestBody final ArrDescItemVO descItemVO,
                                         @PathVariable(value = "findingAidVersionId") final Integer findingAidVersionId,
                                         @PathVariable(value = "descItemTypeId") final Integer descItemTypeId,
                                         @PathVariable(value = "nodeId") final Integer nodeId,
                                         @PathVariable(value = "nodeVersion") final Integer nodeVersion) {
        Assert.notNull(descItemVO);
        Assert.notNull(findingAidVersionId);
        Assert.notNull(descItemTypeId);
        Assert.notNull(nodeId);
        Assert.notNull(nodeVersion);

        ArrDescItem descItem = factoryDO.createDescItem(descItemVO, descItemTypeId);

        ArrDescItem descItemCreated = descriptionItemService.createDescriptionItem(descItem, nodeId, nodeVersion,
                findingAidVersionId);

        DescItemResult descItemResult = new DescItemResult();
        descItemResult.setDescItem(factoryVo.createDescItem(descItemCreated));
        descItemResult.setNode(factoryVo.createArrNode(descItemCreated.getNode()));

        return descItemResult;
    }

    @RequestMapping(value = "/getFindingAids", method = RequestMethod.GET)
    public List<ArrFindingAidVO> getFindingAids() {
        Map<Integer, ArrFindingAidVO> findingAids = new LinkedHashMap<>();
        findingAidVersionRepository.findAllFetchFindingAids().forEach(version -> {
            ArrFindingAid findingAid = version.getFindingAid();
            ArrFindingAidVO findingAidVO = factoryVo
                    .getOrCreateVo(findingAid.getFindingAidId(), findingAid, findingAids, ArrFindingAidVO.class);
            findingAidVO.getVersions().add(factoryVo.createFindingAidVersion(version));
        });


        return new ArrayList<ArrFindingAidVO>(findingAids.values());
    }

    /**
     * Načte FA pro dané verze.
     *
     * @param idsParam id verzí
     * @return seznam FA, každá obsahuje pouze jednu verzi, jinak je vrácená víckrát
     */
    @RequestMapping(value = "/getVersions", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<ArrFindingAidVO> getFindingAidsByVersionIds(@RequestBody final IdsParam idsParam) {

        if (CollectionUtils.isEmpty(idsParam.getIds())) {
            return Collections.EMPTY_LIST;
        }


        List<ArrFindingAidVersion> versions = findingAidVersionRepository.findAll(idsParam.getIds());


        List<ArrFindingAidVO> result = new LinkedList<>();
        for (ArrFindingAidVersion version : versions) {
            ArrFindingAidVO findingAid = factoryVo.createArrFindingAidVO(version.getFindingAid(), false);
            ArrFindingAidVersionVO versionVo = factoryVo.createFindingAidVersion(version);
            findingAid.setVersions(Arrays.asList(versionVo));

            result.add(findingAid);
        }

        return result;
    }


    /**
     * Provede načtení stromu uzlů. Uzly mohou být rozbaleny.
     *
     * @param input vstupní data pro načtení
     * @return data stromu
     */
    @RequestMapping(value = "/faTree", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public TreeData getFaTree(final @RequestBody FaTreeParam input) {
        Assert.notNull(input);
        Assert.notNull(input.getVersionId());

        return levelTreeCacheService
                .getFaTree(input.getVersionId(), input.getNodeId(), input.getExpandedIds(), input.getIncludeIds());
    }

    /**
     * Načte seznam rodičů daného uzlu. Seřazeno od prvního rodiče po kořen stromu.
     *
     * @param nodeId    nodeid uzlu
     * @param versionId id verze stromu
     * @return seznam rodičů
     */
    @RequestMapping(value = "/nodeParents", method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public List<TreeNodeClient> getNodeParents(@RequestParam(value = "nodeId") final Integer nodeId,
                                               @RequestParam(value = "versionId") final Integer versionId) {
        Assert.notNull(nodeId);
        Assert.notNull(versionId);

        return levelTreeCacheService.getNodeParents(nodeId, versionId);
    }

    /**
     * Uzavře otevřenou verzi archivní pomůcky a otevře novou verzi.
     *
     * @param versionId         verze, která se má uzavřít
     * @param arrangementTypeId id typu výstupu nové verze
     * @param ruleSetId         id pravidel podle kterých se vytváří popis v nové verzi
     * @return nová verze archivní pomůcky
     * @throws ConcurrentUpdateException chyba při současné manipulaci s položkou více uživateli
     */
    @Transactional
    @RequestMapping(value = "/approveVersion", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ArrFindingAidVersionVO approveVersion(@RequestParam("versionId") final Integer versionId,
                                                 @RequestParam("arrangementTypeId") final Integer arrangementTypeId,
                                                 @RequestParam("ruleSetId") final Integer ruleSetId) {
        Assert.notNull(versionId);
        Assert.notNull(arrangementTypeId);
        Assert.notNull(ruleSetId);

        ArrFindingAidVersion version = findingAidVersionRepository.findOne(versionId);
        RulArrangementType arrangementType = arrangementTypeRepository.findOne(arrangementTypeId);
        RulRuleSet ruleSet = ruleSetRepository.findOne(ruleSetId);

        Assert.notNull(version, "Nebyla nalezena verze s id " + versionId);
        Assert.notNull(arrangementType, "Nebyl nalezen typ výstupu podle zvolených pravidel s id " + arrangementTypeId);
        Assert.notNull(ruleSet, "Nebyla nalezena pravidla tvorby s id " + ruleSetId);


        ArrFindingAidVersion nextVersion = arrangementService
                .approveVersion(version, arrangementType, ruleSet);
        return factoryVo.createFindingAidVersion(nextVersion);
    }


    @RequestMapping(value = "/nodes/{nodeId}/{versionId}/form", method = RequestMethod.GET)
    public NodeFormDataVO getNodeFormData(@PathVariable(value = "nodeId") Integer nodeId,
                                          @PathVariable(value = "versionId") Integer versionId) {
        Assert.notNull(versionId, "Identifikátor verze musí být vyplněn");
        Assert.notNull(nodeId, "Identifikátor uzlu musí být vyplněn");

        ArrFindingAidVersion version = findingAidVersionRepository.findOne(versionId);
        ArrNode node = nodeRepository.findOne(nodeId);

        Assert.notNull(version, "Verze AP neexistuje");
        Assert.notNull(node, "Uzel neexistuje");

        List<ArrDescItem> descItems = arrangementService.getDescItems(version, node);
        List<RulDescItemTypeExt> descItemTypes = ruleService.getDescriptionItemTypes(versionId, nodeId);

        ArrNodeVO nodeVO = factoryVo.createArrNode(node);
        List<ArrDescItemGroupVO> descItemGroupsVO = factoryVo.createDescItemGroups(descItems);
        List<ArrDescItemTypeGroupVO> descItemTypeGroupsVO = factoryVo.createDescItemTypeGroups(descItemTypes);
        return new NodeFormDataVO(nodeVO, descItemGroupsVO, descItemTypeGroupsVO);
    }

    @RequestMapping(value = "/calendarTypes", method = RequestMethod.GET)
    public List<ArrCalendarTypeVO> getCalendarTypes() {
        List<ArrCalendarType> calendarTypes = calendarTypeRepository.findAll();
        return factoryVo.createCalendarTypes(calendarTypes);
    }

    /**
     * Vytvoří novou archivní pomůcku se zadaným názvem. Jako datum založení vyplní aktuální datum a čas.
     *
     * @param name              název archivní pomůcky
     * @param arrangementTypeId id typu výstupu
     * @param ruleSetId         id pravidel podle kterých se vytváří popis
     * @return nová archivní pomůcka
     */
    @Transactional
    @RequestMapping(value = "/findingAids", method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ArrFindingAidVO createFindingAid(@RequestParam(value = "name") final String name,
                                            @RequestParam(value = "arrangementTypeId") final Integer arrangementTypeId,
                                            @RequestParam(value = "ruleSetId") final Integer ruleSetId) {

        Assert.hasText(name);
        Assert.notNull(arrangementTypeId);
        Assert.notNull(ruleSetId);

        RulArrangementType arrangementType = arrangementTypeRepository.findOne(arrangementTypeId);
        RulRuleSet ruleSet = ruleSetRepository.findOne(ruleSetId);

        Assert.notNull(arrangementType, "Nebyl nalezen typ výstupu podle zvolených pravidel s id " + arrangementTypeId);
        Assert.notNull(ruleSet, "Nebyla nalezena pravidla tvorby s id " + ruleSetId);


        ArrFindingAid newFindingAid = arrangementService
                .createFindingAidWithScenario(name, ruleSet, arrangementType);

        return factoryVo.createArrFindingAidVO(newFindingAid, true);
    }


    /**
     * Přesun uzlů se stejným rodičem před jiný uzel.
     *
     * @param moveParam vstupní parametry
     */
    @Transactional
    @RequestMapping(value = "/moveLevelBefore", method = RequestMethod.PUT)
    public void moveLevelBefore(@RequestBody LevelMoveParam moveParam) {
        Assert.notNull(moveParam);


        ArrFindingAidVersion version = findingAidVersionRepository.findOne(moveParam.getVersionId());

        ArrNode staticNode = factoryDO.createNode(moveParam.getStaticNode());
        ArrNode staticNodeParent = factoryDO.createNode(moveParam.getStaticNodeParent());
        List<ArrNode> transportNodes = factoryDO.createNodes(moveParam.getTransportNodes());
        ArrNode transportNodeParent = factoryDO.createNode(moveParam.getTransportNodeParent());

        moveLevelService.moveLevelsBefore(version, staticNode, staticNodeParent,
                transportNodes, transportNodeParent);
    }

    /**
     * Přesun uzlů se stejným rodičem za jiný uzel.
     *
     * @param moveParam vstupní parametry
     */
    @Transactional
    @RequestMapping(value = "/moveLevelAfter", method = RequestMethod.PUT)
    public void moveLevelAfter(@RequestBody LevelMoveParam moveParam) {
        Assert.notNull(moveParam);


        ArrFindingAidVersion version = findingAidVersionRepository.findOne(moveParam.getVersionId());

        ArrNode staticNode = factoryDO.createNode(moveParam.getStaticNode());
        ArrNode staticNodeParent = factoryDO.createNode(moveParam.getStaticNodeParent());
        List<ArrNode> transportNodes = factoryDO.createNodes(moveParam.getTransportNodes());
        ArrNode transportNodeParent = factoryDO.createNode(moveParam.getTransportNodeParent());

        moveLevelService.moveLevelsAfter(version, staticNode, staticNodeParent,
                transportNodes, transportNodeParent);
    }


    /**
     * Přesun uzlů se stejným rodičem pod jiný uzel.
     *
     * @param moveParam vstupní parametry
     */
    @Transactional
    @RequestMapping(value = "/moveLevelUnder", method = RequestMethod.PUT)
    public void moveLevelUnder(@RequestBody LevelMoveParam moveParam) {
        Assert.notNull(moveParam);

        ArrFindingAidVersion version = findingAidVersionRepository.findOne(moveParam.getVersionId());

        ArrNode staticNode = factoryDO.createNode(moveParam.getStaticNode());
        List<ArrNode> transportNodes = factoryDO.createNodes(moveParam.getTransportNodes());
        ArrNode transportNodeParent = factoryDO.createNode(moveParam.getTransportNodeParent());

        moveLevelService.moveLevelsUnder(version, staticNode,
                transportNodes, transportNodeParent);
    }


    /**
     * Vyhledá scénáře pro možné archivní pomůcky
     *
     * @param param vstupní parametry
     * @return List scénářů
     */
    @RequestMapping(value = "/getDescriptionItemTypesForNewLevel", method = RequestMethod.POST)
    public List<ScenarioOfNewLevelVO> getDescriptionItemTypesForNewLevel(@RequestBody final DescriptionItemParam param) {
        return factoryVo.createScenarioOfNewLevelList(descriptionItemService.getDescriptionItemTypesForNewLevel(param.getNode().getId(), param.getDirection(), param.getVersionId()));
    }


    /**
     * Načte seznam uzlů podle jejich id.
     *
     * @param idsParam seznam id
     * @return seznam vo uzlů s danými id
     */
    @RequestMapping(value = "/nodes", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public List<TreeNodeClient> getNodes(@RequestBody final IdsParam idsParam) {
        Assert.notNull(idsParam.getVersionId(), "Nebyla zadána verze stromu.");

        List<Integer> nodeIds = idsParam.getIds();
        if (nodeIds.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        return levelTreeCacheService.getNodesByIds(nodeIds, idsParam.getVersionId());
    }


    /**
     * Přidání uzlu do stromu.
     *
     * @param addLevelParam vstupní parametry
     * @return nový přidaný uzel
     */
    @Transactional
    @RequestMapping(value = "/levels", method = RequestMethod.PUT)
    public NodeWithParent addLevel(@RequestBody final AddLevelParam addLevelParam) {
        Assert.notNull(addLevelParam);
        Assert.notNull(addLevelParam.getVersionId());
        Assert.notNull(addLevelParam.getDirection());

        ArrFindingAidVersion version = findingAidVersionRepository.findOne(addLevelParam.getVersionId());

        ArrNode staticNode = factoryDO.createNode(addLevelParam.getStaticNode());
        ArrNode staticParentNode = addLevelParam.getStaticNodeParent() == null ? null : factoryDO
                .createNode(addLevelParam.getStaticNodeParent());

        Set<RulDescItemType> descItemCopyTypes = new HashSet<>();
        if (CollectionUtils.isNotEmpty(addLevelParam.getDescItemCopyTypes())) {
            descItemCopyTypes.addAll(descItemTypeRepository.findAll(addLevelParam.getDescItemCopyTypes()));
        }


        ArrLevel newLevel = moveLevelService.addNewLevel(version, staticNode, staticParentNode,
                addLevelParam.getDirection(), addLevelParam.getScenarioName(),
                descItemCopyTypes);


        return new NodeWithParent(factoryVo.createArrNode(newLevel.getNode()),
                factoryVo.createArrNode(newLevel.getNodeParent()));
    }

    /**
     * Smazání uzlu.
     * @param nodeParam vstupní parametry pro smazání
     */
    @Transactional
    @RequestMapping(value = "/levels", method = RequestMethod.DELETE)
    public NodeWithParent deleteLevel(@RequestBody final NodeParam nodeParam){
        Assert.notNull(nodeParam);
        Assert.notNull(nodeParam.getVersionId());
        Assert.notNull(nodeParam.getStaticNode());

        ArrNode deleteNode = factoryDO.createNode(nodeParam.getStaticNode());
        ArrNode deleteParent = nodeParam.getStaticNodeParent() == null ? null : factoryDO
                .createNode(nodeParam.getStaticNodeParent());

        ArrFindingAidVersion version = findingAidVersionRepository.findOne(nodeParam.getVersionId());

        ArrLevel deleteLevel = moveLevelService.deleteLevel(version, deleteNode, deleteParent);

        return new NodeWithParent(factoryVo.createArrNode(deleteLevel.getNode()),
                factoryVo.createArrNode(deleteLevel.getNodeParent()));
    }


    /**
     * Provede zkopírování atributu daného typu ze staršího bratra uzlu.
     *
     * @param versionId      id verze stromu
     * @param descItemTypeId typ atributu, který chceme zkopírovat
     * @param nodeVO         uzel, na který nastavíme hodnoty ze staršího bratra
     * @return vytvořené hodnoty
     */
    @Transactional
    @RequestMapping(value = "/copyOlderSiblingAttribute", method = RequestMethod.PUT)
    public CopySiblingResult copyOlderSiblingAttribute(
            @RequestParam(required = true) final Integer versionId,
            @RequestParam(required = true) final Integer descItemTypeId,
            @RequestBody final ArrNodeVO nodeVO) {

        ArrFindingAidVersion version = findingAidVersionRepository.getOneCheckExist(versionId);
        RulDescItemType descItemType = descItemTypeRepository.getOneCheckExist(descItemTypeId);
        ArrNode node = factoryDO.createNode(nodeVO);
        ArrLevel level = arrangementService.lockNode(node, version);

        List<ArrDescItem> newDescItems = arrangementService.copyOlderSiblingAttribute(version, descItemType, level);
        newDescItems = descItemFactory.getDescItems(newDescItems);

        RulDescItemTypeDescItemsVO descItemTypeVO = factoryVo.createDescItemTypeVO(descItemType);
        descItemTypeVO.setDescItems(factoryVo.createDescItems(newDescItems));

        ArrNodeVO resultNode = factoryVo.createArrNode(level.getNode());

        return new CopySiblingResult(resultNode, descItemTypeVO);
    }


    /**
     * Provede načtení stromu uzlů. Uzly mohou být rozbaleny.
     *
     * @param input vstupní data pro načtení
     * @return data stromu
     */
    @RequestMapping(value = "/fulltext", method = RequestMethod.POST)
    public List<TreeNodeFulltext> fulltext(final @RequestBody FaFulltextParam input) {
        Assert.notNull(input);
        Assert.notNull(input.getVersionId());

        ArrFindingAidVersion version = findingAidVersionRepository.getOne(input.getVersionId());

        Set<Integer> nodeIds = arrangementService.findNodeIdsByFulltext(version, input.getNodeId(),
                input.getSearchValue(), input.getDepth());

        return levelTreeCacheService.findParentsForNodes(nodeIds, version);
    }

    @RequestMapping(value = "/registerLinks/{nodeId}/{versionId}",
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ArrNodeRegisterVO> findRegisterLinks(final @PathVariable(value = "versionId") Integer versionId,
                                                     final @PathVariable(value = "nodeId") Integer nodeId) {
        List<ArrNodeRegister> registerLinks = registryService.findRegisterLinks(versionId, nodeId);
        return factoryVo.createRegisterLinkList(registerLinks);
    }

    @RequestMapping(value = "/registerLinks/{nodeId}/{versionId}/form",
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public NodeRegisterDataVO findRegisterLinksForm(final @PathVariable(value = "versionId") Integer versionId,
                                                    final @PathVariable(value = "nodeId") Integer nodeId) {
        List<ArrNodeRegisterVO> nodeRegistersVO = findRegisterLinks(versionId, nodeId);
        ArrNode node = nodeRepository.findOne(nodeId);
        return new NodeRegisterDataVO(factoryVo.createArrNode(node), nodeRegistersVO);
    }

    @Transactional
    @RequestMapping(value = "/registerLinks/{nodeId}/{versionId}/create",
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ArrNodeRegisterVO createRegisterLinks(final @PathVariable(value = "versionId") Integer versionId,
                                                       final @PathVariable(value = "nodeId") Integer nodeId,
                                                       final @RequestBody ArrNodeRegisterVO nodeRegisterVO) {
        ArrNodeRegister nodeRegister = factoryDO.createRegisterLink(nodeRegisterVO);
        nodeRegister = registryService.createRegisterLink(versionId, nodeId, nodeRegister);
        return factoryVo.createRegisterLink(nodeRegister);
    }

    @Transactional
    @RequestMapping(value = "/registerLinks/{nodeId}/{versionId}/update",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ArrNodeRegisterVO updateRegisterLinks(final @PathVariable(value = "versionId") Integer versionId,
                                                 final @PathVariable(value = "nodeId") Integer nodeId,
                                                 final @RequestBody ArrNodeRegisterVO nodeRegisterVO) {
        ArrNodeRegister nodeRegister = factoryDO.createRegisterLink(nodeRegisterVO);
        nodeRegister = registryService.updateRegisterLink(versionId, nodeId, nodeRegister);
        return factoryVo.createRegisterLink(nodeRegister);
    }

    @Transactional
    @RequestMapping(value = "/registerLinks/{nodeId}/{versionId}/delete",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ArrNodeRegisterVO deleteRegisterLinks(final @PathVariable(value = "versionId") Integer versionId,
                                                 final @PathVariable(value = "nodeId") Integer nodeId,
                                                 final @RequestBody ArrNodeRegisterVO nodeRegisterVO) {
        ArrNodeRegister nodeRegister = factoryDO.createRegisterLink(nodeRegisterVO);
        nodeRegister = registryService.deleteRegisterLink(versionId, nodeId, nodeRegister);
        return factoryVo.createRegisterLink(nodeRegister);
    }

    /**
     * Validuje verzi archivní pomůcky a vrátí list chyb.
     * Pokud je počet chyb 0 pak předpokládáme že stav AP = OK
     *
     * @param versionId         verze, která se má validovat
     * @return Objekt s listem (prvních 20) chyb
     */
    @RequestMapping(value = "/validateVersion/{versionId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<VersionValidationItem> validateVersion(@PathVariable("versionId") final Integer versionId) {
        Assert.notNull(versionId);

        ArrFindingAidVersion findingAidVersion = findingAidVersionRepository.findOne(versionId);
        if (findingAidVersion == null) {
            throw new IllegalStateException("Neexistuje verze archivní pomůcky s id " + versionId);
        }

        List<ArrNodeConformity> validationErrors = arrangementService.findConformityErrors(findingAidVersion);

        Map<Integer, String> validations = new LinkedHashMap<Integer, String>();
        for (ArrNodeConformity conformity : validationErrors) {
            String description = validations.get(conformity.getNode().getNodeId());

            if (description == null) {
                description = "";
            }

            List<String> descriptions = new LinkedList<String>();
            for (ArrNodeConformityError error : conformity.getErrorConformity()) {
                descriptions.add(error.getDescription());
            }

            for (ArrNodeConformityMissing missing : conformity.getMissingConformity()) {
                descriptions.add(missing.getDescription());
            }

            description += description + StringUtils.join(descriptions, " ");

            validations.put(conformity.getNode().getNodeId(), description);
        }

        List<VersionValidationItem> versionValidationItems = new ArrayList<>(validations.size());
        for (Integer nodeId : validations.keySet()) {
            String description = validations.get(nodeId);
            VersionValidationItem versionValidationItem = new VersionValidationItem();
            versionValidationItem.setDescription(description);
            versionValidationItem.setNodeId(nodeId);

            versionValidationItems.add(versionValidationItem);
        }

        return versionValidationItems;
    }

    /**
     * Validuje verzi archivní pomůcky a vrátí počet chyb
     * Pokud je počet chyb 0 pak předpokládáme že stav AP = OK
     *
     * @param versionId         verze, která se má validovat
     * @return počet chyb
     */
    @RequestMapping(value = "/validateVersionCount/{versionId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Integer validateVersionCount(@PathVariable("versionId") final Integer versionId) {
        Assert.notNull(versionId);

        ArrFindingAidVersion findingAidVersion = findingAidVersionRepository.findOne(versionId);
        if (findingAidVersion == null) {
            throw new IllegalStateException("Neexistuje verze archivní pomůcky s id " + versionId);
        }

        return arrangementService.getVersionErrorCount(findingAidVersion);
    }

    public static class VersionValidationItem {

        private int nodeId;

        private String description;

        public int getNodeId() {
            return nodeId;
        }

        public void setNodeId(int nodeId) {
            this.nodeId = nodeId;
        }

        public String getDesciption() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    /**
     * Výstupní objekt pro získaná data pro formulář detailu uzlu.
     */
    public static class NodeRegisterDataVO {

        /**
         * Uzel
         */
        private ArrNodeVO node;

        /**
         * Seznam odkazů
         */
        private List<ArrNodeRegisterVO> nodeRegisters;

        public NodeRegisterDataVO(final ArrNodeVO node, final List<ArrNodeRegisterVO> nodeRegisters) {
            this.node = node;
            this.nodeRegisters = nodeRegisters;
        }

        public ArrNodeVO getNode() {
            return node;
        }

        public void setNode(final ArrNodeVO node) {
            this.node = node;
        }

        public List<ArrNodeRegisterVO> getNodeRegisters() {
            return nodeRegisters;
        }

        public void setNodeRegisters(final List<ArrNodeRegisterVO> nodeRegisters) {
            this.nodeRegisters = nodeRegisters;
        }
    }

    /**
     * Výstupní objekt pro získaná data pro formulář detailu uzlu.
     */
    public static class NodeFormDataVO {

        /**
         * Uzel
         */
        private ArrNodeVO node;

        /**
         * Seznam skupin
         */
        private List<ArrDescItemGroupVO> descItemGroups;

        /**
         * Seznam skupin typů hodnot archivní pomůcky
         */
        private List<ArrDescItemTypeGroupVO> descItemTypeGroups;

        public NodeFormDataVO() {

        }

        public NodeFormDataVO(final ArrNodeVO node,
                              final List<ArrDescItemGroupVO> descItemGroups,
                              final List<ArrDescItemTypeGroupVO> descItemTypeGroups) {
            this.node = node;
            this.descItemGroups = descItemGroups;
            this.descItemTypeGroups = descItemTypeGroups;
        }

        public ArrNodeVO getNode() {
            return node;
        }

        public void setNodeVO(final ArrNodeVO node) {
            this.node = node;
        }

        public List<ArrDescItemGroupVO> getDescItemGroups() {
            return descItemGroups;
        }

        public void setDescItemGroups(final List<ArrDescItemGroupVO> descItemGroups) {
            this.descItemGroups = descItemGroups;
        }

        public List<ArrDescItemTypeGroupVO> getDescItemTypeGroups() {
            return descItemTypeGroups;
        }

        public void setDescItemTypeGroups(final List<ArrDescItemTypeGroupVO> descItemTypeGroups) {
            this.descItemTypeGroups = descItemTypeGroups;
        }
    }


    /**
     * Vstupní parametry pro metodu /faTree {@link #getFaTree(FaTreeParam)}.
     */
    public static class FaTreeParam {

        /**
         * Id verze.
         */
        private Integer versionId;
        /**
         * Id kořenového uzlu vrácených výsledků.
         */
        private Integer nodeId;
        /**
         * Množina rozobalených uzlů.
         */
        private Set<Integer> expandedIds;
        /**
         * Množina id uzlů, které chceme zviditelnit.
         */
        private Set<Integer> includeIds;

        public Integer getVersionId() {
            return versionId;
        }

        public void setVersionId(final Integer versionId) {
            this.versionId = versionId;
        }

        public Integer getNodeId() {
            return nodeId;
        }

        public void setNodeId(final Integer nodeId) {
            this.nodeId = nodeId;
        }

        public Set<Integer> getExpandedIds() {
            return expandedIds;
        }

        public void setExpandedIds(final Set<Integer> expandedIds) {
            this.expandedIds = expandedIds;
        }

        public Set<Integer> getIncludeIds() {
            return includeIds;
        }

        public void setIncludeIds(final Set<Integer> includeIds) {
            this.includeIds = includeIds;
        }
    }

    /**
     * Výstupní objekt pro hodnotu atributu a uzel.
     * - pro create / delete / update
     */
    public static class DescItemResult {

        /**
         * uzel
         */
        private ArrNodeVO node;

        /**
         * hodnota atributu
         */
        private ArrDescItemVO descItem;

        public ArrNodeVO getNode() {
            return node;
        }

        public void setNode(final ArrNodeVO node) {
            this.node = node;
        }

        public ArrDescItemVO getDescItem() {
            return descItem;
        }

        public void setDescItem(final ArrDescItemVO descItem) {
            this.descItem = descItem;
        }
    }


    /**
     * Vstupní parametry pro přesuny uzlů.
     */
    public static class LevelMoveParam extends NodeParam{

        /**
         * Seznam uzlů, které přesouváme.
         */
        private List<ArrNodeVO> transportNodes;
        /**
         * Rodič uzlů, které přesouváme.
         */
        private ArrNodeVO transportNodeParent;

        public List<ArrNodeVO> getTransportNodes() {
            return transportNodes;
        }

        public void setTransportNodes(final List<ArrNodeVO> transportNodes) {
            this.transportNodes = transportNodes;
        }

        public ArrNodeVO getTransportNodeParent() {
            return transportNodeParent;
        }

        public void setTransportNodeParent(final ArrNodeVO transportNodeParent) {
            this.transportNodeParent = transportNodeParent;
        }
    }

    /**
     * Vstupní parametry pro přidání uzlu.
     */
    public static class AddLevelParam extends NodeParam{
        /**
         * Směr přidávání uzlu (před, za, pod)
         */
        private ArrMoveLevelService.AddLevelDirection direction;
        /**
         * Název scénáře, ze kterého se mají převzít výchozí hodnoty atributů.
         */
        @Nullable
        private String scenarioName;

        /**
         * Seznam id typů atributů, které budou zkopírovány z uzlu přímo nadřazeným nad přidaným uzlem (jeho mladší sourozenec).
         */
        @Nullable
        private Set<Integer> descItemCopyTypes;

        public ArrMoveLevelService.AddLevelDirection getDirection() {
            return direction;
        }

        public void setDirection(final ArrMoveLevelService.AddLevelDirection direction) {
            this.direction = direction;
        }

        public String getScenarioName() {
            return scenarioName;
        }

        public void setScenarioName(final String scenarioName) {
            this.scenarioName = scenarioName;
        }

        public Set<Integer> getDescItemCopyTypes() {
            return descItemCopyTypes;
        }

        public void setDescItemCopyTypes(final Set<Integer> descItemCopyTypes) {
            this.descItemCopyTypes = descItemCopyTypes;
        }
    }

    public static class NodeParam {

        /**
         * Id verze stromu.
         */
        private Integer versionId;
        /**
         * Statický uzel (za/před/pod který přidáváme)
         */
        private ArrNodeVO staticNode;
        /**
         * Rodič statického uzlu (za/před/pod který přidáváme)
         */
        private ArrNodeVO staticNodeParent;

        public Integer getVersionId() {
            return versionId;
        }

        public void setVersionId(final Integer versionId) {
            this.versionId = versionId;
        }

        public ArrNodeVO getStaticNode() {
            return staticNode;
        }

        public void setStaticNode(final ArrNodeVO staticNode) {
            this.staticNode = staticNode;
        }

        public ArrNodeVO getStaticNodeParent() {
            return staticNodeParent;
        }

        public void setStaticNodeParent(final ArrNodeVO staticNodeParent) {
            this.staticNodeParent = staticNodeParent;
        }
    }

    /**
     * Vstupní parametry pro metodu /fulltext {@link #fulltext(FaFulltextParam)}.
     */
    public static class FaFulltextParam {

        /**
         * Id verze.
         */
        private Integer versionId;
        /**
         * Id uzlu pod kterým se má hledat.
         */
        private Integer nodeId;
        /**
         * Hledaná hodnota.
         */
        private String searchValue;
        /**
         * Hloubka v jaké se má hledat pokud je předáno nodeId.
         */
        private Depth depth;

        public Integer getVersionId() {
            return versionId;
        }
        public void setVersionId(Integer versionId) {
            this.versionId = versionId;
        }
        public Integer getNodeId() {
            return nodeId;
        }
        public void setNodeId(Integer nodeId) {
            this.nodeId = nodeId;
        }
        public String getSearchValue() {
            return searchValue;
        }
        public void setSearchValue(String searchValue) {
            this.searchValue = searchValue;
        }
        public Depth getDepth() {
            return depth;
        }
        public void setDepth(Depth depth) {
            this.depth = depth;
        }

    }

    /**
     * Hloubka v jaké se bude ve stromu vyhledávat.
     */
    public static enum Depth {

        /** Vyhledává se v celém podstromu. */
        SUBTREE,
        /** Vyhledává se jen na úrovni pod předaným nodeId. */
        ONE_LEVEL;
    }

    /** Výstup metody /fulltext {@link #fulltext(FaFulltextParam)}. */
    public static class TreeNodeFulltext {

        /** Id nalezeného nodu. */
        private Integer nodeId;

        /** Rodič nalezeného nodu. */
        private TreeNodeClient parent;

        public Integer getNodeId() {
            return nodeId;
        }

        public void setNodeId(Integer nodeId) {
            this.nodeId = nodeId;
        }

        public TreeNodeClient getParent() {
            return parent;
        }

        public void setParent(TreeNodeClient parent) {
            this.parent = parent;
        }
    }

    public static class IdsParam {

        private List<Integer> ids;
        private Integer versionId;

        public List<Integer> getIds() {
            return ids;
        }

        public void setIds(final List<Integer> ids) {
            this.ids = ids;
        }

        public Integer getVersionId() {
            return versionId;
        }

        public void setVersionId(final Integer versionId) {
            this.versionId = versionId;
        }
    }

    public static class CopySiblingResult {

        private ArrNodeVO node;
        private RulDescItemTypeDescItemsVO type;

        public CopySiblingResult(final ArrNodeVO node, final RulDescItemTypeDescItemsVO type) {
            this.node = node;
            this.type = type;
        }

        public ArrNodeVO getNode() {
            return node;
        }

        public void setNode(final ArrNodeVO node) {
            this.node = node;
        }

        public RulDescItemTypeDescItemsVO getType() {
            return type;
        }

        public void setType(final RulDescItemTypeDescItemsVO type) {
            this.type = type;
        }
    }

    public static class DescriptionItemParam {
        /**
         * Id verze stromu.
         */
        private Integer versionId;
        /**
         * Statický uzel (za/před/pod který přidáváme)
         */
        private ArrNodeVO node;
        /**
         * Směr přidávání uzlu (před, za, pod)
         */
        private DirectionLevel direction;

        public Integer getVersionId() {
            return versionId;
        }

        public void setVersionId(Integer versionId) {
            this.versionId = versionId;
        }

        public ArrNodeVO getNode() {
            return node;
        }

        public void setNode(ArrNodeVO node) {
            this.node = node;
        }

        public DirectionLevel getDirection() {
            return direction;
        }

        public void setDirection(DirectionLevel direction) {
            this.direction = direction;
        }
    }

    /**
     * Jednotka popisu - node + node parent
     */
    public static class NodeWithParent {

        /**
         * Jednotka popisu.
         */
        private ArrNodeVO node;

        /**
         * Rodič jednotky popisu.
         */
        private ArrNodeVO parentNode;

        public ArrNodeVO getNode() {
            return node;
        }

        public void setNode(final ArrNodeVO node) {
            this.node = node;
        }

        public ArrNodeVO getParentNode() {
            return parentNode;
        }

        public void setParentNode(final ArrNodeVO parentNode) {
            this.parentNode = parentNode;
        }

        public NodeWithParent(final ArrNodeVO node, final ArrNodeVO parentNode) {
            this.node = node;
            this.parentNode = parentNode;
        }
    }
}
