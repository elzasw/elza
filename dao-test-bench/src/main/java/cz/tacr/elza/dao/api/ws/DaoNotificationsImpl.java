package cz.tacr.elza.dao.api.ws;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.dao.exception.DaoComponentException;
import cz.tacr.elza.dao.service.StorageDaoService;
import cz.tacr.elza.ws.dao_service.v1.DaoNotifications;
import cz.tacr.elza.ws.dao_service.v1.DaoServiceException;
import cz.tacr.elza.ws.types.v1.OnDaoLinked;
import cz.tacr.elza.ws.types.v1.OnDaoUnlinked;
import jakarta.jws.WebService;

@Service
@WebService(name = DaoNotificationsImpl.NAME,
		portName = DaoNotificationsImpl.NAME,
		serviceName = DaoNotificationsImpl.NAME,
		targetNamespace = "http://elza.tacr.cz/ws/dao-service/v1",
		endpointInterface = "cz.tacr.elza.ws.dao_service.v1.DaoNotifications")
public class DaoNotificationsImpl implements DaoNotifications {

	public static final String NAME = "DaoNotifications";

	@Autowired
	private StorageDaoService storageDaoService;

	@Override
	public void onDaoLinked(OnDaoLinked onDaoLinked) throws DaoServiceException {
		storageDaoService.checkRejectMode();
		try {
			if (onDaoLinked.getDid() == null) {
				throw new DaoComponentException("did identifier not set, daoIdentifier:" + onDaoLinked.getDaoIdentifier());
			}
			storageDaoService.linkDao(onDaoLinked.getDaoIdentifier(), onDaoLinked.getDid().getIdentifier());
		} catch (DaoComponentException e) {
			throw new DaoServiceException(e.getMessage(), e.getCause());
		}
	}

	@Override
	public void onDaoUnlinked(OnDaoUnlinked onDaoUnlinked) throws DaoServiceException {
		storageDaoService.checkRejectMode();
		try {
			storageDaoService.unlinkDao(onDaoUnlinked.getDaoIdentifier());
		} catch (DaoComponentException e) {
			throw new DaoServiceException(e.getMessage(), e.getCause());
		}
	}
}
