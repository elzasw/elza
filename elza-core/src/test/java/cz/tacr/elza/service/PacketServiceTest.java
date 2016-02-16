package cz.tacr.elza.service;

import java.util.List;

import javax.transaction.Transactional;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import cz.tacr.elza.domain.RulPacketType;


/**
 * @author Martin Šlapa
 * @since 16.2.2016
 */
public class PacketServiceTest extends AbstractServiceTest {

    @Autowired
    private PacketService packetService;

    @Test
    public void getPacketTypesTest() {
        List<RulPacketType> packetTypes = packetService.getPacketTypes();

        Assert.notNull(packetTypes);
        Assert.notEmpty(packetTypes);
    }

    @Test
    public void getPacketsTest() {
        // TODO
    }

    @Test
    @Transactional
    public void insertPacketTest() {
        // TODO
    }

    @Test
    @Transactional
    public void updatePacketTest() {
        // TODO
    }

    @Test
    public void getPacketTest() {
        // TODO
    }

    @Test
    @Transactional
    public void deactivatePacketTest() {
        // TODO
    }

}
