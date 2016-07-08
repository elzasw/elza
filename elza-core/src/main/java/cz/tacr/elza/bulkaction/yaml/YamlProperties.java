package cz.tacr.elza.bulkaction.yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;

import liquibase.util.file.FilenameUtils;


/**
 * Třída zjednodušuje práci s Yaml soubory. Tváří se jako {@link java.util.Properties}.
 *
 * @author <a href="mailto:jaroslav.pubal@marbes.cz">Jaroslav Půbal</a>
 */
public class YamlProperties extends Properties {

    /**
     * Singleton Yaml procesor
     */
    private Yaml yaml = new Yaml();

    /**
     * Načtené hodnoty z Yaml souboru
     */
    private Map map = new HashMap();


    @Override
    public synchronized void load(Reader reader) throws IOException {
        map = (Map) yaml.load(reader);
    }

    @Override
    public synchronized void load(InputStream inStream) throws IOException {
        map = (Map) yaml.load(inStream);
    }

    @Override
    public void store(Writer writer, String comments) throws IOException {
        String yamlContent = yaml.dumpAs(map, Tag.MAP, DumperOptions.FlowStyle.BLOCK);
        writer.write(yamlContent);
    }

    @Override
    public void store(OutputStream out, String comments) throws IOException {
        String yamlContent = yaml.dumpAs(map, Tag.MAP, DumperOptions.FlowStyle.BLOCK);
        PrintStream printStream = new PrintStream(out, true, "utf-8");
        printStream.print(yamlContent);
        printStream.close();
    }

    @Override
    public String getProperty(String key) {
        Map actualMap = getLastMapFromYamlTree(key);
        String lastName = getLastName(key);

        Object value = actualMap.get(lastName);
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof Number) {
            return value.toString();
        }
        return null;
    }

    private Map getLastMapFromYamlTree(String key) {
        String[] names = StringUtils.split(key, '.');
        //String propertyName = names[names.length - 1];

        Object[] path = ArrayUtils.subarray(names, 0, names.length - 1);

        Map actualMap = map;
        for (Object name : path) {
            Object o = actualMap.get(name);
            if (o instanceof Map) {
                actualMap = (Map) o;
            }
        }
        return actualMap;
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        Map actualMap = getLastMapFromYamlTree(key);
        String lastName = getLastName(key);

        Object value = actualMap.get(lastName);
        if (value instanceof String) {
            return (String) value;
        }
        return defaultValue;
    }

    private String getLastName(String key) {
        return StringUtils.contains(key, '.') ? StringUtils.substringAfterLast(key, ".") : key;
    }

    @Override
    public synchronized boolean containsKey(Object key) {
        Map actualMap = getLastMapFromYamlTree((String) key);
        String lastName = getLastName((String) key);
        return actualMap.containsKey(lastName);
    }

    @Override
    public synchronized Object setProperty(String key, String value) {
        Map actualMap = getLastMapFromYamlTree(key);
        String lastName = getLastName(key);
        return actualMap.put(lastName, value);

    }

    @Override
    public String toString() {
        StringWriter writer = new StringWriter();
        try {
            this.store(writer, "");
        } catch (IOException e) {
            throw new IllegalStateException("Problém s převedením YAML na string", e);
        }
        return writer.getBuffer().toString();
    }


    //---------------------- static pomocne funkce -----------------------

    /**
     * Zjití tento soubor je Yaml soubor.
     *
     * @param propertiesFile properties soubor
     * @return true - ano je
     */
    public static boolean isYamlFile(File propertiesFile) {
        return FilenameUtils.isExtension(propertiesFile.getPath(), "yaml");
    }

}