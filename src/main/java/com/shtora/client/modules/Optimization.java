package com.shtora.client.modules;

import com.shtora.client.Category;
import com.shtora.client.Module;
import com.shtora.client.setting.BoolSetting;
import com.shtora.client.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.particle.ParticlesMode;

/** Лёгкая оптимизация клиента: меньше частиц, теней и освещения, меньше дистанция рендера. */
public class Optimization extends Module {
    public final BoolSetting minimalParticles = add(new BoolSetting("Minimal particles", true));
    public final BoolSetting noClouds = add(new BoolSetting("No clouds", true));
    public final BoolSetting noShadows = add(new BoolSetting("No entity shadows", true));
    public final BoolSetting noSmoothLighting = add(new BoolSetting("No smooth lighting", true));
    public final BoolSetting fastGraphics = add(new BoolSetting("Fast graphics", true));
    public final BoolSetting restoreOnDisable = add(new BoolSetting("Restore on disable", true));
    public final NumberSetting viewDistance = add(new NumberSetting("View distance", 8, 2, 32, 1));
    public final NumberSetting simulationDistance = add(new NumberSetting("Simulation distance", 6, 2, 32, 1));

    private boolean saved;
    private ParticlesMode oldParticles;
    private CloudRenderMode oldClouds;
    private GraphicsMode oldGraphics;
    private Boolean oldShadows, oldAo;
    private Integer oldView, oldSim;

    public Optimization() { super("Optimization", Category.CLIENT); }

    @Override
    public void onEnable(MinecraftClient mc) { save(mc); apply(mc); }

    @Override
    public void onTick(MinecraftClient mc) {
        apply(mc);
    }

    @Override
    public void onDisable(MinecraftClient mc) {
        if (!saved || !restoreOnDisable.get()) return;
        try {
            if (oldParticles != null) mc.options.getParticles().setValue(oldParticles);
            if (oldClouds != null) mc.options.getCloudRenderMode().setValue(oldClouds);
            if (oldGraphics != null) mc.options.getGraphicsMode().setValue(oldGraphics);
            if (oldShadows != null) mc.options.getEntityShadows().setValue(oldShadows);
            if (oldAo != null) mc.options.getAo().setValue(oldAo);
            if (oldView != null) mc.options.getViewDistance().setValue(oldView);
            if (oldSim != null) mc.options.getSimulationDistance().setValue(oldSim);
        } catch (Throwable ignored) {
        }
        saved = false;
    }

    private void save(MinecraftClient mc) {
        if (saved || mc == null || mc.options == null) return;
        try {
            oldParticles = mc.options.getParticles().getValue();
            oldClouds = mc.options.getCloudRenderMode().getValue();
            oldGraphics = mc.options.getGraphicsMode().getValue();
            oldShadows = mc.options.getEntityShadows().getValue();
            oldAo = mc.options.getAo().getValue();
            oldView = mc.options.getViewDistance().getValue();
            oldSim = mc.options.getSimulationDistance().getValue();
            saved = true;
        } catch (Throwable ignored) {
        }
    }

    private void apply(MinecraftClient mc) {
        if (mc == null || mc.options == null) return;
        save(mc);
        try {
            if (minimalParticles.get()) mc.options.getParticles().setValue(ParticlesMode.MINIMAL);
            if (noClouds.get()) mc.options.getCloudRenderMode().setValue(CloudRenderMode.OFF);
            if (fastGraphics.get()) mc.options.getGraphicsMode().setValue(GraphicsMode.FAST);
            if (noShadows.get()) mc.options.getEntityShadows().setValue(false);
            if (noSmoothLighting.get()) mc.options.getAo().setValue(false);
            mc.options.getViewDistance().setValue(Math.max(2, (int) viewDistance.get()));
            mc.options.getSimulationDistance().setValue(Math.max(2, (int) simulationDistance.get()));
        } catch (Throwable ignored) {
        }
    }
}
