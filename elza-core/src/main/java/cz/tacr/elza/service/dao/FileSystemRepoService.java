package cz.tacr.elza.service.dao;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.ElzaTools;
import cz.tacr.elza.api.DigitalRepositoryType;
import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrDao.DaoType;
import cz.tacr.elza.domain.ArrDaoPackage;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.DaoPackageRepository;
import cz.tacr.elza.repository.DaoRepository;
import cz.tacr.elza.service.ExternalSystemService;

@Service
public class FileSystemRepoService {

    public static final String FILE_URI_PREFIX = "file://";

    @Autowired
    private DaoPackageRepository daoPackageRepos;

    @Autowired
    private DaoRepository daoRepository;

    @Autowired
    private DaoServiceInternal daoServiceInternal;

    @Autowired
    private ExternalSystemService externalSystemService;

    /**
     * Creates (or reuses) the {@link ArrDao} anchor for a repository-relative path.
     * The DAO carries only the path in its {@code code}; file content is read live
     * from the repository, no per-file entities are persisted.
     */
    public ArrDao createDao(ArrDigitalRepository digiRepo, ArrFundVersion fundVersion, String itemRelatPath) {
    	itemRelatPath = normalizeRelatPath(itemRelatPath);
        Path repoPath = getPath(digiRepo, fundVersion.getFund());
        // containment check only; the resolved path itself is not stored
        resolvePath(repoPath, itemRelatPath);

        ArrDigitalRepository digiRep = externalSystemService.getDigitalRepository(digiRepo.getExternalSystemId());
        // check if package exists
        List<ArrDaoPackage> daoPackages = this.daoPackageRepos.findAllByDigitalRepositoryAndFund(digiRep, fundVersion.getFund());
        ArrDaoPackage daoPackage;
        if (CollectionUtils.isEmpty(daoPackages)) {
            // arr_dao_package.code has a global UNIQUE constraint, so the raw repository
            // path cannot be reused across funds — include the fund id in the code to
            // keep it unique per (repository, fund).
            String packageCode = repoPath.toString() + "#fund=" + fundVersion.getFundId();
            daoPackage = daoServiceInternal.createDaoPackage(fundVersion.getFund(), digiRep, packageCode, null);
        } else {
            daoPackage = daoPackages.get(0);
        }
        // Check if Dao exists
        List<ArrDao> daos = daoRepository.findDettachedByFundAndCodes(digiRep, fundVersion.getFund(),
                                                                      Collections.singletonList(itemRelatPath));

        ArrDao dao;
        if (CollectionUtils.isNotEmpty(daos)) {
            // return first available
            dao = daos.get(0);
        } else {
            // create dao
            dao = daoServiceInternal.createDao(daoPackage, itemRelatPath, itemRelatPath, null, DaoType.ATTACHMENT);
            dao = daoServiceInternal.persistDao(dao);
        }
        return dao;
    }

    /**
     * Repository-relative paths are stored and compared with forward slashes,
     * regardless of the OS the server runs on. Call at every write site so
     * DB values stay consistent.
     */
    public static String normalizeRelatPath(String path) {
        return path == null ? null : path.replace('\\', '/');
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
