package cz.tacr.elza.dao.api.impl;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.dao.service.DcsResourceService;
import cz.tacr.elza.dao.service.DcsResourceService.DaoFileResource;

@RestController
@RequestMapping(value = "/dl")
public class DcsResourceController {

	@Autowired
	private DcsResourceService resourceService;

	@RequestMapping(value = "/{packageIdentifier}/{daoIdentifier}/{fileIdentifier:.+}", method = RequestMethod.GET)
	public ResponseEntity<InputStreamResource> downloadFile(
			@PathVariable String packageIdentifier,
			@PathVariable String daoIdentifier,
			@PathVariable String fileIdentifier) throws IOException {

		DaoFileResource resource = resourceService.getFileResource(packageIdentifier, daoIdentifier, fileIdentifier);
		if (resource == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.set(HttpHeaders.CONTENT_TYPE, resource.getMimeType() == null
				? MediaType.APPLICATION_OCTET_STREAM_VALUE : resource.getMimeType());
		if (resource.getSize() == null) {
			httpHeaders.set(HttpHeaders.TRANSFER_ENCODING, "chunked");
		} else {
			httpHeaders.setContentLength(resource.getSize());
		}
		httpHeaders.setContentDispositionFormData("attachment", resource.getFileName());

		InputStreamResource isr = new InputStreamResource(resource.getInputStream());
		return new ResponseEntity<InputStreamResource>(isr, httpHeaders, HttpStatus.OK);
	}
}
