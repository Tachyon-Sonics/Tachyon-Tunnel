package ch.tachyon.tunnel.common;


public class AudioStreamFormat {

    private final float sampleRate;
    private final int nbChans;


    public AudioStreamFormat(float sampleRate, int nbChans) {
        this.sampleRate = sampleRate;
        this.nbChans = nbChans;
    }

    public float getSampleRate() {
        return sampleRate;
    }

    public int getNbChans() {
        return nbChans;
    }

}
