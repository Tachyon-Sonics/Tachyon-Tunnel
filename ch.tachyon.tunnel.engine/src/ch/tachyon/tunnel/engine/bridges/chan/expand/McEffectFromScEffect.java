package ch.tachyon.tunnel.engine.bridges.chan.expand;

import ch.tachyon.tunnel.engine.utils.concurrent.*;
import ch.tachyon.tunnel.host.*;
import ch.tachyon.tunnel.host.effect.*;


public class McEffectFromScEffect extends McFromScBase<ISimpleEffect, ISingleChanEffect> implements ISimpleEffect {

    private ProcessOnChanTask[] tasks;

    
    @Override
    public Class<? extends IEffect> getInterface() {
        return ISimpleEffect.class;
    }

    public int getLatency() {
        return descTarget.getLatency();
    }

    @Override
    public void startProcessing(IProcessingInfo info) {
        super.startProcessing(info);
        tasks = new ProcessOnChanTask[nbChans];
        for (int chan = 0; chan < nbChans; chan++)
            tasks[chan] = new ProcessOnChanTask(chan);
    }
    
    public void process(float[][] samples) {
        if (samples.length != nbChans)
            throw new IllegalArgumentException("Wrong number of channels. Found samples.length = " + samples.length + ", excpected: " + nbChans);
        assert targets.size() == nbChans;
        super.preProcessMidSide(samples);
        int nbFrames = 0;
        for (int chan = 0; chan < nbChans; chan++) {
            if (chan == 0)
                nbFrames = samples[chan].length;
            else if (nbFrames != samples[chan].length)
                throw new IllegalArgumentException("Number of samples is not the same in all channels, Channel 0 has " + nbFrames + " samples and channel " + chan + " has " + samples[chan].length);
            tasks[chan].setup(samples[chan]);
        }
        if (pool != null)
            pool.executeAndWait(tasks);
        else {
            for (Task task : tasks)
                task.run();
        }
        super.postProcessMidSide(samples);
    }
    
    class ProcessOnChanTask extends Task {
        
        private final int chan;
        private float[] input;

        public ProcessOnChanTask(int chan) {
            this.chan = chan;
        }
        
        void setup(float[] input) {
            this.input = input;
        }

        @Override
        public void run() {
            targets.get(chan).process(input);
        }
        
    }
    
    @Override
    public void stopProcessing() {
        super.stopProcessing();
        tasks = null;
    }

}
