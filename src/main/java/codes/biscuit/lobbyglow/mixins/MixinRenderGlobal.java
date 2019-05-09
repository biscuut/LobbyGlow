package codes.biscuit.lobbyglow.mixins;

import codes.biscuit.lobbyglow.LobbyGlow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(RenderGlobal.class)
public class MixinRenderGlobal {

    @Shadow private WorldClient theWorld;
    @Final @Shadow private RenderManager renderManager;
    @Final @Shadow private Minecraft mc;
    @Shadow private Framebuffer entityOutlineFramebuffer;
    @Shadow private ShaderGroup entityOutlineShader;

    // The old entity outline section is returned false to stop it from executing, replaced with the injection below
    @Redirect(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;isRenderEntityOutlines()Z", ordinal = 0, args = {"log=true"}))
    private boolean onIsRenderEntityOutlines(RenderGlobal renderGlobal) {
        return false;
    }

    // This is the entity outline section of RenderGlobal#renderEntities edited to work outside of spectator mode & stuff
    @Inject(method = "renderEntities", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/profiler/Profiler;endStartSection(Ljava/lang/String;)V", ordinal = 2, args = {"ldc=entities", "log=true"}), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void onRenderEntitySimple(Entity renderViewEntity, ICamera camera, float partialTicks, CallbackInfo ci, int pass, double d0, double d1, double d2, Entity entity, double d3, double d4, double d5, List<Entity> list) { // must add boolean bool, boolean bool1, int i for optifine
        if (this.entityOutlineFramebuffer != null && this.entityOutlineShader != null && this.mc.thePlayer != null && LobbyGlow.INSTANCE.getUtils().isInHypixelLobby()) // Edited to remove the keybind, spectator mode conditions and added condition to be in the hypixel lobby.
        {
            GlStateManager.depthFunc(519);
            GlStateManager.disableFog();
            this.entityOutlineFramebuffer.framebufferClear();
            this.entityOutlineFramebuffer.bindFramebuffer(false);
            this.theWorld.theProfiler.endStartSection("entityOutlines");
            RenderHelper.disableStandardItemLighting();
            this.renderManager.setRenderOutlines(true);
            for (int j = 0; j < list.size(); ++j)
            {
                Entity entity3 = list.get(j);
//                if (!entity3.shouldRenderInPass(pass)) continue; // This condition always seems to come out true, removing it
                boolean flag = this.mc.getRenderViewEntity() instanceof EntityLivingBase && ((EntityLivingBase)this.mc.getRenderViewEntity()).isPlayerSleeping();
                boolean flag1 = entity3.isInRangeToRender3d(d0, d1, d2) && (entity3.ignoreFrustumCheck || camera.isBoundingBoxInFrustum(entity3.getEntityBoundingBox()) || entity3.riddenByEntity == this.mc.thePlayer) && entity3 instanceof EntityPlayer;

                if ((entity3 != this.mc.getRenderViewEntity() || this.mc.gameSettings.thirdPersonView != 0 || flag) && flag1)
                {
                    if (LobbyGlow.INSTANCE.getUtils().shouldGlow(entity3)) {
                        this.renderManager.renderEntitySimple(entity3, partialTicks);
                    }
                }
            }

            this.renderManager.setRenderOutlines(false);
            RenderHelper.enableStandardItemLighting();
            GlStateManager.depthMask(false);
            this.entityOutlineShader.loadShaderGroup(partialTicks);
            GlStateManager.enableLighting();
            GlStateManager.depthMask(true);
            this.mc.getFramebuffer().bindFramebuffer(false);
            GlStateManager.enableFog();
            GlStateManager.enableBlend();
            GlStateManager.enableColorMaterial();
            GlStateManager.depthFunc(515);
            GlStateManager.enableDepth();
            GlStateManager.enableAlpha();
        }
    }
}