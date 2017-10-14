package com.melonfishy.florabeats;

import android.content.Context;

import com.melonfishy.florabeats.data.RhythmPlant;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;

/**
 * Created by bakafish on 8/25/17.
 */
@RunWith(MockitoJUnitRunner.class)
public class RhythmMechanicsTest {
    @Test
    public void rhythmPlantTest() throws Exception {
        RhythmPlant plant = new RhythmPlant();
        plant.addChild(0);
        plant.addChild(0);
        plant.addChild(1);
        plant.addChild(2);
        plant.addChild(1);
        plant.addChild(2);
        String export = plant.exportDataString();
        String expected = "[0_null, 1_0, 2_0, 3_1, 4_2, 5_1, 6_2]";
        assertEquals("Export data strings failed to match: \n" +
                "Expected - " + expected + "\n" + "Got - " +
                export, expected, export);
        RhythmPlant clone = new RhythmPlant(export);
        assertEquals(true, plant.equals(clone));
    }

    @Mock
    Context mMockContext;
}
