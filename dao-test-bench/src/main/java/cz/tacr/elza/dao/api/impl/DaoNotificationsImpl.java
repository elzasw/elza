package cz.tacr.elza.dao.api.impl;

import java.io.IOException;

import javax.jws.WebService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.dao.service.DaoNotificationService;
import cz.tacr.elza.ws.dao_service.v1.DaoNotifications;
import cz.tacr.elza.ws.dao_service.v1.DaoServiceException;
import cz.tacr.elza.ws.types.v1.OnDaoLinked;
import cz.tacr.elza.ws.types.v1.OnDaoUnlinked;

@Service
@WebService(endpointInterface = "cz.tacr.elza.ws.dao_service.v1.DaoNotifications")
public class DaoNotificationsImpl implements DaoNotifications {

	@Autowired
	private DaoNotificationService notificationService;

	@Override
	public void onDaoLinked(OnDaoLinked onDaoLinked) throws DaoServiceException {
		notificationService.checkRejectMode();
		try {
			notificationService.linkDao(onDaoLinked.getDaoIdentifier(), onDaoLinked.getDid().getIdentifier());
		} catch (IOException e) {
			throw new DaoServiceException(e.getMessage());
		}
	}

	@Override
	public void onDaoUnlinked(OnDaoUnlinked onDaoUnlinked) throws DaoServiceException {
		notificationService.checkRejectMode();
		try {
			notificationService.unlinkDao(onDaoUnlinked.getDaoIdentifier());
		} catch (IOException e) {
			throw new DaoServiceException(e.getMessage());
		}
	}
}