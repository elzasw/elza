package cz.tacr.elza.dao.api.storage;

import java.io.IOException;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.dao.DCStorageConfig;
import cz.tacr.elza.dao.bo.DaoFileBo;
import cz.tacr.elza.dao.bo.resource.DaoFileInfo;
import cz.tacr.elza.dao.service.ResourceService;

@RestController
@RequestMapping(value = "/file")
public class FileController {

	@Autowired
	private ResourceService dcsResourceService;

	/**
	 * Returns file based on unique dao file identifier which is formatted as "package/dao/file".
	 * (original file extensions are preserved as part of file identifier)
	 */
	@RequestMapping(value = "/{packageIdentifier}/{daoIdentifier}/{fileIdentifier:.+}", method = RequestMethod.GET)
	public ResponseEntity<Resource> downloadDaoFile(
			@PathVariable String packageIdentifier,
			@PathVariable String daoIdentifier,
			@PathVariable String fileIdentifier) throws IOException {
		DaoFileBo daoFile = dcsResourceService.getDaoFile(packageIdentifier, daoIdentifier, fileIdentifier);
		DaoFileInfo fileInfo = daoFile.getFileInfo();

		MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
		if (fileInfo.getMimeType() != null) {
			mediaType = MediaType.parseMediaType(fileInfo.getMimeType());
		}
		return ResponseEntity
				.ok()
				.contentLength(fileInfo.getSize())
				.contentType(mediaType)
				.body(new PathResource(fileInfo.getFilePath()));
	}

	/**
	 * Returns HTML formatted page which contains files for unique dao identifier formatted as "package/dao".
	 */
	@RequestMapping(value = "/{packageIdentifier}/{daoIdentifier}", produces = "text/html;charset=UTF-8", method = RequestMethod.GET)
	public String listDaoFiles(
			@PathVariable String packageIdentifier,
			@PathVariable String daoIdentifier,
			Model model)
			throws IOException {
		Collection<DaoFileBo> daoFiles = dcsResourceService.getDaoFiles(packageIdentifier, daoIdentifier);

		StringBuilder sb = new StringBuilder("<!DOCTYPE html><head><meta charset='UTF-8'/><title>Dao files</title>");
		sb.append("<style>th{text-align:left}td{padding-right:60px;}</style></head>");
		sb.append("<body><div style='white-space:nowrap'>repositoryIdentifier: ");
		sb.append(DCStorageConfig.get().getRepositoryIdentifier());
		sb.append("<br/>packageIdentifier: ");
		sb.append(packageIdentifier);
		sb.append("<br/>daoIdentifier: ");
		sb.append(daoIdentifier);
		sb.append("</div><br/><table><tr><th>Files</th><th>Created</th><th>Size[kB]</th></tr>");
		for (DaoFileBo daoFile : daoFiles) {
			sb.append("<tr><td><a href='/");
			sb.append(DCStorageConfig.get().getRepositoryIdentifier());
			sb.append("/file/");
			sb.append(packageIdentifier);
			sb.append('/');
			sb.append(daoIdentifier);
			sb.append('/');
			sb.append(daoFile.getIdentifier());
			sb.append("' target='_blank'>");
			sb.append(daoFile.getIdentifier());
			sb.append("</a></td><td>");
			sb.append(daoFile.getFileInfo().getCreated());
			sb.append("</td><td>");
			sb.append(daoFile.getFileInfo().getSize() / 1000f);
			sb.append("</td></tr>");
		}
		sb.append("</table><body></html>");
		return sb.toString();
	}
}