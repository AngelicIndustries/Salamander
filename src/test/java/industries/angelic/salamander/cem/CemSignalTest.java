package industries.angelic.salamander.cem;

import static org.junit.Assert.*;

import org.junit.Test;

import industries.angelic.salamander.cem.network.CemSignalPacket;
import industries.angelic.salamander.cem.network.CemSignalState;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class CemSignalTest {

    @Test
    public void roundTripAndRejectInvalidWireData() {
        CemSignalPacket original = packet(10, 4, CemSignalPacket.UPDATE, true);
        ByteBuf buffer = Unpooled.buffer();
        try {
            original.toBytes(buffer);
            CemSignalPacket decoded = new CemSignalPacket();
            decoded.fromBytes(buffer);
            assertTrue(decoded.valid());
            assertEquals(10, decoded.token);
            assertEquals(4, decoded.sequence);
            assertTrue(decoded.aggressive);
            assertEquals(CemSignalPacket.UPDATE, decoded.kind);
            decoded.version++;
            assertFalse(decoded.valid());
            buffer.clear();
            buffer.writeByte(1);
            decoded = new CemSignalPacket();
            decoded.fromBytes(buffer);
            assertFalse(decoded.valid());
        } finally {
            buffer.release();
        }
    }

    @Test
    public void snapshotsDontInventAttacksAndUpdatesDontReplayThem() {
        CemSignalState state = new CemSignalState(packet(10, 5, CemSignalPacket.SNAPSHOT, true));
        assertTrue(state.aggressive());
        assertEquals(-1, state.swing(100, .5f), 0);
        state.update(packet(10, 6, CemSignalPacket.UPDATE, true), 100);
        assertEquals(.5, state.swing(103, 0), 0);
        state.update(packet(10, 6, CemSignalPacket.UPDATE, false), 103);
        assertFalse(state.aggressive());
        assertEquals(.5, state.swing(103, 0), 0);
        state.update(packet(9, 100, CemSignalPacket.UPDATE, true), 103);
        state.update(packet(10, 5, CemSignalPacket.UPDATE, true), 103);
        assertFalse(state.aggressive());
        assertEquals(-1, state.swing(106, 0), 0);
    }

    private static CemSignalPacket packet(long token, int sequence, int kind, boolean aggressive) {
        return new CemSignalPacket(0, 42, token, sequence, kind, aggressive);
    }

    @Test
    public void attackSequenceWrapsWithoutReplayingOldEvents() {
        CemSignalState state = new CemSignalState(packet(10, Integer.MAX_VALUE, CemSignalPacket.SNAPSHOT, false));
        state.update(packet(10, Integer.MIN_VALUE, CemSignalPacket.UPDATE, true), 100);
        assertTrue(state.aggressive());
        assertEquals(.5, state.swing(103, 0), 0);
        state.update(packet(10, Integer.MAX_VALUE, CemSignalPacket.UPDATE, false), 103);
        assertTrue(state.aggressive());
        assertEquals(.5, state.swing(103, 0), 0);
    }
}
