package ch.tachyon.tunnel.engine.bridges.chan;

import ch.tachyon.tunnel.host.*;
import ch.tachyon.tunnel.host.effect.*;


public interface IScEffectFactory<SC extends IEffect> {
    
    public SC create(SC mainEffect, IProcessingInfo info);

}
