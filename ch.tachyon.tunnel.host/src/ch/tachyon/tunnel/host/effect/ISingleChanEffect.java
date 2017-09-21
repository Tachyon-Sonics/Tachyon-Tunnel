package ch.tachyon.tunnel.host.effect;


public interface ISingleChanEffect extends IEffect {

    public int getLatency();

    public void process(float[] samples);

}
