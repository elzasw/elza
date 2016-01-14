package cz.tacr.elza.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.transaction.Transactional;

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
import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.ArrCalendarTypeVO;
import cz.tacr.elza.controller.vo.ArrFindingAidVO;
import cz.tacr.elza.controller.vo.ArrFindingAidVersionVO;
import cz.tacr.elza.controller.vo.TreeData;
import cz.tacr.elza.controller.vo.TreeNodeClient;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeExtVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrDescItemGroupVO;
import cz.tacr.elza.domain.ArrCalendarType;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFindingAid;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulArrangementType;
import cz.tacr.elza.domain.RulDescItemTypeExt;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.repository.ArrangementTypeRepository;
import cz.tacr.elza.repository.CalendarTypeRepository;
import cz.tacr.elza.repository.FindingAidVersionRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.RuleSetRepository;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.LevelTreeCacheService;
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
    private ClientFactoryVO factoryVo;


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
        List<RulDescItemTypeExtVO> descItemTypesVO = factoryVo.createDescItemTypes(descItemTypes);
        return new NodeFormDataVO(nodeVO, descItemGroupsVO, descItemTypesVO);
    }

    @RequestMapping(value = "/calendarTypes", method = RequestMethod.GET)
    public List<ArrCalendarTypeVO> getCalendarTypes() {
        List<ArrCalendarType> calendarTypes = calendarTypeRepository.findAll();
        return factoryVo.createCalendarTypes(calendarTypes);
    }

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
         * Seznam typů hodnot archivní pomůcky
         */
        private List<RulDescItemTypeExtVO> descItemTypes;

        public NodeFormDataVO() {

        }

        public NodeFormDataVO(final ArrNodeVO node,
                              final List<ArrDescItemGroupVO> descItemGroups,
                              final List<RulDescItemTypeExtVO> descItemTypes) {
            this.node = node;
            this.descItemGroups = descItemGroups;
            this.descItemTypes = descItemTypes;
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

        public List<RulDescItemTypeExtVO> getDescItemTypes() {
            return descItemTypes;
        }

        public void setDescItemTypes(final List<RulDescItemTypeExtVO> descItemTypes) {
            this.descItemTypes = descItemTypes;
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
}
