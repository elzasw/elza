package cz.tacr.elza.packageimport;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.PackageCode;
import cz.tacr.elza.packageimport.autoimport.PackageInfoWrapper;
import cz.tacr.elza.packageimport.xml.PackageInfo;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.ValidationEventLocator;

/**
 * Utils pro import balíčků.
 *
 * @since 19.09.2017
 */
public class PackageUtils {

    /**
     * Hash souboru.
     *
     * @param file hashovaný soubor
     * @return hash
     */
    public static String sha256File(final File file) {
        byte[] buffer = new byte[8192];
        int count;
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            while ((count = bis.read(buffer)) > 0) {
                digest.update(buffer, 0, count);
            }
            byte[] hash = digest.digest();
            return Hex.encodeHexString(hash);
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new SystemException("Nastal problém při zjišťování hash sha256 na souboru: " + file.getPath(), e);
        }
    }

    /**
     * Vytviření mapy streamů souborů v zip souboru.
     *
     * @param file soubor zip
     * @return mapa streamů
     * @throws IOException 
     * @throws ZipException 
     */
    public static Map<String, ByteArrayInputStream> createStreamsMap(final File file) throws ZipException, IOException {
        ZipFile zipFile = new ZipFile(file);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();

    	return createStreamsMap(zipFile, entries);
    }

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
     * Převod souboru XML na třídu.
     *
     * @param classObject objekt XML
     * @param xmlFile     xml soubor
     * @param <T>         typ pro převod
     */
    public static <T> T convertXmlFileToObject(final Class<T> classObject, final Path xmlFile) throws IOException {
    	if (Files.exists(xmlFile)) { 
    		return convertXmlStreamToObject(classObject, new ByteArrayInputStream(FileUtils.readFileToByteArray(xmlFile.toFile())),
                                            xmlFile.toString());
    	}
    	return null;
    }

    /**
     * Převod streamu souboru XML na třídu.
     *
     * @param classObject objekt XML
     * @param xmlStream   xml stream
     * @param <T>         typ pro převod
     */
    public static <T> T convertXmlStreamToObject(final Class<T> classObject, final ByteArrayInputStream xmlStream) {
        return convertXmlStreamToObject(classObject, xmlStream, null);
    }

    /**
     * Převod streamu souboru XML na třídu; neznámý nebo špatně umístěný element je chyba.
     *
     * @param fileName název souboru do chybové zprávy, může být null
     */
    public static <T> T convertXmlStreamToObject(final Class<T> classObject, final ByteArrayInputStream xmlStream,
                                                 final String fileName) {
        if (xmlStream != null) {
            // the first event stops reading: JAXB's default handler skips unknown elements silently, so a
            // misplaced element (e.g. <category> without <categories>) would be lost without an error
            List<String> events = new ArrayList<>(1);
            try {
                JAXBContext jaxbContext = JAXBContext.newInstance(classObject);
                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                unmarshaller.setEventHandler(event -> {
                    ValidationEventLocator locator = event.getLocator();
                    events.add(locator != null && locator.getLineNumber() > 0
                            ? "line " + locator.getLineNumber() + ": " + event.getMessage()
                            : event.getMessage());
                    return false;
                });
                return (T) unmarshaller.unmarshal(xmlStream);
            } catch (Exception e) {
                SystemException se = new SystemException("Nepodařilo se načíst objekt " + classObject.getSimpleName()
                        + (fileName != null ? " ze souboru " + fileName : " ze streamu") + (events.isEmpty() ? "" : ": " + events.get(0)),
                        e, PackageCode.PARSE_ERROR);
                se.set("class", classObject.toString());
                if (fileName != null) {
                    se.set("file", fileName);
                }
                if (!events.isEmpty()) {
                    se.set("detail", events.get(0));
                }
                throw se;
            }
        }
        return null;
    }

    public static <T> ByteArrayOutputStream convertXmlObjectToStream(T data) {
        try {
            //File file = File.createTempFile(data.getClass().getSimpleName() + "-", ".xml");
            JAXBContext jaxbContext = JAXBContext.newInstance(data.getClass());
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            jaxbMarshaller.marshal(data, result);
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("Problém při konverzi xml objektu do xml souboru", e);
        }
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
        public Graph(int v)
        {
            V = v;
            adj = new LinkedList[v];
            for (int i=0; i<v; ++i) {
                adj[i] = new LinkedList<>();
            }
        }

        /**
         * Adds a vertex without edges (a package without dependencies that no package depends on); the
         * order of vertices without edges between them is the order of their addition.
         */
        public void addVertex(T vv) {
            Integer v = map.computeIfAbsent(vv, k -> map.size());
            if (map.size() > V) {
                throw new IllegalStateException("Graph has only " + V + " vertex");
            }
            reverseMap.put(v, vv);
        }

        public void addEdge(T vv, T ww) {
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
        public List<T> topologicalSort()
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

    private static final Logger logger = LoggerFactory.getLogger(PackageUtils.class);

    private static final String PACKAGE_XML = "package.xml";

    /**
     * Reads the description of a package from its ZIP file.
     *
     * @return the description with the path of the file, or {@code null} when the file holds no
     *         {@code package.xml}
     */
    public static PackageInfoWrapper readPackageInfo(final Path path) throws IOException {
        try (ZipFile zipFile = new ZipFile(path.toFile())) {
            ZipEntry zipEntry = zipFile.getEntry(PACKAGE_XML);
            if (zipEntry == null) {
                return null;
            }
            try (InputStream is = zipFile.getInputStream(zipEntry)) {
                ByteArrayInputStream bais = new ByteArrayInputStream(IOUtils.toByteArray(is));
                PackageInfo packageInfo = convertXmlStreamToObject(PackageInfo.class, bais, path + "/" + PACKAGE_XML);
                return new PackageInfoWrapper(packageInfo, path);
            }
        }
    }

    /**
     * Reads the packages of a directory: the ZIP files by code, in the order of the file names; of
     * two files of one package the one with the higher version. A file without a package
     * description is skipped with an error in the log. A missing directory holds no packages.
     */
    public static Map<String, PackageInfoWrapper> readPackageDirectory(final Path dir) throws IOException {
        Map<String, PackageInfoWrapper> packages = new LinkedHashMap<>();
        if (!Files.isDirectory(dir)) {
            return packages;
        }
        try (Stream<Path> paths = Files.list(dir)) {
            for (Path path : paths.sorted().toList()) {
                if (Files.isDirectory(path) || !path.getFileName().toString().endsWith("zip")) {
                    continue;
                }
                logger.info("Reading package info: {}", path);
                PackageInfoWrapper pkg = readPackageInfo(path);
                if (pkg == null) {
                    logger.error("Cannot read package info from file : {}. File is skipped.", path);
                    continue;
                }
                PackageInfoWrapper other = packages.get(pkg.getCode());
                if (other != null && other.getVersion() >= pkg.getVersion()) {
                    logger.warn("Package {} version {} in file {} is not newer than file {}. File is skipped.",
                                pkg.getCode(), pkg.getVersion(), path, other.getPath());
                    continue;
                }
                packages.put(pkg.getCode(), pkg);
            }
        }
        return packages;
    }
}
