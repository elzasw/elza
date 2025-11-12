package cz.tacr.elza.repository.vo;

public class DataIdTypeId {

	final Integer dataId;

	final Integer typeId;

	public DataIdTypeId(final Integer dataId, final Integer typeId) {
		this.dataId = dataId;
		this.typeId = typeId;
	}

	public Integer getDataId() {
		return dataId;
	}

	public Integer getTypeId() {
		return typeId;
	}
}
