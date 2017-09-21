package ch.tachyon.tunnel.plugin.presets;

import java.lang.annotation.*;

/**
 * Placed on a plugin class, it defines the presets for that plugin.
 * <p>
 * Each preset is a list of parameter's values.
 * <p>
 * The value of this annotation is an array of {@link Preset}. Each element of the array correspond to one
 * named preset. The {@link Preset} annotation itself must provide the name of the preset as a string, and an
 * array of {@link PresetParameterValue} to specify the value of each parameter.
 * <p>
 * Example:
 * <pre>
 * {@literal @}Presets(value = {
 *     {@literal @}Preset(name = "<i>Name of preset 1</i>", value = {
 *         {@literal @}PresetParameterValue(name = "<i>name of parameter 1</i>", value = <i>value</i>),
 *         {@literal @}PresetParameterValue(name = "<i>name of parameter 2</i>", value = <i>value</i>)
 *     }),
 *     {@literal @}Preset(name = "<i>Name of preset 2</i>", value = {
 *         {@literal @}PresetParameterValue(name = "<i>name of parameter 1</i>", value = <i>value</i>),
 *         {@literal @}PresetParameterValue(name = "<i>name of parameter 2</i>", value = <i>value</i>)
 *     })
 * })
 *</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Presets {
    
    public Preset[] value();

}
