package cz.tacr.elza.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.util.List;

import org.junit.jupiter.api.Test;

import cz.tacr.elza.controller.vo.TreeNodeCustomGen;
import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaDao;
import cz.tacr.elza.domain.DaLevelView;
import cz.tacr.elza.repository.AipRepository;

/**
 * Logical structure of the selected packages. Level views are shared by all packages of the
 * fund (one view per label), so a view of a selected package may have child views that only
 * other packages have - those are not part of the selection's tree.
 */
class AipServiceLogicalTreeTest {

    private final AipRepository aipRepository = mock(AipRepository.class);
    private final DaoService daoService = mock(DaoService.class);
    private final AipService service = new AipService();

    AipServiceLogicalTreeTest() {
        setField(service, "aipRepository", aipRepository);
        setField(service, "daoService", daoService);
    }

    private static DaAip aip(int id) {
        DaAip aip = new DaAip();
        aip.setAipId(id);
        return aip;
    }

    private static DaLevelView view(int id, String label, DaLevelView parent) {
        DaLevelView view = new DaLevelView();
        view.setLevelViewId(id);
        view.setLabel(label);
        view.setParentLevelView(parent);
        return view;
    }

    private static DaDao logical(DaAip aip, DaLevelView view) {
        DaDao dao = new DaDao();
        dao.setAip(aip);
        dao.setType(DaDao.DaoType.LOGICAL);
        dao.setLevelView(view);
        return dao;
    }

    @Test
    void childViewsOfOtherPackagesAreLeftOut() {
        DaAip selected = aip(127);
        DaLevelView group = view(10, "vecnaskp:PRO VŠECHNY OBLASTI", null);
        DaLevelView ownChild = view(11, "spis:ZAD", group);
        DaLevelView foreignChild = view(12, "spis:jiného balíčku", group);
        group.setChildren(List.of(ownChild, foreignChild));
        // the root part of the structure lies outside the level views
        DaDao rootPart = logical(selected, null);

        when(daoService.getDaosByTypeAndAipIn(anyList(), eq(DaDao.DaoType.LOGICAL)))
                .thenReturn(List.of(logical(selected, group), logical(selected, ownChild), rootPart));
        when(aipRepository.findAllById(anyList())).thenReturn(List.of(selected));

        List<TreeNodeCustomGen> nodes = service.getAipsLogicalTree(List.of(127)).getNodes();

        assertEquals(List.of("Logická struktura", "vecnaskp:PRO VŠECHNY OBLASTI", "spis:ZAD"),
                     nodes.stream().map(TreeNodeCustomGen::getName).toList());
        assertEquals(List.of(127), nodes.get(2).getValue());
    }
}
