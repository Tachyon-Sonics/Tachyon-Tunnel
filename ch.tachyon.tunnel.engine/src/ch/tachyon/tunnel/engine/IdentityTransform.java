package ch.tachyon.tunnel.engine;

import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.param.*;

public class IdentityTransform implements IParameterTransform {

    private static IdentityTransform instance;


    private IdentityTransform() {

    }

    public static IdentityTransform getInstance() {
        if (instance == null)
            instance = new IdentityTransform();
        return instance;
    }

    public <E extends Number> E transform(E value, IProcessingInfo info) {
        return value;
    }

    public <E extends Number> E untransform(E value, IProcessingInfo info) {
        return value;
    }

}
