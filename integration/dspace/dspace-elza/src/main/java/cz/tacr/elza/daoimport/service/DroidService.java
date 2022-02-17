package cz.tacr.elza.daoimport.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.stereotype.Service;

import cz.tacr.elza.daoimport.protocol.Protocol;

/*
 * import uk.gov.nationalarchives.droid.core.BinarySignatureIdentifier;
 * import uk.gov.nationalarchives.droid.core.SignatureParseException;
 * import uk.gov.nationalarchives.droid.core.interfaces.IdentificationRequest;
 * import uk.gov.nationalarchives.droid.core.interfaces.IdentificationResult;
 * import
 * uk.gov.nationalarchives.droid.core.interfaces.IdentificationResultCollection;
 * import uk.gov.nationalarchives.droid.core.interfaces.RequestIdentifier;
 * import uk.gov.nationalarchives.droid.core.interfaces.resource.
 * FileSystemIdentificationRequest;
 * import
 * uk.gov.nationalarchives.droid.core.interfaces.resource.RequestMetaData;
 */

@Service
public class DroidService {

    private static Logger log = Logger.getLogger(DroidService.class);

    //private BinarySignatureIdentifier droidCore;

    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    public String getMimeType(final Path file, final Protocol protocol) throws IOException {
        String mimeType = null;
        
        // Simplified mimetype detection from extension
        String fileName = file.getFileName().toString().toLowerCase();
        if(fileName.endsWith("jpg")||fileName.endsWith("jpeg")) {
        	return "image/jpeg";
        }
        
        if(fileName.endsWith("tif")||fileName.endsWith("tiff")) {
        	return "image/tiff";
        }

        /*
        if (droidCore == null) {
            log.warn("Droid nebyl inicializován a proto nejde určite MimeType.");
            return  mimeType;
        }
        
        try {
            File f = file.toFile();
            RequestMetaData metadata = new RequestMetaData(f.length(), f.lastModified(), f.getAbsolutePath());
        
            URI uri = new URI("");
            RequestIdentifier identifier = new RequestIdentifier(uri);
            IdentificationRequest request = new FileSystemIdentificationRequest(metadata, identifier);
            request.open(f);
            IdentificationResultCollection identificationResultCollection = droidCore.matchBinarySignatures(request);
            request.close();
        
            List<IdentificationResult> results = identificationResultCollection.getResults();
            if (results.isEmpty()) {
                protocol.write("Nepodařilo se zjistit MimeType souboru " + file.toAbsolutePath());
                protocol.newLine();
            } else {
                mimeType = results.iterator().next().getMimeType();
        
                protocol.write("MimeType souboru " + file.toAbsolutePath() + " byl určen jako " + mimeType);
                protocol.newLine();
            }
        } catch (IOException|URISyntaxException e) {
            protocol.write("Nastala chyba při zjišťování MimeType souboru " + file.toAbsolutePath());
            protocol.write(ExceptionUtils.getStackTrace(e));
            protocol.newLine();
        }*/

        return mimeType;
    }

    /*
    @PostConstruct
    private void init() {
        String signatureFile = configurationService.getProperty("elza.droid.signatureFile");
        if (StringUtils.isBlank(signatureFile)) {
            log.warn("Není vyplněna hodnota elza.droid.signatureFile v souboru elza.cfg.");
        } else {
            try {
                droidCore = new BinarySignatureIdentifier();
                droidCore.setSignatureFile(signatureFile);
                droidCore.init();
            } catch (Exception e) {
                log.error("Chyba při inicializaci DROID.", e);
            }
        }
    }*/
}
