package cz.tacr.elza.utils;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * YAML
 * <p>
 * Nástroj pro čtení, editaci a uložení YAML včetně originální struktury a komentářů (krom inline na řádku vedle hodnoty)
 * - podporujeme komentáře v YAML krom inline komentáře za hodnotou
 * <p>
 * Používáme několik základních termínů
 * - emptyline (empty) => prázdná řádka
 * - group => skupina (app:\n uid: aaa # skupina app)
 * - key => klíč - kompletní cesta k hodnotě (app:\n uid: aaa # klíč je app.uid)
 * - inline -> inline hodnota
 * -------
 * <p>
 * Tool for read, update and save YAML with original sructure and comments
 *
 * TODO: Neumí escape, třeba hodnotu s @ na začátku.
 *
 * @author <a href="mailto:petr.compel@marbes.cz">Petr Compel</a>
 */
@SuppressWarnings("unused")
public class Yaml {
    private static final int BOM_SIZE = 4;
    private static final int BUFFER_SIZE = 512;

    private HashMap<String, String> data;
    private List<String> writeHistory;
    private HashSet<String> keys;
    private HashSet<String> emptyKeys;

    /**
     * Základní konstruktor
     * ----
     * Default Constructor for initializing an empty YAML object
     */
    public Yaml() {
        data = new HashMap<>();
        writeHistory = new LinkedList<>();
        keys = new HashSet<>();
        emptyKeys = new HashSet<>();
    }

    /**
     * Kontruktor pro načteníí YAML ze souboru
     * -------
     * Constructor to initialize a YAML object and load data from a file
     *
     * @param file The file that should be loaded to the YAML object
     * @throws IOException                 If any file handling failed
     * @throws YAMLInvalidContentException If the YAML content is invalid
     */
    public Yaml(File file) throws IOException, YAMLInvalidContentException {
        this();
        load(file);
    }

    /**
     * Konstruktor pro načtení YAML ze streamu
     * -------
     * Constructor to initialize a YAML object and load data from a stream
     *
     * @param stream The stream to load data from
     * @throws IOException                 If any handling with the stream failed
     * @throws YAMLInvalidContentException If the YAML content is invalid
     */
    public Yaml(InputStream stream) throws IOException, YAMLInvalidContentException {
        this();
        load(stream);
    }

    /**
     * Konstruktor pro načtení YAML ze stringu
     * -------
     * Constructor to initialize a YAML object and load data from a string
     *
     * @param dataString The YAML data that should be processed to the YAML object
     * @throws YAMLInvalidContentException If the YAML content is invalid
     */
    public Yaml(String dataString) throws YAMLInvalidContentException {
        this();
        load(dataString);
    }

    /**
     * Funkce pro načtení dat ze souboru
     * -------
     * Function to load data from a file to the YAML object
     *
     * @param file File object to load data from
     * @throws IOException                 If any file handling failed
     * @throws YAMLInvalidContentException If the YAML content is invalid
     */
    public void load(File file) throws IOException, YAMLInvalidContentException {
        try (FileInputStream inputStream = new FileInputStream(file)) {
            load(inputStream);
        }
    }

    /**
     * Funkce pro načtení dat ze streamu
     * -------
     * Function to load data from a stream to the YAML object
     *
     * @param stream The stream from where data should be loaded to the YAML object
     * @throws IOException                 If any handling with the stream failed
     * @throws YAMLInvalidContentException If the YAML content is invalid
     */
    public void load(InputStream stream) throws IOException, YAMLInvalidContentException {
        byte[] bom = new byte[BOM_SIZE];
        String encoding = null;
        InputStreamReader reader;
        int unread;
        PushbackInputStream pushbackInputStream = new PushbackInputStream(stream, BOM_SIZE);
        int count = pushbackInputStream.read(bom, 0, bom.length);
        /** Detekce kódování  a BOM */
        if (bom[0] == (byte) 0xEF && bom[1] == (byte) 0xBB && bom[2] == (byte) 0xBF) {
            encoding = "UTF-8";
            unread = count - 3;
        } else if (bom[0] == (byte) 0xFE && bom[1] == (byte) 0xFF) {
            encoding = "UTF-16BE";
            unread = count - 2;
        } else if (bom[1] == (byte) 0xFF && bom[0] == (byte) 0xFE) {
            encoding = "UTF-16LE";
            unread = count - 2;
        } else if (bom[0] == (byte) 0x00 && bom[1] == (byte) 0x00 && bom[2] == (byte) 0xFE && bom[3] == (byte) 0xFF) {
            encoding = "UTF-32BE";
            unread = count - 4;
        } else if (bom[0] == (byte) 0xFF && bom[1] == (byte) 0xFE && bom[2] == (byte) 0x00 && bom[3] == (byte) 0x00) {
            encoding = "UTF-32LE";
            unread = count - 4;
        } else {
            unread = count;
        }
        if (unread > 0) {
            pushbackInputStream.unread(bom, (count - unread), unread);
        } else if (unread < -1) {
            pushbackInputStream.unread(bom, 0, 0);
        }
        if (encoding == null) {
            reader = new InputStreamReader(pushbackInputStream);
        } else {
            reader = new InputStreamReader(pushbackInputStream, encoding);
        }
        //bom = null;
        char[] buffer = new char[BUFFER_SIZE];
        StringBuilder data = new StringBuilder();
        count = reader.read(buffer, 0, buffer.length);
        while (count > 0) {
            data.append(buffer, 0, count);
            count = reader.read(buffer, 0, buffer.length);
        }
        load(data.toString());
    }

    /**
     * Funkce pro načtení dat ze stringu
     * -------
     * Function to load data from a given string to the YAML object
     *
     * @param dataString The data string from which data should be loaded to the object
     */
    public void load(String dataString) throws YAMLInvalidContentException {
        clear();
        List<Integer> indentations = new LinkedList<>();
        List<Integer> indentationIndices = new LinkedList<>();
        indentations.add(0);
        indentationIndices.add(0);
        /** Rozdělení na jednotlivé řádky - (\r pro Win, \n standard) */
        String[] lines = dataString.split("\r?\n");
        String globalKey = "";
        String key = "";
        String value = "";
        int indentationCount;
        int indentationIndex = 0;
        /**
         * Zpracování jednotlivých řádek
         * Samostatené " " bereme jako odsazení - nutno dbát na ručně psanou konfiguraci zda je psaná správně
         */
        for (String line : lines) {
            if (line.replaceAll("\\s*", "").length() == 0) { /** Prázdné řádky */
                saveToWriteHistory("emptyline");
            } else if (line.trim().charAt(0) == '#') { /** Komentáře */
                saveToWriteHistory(line);
            } else {
                /** Hodnoty,skupiny atd... */
                indentationCount = 0;
                while (line.length() > 0) {
                    if (line.charAt(0) == ' ') {
                        ++indentationCount;
                    } else if (line.charAt(0) == '\t') {
                        indentationCount += 4;
                    } else {
                        break;
                    }
                    line = line.substring(1);
                }
                while (indentationCount != indentations.get(indentationIndex)) {
                    if (indentationCount < indentations.get(indentationIndex)) {
                        if (indentationIndex == 1) {
                            globalKey = "";
                        } else {
                            globalKey = globalKey.substring(0, globalKey.lastIndexOf('.'));
                        }
                        indentationIndices.remove(indentationIndex);
                        indentations.remove(indentationIndex--);
                    } else {
                        globalKey = key;
                        indentations.add(indentationCount);
                        indentationIndices.add(0);
                        ++indentationIndex;
                    }
                }
                line = line.trim();
                if (line.length() > 0) {
                    if (line.charAt(0) == '-') {
                        int currentIndex = indentationIndices.get(indentationIndex);
                        key = (globalKey.length() == 0 ? "" : globalKey + ".") + currentIndex;
                        indentationIndices.set(indentationIndex, currentIndex + 1);
                        saveValues(key, line.substring(1).trim());
                    } else {
                        int splitIndex = line.indexOf(':');
                        if (splitIndex < 0) {
                            throw new YAMLInvalidContentException("The YAML content is invalid");
                        } else {
                            key = (globalKey.length() == 0 ? "" : globalKey + ".") + line.substring(0, splitIndex);
                            saveValues(key, line.substring(splitIndex + 1).trim());
                        }
                    }
                }
            }
        }
        keys = new HashSet<>(data.keySet());
    }

    /**
     * Uložení dat do souboru
     * -------
     * Saves the content of the YAML object into a file
     *
     * @throws YAMLNotInitializedException If the YAML object is not initialized properly
     * @throws FileNotFoundException       If the file could not be found
     */
    public void save(File file) throws YAMLNotInitializedException, FileNotFoundException {
        FileOutputStream stream = new FileOutputStream(file);
        PrintStream out = new PrintStream(stream);
        out.append(saveAsString());
        out.flush();
        out.close();
    }

    /**
     * Vytvoření result streamu s daty
     * -------
     * Saves the content of the YAML object into a stream
     *
     * @return The YAML stream that represents the object
     * @throws YAMLNotInitializedException If the YAML object is not initialized properly
     * @throws IOException                 If any IO handling fails
     */
    public OutputStream saveAsStream() throws YAMLNotInitializedException, IOException {
        OutputStream stream = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(stream);
        out.writeBytes(saveAsString());
        out.flush();
        out.close();
        return stream;
    }

    /**
     * Vytvoření stringu s daty
     * -------
     * Saves the content of the YAML object into a string
     *
     * @return The YAML string that represents the object
     * @throws YAMLNotInitializedException If the YAML object is not initialized properly
     */
    public String saveAsString() throws YAMLNotInitializedException {
        StringBuilder yamlString = new StringBuilder();
        int writeCount = getLogicalLineCount();
        String writeLine;
        String inlineKey = "";
        boolean inline = false;
        for (int i = 0; i < writeCount; i++) {
            writeLine = getLogicalLine(i);
            if (inline && writeLine.startsWith("inline." + inlineKey + '.')) {
                continue;
            }
            inline = false;
            yamlString.append("\n");
            if (writeLine.equals("emptyline")) { /** Prázdná přebytečná řádka */
                continue;
            } else if (writeLine.startsWith("empty.")) { /** Prázdná hodnota */
                writeLine = writeLine.substring(6);
                yamlString.append(getIndentation(writeLine, null));
                yamlString.append(getWriteKey(writeLine, null));
                yamlString.append(": []");
            } else if (writeLine.startsWith("group.")) { /** Skupina */
                writeLine = writeLine.substring(6);
                yamlString.append(getIndentation(writeLine, null));
                yamlString.append(getWriteKey(writeLine, null)).append(':');
            } else if (writeLine.startsWith("inline.")) { /** Inline hodnota */
                inline = true;
                writeLine = writeLine.substring(7);
                inlineKey = writeLine.substring(0, writeLine.lastIndexOf('.'));
                try {
                    List<String> list = getStringList(inlineKey, null);
                    int listSize = list == null ? 0 : list.size();
                    if ((i + listSize < writeCount && !getLogicalLine(i + listSize + 1).startsWith("inline." + inlineKey + '.')) || (i + listSize >= writeCount)) {
                        yamlString.append(inlineKey).append(": [");
                        yamlString.append(getWriteValue(inlineKey + ".0"));
                        for (int j = 1; j < listSize; j++) {
                            yamlString.append(", ");
                            yamlString.append(getWriteValue(inlineKey + '.' + j));
                        }
                        yamlString.append("]");
                        i += listSize - 1;
                    } else {
                        yamlString.append(inlineKey).append(": {");
                        yamlString.append(getKeyValuePair(writeLine, false, null));
                        while (i < writeCount) {
                            writeLine = getLogicalLine(++i);
                            if (writeLine.startsWith("inline." + inlineKey + '.')) {
                                yamlString.append(", ");
                                yamlString.append(getKeyValuePair(writeLine.substring(7), false, null));
                            } else {
                                break;
                            }
                        }
                        --i;
                        yamlString.append("}");
                    }
                    if (list != null) {
                        list.clear();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else if (writeLine.startsWith("key.")) { /** Hodnota */
                writeLine = writeLine.substring(4);
                String group = "";
                /** Detekce skupiny hodnoty */
                if (writeLine.contains(".")) {
                    String tempLine = "group." + writeLine;
                    int dotCount = tempLine.split("\\.").length - 1;
                    for (int q = i - 1; q >= 0; q--) {
                        String logicLine = getLogicalLine(q);
                        if (tempLine.contains(logicLine) && (dotCount - (logicLine.split("\\.").length - 1)) > 0 && group.length() < tempLine.length()) {
                            group = logicLine;
                            break;
                        }
                    }
                }
                yamlString.append(getKeyValuePair(writeLine, true, group.isEmpty() ? null : group.substring(6)));
            } else {
                yamlString.append(writeLine);
            }
        }
        return yamlString.toString().substring(1);
    }

    /**
     * Zkontroluje zda YAML obsahuje klíč
     * -------
     * Checks if the given key exists in the YAML object
     *
     * @param key The key that should be checked
     * @return If the key exists in the object or not
     */
    public boolean isSet(String key) {
        return keys.contains(key);
    }

    /**
     * Vrátí počet konfiguračních řádek
     * -------
     * Function to get the amount of logical lines of the YAML object
     *
     * @return Amount of logical lines
     * @throws YAMLNotInitializedException If the YAML object is not initialized properly
     */
    public int getLogicalLineCount() throws YAMLNotInitializedException {
        if (writeHistory == null) {
            throw new YAMLNotInitializedException("The write history of the YAML object ist not initialized");
        }
        return writeHistory.size();
    }

    /**
     * Vrátí obsah konfigurační řádky dle indexu
     * -------
     * Function that returns the content that a logical line contains
     *
     * @param lineIndex Index of the line you want to get the content from
     * @return Content of the selected logical line
     * @throws YAMLNotInitializedException If the YAML object is not initialized properly
     */
    public String getLogicalLine(int lineIndex) throws YAMLNotInitializedException {
        if (writeHistory == null) {
            throw new YAMLNotInitializedException("The write history of the YAML object ist not initialized");
        }
        return writeHistory.get(lineIndex);
    }

    /**
     * Vrátí sekci Yaml - opět jako YAML
     * -------
     * Gets a section of the YAML object
     *
     * @param key Key of the section you want to get
     * @return The selected section
     * @throws YAMLNotInitializedException If the YAML object is not initialized properly
     */
    public Yaml getSection(String key) throws YAMLNotInitializedException {
        int lineCount = getLogicalLineCount();
        String line;
        Yaml section = new Yaml();
        for (int i = 0; i < lineCount; i++) {
            line = getLogicalLine(i);
            if (line.startsWith("group." + key) && !line.equals("group." + key)) {
                section.writeHistory.add(line.replace(key + ".", ""));
            } else if (line.startsWith("inline." + key) || line.startsWith("key." + key)) {
                String lineNew = line.replace("key." + key, "");
                section.writeHistory.add(line);
                section.data.put(lineNew, data.get(line.substring(line.indexOf('.') + 1)));
            }
        }
        section.keys = new HashSet<>(section.data.keySet());
        return section;
    }

    /**
     * Vrátí všechny klíče YAML
     * -------
     * Function to get the keys of the YAML object
     *
     * @return The keys of the YAML object in a Set object
     */
    public HashSet<String> getKeys() {
        return new HashSet<>(keys);
    }

    /**
     * Vrácí klíče YAML
     * - subKeys
     * -- true - vrátí všechny
     * -- false - pouze hlavní sekce
     * -------
     * Function to get the keys of the YAML object
     *
     * @param subKeys If set to false the function only returns the high level keys
     * @return The key set of the YAML object
     */
    public HashSet<String> getKeys(boolean subKeys) {
        if (subKeys) {
            return getKeys();
        }
        return keys.stream().filter(key -> key.indexOf('.') < 0).collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * Vrátí klíče YAML
     * - subKeys
     * -- true - vrátí všechny
     * -- false - pouze hlavní sekce
     * - empty
     * -- true - vrátí i prázdné klíče
     * -- false - vrátí pouze ty které mají hodnotu
     * -------
     * Function to get the keys of the YAML object
     *
     * @param subKeys If set to false the function only returns the high level keys
     * @param empty   If set also empty set keys will be returned
     * @return The key set of the YAML object
     */
    public HashSet<String> getKeys(boolean subKeys, boolean empty) {
        HashSet<String> returnedKeys = getKeys(subKeys);
        if (empty) {
            returnedKeys.addAll(emptyKeys.stream().filter(key -> subKeys || key.indexOf('.') < 0).collect(Collectors.toList()));
        }
        return returnedKeys;
    }

    /**
     * Vrátí byte hodnotu z daného klíče
     * -------
     * Gets a byte value from the YAML object
     *
     * @param key The key of the value you want to get
     * @return The value of the key you searched for
     * @throws YAMLKeyNotFoundException If the key you searched for could not be found in the YAML object
     * @throws NumberFormatException    If the value of the searched key can not be converted to a byte
     */
    public byte getByte(String key) throws YAMLKeyNotFoundException, NumberFormatException {
        return Byte.parseByte(getString(key));
    }

    /**
     * Gets a byte value from the YAML object
     *
     * @param key          The key of the value you want to get
     * @param defaultValue The default value that should be returned if the key could not be found
     * @return The value of the key you searched for
     * @throws NumberFormatException If the value of the searched key can not be converted to a byte
     */
    public byte getByte(String key, byte defaultValue) throws NumberFormatException {
        return Byte.parseByte(getString(key, "" + defaultValue));
    }

    /**
     * Gets a list of byte values from the YAML object
     *
     * @param key The key of the list you want to get
     * @return The byte list of the key you searched for
     * @throws YAMLKeyNotFoundException If the key you searched for could not be found in the YAML object
     * @throws NumberFormatException    If the value of the searched key can not be converted to a byte list
     */
    public List<Byte> getByteList(String key) throws YAMLKeyNotFoundException, NumberFormatException {
        List<String> stringValues = getStringList(key);
        return stringValues.stream().map(Byte::parseByte).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Gets a list of byte values from the YAML object
     *
     * @param key          The key of the list you want to get
     * @param defaultValue The default value that should be returned if the key could not be found
     * @return The byte list of the key you searched for
     * @throws NumberFormatException If the value of the searched key can not be converted to a byte list
     */
    public List<Byte> getByteList(String key, List<Byte> defaultValue) throws NumberFormatException {
        List<String> stringValues = getStringList(key, null);
        if (stringValues == null) {
            return defaultValue;
        }
        return stringValues.stream().map(Byte::parseByte).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Gets a short value from the YAML object
     *
     * @param key The key of the value you want to get
     * @return The value of the key you searched for
     * @throws YAMLKeyNotFoundException If the key you searched for could not be found in the YAML object
     * @throws NumberFormatException    If the value of the searched key can not be converted to a short
     */
    public short getShort(String key) throws YAMLKeyNotFoundException, NumberFormatException {
        return Short.parseShort(getString(key));
    }

    /**
     * Gets a short value from the YAML object
     *
     * @param key          The key of the value you want to get
     * @param defaultValue The default value that should be returned if the key could not be found
     * @return The value of the key you searched for
     * @throws NumberFormatException If the value of the searched key can not be converted to a short
     */
    public short getShort(String key, short defaultValue) throws NumberFormatException {
        return Short.parseShort(getString(key, "" + defaultValue));
    }

    /**
     * Gets a list of short values from the YAML object
     *
     * @param key The key of the list you want to get
     * @return The short list of the key you searched for
     * @throws YAMLKeyNotFoundException If the key you searched for could not be found in the YAML object
     * @throws NumberFormatException    If the value of the searched key can not be converted to a short list
     */
    public List<Short> getShortList(String key) throws YAMLKeyNotFoundException, NumberFormatException {
        List<String> stringValues = getStringList(key);
        return stringValues.stream().map(Short::parseShort).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Gets a list of short values from the YAML object
     *
     * @param key          The key of the list you want to get
     * @param defaultValue The default value that should be returned if the key could not be found
     * @return The short list of the key you searched for
     * @throws NumberFormatException If the value of the searched key can not be converted to a short list
     */
    public List<Short> getShortList(String key, List<Short> defaultValue) throws NumberFormatException {
        List<String> stringValues = getStringList(key, null);
        if (stringValues == null) {
            return defaultValue;
        }
        return stringValues.stream().map(Short::parseShort).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Gets an int value from the YAML object
     *
     * @param key The key of the value you want to get
     * @return The value of the key you searched for
     * @throws YAMLKeyNotFoundException If the key you searched for could not be found in the YAML object
     * @throws NumberFormatException    If the value of the searched key can not be converted to an int
     */
    public int getInt(String key) throws YAMLKeyNotFoundException, NumberFormatException {
        return Integer.parseInt(getString(key));
    }

    /**
     * Gets an int value from the YAML object
     *
     * @param key          The key of the value you want to get
     * @param defaultValue The default value that should be returned if the key could not be found
     * @return The value of the key you searched for
     * @throws NumberFormatException If the value of the searched key can not be converted to an int
     */
    public int getInt(String key, int defaultValue) throws NumberFormatException {
        return Integer.parseInt(getString(key, "" + defaultValue));
    }

    /**
     * Gets a list of int values from the YAML object
     *
     * @param key The key of the list you want to get
     * @return The int list of the key you searched for
     * @throws YAMLKeyNotFoundException If the key you searched for could not be found in the YAML object
     * @throws NumberFormatException    If the value of the searched key can not be converted to an int list
     */
    public List<Integer> getIntList(String key) throws YAMLKeyNotFoundException, NumberFormatException {
        List<String> stringValues = getStringList(key);
        return stringValues.stream().map(Integer::parseInt).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Gets a list of int values from the YAML object
     *
     * @param key          The key of the list you want to get
     * @param defaultValue The default value that should be returned if the key could not be found
     * @return The int list of the key you searched for
     * @throws NumberFormatException If the value of the searched key can not be converted to an int list
     */
    public List<Integer> getIntList(String key, List<Integer> defaultValue) throws NumberFormatException {
        List<String> stringValues = getStringList(key, null);
        if (stringValues == null) {
            return defaultValue;
        }
        return stringValues.stream().map(Integer::parseInt).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Gets a long value from the YAML object
     *
     * @param key The key of the value you want to get
     * @return The value of the key you searched for
     * @throws YAMLKeyNotFoundException If the key you searched for could not be found in the YAML object
     * @throws NumberFormatException    If the value of the searched key can not be converted to a long
     */
    public long getLong(String key) throws YAMLKeyNotFoundException, NumberFormatException {
        return Long.parseLong(getString(key));
    }

    /**
     * Gets a long value from the YAML object
     *
     * @param key          The key of the value you want to get
     * @param defaultValue The default value that should be returned if the key could not be found
     * @return The value of the key you searched for
     * @throws NumberFormatException If the value of the searched key can not be converted to a long
     */
    public long getLong(String key, long defaultValue) throws NumberFormatException {
        return Long.parseLong(getString(key, "" + defaultValue));
    }

    /**
     * Gets a list of long values from the YAML object
     *
     * @param key The key of the list you want to get
     * @return The long list of the key you searched for
     * @throws YAMLKeyNotFoundException If the key you searched for could not be found in the YAML object
     * @throws NumberFormatException    If the value of the searched key can not be converted to a long list
     */
    public List<Long> getLongList(String key) throws YAMLKeyNotFoundException, NumberFormatException {
        List<String> stringValues = getStringList(key);
        return stringValues.stream().map(Long::parseLong).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Gets a list of long values from the YAML object
     *
     * @param key          The key of the list you want to get
     * @param defaultValue The default value that should be returned if the key could not be found
     * @return The long list of the key you searched for
     * @throws NumberFormatException If the value of the searched key can not be converted to a long list
     */
    public List<Long> getLongList(String key, List<Long> defaultValue) throws NumberFormatException {
        List<String> stringValues = getStringList(key, null);
        if (stringValues == null) {
            return defaultValue;
        }
        return stringValues.stream().map(Long::parseLong).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Gets a float value from the YAML object
     *
     * @param key The key of the value you want to get
     * @return The value of the key you searched for
     * @throws YAMLKeyNotFoundException If the key you searched for could not be found in the YAML object
     * @throws NumberFormatException    If the value of the searched key can not be converted to a float
     */
    public float getFloat(String key) throws YAMLKeyNotFoundException, NumberFormatException {
        return Float.parseFloat(getString(key));
    }

    /**
     * Gets a float value from the YAML object
     *
     * @param key          The key of the value you want to get
     * @param defaultValue The default value that should be returned if the key could not be found
     * @return The value of the key you searched for
     * @throws NumberFormatException If the value of the searched key can not be converted to a float
     */
    public float getFloat(String key, float defaultValue) throws NumberFormatException {
        return Float.parseFloat(getString(key, "" + defaultValue));
    }

    /**
     * Gets a list of float values from the YAML object
     *
     * @param key The key of the list you want to get
     * @return The float list of the key you searched for
     * @throws YAMLKeyNotFoundException If the key you searched for could not be found in the YAML object
     * @throws NumberFormatException    If the value of the searched key can not be converted to a float list
     */
    public List<Float> getFloatList(String key) throws YAMLKeyNotFoundException, NumberFormatException {
        List<String> stringValues = getStringList(key);
        return stringValues.stream().map(Float::parseFloat).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Gets a list of float values from the YAML object
     *
     * @param key          The key of the list you want to get
     * @param defaultValue The default value that should be returned if the key could not be found
     * @return The float list of the key you searched for
     * @throws NumberFormatException If the value of the searched key can not be converted to a float list
     */
    public List<Float> getFloatList(String key, List<Float> defaultValue) throws NumberFormatException {
        List<String> stringValues = getStringList(key, null);
        if (stringValues == null) {
            return defaultValue;
        }
        return stringValues.stream().map(Float::parseFloat).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Gets a double value from the YAML object
     *
     * @param key The key of the value you want to get
     * @return The value of the key you searched for
     * @throws YAMLKeyNotFoundException If the key you searched for could not be found in the YAML object
     * @throws NumberFormatException    If the value of the searched key can not be converted to a double
     */
    public double getDouble(String key) throws YAMLKeyNotFoundException, NumberFormatException {
        return Double.parseDouble(getString(key));
    }

    /**
     * Gets a double value from the YAML object
     *
     * @param key          The key of the value you want to get
     * @param defaultValue The default value that should be returned if the key could not be found
     * @return The value of the key you searched for
     * @throws NumberFormatException If the value of the searched key can not be converted to a double
     */
    public double getDouble(String key, double defaultValue) throws NumberFormatException {
        return Double.parseDouble(getString(key, "" + defaultValue));
    }

    /**
     * Gets a list of double values from the YAML object
     *
     * @param key The key of the list you want to get
     * @return The double list of the key you searched for
     * @throws YAMLKeyNotFoundException If the key you searched for could not be found in the YAML object
     * @throws NumberFormatException    If the value of the searched key can not be converted to a double list
     */
    public List<Double> getDoubleList(String key) throws YAMLKeyNotFoundException, NumberFormatException {
        List<String> stringValues = getStringList(key);
        return stringValues.stream().map(Double::parseDouble).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Gets a list of double values from the YAML object
     *
     * @param key          The key of the list you want to get
     * @param defaultValue The default value that should be returned if the key could not be found
     * @return The double list of the key you searched for
     * @throws NumberFormatException If the value of the searched key can not be converted to a double list
     */
    public List<Double> getDoubleList(String key, List<Double> defaultValue) throws NumberFormatException {
        List<String> stringValues = getStringList(key, null);
        if (stringValues == null) {
            return defaultValue;
        }
        return stringValues.stream().map(Double::parseDouble).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Gets a boolean value from the YAML object
     *
     * @param key The key of the value you want to get
     * @return The value of the key you searched for
     * @throws YAMLKeyNotFoundException If the key you searched for could not be found in the YAML object
     */
    public boolean getBoolean(String key) throws YAMLKeyNotFoundException {
        return Boolean.parseBoolean(getString(key));
    }

    /**
     * Gets a boolean value from the YAML object
     *
     * @param key          The key of the value you want to get
     * @param defaultValue The default value that should be returned if the key could not be found
     * @return The value of the key you searched for
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        return Boolean.parseBoolean(getString(key, "" + defaultValue));
    }

    /**
     * Gets a list of boolean values from the YAML object
     *
     * @param key The key of the list you want to get
     * @return The boolean list of the key you searched for
     * @throws YAMLKeyNotFoundException If the key you searched for could not be found in the YAML object
     */
    public List<Boolean> getBooleanList(String key) throws YAMLKeyNotFoundException {
        List<String> stringValues = getStringList(key);
        return stringValues.stream().map(Boolean::parseBoolean).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Gets a list of boolean values from the YAML object
     *
     * @param key          The key of the list you want to get
     * @param defaultValue The default value that should be returned if the key could not be found
     * @return The boolean list of the key you searched for
     */
    public List<Boolean> getBooleanList(String key, List<Boolean> defaultValue) {
        List<String> stringValues = getStringList(key, null);
        if (stringValues == null) {
            return defaultValue;
        }
        return stringValues.stream().map(Boolean::parseBoolean).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Gets a char value from the YAML object
     *
     * @param key The key of the value you want to get
     * @return The value of the key you searched for
     * @throws YAMLKeyNotFoundException    If the key you searched for could not be found in the YAML object
     * @throws YAMLInvalidContentException If the value of the searched key can not be converted to a char
     */
    public char getChar(String key) throws YAMLKeyNotFoundException, YAMLInvalidContentException {
        String value = getString(key);
        if (value.length() == 1) {
            return value.charAt(0);
        }
        throw new YAMLInvalidContentException("The value you searched for could not be converted to a character");
    }

    /**
     * Gets a char value from the YAML object
     *
     * @param key          The key of the value you want to get
     * @param defaultValue The default value that should be returned if the key could not be found
     * @return The value of the key you searched for
     * @throws YAMLInvalidContentException If the value of the searched key can not be converted to a char
     */
    public char getChar(String key, char defaultValue) throws YAMLInvalidContentException {
        String value = getString(key, "" + defaultValue);
        if (value.length() == 1) {
            return value.charAt(0);
        }
        throw new YAMLInvalidContentException("The value you searched for could not be converted to a character");
    }

    /**
     * Gets a list of char values from the YAML object
     *
     * @param key The key of the list you want to get
     * @return The char list of the key you searched for
     * @throws YAMLKeyNotFoundException    If the key you searched for could not be found in the YAML object
     * @throws YAMLInvalidContentException If the value of the searched key can not be converted to a char list
     */
    public List<Character> getCharList(String key) throws YAMLKeyNotFoundException, YAMLInvalidContentException {
        List<String> stringValues = getStringList(key);
        return getCharList(stringValues);
    }

    /**
     * Modify a list of char values from the YAML object
     *
     * @param stringValues list of strings to be converted to char
     * @throws YAMLInvalidContentException If the value of the searched key can not be converted to a char list
     */
    private List<Character> getCharList(final List<String> stringValues) throws YAMLInvalidContentException {
        List<Character> values = new LinkedList<>();
        for (String value : stringValues) {
            if (value.length() == 1) {
                values.add(value.charAt(0));
            } else {
                values.clear();
                throw new YAMLInvalidContentException("The value you searched for could not be converted to a character");
            }
        }
        return values;
    }

    /**
     * Gets a list of char values from the YAML object
     *
     * @param key          The key of the list you want to get
     * @param defaultValue The default value that should be returned if the key could not be found
     * @return The char list of the key you searched for
     * @throws YAMLInvalidContentException If the value of the searched key can not be converted to a char list
     */
    public List<Character> getCharList(String key, List<Character> defaultValue) throws YAMLInvalidContentException {
        List<String> stringValues = getStringList(key, null);
        if (stringValues == null) {
            return defaultValue;
        }
        return getCharList(stringValues);
    }

    /**
     * Gets a string value from the YAML object
     *
     * @param key The key of the value you want to get
     * @return The value of the key you searched for
     * @throws YAMLKeyNotFoundException If the key you searched for could not be found in the YAML object
     */
    public String getString(String key) throws YAMLKeyNotFoundException {
        if (keys.contains(key)) {
            return data.get(key);
        } else if (writeHistory.contains("empty." + key)) {
            return "";
        }
        throw new YAMLKeyNotFoundException("The key you wanted to retrieve (\"" + key + "\") could not be found in the YAML object");
    }

    /**
     * Gets a string value from the YAML object
     *
     * @param key          The key of the value you want to get
     * @param defaultValue The default value that should be returned if the key could not be found
     * @return The value of the key you searched for
     */
    public String getString(String key, String defaultValue) {
        if (keys.contains(key)) {
            return data.get(key);
        }
        return defaultValue;
    }

    /**
     * Gets a list of strings from the YAML object
     *
     * @param key The key of the list you want to get
     * @return The list of values you searched for
     * @throws YAMLKeyNotFoundException If the key you searched for could not be found in the YAML object
     */
    public List<String> getStringList(String key) throws YAMLKeyNotFoundException {
        List<String> values = getStringsList(key);
        if (values != null) return values;
        throw new YAMLKeyNotFoundException("The key you wanted to retrieve (\"" + key + "\") could not be found in the YAML object");
    }

    /**
     * Gets a list of strings from the YAML object
     *
     * @param key          The key of the list you want to get
     * @param defaultValue The default value that should be returned if the key could not be found
     * @return The list of values you searched for
     */
    public List<String> getStringList(String key, List<String> defaultValue) {
        List<String> values = getStringsList(key);
        if (values != null) return values;
        return defaultValue;
    }

    /**
     * Returns list of string
     *
     * @param key key on which we will find values
     * @return list of strings
     */
    private List<String> getStringsList(String key) {
        if (keys.contains(key + ".0")) {
            key += ".";
            List<String> values = new LinkedList<>();
            values.add(data.get(key + "0"));
            for (int i = 1; i < Integer.MAX_VALUE; i++) {
                if (keys.contains(key + i)) {
                    values.add(data.get(key + i));
                } else {
                    break;
                }
            }
            return values;
        }
        return null;
    }

    /**
     * Nastaví hodnotu na daný klíč
     * -------
     * Sets the value for the given key in the YAML object
     *
     * @param key   The key for which the value should be set
     * @param value The value that should be assigned
     */
    public void set(String key, Object value) {
        String stringValue = value.toString();

        if (stringValue.equals("")) { /** prázdná hodnota - vytvoříme prázdný klíč**/
            if (!writeHistory.contains("empty." + key)) {
                writeHistory.add("empty." + key);
                emptyKeys.add(key);
            }
            return;
        } else if (writeHistory.contains("empty." + key)) { /** neprázdná hodnota - pokud existuje prázdný klíč tak jej smažeme */
            writeHistory.remove(writeHistory.indexOf("empty." + key));
            emptyKeys.remove(key);
        }
        data.put(key, stringValue);
        /** detekce a zapsání do skupiny a nastavení druhy hodnoty */
        if (keys.add(key)) {
            int index = key.indexOf('.');
            if (index < 0) {
                writeHistory.add("key." + key);
            } else {
                String group = key.substring(0, key.lastIndexOf('.'));
                int writeHistorySize = writeHistory.size();
                index = writeHistory.indexOf("group." + group);
                String currentKey;
                if (index >= 0) {
                    do {
                        if (++index < writeHistorySize) {
                            currentKey = writeHistory.get(index);
                        } else {
                            index++;
                            break;
                        }
                    }
                    while (currentKey.startsWith("empty." + group) || currentKey.startsWith("group." + group) || currentKey.startsWith("key." + group) || currentKey.startsWith("inline." + group) || currentKey.startsWith(" ") || currentKey.startsWith("\n") || currentKey.startsWith("#"));
                    writeHistory.add(--index, "key." + key);
                } else {
                    boolean startFound = false;
                    for (index = 0; index < writeHistorySize; index++) {
                        if (startFound) {
                            if (!writeHistory.get(index).startsWith("inline." + group)) {
                                index++;
                                break;
                            }
                        } else if (writeHistory.get(index).startsWith("inline." + group)) {
                            startFound = true;
                        }
                    }
                    if (startFound) {
                        writeHistory.add(--index, "inline." + key);
                    } else {
                        int lastIndex = 0;
                        int subIndex;
                        String subKey;
                        String lastSubKey = "";
                        index = key.indexOf('.');
                        while (index >= 0) {
                            subKey = key.substring(0, index);
                            subIndex = writeHistory.indexOf("group." + subKey);
                            if (subIndex < 0) {
                                do {
                                    if (++lastIndex < writeHistorySize) {
                                        currentKey = writeHistory.get(lastIndex);
                                    } else {
                                        lastIndex++;
                                        break;
                                    }
                                }
                                while (currentKey.startsWith("empty." + lastSubKey) || currentKey.startsWith("group." + lastSubKey) || currentKey.startsWith("key." + lastSubKey) || currentKey.startsWith("inline." + lastSubKey) || currentKey.startsWith(" ") || currentKey.startsWith("\n") || currentKey.startsWith("#"));
                                writeHistory.add(--lastIndex, "group." + subKey);
                                ++writeHistorySize;
                            } else {
                                lastIndex = subIndex;
                            }
                            lastSubKey = subKey;
                            index = key.indexOf('.', index + 1);
                        }
                        writeHistory.add(lastIndex + 1, "key." + key);
                    }
                }
            }
        }
    }

    /**
     * Funkce pro vyčištění objektu
     * -------
     * Function to clear all data of the YAML object
     */
    public void clear() {
        data.clear();
        writeHistory.clear();
        keys.clear();
        emptyKeys.clear();
    }

    /**
     * Funkce pro uvolnění zdrojů
     * -------
     * Function that frees all resources of the YAML object
     */
    public void dispose() {
        clear();
        data = null;
        writeHistory = null;
        keys = null;
        emptyKeys = null;
    }

    /**
     * Uloží do zapisovací historie obsah pokud jeho délka je větší než 0
     * -------
     * Saves the stated content to the write history of the YAML object if the content is not empty
     *
     * @param content The content that has to be saved to the write history
     */
    private void saveToWriteHistory(String content) {
        if (content.length() > 0) {
            writeHistory.add(content);
        }
    }

    /**
     * Uloží hodnoty do YAML na daný kláč
     * -------
     * Saves the given values to the YAML object data
     *
     * @param key         The current key
     * @param valueString The value string that has to be processed
     */
    private void saveValues(String key, String valueString) throws YAMLInvalidContentException {
        if (valueString.length() > 0) {
            if (valueString.charAt(0) == '[') {
                if (valueString.charAt(valueString.length() - 1) == ']') {
                    valueString = valueString.substring(1, valueString.length() - 1).trim();
                    String[] values = valueString.split(",");
                    if (values.length == 1 && values[0].equals("")) {
                        saveToWriteHistory("empty." + key);
                        emptyKeys.add(key);
                        return;
                    }
                    for (int i = 0; i < values.length; i++) {
                        saveToWriteHistory("inline." + key + "." + i);
                        data.put(key + "." + i, getValue(values[i].trim()));
                    }
                } else {
                    throw new YAMLInvalidContentException("The value for the given list is invalid");
                }
            } else if (valueString.charAt(0) == '{') {
                if (valueString.charAt(valueString.length() - 1) == '}') {
                    valueString = valueString.substring(1, valueString.length() - 1).trim();
                    String[] values = valueString.split(",");
                    if (values.length == 1 && values[0].equals("")) {
                        saveToWriteHistory("empty." + key);
                        emptyKeys.add(key);
                        return;
                    }
                    int keyIndex = 0;
                    int splitIndex;
                    for (String value : values) {
                        value = value.trim();
                        if (value.length() > 0) {
                            if (value.charAt(0) == '-') {
                                saveToWriteHistory("inline." + key + "." + keyIndex);
                                data.put(key + "." + keyIndex++, getValue(value.substring(1).trim()));
                            } else if (value.indexOf(':') > 0) {
                                splitIndex = value.indexOf(':');
                                String tempKey = value.substring(0, splitIndex);
                                saveToWriteHistory("inline." + key + "." + tempKey);
                                data.put(key + "." + tempKey, getValue(value.substring(splitIndex + 1).trim()));
                            } else {
                                throw new YAMLInvalidContentException("The value for the given mapping in the list is invalid");
                            }
                        }
                    }
                } else {
                    throw new YAMLInvalidContentException("The value for the given list is invalid");
                }
            } else {
                saveToWriteHistory("key." + key);
                data.put(key, getValue(valueString));
            }
        } else {
            saveToWriteHistory("group." + key);
        }
    }

    /**
     * Získání string hodnoty z YAML (extrakce - esceping atd...)
     * -------
     * Function that gets the value of a stated value string
     *
     * @param valueString The value string of which the value should be extracted
     * @return The value of the value string
     */
    private String getValue(String valueString) throws YAMLInvalidContentException {
        if (valueString.length() == 0) {
            return "";
        } else if (valueString.charAt(0) == '\'') {
            int length = valueString.length();
            if (valueString.charAt(--length) == '\'') {
                return valueString.substring(1, length).replace("\\'", "'");
            }
            throw new YAMLInvalidContentException("The value for the given string is invalid");
        } else if (valueString.charAt(0) == '"') {
            if (valueString.length() == 1 || !valueString.substring(1).contains("\"")) {
                throw new YAMLInvalidContentException("The value for the given string is invalid");
            }

            if (valueString.charAt(1) == '"') {
                return "";
            }

            int endIndex = 1;
            int length = valueString.length() - 1;
            while (endIndex < length) {
                int index = valueString.indexOf('"', endIndex + 1);
                if (index == -1) {
                    throw new YAMLInvalidContentException("The value for the given string '" + valueString + "' is invalid");
                }
                char previousChar = valueString.charAt(index - 1);
                char prePreviousChar = valueString.charAt(index - 2);
                if (previousChar != '\\' || prePreviousChar == '\\') {
                    return valueString.substring(1, index).replace("\\\"", "\"").replace("\\\\", "\\");
                }
                endIndex = index;
            }

            throw new YAMLInvalidContentException("The value for the given string  '" + valueString + "' is invalid");
        } else {
            if (valueString.contains(" #")) {
                return valueString.substring(0, valueString.indexOf(" #")).trim();
            }
            return valueString;
        }
    }

    /**
     * Vrátí string reprezentující odsazení pro daný klíč (a skupinu)
     * -------
     * Returns an indentation string
     *
     * @param key Key of the value
     * @return Indentation string
     */
    private String getIndentation(String key, String group) {
        String indentation = "";
        if (group != null) {
            key = group + ".";
        }
        int startIndex = key.indexOf(".");
        while (startIndex >= 0) {
            indentation += "  ";
            startIndex = key.indexOf(".", startIndex + 1);
        }
        return indentation;
    }

    /**
     * Vrátí klíč který bude zapsán do yaml (v nějaké skupině)
     * -------
     * Gets the key that should be saved
     *
     * @param key The original save key
     * @return The key that could be saved
     */
    private String getWriteKey(String key, String group) {
        if (group != null) {
            return key.replace(group + ".", "");
        } else {
            int index = key.lastIndexOf('.');
            if (index >= 0) {
                return key.substring(index + 1);
            }
            return key;
        }
    }

    /**
     * Vrátí hodnotu k zapsání
     * -------
     * Gets the value that should be saved
     *
     * @param key The key of the value you want to get
     * @return The value to save
     */
    private String getWriteValue(String key) {
        key = data.get(key).replace("\\", "\\\\").replace("\"", "\\\"");
        /*boolean hasComment = key.contains(" #");
        String comment = "";
        if (hasComment) {
            int indexOfComment = key.indexOf(" #");
            comment = key.substring(indexOfComment);
            key = key.substring(0, indexOfComment);
        }*/
        if (key.indexOf(' ') >= 0 || key.indexOf('{') >= 0 || key.indexOf('}') >= 0 || key.indexOf('[') >= 0 || key.indexOf(']') >= 0) {
            return '"' + key + '"'; //+ comment;
        }
        return key;// + comment;
    }

    /**
     * Vrátí pár klíč a hodnota včetně odsazení
     * -------
     * Gets a writable key value pair
     *
     * @param writeLine   The line that defines the key value pair
     * @param indentation If indentation should be used
     * @return The key value string
     */
    private String getKeyValuePair(String writeLine, boolean indentation, String group) {
        try {
            int index = Integer.parseInt(writeLine.substring(writeLine.lastIndexOf('.') + 1));
            return (indentation ? getIndentation(writeLine, group) : "") + "- " + getWriteValue(writeLine);
        } catch (Exception ex) {
            return (indentation ? getIndentation(writeLine, group) : "") + getWriteKey(writeLine, group) + ": " + getWriteValue(writeLine);
        }
    }

    /**
     * Vyjímky
     * -------
     * Exceptions
     */

    /**
     * Invalidní YAML
     * -------
     * Invalid YAML
     */
    public class YAMLInvalidContentException extends Exception {
        public YAMLInvalidContentException() {
            super();
        }

        public YAMLInvalidContentException(String message) {
            super(message);
        }
    }

    /**
     * Klíč nenalezen
     * -------
     * Key not found
     */
    public class YAMLKeyNotFoundException extends Exception {
        public YAMLKeyNotFoundException() {
            super();
        }

        public YAMLKeyNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Neplatný YAML / špatně inicializovaný objekt
     * -------
     * Not inicialized by YAML
     */
    public class YAMLNotInitializedException extends Exception {
        public YAMLNotInitializedException() {
            super();
        }

        public YAMLNotInitializedException(String message) {
            super(message);
        }
    }
}