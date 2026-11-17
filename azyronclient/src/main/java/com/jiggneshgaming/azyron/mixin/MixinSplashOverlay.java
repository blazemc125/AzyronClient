package com.jiggneshgaming.azyron.mixin;

import com.jiggneshgaming.azyron.util.ClientRefs;
import com.jiggneshgaming.azyron.util.Texts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Rebrands the startup splash with AZYRON CLIENT chrome:
 * "Welcome to AZYRON CLIENT — Performance. Smoothness. Style."
 *
 * <p>Optional mixin — failure to apply cannot block game startup.</p>
 */
@Mixin(SplashOverlay.class)
public abstract class MixinSplashOverlay {

    @Inject(method = "render", at = @At("TAIL"))
    private void azyronclient$brandLoadingScreen(DrawContext context, int mouseX, int mouseY,
                                                 float delta, CallbackInfo ci) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            TextRenderer tr = ClientRefs.textRenderer(client);
            int width = ClientRefs.scaledWidth(client);
            int height = ClientRefs.scaledHeight(client);
            int centerX = width / 2;

            // Accent divider line.
            int lineY = height / 2 + 64;
            for (int i = -110; i < 110; i++) {
                float t = (i + 110) / 220.0F;
                int alpha = (int) (220 * (1.0F - Math.abs(t - 0.5F) * 2.0F));
                int color = (Math.max(0, alpha) << 24) | (t < 0.5F ? 0x7C3AED : 0x33CCFF);
                context.fill(centerX + i, lineY, centerX + i + 1, lineY + 1, color);
            }

            String welcome = "Welcome to AZYRON CLIENT";
            String tagline = "PERFORMANCE. SMOOTHNESS. STYLE.";
            if (tr != null) {
                Texts.drawCentered(context, tr, welcome, centerX, lineY + 14, 0xFFA78BFA, true);
                Texts.drawCentered(context, tr, tagline, centerX, lineY + 28, 0xFF33CCFF, true);
            }
        } catch (Throwable ignored) {
            // Splash render must never crash startup.
        }
    }
}
