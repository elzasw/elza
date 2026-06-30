package cz.tacr.elza.ws.core.v1;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataNull;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataStructureRef;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrStructuredObject;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.exception.AbstractException;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.ErrorCode;
import cz.tacr.elza.service.ArrangementInternalService;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.StructObjService;
import cz.tacr.elza.ws.types.v1.ErrorDescription;
import cz.tacr.elza.ws.types.v1.FundIdentifiers;
import cz.tacr.elza.ws.types.v1.ItemEnum;
import cz.tacr.elza.ws.types.v1.ItemLong;
import cz.tacr.elza.ws.types.v1.ItemString;

@Component
public class WSHelper {

    final private static Logger logger = LoggerFactory.getLogger(WSHelper.class);

    @Autowired
    ArrangementInternalService arrangementIntService;

    @Autowired
    ArrangementService arrangementService;

    @Autowired
    StaticDataService staticDataService;

    @Autowired
    StructObjService structObjService;

    public Integer getFundId(FundIdentifiers fundInfo) {
        Objects.requireNonNull(fundInfo);
        if (fundInfo.getId() != null) {
            return Integer.valueOf(fundInfo.getId());
        } else {
            Validate.notNull(fundInfo.getUuid(), "Fund ID or UUID have to be specified");
            ArrNode node = arrangementIntService.findNodeByUuid(fundInfo.getUuid());
            if(node==null) {
            	logger.error("Fund not found, UUID: {}", fundInfo.getUuid());
            	throw new BusinessException("Fund not found, UUID: "+fundInfo.getUuid(), BaseCode.ID_NOT_EXIST)
            		.set("uuid", fundInfo.getUuid());
            }
            return node.getFundId();
        }
    }

    public ArrFund getFund(FundIdentifiers fundInfo) {
        Integer fundId = getFundId(fundInfo);
        ArrFund fund = arrangementService.getFund(fundId);
        return fund;
    }

    public void convertItem(ArrItem trgItem, Object srcItem) {
        if (srcItem instanceof ItemString) {
            convertItemString(trgItem, (ItemString) srcItem);
        } else if (srcItem instanceof ItemLong) {
            convertItemLong(trgItem, (ItemLong) srcItem);
        } else if (srcItem instanceof ItemEnum) {
            convertItemEnum(trgItem, (ItemEnum) srcItem);
        }
        else {
            Validate.isTrue(false, "Cannot convert srcItem to trgItem: %s", srcItem);
        }
        
    }

    private void convertItemEnum(ArrItem trgItem, ItemEnum srcItem) {
        ItemType itemType = prepareItem(trgItem, srcItem.getType(), srcItem.getSpec(), srcItem.isReadOnly());
        ArrData data = null;
        switch (itemType.getDataType()) {
        case ENUM:
            ArrDataNull dn = new ArrDataNull();
            data = dn;
            break;
        default:
            Validate.isTrue(false, "Cannot convert enum to data type: %s, item type: %s", itemType.getDataType(),
                            srcItem.getType());
        }
        data.setDataType(itemType.getDataType().getEntity());
        trgItem.setData(data);

    }

    /**
     * Convert long item to ArrItem
     * 
     * @param trgItem
     * @param srcItem
     */
    private void convertItemLong(ArrItem trgItem, ItemLong srcItem) {
        ItemType itemType = prepareItem(trgItem, srcItem.getType(), srcItem.getSpec(), srcItem.isReadOnly());
        ArrData data = null;
        switch (itemType.getDataType()) {
        case STRING:
            ArrDataString ds = new ArrDataString();
            ds.setStringValue(Long.toString(srcItem.getValue()));
            data = ds;
            break;
        case TEXT:
            ArrDataText dt = new ArrDataText();
            dt.setTextValue(Long.toString(srcItem.getValue()));
            data = dt;
            break;
        case INT:
            ArrDataInteger di = new ArrDataInteger();
            di.setIntegerValue((int) srcItem.getValue());
            data = di;
            break;
        default:
            Validate.isTrue(false, "Cannot convert long to data type: %s, item type: %s", itemType.getDataType(),
                            srcItem.getType());
        }
        data.setDataType(itemType.getDataType().getEntity());
        trgItem.setData(data);
    }

    /**
     * Convert string item to ArrItem
     * 
     * @param trgItem
     * @param srcItem
     */
    private void convertItemString(ArrItem trgItem, ItemString srcItem) {
        ItemType itemType = prepareItem(trgItem, srcItem.getType(), srcItem.getSpec(), srcItem.isReadOnly());
        ArrData data = null;
        switch (itemType.getDataType()) {
        case STRING:
            ArrDataString ds = new ArrDataString();
            ds.setStringValue(srcItem.getValue());
            data = ds;
            break;
        case TEXT:
            ArrDataText dt = new ArrDataText();
            dt.setTextValue(srcItem.getValue());
            data = dt;
            break;
        case INT:
            ArrDataInteger di = new ArrDataInteger();
            di.setIntegerValue(Integer.valueOf(srcItem.getValue()));
            data = di;
            break;
        case UNITDATE:
            ArrDataUnitdate du = ArrDataUnitdate.valueOf(srcItem.getValue());
            data = du;
            break;
        case STRUCTURED:
            logger.debug("Finding structured object, uuid: {}", srcItem.getValue());
            ArrStructuredObject structuredObject = structObjService.getExistingStructObj(srcItem.getValue());
            // Validate type of structured object
            Validate.isTrue(structuredObject.getStructuredTypeId() == itemType.getEntity().getStructuredTypeId(),
                            "Structured object (%i) has unexpected type, exptected: %i, real type: %i",
                            structuredObject.getStructuredObjectId(),
                            itemType.getEntity().getStructuredTypeId(),
                            structuredObject.getStructuredTypeId());

            ArrDataStructureRef dsr = new ArrDataStructureRef();
            dsr.setStructuredObject(structuredObject);
            data = dsr;
            break;
        default:
            Validate.isTrue(false, "Cannot convert string to data type: %s, item type: %s", itemType.getDataType(),
                            srcItem.getType());
        }
        data.setDataType(itemType.getDataType().getEntity());
        trgItem.setData(data);
    }

    private ItemType prepareItem(ArrItem trgItem, String type, String spec, Boolean readOnly) {
        StaticDataProvider sdp = staticDataService.getData();
        ItemType itemType = sdp.getItemTypeByCode(type);
        Validate.notNull(itemType, "Item type not found: {}", type);

        trgItem.setItemType(itemType.getEntity());
        trgItem.setReadOnly(readOnly==null?false:readOnly);

        if (itemType.hasSpecifications()) {
            Validate.notNull(spec, "Missing specification for item type: %s", type);
            RulItemSpec itemSpec = itemType.getItemSpecByCode(spec);
            Validate.notNull(itemSpec, "Cannot find specification for item type: %s, spec code: %s", type, spec);

            trgItem.setItemSpec(itemSpec);
        } else {
            Validate.isTrue(spec == null, "Item type cannot have specification: %s, value: %s", type, spec);
        }
        return itemType;
    }

    /**
     * Iterate all items and fill in position
     * 
     * @param result
     */
    public void countPositions(List<? extends ArrItem> result) {
        final Map<RulItemType, Integer> positionMap = new HashMap<>();
        result.stream().forEach(item -> {
            Integer position = positionMap.compute(item.getItemType(), (k, v) -> v == null ? 1 : ++v);
            item.setPosition(position);
        });

    }

    static public CoreServiceException prepareException(String msg, Exception e) {
        return prepareException(msg, detailOf(e), e);
    }

    /**
     * Prepare new exception
     * 
     * If e is already CoreServiceException same exception is returned
     * 
     * @param msg
     * @param detail
     * @param e
     * @return
     */
    static public CoreServiceException prepareException(String msg, String detail, Exception e) {
        if (e != null && e instanceof CoreServiceException) {
            return (CoreServiceException) e;
        }
        ErrorDescription ed = prepareErrorDescription(msg, detail);
        return new CoreServiceException(msg, ed, e);
    }

    static public ErrorDescription prepareErrorDescription(String msg, String detail) {
        ErrorDescription ed = new ErrorDescription();
        ed.setUserMessage(msg);
        ed.setDetail(detail);
        return ed;
    }

    /**
     * Build a human-readable fault detail describing the failure.
     *
     * CXF/Spring wrappers often carry a null or uninformative message, so the deepest
     * cause is appended when it differs from the top exception. If any exception in the
     * cause chain is an Elza {@link AbstractException}, its stable {@link ErrorCode} is
     * prepended so clients can branch on the code instead of parsing the message text.
     */
    static private String detailOf(Exception e) {
        if (e == null) {
            return null;
        }

        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }

        StringBuilder sb = new StringBuilder();

        String errorCode = findErrorCode(e);
        if (errorCode != null) {
            sb.append(errorCode).append(": ");
        }

        sb.append(e.getClass().getSimpleName());
        if (e.getMessage() != null) {
            sb.append(": ").append(e.getMessage());
        }

        if (root != e) {
            sb.append(" | cause: ").append(root.getClass().getSimpleName());
            if (root.getMessage() != null) {
                sb.append(": ").append(root.getMessage());
            }
        }

        return sb.toString();
    }

    /**
     * Walk the cause chain and return the code of the first Elza {@link AbstractException}
     * carrying an {@link ErrorCode}, or {@code null} if none is present.
     */
    static private String findErrorCode(Throwable t) {
        while (t != null) {
            if (t instanceof AbstractException) {
                ErrorCode code = ((AbstractException) t).getErrorCode();
                if (code != null) {
                    return code.getCode();
                }
            }
            if (t.getCause() == t) {
                break;
            }
            t = t.getCause();
        }
        return null;
    }

    /**
     * After a failure is logged with its full stack trace, the same failure recurring
     * within this window is logged as a single line without the stack trace. Once the
     * window elapses, the next occurrence is logged with the full stack again, so a
     * persistent problem stays visible while a retrying client cannot flood the log.
     */
    private static final long FAILURE_DEDUP_WINDOW_MINUTES = 10;

    /**
     * Upper bound on distinct failure signatures tracked at once, to keep the dedup
     * state bounded regardless of how many different failures occur.
     */
    private static final long MAX_TRACKED_FAILURES = 500;

    private static final Cache<String, FailureOccurrence> RECENT_FAILURES = CacheBuilder.newBuilder()
            .expireAfterWrite(FAILURE_DEDUP_WINDOW_MINUTES, TimeUnit.MINUTES)
            .maximumSize(MAX_TRACKED_FAILURES)
            .build();

    private static final class FailureOccurrence {
        private final long firstSeenMs;
        private final AtomicLong count = new AtomicLong();

        FailureOccurrence(long firstSeenMs) {
            this.firstSeenMs = firstSeenMs;
        }
    }

    /**
     * Log a web service failure at WARN, deduplicating the stack trace of repeating failures.
     *
     * @see #logWsFailure(Logger, Level, String, String, Exception)
     */
    public static void logWsFailure(Logger logger, String operation, String context, Exception e) {
        logWsFailure(logger, Level.WARN, operation, context, e);
    }

    /**
     * Log a web service failure, deduplicating the stack trace of repeating failures.
     *
     * The first occurrence of a given failure (identified by operation and the exception
     * summary, see {@link #detailOf(Exception)}) is logged at the given level with the full
     * stack trace. Subsequent identical failures within {@link #FAILURE_DEDUP_WINDOW_MINUTES}
     * are logged as a single line carrying the occurrence count, without the stack trace.
     * The full stack trace is logged again on the first occurrence after the window elapses,
     * so a persistent problem stays visible while a retrying client cannot flood the log.
     *
     * @param logger    logger of the calling web service endpoint
     * @param level     level to log at (e.g. {@link Level#WARN} or {@link Level#ERROR})
     * @param operation short description of the failed operation (e.g. "Failed to update fund")
     * @param context   request context identifying the affected entity (e.g. "id: 5, uuid: ..."),
     *                  may be {@code null} or blank
     * @param e         the caught exception
     */
    public static void logWsFailure(Logger logger, Level level, String operation, String context, Exception e) {
        String detail = detailOf(e);
        String signature = operation + '|' + detail;
        long now = System.currentTimeMillis();
        String where = (context != null && !context.isBlank()) ? operation + " (" + context + ")" : operation;

        FailureOccurrence occurrence;
        try {
            occurrence = RECENT_FAILURES.get(signature, () -> new FailureOccurrence(now));
        } catch (ExecutionException ignored) {
            // The loader cannot throw a checked exception; on the off chance the cache
            // fails, fall back to logging the full stack trace.
            logAtLevel(logger, level, "{}: {}", where, detail, e);
            return;
        }

        long occurrenceCount = occurrence.count.incrementAndGet();
        if (occurrenceCount == 1) {
            logAtLevel(logger, level, "{}: {}", where, detail, e);
        } else {
            logAtLevel(logger, level, "{}: {} (repeated {}x within {}, stack trace suppressed)",
                    where, detail, occurrenceCount, formatElapsed(now - occurrence.firstSeenMs));
        }
    }

    private static void logAtLevel(Logger logger, Level level, String format, Object... args) {
        switch (level) {
        case ERROR:
            logger.error(format, args);
            break;
        case WARN:
            logger.warn(format, args);
            break;
        case INFO:
            logger.info(format, args);
            break;
        case DEBUG:
            logger.debug(format, args);
            break;
        case TRACE:
            logger.trace(format, args);
            break;
        }
    }

    private static String formatElapsed(long elapsedMs) {
        long seconds = elapsedMs / 1000;
        if (seconds < 60) {
            return seconds + "s";
        }
        return (seconds / 60) + "m" + (seconds % 60) + "s";
    }

    /**
     * Prepare the fault declared by the createFund operation.
     *
     * The fault bean must be thrown as the exception type declared by the operation
     * (mapped to the createFundFailed element), otherwise CXF cannot resolve the fault
     * detail element and fails to marshal it.
     */
    static public CreateFundException prepareCreateFundException(String msg, Exception e) {
        if (e instanceof CreateFundException) {
            return (CreateFundException) e;
        }
        return new CreateFundException(msg, prepareErrorDescription(msg, detailOf(e)), e);
    }

    static public UpdateFundException prepareUpdateFundException(String msg, Exception e) {
        if (e instanceof UpdateFundException) {
            return (UpdateFundException) e;
        }
        return new UpdateFundException(msg, prepareErrorDescription(msg, detailOf(e)), e);
    }

    static public DeleteFundException prepareDeleteFundException(String msg, Exception e) {
        if (e instanceof DeleteFundException) {
            return (DeleteFundException) e;
        }
        return new DeleteFundException(msg, prepareErrorDescription(msg, detailOf(e)), e);
    }
}
