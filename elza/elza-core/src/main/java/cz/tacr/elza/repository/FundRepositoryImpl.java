package cz.tacr.elza.repository;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import cz.tacr.elza.domain.ParInstitution;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.vo.ArrFundOpenVersion;


/**
 * Rozšiřující rozhraní pro archivní fondy.
 */
@Component
public class FundRepositoryImpl implements FundRepositoryCustom {

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private FundRepository fundRepository;

	@Override
	public List<ArrFundOpenVersion> findByFulltext(final String fulltext, final int max, final Integer userId) {

		String hql = "SELECT f.fundId, max(v) FROM arr_fund f JOIN f.versions v "
				+ createFulltextWhereClause(fulltext, userId)
				+ " GROUP BY f.fundId, f.name ORDER BY f.name";

		Query query = createFulltextQuery(hql, fulltext, userId);

		query.setMaxResults(max);
		List<Object[]> arrayList = query.getResultList();
		List<ArrFundOpenVersion> result = new ArrayList<>(arrayList.size());
		for (Object[] array : arrayList) {
			final int fundId = (int) array[0];
			final ArrFund fund = fundRepository.getOneCheckExist(fundId);
			result.add(new ArrFundOpenVersion(fund, (ArrFundVersion) array[1]));
		}
		return result;
	}

	@Override
	public Integer findCountByFulltext(final String fulltext, final Integer userId) {
		String hql = "SELECT count(f) FROM arr_fund f " + createFulltextWhereClause(fulltext, userId);
		Query query = createFulltextQuery(hql, fulltext, userId);
		return Math.toIntExact((long) query.getSingleResult());
	}

	@Override
	public List<ArrFund> findFundByFulltext(final String fulltext, final Integer userId) {
		String hql = "SELECT f FROM arr_fund f " + createFulltextWhereClause(fulltext, userId);
		Query query = createFulltextQuery(hql, fulltext, userId);
		return query.getResultList();
	}

	public List<ArrFund> findFundByInstitution(Integer userId, ParInstitution institution) {
		String hql = "SELECT f FROM arr_fund f WHERE institution_id = " + institution.getInstitutionId();
		Query query = createFulltextQuery(hql, null, userId);
		return query.getResultList();
	}

	/**
	 * Vytvoří WHERE podmínky pro dotazy vyhledávání podle fulltextu.
	 *
	 * @param fulltext fulltext
	 * @return WHERE podmínka (pouze pokud je nastaven fulltext)
	 */
	private String createFulltextWhereClause(final String fulltext, Integer userId) {

		StringBuilder result = new StringBuilder(256);

		if (StringUtils.isNotBlank(fulltext)) {
			result.append(" WHERE LOWER(f.name) LIKE :text OR LOWER(f.internalCode) LIKE :text");
		}

		if (userId != null) {
			if (result.length() == 0) {
				result.append(" WHERE ");
			} else {
				result.append(" AND ");
			}
			result.append(" f IN (SELECT pv.fund FROM usr_permission_view pv WHERE user_id = :userId)");
		}

		return result.toString();
	}

	private Query createFulltextQuery(String hql, String fulltext, Integer userId) {

		Query query = entityManager.createQuery(hql);

		if (StringUtils.isNotBlank(fulltext)) {
			String text = "%" + fulltext + "%";
			query.setParameter("text", text.toLowerCase());
		}

		if (userId != null) {
			query.setParameter("userId", userId);
		}
		return query;
	}

	@Override
	public FilteredResult<ArrFund> findFunds(String search, int firstResult, int maxResults) {
		TypedQuery<ArrFund> data = buildFundFindQuery(true, search, firstResult, maxResults, ArrFund.class);
		List<ArrFund> results = data.getResultList();
		int totalCount = results.size();

		// get total number of records if needed
		if (totalCount >= maxResults || firstResult > 0) {
			TypedQuery<Number> count = buildFundFindQuery(false, search, firstResult, maxResults, Number.class);
			totalCount = count.getSingleResult().intValue();
		}
		return new FilteredResult<>(firstResult, maxResults, totalCount, results);
	}

	@Override
	public FilteredResult<ArrFund> findFunds(String search, Integer institutionId, int firstResult, int maxResults) {
		TypedQuery<ArrFund> data = buildFundFindQuery(true, search, institutionId, firstResult, maxResults, ArrFund.class);
		List<ArrFund> results = data.getResultList();
		int totalCount = results.size();

		// get total number of records if needed
		if (totalCount >= maxResults || firstResult > 0) {
			TypedQuery<Number> count = buildFundFindQuery(false, search, firstResult, maxResults, Number.class);
			totalCount = count.getSingleResult().intValue();
		}
		return new FilteredResult<>(firstResult, maxResults, totalCount, results);
	}

	@Override
	public FilteredResult<ArrFund> findFundsWithPermissions(final String search, final int firstResult,
															final int maxResults, final int userId) {
		TypedQuery<ArrFund> data = buildFundFindWithPermQuery(true, search, firstResult, maxResults, userId,
				ArrFund.class);
		List<ArrFund> results = data.getResultList();
		int totalCount = results.size();

		// get total number of records if needed
		if (totalCount >= maxResults || firstResult > 0) {
			TypedQuery<Number> count = buildFundFindWithPermQuery(false, search, firstResult, maxResults, userId,
					Number.class);
			totalCount = count.getSingleResult().intValue();
		}
		return new FilteredResult<>(firstResult, maxResults, totalCount, results);
	}

	@Override
	public FilteredResult<ArrFund> findFundsWithPermissions(final String search, final Integer institutionId, final int firstResult,
															final int maxResults, final int userId) {
		TypedQuery<ArrFund> data = buildFundFindWithPermQuery(true, search, institutionId, firstResult, maxResults, userId,
				ArrFund.class);
		List<ArrFund> results = data.getResultList();
		int totalCount = results.size();

		// get total number of records if needed
		if (totalCount >= maxResults || firstResult > 0) {
			TypedQuery<Number> count = buildFundFindWithPermQuery(false, search, institutionId, firstResult, maxResults, userId,
					Number.class);
			totalCount = count.getSingleResult().intValue();
		}
		return new FilteredResult<>(firstResult, maxResults, totalCount, results);
	}

	@Override
	public List<ArrFund> findFundsByInstitutionId(Integer institutionId) {
		return fundRepository.findByInstitutionId(institutionId);
	}

	private <T> TypedQuery<T> buildFundFindQuery(final boolean dataQuery,
												 final String search,
												 final int firstResult,
												 final int maxResults,
												 Class<T> clazz) {

        StringBuilder query = new StringBuilder();
        StringBuilder orderBy = new StringBuilder();
        if (dataQuery) {
            query.append("SELECT DISTINCT f ");
            orderBy.append("f.name");
        } else {
            query.append("SELECT COUNT(DISTINCT f) ");
        }

        query.append("FROM arr_fund f ");

        // text condition
        if (!StringUtils.isEmpty(search)) {
            query.append("WHERE "
                + "(LOWER(f.name) LIKE :search OR "
                + "LOWER(f.internalCode) LIKE :search OR "
                + "CAST(f.fundNumber AS text) LIKE :search OR "
                + "LOWER(f.mark) LIKE :search) "
            );
        }
        if (orderBy.length() > 0) {
            query.append("ORDER BY ").append(orderBy);
        }

		TypedQuery<T> q = entityManager.createQuery(query.toString(), clazz);

		// set parameters
		if (!StringUtils.isEmpty(search)) {
			q.setParameter("search", "%" + search.toLowerCase() + "%");
		}

		if (dataQuery) {
			q.setFirstResult(firstResult);
			if (maxResults >= 0) {
				q.setMaxResults(maxResults);
			}
		}
		return q;
	}

	private <T> TypedQuery<T> buildFundFindQuery(final boolean dataQuery,
												 final String search,
												 final Integer institutionId,
												 final int firstResult,
												 final int maxResults,
												 Class<T> clazz) {

        StringBuilder query = new StringBuilder();
        StringBuilder orderBy = new StringBuilder();
        if (dataQuery) {
            query.append("SELECT DISTINCT f ");
            orderBy.append("f.name");
        } else {
            query.append("SELECT COUNT(DISTINCT f) ");
        }

        query.append("FROM arr_fund f ");

        // text condition
        if (!StringUtils.isEmpty(search)) {
            query.append("WHERE "
                + "(LOWER(f.name) LIKE :search OR "
                + "LOWER(f.internalCode) LIKE :search OR "
                + "CAST(f.fundNumber AS text) LIKE :search OR "
                + "LOWER(f.mark) LIKE :search) "
            );
            if (institutionId != null) {
                query.append("AND f.institution.institutionId = :institutionId ");
            }
        } else {
            if (institutionId != null) {
                query.append("WHERE f.institution.institutionId = :institutionId ");
            }
        }
        if (orderBy.length() > 0) {
            query.append("ORDER BY ").append(orderBy);
        }

		TypedQuery<T> q = entityManager.createQuery(query.toString(), clazz);

		// set parameters
		if (!StringUtils.isEmpty(search)) {
			q.setParameter("search", "%" + search.toLowerCase() + "%");
		}
		if (institutionId != null) {
			q.setParameter("institutionId", institutionId);
		}

		if (dataQuery) {
			q.setFirstResult(firstResult);
			if (maxResults >= 0) {
				q.setMaxResults(maxResults);
			}
		}
		return q;
	}

	/* Dotaz detailne

	-- zajimaji nas fondy, na nez jsou nastavena nejaka explicitni prava
	-- pro uzivatele, tj. muze je predavat dale, opravneni jsou
	-- nastavena bud primo nebo pres skupinu

	select distinct f.* from arr_fund f
	join usr_permission fu on f.fund_id = fu.fund_id
	where
	fu.user_id = 23
	OR
	fu.group_id IN (
	-- vyber skupin jejichz je clenem
	SELECT gu.group_Id FROM usr_group_user gu WHERE gu.user_Id = 23
	)
	-- AND LOWER(f.name) like '%t%' OR LOWER(f.internalCode) like '%t%'
	ORDER BY f.name
	 */

	private <T> TypedQuery<T> buildFundFindWithPermQuery(final boolean dataQuery,
														 final String search,
														 final int firstResult,
														 final int maxResults,
														 final int userId,
														 Class<T> clazz) {

        StringBuilder query = new StringBuilder();
        StringBuilder orderBy = new StringBuilder();
        if (dataQuery) {
            query.append("SELECT DISTINCT f ");
            orderBy.append("f.name");
        } else {
            query.append("SELECT COUNT(DISTINCT f) ");
        }

        query.append("FROM arr_fund f "
                + "JOIN usr_permission fu ON fu.fund = f "
                + "WHERE "
                + "(fu.userId = :userId OR"
                + " fu.groupId IN"
                + " (SELECT gu.groupId FROM usr_group_user gu WHERE gu.userId = :userId)"
                + ") "
        );

        // text condition
        if (!StringUtils.isEmpty(search)) {
            query.append("AND "
                + "(LOWER(f.name) LIKE :search OR "
                + "LOWER(f.internalCode) LIKE :search OR "
                + "CAST(f.fundNumber AS text) LIKE :search OR "
                + "LOWER(f.mark) LIKE :search) "
            );
        }

        // Připojení podmínek ke query
        if (orderBy.length() > 0) {
            query.append("ORDER BY ").append(orderBy);
        }

		TypedQuery<T> q = entityManager.createQuery(query.toString(), clazz);

		// set parameters
		q.setParameter("userId", userId);
		if (!StringUtils.isEmpty(search)) {
			q.setParameter("search", "%" + search.toLowerCase() + "%");
		}

		if (dataQuery) {
			q.setFirstResult(firstResult);
			if (maxResults >= 0) {
				q.setMaxResults(maxResults);
			}
		}
		return q;
	}

    /* Dotaz detailne

	-- zajimaji nas fondy, na nez jsou nastavena nejaka explicitni prava
	-- pro uzivatele, tj. muze je predavat dale, opravneni jsou
	-- nastavena bud primo nebo pres skupinu

	select distinct f.* from arr_fund f
	join usr_permission fu on f.fund_id = fu.fund_id
	where
	fu.user_id = 23
	OR
	fu.group_id IN (
	-- vyber skupin jejichz je clenem
	SELECT gu.group_Id FROM usr_group_user gu WHERE gu.user_Id = 23
	)
	-- AND LOWER(f.name) like '%t%' OR LOWER(f.internalCode)  like '%t%'
	ORDER BY f.name
	 */

	private <T> TypedQuery<T> buildFundFindWithPermQuery(final boolean dataQuery,
														 final String search,
														 final Integer institutionId,
														 final int firstResult,
														 final int maxResults,
														 final int userId,
														 Class<T> clazz) {

        StringBuilder query = new StringBuilder();
        StringBuilder orderBy = new StringBuilder();
        if (dataQuery) {
            query.append("SELECT DISTINCT f ");
            orderBy.append("f.name");
        } else {
            query.append("SELECT COUNT(DISTINCT f) ");
        }

        query.append("FROM arr_fund f "
                + "JOIN usr_permission fu ON fu.fund = f "
                + "WHERE "
                + "(fu.userId = :userId OR"
                + " fu.groupId IN"
                + " (SELECT gu.groupId FROM usr_group_user gu WHERE gu.userId = :userId)"
                + ") "
        );

        // text condition
        if (!StringUtils.isEmpty(search)) {
            query.append("AND "
                + "(LOWER(f.name) LIKE :search OR "
                + "LOWER(f.internalCode) LIKE :search OR "
                + "CAST(f.fundNumber AS text) LIKE :search OR "
                + "LOWER(f.mark) LIKE :search) "
            );
        }
        if (institutionId != null) {
            query.append("AND ").append("f.institution.institutionId = :institutionId ");
        }

        // Připojení podmínek ke query
        if (orderBy.length() > 0) {
            query.append("ORDER BY ").append(orderBy);
        }

		TypedQuery<T> q = entityManager.createQuery(query.toString(), clazz);

		// set parameters
		q.setParameter("userId", userId);
		if (!StringUtils.isEmpty(search)) {
			q.setParameter("search", "%" + search.toLowerCase() + "%");
		}
		if (institutionId != null) {
			q.setParameter("institutionId", institutionId);
		}

		if (dataQuery) {
			q.setFirstResult(firstResult);
			if (maxResults >= 0) {
				q.setMaxResults(maxResults);
			}
		}
		return q;
	}
}
