package cz.tacr.elza.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import cz.tacr.elza.annotation.AuthMethod;
import cz.tacr.elza.annotation.AuthParam;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.domain.RulPacketType;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.DeleteException;
import cz.tacr.elza.exception.Level;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.repository.DataPacketRefRepository;
import cz.tacr.elza.repository.PacketRepository;
import cz.tacr.elza.repository.PacketTypeRepository;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventId;
import cz.tacr.elza.service.eventnotification.events.EventType;
import cz.tacr.elza.utils.ObjectListIterator;


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
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public ArrPacket insertPacket(@AuthParam(type = AuthParam.Type.FUND) final ArrPacket packet) {
        Assert.notNull(packet, "Obal musí být vyplněn");
        Assert.isNull(packet.getPacketId(), "Identifikátor obalu musí být vyplněn");
        Assert.notNull(packet.getStorageNumber(), "Storage number musí být vyplněno");
        Assert.notNull(packet.getState(), "Stav musí být vyplněn");

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
                .findByFundAndStorageNumberAndPacketType(packet.getFund(), packet.getStorageNumber(), packet.getPacketType());

        if (packetDb != null && !packetDb.getPacketId().equals(packet.getPacketId())) {
            throw new BusinessException(
                    "Obal s " + packet.getStorageNumber() + " číslem pro tuto archivní pomůcku již existuje.",
                    ArrangementCode.PACKET_DUPLICATE).set("storageNumber", packet.getStorageNumber());
        }
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
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public List<ArrPacket> generatePackets(@AuthParam(type = AuthParam.Type.FUND) final ArrFund fund,
                                           final RulPacketType packetType,
                                           final String prefix,
                                           final Integer fromNumber,
                                           final Integer lenNumber,
                                           final Integer count,
                                           final Integer[] packetIds) {
        Assert.notNull(fund, "AS musí být vyplněn");
        String prefixFinal = StringUtils.isEmpty(prefix) ? "" : prefix;

        if (count <= 0
            || fromNumber < 0
            || lenNumber <= 0
            || (packetIds != null && packetIds.length <= 0)) {
            throw new IllegalArgumentException("Neplatný vstup");
        }

        List<ArrPacket> packets = new ArrayList<>();

        List<String> storageNumbers = packetRepository.findStorageNumbers(fund, prefixFinal, Arrays.asList(ArrPacket.State.OPEN, ArrPacket.State.CLOSED));
        if (packetIds == null) {
            for (int i = 0; i < count; i++) {
                ArrPacket packet = new ArrPacket();
                packet.setFund(fund);
                packet.setState(ArrPacket.State.OPEN);
                packet.setPacketType(packetType);
                String storageNumber = createAndCheckStorageNumber(prefixFinal, lenNumber, storageNumbers, fromNumber + i);
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
                String storageNumber = createAndCheckStorageNumber(prefixFinal, lenNumber, storageNumbers, fromNumber + i);
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
            throw new BusinessException(
                    "Obal s " + storageNumber + " číslem pro tuto archivní pomůcku již existuje.",
                    ArrangementCode.PACKET_DUPLICATE).set("storageNumber", storageNumber);
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
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public List<ArrPacket> findPackets(@AuthParam(type = AuthParam.Type.FUND) final ArrFund fund,
                                       @Nullable final String prefix,
                                       final ArrPacket.State state) {
        Assert.notNull(fund, "AS musí být vyplněn");
        Assert.notNull(state, "Stav musí být vyplněn");
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
    public List<ArrPacket> findPackets(final ArrFund fund,
                                       final Integer limit,
                                       @Nullable final String text) {
        Assert.notNull(fund, "AS musí být vyplněn");
        Assert.notNull(limit, "Limit musí být vyplněn");
        Assert.isTrue(limit > 0, "Limit musí být alespoň 1");
        return packetRepository.findPackets(fund, limit, text, ArrPacket.State.OPEN);
    }

    /**
     * Smazání obalů.
     *
     * @param fund  archivní fond
     * @param packetIds seznam identifikátorů ke smazání
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public void deletePackets(@AuthParam(type = AuthParam.Type.FUND) final ArrFund fund,
                              final Integer[] packetIds) {
        Assert.notNull(fund, "AS musí být vyplněn");
        Assert.notNull(packetIds, "Musí být vyplněny identifikátory obalů");
        Assert.isTrue(packetIds.length > 0, "Musí být alespoň jeden ke smazání");

        List<Integer> packetIdList = Arrays.asList(packetIds);
        List<ArrPacket> usePackets = dataPacketRefRepository.findUsePacketsByPacketIds(packetIdList);

        if (usePackets.size() > 0) {
            throw new DeleteException("Není možné odstranit použíté obaly: " + usePackets, ArrangementCode.PACKET_DELETE_ERROR)
                    .set("packets", usePackets.stream().map(ArrPacket::getStorageNumber).collect(Collectors.toList())).level(Level.WARNING);
        }

        ObjectListIterator<Integer> packetIdsIterator = new ObjectListIterator<>(packetIdList);
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
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public void setStatePackets(@AuthParam(type = AuthParam.Type.FUND) final ArrFund fund,
                                final Integer[] packetIds,
                                final ArrPacket.State state) {
        Assert.notNull(fund, "AS musí být vyplněn");
        Assert.notNull(packetIds, "Musí být vyplněny identifikátory obalů");
        Assert.notNull(state, "Stav musí být vyplněn");
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
                throw new SystemException("V otevřené verzi fondu existuje přiřazený obal");
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
