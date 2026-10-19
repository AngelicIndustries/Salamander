package industries.angelic.salamander.debug.entity;

import net.minecraft.entity.EntityCreature;
import net.minecraft.world.World;

/** Inert entity used to render Ice and Fire's fire-dragon Tabula model. */
public final class DebugCitadelDragon extends EntityCreature {

    public DebugCitadelDragon(World world) {
        super(world);
        setSize(3, 2.5F);
        this.stepHeight = 1;
        func_110163_bv();
    }

    @Override
    protected boolean isAIEnabled() {
        return false;
    }

    @Override
    protected boolean canDespawn() {
        return false;
    }
}
