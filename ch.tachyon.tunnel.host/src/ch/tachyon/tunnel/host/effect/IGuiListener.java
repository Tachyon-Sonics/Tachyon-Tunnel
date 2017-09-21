package ch.tachyon.tunnel.host.effect;

public interface IGuiListener {

    public void cancel();

    public void process();

    public void setPreviewing(boolean value);

    public void setByPass(boolean value);

}
