package industries.angelic.salamander.cem.client;

import java.util.Map;

import net.minecraft.client.model.ModelRenderer;

/** Native model bindings. Implemented by mixins or by a mod's own model; no renderer replacement is required. */
public interface CemModelParts {

    Map<String, ModelRenderer> salamander$cemParts();
}
