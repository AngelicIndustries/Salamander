package industries.angelic.salamander.cem.client;

/**
 * How many times a model part was drawn in a draw, kept on the part itself so a draw needs no map of them. A model
 * can draw one part object for several CEM parts, which take turns by call.
 */
public interface CemPartCalls {

    /** This part's call number in the draw stamped {@code stamp}: 0 the first time, then 1, and so on. */
    int salamander$nextCall(int stamp);

    /** A stamp no draw has used yet. Client thread only. */
    static int nextStamp() {
        return Stamps.next++;
    }

    /** The last stamp handed out. */
    final class Stamps {

        static int next = 1;

        private Stamps() {}
    }
}
