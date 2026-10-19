package industries.angelic.salamander.mixins.early.minecraft.client.model;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.model.ModelBook;
import net.minecraft.client.model.ModelRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import industries.angelic.salamander.cem.client.CemModelParts;

@Mixin(ModelBook.class)
public abstract class MixinModelBookCemParts implements CemModelParts {

    @Shadow
    private ModelRenderer coverRight;
    @Shadow
    private ModelRenderer coverLeft;
    @Shadow
    private ModelRenderer pagesRight;
    @Shadow
    private ModelRenderer pagesLeft;
    @Shadow
    private ModelRenderer flippingPageRight;
    @Shadow
    private ModelRenderer flippingPageLeft;
    @Shadow
    private ModelRenderer bookSpine;

    @Override
    public Map<String, ModelRenderer> salamander$cemParts() {
        Map<String, ModelRenderer> parts = new LinkedHashMap<>();
        parts.put("cover_right", coverRight);
        parts.put("cover_left", coverLeft);
        parts.put("pages_right", pagesRight);
        parts.put("pages_left", pagesLeft);
        parts.put("flipping_page_right", flippingPageRight);
        parts.put("flipping_page_left", flippingPageLeft);
        parts.put("book_spine", bookSpine);
        return parts;
    }
}
