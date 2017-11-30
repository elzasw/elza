package cz.tacr.elza.packageimport;

import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.PackageCode;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Utils pro import balíčků.
 *
 * @since 19.09.2017
 */
public class PackageUtils {


    /**
     * Vytviření mapy streamů souborů v zipu.
     *
     * @param zipFile soubor zip
     * @param entries záznamy
     * @return mapa streamů
     */
    public static Map<String, ByteArrayInputStream> createStreamsMap(final ZipFile zipFile,
                                                                     final Enumeration<? extends ZipEntry> entries)
            throws IOException {
        Map<String, ByteArrayInputStream> mapEntry = new HashMap<>();

        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            InputStream stream = zipFile.getInputStream(entry);

            ByteArrayOutputStream fout = new ByteArrayOutputStream();

            for (int c = stream.read(); c != -1; c = stream.read()) {
                fout.write(c);
            }
            stream.close();

            mapEntry.put(entry.getName(), new ByteArrayInputStream(fout.toByteArray()));
            fout.close();
        }
        return mapEntry;
    }

    /**
     * Převod streamu na XML soubor.
     *
     * @param classObject objekt XML
     * @param xmlStream   xml stream
     * @param <T>         typ pro převod
     */
    public static <T> T convertXmlStreamToObject(final Class<T> classObject, final ByteArrayInputStream xmlStream) {
        if (xmlStream != null) {
            try {
                JAXBContext jaxbContext = JAXBContext.newInstance(classObject);
                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                return (T) unmarshaller.unmarshal(xmlStream);
            } catch (Exception e) {
                throw new SystemException("Nepodařilo se načíst objekt " + classObject.getSimpleName() + " ze streamu", e, PackageCode.PARSE_ERROR).set("class", classObject.toString());
            }
        }
        return null;
    }

    /**
     * Vyhledání adresářů pravidel ze seznamu souborů v ZIP.
     *
     * @param ruleDir název adresáře, který prohledáváme
     * @param paths   seznam všech položek v ZIP
     * @return kód pravidel -> adresář s pravidly
     */
    public static Map<String, String> findRulePaths(final String ruleDir, final Collection<String> paths) {
        Map<String, String> result = new HashMap<>();
        String regex = "^(" + ruleDir + "/([^/]+)/)(.*)$";
        Pattern pattern = Pattern.compile(regex);
        for (String path : paths) {
            Matcher matcher = pattern.matcher(path);
            if (matcher.find()) {
                String ruleCode = matcher.group(2);
                String dirRulePath = matcher.group(1);
                result.putIfAbsent(ruleCode, dirRulePath);
            }
        }
        return result;
    }

    /**
     * Reprezentace grafu pro topologické řazení.
     *
     * @see <a href="http://www.geeksforgeeks.org/topological-sorting/">Topological Sorting</a>
     */
    public static class Graph<T>
    {
        private int V;   // No. of vertices
        private LinkedList<Integer>[] adj; // Adjacency List
        private Map<T, Integer> map = new HashMap<>(); // Vertex converse map
        private Map<Integer, T> reverseMap = new HashMap<>(); // reverse map

        //Constructor
        Graph(int v)
        {
            V = v;
            adj = new LinkedList[v];
            for (int i=0; i<v; ++i) {
                adj[i] = new LinkedList<>();
            }
        }

        void addEdge(T vv, T ww) {
            Integer v = map.computeIfAbsent(vv, k -> map.size());
            Integer w = map.computeIfAbsent(ww, k -> map.size());
            if (map.size() > V) {
                throw new IllegalStateException("Graph has only " + V + " vertex");
            }
            reverseMap.put(v, vv);
            reverseMap.put(w, ww);
            adj[v].add(w);
        }

        // A recursive function used by topologicalSort
        void topologicalSortUtil(int v, boolean visited[],
                                 Stack<T> stack)
        {
            // Mark the current node as visited.
            visited[v] = true;
            Integer i;

            // Recur for all the vertices adjacent to this
            // vertex
            for (Integer integer : adj[v]) {
                i = integer;
                if (!visited[i])
                    topologicalSortUtil(i, visited, stack);
            }

            // Push current vertex to stack which stores result
            stack.push(reverseMap.get(v));
        }

        // The function to do Topological Sort. It uses
        // recursive topologicalSortUtil()
        List<T> topologicalSort()
        {
            Stack<T> stack = new Stack<>();

            // Mark all the vertices as not visited
            boolean visited[] = new boolean[V];
            for (int i = 0; i < V; i++) {
                visited[i] = false;
            }

            // Call the recursive helper function to store
            // Topological Sort starting from all vertices
            // one by one
            for (int i = 0; i < V; i++) {
                if (!visited[i]) {
                    topologicalSortUtil(i, visited, stack);
                }
            }

            List<T> result = new ArrayList<>();

            while (!stack.empty()) {
                result.add(0, stack.pop());
            }

            return result;
        }

    }

    // Testovací spuštění
    public static void main(String args[])
    {
        class K {
            int i;
            public K(int i) {
                this.i = i;
            }

            @Override
            public String toString() {
                return "" + i;
            }
        }

        Graph<K> g = new Graph<>(6);

        K ob0 = new K(0);
        K ob1 = new K(1);
        K ob2 = new K(2);
        K ob3 = new K(3);
        K ob4 = new K(4);
        K ob5 = new K(5);

        g.addEdge(ob5, ob2);
        g.addEdge(ob5, ob0);
        g.addEdge(ob4, ob0);
        g.addEdge(ob4, ob1);
        g.addEdge(ob2, ob3);
        g.addEdge(ob3, ob1);

        System.out.println("Following is a Topological " +
                "sort of the given graph");
        System.out.println(g.topologicalSort());
    }
}
