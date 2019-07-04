/**
 * Created by Marbes Consulting
 * ludek.cacha@marbes.cz / 20.06.2019.
 */
-- DestructionRequest table
CREATE TABLE destruc_transfer_request
(
  request_id        INTEGER NOT NULL PRIMARY KEY,
  uuid              VARCHAR(50) NOT NULL UNIQUE,
  request_type      VARCHAR(50) NOT NULL,
  dao_identifiers   TEXT,
  identifier        VARCHAR(50) NOT NULL UNIQUE,
  system_identifier VARCHAR(50),
  description       VARCHAR(1000),
  user_name         VARCHAR(50),
  target_fund       VARCHAR(50),
  status            VARCHAR(20) NOT NULL,
  request_date      TIMESTAMP NOT NULL,
  processing_date   TIMESTAMP,
  rejected_message  VARCHAR(1000)
);

CREATE SEQUENCE destructionrequest_seq;

