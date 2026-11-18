package cz.tacr.elza.service.ai;

import cz.tacr.elza.aiprovider.client.vo.DataType;

/**
 * Maps Elza data types to the data types of the AI provider contract.
 */
final class AiDataTypes {

    private AiDataTypes() {
    }

    /**
     * The contract enum declares the same codes as the domain enum. Its generated
     * {@code fromValue} does not fail on an unknown code, it returns
     * {@link DataType#UNKNOWN_DEFAULT_OPEN_API}; that case is reported here.
     *
     * @throws IllegalStateException when the contract does not know the data type
     */
    static DataType toContract(cz.tacr.elza.core.data.DataType dataType) {
        DataType result = DataType.fromValue(dataType.getCode());
        if (result == DataType.UNKNOWN_DEFAULT_OPEN_API) {
            throw new IllegalStateException("Data type " + dataType + " is not in the AI provider contract");
        }
        return result;
    }
}
