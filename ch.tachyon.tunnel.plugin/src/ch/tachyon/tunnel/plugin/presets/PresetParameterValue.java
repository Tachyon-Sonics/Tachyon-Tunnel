package ch.tachyon.tunnel.plugin.presets;

import java.lang.annotation.*;

import ch.tachyon.tunnel.plugin.param.*;

/**
 * This annotation specifies the value of a parameter, within a {@link Preset}.
 * @see Preset
 * @see Presets
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface PresetParameterValue {
    
    /**
     * The name of the Java property (in camelCase) corresponding to the parameter
     */
    public String name();
    
    /**
     * The value for that parameter.
     * <p>
     * Type conversion is done like for {@link Range}
     */
    public double value();

}
