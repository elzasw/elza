package cz.tacr.elza.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import cz.tacr.elza.domain.HsearchOutboxEvent;

public interface HsearchOutboxEventRepository extends JpaRepository<HsearchOutboxEvent, UUID> {

}
