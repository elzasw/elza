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
     * Vytvoření obalu.
     *
     * @param packet obal
     * @return nový obal
     */
    public ArrPacket insertPacket(final ArrPacket packet) {
        Assert.notNull(packet);
        Assert.isNull(packet.getPacketId());
        Assert.notNull(packet.getStorageNumber());
        Assert.notNull(packet.getState());

        checkPacketDuplicate(packet);

        EventId event = EventFactory
                .createIdEvent(EventType.PACKETS_CHANGE, packet.getFund().getFundId());
        eventNotificationService.publishEvent(event);

        return packetRepository.save(packet);
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
     * Vygenerování / přegenerování obalů.
     *
     * @param fund          archivní fond
     * @param packetType    typ obalu
     * @param prefix        prefix
     * @param fromNumber    generuj od čísla
     * @param lenNumber     počet cifer v pořadí
     * @param count         počet generovaných obalů
     * @param packetIds     přegenerovaný id obalů
     * @return  seznam obalů
     */
    public List<ArrPacket> generatePackets(final ArrFund fund,
                                           final RulPacketType packetType,
                                           final String prefix,
                                           final Integer fromNumber,
                                           final Integer lenNumber,
                                           final Integer count,
                                           final Integer[] packetIds) {
        Assert.notNull(fund);
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

        EventId event = EventFactory
                .createIdEvent(EventType.PACKETS_CHANGE, fund.getFundId());
        eventNotificationService.publishEvent(event);

        return packets;

    }

    /**
     * Sestavení a kontrola storage number.
     *
     * @param prefix            prefix
     * @param lenNumber         počet cifer v pořadí
     * @param storageNumbers    seznam existujících označení
     * @param number            pořadové číslo
     * @return  výsledné storage number
     */
    private String createAndCheckStorageNumber(final String prefix,
                                               final Integer lenNumber,
                                               final List<String> storageNumbers,
                                               final int number) {
        String storageNumber = prefix + String.format("%0" + lenNumber + "d", number);
        checkStorageNumber(storageNumbers, storageNumber);
        return storageNumber;
    }

    /**
     * Kontrola existence storage number.
     *
     * @param storageNumbers    seznam existujících označení
     * @param storageNumber     kontrolované storage number
     */
    private void checkStorageNumber(final List<String> storageNumbers, final String storageNumber) {
        if (storageNumbers.contains(storageNumber)) {
            throw new IllegalStateException("Packet " + storageNumber + " již existuje!");
        }
    }

    /**
     * Vyhledání obalů.
     *
     * @param fund      archivní fond
     * @param prefix    prefix
     * @param state     stav
     * @return  seznam nalezených obalů
     */
    public List<ArrPacket> findPackets(final ArrFund fund, @Nullable final String prefix, final ArrPacket.State state) {
        Assert.notNull(fund);
        Assert.notNull(state);
        return packetRepository.findPackets(fund, prefix, state);
    }

    /**
     * Vyhledání obalů.
     *
     * @param fund  archivní fond
     * @param limit maximální počet
     * @param text  fulltext pro vyhledávání
     * @return  seznam nalezených obalů
     */
    public List<ArrPacket> findPackets(final ArrFund fund, final Integer limit, @Nullable final String text) {
        Assert.notNull(fund);
        Assert.notNull(limit);
        Assert.isTrue(limit > 0, "Limit musí být alespoň 1");
        return packetRepository.findPackets(fund, limit, text, ArrPacket.State.OPEN);
    }

    /**
     * Smazání obalů.
     *
     * @param fund  archivní fond
     * @param packetIds seznam identifikátorů ke smazání
     */
    public void deletePackets(final ArrFund fund, final Integer[] packetIds) {
        Assert.notNull(fund);
        Assert.notNull(packetIds);
        Assert.isTrue(packetIds.length > 0, "Musí být alespoň jeden ke smazání");

        ObjectListIterator<Integer> packetIdsIterator = new ObjectListIterator<>(Arrays.asList(packetIds));
        while (packetIdsIterator.hasNext()) {
            packetRepository.deletePackets(fund, packetIdsIterator.next());
        }

        EventId event = EventFactory
                .createIdEvent(EventType.PACKETS_CHANGE, fund.getFundId());
        eventNotificationService.publishEvent(event);
    }

    /**
     * Hromadná změna stavu obalů.
     *
     * @param fund  archivní fond
     * @param packetIds seznam identifikátorů ke změně stavu
     * @param state stav
     */
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
                } else if (packet.getState().equals(ArrPacket.State.CANCELED)) {
                    checkStorageNumber(storageNumbers, packet.getStorageNumber());
                }
                packet.setState(state);
            }
        }

        EventId event = EventFactory
                .createIdEvent(EventType.PACKETS_CHANGE, fund.getFundId());
        eventNotificationService.publishEvent(event);

        packetRepository.save(packets);
    }

}
