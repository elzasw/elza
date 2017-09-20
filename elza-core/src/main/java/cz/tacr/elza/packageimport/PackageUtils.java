package cz.tacr.elza.packageimport;

import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.PackageCode;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
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
}
