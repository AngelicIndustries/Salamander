package industries.angelic.salamander.cem.client;

import java.util.List;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

/**
 * Stands for a pack model part's faces where Angelica's batcher wants a model part: it keys the batcher's cached
 * geometry, which it builds once from these faces. Never drawn itself.
 */
final class CemProxyPart extends ModelRenderer {

    private static final ModelBase OWNER = new ModelBase() {};

    final List<float[]> faces;

    CemProxyPart(List<float[]> faces) {
        super(OWNER);
        // A part adds itself to its model's parts; the owner is only there because a part needs one.
        OWNER.boxList.remove(this);
        this.faces = faces;
    }
}
