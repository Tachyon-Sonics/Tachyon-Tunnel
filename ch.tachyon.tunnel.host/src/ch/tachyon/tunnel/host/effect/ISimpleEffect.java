package ch.tachyon.tunnel.host.effect;


public interface ISimpleEffect extends IEffect {

    public int getLatency();

    public void process(float[][] samples);

}
