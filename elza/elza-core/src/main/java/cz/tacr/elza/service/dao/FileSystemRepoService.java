package cz.tacr.elza.service.dao;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import cz.tacr.elza.ElzaTools;
import cz.tacr.elza.api.DigitalRepositoryType;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;

@Service
public class FileSystemRepoService {

    public static final String FILE_URI_PREFIX = "file://";

    /**
     * Parameter placeholder in a repository URL, e.g. {@code {fundId}}. Substituted
     * by {@link #getPath(ArrDigitalRepository, ArrFund)}; see
     * {@link ElzaTools#bindingUrlParams(String, ElzaTools.UrlParams)}.
     */
    private static final Pattern URL_PARAM_PATTERN = Pattern.compile("\\{[a-zA-Z]\\w*\\}");

    /**
     * Repository-relative paths are stored and compared with forward slashes,
     * regardless of the OS the server runs on. Call at every write site so
     * DB values stay consistent.
     */
    public static String normalizeRelatPath(String path) {
        return path == null ? null : path.replace('\\', '/');
    }

    /**
     * True when the repository root depends on the fund it is browsed for, i.e. the
     * configured URL contains parameter placeholders.
     */
    public static boolean isTemplatedUrl(String url) {
        return url != null && URL_PARAM_PATTERN.matcher(url).find();
    }

    /**
     * Fixed part of a (possibly templated) repository URL — everything before the path
     * segment that carries the first parameter. For a URL without parameters this is the
     * whole URL. Used to validate as much of a fund dependent configuration as possible.
     */
    public static String getFixedUrlPrefix(String url) {
        if (url == null) {
            return null;
        }
        Matcher matcher = URL_PARAM_PATTERN.matcher(url);
        if (!matcher.find()) {
            return url;
        }
        String head = url.substring(0, matcher.start());
        int lastSeparator = Math.max(head.lastIndexOf('/'), head.lastIndexOf('\\'));
        return lastSeparator <= 0 ? head : head.substring(0, lastSeparator);
    }

    public static String getRelatPath(Path rootPath, Path itemPath) {
        return normalizeRelatPath(rootPath.relativize(itemPath).toString());
    }

    public String getMimetype(Path fp) {
        try {
            String type = Files.probeContentType(fp);
            if (type != null) {
                return type;
            }
        } catch (IOException e) {
        	throw new BusinessException("Failed detecting file type, path: " + fp, e, BaseCode.INVALID_STATE);
        }
        return getMimetype(fp.getFileName().toString());
    }

    public String getMimetype(String name) {
	    String type = URLConnection.guessContentTypeFromName(name);
	    if (type != null) {
	        return type;
	    }
	    String ext = FilenameUtils.getExtension(name).toLowerCase();
	    switch (ext) {
	        case "jpg":
	        case "jpeg": return "image/jpeg";
	        case "png":  return "image/png";
	        case "tif":
	        case "tiff": return "image/tiff";
	        case "gif":  return "image/gif";
	        case "webp": return "image/webp";
	        case "bmp":  return "image/bmp";
	        case "pdf":  return "application/pdf";
	        case "txt":  return "text/plain";
	        case "xml":  return "application/xml";
	        case "json": return "application/json";
	        default:     return null;
	    }
    }

    public boolean isFileSystemRepository(ArrDigitalRepository digiRep) {
    	return digiRep.getDigitalRepositoryType() == DigitalRepositoryType.FILESYSTEM;
    }

    public InputStream getInputStream(ArrDigitalRepository digiRepo, String filePath) throws IOException {
        Path fp = resolvePath(digiRepo, filePath);
        return Files.newInputStream(fp);
    }

    public Path resolvePath(ArrDigitalRepository digiRepo, String filePath) {
        if (!isFileSystemRepository(digiRepo)) {
            throw new BusinessException("Not a FileSystemRepository", BaseCode.INVALID_STATE)
                    .set("RepositoryId", digiRepo.getExternalSystemId());
        }
        String repoPath = digiRepo.getUrl();
        if (StringUtils.isBlank(repoPath)) {
            throw new BusinessException("Repository URL is not configured", BaseCode.INVALID_STATE)
                    .set("RepositoryId", digiRepo.getExternalSystemId());
        }
        Path rootPath = Paths.get(repoPath).toAbsolutePath();
        return resolveInsideRoot(rootPath, filePath);
    }

    public Path getPath(ArrDigitalRepository digiRepo, ArrFund fund) {
    	if (StringUtils.isBlank(digiRepo.getUrl())) {
    	    throw new BusinessException("Repository URL is not configured", BaseCode.INVALID_STATE)
    	            .set("RepositoryId", digiRepo.getExternalSystemId());
    	}
        if (!isFileSystemRepository(digiRepo)) {
            throw new BusinessException("Not a FileSystemRepository", BaseCode.INVALID_STATE)
                    .set("RepositoryId", digiRepo.getExternalSystemId());
        }
        String repoPath = digiRepo.getUrl();

        ElzaTools.UrlParams params = ElzaTools.createUrlParams()
                .add("repoId", digiRepo.getExternalSystemId())
                .add("repoCode", digiRepo.getElzaCode())
                .add("repoElzaCode", digiRepo.getElzaCode());
        if (fund != null) {
            params.add("fundId", fund.getFundId())
                    .add("fundNumber", fund.getFundNumber())
                    .add("fundCode", fund.getInternalCode())
                    .add("fundMark", fund.getMark())
                    .add("institutionId", fund.getInstitution().getInstitutionId())
                    .add("institutionCode", fund.getInstitution().getInternalCode());
        }
        repoPath = ElzaTools.bindingUrlParams(repoPath, params);
        Path path = Paths.get(repoPath).toAbsolutePath();
        return path;
    }

    public Path resolvePath(ArrDigitalRepository digiRepo, ArrFund fund, String itemPath) {
        Path rootPath = getPath(digiRepo, fund);
        // check if dir exists
        if (!Files.isDirectory(rootPath)) {
            throw new BusinessException("Repository is not vailable", BaseCode.INVALID_STATE)
                    .set("repoPath", rootPath)
                    .set("fundId", fund.getFundId())
                    .set("fsrepoId", digiRepo.getExternalSystemId());
        }

        return resolvePath(rootPath, itemPath);
    }

    public Path resolvePath(Path rootRepoPath, String itemPath) {
    	return resolveInsideRoot(rootRepoPath, itemPath);
    }

    /**
     * Resolves a repository-relative path against a root and asserts the result
     * stays inside that root. Blocks directory traversal ("../foo") and absolute
     * paths that would replace the root ("/etc/passwd").
     */
    private Path resolveInsideRoot(Path rootPath, String itemPath) {
        Path normalizedRoot = rootPath.normalize();
        if (StringUtils.isBlank(itemPath)) {
            return normalizedRoot;
        }
        Path resolved = normalizedRoot.resolve(itemPath).normalize();
        if (!resolved.startsWith(normalizedRoot)) {
            throw new BusinessException("Path escapes repository root", BaseCode.INVALID_STATE)
                    .set("root", normalizedRoot.toString())
                    .set("requested", itemPath);
        }
        return resolved;
    }

    public static boolean isInlineRenderable(String contentType) {
        if (contentType == null) {
            return false;
        }
        String lower = contentType.toLowerCase();
        return lower.startsWith("image/")
            || lower.equals("application/pdf")
            || lower.startsWith("text/");
    }
}
