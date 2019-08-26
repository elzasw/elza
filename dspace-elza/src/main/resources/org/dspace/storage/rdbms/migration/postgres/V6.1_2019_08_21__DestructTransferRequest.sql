/**
 * Created by Marbes Consulting
 * ludek.cacha@marbes.cz / 20.06.2019.
 */
-- DestructionRequest table

DROP TABLE destruc_transfer_request;

CREATE TABLE destruc_transfer_request
(
  request_id        INTEGER NOT NULL PRIMARY KEY DEFAULT nextval('destructionrequest_seq'),
  type              VARCHAR(30) NOT NULL,
  identifier        VARCHAR(50) NOT NULL,
  status            VARCHAR(20) NOT NULL,
  rejected_message  VARCHAR(1000),
  request_date      TIMESTAMP NOT NULL
);


