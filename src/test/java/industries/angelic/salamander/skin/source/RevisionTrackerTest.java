package industries.angelic.salamander.skin.source;

import org.junit.Assert;
import org.junit.Test;

public class RevisionTrackerTest {

    @Test
    public void sameSourceKeepsTheValueAndRevision() {
        RevisionTracker<String, String, Long> tracker = new RevisionTracker<>(8);
        long first = tracker.get("a", "skin-1", revision -> revision);
        long again = tracker.get("a", "skin-1", revision -> revision);
        Assert.assertEquals(first, again);
    }

    @Test
    public void changedSourceGetsANewRevision() {
        RevisionTracker<String, String, Long> tracker = new RevisionTracker<>(8);
        long first = tracker.get("a", "skin-1", revision -> revision);
        long second = tracker.get("a", "skin-2", revision -> revision);
        Assert.assertTrue(second > first);
    }

    @Test
    public void revisionsAreUniqueAcrossKeys() {
        RevisionTracker<String, String, Long> tracker = new RevisionTracker<>(8);
        long a = tracker.get("a", "same", revision -> revision);
        long b = tracker.get("b", "same", revision -> revision);
        Assert.assertNotEquals(a, b);
    }

    @Test
    public void leastRecentlyUsedKeysAreDropped() {
        RevisionTracker<Integer, String, Long> tracker = new RevisionTracker<>(2);
        long first = tracker.get(1, "s", revision -> revision);
        tracker.get(2, "s", revision -> revision);
        tracker.get(1, "s", revision -> revision);
        tracker.get(3, "s", revision -> revision);
        Assert.assertEquals(2, tracker.size());
        Assert.assertEquals(first, (long) tracker.get(1, "s", revision -> revision));
        Assert.assertNotEquals(first, (long) tracker.get(2, "s", revision -> revision));
    }

}
