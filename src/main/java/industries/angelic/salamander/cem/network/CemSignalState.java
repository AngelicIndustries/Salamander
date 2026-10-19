package industries.angelic.salamander.cem.network;

/** Per-entity cosmetic signals with tracking-session and duplicate-event protection. */
public final class CemSignalState {

    public final long token;
    private int sequence;
    private boolean aggressive;
    private long attackTick = -1;

    public CemSignalState(CemSignalPacket snapshot) {
        if (!snapshot.valid() || snapshot.kind != CemSignalPacket.SNAPSHOT)
            throw new IllegalArgumentException("Expected CEM snapshot");
        token = snapshot.token;
        sequence = snapshot.sequence;
        aggressive = snapshot.aggressive;
    }

    public void update(CemSignalPacket packet, long tick) {
        if (!packet.valid() || packet.kind != CemSignalPacket.UPDATE
            || packet.token != token
            || packet.sequence - sequence < 0) return;
        if (packet.sequence != sequence) attackTick = tick;
        sequence = packet.sequence;
        aggressive = packet.aggressive;
    }

    public boolean aggressive() {
        return aggressive;
    }

    /** Negative means there is no active server-observed swing; callers may use their native swing value. */
    public double swing(long tick, float partialTicks) {
        double elapsed = attackTick < 0 ? 6 : tick - attackTick + partialTicks;
        return elapsed >= 0 && elapsed < 6 ? elapsed / 6 : -1;
    }
}
