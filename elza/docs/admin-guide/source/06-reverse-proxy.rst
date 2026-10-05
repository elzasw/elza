==============================
Running Behind a Reverse Proxy
==============================

In production, ELZA runs behind a web server acting as a reverse proxy
(Apache HTTPD or NGINX). The web server terminates HTTPS and forwards the
requests to ELZA's HTTP port (8080 by default, see ``server.port`` in
:doc:`04-configuration`).

The proxy must also forward WebSocket connections: the client receives
notifications over the path ``/stomp``. Without it the application loads,
but users do not see changes made by others or the progress of
background tasks.

ELZA on its own host name
=========================

Example for ``https://elza.archive.example``.

Apache HTTPD
------------

Requires the modules ``mod_proxy``, ``mod_proxy_http`` and
``mod_proxy_wstunnel``.

.. code-block:: apache

   <VirtualHost *:443>
     ServerName elza.archive.example
     # SSLEngine, certificates ...

     ProxyPreserveHost On
     ProxyRequests Off
     ProxyTimeout 60

     ProxyPass        "/stomp" "ws://app-server.internal:8080/stomp"
     ProxyPassReverse "/stomp" "ws://app-server.internal:8080/stomp"

     ProxyPass        "/" "http://app-server.internal:8080/"
     ProxyPassReverse "/" "http://app-server.internal:8080/"
   </VirtualHost>

NGINX
-----

.. code-block:: nginx

   server {
     listen 443 ssl;
     listen [::]:443 ssl;
     server_name elza.archive.example;

     ssl_certificate     /etc/cert/server-chain.pem;
     ssl_certificate_key /etc/cert/server-key.pem;

     client_max_body_size 100m;

     location / {
       proxy_pass http://localhost:8080;
       proxy_set_header Host $host;
       proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
       proxy_set_header X-Forwarded-Proto $scheme;

       # WebSocket
       proxy_http_version 1.1;
       proxy_set_header Upgrade $http_upgrade;
       proxy_set_header Connection "upgrade";
     }
   }

``client_max_body_size`` has to allow the largest upload permitted by
ELZA (``elza.upload.max_request_size``, 100 MB by default).

ELZA under a path
=================

ELZA can also run under a path of an existing site, for example
``https://www.archive.example/elza``. The proxy then passes the original
host, protocol and path prefix in ``X-Forwarded-*`` headers, and ELZA has
to be told to accept them:

.. code-block:: yaml

   elza:
     security:
       acceptForwardedHeaders: true

.. warning::

   Enable ``acceptForwardedHeaders`` only when ELZA is reachable solely
   through the proxy. Otherwise a client could send forged
   ``X-Forwarded-*`` headers directly.

Apache HTTPD example, forwarding to the internal server
``10.0.0.27``:

.. code-block:: apache

   <VirtualHost *:443>
     ServerName www.archive.example

     <Location "/elza">
       RequestHeader set X-Forwarded-Prefix "/elza"
       RequestHeader set X-Forwarded-Host "www.archive.example"
       RequestHeader set X-Forwarded-Proto "https"
     </Location>

     RewriteEngine On
     RewriteCond %{HTTP:Upgrade} websocket [NC]
     RewriteRule "^/elza/(.*)$" "ws://10.0.0.27:8080/$1" [P,UnsafeAllow3F]
     RewriteRule "^/elza/(.*)$" "http://10.0.0.27:8080/$1" [P,UnsafeAllow3F]
   </VirtualHost>

The ``UnsafeAllow3F`` flag is required since Apache HTTPD 2.4.61 for URLs
with an encoded question mark; omit it in older versions.
