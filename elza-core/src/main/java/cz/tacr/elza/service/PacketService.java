package cz.tacr.elza.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.domain.RulPacketType;
import cz.tacr.elza.repository.FundRepository;
import cz.tacr.elza.repository.PacketRepository;
import cz.tacr.elza.repository.PacketTypeRepository;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventId;
import cz.tacr.elza.service.eventnotification.events.EventType;


/**
 * Servisní třídy pro obaly.
 *
 * @author Martin Šlapa
 * @since 22.1.2016
 */
@Service
public class PacketService {

    @Autowired
    private PacketRepository packetRepository;

    @Autowired
    private FundRepository fundRepository;

    @Autowired
    private PacketTypeRepository packetTypeRepository;

    @Autowired
    private EventNotificationService eventNotificationService;

    /**
     * Vrací seznam typů obalů.
     *
     * @return seznam typů obalů
     */
    public List<RulPacketType> getPacketTypes() {
        return packetTypeRepository.findAll();
    }

    /**
     * Vyhledá obaly podle archivní pomůcky.
     *
     * @param fundId identifikátor archivní pomůcky
     * @return seznam obalů
     */
    public List<ArrPacket> getPackets(final Integer fundId) {
        Assert.notNull(fundId);

        ArrFund fund = fundRepository.findOne(fundId);
        Assert.notNull(fund, "Archivní pomůcka neexistuje (ID=" + fundId + ")");

        return packetRepository.findByFund(fund);
    }

    /**
     * Vytvoření obalu.
     *
     * @param packet obal
     * @return nový obal
     */
    public ArrPacket insertPacket(final ArrPacket packet) {
        Assert.notNull(packet);
        Assert.isNull(packet.getPacketId());
        return updatePacket(packet);
    }

    /**
     * Otestování duplicity obalu.
     *
     * @param packet testovaný obal
     */
    private void checkPacketDuplicate(final ArrPacket packet) {
        ArrPacket packetDb = packetRepository
                .findByFundAndStorageNumber(packet.getFund(), packet.getStorageNumber());

        if (packetDb != null && !packetDb.getPacketId().equals(packet.getPacketId())) {
            throw new IllegalArgumentException(
                    "Obal s " + packet.getStorageNumber() + " číslem pro tuto archivní pomůcku již existuje.");
        }
    }

    /**
     * Úprava obalu.
     *
     * @param packet upravovaný obal
     * @return upravený obal
     */
    public ArrPacket updatePacket(final ArrPacket packet) {
        Assert.notNull(packet);

        checkPacketDuplicate(packet);

        EventId event = EventFactory
                .createIdEvent(EventType.PACKETS_CHANGE, packet.getFund().getFundId());
        eventNotificationService.publishEvent(event);

        return packetRepository.save(packet);
    }

    /**
     * Vrací obal podle identifikátoru archivní pomůcky a identifikátoru obalu.
     *
     * @param fundId identifikátor archivní pomůcky
     * @param packetId     identifikátor obalu
     * @return obal
     */
    public ArrPacket getPacket(final Integer fundId, final Integer packetId) {
        ArrPacket packet = packetRepository.findOne(packetId);
        Assert.notNull(packet, "Obal neexistuje (ID=" + packetId + ")");
        if (!packet.getFund().getFundId().equals(fundId)) {
            throw new IllegalStateException("Obal nepatří pod archivní pomůcku");
        }
        return packet;
    }

    /**
     * Deaktivace obalu.
     *
     * @param packet deaktivovaný obal
     * @return deaktivovaný obal
     */
    public ArrPacket deactivatePacket(final ArrPacket packet) {
        Assert.notNull(packet);
        packet.setInvalidPacket(Boolean.TRUE);
        return updatePacket(packet);
    }
}
