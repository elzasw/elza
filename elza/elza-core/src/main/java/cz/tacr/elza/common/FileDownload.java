package cz.tacr.elza.common;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import jakarta.servlet.http.HttpServletResponse;


import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;

/**
 * Helper methods to prepare name of downloaded file
 * and set headers
 *
 */
public abstract class FileDownload {
        
    private static final Pattern UNSUPPORTED_CHARS = Pattern.compile("[^ a-zA-Z0-9-_\\.]");
    
    public static String getAsAttachment(String srcFilename, boolean stripAccents) {
    	
    	if(stripAccents) {
    		String fileName = StringUtils.stripAccents(srcFilename);
        
    		fileName = UNSUPPORTED_CHARS.matcher(fileName.trim()).replaceAll("_");
            if(StringUtils.isBlank(fileName)) {
            	fileName = "download.bin";
            } 
            return "attachment; filename=" + fileName;
    	} else {
    		// encode as UTF-8
    		String encodedFileName = URLEncoder.encode(srcFilename, StandardCharsets.UTF_8).replace("+", "%20");
            if(StringUtils.isBlank(encodedFileName)) {
            	encodedFileName = "download.bin";
            } 
    		return "attachment; filename*=UTF-8''" + encodedFileName;
    	}
        
    }

    /**
     * Add instructions to the servlet to download file as attachment
     *
     * @param response
     * @param filename
     */
    public static void addContentDispositionAsAttachment(HttpServletResponse response, String srcFilename) {
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, getAsAttachment(srcFilename, false));
    }
    
    public static void addContentDispositionAsAttachment(HttpHeaders headers, String srcFilename) {
    	headers.add(HttpHeaders.CONTENT_DISPOSITION, getAsAttachment(srcFilename, false));
    }
}
