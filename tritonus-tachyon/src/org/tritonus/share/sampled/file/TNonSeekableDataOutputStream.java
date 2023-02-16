/*
 *	TNonSeekableDataOutputStream.java
 *
 *	This file is part of Tritonus: http://www.tritonus.org/
 */

/*
 *  Copyright (c) 1999 by Florian Bomers <http://www.bomers.de>
 *  Copyright (c) 2000 by Matthias Pfisterer
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

package org.tritonus.share.sampled.file;

import java.io.IOException;
import java.io.OutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;


/**
 * A TDataOutputStream that does not allow seeking.
 *
 * @author Florian Bomers
 * @author Matthias Pfisterer
 */
public class TNonSeekableDataOutputStream
        extends DataOutputStream
        implements TDataOutputStream {
    
    private final OutputStream target;
    
    public TNonSeekableDataOutputStream(OutputStream outputStream) {
        super(outputStream);
        this.target = outputStream;
    }
    
    @Override
    public boolean supportsSeek() {
        return (target instanceof FileOutputStream);
    }


    @Override
    public void seek(long position)
            throws IOException {
        if (target instanceof FileOutputStream) {
            FileOutputStream fOutput = (FileOutputStream) target;
            fOutput.getChannel().position(position);
            return;
        }
        throw new IllegalArgumentException("TNonSeekableDataOutputStream: Call to seek not allowed.");
    }


    @Override
    public long getFilePointer()
            throws IOException {
        if (target instanceof FileOutputStream) {
            FileOutputStream fOutput = (FileOutputStream) target;
            return fOutput.getChannel().position();
        }
        throw new IllegalArgumentException("TNonSeekableDataOutputStream: Call to getFilePointer not allowed.");
    }


    @Override
    public long length()
            throws IOException {
        if (target instanceof FileOutputStream) {
            FileOutputStream fOutput = (FileOutputStream) target;
            return fOutput.getChannel().size();
        }
        throw new IllegalArgumentException("TNonSeekableDataOutputStream: Call to length not allowed.");
    }


    @Override
    public void writeLittleEndian64(long value) throws IOException {
        writeByte((int) (value & 0xFFL));
        writeByte((int) ((value >> 8) & 0xFFL));
        writeByte((int) ((value >> 16) & 0xFFL));
        writeByte((int) ((value >> 24) & 0xFFL));
        writeByte((int) ((value >> 32) & 0xFFL));
        writeByte((int) ((value >> 40) & 0xFFL));
        writeByte((int) ((value >> 48) & 0xFFL));
        writeByte((int) ((value >> 56) & 0xFFL));
    }

    @Override
    public void writeLittleEndian32(int value)
            throws IOException {
        writeByte(value & 0xFF);
        writeByte((value >> 8) & 0xFF);
        writeByte((value >> 16) & 0xFF);
        writeByte((value >> 24) & 0xFF);
    }

    @Override
    public void writeLittleEndian16(short value)
            throws IOException {
        writeByte(value & 0xFF);
        writeByte((value >> 8) & 0xFF);
    }
}


/*** TNonSeekableDataOutputStream.java ***/
