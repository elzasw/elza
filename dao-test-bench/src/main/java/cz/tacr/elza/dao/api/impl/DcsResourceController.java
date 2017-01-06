package cz.tacr.elza.dao.api.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.dao.bo.DaoFileBo;
import cz.tacr.elza.dao.bo.resource.DaoFileInfo;
import cz.tacr.elza.dao.service.DcsResourceService;

@RestController
@RequestMapping(value = "/dl")
public class DcsResourceController {

	@Autowired
	private DcsResourceService resourceService;

	@RequestMapping(value = "/{packageIdentifier}/{daoIdentifier}/{fileIdentifier:.+}", method = RequestMethod.GET)
	public ResponseEntity<InputStreamResource> downloadDaoFile(
			@PathVariable String packageIdentifier,
			@PathVariable String daoIdentifier,
			@PathVariable String fileIdentifier) throws IOException {
		DaoFileBo daoFile = resourceService.getDaoFile(packageIdentifier, daoIdentifier, fileIdentifier);
		DaoFileInfo fileInfo = daoFile.getFileInfo();

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.set(HttpHeaders.CONTENT_TYPE, fileInfo.getMimeType() == null
				? MediaType.APPLICATION_OCTET_STREAM_VALUE : fileInfo.getMimeType());
		if (fileInfo.getSize() == null) {
			httpHeaders.set(HttpHeaders.TRANSFER_ENCODING, "chunked");
		} else {
			httpHeaders.setContentLength(fileInfo.getSize());
		}
		httpHeaders.setContentDispositionFormData("attachment", daoFile.getIdentifier());

		InputStreamResource isr = new InputStreamResource(Files.newInputStream(fileInfo.getFilePath()));
		return new ResponseEntity<>(isr, httpHeaders, HttpStatus.OK);
	}

	@RequestMapping(value = "/{packageIdentifier}/{daoIdentifier}", method = RequestMethod.GET)
	public void browseDaoFolder(
			@PathVariable String packageIdentifier,
			@PathVariable String daoIdentifier,
			Model model) throws IOException {
		model.addAttribute("packageIdentifier", packageIdentifier);
        model.addAttribute("daoIdentifier", daoIdentifier);

        Collection<DaoFileBo> daoFiles = resourceService.getDao(packageIdentifier, daoIdentifier);
        model.addAttribute("files", daoFiles.stream().map(f -> f.getIdentifier()).collect(Collectors.toList()));
	}
}
