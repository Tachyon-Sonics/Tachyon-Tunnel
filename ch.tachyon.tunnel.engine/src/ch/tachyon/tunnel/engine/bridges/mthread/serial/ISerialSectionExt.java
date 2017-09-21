package ch.tachyon.tunnel.engine.bridges.mthread.serial;

import ch.tachyon.tunnel.plugin.opt.thread.*;

/**
 * Extends {@link ISerialSection} with methods used by the engine only (not visible by plugins and hosts)
 */
public interface ISerialSectionExt extends ISerialSection {

    public void setClock(long clock);

    public void clearClock();

    public void setActive(boolean value);

}
