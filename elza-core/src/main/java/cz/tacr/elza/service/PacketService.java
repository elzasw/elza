package cz.tacr.elza.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.repository.DataPacketRefRepository;
import cz.tacr.elza.utils.ObjectListIterator;
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

import javax.annotation.Nullable;


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

    @Autowired
    private DataPacketRefRepository dataPacketRefRepository;

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

    public List<ArrPacket> generatePackets(final ArrFund fund,
                                           final RulPacketType packetType,
                                           final String prefix,
                                           final Integer fromNumber,
                                           final Integer lenNumber,
                                           final Integer count,
                                           final Integer[] packetIds) {
        Assert.notNull(fund);
        Assert.notNull(packetType);
        if (count <= 0
            || fromNumber < 0
            || lenNumber <= 0
            || (packetIds != null && packetIds.length <= 0)
            || prefix.length() < 1) {
            throw new IllegalArgumentException("Neplatný vstup");
        }

        List<ArrPacket> packets = new ArrayList<>();

        List<String> storageNumbers = packetRepository.findStorageNumbers(fund, prefix, Arrays.asList(ArrPacket.State.OPEN, ArrPacket.State.CLOSED));
        if (packetIds == null) {
            for (int i = 0; i < count; i++) {
                ArrPacket packet = new ArrPacket();
                packet.setFund(fund);
                packet.setState(ArrPacket.State.OPEN);
                packet.setPacketType(packetType);
                String storageNumber = createAndCheckStorageNumber(prefix, lenNumber, storageNumbers, fromNumber + i);
                storageNumbers.add(storageNumber);
                packet.setStorageNumber(storageNumber);
                packets.add(packet);
            }
            packetRepository.save(packets);
        } else {

            ObjectListIterator<Integer> nodeIdsIterator = new ObjectListIterator<>(Arrays.asList(packetIds));
            while (nodeIdsIterator.hasNext()) {
                packets.addAll(packetRepository.findPackets(fund, nodeIdsIterator.next()));
            }

            for (int i = 0; i < packets.size(); i++) {
                ArrPacket packet = packets.get(i);
                packet.setPacketType(packetType);
                String storageNumber = createAndCheckStorageNumber(prefix, lenNumber, storageNumbers, fromNumber + i);
                storageNumbers.add(storageNumber);
                packet.setStorageNumber(storageNumber);
            }
            packetRepository.save(packets);
        }

        return packets;

    }

    private String createAndCheckStorageNumber(final String prefix,
                                               final Integer lenNumber,
                                               final List<String> storageNumbers,
                                               final int number) {
        String storageNumber = prefix + String.format("%0" + lenNumber + "d", number);
        checkStorageNumber(storageNumbers, storageNumber);
        return storageNumber;
    }

    private void checkStorageNumber(final List<String> storageNumbers, final String storageNumber) {
        if (storageNumbers.contains(storageNumber)) {
            throw new IllegalStateException("Packet " + storageNumber + " již existuje!");
        }
    }

    public List<ArrPacket> findPackets(final ArrFund fund, @Nullable final String prefix, final ArrPacket.State state) {
        Assert.notNull(fund);
        Assert.notNull(state);
        return packetRepository.findPackets(fund, prefix, state);
    }

    public List<ArrPacket> findPackets(final ArrFund fund, final Integer limit, @Nullable final String text) {
        Assert.notNull(fund);
        Assert.notNull(limit);
        Assert.isTrue(limit > 0, "Limit musí být alespoň 1");
        return packetRepository.findPackets(fund, limit, text, ArrPacket.State.OPEN);
    }

    public void deletePackets(final ArrFund fund, final Integer[] packetIds) {
        Assert.notNull(fund);
        Assert.notNull(packetIds);
        Assert.isTrue(packetIds.length > 0, "Musí být alespoň jeden ke smazání");

        ObjectListIterator<Integer> packetIdsIterator = new ObjectListIterator<>(Arrays.asList(packetIds));
        while (packetIdsIterator.hasNext()) {
            packetRepository.deletePackets(fund, packetIdsIterator.next());
        }
    }

    public void setStatePackets(final ArrFund fund, final Integer[] packetIds, final ArrPacket.State state) {
        Assert.notNull(fund);
        Assert.notNull(packetIds);
        Assert.notNull(state);
        Assert.isTrue(packetIds.length > 0, "Musí být alespoň jeden ke změně stavu");

        List<ArrPacket> packets = new ArrayList<>();
        ObjectListIterator<Integer> nodeIdsIterator = new ObjectListIterator<>(Arrays.asList(packetIds));
        while (nodeIdsIterator.hasNext()) {
            packets.addAll(packetRepository.findPackets(fund, nodeIdsIterator.next()));
        }

        if (state.equals(ArrPacket.State.CANCELED)) {
            ArrFundVersion openVersion = fund.getVersions().stream().filter(x -> x.getLockChange() == null).findFirst().get();

            ObjectListIterator<Integer> packetIdsIterator = new ObjectListIterator<>(Arrays.asList(packetIds));
            int count = 0;
            while (packetIdsIterator.hasNext()) {
                count += dataPacketRefRepository.countInFundVersionByPacketIds(packetIdsIterator.next(), openVersion);
            }

            if (count > 0) {
                throw new IllegalArgumentException("V otevřené verzi fondu existuje přiřazený obal");
            }

            for (ArrPacket packet : packets) {
                if (packet.getState().equals(state)) {
                    throw new IllegalArgumentException("Nelze nastavovat stav na stejný: " + state);
                }
                packet.setState(state);
            }
        } else {
            List<String> storageNumbers = packetRepository.findStorageNumbers(fund, Arrays.asList(ArrPacket.State.OPEN, ArrPacket.State.CLOSED));
            for (ArrPacket packet : packets) {
                if (packet.getState().equals(state)) {
                    throw new IllegalArgumentException("Nelze nastavovat stav na stejný: " + state);
                }

                checkStorageNumber(storageNumbers, packet.getStorageNumber());
                packet.setState(state);
            }
        }

        packetRepository.save(packets);
    }

}
