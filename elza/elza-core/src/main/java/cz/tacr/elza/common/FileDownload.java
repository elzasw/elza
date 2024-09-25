package cz.tacr.elza.common;

import java.util.regex.Pattern;
import jakarta.servlet.http.HttpServletResponse;


import org.apache.commons.lang3.StringUtils;

/**
 * Helper methods to prepare name of downloaded file
 * and set headers
 *
 */
public abstract class FileDownload {
    public static final String CONTENT_DISPOSITION = "Content-Disposition";
    
    private static final Pattern UNSUPPORTED_CHARS = Pattern.compile("[^ a-zA-Z0-9-_\\.]");

    /**
     * Add instructions to the servlet to download file as attachment
     *
     * @param response
     * @param filename
     */
    public static void addContentDispositionAsAttachment(HttpServletResponse response, String srcFilename) {
     
        String fileName = StringUtils.stripAccents(srcFilename);
        
        fileName = UNSUPPORTED_CHARS.matcher(fileName.trim()).replaceAll("_");
        
        if(StringUtils.isBlank(fileName)) {
        	fileName = "download.bin";
        }

        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
    }
}
