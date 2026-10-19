package com.geckolib.network.packet.singleton;

import com.geckolib.network.GeckoLibNetwork;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import industries.angelic.salamander.Salamander;
import io.netty.buffer.ByteBuf;

/** Starts one registered triggerable animation on a client-side singleton animatable instance. */
public final class SingletonAnimTriggerPacket implements IMessage {

    private static final int MAX_ID_LENGTH = 512;
    private static final int MAX_NAME_LENGTH = 256;

    private int protocolVersion;
    private String syncableId;
    private long instanceId;
    private String controllerName;
    private String animationName;

    @SuppressWarnings("unused")
    public SingletonAnimTriggerPacket() {}

    public SingletonAnimTriggerPacket(String syncableId, long instanceId, String controllerName, String animationName) {
        this.protocolVersion = GeckoLibNetwork.PROTOCOL_VERSION;
        this.syncableId = syncableId;
        this.instanceId = instanceId;
        this.controllerName = controllerName;
        this.animationName = animationName;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        this.protocolVersion = buffer.readInt();
        this.syncableId = ByteBufUtils.readUTF8String(buffer);
        this.instanceId = buffer.readLong();
        this.controllerName = readNullableString(buffer);
        this.animationName = ByteBufUtils.readUTF8String(buffer);
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(this.protocolVersion);
        ByteBufUtils.writeUTF8String(buffer, this.syncableId);
        buffer.writeLong(this.instanceId);
        writeNullableString(buffer, this.controllerName);
        ByteBufUtils.writeUTF8String(buffer, this.animationName);
    }

    public String syncableId() {
        return this.syncableId;
    }

    public long instanceId() {
        return this.instanceId;
    }

    public String controllerName() {
        return this.controllerName;
    }

    public String animationName() {
        return this.animationName;
    }

    public boolean isValid() {
        return this.protocolVersion == GeckoLibNetwork.PROTOCOL_VERSION && isValidId(this.syncableId)
            && this.instanceId != 0
            && isValidNullableName(this.controllerName)
            && isValidRequiredName(this.animationName);
    }

    private static String readNullableString(ByteBuf buffer) {
        return buffer.readBoolean() ? ByteBufUtils.readUTF8String(buffer) : null;
    }

    private static void writeNullableString(ByteBuf buffer, String value) {
        buffer.writeBoolean(value != null);

        if (value != null) ByteBufUtils.writeUTF8String(buffer, value);
    }

    private static boolean isValidId(String id) {
        return id != null && !id.isEmpty() && id.length() <= MAX_ID_LENGTH;
    }

    private static boolean isValidNullableName(String name) {
        return name == null || isValidRequiredName(name);
    }

    private static boolean isValidRequiredName(String name) {
        return name != null && !name.isEmpty() && name.length() <= MAX_NAME_LENGTH;
    }

    public static final class Handler implements IMessageHandler<SingletonAnimTriggerPacket, IMessage> {

        @Override
        public IMessage onMessage(SingletonAnimTriggerPacket message, MessageContext context) {
            if (message.isValid()) {
                Salamander.proxy.handleSingletonAnimationTrigger(
                    message.syncableId(),
                    message.instanceId(),
                    message.controllerName(),
                    message.animationName());
            }

            return null;
        }
    }
}
