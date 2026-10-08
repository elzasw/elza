package cz.tacr.elza.packageimport;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Order of the startup import: dependencies before the packages depending on them, independent packages
 * in the order they were added (the order of their codes), packages without any dependency included.
 */
public class PackageGraphTest {

    @Test
    void independentPackagesKeepTheirOrderAndDependenciesComeFirst() {
        PackageUtils.Graph<String> g = new PackageUtils.Graph<>(4);
        for (String code : List.of("CZ_BASE", "ISAAR_CPF", "SIMPLE-DEV", "ZP2015")) {
            g.addVertex(code);
        }
        g.addEdge("ZP2015", "CZ_BASE");
        g.addEdge("SIMPLE-DEV", "CZ_BASE");

        List<String> order = g.topologicalSort();
        assertEquals(4, order.size());
        assertEquals("CZ_BASE", order.get(0));
        assertEquals(List.of("ISAAR_CPF", "SIMPLE-DEV", "ZP2015"), order.subList(1, 4));
    }
}
