package ch.tachyon.tunnel.engine.utils;


public class McRebufferizer {

    private final FloatQueue[] inBuffers;
    private final FloatQueue[] outBuffers;
    private final float[][] output;
    private final int targetChunkSize;


    public McRebufferizer(int nbChans, int maxInputSize, int targetOutputSize) {
        targetChunkSize = targetOutputSize;
        inBuffers = new FloatQueue[nbChans];
        for (int i = 0; i < nbChans; i++)
            inBuffers[i] = new FloatQueue(maxInputSize + targetOutputSize);
        output = new float[nbChans][targetOutputSize];
        outBuffers = new FloatQueue[nbChans];
        for (int i = 0; i < nbChans; i++)
            outBuffers[i] = new FloatQueue(targetOutputSize, maxInputSize + targetOutputSize);
    }

    public void submitVariable(float[][] input) {
        for (int i = 0; i < input.length; i++)
            inBuffers[i].pushArray(input[i]);
    }

    public float[][] retrieveFixed() {
        if (inBuffers[0].getSize() >= targetChunkSize) {
            for (int i = 0; i < inBuffers.length; i++)
                inBuffers[i].popArray(output[i]);
            return output;
        }
        return null;
    }

    public void submitFixed(float[][] input) {
        for (int i = 0; i < input.length; i++)
            outBuffers[i].pushArray(input[i]);
    }

    public void retrieveVariable(float[][] target) {
        for (int i = 0; i < target.length; i++)
            outBuffers[i].popArray(target[i]);
    }

    public int getLatency() {
        return targetChunkSize;
    }

    public int getTargetSize() {
        return targetChunkSize;
    }

}
