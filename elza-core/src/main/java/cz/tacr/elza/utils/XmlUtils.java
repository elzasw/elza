package cz.tacr.elza.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

import cz.tacr.elza.service.ByteStreamResult;
import liquibase.util.file.FilenameUtils;

/**
 * Pomocná třída pro práci s xml daty.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 26. 4. 2016
 */
public class XmlUtils {

    public static final String XSLT_EXTENSION = ".xslt";

    private static final Logger logger = LoggerFactory.getLogger(XmlUtils.class);

    public static byte[] transformData(final byte[] xmlData, final String transformationName, final String transformationsDirectory) {
        Assert.notNull(xmlData);
        Assert.notNull(transformationName);

        StreamSource xmlSource = null;
        StreamSource xsltSource = null;
        ByteStreamResult result = null;
        byte[] byteArray = null;
        try {
            xmlSource = getStreamSource(xmlData);
            xsltSource = getTransformationSource(transformationName, transformationsDirectory);
            result = new ByteStreamResult(new ByteArrayOutputStream());

            TransformerFactory transFact = TransformerFactory.newInstance();
            Transformer trans = transFact.newTransformer(xsltSource);
            trans.setOutputProperty(OutputKeys.ENCODING, "utf-8");
            trans.transform(xmlSource, result);

            byteArray = result.toByteArray();
        } catch (TransformerException ex) {
            throw new IllegalStateException("Chyba při transformaci vstupních dat.", ex);
        } finally {
            if (xmlSource != null) {
                try {
                    xmlSource.getInputStream().close();
                } catch (IOException ex) {
                    logger.error("Chyba při zavírání souboru se vstupními daty.", ex);
                }
            }

            if (xsltSource != null) {
                try {
                    xsltSource.getInputStream().close();
                } catch (IOException ex) {
                    logger.error("Chyba při zavírání souboru s transformací.", ex);
                }
            }

            if (result != null && result.getOutputStream() != null) {
                try {
                    result.getOutputStream().close();
                } catch (IOException ex) {
                    logger.error("Chyba při zavírání výsledného souboru.", ex);
                }
            }
        }

        return byteArray;
    }

    private static StreamSource getTransformationSource(final String transformationName, final String transformationsDirectory) {
        Assert.notNull(transformationName);

        File transformationFile = getTransformationFileByName(transformationName, transformationsDirectory);

        return getStreamSource(transformationFile);
    }

    private static File getTransformationFileByName(final String transformationName, final String transformationsDirectory) {
        File transformationFile = new File(transformationsDirectory + File.separator + transformationName + XSLT_EXTENSION);
        return transformationFile;
    }

    private static StreamSource getStreamSource(final byte[] xmlData) {
        Assert.notNull(xmlData);

        return new StreamSource(new ByteArrayInputStream(xmlData));
    }

    private static StreamSource getStreamSource(final File xmlFile) {
        Assert.notNull(xmlFile);

        logger.info("Otevírání souboru " + xmlFile);
        try {
            return new StreamSource(new FileInputStream(xmlFile));
        } catch (IOException ex) {
            throw new IllegalStateException("Chyba při otevírání souboru " + xmlFile, ex);
        }
    }

    /**
     * Převede data z objektu do xml.
     *
     * @param data data
     *
     * @return pole bytů
     */
    public static <T, C> byte[] marshallData(final T data,final Class<C> cls) {
        Assert.notNull(data);

        try  {
            Marshaller marshaller = createMarshaller(cls);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            marshaller.marshal(data, outputStream);

            return outputStream.toByteArray();
        } catch (JAXBException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Vytvoří marshaller pro data.
     *
     * @return marshaller pro data
     */
    private static <C> Marshaller createMarshaller(final Class<C> cls) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(cls);
        Marshaller marshaller = jaxbContext.createMarshaller();

        return marshaller;
    }

    public static InputStream transformXml(final MultipartFile xmlFile, final File transformationFile)
        throws TransformerFactoryConfigurationError {
        Assert.notNull(xmlFile);

        StreamSource xmlSource = null;
        StreamSource xsltSource = null;
        ByteStreamResult result = null;
        byte[] byteArray = null;
        try {
            xmlSource = getStreamSource(xmlFile);
            xsltSource = getTransformationSource(transformationFile);
            result = new ByteStreamResult(new ByteArrayOutputStream());

            TransformerFactory transFact = TransformerFactory.newInstance();
            Transformer trans = transFact.newTransformer(xsltSource);
            trans.setOutputProperty(OutputKeys.ENCODING, "utf-8");
            trans.transform(xmlSource, result);

            byteArray = result.toByteArray();
        } catch (TransformerException ex) {
            throw new IllegalStateException("Chyba při transformaci vstupních dat.", ex);
        } finally {
            if (xmlSource != null) {
                try {
                    xmlSource.getInputStream().close();
                } catch (IOException ex) {
                    logger.error("Chyba při zavírání souboru se vstupními daty.", ex);
                }
            }

            if (xsltSource != null) {
                try {
                    xsltSource.getInputStream().close();
                } catch (IOException ex) {
                    logger.error("Chyba při zavírání souboru s transformací.", ex);
                }
            }

            if (result != null && result.getOutputStream() != null) {
                try {
                    result.getOutputStream().close();
                } catch (IOException ex) {
                    logger.error("Chyba při zavírání výsledného souboru.", ex);
                }
            }
        }

        return new ByteArrayInputStream(byteArray);
    }

    private static StreamSource getTransformationSource(final File transformationFile) {
        Assert.notNull(transformationFile);

        return getStreamSource(transformationFile);
    }

    private static StreamSource getStreamSource(final MultipartFile xmlFile) {
        Assert.notNull(xmlFile);

        try {
            logger.info("Otevírání souboru " + xmlFile);
            return new StreamSource(xmlFile.getInputStream());
        } catch (IOException ex) {
            throw new IllegalStateException("Chyba při otevírání souboru " + xmlFile, ex);
        }
    }

    /**
     * Převede data z xml do objektu.
     *
     * @param inputStream stream
     *
     * @return objekt typu T
     */
    public static <T, C> T unmarshallData(final InputStream inputStream, final Class<C> cls) throws JAXBException {
        Assert.notNull(inputStream);

        Unmarshaller unmarshaller = createUnmarshaller(cls);

        return  (T) unmarshaller.unmarshal(inputStream);
    }

    /**
     * Vytvoří unmarshaller pro data.
     *
     * @return unmarshaller pro data
     */
    private static <C> Unmarshaller createUnmarshaller(final Class<C> cls) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(cls);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

        return unmarshaller;
    }

    /**
     * Vrátí názvy šablon.
     *
     * @return názvy šablon
     */
    public static List<String> getTransformationNames(final String transformationsDirectory) {
        File transformDir = new File(transformationsDirectory);

        if (!transformDir.exists()) {
            transformDir.mkdirs();
            return Collections.EMPTY_LIST;
        }

        if (!transformDir.isDirectory()) {
            throw new IllegalStateException("Cesta " + transformDir.getAbsolutePath() + " není adresář.");
        }

        File[] listFiles = transformDir.listFiles((dir, name) -> name.endsWith(XmlUtils.XSLT_EXTENSION));
        if (listFiles == null) {
            throw new IllegalStateException("Chyba při načítání souborů z adresáře " + transformDir.getAbsolutePath());
        }
        List<String> transformationNames = new ArrayList<>(listFiles.length);
        for (File file : listFiles) {
            String transformationName = FilenameUtils.getBaseName(file.getName());
            transformationNames.add(transformationName.toLowerCase(Locale.getDefault()));
        }

        Collator collator = Collator.getInstance(Locale.getDefault());
        collator.setStrength(Collator.PRIMARY);
        Collections.sort(transformationNames, collator);

        return transformationNames;
    }
}
