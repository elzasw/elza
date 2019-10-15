package cz.tacr.elza.bulkaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Set;

import org.junit.Test;

import cz.tacr.elza.bulkaction.generator.unitid.AssignedUnitId;
import cz.tacr.elza.bulkaction.generator.unitid.AssignedUnitId.LevelType;
import cz.tacr.elza.bulkaction.generator.unitid.LevelGenerator;
import cz.tacr.elza.bulkaction.generator.unitid.PartSealedUnitId;
import cz.tacr.elza.bulkaction.generator.unitid.PartSealedUnitId.SealType;
import cz.tacr.elza.bulkaction.generator.unitid.SealedLevel;
import cz.tacr.elza.bulkaction.generator.unitid.SealedUnitIdTree;
import cz.tacr.elza.bulkaction.generator.unitid.UnitIdException;
import cz.tacr.elza.bulkaction.generator.unitid.UnitIdPart;

public class UnitIdTest {

    @Test
    public void testPartParser1() {
        // parse "1"
        try {
            UnitIdPart part = UnitIdPart.parse("1");

            assertTrue(part.getParticlesCount() == 1);
            assertTrue(part.getParticle(0) == 1);
        } catch (UnitIdException e) {
            fail(e.toString());
        }
    }

    @Test
    public void testPartParser2() {

        // parse "1+2"
        try {
            UnitIdPart part = UnitIdPart.parse("1+2");

            assertTrue(part.getParticlesCount() == 2);
            assertTrue(part.getParticle(0) == 1);
            assertTrue(part.getParticle(1) == 2);
        } catch (UnitIdException e) {
            fail(e.toString());
        }
    }

    @Test
    public void testPartParser3() {
        // parse "1+2-14"
        try {
            UnitIdPart part = UnitIdPart.parse("1+2-104");

            assertTrue(part.getParticlesCount() == 3);
            assertTrue(part.getParticle(0) == 1);
            assertTrue(part.getParticle(1) == 2);
            assertTrue(part.getParticle(2) == -104);
        } catch (UnitIdException e) {
            fail(e.toString());
        }
    }

    @Test
    public void testPartParser4() {
        // failure "+1"
        try {
            UnitIdPart.parse("+1");
            fail();
        } catch (UnitIdException e) {
        }
    }

    @Test
    public void testPartParser5() {
        try {
            UnitIdPart.parse("1+");
            fail();
        } catch (UnitIdException e) {
        }
    }

    @Test
    public void testPartParser6() {
        try {
            UnitIdPart.parse("1++1");
            fail();
        } catch (UnitIdException e) {
        }
    }

    @Test
    public void testPartParser7() {
        try {
            UnitIdPart.parse("1+-1");
            fail();
        } catch (UnitIdException e) {
        }
    }

    @Test
    public void testPartParser8() {
        try {
            UnitIdPart.parse("a");
            fail();
        } catch (UnitIdException e) {
        }
    }

    @Test
    public void testPartParser9() {
        try {
            UnitIdPart.parse("01");
            fail();
        } catch (UnitIdException e) {
        }
    }

    @Test
    public void testPartParser10() {
        try {
            UnitIdPart.parse("1+0");
            fail();
        } catch (UnitIdException e) {
        }
    }

    @Test
    public void testCompare1() {
        try {
            UnitIdPart part1 = UnitIdPart.parse("1");
            UnitIdPart part2 = UnitIdPart.parse("1");

            assertTrue(part1.compareTo(part2) == 0);
        } catch (UnitIdException e) {
            fail(e.toString());
        }
    }

    @Test
    public void testCompare2() {
        try {
            UnitIdPart part1 = UnitIdPart.parse("1");
            UnitIdPart part2 = UnitIdPart.parse("2");

            assertTrue(part1.compareTo(part2) < 0);
            assertTrue(part2.compareTo(part1) > 0);
        } catch (UnitIdException e) {
            fail(e.toString());
        }
    }

    @Test
    public void testCompare3() {
        try {
            UnitIdPart part1 = UnitIdPart.parse("1+3");
            UnitIdPart part2 = UnitIdPart.parse("1+3");

            assertTrue(part1.compareTo(part2) == 0);
        } catch (UnitIdException e) {
            fail(e.toString());
        }
    }

    @Test
    public void testCompare4() {
        try {
            UnitIdPart part1 = UnitIdPart.parse("1+3");
            UnitIdPart part2 = UnitIdPart.parse("1+5");

            assertTrue(part1.compareTo(part2) < 0);
            assertTrue(part2.compareTo(part1) > 0);
        } catch (UnitIdException e) {
            fail(e.toString());
        }
    }

    @Test
    public void testCompare5() {
        try {
            UnitIdPart part1 = UnitIdPart.parse("1-5");
            UnitIdPart part2 = UnitIdPart.parse("1+5");

            assertTrue(part1.compareTo(part2) < 0);
            assertTrue(part2.compareTo(part1) > 0);
        } catch (UnitIdException e) {
            fail(e.toString());
        }
    }

    @Test
    public void testCompare6() {
        UnitIdPart part1 = null, part2 = null;
        try {
            part1 = UnitIdPart.parse("1");
            part2 = UnitIdPart.parse("1+5");
        } catch (UnitIdException e) {
            fail(e.toString());
        }

        assertTrue(part1.compareTo(part2) < 0);
        assertTrue(part2.compareTo(part1) > 0);
    }

    @Test
    public void testCompare7() {
        UnitIdPart part1 = null, part2 = null;
        try {
            part1 = UnitIdPart.parse("1");
            part2 = UnitIdPart.parse("1+5-2");
        } catch (UnitIdException e) {
            fail(e.toString());
        }

        assertTrue(part1.compareTo(part2) < 0);
        assertTrue(part2.compareTo(part1) > 0);
    }

    @Test
    public void testTreeBuilder1() {
        SealedUnitIdTree root = new SealedUnitIdTree();
        try {
            root.addSealedValue("1", null);

            SealedLevel level = root.getLevel(AssignedUnitId.LevelType.DEFAULT);
            assertTrue(level.getUnitCount() == 1);
            PartSealedUnitId unit = level.getUnit("1");
            assertNotNull(unit);
            assertTrue(unit.isLeaf());
            assertTrue(unit.getDepth() == 1);

            SealedLevel level2 = root.getLevel(AssignedUnitId.LevelType.SLASHED);
            assertTrue(level2.getUnitCount() == 0);
        } catch (UnitIdException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testTreeBuilder2() {
        SealedUnitIdTree root = new SealedUnitIdTree();
        try {
            root.addSealedValue("1", null);
            root.addSealedValue("1//1/2", null);
            root.addSealedValue("1/1/2", null);
            root.addSealedValue("2/1/1", null);
            root.addSealedValue("2/1", null);
            root.addSealedValue("/3", null);

            SealedLevel level = root.getLevel(AssignedUnitId.LevelType.DEFAULT);
            assertTrue(level.getUnitCount() == 2);

            // check "1"
            PartSealedUnitId unit1 = level.getUnit("1");
            assertNotNull(unit1);
            assertTrue(!unit1.isLeaf());
            assertTrue(unit1.isSealed());
            // check child nodes
            SealedLevel level1Def = unit1.getLevel(AssignedUnitId.LevelType.DEFAULT);
            // contains 1/1/2
            assertTrue(level1Def.getUnitCount() == 1);
            PartSealedUnitId unit11 = level1Def.getUnit("1");
            assertNotNull(unit11);
            assertFalse(unit11.isSealed());
            assertFalse(unit11.isLeaf());
            SealedLevel level11Def = unit11.getLevel(AssignedUnitId.LevelType.DEFAULT);
            assertTrue(level11Def.getUnitCount() == 1);
            SealedLevel level11Sls = unit11.getLevel(AssignedUnitId.LevelType.SLASHED);
            assertTrue(level11Sls.getUnitCount() == 0);

            PartSealedUnitId unit112 = level11Def.getUnit("2");
            assertNotNull(unit112);
            assertTrue(unit112.isSealed());
            assertTrue(unit112.isLeaf());
            assertTrue(unit112.getDepth() == 3);

            // check "/3"
            SealedLevel level2 = root.getLevel(AssignedUnitId.LevelType.SLASHED);
            assertTrue(level2.getUnitCount() == 1);
            PartSealedUnitId unit3 = level2.getUnit("3");
            assertNotNull(unit3);
            assertTrue(unit3.isSealed());
            assertTrue(unit3.isLeaf());
            assertTrue(unit3.getDepth() == 1);
        } catch (UnitIdException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testFindLongestSequence1() {

        try {
            ArrayList<UnitIdPart> parts = new ArrayList<>();

            UnitIdPart part1 = UnitIdPart.parse("1");
            parts.add(part1);

            Set<UnitIdPart> result = LevelGenerator.findLongestUnitIdSequence(parts);
            assertTrue(result.size() == 1);
            assertTrue(result.contains(part1));
        } catch (UnitIdException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testFindLongestSequence2() {
        try {
            ArrayList<UnitIdPart> parts = new ArrayList<>();

            UnitIdPart part1 = UnitIdPart.parse("1");
            parts.add(part1);
            UnitIdPart part2 = UnitIdPart.parse("2");
            parts.add(part2);

            Set<UnitIdPart> result = LevelGenerator.findLongestUnitIdSequence(parts);
            assertTrue(result.size() == 2);
            assertTrue(result.contains(part1));
            assertTrue(result.contains(part2));
        } catch (UnitIdException e) {
            fail(e.getMessage());
        }

    }

    @Test
    public void testFindLongestSequence3() {
        try {
            ArrayList<UnitIdPart> parts = new ArrayList<>();

            UnitIdPart part1 = UnitIdPart.parse("2");
            parts.add(part1);
            UnitIdPart part2 = UnitIdPart.parse("1");
            parts.add(part2);

            Set<UnitIdPart> result = LevelGenerator.findLongestUnitIdSequence(parts);
            assertTrue(result.size() == 1);
            assertTrue(result.contains(part2));
        } catch (UnitIdException e) {
            fail(e.getMessage());
        }

    }

    @Test
    public void testFindLongestSequence4() {
        try {
            ArrayList<UnitIdPart> parts = new ArrayList<>();

            UnitIdPart part1 = UnitIdPart.parse("2");
            parts.add(part1);
            UnitIdPart part2 = UnitIdPart.parse("1");
            parts.add(part2);
            UnitIdPart part3 = UnitIdPart.parse("3");
            parts.add(part3);

            Set<UnitIdPart> result = LevelGenerator.findLongestUnitIdSequence(parts);
            assertTrue(result.size() == 2);
            assertTrue(result.contains(part2));
            assertTrue(result.contains(part3));
        } catch (UnitIdException e) {
            fail(e.getMessage());
        }

    }

    @Test
    public void testFindLongestSequence5() {
        try {
            ArrayList<UnitIdPart> parts = new ArrayList<>();

            UnitIdPart part1 = UnitIdPart.parse("10");
            parts.add(part1);
            UnitIdPart part2 = UnitIdPart.parse("11");
            parts.add(part2);
            UnitIdPart part3 = UnitIdPart.parse("1");
            parts.add(part3);
            UnitIdPart part4 = UnitIdPart.parse("2");
            parts.add(part4);
            UnitIdPart part5 = UnitIdPart.parse("3");
            parts.add(part5);

            Set<UnitIdPart> result = LevelGenerator.findLongestUnitIdSequence(parts);
            assertTrue(result.size() == 3);
            assertTrue(result.contains(part3));
            assertTrue(result.contains(part4));
            assertTrue(result.contains(part5));
        } catch (UnitIdException e) {
            fail(e.getMessage());
        }

    }

    @Test
    public void testFindLongestSequence6() {
        try {
            ArrayList<UnitIdPart> parts = new ArrayList<>();

            UnitIdPart part1 = UnitIdPart.parse("10");
            parts.add(part1);
            UnitIdPart part2 = UnitIdPart.parse("1");
            parts.add(part2);
            UnitIdPart part3 = UnitIdPart.parse("11");
            parts.add(part3);
            UnitIdPart part4 = UnitIdPart.parse("2");
            parts.add(part4);
            UnitIdPart part5 = UnitIdPart.parse("12");
            parts.add(part5);

            Set<UnitIdPart> result = LevelGenerator.findLongestUnitIdSequence(parts);
            assertTrue(result.size() == 3);
            assertTrue(result.contains(part2));
            assertTrue(result.contains(part4));
            assertTrue(result.contains(part5));
        } catch (UnitIdException e) {
            fail(e.getMessage());
        }

    }

    @Test
    public void testFindLongestSequence7() {
        try {
            ArrayList<UnitIdPart> parts = new ArrayList<>();

            UnitIdPart part1 = UnitIdPart.parse("100");
            parts.add(part1);
            UnitIdPart part2 = UnitIdPart.parse("10");
            parts.add(part2);
            UnitIdPart part3 = UnitIdPart.parse("1");
            parts.add(part3);
            UnitIdPart part4 = UnitIdPart.parse("101");
            parts.add(part4);
            UnitIdPart part5 = UnitIdPart.parse("11");
            parts.add(part5);
            UnitIdPart part6 = UnitIdPart.parse("2");
            parts.add(part6);
            UnitIdPart part7 = UnitIdPart.parse("102");
            parts.add(part7);

            Set<UnitIdPart> result = LevelGenerator.findLongestUnitIdSequence(parts);
            assertTrue(result.size() == 3);
            assertTrue(result.contains(part3));
            assertTrue(result.contains(part6));
            assertTrue(result.contains(part7));
        } catch (UnitIdException e) {
            fail(e.getMessage());
        }
    }

    // test loBorder
    @Test
    public void testCreateSealed1() {
        SealedUnitIdTree root = new SealedUnitIdTree();
        SealedLevel level = root.getLevel(LevelType.DEFAULT);
        PartSealedUnitId sealed = level.createSealed(null, null);
        assertNotNull(sealed);
        UnitIdPart part = sealed.getPart();
        assertNotNull(part);
        assertTrue(part.equals(UnitIdPart.getLowest()));
    }

    // test loBorder
    @Test
    public void testCreateSealed2() {
        SealedUnitIdTree root = new SealedUnitIdTree();
        try {
            root.addSealedValue("1", null);

            SealedLevel level = root.getLevel(LevelType.DEFAULT);
            PartSealedUnitId sealed = level.createSealed(null, null);
            assertNotNull(sealed);
            UnitIdPart part = sealed.getPart();
            assertNotNull(part);
            assertTrue(part.equals(UnitIdPart.parse("2")));
        } catch (UnitIdException e) {
            fail(e.getMessage());
        }
    }

    // test loBorder
    @Test
    public void testCreateSealed3() {
        SealedUnitIdTree root = new SealedUnitIdTree();
        try {

            UnitIdPart part2 = UnitIdPart.parse("2");
            UnitIdPart part3 = UnitIdPart.parse("3");

            SealedLevel level = root.getLevel(LevelType.DEFAULT);
            PartSealedUnitId sealed = level.createSealed(part2, null);
            assertNotNull(sealed);
            UnitIdPart part = sealed.getPart();
            assertNotNull(part);
            assertTrue(part.equals(part3));
        } catch (UnitIdException e) {
            fail(e.getMessage());
        }
    }

    // test loBorder
    @Test
    public void testCreateSealed4() {
        SealedUnitIdTree root = new SealedUnitIdTree();
        try {
            root.addSealedValue("3", null);

            UnitIdPart part2 = UnitIdPart.parse("2");
            UnitIdPart part4 = UnitIdPart.parse("4");

            SealedLevel level = root.getLevel(LevelType.DEFAULT);
            PartSealedUnitId sealed = level.createSealed(part2, null);
            assertNotNull(sealed);
            UnitIdPart part = sealed.getPart();
            assertNotNull(part);
            assertTrue(part.equals(part4));
        } catch (UnitIdException e) {
            fail(e.getMessage());
        }
    }

    // test loBorder
    @Test
    public void testCreateSealed5() {
        SealedUnitIdTree root = new SealedUnitIdTree();
        try {
            root.addSealedValue("3", null);
            root.addSealedValue("3+1", null);
            root.addSealedValue("4+1", null);
            root.addSealedValue("5", null);

            UnitIdPart part2 = UnitIdPart.parse("2+1");
            UnitIdPart part4 = UnitIdPart.parse("4");

            SealedLevel level = root.getLevel(LevelType.DEFAULT);
            PartSealedUnitId sealed = level.createSealed(part2, null);
            assertNotNull(sealed);
            UnitIdPart part = sealed.getPart();
            assertNotNull(part);
            assertTrue(part.equals(part4));
        } catch (UnitIdException e) {
            fail(e.getMessage());
        }
    }

    // test loBorder
    @Test
    public void testCreateSealed5_1() {
        SealedUnitIdTree root = new SealedUnitIdTree();
        try {
            UnitIdPart part1_m1 = UnitIdPart.parse("1-1");
            UnitIdPart part2 = UnitIdPart.parse("2");

            SealedLevel level = root.getLevel(LevelType.DEFAULT);
            PartSealedUnitId sealed = level.createSealed(part1_m1, null);
            assertNotNull(sealed);
            UnitIdPart part = sealed.getPart();
            assertNotNull(part);
            assertTrue(part.equals(part2));
        } catch (UnitIdException e) {
            fail(e.getMessage());
        }
    }

    // test hiBorder
    // ? "1" -> "1-1" "1"
    @Test
    public void testCreateSealed6() {
        SealedUnitIdTree root = new SealedUnitIdTree();
        try {
            UnitIdPart part1 = UnitIdPart.parse("1");

            SealedLevel level = root.getLevel(LevelType.DEFAULT);
            PartSealedUnitId sealed = level.createSealed(null, part1);
            assertNotNull(sealed);
            UnitIdPart part = sealed.getPart();
            assertNotNull(part);
            assertTrue(part.equals(UnitIdPart.parse("1-1")));
        } catch (UnitIdException e) {
            fail(e.getMessage());
        }
    }

    // test hiBorder
    @Test
    public void testCreateSealed7() {
        SealedUnitIdTree root = new SealedUnitIdTree();
        try {
            root.addSealedValue("1-1", null);
            UnitIdPart part1 = UnitIdPart.parse("1");
            //UnitIdPart part1_1 = UnitIdPart.parse("1-1");

            SealedLevel level = root.getLevel(LevelType.DEFAULT);
            PartSealedUnitId sealed = level.createSealed(null, part1);
            assertNotNull(sealed);
            UnitIdPart part = sealed.getPart();
            assertNotNull(part);
            assertTrue(part.equals(UnitIdPart.parse("1-2")));
        } catch (UnitIdException e) {
            fail(e.getMessage());
        }
    }

    // test hiBorder
    @Test
    public void testCreateSealed7_2() {
        SealedUnitIdTree root = new SealedUnitIdTree();
        try {
            root.addSealedValue("1-3", null);
            root.addSealedValue("1-4", null);

            UnitIdPart part1_m2 = UnitIdPart.parse("1-2");

            SealedLevel level = root.getLevel(LevelType.DEFAULT);
            PartSealedUnitId sealed = level.createSealed(null, part1_m2);
            assertNotNull(sealed);
            UnitIdPart part = sealed.getPart();
            assertNotNull(part);
            assertTrue(part.equals(UnitIdPart.parse("1-5")));
        } catch (UnitIdException e) {
            fail(e.getMessage());
        }
    }

    // test hiBorder
    @Test
    public void testCreateSealed7_3() {
        SealedUnitIdTree root = new SealedUnitIdTree();
        try {
            root.addSealedValue("1", null);
            root.addSealedValue("2", null);
            root.addSealedValue("3", null);

            UnitIdPart part5 = UnitIdPart.parse("5");

            SealedLevel level = root.getLevel(LevelType.DEFAULT);
            PartSealedUnitId sealed = level.createSealed(null, part5);
            assertNotNull(sealed);
            UnitIdPart part = sealed.getPart();
            assertNotNull(part);
            assertTrue(part.equals(UnitIdPart.parse("4")));
        } catch (UnitIdException e) {
            fail(e.getMessage());
        }
    }

    // test hiBorder
    @Test
    public void testCreateSealed7_4() {
        SealedUnitIdTree root = new SealedUnitIdTree();
        try {
            root.addSealedValue("1", null);
            root.addSealedValue("1+1", null);
            root.addSealedValue("2", null);
            root.addSealedValue("2+1", null);
            root.addSealedValue("3", null);

            UnitIdPart part3 = UnitIdPart.parse("3");

            SealedLevel level = root.getLevel(LevelType.DEFAULT);
            PartSealedUnitId sealed = level.createSealed(null, part3);
            assertNotNull(sealed);
            UnitIdPart part = sealed.getPart();
            assertNotNull(part);
            assertTrue(part.equals(UnitIdPart.parse("2+2")));
        } catch (UnitIdException e) {
            fail(e.getMessage());
        }
    }

    // test hiBorder
    @Test
    public void testCreateSealed7_5() {
        SealedUnitIdTree root = new SealedUnitIdTree();
        try {
            UnitIdPart part3 = UnitIdPart.parse("3");

            SealedLevel level = root.getLevel(LevelType.DEFAULT);
            PartSealedUnitId sealed = level.createSealed(null, part3);
            assertNotNull(sealed);
            UnitIdPart part = sealed.getPart();
            assertNotNull(part);
            assertTrue(part.equals(UnitIdPart.parse("2")));
        } catch (UnitIdException e) {
            fail(e.getMessage());
        }
    }

    // test hiBorder
    @Test
    public void testCreateSealed7_6() {
        SealedUnitIdTree root = new SealedUnitIdTree();
        try {
            root.addSealedValue("1-1", null);

            UnitIdPart part1_m1 = UnitIdPart.parse("1");

            SealedLevel level = root.getLevel(LevelType.DEFAULT);
            PartSealedUnitId sealed = level.createSealed(null, part1_m1);
            assertNotNull(sealed);
            UnitIdPart part = sealed.getPart();
            assertNotNull(part);
            assertTrue(part.equals(UnitIdPart.parse("1-2")));
        } catch (UnitIdException e) {
            fail(e.getMessage());
        }
    }

    // test loBoarder and hiBorder
    // space between items
    @Test
    public void testCreateSealed8() {
        SealedUnitIdTree root = new SealedUnitIdTree();
        try {
            root.addSealedValue("2+4", null);
            root.addSealedValue("2+5", null);

            UnitIdPart part3 = UnitIdPart.parse("2+3");
            UnitIdPart part6 = UnitIdPart.parse("2+6");
            UnitIdPart part7 = UnitIdPart.parse("2+7");

            SealedLevel level = root.getLevel(LevelType.DEFAULT);
            PartSealedUnitId sealed = level.createSealed(part3, part7);
            assertNotNull(sealed);
            UnitIdPart part = sealed.getPart();
            assertNotNull(part);
            assertTrue(part.equals(part6));
        } catch (UnitIdException e) {
            fail(e.getMessage());
        }
    }

    // test loBoarder and hiBorder
    @Test
    public void testCreateSealed9() {
        SealedUnitIdTree root = new SealedUnitIdTree();
        try {
            root.addSealedValue("2+4", null);
            root.addSealedValue("2+5", null);

            UnitIdPart part3 = UnitIdPart.parse("2+3");
            UnitIdPart part4_1 = UnitIdPart.parse("2+4+1");
            UnitIdPart part6 = UnitIdPart.parse("2+6");

            SealedLevel level = root.getLevel(LevelType.DEFAULT);
            PartSealedUnitId sealed = level.createSealed(part3, part6);
            assertNotNull(sealed);
            UnitIdPart part = sealed.getPart();
            assertNotNull(part);
            assertTrue(part.equals(part4_1));
        } catch (UnitIdException e) {
            fail(e.getMessage());
        }
    }

    // test loBoarder and hiBorder
    @Test
    public void testCreateSealed10() {
        SealedUnitIdTree root = new SealedUnitIdTree();
        try {
            root.addSealedValue("2+4+2", null);

            UnitIdPart part4_1 = UnitIdPart.parse("2+4+1+5");
            UnitIdPart part4_3 = UnitIdPart.parse("2+4+3");
            UnitIdPart part5 = UnitIdPart.parse("2+5");

            SealedLevel level = root.getLevel(LevelType.DEFAULT);
            PartSealedUnitId sealed = level.createSealed(part4_1, part5);
            assertNotNull(sealed);
            UnitIdPart part = sealed.getPart();
            assertNotNull(part);
            assertTrue(part.equals(part4_3));
        } catch (UnitIdException e) {
            fail(e.getMessage());
        }
    }

    // test loBoarder and hiBorder
    @Test
    public void testCreateSealed11() {
        SealedUnitIdTree root = new SealedUnitIdTree();
        try {
            root.addSealedValue("1", null);
            root.addSealedValue("1+1", null);
            root.addSealedValue("1+1+1-1", null);

            UnitIdPart part1 = UnitIdPart.parse("1");
            UnitIdPart part1_1_1 = UnitIdPart.parse("1+1+1");
            UnitIdPart part1_1_1_m2 = UnitIdPart.parse("1+1+1-2");

            SealedLevel level = root.getLevel(LevelType.DEFAULT);
            PartSealedUnitId sealed = level.createSealed(part1, part1_1_1);
            assertNotNull(sealed);
            UnitIdPart part = sealed.getPart();
            assertNotNull(part);
            assertTrue(part.equals(part1_1_1_m2));
        } catch (UnitIdException e) {
            fail(e.getMessage());
        }
    }

    // test loBoarder and hiBorder
    // tight space
    @Test
    public void testCreateSealed12() {
        SealedUnitIdTree root = new SealedUnitIdTree();
        try {

            UnitIdPart part1_m1 = UnitIdPart.parse("1-1");
            UnitIdPart part1 = UnitIdPart.parse("1");
            UnitIdPart part2 = UnitIdPart.parse("2");

            SealedLevel level = root.getLevel(LevelType.DEFAULT);
            PartSealedUnitId sealed = level.createSealed(part1_m1, part2);
            assertNotNull(sealed);
            UnitIdPart part = sealed.getPart();
            assertNotNull(part);
            assertTrue(part.equals(part1));
        } catch (UnitIdException e) {
            fail(e.getMessage());
        }
    }

    // test loBoarder and hiBorder
    // tight space
    @Test
    public void testCreateSealed13() {
        SealedUnitIdTree root = new SealedUnitIdTree();
        try {

            UnitIdPart part1 = UnitIdPart.parse("1");
            UnitIdPart part2 = UnitIdPart.parse("2");
            UnitIdPart part2_1 = UnitIdPart.parse("2+1");

            SealedLevel level = root.getLevel(LevelType.DEFAULT);
            PartSealedUnitId sealed = level.createSealed(part1, part2_1);
            assertNotNull(sealed);
            UnitIdPart part = sealed.getPart();
            assertNotNull(part);
            assertTrue(part.equals(part2));
        } catch (UnitIdException e) {
            fail(e.getMessage());
        }
    }

    // test loBoarder and hiBorder
    // tight space
    @Test
    public void testCreateSealed14() {
        SealedUnitIdTree root = new SealedUnitIdTree();
        try {
            // add value -> 1 is in the tree but not sealed
            root.addValue("1", SealType.NOT_SEALED, null);

            UnitIdPart part1_m1 = UnitIdPart.parse("1-1");
            UnitIdPart part1 = UnitIdPart.parse("1");
            UnitIdPart part1_1 = UnitIdPart.parse("1+1");

            SealedLevel level = root.getLevel(LevelType.DEFAULT);
            PartSealedUnitId sealed = level.createSealed(part1_m1, part1_1);
            assertNotNull(sealed);
            UnitIdPart part = sealed.getPart();
            assertNotNull(part);
            assertTrue(part.equals(part1));
        } catch (UnitIdException e) {
            fail(e.getMessage());
        }
    }

    // test loBoarder and hiBorder
    // tight space
    @Test
    public void testCreateSealed15() {
        SealedUnitIdTree root = new SealedUnitIdTree();
        try {
            // add value -> 1 is in the tree but not sealed
            root.addSealedValue("1", null);

            UnitIdPart part1_m1 = UnitIdPart.parse("1-1");
            UnitIdPart part1_m1_1 = UnitIdPart.parse("1-1+1");
            UnitIdPart part1_1 = UnitIdPart.parse("1+1");

            SealedLevel level = root.getLevel(LevelType.DEFAULT);
            PartSealedUnitId sealed = level.createSealed(part1_m1, part1_1);
            assertNotNull(sealed);
            UnitIdPart part = sealed.getPart();
            assertNotNull(part);
            assertTrue(part.equals(part1_m1_1));
        } catch (UnitIdException e) {
            fail(e.getMessage());
        }
    }

    // test print
    @Test
    public void testPrint1() {
        SealedUnitIdTree root = new SealedUnitIdTree();
        try {
            // create value 1//2/3+4

            // 1
            SealedLevel level1 = root.getLevel(LevelType.DEFAULT);
            PartSealedUnitId sealed1 = level1.createSealed(null, null);

            // 1//2
            SealedLevel level2 = sealed1.getLevel(LevelType.SLASHED);
            PartSealedUnitId sealed2 = level2.createSealed(UnitIdPart.parse("1"), null);

            // 1//2/3+4
            SealedLevel level3 = sealed2.getLevel(LevelType.DEFAULT);
            PartSealedUnitId sealed3 = level3.createSealed(UnitIdPart.parse("3+3"), UnitIdPart.parse("4"));

            String value = sealed3.getValue();
            assertNotNull(value);
            assertEquals(value, "1//2/3+4");
        } catch (UnitIdException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testFind1() {
        SealedUnitIdTree root = new SealedUnitIdTree();
        try {
            root.addSealedValue("1", null);
            root.addSealedValue("1/1", null);
            root.addSealedValue("1/1+1", null);
            root.addSealedValue("1/2", null);
            root.addSealedValue("1/2//1", null);
            root.addSealedValue("1/2//2", null);
            root.addSealedValue("1/2//2/5", null);
            root.addSealedValue("1/2//2/6", null);
            root.addSealedValue("1/2//2/7-1", null);
            root.addSealedValue("1/2//3", null);
            root.addSealedValue("2", null);
            root.addSealedValue("3", null);

            PartSealedUnitId result1 = root.find("1");
            assertEquals(result1.getValue(), "1");
            PartSealedUnitId result1_2 = root.find("1/2");
            assertEquals(result1_2.getValue(), "1/2");
            // test not found
            PartSealedUnitId result1_3 = root.find("1/3");
            assertNull(result1_3);
            // test find double slash
            PartSealedUnitId result2 = root.find("1/2//2");
            assertEquals(result2.getValue(), "1/2//2");

            // test find with plus
            PartSealedUnitId result3 = root.find("1/1+1");
            assertEquals(result3.getValue(), "1/1+1");

            // test find with minus
            PartSealedUnitId result4 = root.find("1/2//2/7-1");
            assertEquals(result4.getValue(), "1/2//2/7-1");
        } catch (UnitIdException e) {
            fail(e.getMessage());
        }
    }
}
