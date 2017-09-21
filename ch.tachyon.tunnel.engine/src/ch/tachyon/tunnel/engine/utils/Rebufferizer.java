package ch.tachyon.tunnel.engine.utils;


/**
 * Bufferize audio chunks of arbitrary sizes (but no longer than a maximum) into
 * chunks of a fixed size.
 * <p>
 * Typical usage (in the body of a loop):
 * <pre>
 * rebufferizer.submitVariable(samples); // &lt;- can be any size, but no more than specified maximum
 * float[] fixed = rebufferizer.retrieveFixed(); // &lt;- is of specified fixed size or null
 * while (fixed != null) {
 *     [... handle buffer fixed[] of fixed size ...]
 *     rebufferizer.submitFixed(fixed);
 *     fixed = rebufferizer.retrieveFixed(); // &lt;- is of specified fixed size or null
 * }
 * rebufferizer.retrieveVariable(samples); // &lt; same size as the originally submitted array, but with added {@link #getLatency() latency}
 * </pre>
 */
public class Rebufferizer {

    private final FloatQueue inBuffer;
    private final FloatQueue outBuffer;
    private final float[] output;
    private final int targetChunkSize;


    public Rebufferizer(int maxInputSize, int targetOutputSize) {
        targetChunkSize = targetOutputSize;
        inBuffer = new FloatQueue(maxInputSize + targetOutputSize);
        output = new float[targetOutputSize];
        outBuffer = new FloatQueue(targetOutputSize, maxInputSize + targetOutputSize);
    }

    public void submitVariable(float[] input) {
        inBuffer.pushArray(input);
    }

    public float[] retrieveFixed() {
        if (inBuffer.getSize() >= targetChunkSize) {
            inBuffer.popArray(output);
            return output;
        }
        return null;
    }

    public void submitFixed(float[] input) {
        outBuffer.pushArray(input);
    }

    public void retrieveVariable(float[] target) {
        outBuffer.popArray(target);
    }

    public int getLatency() {
        return targetChunkSize;
    }

    public int getTargetSize() {
        return targetChunkSize;
    }

    @Override
    public String toString() {
        return "Rebufferizer [0.." + (inBuffer.getCapacity() - targetChunkSize) + " -> " + targetChunkSize + "]";
    }

}
