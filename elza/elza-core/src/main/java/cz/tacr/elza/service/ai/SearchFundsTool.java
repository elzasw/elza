package cz.tacr.elza.service.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.aiprovider.client.vo.SearchFundsParams;
import cz.tacr.elza.aiprovider.client.vo.SearchFundsResult;
import cz.tacr.elza.aiprovider.client.vo.StandardToolName;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.repository.FilteredResult;
import cz.tacr.elza.repository.FundRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.InstitutionRepository;
import cz.tacr.elza.service.UserService;

/**
 * Standard {@code searchFunds} tool — fund lookup/listing by the fund's own
 * identity, requested by the AI model mid-turn. Argument/result shapes are
 * defined by the AI provider contract ({@code SearchFundsParams} /
 * {@code SearchFundsResult}); design notes:
 * {@code elza-development/typespec-ai/fund-search.md}.
 *
 * <p>One query string matches every fund identifier at once (name, internal
 * code, fund number, mark — the repository's fund-find predicate); an
 * unconstrained call lists the funds the user may read, ordered by name, in a
 * window ({@code from} + {@code totalCount}) — funds are a bounded ordered
 * list, so listing and paging are legitimate here, unlike node/entity search.
 *
 * <p>The tool runs on the poller thread, outside the request security context,
 * so it enforces the conversation owner's read permissions itself: a user with
 * {@code ADMIN}/{@code FUND_RD_ALL} lists unrestricted, anyone else only the
 * funds their permissions cover — applied inside the query (the
 * {@code usr_permission_view} join), never post-hoc.
 */
@Component
public class SearchFundsTool implements AiTool {

    /** Funds returned per window, regardless of the requested limit. */
    static final int MAX_WINDOW = 50;

    /** Window size when the model requests none. */
    static final int DEFAULT_WINDOW = 25;

    private final UserService userService;
    private final FundRepository fundRepository;
    private final FundVersionRepository fundVersionRepository;
    private final InstitutionRepository institutionRepository;
    private final AiContextResolver aiContextResolver;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;

    public SearchFundsTool(final UserService userService,
                           final FundRepository fundRepository,
                           final FundVersionRepository fundVersionRepository,
                           final InstitutionRepository institutionRepository,
                           final AiContextResolver aiContextResolver,
                           final TransactionTemplate transactionTemplate,
                           final ObjectMapper objectMapper) {
        this.userService = userService;
        this.fundRepository = fundRepository;
        this.fundVersionRepository = fundVersionRepository;
        this.institutionRepository = institutionRepository;
        this.aiContextResolver = aiContextResolver;
        this.transactionTemplate = transactionTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public StandardToolName name() {
        return StandardToolName.SEARCH_FUNDS;
    }

    @Override
    public Object execute(final Object arguments, final AiToolContext context) {
        SearchFundsParams params = objectMapper.convertValue(arguments, SearchFundsParams.class);
        String fulltext = params == null ? null : StringUtils.trimToNull(params.getFulltext());
        String institutionCode = params == null ? null : StringUtils.trimToNull(params.getInstitutionCode());
        int from = params != null && params.getFrom() != null && params.getFrom() > 0 ? params.getFrom() : 0;
        int limit = params != null && params.getLimit() != null && params.getLimit() > 0
                ? Math.min(params.getLimit(), MAX_WINDOW)
                : DEFAULT_WINDOW;

        boolean unrestricted = isUnrestricted(context);

        // The poller thread has no transaction; the query and the lazy
        // institution / rule-set / root-node reads need one.
        return transactionTemplate.execute(status -> {
            Integer institutionId = null;
            if (institutionCode != null) {
                ParInstitution institution = institutionRepository.findByInternalCode(institutionCode);
                if (institution == null) {
                    // unknown institution — an empty result, not an error: the
                    // model adapts, nothing leaks
                    return new SearchFundsResult().funds(List.of()).from(from).totalCount(0L);
                }
                institutionId = institution.getInstitutionId();
            }
            return toResult(findFunds(fulltext, institutionId, from, limit, unrestricted, context), from);
        });
    }

    /**
     * {@code ADMIN}/{@code FUND_RD_ALL} (or the virtual admin account, which has
     * no user row) list without the permission join.
     */
    private boolean isUnrestricted(final AiToolContext context) {
        if (context.userId() == null) {
            return true;
        }
        return userService.getUserPermissions(context.userId()).stream()
                .anyMatch(p -> p.getPermission() == Permission.ADMIN
                        || p.getPermission() == Permission.FUND_RD_ALL);
    }

    @SuppressWarnings("deprecation") // the institution overloads have no replacement (deprecated by #9154)
    private FilteredResult<ArrFund> findFunds(final String fulltext, final Integer institutionId,
                                              final int from, final int limit,
                                              final boolean unrestricted, final AiToolContext context) {
        if (unrestricted) {
            return institutionId == null
                    ? fundRepository.findFunds(fulltext, from, limit)
                    : fundRepository.findFunds(fulltext, institutionId, from, limit);
        }
        return institutionId == null
                ? fundRepository.findFundsWithPermissions(fulltext, from, limit, context.userId())
                : fundRepository.findFundsWithPermissions(fulltext, institutionId, from, limit,
                        context.userId());
    }

    /**
     * The page as the contract result: each fund's {@code FundInfo} with its open
     * version's root node and rule set (batch-loaded). A fund without an open
     * version stays listed — its version-bound fields absent — so the window
     * always matches {@code totalCount}.
     */
    private SearchFundsResult toResult(final FilteredResult<ArrFund> found, final int from) {
        List<Integer> fundIds = found.getList().stream().map(ArrFund::getFundId).toList();
        Map<Integer, ArrFundVersion> versionByFund = fundIds.isEmpty()
                ? Map.of()
                : fundVersionRepository.findByFundIdsAndLockChangeIsNull(fundIds).stream()
                        .collect(Collectors.toMap(ArrFundVersion::getFundId, v -> v));
        SearchFundsResult result = new SearchFundsResult()
                .funds(new ArrayList<>())
                .from(from)
                .totalCount((long) found.getTotalCount());
        for (ArrFund fund : found.getList()) {
            result.addFundsItem(aiContextResolver.buildFundInfo(fund, versionByFund.get(fund.getFundId())));
        }
        return result;
    }
}
