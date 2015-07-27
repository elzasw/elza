package cz.tacr.elza.controller;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import cz.tacr.elza.ElzaApp;
import cz.tacr.elza.domain.FindingAid;

/**
 * Testy pro {@link ArrangementManager}.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = ElzaApp.class)
@WebAppConfiguration
public class ArrangementManagerTest {

    @Autowired
    ArrangementManager arrangementManager;

    @Test
    public void testCreateFindingAid() throws Exception {
        arrangementManager.createFindingAid("Test name");
    }

    @Test
    public void testDeleteFindingAid() throws Exception {
        FindingAid findingAid = arrangementManager.createFindingAid("Test name");

        arrangementManager.deleteFindingAid(findingAid.getFindigAidId());
    }

    @Test
    public void testGetFindingAids() throws Exception {
        arrangementManager.createFindingAid("Test name");

        Assert.assertFalse(arrangementManager.getFindingAids().isEmpty());
    }

    @Test
    public void testUpdateFindingAid() throws Exception {
        FindingAid findingAid = arrangementManager.createFindingAid("Test name");

        arrangementManager.updateFindingAid(findingAid.getFindigAidId(), "Update name");
    }
}