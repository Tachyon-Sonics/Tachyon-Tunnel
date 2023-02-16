/*
 *	AlsaSynthesizer.java
 *
 *	This file is part of Tritonus: http://www.tritonus.org/
 */

/*
 *  Copyright (c) 1999 by Matthias Pfisterer
 *
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Library General Public License as published
 *   by the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

/*
|<---            this code is formatted to fit into 80 columns             --->|
*/

package org.tritonus.midi.device.alsa;

import java.util.ArrayList;
import java.util.List;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Instrument;
import javax.sound.midi.Soundbank;
import javax.sound.midi.VoiceStatus;
import javax.sound.midi.Patch;

import org.tritonus.share.GlobalInfo;
import org.tritonus.share.TDebug;
import org.tritonus.share.midi.TMidiDevice;


public class AlsaSynthesizer
        extends AlsaMidiDevice
        implements Synthesizer {
    private static final MidiChannel[] EMPTY_MIDICHANNEL_ARRAY = new MidiChannel[0];
    private static final VoiceStatus[] EMPTY_VOICESTATUS_ARRAY = new VoiceStatus[0];

    private List<MidiChannel> m_channels;
    private int m_nVoices;


    public AlsaSynthesizer(int nClient, int nPort, int nVoices) {
        super(
                new TMidiDevice.Info(
                        "ALSA Synthesizer (" + nClient + ":" + nPort + ")",
                        GlobalInfo.getVendor(),
                        "Synthesizer based on the ALSA sequencer",
                        GlobalInfo.getVersion()),
                nClient, nPort, false, true);
        m_nVoices = nVoices;
        m_channels = new ArrayList<MidiChannel>();
    }


    @Override
    protected void openImpl() {
        super.openImpl();
        // TDebug.out("AlsaSynthesizer.openImpl(): called");
        // necessary? thread-safe?
        m_channels.clear();
        Receiver receiver = null;
        try {
            receiver = this.getReceiver();
        } catch (MidiUnavailableException e) {
            if (TDebug.TraceAllExceptions) {
                TDebug.out(e);
            }
        }
        for (int i = 0; i < 16; i++) {
            MidiChannel channel = new AlsaMidiChannel(
                    receiver, i);
            m_channels.add(channel);
        }
    }


    @Override
    protected void closeImpl() {
        super.closeImpl();
    }


    @Override
    public int getMaxPolyphony() {
        return m_nVoices;
    }


    @Override
    public long getLatency() {
        return -1L;
    }


    @Override
    public MidiChannel[] getChannels() {
        return m_channels.toArray(EMPTY_MIDICHANNEL_ARRAY);
    }


    @Override
    public VoiceStatus[] getVoiceStatus() {
        return EMPTY_VOICESTATUS_ARRAY;
    }


    @Override
    public boolean isSoundbankSupported(Soundbank soundbank) {
        return false;
    }


    @Override
    public boolean loadInstrument(Instrument instrument) {
        return false;
    }


    @Override
    public void unloadInstrument(Instrument instrument) {
    }


    @Override
    public boolean remapInstrument(Instrument from, Instrument to) {
        return false;
    }


    @Override
    public Soundbank getDefaultSoundbank() {
        return null;
    }


    @Override
    public Instrument[] getAvailableInstruments() {
        return null;
    }


    @Override
    public Instrument[] getLoadedInstruments() {
        return null;
    }


    @Override
    public boolean loadAllInstruments(Soundbank soundbank) {
        return false;
    }


    @Override
    public void unloadAllInstruments(Soundbank soundbank) {
    }


    @Override
    public boolean loadInstruments(Soundbank soundbank, Patch[] aPatches) {
        return false;
    }


    @Override
    public void unloadInstruments(Soundbank soundbank, Patch[] aPatches) {
    }


}


/*** AlsaSynthesizer.java ***/

