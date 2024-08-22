package cz.tacr.elza.service;

import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.*;
import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaDao;
import cz.tacr.elza.domain.DaDaoRelation;
import cz.tacr.elza.domain.DaLevelView;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.repository.AipRepository;
import cz.tacr.elza.repository.DaDaoRepository;
import cz.tacr.elza.repository.FilteredResult;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static cz.tacr.elza.exception.codes.ArrangementCode.AIP_NOT_FOUND;

@Service
public class AipService {
    @Autowired
    private AipRepository aipRepository;
    @Autowired
    private ClientFactoryVO clientFactoryVO;
    @Autowired
    private DaDaoRepository daDaoRepository;
    @Autowired
    private DaoService daoService;

    @Transactional
    public FilteredResult<DaAip> findAipDetailsByFilter( List<AipFilterGen> filters, Integer from, Integer count) {
        return aipRepository.findAipsByFilter(filters, from, count);
    }

    @Transactional
    public AipDetailVO getAipDetail(@NotNull Integer id) {
        DaAip aip = aipRepository.findById(id).orElseThrow(() -> new ObjectNotFoundException("Nenalezeno AIP s id " + id, AIP_NOT_FOUND));
        if(aip == null) {
            return null;
        }
        return clientFactoryVO.createAipDetail(aip);
    }

    @Transactional
    public DaAip getAip(@NotNull Integer id) {
        return aipRepository.findById(id).orElseThrow(() -> new ObjectNotFoundException("Nenalezeno AIP s id " + id, AIP_NOT_FOUND));
    }

    @Transactional
    public TreeDataCustomGen getAipsLogicalTree(List<Integer> aipIds) {
        TreeDataCustomGen result = new TreeDataCustomGen();
        List<DaDao> daoList = daoService.getDaosByTypeAndAipIn(aipIds, DaDao.DaoType.LOGICAL);
        Map<Integer, List<DaDao>> levelViewIdToDaosMap = daoList.stream()
                .collect(Collectors.groupingBy(
                        dao -> dao.getLevelView().getLevelViewId()
                ));
        Set<DaLevelView> allLevelViews = daoList.stream()
                .map(DaDao::getLevelView)
                .collect(Collectors.toSet());
        Set<DaLevelView> rootLevelViews = allLevelViews.stream()
                .filter(levelView -> levelView.getParentLevelView() == null)
                .collect(Collectors.toSet());
        List<DaAip> allAips = aipRepository.findAllById(aipIds);
        List<TreeNodeCustomGen> treeNodes = new ArrayList<>();

        TreeNodeCustomGen withoutRoot = createNodeCustomGen(null, "Bez logické struktury", 1, null, null, null);
        List<TreeNodeCustomGen> withoutStructure = new ArrayList<>(allAips.stream()
                .filter(aip -> daoList
                        .stream()
                        .noneMatch(
                                dao -> dao.getAip().getAipId().equals(aip.getAipId()) &&
                                        dao.getType() == DaDao.DaoType.LOGICAL && dao.getLevelView() != null)
                )
                .map(aip -> createNodeCustomGen(
                        Collections.singletonList(aip.getAipId()),
                        aip.getAipId().toString(),
                        2,
                        withoutRoot.getUUID(),
                        false,
                        null
                ))
                .toList());

        List<Integer> withoutStructureAipIds = withoutStructure.stream().map(TreeNodeCustomGen::getValue).flatMap(List::stream).toList();
        withoutRoot.setValue(withoutStructureAipIds);
        withoutRoot.setHasChildren(!withoutStructure.isEmpty());
        List<Integer> withStructure = aipIds.stream().filter(i -> !withoutStructureAipIds.contains(i)).toList();

        TreeNodeCustomGen root = createNodeCustomGen(withStructure, "Logická struktura", 1, null, null, null);

        if(!withoutStructure.isEmpty()) {
            withoutStructure.add(0, withoutRoot);
            treeNodes.addAll(withoutStructure);
        }
        for (DaLevelView view : rootLevelViews) {
            buildLogicalTree(view, levelViewIdToDaosMap, treeNodes, 2, root.getUUID(), aipIds);
        }
        if(!treeNodes.isEmpty()) {
            root.setHasChildren(true);
            treeNodes.add(0, root);
        }

        result.setNodes(treeNodes);
        result.setExpandedIdsExtension(null);

        return result;
    }

    private void buildLogicalTree(
            DaLevelView levelView,
            Map<Integer, List<DaDao>> levelViewIdToDaosMap,
            List<TreeNodeCustomGen> treeNodes,
            int depth,
            String parentUUID,
            List<Integer> aipIds
    ) {
        List<DaDao> daos = levelViewIdToDaosMap.get(levelView.getLevelViewId());
        List<DaLevelView> children = levelView.getChildren();
        List<Integer> relatedAipsIds = daos
                .stream()
                .map(d -> d.getAip().getAipId())
                .distinct()
                .filter(aipIds::contains)
                .toList();
        TreeNodeCustomGen root = createNodeCustomGen(relatedAipsIds, levelView.getLabel(), depth, parentUUID, !daos.isEmpty(), levelView.getLevelViewId());
        treeNodes.add(root);
        if (children != null) {
            for (DaLevelView child : children) {
                buildLogicalTree(child, levelViewIdToDaosMap, treeNodes, depth + 1, root.getUUID(), aipIds);
            }
        }
    }

    private TreeNodeCustomGen createNodeCustomGen(
            List<Integer> ids,
            String name,
            Integer depth,
            String parentUUID,
            Boolean hasChildren,
            Integer daLevelViewId
    ) {
        TreeNodeCustomGen node = new TreeNodeCustomGen();
        node.setUUID(UUID.randomUUID().toString());
        node.setValue(ids);
        node.setName(name);
        node.setDepth(depth);
        node.setParent(parentUUID);
        node.setHasChildren(hasChildren);
        node.setDaLeveViewId(daLevelViewId);
        return node;
    }
}
