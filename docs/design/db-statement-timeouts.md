Database statement timeouts
===========================

**Proposal, not implemented yet** — none of the components below exists in the code.

Why
---

The product has no database timeout of any kind: no `statement_timeout`, no
`@Transactional(timeout=)`, no query hints. A pathological query keeps
running after the user has given up, holding a connection of a pool whose
`maximumPoolSize` is 20. The synchronous pre-generation check of an output
(`OutputServiceInternal.checkRecommendedActions`, a change-detection query)
has shown the failure mode: the browser gives up, nginx logs 499, the
database never notices, and abandoned copies of the query, each running for
hours, drain the pool. This proposal is the defence in depth.

Two facts shape the design:

* Only a server-side or JDBC-side limit releases the connection. Client,
  proxy and browser timeouts are cosmetic; they end the user's wait, not the
  query.
* PostgreSQL `statement_timeout` is per *statement*, not per transaction, but
  this codebase contains single statements that legitimately run for
  minutes: the statistics reports (`ReportServiceQuery`: view-refresh
  `INSERT … SELECT` over all of `arr_item`/`arr_change`, `SYS_MONTH_USER_COUNT`
  with seven CTEs), whole-fund `readLevelTree`, and the `arr_change` cleanup
  in `DeleteFundAction` that is already marked *"velmi pomalé"* in the code.
  One global number cannot serve both a web request and a night-time report.


Design
------

| Layer | Value | Applies to | Mechanism |
|---|---|---|---|
| Pool default | 30 min | every pooled connection, incl. code that bypasses Spring transactions (mass indexer) | `connectionInitSql` set on the `HikariDataSource`, PostgreSQL only |
| Request limit | 5 min | every transaction begun on a request-bound thread (REST, SOAP) | `TransactionExecutionListener.afterBegin` → `SET LOCAL` |
| Long-running | 2 h | report worker thread; fund delete, history delete, revert | thread marker read by the same listener; explicit `relaxCurrentTransaction()` where `@Transactional` began the transaction first |
| Pre-generate check | 30 s | the rest of the `addRequest` transaction | explicit `limitCurrentTransaction(ms)` |
| Liquibase | none | migrations | own non-pooled `SimpleDriverDataSource` |

`SET LOCAL` reverts at commit/rollback, so a pooled connection always comes
back with the 30-minute default. Whichever limit is tighter fires first; all
of them surface as SQLSTATE 57014, so one exception path serves all.

The pool default is generous and requests are tightened per transaction,
not the reverse: if the listener ever fails to fire, the result is
*degraded* (30 min), not *broken*.

Why 5 minutes for requests: it is the point where the client is certainly
gone (nginx default is 60 s; sites that raised it still fit), while heavy
but legitimate synchronous work on large funds (change listing, CSV import,
DAO sync) fits comfortably. The three admin operations known to exceed it
are exempted explicitly. Anything else that reaches 5 minutes on a request
thread is a bug to surface, not a case to tolerate.

Alternatives not taken: `spring.transaction.default-timeout` (applies to
every `TransactionTemplate` in ~35 background sites and is enforced
client-side); a `@Transactional(timeout)` decorator (same mechanism, and
Hibernate throws `TransactionException`, not a timeout, once the budget is
gone); a second connection pool for background threads (needs a second
`EntityManagerFactory`); `ALTER ROLE … SET statement_timeout` (DBA action,
not a shippable default, and it also hits `psql`/`pg_dump`); pgjdbc
`query.setTimeout` for the tight limit (client-side cancel through a second
TCP connection, which fails silently when that connection cannot be opened).


Implementation
--------------

### 1. Liquibase off the pool — separate, revertible commit, lands first

`liquibase-core` is a dependency of `elza-core`, the
changelog is at Boot's default path, there is no `SpringLiquibase` bean and
no `spring.liquibase.url`, so migrations run on the primary Hikari
`DataSource`. A pool-level timeout could abort a `CREATE INDEX` on `arr_item`
(tens of millions of rows) and leave an install broken mid-upgrade.

`elza-core/src/main/resources/elza.yaml`, inside `spring:`:

```yaml
  # Liquibase drží vlastní, nepoolované spojení mimo Hikari - na poolu je
  # statement_timeout a migrace nad velkými tabulkami běží v jednom příkazu déle.
  liquibase:
    url: ${elza.data.url}
    user: ${elza.data.user}
    password: ${elza.data.pass}
```

Boot builds a `SimpleDriverDataSource` for it and closes it after migration.
`ElzaCore.configure()` already sets the changelog table names
through system properties — unaffected.

### 2. `DbStatementTimeout` — the one component

New `cz.tacr.elza.core.db.DbStatementTimeout`, a `@Component` implementing
`org.springframework.transaction.TransactionExecutionListener` (Spring 6.1+;
the resolved `spring-tx-6.2.15` has it). Spring Boot 3.5 registers every
`TransactionExecutionListener` bean on the transaction manager by itself
(`ExecutionListenersTransactionManagerCustomizer`), so there is nothing to
wire.

```java
@Component
public class DbStatementTimeout implements TransactionExecutionListener {

    private static final ThreadLocal<Boolean> LONG_RUNNING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Value("${elza.db.requestStatementTimeoutMs:300000}")
    private long requestTimeoutMs;

    @Value("${elza.db.longRunningStatementTimeoutMs:7200000}")
    private long longRunningTimeoutMs;

    @PersistenceContext
    private EntityManager em;

    /** Každá transakce zahájená na tomto vlákně dostane limit pro dlouhé operace. */
    public static void markThreadLongRunning() {
        LONG_RUNNING.set(Boolean.TRUE);
    }

    @Override
    public void afterBegin(TransactionExecution tx, @Nullable Throwable beginFailure) {
        if (beginFailure != null || !DatabaseType.isPostgres()) {
            return;
        }
        if (LONG_RUNNING.get()) {
            apply(longRunningTimeoutMs);
        } else if (RequestContextHolder.getRequestAttributes() != null && requestTimeoutMs > 0) {
            apply(requestTimeoutMs);
        }
        // jinak platí výchozí hodnota poolu
    }

    /** Pro transakce, které už zahájil @Transactional (mazání AS, revert). */
    public void relaxCurrentTransaction() {
        applyToCurrent(longRunningTimeoutMs);
    }

    /** Přísnější limit pro zbytek aktuální transakce (kontrola před generováním). */
    public void limitCurrentTransaction(long ms) {
        applyToCurrent(ms);
    }

    private void applyToCurrent(long ms) {
        if (DatabaseType.isPostgres() && TransactionSynchronizationManager.isActualTransactionActive()) {
            apply(ms);
        }
    }

    private void apply(long ms) {
        // SET nelze parametrizovat; ms je long, vložení do textu je bezpečné
        em.unwrap(Session.class).doWork(conn -> {
            try (Statement st = conn.createStatement()) {
                st.execute("SET LOCAL statement_timeout = " + ms);
            }
        });
    }
}
```

Notes on correctness:

* `afterBegin` runs after `doBegin` and `prepareSynchronization`, so the
  transactional `EntityManager` is bound and `SET LOCAL` lands inside the
  new transaction. It is *not* called for participating (joined)
  transactions, so the statement runs once per real transaction.
* `RequestContextHolder` is populated by Boot's `RequestContextFilter` for
  every servlet request, so CXF/SOAP requests count as request-bound too.
  STOMP handlers (`clientInboundChannel-N`) and `@Scheduled` methods are not,
  and fall through to the pool default — the safe direction.
* `DatabaseType.isPostgres()` is `false` until `StartupService` calls
  `DatabaseType.init(em)`; start-up transactions therefore run under the
  pool default. Nothing is served before that point.
* `TransactionSynchronizationManager.isActualTransactionActive()` guards the
  explicit calls: `SET LOCAL` outside a transaction is a no-op that only
  produces a server warning.
* Hibernate's default connection handling for resource-local transactions
  keeps the connection for the whole transaction, so the `SET LOCAL` and
  the statements it governs share one connection.
* Cost: one extra round-trip per request transaction (sub-millisecond on
  localhost). Background transactions pay nothing.

Unit test `DbStatementTimeoutTest`: extract the decision into a pure static
`resolveTimeoutMs(boolean longRunning, boolean requestBound, long req, long lr)`
returning `OptionalLong`, and test the four combinations. H2-safe, no
database.

### 3. Exemptions — four lines

* `service/report/ReportWorker` — the worker thread is created
  with `new Thread(() -> run())`. Call `DbStatementTimeout.markThreadLongRunning()`
  as the first statement of that lambda. This covers `checkAndUpdateViews`
  (four `TransactionTemplate` blocks) *and* the report queries
  (`ReportSimpleQuery`, `ReportSysMonthUserCount`, `ReportSysInstitutionCount`),
  all of which execute inside `createReport` on that thread.
* `ArrangementService.deleteFund` and `ArrangementService.deleteFundHistory` — both `@Transactional`; call
  `dbStatementTimeout.relaxCurrentTransaction()` before delegating to the
  `DeleteFundAction` / `DeleteFundHistoryAction` bean.
* `RevertingChangesService.revertChanges` — transaction
  begun by `ArrangementController.revertChanges`; same call at the top.

Not exempted, by decision: `readLevelTree` (seconds to low minutes even on
the largest funds, well inside the 30-minute background default; the real
fix is a fetch size, see *Follow-ups*) and `AdminService.reindex` (the mass
indexer bypasses Spring transactions, so `SET LOCAL` would not reach it; its
`COUNT(*)` statements are far under 30 minutes; if that ever changes, use
Hibernate Search's own `idFetchSize` / `transactionTimeout`, not this
helper).

### 4. Pre-generate check — 30 s

`OutputServiceInternal.addRequest` is `@Transactional(TxType.MANDATORY)`,
i.e. it joins the controller's transaction, and after
`checkRecommendedActions` it only sets a state, publishes an event and
inserts one queue row. So limiting *the rest of that transaction* is
equivalent to limiting the check, with no second connection involved:

```java
@Value("${elza.output.generateCheckTimeoutMs:30000}")
private long generateCheckTimeoutMs;

// v addRequest, před checkRecommendedActions:
if (checkBulkActions) {
    dbStatementTimeout.limitCurrentTransaction(generateCheckTimeoutMs);
    ...
```

No overload of `existsChangeAfter`, no change to `RevertingChangesServiceTest`.

In `checkRecommendedActions`, wrap the `existsChangeAfter` call and rethrow
with context so the user is told about the working path — `forced=true`
skips this check entirely:

```java
} catch (jakarta.persistence.QueryTimeoutException | org.springframework.dao.QueryTimeoutException e) {
    throw new BusinessException("Kontrola změn v AS před generováním výstupu překročila časový limit",
                                e, BaseCode.QUERY_TIMEOUT)
            .set("timeoutMs", generateCheckTimeoutMs)
            .set("fundId", fundVersion.getFundId())
            .set("outputId", output.getOutputId())
            .level(Level.WARNING);
}
```

### 5. Error mapping — global

* `exception/codes/BaseCode.java` — add `QUERY_TIMEOUT` after
  `TOO_MANY_RESULTS` (turn its `;` into `,`). Nothing existing fits;
  `TOO_MANY_RESULTS` is semantically different.
* `controller/config/ControllerExceptionHandler.java` — new handler
  alongside the existing ones (Spring picks the most specific exception
  type, so declaration order does not matter):

```java
@ExceptionHandler({jakarta.persistence.QueryTimeoutException.class,
                   org.springframework.dao.QueryTimeoutException.class,
                   org.hibernate.QueryTimeoutException.class})
@ResponseBody
public ResponseEntity<ExceptionResponse> queryTimeout(final Exception cause) {
    SystemException ex = new SystemException("Databázový dotaz překročil časový limit", cause,
                                             BaseCode.QUERY_TIMEOUT).level(Level.WARNING);
    ExceptionResponseBuilder builder = ExceptionResponseBuilder.createFrom(ex);
    builder.logError(logger);
    return new ResponseEntity<>(builder.build(), headerContentTypeJson(), HttpStatus.INTERNAL_SERVER_ERROR);
}
```

  Status stays 500 like every other `AbstractException`; the UI keys off
  `code`. Three exception types because PostgreSQL 57014 becomes
  `org.hibernate.QueryTimeoutException`, Hibernate's `ExceptionConverter`
  turns it into the `jakarta.persistence` one at the `EntityManager`
  boundary, and Spring Data repositories translate it once more into
  `org.springframework.dao.QueryTimeoutException`.

* `elza-react/src/components/exception/messages.ts` — add
  `exception.base.QUERY_TIMEOUT` ("Databázový dotaz překročil časový limit.
  Zkuste akci opakovat; u generování výstupu lze použít volbu „Generovat i
  přes varování“."). The catalog already interpolates `{property}` values.

Deliberately not added: a new `OutputRequestStatus` value. That enum is
generated from `elza-development/typespec/main.tsp` and would need the
TypeSpec → OpenAPI → client regeneration chain for a case that should be
rare once the caps exist.

### 6. Pool default — from Java, PostgreSQL only

`SET statement_timeout` is invalid on H2 and MSSQL; a YAML
`connection-init-sql` would make the pool refuse to start on those, and
`elza.yaml.template` still lists H2 as a developer option. So the init SQL
is set from Java, only for PostgreSQL URLs.

New `cz.tacr.elza.core.db.PostgresStatementTimeoutInitializer`, a
`BeanPostProcessor` + `EnvironmentAware` (no bean dependencies, so it does
not trigger early instantiation):

```java
@Override
public Object postProcessAfterInitialization(Object bean, String beanName) {
    if (bean instanceof HikariDataSource ds && ds.getConnectionInitSql() == null) {
        long ms = env.getProperty("elza.db.statementTimeoutMs", Long.class, 1_800_000L);
        String url = ds.getJdbcUrl();
        if (ms > 0 && url != null && url.startsWith("jdbc:postgresql:")) {
            ds.setConnectionInitSql("SET statement_timeout = " + ms);
        }
    }
    return bean;
}
```

Why this works: Boot binds `spring.datasource.hikari.*` in
`postProcessBeforeInitialization`, Hikari seals its configuration only on
the first `getConnection()` (which happens when the `EntityManagerFactory`
resolves the dialect, after the `DataSource` bean is complete), and Hikari
runs `connectionInitSql` on *every* physical connection with
`autoCommit=true`, including replacements after `maxLifetime` — so the
setting cannot drift. An explicit YAML `connection-init-sql` is honoured
untouched. Liquibase's `SimpleDriverDataSource` is not a `HikariDataSource`
and is never touched (that is what step 1 buys). Should some earlier bean
already have opened a connection, `setConnectionInitSql` throws
`IllegalStateException` at start-up — loud, not silent; the YAML line below
is the fallback.

Invariant: do not set `spring.datasource.hikari.auto-commit: false`; the
init `SET` would then be rolled back on connection return.

Pure-YAML equivalent, if accepted that H2/MSSQL installs must then set the
value to `0` *and* remove the line: under `spring.datasource.hikari`,
`connection-init-sql: SET statement_timeout = ${elza.db.statementTimeoutMs:1800000}`.

### 7. Configuration

All in milliseconds; `0` disables the layer (for the long-running value,
`0` means unlimited, which is what PostgreSQL means by it).

| Property | Default | Read by |
|---|---|---|
| `elza.db.statementTimeoutMs` | `1800000` (30 min) | `PostgresStatementTimeoutInitializer` |
| `elza.db.requestStatementTimeoutMs` | `300000` (5 min) | `DbStatementTimeout` listener |
| `elza.db.longRunningStatementTimeoutMs` | `7200000` (2 h) | `DbStatementTimeout` marker / relax |
| `elza.output.generateCheckTimeoutMs` | `30000` (30 s) | `OutputServiceInternal` |

`@Value("${elza.x.y:default}")` is the house convention (precedent
`NodeSearchService`). Do not hang these off `elza.data.*` —
`HibernateConfiguration` binds that prefix.

Document them, commented out, in `elza-web/config/elza.yaml.template` and
`distrib/distribution/src/assembly/server/config/elza.yaml` (the latter
becomes `/opt/elza/config/elza.yaml`). External config merges per property,
so overriding a single key is enough. The distrib comment must say
explicitly: *if a nightly report, a fund delete or a revert starts failing
with SQLSTATE 57014, raise `elza.db.longRunningStatementTimeoutMs`; if an
ordinary request does, raise `elza.db.requestStatementTimeoutMs` and file a
bug for the slow query.* Without that note this becomes a mystery 3 a.m.
failure.


Verification
------------

**Tests need no changes — but prove it.** `AbstractTest` uses
`@ContextConfiguration(classes = ElzaCoreMain.class)`, and
`ElzaCoreMain.configure()` deliberately omits `spring.config.name=elza`, so
the suite loads only `elza-core/src/test/resources/application.yaml` (H2).
The listener no-ops because `DatabaseType.isPostgres()` is false; the
initializer skips `jdbc:h2:` URLs. Confirm once:

```
mvn -o -pl elza-core test -Dtest=RevertingChangesServiceTest,OutputGenerationTest,DbStatementTimeoutTest
```

and one `AbstractControllerTest` subclass, to prove the context still
starts with the two new beans.

**On PostgreSQL (a staging copy, not production):**

1. Pool default: through the application (a scratch endpoint or a debug
   log line in `StartupService`), `SELECT current_setting('statement_timeout')`
   → `30min`. During start-up, `pg_stat_activity` should show the Liquibase
   backend with `0`; the concrete check is a throwaway changeset running
   `SELECT pg_sleep(1900)` — start-up must succeed. Delete it afterwards.
2. Request limit: a throwaway `@Transactional` endpoint running
   `SELECT pg_sleep(400)` must fail at ~300 s with `code: QUERY_TIMEOUT`,
   level `warning`, and leave no backend behind in `pg_stat_activity`.
3. Long-running marker — the case that motivated the exemption:
   temporarily set `elza.db.longRunningStatementTimeoutMs: 1000` and run a
   statistics report; it must fail with 57014 (proving the marker reaches
   the worker's transactions). Restore the value; the same report must then
   succeed.
4. Fund delete on the largest fund available, with the exemption in place.
5. Reproduce a hanging pre-generation check on a copy of a large fund with
   `checkRecommendedActions` temporarily made slow (e.g. `pg_sleep(60)`
   appended to a branch): `GET …/output/generate/{id}?forced=false` must
   return in ~30 s with `QUERY_TIMEOUT`, and `hikaricp_connections_active`
   (Prometheus is already exposed on loopback:8081) must return to
   baseline. Then set `elza.output.generateCheckTimeoutMs: 0` and confirm
   the request limit (5 min) takes over.
6. Watch the log for `QUERY_TIMEOUT` warnings for two weeks after rollout;
   every hit is either a slow query to fix or a value to tune.


Rollout
-------

1. Commit: Liquibase off the pool (step 1). Revertible on its own.
2. Commit: `DbStatementTimeout` + the four exemption lines + `BaseCode`,
   handler, pre-check limit and frontend message (steps 2–5). This commit is
   self-consistent: the request limit and its exemptions land together.
3. Commit: `PostgresStatementTimeoutInitializer` + YAML documentation
   (steps 6–7). Last, so no build exists in which long jobs are capped
   without their exemptions.

Setting all four properties to `0` restores the behaviour without timeouts
exactly, which is the rollback path for any single site.


Open work beyond this proposal
------------------------------

* **No JDBC fetch size anywhere.** `readLevelTree`, `findUncachedNodes` and
  `findUncachedAccessPoints` use `scroll(FORWARD_ONLY)`, but with pgjdbc's
  default fetch size of 0 the driver materialises the whole result set in
  one round-trip — a memory spike and one long statement. Setting
  `hibernate.jdbc.fetch_size` (e.g. 1000) turns each into many short
  fetches; PostgreSQL documents `statement_timeout` as restarting per
  `Execute` message in the extended protocol, but measure before relying on
  it.
* **STOMP/websocket handlers** run user edits on `clientInboundChannel-N`
  threads, which are not request-bound and therefore get the 30-minute pool
  default. A `ChannelInterceptor` could mark them like requests if that
  ever matters.
* **`idle_in_transaction_session_timeout`** guards a different failure (a
  thread blocked outside the database while holding a transaction). It can
  be appended to the same init SQL later; not part of this change.
* **nginx `proxy_read_timeout`** defaults to 60 s, below the 5-minute
  request limit, so users see a 504 before the database gives up. Backend
  work is bounded either way; aligning the two is a UX choice.
* `RevertingChangesService` — the `arr_change` cleanup that is
  disabled in `revertChanges` but still executed by `DeleteFundAction`.
  Exempting it is a stopgap; the code's own TODO asks for a better query.
