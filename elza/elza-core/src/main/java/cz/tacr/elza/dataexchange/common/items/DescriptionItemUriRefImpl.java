package cz.tacr.elza.dataexchange.common.items;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.domain.ArrDataUriRef;
import cz.tacr.elza.schema.v2.DescriptionItemUriRef;

import java.net.URI;

public class DescriptionItemUriRefImpl extends DescriptionItemUriRef {

    @Override
    public ImportableItemData createData(ImportContext context, DataType dataType) {
        if (dataType != DataType.URI_REF) {
            throw new DEImportException("Unsupported data type: " + dataType);
        }

        String uri = getUri();
        String scheme = getSchm();
        String description = getLbl();

        ArrDataUriRef data = null;
        if (uri != null && scheme != null) {
            URI tempUri = URI.create(uri).normalize();
            String uriScheme = tempUri.getScheme();
            if (!scheme.equalsIgnoreCase(tempUri.getScheme())) {
                throw new IllegalArgumentException("Rozdílné schema: " + scheme + " " + uriScheme + " (" + uri + ")");
            }
            // arr node je null, bude se doplňovat v post processu importu, kdy již budou založené všechny JP
            data = new ArrDataUriRef(uriScheme, uri, description, null);
            data.setDataType(dataType.getEntity());
        }

        return new ImportableItemData(data);
    }


}
