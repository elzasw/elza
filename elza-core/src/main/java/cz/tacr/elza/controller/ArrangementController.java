package cz.tacr.elza.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.transaction.Transactional;

import cz.tacr.elza.controller.vo.NodeItemWithParent;
import cz.tacr.elza.repository.PacketTypeRepository;
import cz.tacr.elza.service.PolicyService;
import org.apache.commons.collections4.CollectionUtils;
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
import cz.tacr.elza.controller.vo.ArrFundVO;
import cz.tacr.elza.controller.vo.ArrFundVersionVO;
import cz.tacr.elza.controller.vo.ArrNodeRegisterVO;
import cz.tacr.elza.controller.vo.ArrPacketVO;
import cz.tacr.elza.controller.vo.FilterNode;
import cz.tacr.elza.controller.vo.FilterNodePosition;
import cz.tacr.elza.controller.vo.FundListCountResult;
import cz.tacr.elza.controller.vo.RulPacketTypeVO;
import cz.tacr.elza.controller.vo.ScenarioOfNewLevelVO;
import cz.tacr.elza.controller.vo.TreeData;
import cz.tacr.elza.controller.vo.TreeNodeClient;
import cz.tacr.elza.controller.vo.filter.Filters;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeDescItemsVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrDescItemVO;
import cz.tacr.elza.controller.vo.nodes.descitems.DescItemGroupVO;
import cz.tacr.elza.controller.vo.nodes.descitems.DescItemTypeGroupVO;
import cz.tacr.elza.domain.ArrCalendarType;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeConformity;
import cz.tacr.elza.domain.ArrNodeRegister;
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.RulDescItemTypeExt;
import cz.tacr.elza.domain.RulPacketType;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.factory.DescItemFactory;
import cz.tacr.elza.drools.DirectionLevel;
import cz.tacr.elza.exception.FilterExpiredException;
import cz.tacr.elza.filter.DescItemTypeFilter;
import cz.tacr.elza.repository.ArrangementTypeRepository;
import cz.tacr.elza.repository.CalendarTypeRepository;
import cz.tacr.elza.repository.DescItemSpecRepository;
import cz.tacr.elza.repository.DescItemTypeRepository;
import cz.tacr.elza.repository.FundRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.InstitutionRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.RuleSetRepository;
import cz.tacr.elza.service.ArrMoveLevelService;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.DescriptionItemService;
import cz.tacr.elza.service.FilterTreeService;
import cz.tacr.elza.service.LevelTreeCacheService;
import cz.tacr.elza.service.PacketService;
import cz.tacr.elza.service.RegistryService;
import cz.tacr.elza.service.RuleService;
import cz.tacr.elza.service.exception.DeleteFailedException;


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
    private FundVersionRepository fundVersionRepository;

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

    @Autowired
    private FilterTreeService filterTreeService;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private DescItemSpecRepository descItemSpecRepository;

    @Autowired
    private FundRepository fundRepository;

    @Autowired
    private PolicyService policyService;

    @Autowired
    private PacketTypeRepository packetTypeRepository;

    /**
     * Seznam typů obalů.
     * @return typy obalů
     */
    @RequestMapping(value = "/packets/types",
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public List<RulPacketTypeVO> getPacketTypes() {
        List<RulPacketType> packetTypes = packetService.getPacketTypes();
        return factoryVo.createPacketTypeList(packetTypes);
    }

    /**
     * Vložení nového obalu pro AP.
     *
     * @param fundId  identifikátor AP
     * @param packetVO      obal
     * @return obal
     */
    @Transactional
    @RequestMapping(value = "/packets/{fundId}",
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ArrPacketVO insertPacket(@PathVariable(value = "fundId") final Integer fundId,
                                    @RequestBody final ArrPacketVO packetVO) {
        Assert.notNull(fundId);
        Assert.notNull(packetVO);

        ArrPacket packet = factoryDO.createPacket(packetVO, fundId);
        return factoryVo.createPacket(packetService.insertPacket(packet));
    }

    /**
     * Vyhledání obalů podle textu - pro formulář JP.
     *
     * @param fundId    id archivního fondu
     * @param input     vstupní parametry
     * @return  seznam obalů
     */
    @RequestMapping(value = "/packets/{fundId}/find/form",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ArrPacketVO> findPacketsForm(@PathVariable(value = "fundId") final Integer fundId,
                                             @RequestBody final PacketFindFormParam input) {
        Assert.notNull(fundId);
        Assert.notNull(input);
        Assert.notNull(input.getLimit());

        ArrFund fund = fundRepository.getOneCheckExist(fundId);
        List<ArrPacket> packets = packetService.findPackets(fund, input.getLimit(), input.getText());
        return factoryVo.createPacketList(packets);
    }

    /**
     * Vyhledání obalů pro správu.
     *
     * @param fundId    id archivního fondu
     * @param input     vstupní parametry
     * @return seznam obalů
     */
    @RequestMapping(value = "/packets/{fundId}/find",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ArrPacketVO> findPackets(@PathVariable(value = "fundId") final Integer fundId,
                                         @RequestBody final PacketFindParam input) {
        Assert.notNull(fundId);
        Assert.notNull(input);
        Assert.notNull(input.getState());

        ArrFund fund = fundRepository.getOneCheckExist(fundId);
        List<ArrPacket> packets = packetService.findPackets(fund, input.getPrefix(), input.getState());
        return factoryVo.createPacketList(packets);
    }

    /**
     * Smazání obalů.
     *
     * @param fundId    id archivního fondu
     * @param input     vstupní parametry
     */
    @Transactional
    @RequestMapping(value = "/packets/{fundId}",
            method = RequestMethod.DELETE,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public void deletePackets(@PathVariable(value = "fundId") final Integer fundId,
                              @RequestBody final PacketDeleteParam input) {
        Assert.notNull(fundId);
        Assert.notNull(input);

        ArrFund fund = fundRepository.getOneCheckExist(fundId);
        packetService.deletePackets(fund, input.getPacketIds());
    }

    /**
     * Změna stavu obalů.
     *
     * @param fundId    id archivního fondu
     * @param input     vstupní parametry
     */
    @Transactional
    @RequestMapping(value = "/packets/{fundId}",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public void setStatePackets(@PathVariable(value = "fundId") final Integer fundId,
                                @RequestBody final PacketSetStateParam input) {
        Assert.notNull(fundId);
        Assert.notNull(input);

        ArrFund fund = fundRepository.getOneCheckExist(fundId);
        packetService.setStatePackets(fund, input.getPacketIds(), input.getState());
    }

    /**
     * Vygenerování/přegenerování obalů.
     *
     * @param fundId    id archivního fondu
     * @param input     vstupní parametry
     */
    @Transactional
    @RequestMapping(value = "/packets/{fundId}/generate",
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public void generatePackets(@PathVariable(value = "fundId") final Integer fundId,
                                @RequestBody final PacketGenerateParam input) {
        Assert.notNull(fundId);
        Assert.notNull(input);

        Assert.notNull(input.getFromNumber());
        Assert.notNull(input.getLenNumber());
        Assert.notNull(input.getPrefix());
        Assert.notNull(input.getCount());

        ArrFund fund = fundRepository.getOneCheckExist(fundId);
        RulPacketType packetType = input.getPacketTypeId() != null ?
                packetTypeRepository.getOneCheckExist(input.getPacketTypeId()) : null;

        packetService.generatePackets(fund,
                packetType,
                input.getPrefix(),
                input.getFromNumber(),
                input.getLenNumber(),
                input.getCount(),
                input.getPacketIds());
    }

    /**
     * Smazání hodnot atributu podle typu.
     *
     * @param fundVersionId   identfikátor verze AP
     * @param nodeId                identfikátor JP
     * @param nodeVersion           verze JP
     * @param descItemTypeId        identfikátor typu hodnoty atributu
     */
    @Transactional
    @RequestMapping(value = "/descItems/{fundVersionId}/{nodeId}/{nodeVersion}/{descItemTypeId}",
            method = RequestMethod.DELETE,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public DescItemResult deleteDescItemsByType(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                                @PathVariable(value = "nodeId") final Integer nodeId,
                                                @PathVariable(value = "nodeVersion") final Integer nodeVersion,
                                                @PathVariable(value = "descItemTypeId") final Integer descItemTypeId) {

        Assert.notNull(fundVersionId);
        Assert.notNull(nodeVersion);
        Assert.notNull(descItemTypeId);
        Assert.notNull(nodeId);

        ArrNode node = descriptionItemService
                .deleteDescriptionItemsByType(fundVersionId, nodeId, nodeVersion, descItemTypeId);

        DescItemResult descItemResult = new DescItemResult();
        descItemResult.setDescItem(null);
        descItemResult.setNode(factoryVo.createArrNode(node));

        return descItemResult;
    }

    /**
     * Smazání hodnoty atributu.
     *
     * @param descItemVO            hodnota atributu
     * @param fundVersionId   identfikátor verze AP
     * @param nodeVersion           verze JP
     */
    @Transactional
    @RequestMapping(value = "/descItems/{fundVersionId}/{nodeVersion}/delete",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public DescItemResult deleteDescItem(@RequestBody final ArrDescItemVO descItemVO,
                                         @PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                         @PathVariable(value = "nodeVersion") final Integer nodeVersion) {
        Assert.notNull(descItemVO);
        Assert.notNull(fundVersionId);
        Assert.notNull(nodeVersion);

        ArrDescItem descItemDeleted = descriptionItemService
                .deleteDescriptionItem(descItemVO.getDescItemObjectId(), nodeVersion, fundVersionId);

        DescItemResult descItemResult = new DescItemResult();
        descItemResult.setDescItem(null);
        descItemResult.setNode(factoryVo.createArrNode(descItemDeleted.getNode()));

        return descItemResult;
    }

    /**
     * Aktualizace hodnoty atributu.
     *
     * @param descItemVO            hodnota atributu
     * @param fundVersionId   identfikátor verze AP
     * @param nodeVersion           verze JP
     * @param createNewVersion      vytvořit novou verzi?
     */
    @Transactional
    @RequestMapping(value = "/descItems/{fundVersionId}/{nodeVersion}/update/{createNewVersion}",
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public DescItemResult updateDescItem(@RequestBody final ArrDescItemVO descItemVO,
                                         @PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                         @PathVariable(value = "nodeVersion") final Integer nodeVersion,
                                         @PathVariable(value = "createNewVersion") final Boolean createNewVersion) {
        Assert.notNull(descItemVO);
        Assert.notNull(fundVersionId);
        Assert.notNull(nodeVersion);
        Assert.notNull(createNewVersion);

        ArrDescItem descItem = factoryDO.createDescItem(descItemVO);

        ArrDescItem descItemUpdated = descriptionItemService
                .updateDescriptionItem(descItem, nodeVersion, fundVersionId, createNewVersion);

        DescItemResult descItemResult = new DescItemResult();
        descItemResult.setDescItem(factoryVo.createDescItem(descItemUpdated));
        descItemResult.setNode(factoryVo.createArrNode(descItemUpdated.getNode()));

        return descItemResult;
    }

    /**
     * Vytvoření hodnoty atributu.
     *
     * @param descItemVO            hodnota atributu
     * @param fundVersionId   identfikátor verze AP
     * @param descItemTypeId        identfikátor typu hodnoty atributu
     * @param nodeId                identfikátor JP
     * @param nodeVersion           verze JP
     * @return hodnota atributu
     */
    @Transactional
    @RequestMapping(value = "/descItems/{fundVersionId}/{nodeId}/{nodeVersion}/{descItemTypeId}/create",
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public DescItemResult createDescItem(@RequestBody final ArrDescItemVO descItemVO,
                                         @PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                         @PathVariable(value = "descItemTypeId") final Integer descItemTypeId,
                                         @PathVariable(value = "nodeId") final Integer nodeId,
                                         @PathVariable(value = "nodeVersion") final Integer nodeVersion) {
        Assert.notNull(descItemVO);
        Assert.notNull(fundVersionId);
        Assert.notNull(descItemTypeId);
        Assert.notNull(nodeId);
        Assert.notNull(nodeVersion);

        ArrDescItem descItem = factoryDO.createDescItem(descItemVO, descItemTypeId);

        ArrDescItem descItemCreated = descriptionItemService.createDescriptionItem(descItem, nodeId, nodeVersion,
                fundVersionId);

        DescItemResult descItemResult = new DescItemResult();
        descItemResult.setDescItem(factoryVo.createDescItem(descItemCreated));
        descItemResult.setNode(factoryVo.createArrNode(descItemCreated.getNode()));

        return descItemResult;
    }

    /**
     * Seznam AP.
     *
     * @param fulltext     fulltext podle názvu a interního čísla AS
    * @param max            maximální počet záznamů
     * @return seznam AP
     */
    @RequestMapping(value = "/getFunds", method = RequestMethod.GET)
    public FundListCountResult getFunds(@RequestParam(value = "fulltext", required = false) final String fulltext,
                                        @RequestParam(value = "max") final Integer max) {

        List<ArrFundVO> fundList = new LinkedList<>();
        fundRepository.findByFulltext(fulltext, max).forEach(f -> {
            ArrFundVO fundVO = factoryVo.createFundVO(f.getFund(), false);
            fundVO.setVersions(Arrays.asList(factoryVo.createFundVersion(f.getOpenVersion())));
            fundList.add(fundVO);
        });

        return new FundListCountResult(fundList, fundRepository.findCountByFulltext(fulltext));
    }

    /**
     * Načtení souboru na základě id.
     * @param fundId id souboru
     * @return konkrétní AP
     */
    @RequestMapping(value = "/getFund/{fundId}", method = RequestMethod.GET)
    public ArrFundVO getFund(@PathVariable("fundId") final Integer fundId) {

        ArrFund fund = fundRepository.findOne(fundId);
        ArrFundVO fundVO = factoryVo.createFundVO(fund, true);

        return fundVO;
    }

    /**
     * Smazání celého archivního souboru. (pouze pokud neexistuje výstup (arr_named_output))
     *
     * @param fundId id archivního souboru
     * @throws DeleteFailedException Nelze smazat archivní soubor, pro který existuje alespoň jeden výstup.
     */
    @Transactional
    @RequestMapping(value = "/deleteFund/{fundId}", method = RequestMethod.DELETE)
    public void deleteFund(@PathVariable("fundId") final Integer fundId) throws DeleteFailedException {

        arrangementService.deleteFund(fundId);
    }


    /**
     * Načte AS pro dané verze.
     *
     * @param idsParam id verzí
     * @return seznam AS, každá obsahuje pouze jednu verzi, jinak je vrácená víckrát
     */
    @RequestMapping(value = "/getVersions", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<ArrFundVO> getFundsByVersionIds(@RequestBody final IdsParam idsParam) {

        if (CollectionUtils.isEmpty(idsParam.getIds())) {
            return Collections.EMPTY_LIST;
        }

        List<ArrFundVersion> versions = fundVersionRepository.findAll(idsParam.getIds());

        List<ArrFundVO> result = new LinkedList<>();
        for (ArrFundVersion version : versions) {
            ArrFundVO fund = factoryVo.createFundVO(version.getFund(), false);
            ArrFundVersionVO versionVo = factoryVo.createFundVersion(version);
            fund.setVersions(Arrays.asList(versionVo));

            result.add(fund);
        }

        return result;
    }


    /**
     * Provede načtení stromu uzlů. Uzly mohou být rozbaleny.
     *
     * @param input vstupní data pro načtení
     * @return data stromu
     */
    @RequestMapping(value = "/fundTree", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public TreeData getFundTree(final @RequestBody FaTreeParam input) {
        Assert.notNull(input);
        Assert.notNull(input.getVersionId());

        return levelTreeCacheService
                .getFaTree(input.getVersionId(), input.getNodeId(), input.getExpandedIds(), input.getIncludeIds());
    }

    /**
     * Provede načtení požadovaných uzlů ze stromu.
     *
     * @param input vstupní data pro načtení
     * @return data stromu
     */
    @RequestMapping(value = "/fundTree/nodes", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Collection<TreeNodeClient> getFundTreeNodes(final @RequestBody FaTreeNodesParam input) {
        Assert.notNull(input);
        Assert.notNull(input.getVersionId());
        Assert.notNull(input.getNodeIds());

        return levelTreeCacheService.getFaTreeNodes(input.getVersionId(), input.getNodeIds());
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
    public Collection<TreeNodeClient> getNodeParents(@RequestParam(value = "nodeId") final Integer nodeId,
                                               @RequestParam(value = "versionId") final Integer versionId) {
        Assert.notNull(nodeId);
        Assert.notNull(versionId);

        return levelTreeCacheService.getNodeParents(nodeId, versionId);
    }

    /**
     * Uzavře otevřenou verzi archivní pomůcky a otevře novou verzi.
     *
     * @param versionId         verze, která se má uzavřít
     * @param dateRange         vysčítaná informace o časovém rozsahu fondu
     * @return nová verze archivní pomůcky
     * @throws ConcurrentUpdateException chyba při současné manipulaci s položkou více uživateli
     */
    @Transactional
    @RequestMapping(value = "/approveVersion", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ArrFundVersionVO approveVersion(@RequestParam("versionId") final Integer versionId,
                                           @RequestParam(value = "dateRange", required = false) final String dateRange) {
        Assert.notNull(versionId);

        ArrFundVersion version = fundVersionRepository.findOne(versionId);

        Assert.notNull(version, "Nebyla nalezena verze s id " + versionId);

        ArrFundVersion nextVersion = arrangementService.approveVersion(version, dateRange);
        return factoryVo.createFundVersion(nextVersion);
    }

    /**
     * Získání dat pro formulář.
     *
     * @param nodeId    identfikátor JP
     * @param versionId id verze stromu
     * @return formulář
     */
    @RequestMapping(value = "/nodes/{nodeId}/{versionId}/form", method = RequestMethod.GET)
    public NodeFormDataNewVO getNodeFormData(@PathVariable(value = "nodeId") Integer nodeId,
                                             @PathVariable(value = "versionId") Integer versionId) {
        Assert.notNull(versionId, "Identifikátor verze musí být vyplněn");
        Assert.notNull(nodeId, "Identifikátor uzlu musí být vyplněn");

        ArrFundVersion version = fundVersionRepository.findOne(versionId);
        ArrNode node = nodeRepository.findOne(nodeId);

        Assert.notNull(version, "Verze AP neexistuje");
        Assert.notNull(node, "Uzel neexistuje");

        List<ArrDescItem> descItems = arrangementService.getDescItems(version, node);
        List<RulDescItemTypeExt> descItemTypes;
        try {
            descItemTypes = ruleService.getDescriptionItemTypes(versionId, nodeId);
        } catch (Exception e) {
            descItemTypes = new ArrayList<>();
        }

        Integer fundId = version.getFund().getFundId();
        String ruleCode = version.getRuleSet().getCode();

        ArrNodeVO nodeVO = factoryVo.createArrNode(node);
        List<DescItemGroupVO> descItemGroupsVO = factoryVo.createDescItemGroupsNew(ruleCode, fundId, descItems);
        List<DescItemTypeGroupVO> descItemTypeGroupsVO = factoryVo
                .createDescItemTypeGroupsNew(ruleCode, fundId, descItemTypes);
        return new NodeFormDataNewVO(nodeVO, descItemGroupsVO, descItemTypeGroupsVO);
    }

    /**
     * Získání dat pro formuláře.
     * @param nodeIds   identfikátory JP
     * @param versionId id verze stromu
     * @return formuláře
     */
    @RequestMapping(value = "/nodes/{versionId}/forms", method = RequestMethod.GET)
    public NodeFormsDataVO getNodeFormsData(@RequestParam(value = "nodeIds") Integer[] nodeIds,
                                            @PathVariable(value = "versionId") Integer versionId) {
        Assert.notNull(versionId, "Identifikátor verze musí být vyplněn");
        Assert.notNull(nodeIds, "Identifikátory uzlů musí být vyplněny");

        Map<Integer, NodeFormDataNewVO> forms = new HashMap<>();

        for (int i = 0; i < nodeIds.length; i++) {
            forms.put(nodeIds[i], getNodeFormData(nodeIds[i], versionId));
        }

        return new NodeFormsDataVO(forms);
    }

    /**
     * Získání dat formuláře pro JP a jeho okolí.
     *
     * @param nodeId    identfikátory JP
     * @param versionId id verze stromu
     * @param around    velikost okolí - počet před a za uvedeným uzlem
     * @return formuláře
     */
    @RequestMapping(value = "/nodes/{versionId}/{nodeId}/{around}/forms", method = RequestMethod.GET)
    public NodeFormsDataVO getNodeWithAroundFormsData(@PathVariable(value = "versionId") final Integer versionId,
                                                      @PathVariable(value = "nodeId") final Integer nodeId,
                                                      @PathVariable(value = "around") final Integer around) {
        Assert.notNull(versionId, "Identifikátor verze musí být vyplněn");
        Assert.notNull(nodeId, "Identifikátor uzlu musí být vyplněn");
        Assert.notNull(around, "Velikost okolí musí být vyplněno");

        ArrFundVersion version = fundVersionRepository.findOne(versionId);
        ArrNode node = nodeRepository.findOne(nodeId);

        Assert.notNull(version, "Verze AP neexistuje");
        Assert.notNull(node, "Uzel neexistuje");

        List<ArrNode> nodes = arrangementService.findSiblingsAroundOfNode(version, node, around);

        Map<Integer, NodeFormDataNewVO> forms = new HashMap<>();

        for (ArrNode arrNode : nodes) {
            forms.put(arrNode.getNodeId(), getNodeFormData(arrNode.getNodeId(), versionId));
        }

        return new NodeFormsDataVO(forms);
    }

    /**
     * Načte číselník typů kalendářů.
     * @return typy kalendářů
     */
    @RequestMapping(value = "/calendarTypes", method = RequestMethod.GET)
    public List<ArrCalendarTypeVO> getCalendarTypes() {
        List<ArrCalendarType> calendarTypes = calendarTypeRepository.findAll();
        return factoryVo.createCalendarTypes(calendarTypes);
    }

    /**
     * Vytvoří novou archivní pomůcku se zadaným názvem. Jako datum založení vyplní aktuální datum a čas.
     *
     * @param name              název archivní pomůcky
     * @param ruleSetId         id pravidel podle kterých se vytváří popis
     * @param dateRange         vysčítaná informace o časovém rozsahu fondu
     * @param internalCode      interní kód
     * @param institutionId     id instituce
     * @return nová archivní pomůcka
     */
    @Transactional
    @RequestMapping(value = "/funds", method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ArrFundVO createFund(@RequestParam(value = "name") final String name,
                                @RequestParam(value = "ruleSetId") final Integer ruleSetId,
                                @RequestParam(value = "internalCode") final String internalCode,
                                @RequestParam(value = "institutionId") final Integer institutionId,
                                @RequestParam(value = "dateRange", required = false) final String dateRange) {

        Assert.hasText(name);
        Assert.hasText(internalCode);
        Assert.notNull(institutionId);
        Assert.notNull(ruleSetId);

        RulRuleSet ruleSet = ruleSetRepository.findOne(ruleSetId);
        Assert.notNull(ruleSet, "Nebyla nalezena pravidla tvorby s id " + ruleSetId);

        ParInstitution institution = institutionRepository.findOne(institutionId);
        Assert.notNull(institution, "Nebyla nalezena instituce s id " + institutionId);

        ArrFund newFund = arrangementService
                .createFundWithScenario(name, ruleSet, internalCode, institution, dateRange);

        return factoryVo.createFundVO(newFund, true);
    }

    /**
     * Úprava archivní pomůcky
     * @param ruleSetId id pravidel, která budou nastavena otevřené verzi
     * @param arrFundVO Archivní pomůcka k úpravě
     * @return
     */
    @Transactional
    @RequestMapping(value = "/updateFund", method = RequestMethod.POST)
    public ArrFundVO updateFund(@RequestParam("ruleSetId") final Integer ruleSetId,
            @RequestBody ArrFundVO arrFundVO) {
        Assert.notNull(arrFundVO);

        return factoryVo.createFundVO(
                arrangementService.updateFund(
                        factoryDO.createFund(arrFundVO),
                        ruleSetRepository.findOne(ruleSetId),
                        factoryDO.createScopeList(arrFundVO.getRegScopes()
                        )
                ),
                false
        );
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


        ArrFundVersion version = fundVersionRepository.findOne(moveParam.getVersionId());

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


        ArrFundVersion version = fundVersionRepository.findOne(moveParam.getVersionId());

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

        ArrFundVersion version = fundVersionRepository.findOne(moveParam.getVersionId());

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
    @RequestMapping(value = "/scenarios", method = RequestMethod.POST)
    public List<ScenarioOfNewLevelVO> getDescriptionItemTypesForNewLevel(
            @RequestParam(required = false, value = "withGroups") final Boolean withGroups,
            @RequestBody final DescriptionItemParam param) {

        ArrFundVersion version = fundVersionRepository.findOne(param.getVersionId());
        Assert.notNull(version, "Neplatná verze AP");

        Integer fundId = version.getFund().getFundId();
        String ruleCode = version.getRuleSet().getCode();

        return factoryVo.createScenarioOfNewLevelList(descriptionItemService
                .getDescriptionItemTypesForNewLevel(param.getNode().getId(), param.getDirection(),
                        param.getVersionId()), withGroups, ruleCode, fundId);
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

        ArrFundVersion version = fundVersionRepository.findOne(addLevelParam.getVersionId());

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

        Collection<TreeNodeClient> nodeClients = levelTreeCacheService
                .getNodesByIds(Arrays.asList(newLevel.getNodeParent().getNodeId()), version.getFundVersionId());
        Assert.notEmpty(nodeClients);
        return new NodeWithParent(factoryVo.createArrNode(newLevel.getNode()), nodeClients.iterator().next());
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

        ArrFundVersion version = fundVersionRepository.findOne(nodeParam.getVersionId());

        ArrLevel deleteLevel = moveLevelService.deleteLevel(version, deleteNode, deleteParent);

        Collection<TreeNodeClient> nodeClients = levelTreeCacheService
                .getNodesByIds(Arrays.asList(deleteLevel.getNodeParent().getNodeId()),
                        version.getFundVersionId());
        Assert.notEmpty(nodeClients);
        return new NodeWithParent(factoryVo.createArrNode(deleteLevel.getNode()), nodeClients.iterator().next());
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

        ArrFundVersion version = fundVersionRepository.getOneCheckExist(versionId);
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

        ArrFundVersion version = fundVersionRepository.getOne(input.getVersionId());

        Set<Integer> nodeIds = arrangementService.findNodeIdsByFulltext(version, input.getNodeId(),
                input.getSearchValue(), input.getDepth());

        return arrangementService.createTreeNodeFulltextList(nodeIds, version);
    }

    /**
     * Vyhledání vazeb AP - rejstříky.
     *
     * @param versionId id verze stromu
     * @param nodeId    identfikátor JP
     * @return vazby
     */
    @RequestMapping(value = "/registerLinks/{nodeId}/{versionId}",
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ArrNodeRegisterVO> findRegisterLinks(final @PathVariable(value = "versionId") Integer versionId,
                                                     final @PathVariable(value = "nodeId") Integer nodeId) {
        List<ArrNodeRegister> registerLinks = registryService.findRegisterLinks(versionId, nodeId);
        return factoryVo.createRegisterLinkList(registerLinks);
    }

    /**
     * Vyhledání vazeb AP - rejstříky pro formulář.
     *
     * @param versionId id verze stromu
     * @param nodeId    identfikátor JP
     * @return vazby pro formulář
     */
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

    /**
     * Vytvoření vazby AP - rejstříky
     *
     * @param versionId         id verze stromu
     * @param nodeId            identfikátor JP
     * @param nodeRegisterVO    vazba
     * @return vazba
     */
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

    /**
     * Upravení vazby AP - rejstříky.
     *
     * @param versionId         id verze stromu
     * @param nodeId            identfikátor JP
     * @param nodeRegisterVO    vazba
     * @return vazba
     */
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

    /**
     * Smazání vazby AP - rejstříky.
     *
     * @param versionId         id verze stromu
     * @param nodeId            identfikátor JP
     * @param nodeRegisterVO    vazba
     * @return  vazba
     */
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
    @RequestMapping(value = "/validateVersion/{versionId}/{showAll}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<VersionValidationItem> validateVersion(@PathVariable("versionId") final Integer versionId,
                                                       @PathVariable("showAll") final Boolean showAll) {
        Assert.notNull(versionId);
        Assert.notNull(showAll);

        ArrFundVersion fundVersion = fundVersionRepository.findOne(versionId);
        if (fundVersion == null) {
            throw new IllegalStateException("Neexistuje verze archivní pomůcky s id " + versionId);
        }

        List<ArrNodeConformity> validationErrors = arrangementService.findConformityErrors(fundVersion, showAll);

        return arrangementService.createVersionValidationItems(validationErrors, fundVersion);
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

        ArrFundVersion fundVersion = fundVersionRepository.findOne(versionId);
        if (fundVersion == null) {
            throw new IllegalStateException("Neexistuje verze archivní pomůcky s id " + versionId);
        }

        return arrangementService.getVersionErrorCount(fundVersion);
    }


    /**
     * Provede filtraci uzlů podle filtru a uloží filtrované id do session.
     *
     * @param versionId id verze
     * @param filters filtry
     *
     * @return počet všech záznamů splňujících filtry
     */
    @RequestMapping(value = "/filterNodes/{versionId}", method = RequestMethod.GET)
    public Integer filterNodes(@PathVariable("versionId") final Integer versionId,
            @RequestBody(required = false) final Filters filters) {
        ArrFundVersion version = fundVersionRepository.getOneCheckExist(versionId);
        List<DescItemTypeFilter> descItemFilters = factoryDO.createFilters(filters);
        return filterTreeService.filterData(version, descItemFilters);
    }

    /**
     * Do filtrovaného seznamu načte hodnoty atributů a vrátí podstránku záznamů.
     *
     * @param versionId       id verze
     * @param page            číslo stránky, od 0
     * @param pageSize        velikost stránky
     * @param descItemTypeIds id typů atributů, které chceme načíst
     * @return mapa hodnot atributů nodeId -> descItemId -> value
     */
    @RequestMapping(value = "/getFilterNodes/{versionId}", method = RequestMethod.PUT)
    public List<FilterNode> getFilteredNodes(@PathVariable("versionId") final Integer versionId,
                                             @RequestParam("page") final Integer page,
                                             @RequestParam("pageSize") final Integer pageSize,
                                             @RequestBody final Set<Integer> descItemTypeIds)
            throws FilterExpiredException {

        ArrFundVersion version = fundVersionRepository.getOneCheckExist(versionId);

        return filterTreeService.getFilteredData(version, page, pageSize, descItemTypeIds);
    }


    /**
     * Ve filtrovaném seznamu najde uzly podle fulltextu. Vrací seřazený seznam uzlů podle jejich indexu v seznamu
     * všech
     * filtrovaných uzlů.
     *
     * @param versionId id verze stromu
     * @param fulltext  fulltext
     * @return seznam uzlů a jejich indexu v seznamu filtrovaných uzlů, seřazené podle indexu
     * @throws FilterExpiredException není nastaven filtr, nejprve zavolat {@link FilterTreeService#filterData(ArrFundVersion,
     *                                Object)}
     */
    @RequestMapping(value = "/getFilteredFulltext/{versionId}", method = RequestMethod.GET)
    public List<FilterNodePosition> getFilteredFulltextNodes(@PathVariable("versionId") final Integer versionId,
                                                             @RequestParam("fulltext") final String fulltext) {
        ArrFundVersion version = fundVersionRepository.getOneCheckExist(versionId);
        return filterTreeService.getFilteredFulltextIds(version, fulltext);
    }

    /**
     * Získání unikátních hodnot atributů podle typu.
     *
     * @param versionId      verze stromu
     * @param descItemTypeId id typu atributu
     * @param fulltext       fultextové hledání
     * @param max            maximální počet záznamů
     * @param specIds        id specifikací / id typů atributů
     * @return seznam unikátních hodnot
     */
    @RequestMapping(value = "/filterUniqueValues/{versionId}", method = RequestMethod.PUT)
    public List<String> filterUniqueValues(@PathVariable("versionId") final Integer versionId,
                                           @RequestParam("descItemTypeId") final Integer descItemTypeId,
                                           @RequestParam(value = "fulltext", required = false) final String fulltext,
                                           @RequestParam(value = "max", required = true) final Integer max,
                                           @RequestBody(required = false) final Set<Integer> specIds) {

        ArrFundVersion version = fundVersionRepository.getOneCheckExist(versionId);
        RulDescItemType descItemType = descItemTypeRepository.findOne(descItemTypeId);


        return filterTreeService.filterUniqueValues(version, descItemType, specIds, fulltext, max);
    }


    /**
     * Nahrazení textu v hodnotách textových atributů.
     * @param versionId id verze stromu
     * @param descItemTypeId typ atributu
     * @param searchText hledaný text v atributu
     * @param replaceText text, který nahradí hledaný text v celém textu
     * @param replaceDataBody seznam uzlů, ve kterých hledáme a seznam specifikací
     */
    @Transactional
    @RequestMapping(value = "/replaceDataValues/{versionId}", method = RequestMethod.PUT)
    public void replaceDataValues(@PathVariable("versionId") final Integer versionId,
                                  @RequestParam("descItemTypeId") final Integer descItemTypeId,
                                  @RequestParam("searchText") final String searchText,
                                  @RequestParam("replaceText") final String replaceText,
                                  @RequestBody final ReplaceDataBody replaceDataBody) {

        ArrFundVersion version = fundVersionRepository.getOneCheckExist(versionId);
        RulDescItemType descItemType = descItemTypeRepository.findOne(descItemTypeId);

        Set<ArrNode> nodesDO = new HashSet<>(factoryDO.createNodes(replaceDataBody.getNodes()));

        Set<RulDescItemSpec> specifications =
                CollectionUtils.isEmpty(replaceDataBody.getSpecIds()) ? null :
                new HashSet<>(descItemSpecRepository.findAll(replaceDataBody.getSpecIds()));

        descriptionItemService.replaceDescItemValues(version, descItemType, nodesDO, specifications, searchText, replaceText);
    }

    /**
     * Nastavení textu hodnotám atributu..
     *
     * @param versionId         id verze stromu
     * @param descItemTypeId    typ atributu
     * @param newDescItemSpecId pokud se jedná o atribut se specifikací -> id specifikace, která bude nastavena
     * @param text              text, který nahradí hledaný text v celém textu
     * @param replaceDataBody   seznam uzlů, ve kterých hledáme a seznam specifikací
     */
    @Transactional
    @RequestMapping(value = "/placeDataValues/{versionId}", method = RequestMethod.PUT)
    public void placeDataValues(@PathVariable("versionId") final Integer versionId,
                                @RequestParam("descItemTypeId") final Integer descItemTypeId,
                                @RequestParam(value = "newDescItemSpecId", required = false) final Integer newDescItemSpecId,
                                @RequestParam("text") final String text,
                                @RequestBody final ReplaceDataBody replaceDataBody) {
        ArrFundVersion version = fundVersionRepository.getOneCheckExist(versionId);
        RulDescItemType descItemType = descItemTypeRepository.findOne(descItemTypeId);

        Set<ArrNode> nodesDO = new HashSet<>(factoryDO.createNodes(replaceDataBody.getNodes()));

        RulDescItemSpec newDescItemSpec = newDescItemSpecId == null ? null
                                                                    : descItemSpecRepository.findOne(newDescItemSpecId);
        Set<RulDescItemSpec> specifications =
                CollectionUtils.isEmpty(replaceDataBody.getSpecIds()) ? null :
                new HashSet<>(descItemSpecRepository.findAll(replaceDataBody.getSpecIds()));

        descriptionItemService
                .placeDescItemValues(version, descItemType, nodesDO, newDescItemSpec, specifications, text);
    }


    /**
     * Smazání hodnot atributů daného typu pro vybrané uzly.
     *
     * @param versionId         id verze stromu
     * @param descItemTypeId    typ atributu
     * @param replaceDataBody   seznam uzlů, ve kterých hledáme a seznam specifikací
     */
    @Transactional
    @RequestMapping(value = "/deleteDataValues/{versionId}", method = RequestMethod.PUT)
    public void deleteDataValues(@PathVariable("versionId") final Integer versionId,
                                 @RequestParam("descItemTypeId") final Integer descItemTypeId,
                                 @RequestBody final ReplaceDataBody replaceDataBody) {
        ArrFundVersion version = fundVersionRepository.getOneCheckExist(versionId);
        RulDescItemType descItemType = descItemTypeRepository.findOne(descItemTypeId);

        Set<ArrNode> nodesDO = new HashSet<>(factoryDO.createNodes(replaceDataBody.getNodes()));

        Set<RulDescItemSpec> specifications =
                CollectionUtils.isEmpty(replaceDataBody.getSpecIds()) ? null :
                new HashSet<>(descItemSpecRepository.findAll(replaceDataBody.getSpecIds()));

        descriptionItemService.deleteDescItemValues(version, descItemType, nodesDO, specifications);
    }

    @RequestMapping(value = "/validation/{fundVersionId}/{fromIndex}/{toIndex}", method = RequestMethod.GET)
    public ValidationItems getValidation(@PathVariable("fundVersionId") final Integer fundVersionId,
                                                 @PathVariable(value = "fromIndex") final Integer fromIndex,
                                                 @PathVariable(value = "toIndex") final Integer toIndex) {
        ArrFundVersion version = fundVersionRepository.getOneCheckExist(fundVersionId);
        return arrangementService.getValidationNodes(version, fromIndex, toIndex);
    }

    @RequestMapping(value = "/validation/{fundVersionId}/find/{nodeId}/{direction}", method = RequestMethod.GET)
    public ValidationItems findValidationError(@PathVariable("fundVersionId") final Integer fundVersionId,
                                               @PathVariable(value = "nodeId") final Integer nodeId,
                                               @PathVariable(value = "direction") final Integer direction) {
        ArrFundVersion version = fundVersionRepository.getOneCheckExist(fundVersionId);
        return arrangementService.findErrorNode(version, nodeId, direction);
    }

    @RequestMapping(value = "/fund/policy/{fundVersionId}", method = RequestMethod.GET)
    public List<NodeItemWithParent> getAllNodesVisiblePolicy(@PathVariable(value = "fundVersionId") final Integer fundVersionId) {
        ArrFundVersion version = fundVersionRepository.getOneCheckExist(fundVersionId);
        return policyService.getTreePolicy(version);
    }

    /**
     * Výstupní objekt pro chybové jednotky popisu.
     */
    public static class ValidationItems {

        /**
         * JP s chybou.
         */
        private List<NodeItemWithParent> items;

        /**
         * Celkový počet chyb v AS.
         */
        private Integer count;

        public ValidationItems() {
        }

        public ValidationItems(final List<NodeItemWithParent> items, final Integer count) {
            this.items = items;
            this.count = count;
        }

        public List<NodeItemWithParent> getItems() {
            return items;
        }

        public void setItems(final List<NodeItemWithParent> items) {
            this.items = items;
        }

        public Integer getCount() {
            return count;
        }

        public void setCount(final Integer count) {
            this.count = count;
        }
    }

    public static class VersionValidationItem {

        private int nodeId;

        private String description;

        private TreeNodeClient parent;

        public int getNodeId() {
            return nodeId;
        }

        public void setNodeId(int nodeId) {
            this.nodeId = nodeId;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public TreeNodeClient getParent() {
            return parent;
        }

        public void setParent(TreeNodeClient parent) {
            this.parent = parent;
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

        public NodeRegisterDataVO() {
        }

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
     * Výstupní objekt pro získaná data pro formuláře detailu uzlu.
     */
    public static class NodeFormsDataVO {

        /**
         * Formuláře
         */
        private Map<Integer, NodeFormDataNewVO> forms;

        public NodeFormsDataVO() {
        }

        public NodeFormsDataVO(final Map<Integer, NodeFormDataNewVO> forms) {
            this.forms = forms;
        }

        public Map<Integer, NodeFormDataNewVO> getForms() {
            return forms;
        }

        public void setForms(final Map<Integer, NodeFormDataNewVO> forms) {
            this.forms = forms;
        }
    }

    /**
     * Výstupní objekt pro získaná data pro formulář detailu uzlu.
     */
    public static class NodeFormDataNewVO {

        /**
         * Uzel
         */
        private ArrNodeVO node;

        /**
         * Seznam skupin
         */
        private List<DescItemGroupVO> groups;

        /**
         * Seznam skupin typů hodnot archivní pomůcky
         */
        private List<DescItemTypeGroupVO> typeGroups;

        public NodeFormDataNewVO() {

        }

        public NodeFormDataNewVO(final ArrNodeVO node,
                              final List<DescItemGroupVO> groups,
                              final List<DescItemTypeGroupVO> typeGroups) {
            this.node = node;
            this.groups = groups;
            this.typeGroups = typeGroups;
        }

        public ArrNodeVO getNode() {
            return node;
        }

        public void setNode(final ArrNodeVO node) {
            this.node = node;
        }

        public List<DescItemGroupVO> getGroups() {
            return groups;
        }

        public void setGroups(final List<DescItemGroupVO> groups) {
            this.groups = groups;
        }

        public List<DescItemTypeGroupVO> getTypeGroups() {
            return typeGroups;
        }

        public void setTypeGroups(final List<DescItemTypeGroupVO> typeGroups) {
            this.typeGroups = typeGroups;
        }
    }

    /**
     * Vstupní parametry změnu stavu obalů.
     */
    public static class PacketSetStateParam extends PacketDeleteParam {

        /**
         * Stav obalu
         */
        private ArrPacket.State state;

        public ArrPacket.State getState() {
            return state;
        }

        public void setState(final ArrPacket.State state) {
            this.state = state;
        }
    }

    /**
     * Vstupní parametry pro smazání obalů.
     */
    public static class PacketDeleteParam {

        /**
         * Seznam id obalů
         */
        private Integer[] packetIds;

        public Integer[] getPacketIds() {
            return packetIds;
        }

        public void setPacketIds(final Integer[] packetIds) {
            this.packetIds = packetIds;
        }
    }

    /**
     * Vstupní parametry pro vyhledání obalů ve formuláři.
     */
    public static class PacketFindFormParam {

        /**
         * Maximální počet výsledků
         */
        private Integer limit;

        /**
         * Vyhledávaný text - může být null
         */
        private String text;

        public Integer getLimit() {
            return limit;
        }

        public void setLimit(final Integer limit) {
            this.limit = limit;
        }

        public String getText() {
            return text;
        }

        public void setText(final String text) {
            this.text = text;
        }
    }

    /**
     * Vstupní parametry pro vyhledávání - ve správě obalů.
     */
    public static class PacketFindParam {

        /**
         * Prefix pro vyhledávání
         */
        private String prefix;

        /**
         * Stav obalu
         */
        private ArrPacket.State state;

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(final String prefix) {
            this.prefix = prefix;
        }

        public ArrPacket.State getState() {
            return state;
        }

        public void setState(final ArrPacket.State state) {
            this.state = state;
        }
    }

    /**
     * Vstupní parametry pro generování/přegenoravání packetů.
     */
    public static class PacketGenerateParam {

        /**
         * Požadovaný prefix
         */
        private String prefix;

        /**
         * Identifikátor typu obalu
         */
        private Integer packetTypeId;

        /**
         * Od čísla, od kterého se má začít generovat
         */
        private Integer fromNumber;

        /**
         * počet cifer (kvůli přidaným nulám)
         */
        private Integer lenNumber;

        /**
         * Počet obalů, které se mají vygenerovat
         */
        private Integer count;

        /**
         * Seznam identifikátorů packetů, které se mají přegenerovat
         */
        private Integer[] packetIds;

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(final String prefix) {
            this.prefix = prefix;
        }

        public Integer getPacketTypeId() {
            return packetTypeId;
        }

        public void setPacketTypeId(final Integer packetTypeId) {
            this.packetTypeId = packetTypeId;
        }

        public Integer getFromNumber() {
            return fromNumber;
        }

        public void setFromNumber(final Integer fromNumber) {
            this.fromNumber = fromNumber;
        }

        public Integer getLenNumber() {
            return lenNumber;
        }

        public void setLenNumber(final Integer lenNumber) {
            this.lenNumber = lenNumber;
        }

        public Integer getCount() {
            return count;
        }

        public void setCount(final Integer count) {
            this.count = count;
        }

        public Integer[] getPacketIds() {
            return packetIds;
        }

        public void setPacketIds(final Integer[] packetIds) {
            this.packetIds = packetIds;
        }
    }

    /**
     * Vstupní parametry pro metodu /faTree {@link #getFundTree(FaTreeParam)}.
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
     * Vstupní parametry pro metodu /faTree/nodes {@link #getFundTreeNodes(FaTreeNodesParam)}.
     */
    public static class FaTreeNodesParam {

        /**
         * Id verze.
         */
        private Integer versionId;

        /**
         * Seznam požadovaných uzlů.
         */
        private List<Integer> nodeIds;

        public Integer getVersionId() {
            return versionId;
        }

        public void setVersionId(final Integer versionId) {
            this.versionId = versionId;
        }

        public List<Integer> getNodeIds() {
            return nodeIds;
        }

        public void setNodeIds(final List<Integer> nodeIds) {
            this.nodeIds = nodeIds;
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

        public CopySiblingResult() {
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
        private TreeNodeClient parentNode;

        public ArrNodeVO getNode() {
            return node;
        }

        public void setNode(final ArrNodeVO node) {
            this.node = node;
        }

        public TreeNodeClient getParentNode() {
            return parentNode;
        }

        public void setParentNode(final TreeNodeClient parentNode) {
            this.parentNode = parentNode;
        }

        public NodeWithParent() {
        }

        public NodeWithParent(final ArrNodeVO node, final TreeNodeClient parentNode) {
            this.node = node;
            this.parentNode = parentNode;
        }
    }

    public static class ReplaceDataBody {

        private Set<ArrNodeVO> nodes;
        private Set<Integer> specIds;

        public Set<ArrNodeVO> getNodes() {
            return nodes;
        }

        public void setNodes(final Set<ArrNodeVO> nodes) {
            this.nodes = nodes;
        }

        public Set<Integer> getSpecIds() {
            return specIds;
        }

        public void setSpecIds(final Set<Integer> specIds) {
            this.specIds = specIds;
        }
    }
}
