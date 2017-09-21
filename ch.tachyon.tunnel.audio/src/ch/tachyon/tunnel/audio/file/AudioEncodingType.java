package ch.tachyon.tunnel.audio.file;


/**
 * Audio encoding/decoding types for a single channel. Includes bits per sample, endianess and signed/unsigned.
 * <p>
 * Every encoding type allows you to convert to and from PCM <tt>float[]</tt> between <tt>-1</tt> and <tt>1</tt>.
 * <p>
 * Only 8/16/24/32bits are supported for integers. Unsigned is only supported for 8bits. 32/64bits IEEE floats
 * are supported as well. Note that 64bit float (corresponds to Java <code>double</code>) are converted to and from
 * Java <code>float</code>.
 */
public enum AudioEncodingType {
    ENC_8BIT_S(1) {
        @Override
        public void decode(byte[] source, float[] target, int length) {
            int srcIndex = 0;
            for (int i = 0; i < length; i++)
                target[i] = (int) source[srcIndex++] / 128.0F;
        }

        @Override
        public void encode(float[] source, byte[] target, int length) {
            mulAndCrop(source, 128, -128, 127, length);
            int dstIndex = 0;
            for (int i = 0; i < length; i++)
                target[dstIndex++] = (byte) source[i];
        }
    },
    ENC_8BIT_U(1) {
        @Override
        public void decode(byte[] source, float[] target, int length) {
            int srcIndex = 0;
            for (int i = 0; i < length; i++)
                target[i] = ((byte) (source[srcIndex++] + 128)) / 128.0F;
        }

        @Override
        public void encode(float[] source, byte[] target, int length) {
            mulAndCrop(source, 128, -128, 127, length);
            int dstIndex = 0;
            for (int i = 0; i < length; i++)
                target[dstIndex++] = (byte) ((byte) source[i] + 128);
        }
    },
    ENC_16BIT_LE(2) {
        @Override
        public void decode(byte[] source, float[] target, int length) {
            int srcIndex = 0;
            for (int i = 0; i < length; i++)
                target[i] = ((source[srcIndex++] & 0xFF) | (source[srcIndex++] << 8)) / 32768.0F;
        }

        @Override
        public void encode(float[] source, byte[] target, int length) {
            mulAndCrop(source, 32768, -32768, 32767, length);
            int dstIndex = 0;
            for (int i = 0; i < length; i++) {
                target[dstIndex++] = (byte) ((int) source[i] & 0xFF);
                target[dstIndex++] = (byte) (((int) source[i] >> 8) & 0xFF);
            }
        }
    },
    ENC_16BIT_BE(2) {
        @Override
        public void decode(byte[] source, float[] target, int length) {
            int srcIndex = 0;
            for (int i = 0; i < length; i++)
                target[i] = ((source[srcIndex++] << 8) | (source[srcIndex++] & 0xFF)) / 32768.0F;
        }

        @Override
        public void encode(float[] source, byte[] target, int length) {
            mulAndCrop(source, 32768, -32768, 32767, length);
            int dstIndex = 0;
            for (int i = 0; i < length; i++) {
                target[dstIndex++] = (byte) (((int) source[i] >> 8) & 0xFF);
                target[dstIndex++] = (byte) ((int) source[i] & 0xFF);
            }
        }
    },
    ENC_24BIT_LE(3) {
        @Override
        public void decode(byte[] source, float[] target, int length) {
            int srcIndex = 0;
            for (int i = 0; i < length; i++)
                target[i] = ((source[srcIndex++] & 0xFF)
                          | ((source[srcIndex++] & 0xFF) << 8)
                          |  (source[srcIndex++] << 16))
                          / 8388608.0F;
        }

        @Override
        public void encode(float[] source, byte[] target, int length) {
            mulAndCrop(source, 8388608, -8388608, 8388607, length);
            int dstIndex = 0;
            for (int i = 0; i < length; i++) {
                target[dstIndex++] = (byte) ((int) source[i] & 0xFF);
                target[dstIndex++] = (byte) (((int) source[i] >> 8) & 0xFF);
                target[dstIndex++] = (byte) (((int) source[i] >> 16) & 0xFF);
            }
        }
    },
    ENC_24BIT_BE(3) {
        @Override
        public void decode(byte[] source, float[] target, int length) {
            int srcIndex = 0;
            for (int i = 0; i < length; i++)
                target[i] = ((source[srcIndex++] << 16)
                          | ((source[srcIndex++] & 0xFF) << 8)
                          |  (source[srcIndex++] & 0xFF))
                          / 8388608.0F;
        }

        @Override
        public void encode(float[] source, byte[] target, int length) {
            mulAndCrop(source, 8388608, -8388608, 8388607, length);
            int dstIndex = 0;
            for (int i = 0; i < length; i++) {
                target[dstIndex++] = (byte) (((int) source[i] >> 16) & 0xFF);
                target[dstIndex++] = (byte) (((int) source[i] >> 8) & 0xFF);
                target[dstIndex++] = (byte) ((int) source[i] & 0xFF);
            }
        }
    },
    ENC_32BIT_LE(4) {
        @Override
        public void decode(byte[] source, float[] target, int length) {
            int srcIndex = 0;
            for (int i = 0; i < length; i++)
                target[i] = ((source[srcIndex++] & 0xFF)
                         | ((source[srcIndex++] & 0xFF) << 8)
                         | ((source[srcIndex++] & 0xFF) << 16)
                         |  (source[srcIndex++] << 24) )
                         / 2147483648.0F;
        }

        @Override
        public void encode(float[] source, byte[] target, int length) {
            mulAndCrop(source, 2147483648.0f, -2147483648.0f, 2147483647.0f, length);
            int dstIndex = 0;
            for (int i = 0; i < length; i++) {
                target[dstIndex++] = (byte) ((int) source[i] & 0xFF);
                target[dstIndex++] = (byte) (((int) source[i] >> 8) & 0xFF);
                target[dstIndex++] = (byte) (((int) source[i] >> 16) & 0xFF);
                target[dstIndex++] = (byte) (((int) source[i] >> 24) & 0xFF);
            }
        }
    },
    ENC_32BIT_BE(4) {
        @Override
        public void decode(byte[] source, float[] target, int length) {
            int srcIndex = 0;
            for (int i = 0; i < length; i++)
                target[i] = ((source[srcIndex++] << 24)
                        | ((source[srcIndex++] & 0xFF) << 16)
                        | ((source[srcIndex++] & 0xFF) << 8)
                        |  (source[srcIndex++] & 0xFF))
                        / 2147483648.0F;
        }

        @Override
        public void encode(float[] source, byte[] target, int length) {
            mulAndCrop(source, 2147483648.0f, -2147483648.0f, 2147483647.0f, length);
            int dstIndex = 0;
            for (int i = 0; i < length; i++) {
                target[dstIndex++] = (byte) (((int) source[i] >> 24) & 0xFF);
                target[dstIndex++] = (byte) (((int) source[i] >> 16) & 0xFF);
                target[dstIndex++] = (byte) (((int) source[i] >> 8) & 0xFF);
                target[dstIndex++] = (byte) ((int) source[i] & 0xFF);
            }
        }
    },
    ENC_FLOAT_LE(4) {

        @Override
        public void decode(byte[] source, float[] target, int length) {
            int srcIndex = 0;
            for (int i = 0; i < length; i++) {
                int value = ((source[srcIndex++] & 0xFF)
                        | ((source[srcIndex++] & 0xFF) << 8)
                        | ((source[srcIndex++] & 0xFF) << 16)
                        |  (source[srcIndex++] << 24) ); // Decode little-endian value
                target[i] = Float.intBitsToFloat(value);
            }

        }

        @Override
        public void encode(float[] source, byte[] target, int length) {
            int dstIndex = 0;
            for (int i = 0; i < length; i++) {
                int value = Float.floatToIntBits(source[i]);
                // Encode as little endian
                target[dstIndex++] = (byte) (value & 0xFF);
                target[dstIndex++] = (byte) ((value >> 8) & 0xFF);
                target[dstIndex++] = (byte) ((value >> 16) & 0xFF);
                target[dstIndex++] = (byte) ((value >> 24) & 0xFF);
            }

        }
    },
    ENC_FLOAT_BE(4) {

        @Override
        public void decode(byte[] source, float[] target, int length) {
            int srcIndex = 0;
            for (int i = 0; i < length; i++) {
                int value = ((source[srcIndex++] << 24)
                        | ((source[srcIndex++] & 0xFF) << 16)
                        | ((source[srcIndex++] & 0xFF) << 8)
                        |  (source[srcIndex++] & 0xFF)); // Decode big-endian value
                target[i] = Float.intBitsToFloat(value);
            }

        }

        @Override
        public void encode(float[] source, byte[] target, int length) {
            int dstIndex = 0;
            for (int i = 0; i < length; i++) {
                int value = Float.floatToIntBits(source[i]);
                // Encode as big endian
                target[dstIndex++] = (byte) ((value >> 24) & 0xFF);
                target[dstIndex++] = (byte) ((value >> 16) & 0xFF);
                target[dstIndex++] = (byte) ((value >> 8) & 0xFF);
                target[dstIndex++] = (byte) (value & 0xFF);
            }

        }
    },
    ENC_DOUBLE_LE(8) {

        @Override
        public void decode(byte[] source, float[] target, int length) {
            int srcIndex = 0;
            for (int i = 0; i < length; i++) {
                long value = ((source[srcIndex++] & 0xFFL)
                        | ((source[srcIndex++] & 0xFFL) << 8)
                        | ((source[srcIndex++] & 0xFFL) << 16)
                        | ((source[srcIndex++] & 0xFFL) << 24)
                        | ((source[srcIndex++] & 0xFFL) << 32)
                        | ((source[srcIndex++] & 0xFFL) << 40)
                        | ((source[srcIndex++] & 0xFFL) << 48)
                        | (((long) source[srcIndex++]) << 56)); // Decode little-endian value
                target[i] = (float) Double.longBitsToDouble(value);
            }

        }

        @Override
        public void encode(float[] source, byte[] target, int length) {
            int dstIndex = 0;
            for (int i = 0; i < length; i++) {
                long value = Double.doubleToLongBits((double) source[i]);
                // Encode as little endian
                target[dstIndex++] = (byte) (value & 0xFFL);
                target[dstIndex++] = (byte) ((value >> 8) & 0xFFL);
                target[dstIndex++] = (byte) ((value >> 16) & 0xFFL);
                target[dstIndex++] = (byte) ((value >> 24) & 0xFFL);
                target[dstIndex++] = (byte) ((value >> 32) & 0xFFL);
                target[dstIndex++] = (byte) ((value >> 40) & 0xFFL);
                target[dstIndex++] = (byte) ((value >> 48) & 0xFFL);
                target[dstIndex++] = (byte) ((value >> 56) & 0xFFL);
            }

        }
    },
    ENC_DOUBLE_BE(8) {

        @Override
        public void decode(byte[] source, float[] target, int length) {
            int srcIndex = 0;
            for (int i = 0; i < length; i++) {
                long value = (((long) source[srcIndex++] << 56)
                        | ((source[srcIndex++] & 0xFFL) << 48)
                        | ((source[srcIndex++] & 0xFFL) << 40)
                        | ((source[srcIndex++] & 0xFFL) << 32)
                        | ((source[srcIndex++] & 0xFFL) << 24)
                        | ((source[srcIndex++] & 0xFFL) << 16)
                        | ((source[srcIndex++] & 0xFFL) << 8)
                        |  (source[srcIndex++] & 0xFFL)); // Decode big-endian value
                target[i] = (float) Double.longBitsToDouble(value);
            }

        }

        @Override
        public void encode(float[] source, byte[] target, int length) {
            int dstIndex = 0;
            for (int i = 0; i < length; i++) {
                long value = Double.doubleToLongBits((double) source[i]);
                // Encode as big endian
                target[dstIndex++] = (byte) ((value >> 56) & 0xFFL);
                target[dstIndex++] = (byte) ((value >> 48) & 0xFFL);
                target[dstIndex++] = (byte) ((value >> 40) & 0xFFL);
                target[dstIndex++] = (byte) ((value >> 32) & 0xFFL);
                target[dstIndex++] = (byte) ((value >> 24) & 0xFFL);
                target[dstIndex++] = (byte) ((value >> 16) & 0xFFL);
                target[dstIndex++] = (byte) ((value >> 8) & 0xFFL);
                target[dstIndex++] = (byte) (value & 0xFFL);
            }

        }
    };

    private final int bytesPerSample;

    private AudioEncodingType(int bytesPerSample) {
        this.bytesPerSample = bytesPerSample;
    }

    public int getBytesPerSample() {
        return bytesPerSample;
    }

    /**
     * @param length in target number of samples
     */
    public abstract void decode(byte[] source, float[] target, int length);

    /**
     * This method may modify the source array.
     * @param length in source number of samples
     */
    public abstract void encode(float[] source, byte[] target, int length);

    /**
     * Pre-multiply and crop the given samples
     * @param source the samples
     * @param mult the amount by which to pre-multiply the samples
     * @param min the minimum value to crop to
     * @param max the maximum value to crop to
     */
    private static void mulAndCrop(float[] source, float mult, float min, float max, int length) {
        for (int i = 0; i < length; i++) {
            float s = source[i] * mult;
            source[i] = (s < min ? min : (s > max ? max : s));
        }
    }
}