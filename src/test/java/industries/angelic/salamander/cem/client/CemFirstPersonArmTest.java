package industries.angelic.salamander.cem.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class CemFirstPersonArmTest {

    @Test
    public void disabledWearDrawsBaseHandAndClaimsEvenAnUnreplacedSleeve() {
        for (boolean replacesSleeve : new boolean[] { false, true }) {
            List<String> calls = new ArrayList<>();
            assertTrue(CemRuntime.firstPersonParts(false, replacesSleeve, () -> calls.add("claim"), name -> {
                calls.add(name);
                return true;
            }));
            assertEquals(Arrays.asList("claim", "right_arm"), calls);
        }
    }

    @Test
    public void allowedPackSleeveIsClaimedAndDrawnBeforeReturningToArmor() {
        List<String> calls = new ArrayList<>();
        boolean[] claimed = { false };
        assertTrue(CemRuntime.firstPersonParts(true, true, () -> {
            claimed[0] = true;
            calls.add("claim");
        }, name -> {
            // WawelAuthBridge.submit only takes over a pack overlay if the current draw has claimed it.
            assertTrue(claimed[0]);
            calls.add(name);
            return true;
        }));
        calls.add("armor");
        // WA's wrapper adds its own sleeve after the original arm call only if the sleeve was not claimed.
        if (!claimed[0]) calls.add("outer_sleeve");
        assertEquals(Arrays.asList("claim", "right_arm", "right_sleeve", "armor"), calls);
    }

    @Test
    public void allowedUnreplacedSleeveKeepsTheNativeFallback() {
        List<String> calls = new ArrayList<>();
        assertTrue(CemRuntime.firstPersonParts(true, false, () -> calls.add("claim"), name -> {
            calls.add(name);
            return name.equals("right_arm");
        }));
        assertEquals(Arrays.asList("right_arm", "right_sleeve"), calls);
    }

    @Test
    public void sleeveHandlingDoesNotTurnAnUnhandledBaseArmIntoASuccess() {
        assertFalse(CemRuntime.firstPersonParts(true, true, () -> {}, name -> name.equals("right_sleeve")));
        assertFalse(CemRuntime.firstPersonParts(false, false, () -> {}, name -> false));
    }
}
