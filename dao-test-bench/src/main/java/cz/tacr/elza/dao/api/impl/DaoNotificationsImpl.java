package cz.tacr.elza.dao.api.impl;

import javax.jws.WebService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.dao.exception.DaoComponentException;
import cz.tacr.elza.dao.service.DcsDaoService;
import cz.tacr.elza.ws.dao_service.v1.DaoNotifications;
import cz.tacr.elza.ws.dao_service.v1.DaoServiceException;
import cz.tacr.elza.ws.types.v1.OnDaoLinked;
import cz.tacr.elza.ws.types.v1.OnDaoUnlinked;

@Service
@WebService(endpointInterface = "cz.tacr.elza.ws.dao_service.v1.DaoNotifications")
public class DaoNotificationsImpl implements DaoNotifications {

	@Autowired
	private DcsDaoService dcsDaoService;

	@Override
	public void onDaoLinked(OnDaoLinked onDaoLinked) throws DaoServiceException {
		dcsDaoService.checkRejectMode();
		try {
			dcsDaoService.linkDao(onDaoLinked.getDaoIdentifier(), onDaoLinked.getDid().getIdentifier());
		} catch (DaoComponentException e) {
			throw new DaoServiceException(e.getMessage(), e.getCause());
		}
	}

	@Override
	public void onDaoUnlinked(OnDaoUnlinked onDaoUnlinked) throws DaoServiceException {
		dcsDaoService.checkRejectMode();
		try {
			dcsDaoService.unlinkDao(onDaoUnlinked.getDaoIdentifier());
		} catch (DaoComponentException e) {
			throw new DaoServiceException(e.getMessage(), e.getCause());
		}
	}
}