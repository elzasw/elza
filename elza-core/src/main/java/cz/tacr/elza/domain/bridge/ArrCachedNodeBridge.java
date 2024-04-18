package cz.tacr.elza.domain.bridge;

import static cz.tacr.elza.domain.ArrDescItem.FIELD_FUND_ID;
import static cz.tacr.elza.domain.ArrDescItem.FULLTEXT_ATT;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import cz.tacr.elza.domain.ArrCachedNode;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.service.cache.AccessPointCacheSerializable;
import cz.tacr.elza.service.cache.ApVisibilityChecker;
import cz.tacr.elza.service.cache.CachedNode;

public class ArrCachedNodeBridge implements TypeBridge<ArrCachedNode> {

	@Override
	public void write(DocumentElement document, ArrCachedNode arrCachedNode, TypeBridgeWriteContext context) {

		final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.setVisibility(new ApVisibilityChecker(AccessPointCacheSerializable.class,
                String.class, Number.class, Boolean.class, Iterable.class,
                LocalDate.class, LocalDateTime.class));

        try {

            CachedNode cachedNode = mapper.readValue(arrCachedNode.getData(), CachedNode.class);
        	document.addValue(FIELD_FUND_ID, cachedNode.getFundId());
        	if (cachedNode.getDescItems() != null) {
	            for (ArrDescItem item : cachedNode.getDescItems()) {
	            	String fullTextValue = item.getFulltextValue();
	            	if (fullTextValue != null) {
	            		document.addValue(FULLTEXT_ATT, fullTextValue);
	            	}
	            }
        	}

        } catch (IOException e) {
            throw new SystemException("Nastal problém při deserializaci objektu", e);
        }
	}
}
