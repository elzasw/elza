package cz.tacr.elza.controller;

import java.util.List;

import javax.transaction.Transactional;
import javax.validation.Valid;

import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.controller.vo.ExtSystemProperty;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.service.ExternalSystemService;
import cz.tacr.elza.service.UserService;

@RestController
@RequestMapping("/api/v1")
public class ExternalSystemController implements ExternalsystemsApi {

	@Autowired
	ExternalSystemService extSystemService;
	
	@Autowired
	UserService userService;

	@Override
    @AuthMethod(permission = { UsrPermission.Permission.ADMIN })
	public ResponseEntity<List<ExtSystemProperty>> externalSystemAllProperties(Integer extSystemId, @Valid Integer userId) {
		List<ExtSystemProperty> properties = extSystemService.findAllProperties(extSystemId, userId);

		return ResponseEntity.ok(properties);
	}

	@Override
	@Transactional
    @AuthMethod(permission = { UsrPermission.Permission.ADMIN })
	public ResponseEntity<Void> externalSystemAddProperty(Integer extSystemId, @Valid ExtSystemProperty extSystemProperty) {
		Validate.notNull(extSystemProperty, "ExtSystemProperty shouldn't be null");
		Validate.notNull(extSystemProperty.getName(), "ExtSystemProperty.name shouldn't be null");
		Validate.notNull(extSystemProperty.getValue(), "ExtSystemProperty.value shouldn't be null");

		ApExternalSystem extSystem = extSystemService.findApExternalSystemById(extSystemId);
		UsrUser user = userService.getUserInternal(extSystemProperty.getUserId());

		extSystemService.addProperty(extSystem, user, extSystemProperty);

		return ResponseEntity.ok().build();
	}

	@Override
	@Transactional
    @AuthMethod(permission = { UsrPermission.Permission.ADMIN })
	public ResponseEntity<Void> externalSystemDeleteProperty(Integer extSysPropertyId) {
		extSystemService.deleteProperty(extSysPropertyId);

		return ResponseEntity.ok().build();
	}
}
