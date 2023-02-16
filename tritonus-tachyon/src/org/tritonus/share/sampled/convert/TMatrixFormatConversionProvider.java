package org.tritonus.share.sampled.convert;

import java.util.Collection;

import javax.sound.sampled.AudioFormat;

/**
 * Added for compatibility with SPI based on tritonus 0.3.6
 */
public abstract class TMatrixFormatConversionProvider extends TEncodingFormatConversionProvider {

    protected TMatrixFormatConversionProvider(Collection<AudioFormat> sourceFormats,
            Collection<AudioFormat> targetFormats) {
        super(sourceFormats, targetFormats);
    }

    /**
     * Added for compatibility. The 'matrix' parameter is ignored
     */
    protected TMatrixFormatConversionProvider(Collection<AudioFormat> sourceFormats,
            Collection<AudioFormat> targetFormats, boolean[][] matrix) {
        super(sourceFormats, targetFormats);
    }


}
