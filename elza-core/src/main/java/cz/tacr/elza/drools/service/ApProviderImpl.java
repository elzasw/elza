package cz.tacr.elza.drools.service;

import java.util.Collections;
import java.util.Objects;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.drools.model.Ap;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ApStateRepository;

public class ApProviderImpl implements ApProvider {
	
	// TODO: Implement some caching
	
	final private ApStateRepository apStateRepository;
	
	final private StaticDataProvider staticDataProvider;
	
	public ApProviderImpl(final ApStateRepository apStateRepository,
			final StaticDataProvider staticDataProvider) {
		this.apStateRepository = apStateRepository;
		this.staticDataProvider = staticDataProvider;
	}

	@Override
	public Ap getAp(Integer accessPointId) {
		ApState apState = apStateRepository.findLastByAccessPointId(accessPointId);
		if(apState==null) {
			throw new BusinessException("Access point not found", BaseCode.ID_NOT_EXIST)
				.set("accessPointId", accessPointId);
		}
		ApType apType = staticDataProvider.getApTypeById(apState.getApTypeId());
		Objects.requireNonNull(apType);
		
		Ap ap = new Ap(apState.getAccessPointId(), apType.getCode(), Collections.emptyList(), apState.getStateApproval());
		return ap;
	}

}
