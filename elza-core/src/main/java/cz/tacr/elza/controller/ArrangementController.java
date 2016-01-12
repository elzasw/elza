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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.ArrFindingAidVO;
import cz.tacr.elza.controller.vo.TreeData;
import cz.tacr.elza.domain.ArrFindingAid;
import cz.tacr.elza.repository.FindingAidVersionRepository;
import cz.tacr.elza.service.LevelTreeCacheService;


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
