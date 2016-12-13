package cz.tacr.elza.utils;

import ma.glasnost.orika.converter.BidirectionalConverter;
import ma.glasnost.orika.metadata.Type;

import java.time.LocalDateTime;

// TODO Lebeda - OK?
/**
 *
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 13.12.16
 */
public class LocalDateTimeConverter extends BidirectionalConverter<LocalDateTime, LocalDateTime> {
    @Override
    public LocalDateTime convertTo(LocalDateTime source, Type<LocalDateTime> destinationType) {
        return LocalDateTime.from(source);
    }

    @Override
    public LocalDateTime convertFrom(LocalDateTime source, Type<LocalDateTime> destinationType) {
        return LocalDateTime.from(source);
    }
}
