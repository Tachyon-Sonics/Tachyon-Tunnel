/*
 *	EsdTargetDataLine.java
 *
 *	This file is part of Tritonus: http://www.tritonus.org/
 */

/*
 *  Copyright (c) 1999, 2000 by Matthias Pfisterer
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

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

import org.tritonus.share.TDebug;
import org.tritonus.lowlevel.esd.Esd;
import org.tritonus.lowlevel.esd.EsdRecordingStream;
import org.tritonus.share.sampled.TConversionTool;
import org.tritonus.share.sampled.mixer.TMixer;
import org.tritonus.share.sampled.mixer.TBaseDataLine;


public class EsdTargetDataLine
        extends TBaseDataLine
        implements TargetDataLine {
    // private static final Class[] CONTROL_CLASSES = {GainControl.class};


    private EsdRecordingStream m_esdStream;
    private boolean m_bSwapBytes;


    public EsdTargetDataLine(TMixer mixer, AudioFormat format, int nBufferSize)
            throws LineUnavailableException {
        // todo: use an info object that represents the mixer's capabilities (all possible formats for the line)
        super(mixer,
                new DataLine.Info(TargetDataLine.class,
                        format,
                        nBufferSize)/*,
                                      // todo: has info object to change if format or buffer size are changed later?
                                      format, nBufferSize*/);
    }


    @Override
    protected void openImpl() {
        if (TDebug.TraceTargetDataLine) {
            TDebug.out("EsdTargetDataLine.openImpl(): called.");
        }
        /*
         *	Checks that a format is set.
         *	Sets the buffer size to a default value if not
         *	already set.
         */
        checkOpen();
        AudioFormat format = getFormat();
        AudioFormat.Encoding encoding = format.getEncoding();
        boolean bBigEndian = format.isBigEndian();
        m_bSwapBytes = false;
        if (format.getSampleSizeInBits() == 16 && bBigEndian) {
            m_bSwapBytes = true;
            bBigEndian = false;
        } else if (format.getSampleSizeInBits() == 8 &&
                encoding.equals(AudioFormat.Encoding.PCM_SIGNED)) {
            m_bSwapBytes = true;
            encoding = AudioFormat.Encoding.PCM_UNSIGNED;
        }
        if (m_bSwapBytes) {
            format = new AudioFormat(encoding,
                    format.getSampleRate(),
                    format.getSampleSizeInBits(),
                    format.getChannels(),
                    format.getFrameSize(),
                    format.getFrameRate(),
                    bBigEndian);
        }
        int nOutFormat = Esd.ESD_STREAM | Esd.ESD_PLAY | EsdUtils.getEsdFormat(format);
        m_esdStream = new EsdRecordingStream();
        m_esdStream.open(nOutFormat,
                (int) format.getSampleRate());
    }

    /*
    	public void start()
    	{
    		setStarted(true);
    		setActive(true);
    		if (TDebug.TraceSourceDataLine)
    		{
    			TDebug.out("EsdTargetDataLine.start(): channel started.");
    		}
    	}
    
    
    
    	public void stop()
    	{
    		setStarted(false);
    	}
    */


    @Override
    public int available() {
        // todo:
        return -1;
        // return m_nAvailable;
    }


    // todo: check if should block
    @Override
    public int read(byte[] abData, int nOffset, int nLength) {
        if (TDebug.TraceTargetDataLine) {
            TDebug.out("EsdTargetDataLine.read(): called.");
            TDebug.out("EsdTargetDataLine.read(): wanted length: " + nLength);
        }
        int nOriginalOffset = nOffset;
        if (nLength > 0 && !isActive()) {
            start();
        }
        if (!isOpen()) {
            if (TDebug.TraceTargetDataLine) {
                TDebug.out("EsdTargetDataLine.read(): stream closed");
            }
        }
        int nBytesRead = m_esdStream.read(abData, nOffset, nLength);
        if (TDebug.TraceTargetDataLine) {
            TDebug.out("EsdTargetDataLine.read(): read (bytes): " + nBytesRead);
        }
        if (m_bSwapBytes && nBytesRead > 0) {
            TConversionTool.swapOrder16(abData, nOriginalOffset, nBytesRead / 2);
        }
        return nBytesRead;
    }


    @Override
    public void closeImpl() {
        m_esdStream.close();
    }


    @Override
    public void drain() {
        // todo:
    }


    @Override
    public void flush() {
        // todo:
    }


    public long getPosition() {
        // todo:
        return 0;
    }


    /**
     *	fGain is logarithmic!!
     */
    protected void setGain(float fGain) {
    }


    public class EsdTargetDataLineGainControl
            extends FloatControl {
        /*
         *	These variables should be static. However, Java 1.1
         *	doesn't allow this. So they aren't.
         */
        /*static*/ final float MAX_GAIN = 90.0F;
        /*static*/ final float MIN_GAIN = -96.0F;

        // todo: recheck this value
        /*static*/ final int GAIN_INCREMENTS = 1000;

        // private float m_fGain;
        // private boolean m_bMuted;


        /*package*/ EsdTargetDataLineGainControl() {
            super(FloatControl.Type.VOLUME, // or MASTER_GAIN ?
                    -96.0F, // MIN_GAIN,
                    24.0F, // MAX_GAIN,
                    0.01F, // precision
                    0, // update period?
                    0.0F, // initial value
                    "dB",
                    "-96.0",
                    "",
                    "+24.0");
            // m_bMuted = false; // should be included in a compund control?
        }


        @Override
        public void setValue(float fGain) {
            fGain = Math.max(Math.min(fGain, getMaximum()), getMinimum());
            if (Math.abs(fGain - getValue()) > 1.0E9) {
                super.setValue(fGain);
                // if (!getMute())
                // {
                EsdTargetDataLine.this.setGain(getValue());
                // }
            }
        }


        /*
          public float getMaximum()
          {
          return MAX_GAIN;
          }
        
        
        
          public float getMinimum()
          {
          return MIN_GAIN;
          }
        
        
        
          public int getIncrements()
          {
          // todo: check this value
          return GAIN_INCREMENTS;
          }
        
        
        
          public void fade(float fInitialGain, float fFinalGain, int nFrames)
          {
          // todo:
          }
        
        
        
          public int getFadePrecision()
          {
          //todo:
          return -1;
          }
        
        
        
          public boolean getMute()
          {
          return m_bMuted;
          }
        
        
        
          public void setMute(boolean bMuted)
          {
          if (bMuted != getMute())
          {
          m_bMuted = bMuted;
          if (getMute())
          {
          EsdTargetDataLine.this.setGain(getMinimum());
          }
          else
          {
          EsdTargetDataLine.this.setGain(getGain());
          }
          }
          }
        */


    }
}


/*** EsdTargetDataLine.java ***/
