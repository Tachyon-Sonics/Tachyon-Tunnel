package ch.tachyon.tunnel.plugin.presets;

import java.lang.annotation.*;

import ch.tachyon.tunnel.plugin.opt.doc.*;

/**
 * Used to define a preset for a plugin. A preset is given by a specific value for one or more (usually all) parameters.
 * <p>
 * This annotation can only be used inside of the {@link Presets} annotation, which defines all the presets for a plugin
 * <p>
 * When applicable it is recommanded to provide the following trivial presets at the end of the list:
 * <ul>
 * <li><b>No Change</b> if there is a combination of parameter values that do not change the audio 
 * <li><b>Silence</b> if there is a combination of parameter values that results in silence
 * </ul>
 * For these presets only, {@link #type()} should be set to {@link PresetType#NO_CHANGE} or {@link PresetType#SILENCE}
 * respectively.
 * @see Presets
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface Preset {
    
    /**
     * The name of the preset
     */
    public String name();
    
    /**
     * An optional description of the preset. Use the rules defined in {@link Description}
     */
    public String description() default "";
    
    /**
     * The details of the preset. This in an array of {@link PresetParameterValue}. Each element is a pair
     * of (parameter name / value) and specifies the value for one parameter for the given preset.
     * <p>
     * It is not allowed to specify the same parameter twice. It is allowed to leave a parameter unspecified, in
     * that case its value will not change when the user chooses the preset. It is a common practice for instance
     * not to specify the "quality" (or equivalent) parameter for example.
     */
    public PresetParameterValue[] value();
    
    /**
     * The type of preset. This is usually left to the default of {@link PresetType#PRESET}, except if this preset correspond
     * to a trivial one.
     */
    public PresetType type() default PresetType.PRESET;
}
