==========
Monitoring
==========

ELZA provides health checks and metrics in the Prometheus format
(`Spring Boot Actuator
<https://docs.spring.io/spring-boot/reference/actuator/index.html>`_).
They can be connected to common monitoring tools such as Prometheus,
Grafana or Zabbix.

Management port
===============

The monitoring endpoints are enabled by default. They are published on a
separate management port, ``8081``, bound to ``127.0.0.1``, so they are
reachable only from the server itself. The application port is not
affected. The built-in defaults are:

.. code-block:: yaml

   management:
     server:
       port: 8081
       address: 127.0.0.1
     endpoints:
       web:
         exposure:
           include: health,prometheus
     endpoint:
       health:
         probes:
           enabled: true
         show-details: always

Only the ``health`` and ``prometheus`` endpoints are published. Any value
can be overridden in :file:`elza.yaml` or with an environment variable
(for example ``MANAGEMENT_SERVER_PORT``).

.. warning::

   The monitoring endpoints do not require authentication; they are
   protected by being reachable only locally. To let a remote Prometheus
   server scrape the metrics, change ``management.server.address`` and
   restrict access to the port by other means (firewall, reverse proxy
   with authentication). Never publish the port to a public network.

Endpoints
=========

``http://127.0.0.1:8081/actuator/health``
   Overall state (``UP`` / ``DOWN``) with the state of the components
   (database, disk space, LDAP when configured).
``http://127.0.0.1:8081/actuator/health/liveness`` and ``.../readiness``
   Liveness and readiness probes, for load balancers or container
   orchestrators.
``http://127.0.0.1:8081/actuator/prometheus``
   Metrics in the Prometheus format.

Example:

.. code-block:: console

   $ curl http://127.0.0.1:8081/actuator/health
   {"status":"UP","components":{"db":{"status":"UP"},"diskSpace":{"status":"UP"},
    "livenessState":{"status":"UP"},"ping":{"status":"UP"},"readinessState":{"status":"UP"}}}

The LDAP health check is included only when LDAP authentication is
configured (``elza.security.ldap.ad-domain``); it then checks the
configured LDAP server. It can be switched off with
``management.health.ldap.enabled: false``.

ELZA metrics
============

Besides the standard metrics of the JVM, the HTTP server
(``http_server_requests_*``), the database connection pool (HikariCP) and
the system, ELZA publishes metrics of its synchronisation queue with
external systems (typically CAM) and of its task scheduler. The metrics
have no labels; queue values are aggregated over all configured external
systems. Age metrics return ``-1`` when there is nothing to measure.

.. list-table::
   :header-rows: 1
   :widths: 34 46 20

   * - Metric
     - Meaning
     - Suggested alert
   * - ``elza_cam_last_success_age_seconds``
     - Seconds since the last successful poll of a CAM external system
       (the oldest across all systems). A poll that finds no changes counts
       as successful. The main signal of CAM availability.
     - above 7200
   * - ``elza_cam_error_count``
     - Number of synchronisation items in the error state (for example a
       failed upload to CAM).
     - above 0
   * - ``elza_cam_oldest_error_age_seconds``
     - Seconds since the oldest item in the error state last changed
       state. Gives severity to the error count and avoids alerts on
       transient failures.
     - above 3600
   * - ``elza_cam_deferred_oldest_age_seconds``
     - Seconds since the oldest deferred item last changed state (for
       example a replacement whose replacing entity is not available yet).
       Deferred items normally resolve themselves.
     - above 86400
   * - ``elza_cam_pending_oldest_age_seconds``
     - Seconds since the oldest item waiting to be downloaded or uploaded
       entered its state. Grows when the queue is not processed, which the
       last-success metric cannot see.
     - above 3600
   * - ``elza_scheduler_heartbeat_age_seconds``
     - Seconds since the task scheduler last ticked. Grows without limit
       when the scheduler is frozen, which stops CAM synchronisation and
       all other scheduled tasks until a restart.
     - above 300
   * - ``elza_scheduler_longest_running_task_seconds``
     - Seconds the longest running scheduled task has been running;
       ``-1`` when idle. For diagnostics; a task running longer than
       ``elza.monitoring.scheduler.stuckThresholdSeconds`` is also logged
       with its stack trace.
     - none
   * - ``elza_users_connected``
     - Number of connected users (WebSocket sessions). Informational.
     - none

The machine-readable list of these metrics with the suggested thresholds
is part of the distribution: :file:`server/config/csc-metrics.json`.

The scheduler can be tuned with ``elza.monitoring.scheduler.poolSize``
(default 4 threads), ``elza.monitoring.scheduler.heartbeatMs`` (default
30000) and ``elza.monitoring.scheduler.stuckThresholdSeconds`` (default
1800).

.. _monitoring-reporting:

Reporting to a supervision service
==================================

The ``elza-health`` command of the install scripts can report the state
of the instance to a remote supervision service. LightComp v.o.s. offers
such a service as part of its support (Customer Service Center, CSC);
another system implementing the same interface can be used as well.

- Reporting is active only when the access credentials
  (``CUSTOMER_ID`` and ``CUSTOMER_SERVICE_SECRET`` in :file:`elza-env`)
  are set. Without them, the scripts only check the state locally and
  send nothing.
- Communication goes only from the instance to the supervision service
  (push) over HTTPS; each report is signed with the customer's key
  (HMAC). The supervision service never connects to the customer's
  infrastructure.

Each report (every 10 minutes by default, ``HEARTBEAT_INTERVAL``)
contains:

- the overall state of the instance (``up`` / ``degraded`` / ``down``),
- the results of the individual checks (systemd service state, HTTP
  check),
- the application version and its start time,
- the host name,
- the values of the metrics listed in :file:`csc-metrics.json`.

Only numeric values of the listed metrics are sent (an allow-list); no
archival content, personal data or complete metrics output. To stop
sending metrics while keeping the state reports, set ``CSC_METRICS_URL``
to an empty value; to stop reporting completely, remove the credentials.

The other settings of the scripts are described at
https://get.lightcomp.com/elza/.
