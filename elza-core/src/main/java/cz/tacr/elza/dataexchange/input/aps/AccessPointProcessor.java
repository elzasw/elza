package cz.tacr.elza.dataexchange.input.aps;

import cz.tacr.elza.domain.ApDescription;
import cz.tacr.elza.domain.ApName;
import cz.tacr.elza.domain.ApNameType;

import java.util.Iterator;
import org.apache.commons.lang3.StringUtils;

import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.schema.v2.AccessPoint;
import cz.tacr.elza.schema.v2.AccessPointName;
import cz.tacr.elza.schema.v2.AccessPointtNames;

/**
 * Processing access points. Implementation is not thread-safe.
 */
public class AccessPointProcessor extends AccessPointEntryProcessor {

	public AccessPointProcessor(ImportContext context) {
		super(context, false);
	}

	@Override
	public void process(Object item) {
		AccessPoint ap = (AccessPoint) item;
		processEntry(ap.getApe());
		processDesc(ap.getChr());
		processNames(ap.getNms());
	}

	private void processDesc(String value) {
		if (StringUtils.isEmpty(value)) {
			return;
		}
		ApDescription apDesc = new ApDescription();
		apDesc.setCreateChange(context.getCreateChange());
		apDesc.setDescription(value);
		context.addDescription(apDesc, info);
	}

	private void processNames(AccessPointtNames names) {
		if (names == null || names.getNm().isEmpty()) {
			throw new DEImportException("AP preferred name not found, apeId=" + info.getEntryId());
		}
		Iterator<AccessPointName> it = names.getNm().iterator();
		processName(it.next(), true);
		while (it.hasNext()) {
			processName(it.next(), false);
		}
	}

	private void processName(AccessPointName name, boolean preferred) {
		if (StringUtils.isEmpty(name.getN())) {
			throw new DEImportException("AP name without value, apeId=" + info.getEntryId());
		}
		if (!context.isValidLanguage(name.getL())) {
			throw new DEImportException(
					"AP name has invalid language apeId=" + info.getEntryId() + ", code=" + name.getL());
		}
		ApNameType nameType = context.getNameTypeByCode(name.getT());
		if (nameType == null) {
			throw new DEImportException("AP name type not found, apeId=" + info.getEntryId() + ", code=" + name.getT());
		}
		// create name
		ApName apName = new ApName();
		apName.setComplement(name.getCpl());
		apName.setCreateChange(context.getCreateChange());
		apName.setLanguage(name.getL());
		apName.setName(name.getN());
		apName.setNameType(nameType);
		apName.setPreferredName(preferred);
		context.addName(apName, info);
	}
}
