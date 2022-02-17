package cz.tacr.elza.daoimport.service;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.mediafilter.JPEGFilter;
import org.dspace.content.MetadataField;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Context;
import org.xml.sax.SAXException;

import cz.tacr.elza.daoimport.parser.TechnicalMDParser;
import cz.tacr.elza.daoimport.protocol.Protocol;
import cz.tacr.elza.daoimport.service.vo.DaoFile;
import cz.tacr.elza.daoimport.service.vo.ImportConfig;
import cz.tacr.elza.daoimport.service.vo.ImportDao;
import cz.tacr.elza.daoimport.service.vo.MetadataInfo;
import cz.tacr.elza.metadataconstants.MetadataEnum;
import cz.tacr.elza.xsd.dspace.dao.Attribute;
import cz.tacr.elza.xsd.dspace.dao.Dao;
import cz.tacr.elza.xsd.dspace.dao.Page;
import cz.tacr.elza.xsd.dspace.dao.Pages;

public class DaoImportWorker {
	final private static Logger log = Logger.getLogger(DaoImportWorker.class);
	
	final ImportConfig config;
	
	final Context context;
	
	Dao daoXml;
	
	final Protocol protocol;

	private Path daoDir;	
	
	final MetadataFieldService metadataFieldService;

	final private DroidService droidService;

	final private JhoveService jhoveService;
	
	final static JAXBContext jaxbContext;
	
	static {
		try {
			jaxbContext = JAXBContext.newInstance(Dao.class);		
		} catch (JAXBException e) {
			log.error("Failed to initialize JAXBContext", e);
            throw new IllegalStateException("Failed to initialize JAXBContext", e);
        }
	}
	
	public DaoImportWorker(final Context context,
			final MetadataFieldService metadataFieldService,
			final DroidService droidService, 
			final JhoveService jhoveService, 
			final ImportConfig config, final Protocol protocol, final Path daoDir) {
		this.context = context;
		this.droidService = droidService;
		this.jhoveService = jhoveService;
		this.metadataFieldService = metadataFieldService;
		this.config = config;
		this.protocol = protocol;
		this.daoDir = daoDir;
	}

	public void prepare(ImportDao importDao) throws IOException {
        Path generatedDir = Paths.get(daoDir.toAbsolutePath().toString(), DaoImportService.GENERATED_DIR);
        if (Files.exists(generatedDir)) {
            protocol.add("Odstraňování adresáře " + DaoImportService.GENERATED_DIR);
            deleteDirectoryRecursion(generatedDir);
        }
        protocol.add("Vytváření adresáře " + DaoImportService.GENERATED_DIR);
        Files.createDirectory(generatedDir);
		
		
	     // načtení dao.xml pokud existuje
        daoXml = readDaoXml(daoDir, protocol);
		
        List<String> daoPages = new ArrayList<>();
        List<Path> daoBitStreams = new ArrayList<>();

        // rotřídění souborů a adresářů k dalšímu zpracování
        try (DirectoryStream<Path> fileStream = Files.newDirectoryStream(daoDir)) {
            for (Path contentFile : fileStream) {            	
            	String fileName = contentFile.getFileName().toString();
            	// skip dao.xml
            	if(DaoImportService.DAO_XML.equalsIgnoreCase(fileName)) {
            		continue;
            	}
            	// skip .meta and .thumb files
            	if(fileName.endsWith(DaoImportService.METADATA_EXTENSION)|| fileName.endsWith(DaoImportService.THUMBNAIL_EXTENSION)) {
            		continue;
            	}
            	if (Files.isRegularFile(contentFile)) {
            		daoPages.add(contentFile.getFileName().toString());
            	} else 
            	if (Files.isDirectory(contentFile)) {
            		daoBitStreams.add(contentFile);
            	} else {
        			protocol.add("Nelze zpracovat poožku: " + contentFile);
                }
            }
        }
        
        fillDaoAttributes(importDao);
		
        processDaoPages(generatedDir, importDao, daoPages);
        /*for(Path daoBitStream: daoBitStreams) {
        	processBitstreamDir(config, generatedDir, importDao, daoBitStream, protocol, daoXml);
        }*/
        
	}
	
    private void processDaoPages(Path generatedDir, ImportDao importDao, List<String> daoPages) throws IOException {

        final Map<String, Integer> positions = new HashMap<>();        
        if (daoXml != null) {            
            // ulozeni pozice v dao
            List<Pages> pagesCol = daoXml.getPages();
            for(Pages pages: pagesCol) {
            	if(pages.getBitstream()==null||DaoImportService.ORIGINAL_BUNDLE.equals(pages.getBitstream())) {            		
            		int pos = 0;
            		for(Page page: pages.getPage()) {
            			positions.put(page.getFile(), pos++);
            		}
            		break;
            	}
            }
        }
        
		// nejprve serazeni dle pozice a abecedy
		daoPages.sort(new Comparator<String>() {

			@Override
			public int compare(String o1, String o2) {
				Integer pos1 = positions.get(o1);
				Integer pos2 = positions.get(o2);
				if (pos1 != null) {
					if (pos2 == null) {
						return -1;
					}
					return pos1.compareTo(pos2);
				} else if (pos2 != null) {
					return 1;
				}
				// pozice neni definovana - porovna se dle abecedy
				return o1.compareTo(o2);
			}
		});        

        for(String daoPageName: daoPages) {
        	Path daoPage = this.daoDir.resolve(daoPageName);
        	
        	DaoFile daoFile = processFile(generatedDir, importDao, daoPage);
        	if(daoFile!=null) {
        		processMetadata(generatedDir, importDao, daoPage, daoFile);
        	
        		processThumbnale(generatedDir, importDao, daoPage);
        	}
        }
	}
    
	private void processThumbnale(Path generatedDir, ImportDao importDao, Path daoPage) throws IOException {
        Path thumbnailFile = Paths.get(daoPage.toString() + DaoImportService.THUMBNAIL_EXTENSION);
        Path destPath = generatedDir.resolve(thumbnailFile.getFileName().toString());
        if (Files.exists(thumbnailFile)) {
            protocol.add("Kopírování souboru " + thumbnailFile.getFileName().toString()  + " do adresáře " + DaoImportService.GENERATED_DIR);

            Files.copy(thumbnailFile, destPath, StandardCopyOption.REPLACE_EXISTING);                    
        } else {
            generateThumbnailFile(daoPage, destPath);
        }
        importDao.addFile(DaoImportService.THUMBNAIL_BUNDLE, destPath);		
	}

	private void processMetadata(Path generatedDir, ImportDao importDao, Path daoPage, DaoFile daoOrigFile) throws IOException {
        Path metadataFile = Paths.get(daoPage.toAbsolutePath().toString() + DaoImportService.METADATA_EXTENSION);
        Path destPath = generatedDir.resolve(metadataFile.getFileName().toString());
        if (Files.exists(metadataFile)) {
            protocol.add("Kopírování souboru " + metadataFile.getFileName().toString()  + " do adresáře " + DaoImportService.GENERATED_DIR);

            daoOrigFile.setTechMD(parseMetadata(metadataFile));
            
            Files.copy(metadataFile, destPath, StandardCopyOption.REPLACE_EXISTING);
            importDao.addFile(DaoImportService.METADATA_BUNDLE, destPath);
        } else {
        	// file has no bitstream
            MetadataInfo metadataInfo = fillBasicdaoXmlSpecification(daoXml, daoOrigFile, null, 
            		daoPage.getFileName().toString());
            protocol.add("Generování metadat souboru " + daoPage.getFileName().toString());
            if (jhoveService.generateMetadata(daoPage, protocol, destPath, metadataInfo)) {            	
            	daoOrigFile.setTechMD(parseMetadata(destPath));
            	importDao.addFile(DaoImportService.METADATA_BUNDLE, destPath);
            }
        }        		
	}

	/**
     * Process file stored in DAO folder without BITSTREAM
     * @param generatedDir
     * @param importDao
     * @param contentFile
     * @throws IOException
     */
	private DaoFile processFile(Path generatedDir, ImportDao importDao, Path contentFile) throws IOException {		
        DaoFile daoFile = new DaoFile();
        BasicFileAttributes fileAttributes = Files.readAttributes(contentFile, BasicFileAttributes.class);
        daoFile.setCreatedDate(new Date(fileAttributes.creationTime().toMillis()));
        
        String mimeType = droidService.getMimeType(contentFile, protocol);
        if (!isFileMimeTypeSupported(contentFile, mimeType)) {
        	protocol.add("Nalezen nepodporovaný soubor: " + contentFile.getFileName().toString()  + ". Soubor není importován.");
        	return null;
        }

        Path destPath;
		if (config.getMimeTypesForConversion().contains(mimeType)) {
			protocol.add("Soubor " + contentFile.getFileName().toString() + " bude převeden na požaadovaný typ ");

			String convertAppCommand = config.getConversionCommand();
			if (StringUtils.isBlank(convertAppCommand)) {
				protocol.add(
						"V konfiguraci není definován příkaz pro konverzi souborů. Nelze zahájit konverzi souboru.");
				return null;
			}

			destPath = convertFile(contentFile, generatedDir);
			mimeType = droidService.getMimeType(contentFile, protocol);
		} else {
			destPath = Paths.get(generatedDir.toString(), contentFile.getFileName().toString());
			protocol.add(
					"Kopírování souboru " + contentFile.getFileName().toString() + " do adresáře " + DaoImportService.GENERATED_DIR);
			Files.copy(contentFile, destPath, StandardCopyOption.REPLACE_EXISTING);			
		}
		daoFile.setFile(destPath);
		
        importDao.addFile(DaoImportService.ORIGINAL_BUNDLE, daoFile);
        return daoFile;
	}
	
    private MetadataInfo fillBasicdaoXmlSpecification(Dao daoXmlSpecification, DaoFile daoFile, 
    		String bitStream,
    		String fileName) {
    	if(daoXmlSpecification == null  ) {
    		return null;
    	}
    	
    	List<Pages> pagesCol = daoXmlSpecification.getPages();
    	if(CollectionUtils.isEmpty(pagesCol)) {
    		return null;
    	}
        // find metadata for bitStream
    	for(Pages pages: pagesCol) {
    		if(!Objects.equals(pages.getBitstream(), bitStream)) {
    			continue;
    		}
    		
            List<Page> pageInfoList = pages.getPage();
            for (Page pageInfo : pageInfoList) {
                if(pageInfo.getFile() != null && fileName.equals(pageInfo.getFile())) {
                	MetadataInfo metadataInfo = new MetadataInfo();
                    metadataInfo.setMimeType(pageInfo.getMimetype());
                    metadataInfo.setCheckSum(pageInfo.getChecksum());
                    daoFile.setDescription(pageInfo.getDescription());
                    return metadataInfo;
                }
            }
        }
        return null;
    }

    
    private Map<MetadataEnum, String> parseMetadata(Path mdFile) {
        try {
            SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
            TechnicalMDParser technicalMdParser = new TechnicalMDParser();
            saxParser.parse(mdFile.toFile(), technicalMdParser);
            return technicalMdParser.getMd();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("Chyba při parsování technických metadat.", e);
        } catch (SAXException e) {
            throw new IllegalStateException("Chyba při parsování technických metadat.", e);
        } catch (IOException e) {
            throw new IllegalStateException("Chyba při parsování technických metadat.", e);
        }
    }
	
    private boolean isFileMimeTypeSupported(Path contentFile, String mimeType) throws IOException {
        List<String> supportedMimeTypes = config.getSupportedMimeTypes();
        if (CollectionUtils.isEmpty(supportedMimeTypes)) {
            return true;
        }

        if (StringUtils.isBlank(mimeType)) {
            protocol.add("Nebyl zjištěn MimeType souboru " + contentFile.toAbsolutePath() +
                    " a proto nelze ověřit zda je mezi povolenými typy pro import. Soubor nebude importován.");
            return false;
        }

        boolean isSupported = supportedMimeTypes.contains(mimeType);
        if (!isSupported) {
            protocol.add("MimeType " + mimeType + " souboru " + contentFile.toAbsolutePath() +
                    " není mezi podporovanými typy. Soubor proto nebude importován.");
        }

        return isSupported;
    }
	
    private void fillDaoAttributes(ImportDao importDao) throws IOException {    	
        Map<String, String> mdForNameGeneration = new HashMap<>();

        if(daoXml!=null&&daoXml.getMeta()!=null)
        {
        	List<Attribute> attrs = daoXml.getMeta().getAttr();
			for (Attribute att : attrs) {
				try {
					MetadataField field = metadataFieldService.findByElement(context, att.getSchema(), 
							att.getName(),
							StringUtils.trimToNull(att.getQualifier()));
					StringBuilder sb = new StringBuilder(att.getSchema()).append(".").append(att.getName());
					if (StringUtils.trimToNull(att.getQualifier()) != null) { 
						sb.append(".").append(att.getQualifier()); 
					}
					String key = sb.toString();
					if (field == null) {
						throw new IllegalStateException("Neexistuje typ metadat " + key);
					}
					mdForNameGeneration.put(key, att.getValue());
					importDao.addMetadata(field.getID(), att.getValue());					
					protocol.add("Načtení atributu metadat " + key + "=" + att.getValue());
				} catch (SQLException e) {
					throw new IllegalStateException(
							"Došlo k chybě při pokusu o načtení typu metadat DAO " + importDao.getDaoId(),
							e);
				}
			}
        }
        
        if (StringUtils.isNotBlank(config.getDaoNameExpression())) {
            String name = generateDaoName(mdForNameGeneration, config.getDaoNameExpression());
            importDao.setDaoName(name);
        }
    }
    
	private String generateDaoName(Map<String, String> md, String daoNameExpression) {
        String regex = "\\$\\{[\\w\\.]+\\}";      // příklad formátu výrazu: ${dc.title} - ${dc.creator}
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(daoNameExpression);
        String name = daoNameExpression;
        while (matcher.find()) {
            String exp = matcher.group();
            String key = exp.substring(2, exp.length() - 1); // získání klíče do mapy metadat, ${dc.title} -> dc.title
            String value = StringUtils.trimToEmpty(md.get(key));
            name = name.replace(exp, value);
        }
        return name;
    }

    private Dao readDaoXml(Path daoDir, Protocol protocol) throws IOException {
        Dao daoXmlSpecification = null;
        Path daoXmlFile = Paths.get(daoDir.toAbsolutePath().toString(), DaoImportService.DAO_XML);
        if (Files.exists(daoXmlFile)) {
            protocol.add("Byl nalezen volitelný soubor dao.xml.");
            try (FileInputStream is = new FileInputStream(daoXmlFile.toFile())) {                
                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                StreamSource source = new StreamSource(is);
                daoXmlSpecification = unmarshaller.unmarshal(source, Dao.class).getValue();
            } catch (JAXBException e) {
                protocol.add("Nepodařilo se načíst data ze souboru dao.xml.");
            }
        }
        return daoXmlSpecification;
    }

    void deleteDirectoryRecursion(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
                for (Path entry : entries) {
                    deleteDirectoryRecursion(entry);
                }
            }
        }
        Files.delete(path);
    }

    private Path convertFile(Path file, Path generatedDir) throws IOException {
        String convertAppCommand = config.getConversionCommand();
        convertAppCommand = convertAppCommand.replace("{input}", "'" + file.toString()) + "'";
        Path outputPath = Paths.get(generatedDir.toString(), file.getFileName().toString());
        String output = FilenameUtils.removeExtension(outputPath.toString());
        output += config.getFileExtensionAferConversion();
        convertAppCommand = convertAppCommand.replace("{output}", "'" + output + "'");
        ProcessBuilder builder = new ProcessBuilder(convertAppCommand);
        builder.redirectErrorStream(true);
        try {
            Process p = builder.start();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            int result = r.read();
            Path convertedPath = Paths.get(output);
            if (result == 0 && Files.exists(convertedPath)) {
                protocol.add("Došlo k úspěšné konverzi souboru: " + file.getFileName().toString() + " na " + output);
                return convertedPath;
            } else {
                throw new IllegalStateException("Došlo k chybě při pokusu o konverzi souboru " + file.getFileName().toString());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Došlo k chybě při pokusu o konverzi souboru " + file.getFileName().toString());
        }
    }
    
    private void generateThumbnailFile(Path srcFile, Path destPath) {
        protocol.add("Generování náhledu souboru " + srcFile.getFileName());

        JPEGFilter jpegFilter = new JPEGFilter();

        try(InputStream is =Files.newInputStream(srcFile);
        		InputStream thumbnailIS = jpegFilter.getDestinationStream(null, is, false); 
        		) {
            Files.copy(thumbnailIS, destPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new IllegalStateException("Chyba při generování náhledu souboru " + srcFile.getFileName() + ". ", e);
        }
    }    
}
