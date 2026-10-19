package com.geckolib.network.packet.blockentity;

import com.geckolib.network.GeckoLibNetwork;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import industries.angelic.salamander.Salamander;
import io.netty.buffer.ByteBuf;

/** Stops one registered triggered animation on a client-side block entity. */
public final class StopTriggeredBlockEntityAnimPacket implements IMessage {

    private static final int MAX_NAME_LENGTH = 256;

    private int protocolVersion;
    private int x;
    private int y;
    private int z;
    private String controllerName;
    private String animationName;

    @SuppressWarnings("unused")
    public StopTriggeredBlockEntityAnimPacket() {}

    public StopTriggeredBlockEntityAnimPacket(int x, int y, int z, String controllerName, String animationName) {
        this.protocolVersion = GeckoLibNetwork.PROTOCOL_VERSION;
        this.x = x;
        this.y = y;
        this.z = z;
        this.controllerName = controllerName;
        this.animationName = animationName;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        this.protocolVersion = buffer.readInt();
        this.x = buffer.readInt();
        this.y = buffer.readInt();
        this.z = buffer.readInt();
        this.controllerName = readNullableString(buffer);
        this.animationName = readNullableString(buffer);
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(this.protocolVersion);
        buffer.writeInt(this.x);
        buffer.writeInt(this.y);
        buffer.writeInt(this.z);
        writeNullableString(buffer, this.controllerName);
        writeNullableString(buffer, this.animationName);
    }

    public int x() {
        return this.x;
    }

    public int y() {
        return this.y;
    }

    public int z() {
        return this.z;
    }

    public String controllerName() {
        return this.controllerName;
    }

    public String animationName() {
        return this.animationName;
    }

    public boolean isValid() {
        return this.protocolVersion == GeckoLibNetwork.PROTOCOL_VERSION && isValidNullableName(this.controllerName)
            && isValidNullableName(this.animationName);
    }

    private static String readNullableString(ByteBuf buffer) {
        return buffer.readBoolean() ? ByteBufUtils.readUTF8String(buffer) : null;
    }

    private static void writeNullableString(ByteBuf buffer, String value) {
        buffer.writeBoolean(value != null);

        if (value != null) ByteBufUtils.writeUTF8String(buffer, value);
    }

    private static boolean isValidNullableName(String name) {
        return name == null || !name.isEmpty() && name.length() <= MAX_NAME_LENGTH;
    }

    public static final class Handler implements IMessageHandler<StopTriggeredBlockEntityAnimPacket, IMessage> {

        @Override
        public IMessage onMessage(StopTriggeredBlockEntityAnimPacket message, MessageContext context) {
            if (message.isValid()) {
                Salamander.proxy.handleStopTriggeredBlockEntityAnimation(
                    message.x(),
                    message.y(),
                    message.z(),
                    message.controllerName(),
                    message.animationName());
            }

            return null;
        }
    }
}
