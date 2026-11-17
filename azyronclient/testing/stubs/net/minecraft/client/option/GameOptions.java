package net.minecraft.client.option;
public class GameOptions {
    public boolean hudHidden;
    public SimpleOption<Integer> getFov() { return new SimpleOption<Integer>(70); }
    public SimpleOption<Integer> getMaxFps() { return new SimpleOption<Integer>(120); }
    public SimpleOption<GraphicsMode> getGraphicsMode() { return new SimpleOption<GraphicsMode>(GraphicsMode.FAST); }
    public SimpleOption<ParticlesMode> getParticles() { return new SimpleOption<ParticlesMode>(ParticlesMode.ALL); }
    public SimpleOption<Integer> getViewDistance() { return new SimpleOption<Integer>(12); }
    public SimpleOption<Integer> getSimulationDistance() { return new SimpleOption<Integer>(12); }
    public SimpleOption<Integer> getMipmapLevels() { return new SimpleOption<Integer>(4); }
    public SimpleOption<Double> getEntityDistanceScaling() { return new SimpleOption<Double>(1.0D); }
    public SimpleOption<Boolean> getVsync() { return new SimpleOption<Boolean>(false); }
    public SimpleOption<Integer> getBiomeBlendRadius() { return new SimpleOption<Integer>(2); }
    public SimpleOption<Boolean> getAo() { return new SimpleOption<Boolean>(true); }
    public SimpleOption<String> getCloudRenderMode() { return new SimpleOption<String>("OFF"); }
}
