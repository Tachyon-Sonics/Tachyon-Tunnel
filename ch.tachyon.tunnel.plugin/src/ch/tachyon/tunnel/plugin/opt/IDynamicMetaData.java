package ch.tachyon.tunnel.plugin.opt;

import java.lang.annotation.*;
import java.util.*;

import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.param.*;

/**
 * This interface can be implemented whenever, for any reason, you cannot use
 * static Java annotations to parametrize your plugin and its parameters, or static Java
 * properties (getter / setter) to specify the parameters.
 * <p>
 * This class can be used if the plugin and its parameters are created dynamically, for instance
 * if you are dynamically wrapping a plugin of another plugin interface.
 * <p>
 * If any of the getters returns <code>null</code>, the standard static analysis is used. Hence if
 * for instance the plugin only has dynamic parameters, only {@link #getParameters()} needs to
 * return a non-<code>null</code> value.
 */
public interface IDynamicMetaData<P extends IPlugin> {

    /**
     * Get the parameters for this plugin.
     */
    public List<ParameterInfo<P>> getParameters();

    /**
     * Get the annotations for this plugin
     */
    public Collection<Annotation> getAnnotations();

    /**
     * Get the interfaces implemented by this plugin. Each object of the returned collection
     * must implement one or more interface of the <code>ch.tachyon.tunnel.plugin</code> package
     */
    public Collection<Object> getInterfaces();

}
