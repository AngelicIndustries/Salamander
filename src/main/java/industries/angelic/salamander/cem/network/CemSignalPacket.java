package industries.angelic.salamander.cem.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

/** A transient snapshot/update for a single tracking session, never entity NBT. */
public final class CemSignalPacket implements IMessage {

    public static final int SNAPSHOT = 0, UPDATE = 1, REMOVE = 2;
    public int version;
    public int dimension;
    public int entityId;
    public long token;
    public int sequence;
    public int kind;
    public boolean aggressive;

    public CemSignalPacket() {}

    public CemSignalPacket(int dimension, int entityId, long token, int sequence, int kind, boolean aggressive) {
        this.version = CemNetwork.VERSION;
        this.dimension = dimension;
        this.entityId = entityId;
        this.token = token;
        this.sequence = sequence;
        this.kind = kind;
        this.aggressive = aggressive;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        version = 0;
        if (buffer.readableBytes() != 26) return;
        version = buffer.readInt();
        dimension = buffer.readInt();
        entityId = buffer.readInt();
        token = buffer.readLong();
        sequence = buffer.readInt();
        kind = buffer.readUnsignedByte();
        aggressive = buffer.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(version);
        buffer.writeInt(dimension);
        buffer.writeInt(entityId);
        buffer.writeLong(token);
        buffer.writeInt(sequence);
        buffer.writeByte(kind);
        buffer.writeBoolean(aggressive);
    }

    public boolean valid() {
        return version == CemNetwork.VERSION && entityId >= 0 && token > 0 && kind >= SNAPSHOT && kind <= REMOVE;
    }
}
