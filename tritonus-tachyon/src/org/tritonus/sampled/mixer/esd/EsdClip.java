/*
 *	EsdClip.java
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

package org.tritonus.sampled.mixer.esd;

import java.io.IOException;
import java.io.ByteArrayInputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.LineUnavailableException;

import org.tritonus.share.TDebug;
import org.tritonus.lowlevel.esd.EsdSample;
import org.tritonus.lowlevel.esd.Esd;
import org.tritonus.share.sampled.mixer.TDataLine;
import org.tritonus.share.sampled.mixer.TMixer;


public class EsdClip
        extends TDataLine
        implements Clip {
    private static final int BUFFER_FRAMES = 16384;


    private EsdSample m_esdSample;


    public EsdClip(TMixer mixer) {
        super(mixer, null);
        m_esdSample = new EsdSample();
    }


    // interface Clip
    @Override
    public void open(AudioFormat audioFormat, byte[] abData, int nOffset, int nNumFrames)
            throws LineUnavailableException {
        int nBufferLength = nNumFrames * audioFormat.getFrameSize();
        // todo: check if nOffset + nBufferLength <= abData.length
        // perhaps truncate automatically
        ByteArrayInputStream bais = new ByteArrayInputStream(abData, nOffset, nBufferLength);
        try {
            AudioInputStream audioInputStream = new AudioInputStream(bais, audioFormat, nNumFrames);
            open(audioInputStream);
        } catch (IOException e) {
            if (TDebug.TraceAllExceptions) {
                TDebug.out(e);
            }
            throw new LineUnavailableException();
        }
    }


    @Override
    public void open(AudioInputStream audioInputStream)
            throws LineUnavailableException, IOException {
        AudioFormat audioFormat = audioInputStream.getFormat();
        // TOOD:
        DataLine.Info info = new DataLine.Info(Clip.class,
                audioFormat, -1/*nBufferSize*/);
        setLineInfo(info);
        int nFrameSize = audioFormat.getFrameSize();
        long lTotalLength = audioInputStream.getFrameLength() * nFrameSize;
        int nFormat = Esd.ESD_STREAM | Esd.ESD_PLAY | EsdUtils.getEsdFormat(audioFormat);
        if (TDebug.TraceClip) {
            TDebug.out("format: " + nFormat);
            TDebug.out("sample rate: " + audioFormat.getSampleRate());
        }
        m_esdSample.open(nFormat, (int) audioFormat.getSampleRate(), (int) lTotalLength);
        if (TDebug.TraceClip) {
            TDebug.out("size in esd: " + audioInputStream.getFrameLength() * nFrameSize);
        }
        int nBufferLength = BUFFER_FRAMES * nFrameSize;
        byte[] abData = new byte[nBufferLength];
        int nBytesRead = 0;
        int nTotalBytes = 0;
        while (nBytesRead != -1) {
            try {
                nBytesRead = audioInputStream.read(abData, 0, abData.length);
            } catch (IOException e) {
                if (TDebug.TraceClip || TDebug.TraceAllExceptions) {
                    TDebug.out(e);
                }
            }
            if (nBytesRead >= 0) {
                nTotalBytes += nBytesRead;
                if (TDebug.TraceClip) {
                    TDebug.out("EsdClip.open(): total bytes: " + nTotalBytes);
                    TDebug.out("EsdClip.open(): Trying to write: " + nBytesRead);
                }
                int nBytesWritten = m_esdSample.write(abData, 0, nBytesRead);
                if (TDebug.TraceClip) {
                    TDebug.out("EsdClip.open(): Written: " + nBytesWritten);
                }
            }
        }
        // to trigger the events
        open();
    }


    @Override
    public int getFrameLength() {
        // todo:
        return -1;
    }


    @Override
    public long getMicrosecondLength() {
        // todo:
        return -1;
    }


    @Override
    public void setFramePosition(int nPosition) {
        // TOOD:
    }


    @Override
    public void setMicrosecondPosition(long lPosition) {
        // TOOD:
    }


    @Override
    public int getFramePosition() {
        // TOOD:
        return -1;
    }


    @Override
    public long getMicrosecondPosition() {
        // TOOD:
        return -1;
    }


    @Override
    public void setLoopPoints(int nStart, int nEnd) {
        // TOOD:
    }


    @SuppressWarnings("unused")
    @Override
    public void loop(int nCount) {
        if (TDebug.TraceClip) {
            TDebug.out("EsdClip.loop(int): called; count = " + nCount);
        }
        if (false/*isStarted()*/) {
            /*
             *	only allow zero count to stop the looping
             *	at the end of an iteration.
             */
            if (nCount == 0) {
                if (TDebug.TraceClip) {
                    TDebug.out("EsdClip.loop(int): stopping sample");
                }
                m_esdSample.stop();
            }
        } else {
            if (nCount == 0) {
                if (TDebug.TraceClip) {
                    TDebug.out("EsdClip.loop(int): starting sample (once)");
                }
                m_esdSample.play();
            } else {
                /*
                 *	we're ignoring the count, because esd
                 *	cannot loop for a fixed number of
                 *	times.
                 */
                // TDebug.out("hallo");
                if (TDebug.TraceClip) {
                    TDebug.out("EsdClip.loop(int): starting sample (forever)");
                }
                m_esdSample.loop();
            }
        }
        // TOOD:
    }


    @Override
    public void flush() {
        // TOOD:
    }


    @Override
    public void drain() {
        // TOOD:
    }


    @Override
    public void close() {
        m_esdSample.free();
        m_esdSample.close();
        // TOOD:
    }


    @Override
    public void open() {
        // todo:
    }


    @Override
    public void start() {
        if (TDebug.TraceClip) {
            TDebug.out("EsdClip.start(): called");
        }
        /*
         *	This is a hack. What start() really should do is
         *	start playing at the position playback was stopped.
         */
        if (TDebug.TraceClip) {
            TDebug.out("EsdClip.start(): calling 'loop(0)' [hack]");
        }
        loop(0);
    }


    @Override
    public void stop() {
        // todo:
        m_esdSample.kill();
    }


    /*
     *	This method is enforced by DataLine, but doesn't make any
     *	sense for Clips.
     */
    @Override
    public int available() {
        return -1;
    }


}


/*** EsdClip.java ***/

