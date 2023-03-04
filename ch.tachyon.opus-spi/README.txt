This is a provider for OPUS audio files, written 100% in Java.

It adds OPUS audio file support to the JavaSound API.

You need to add the three following .jar files (available in the "lib" directory) to your project:
- opus-spi-1.0.0.jar
- vorbis-java-core-0.8b.jar (created from https://github.com/Gagravarr/VorbisJava/tree/master/core/src/main on 03.2023)
- concentus.jar (created from https://github.com/lostromb/concentus/tree/master/Java/Concentus/src/main/java on 03.2023)

Note that tritonus is not required, the spi is coded directly against the JavaSound API.


It is based on:
- https://github.com/Gagravarr/VorbisJava (container format)
- https://github.com/lostromb/concentus (packet decoding)


Implementation note:
A particularity of the OPUS format is that the length is not stored in any header.
Computing the length requires iterating over all packets, and hence reading the entire file.
At least this is how it is implemented here. Another method (not implemented) would involve locating the last packet.
This is quite fast in terms of CPU because the packets themselves do not need to be decoded to compute the length.
However, the entire file must be read, and this is not always desirable.

For that reason:
- The length is only computed and reported if AudioSystem.getAudioFileFormat() is used with a java.io.File argument.
- The length is NOT computed (and hence reported as AudioSystem.NOT_SPECIFIED) if used with a java.io.InputStream
  (including java.io.FileInputStream).

When the length is computed (as a number of audio sample frames), it is also available as a java.lang.Long using the
property key "frameLength" (Opus.FRAME_LENGTH_PROPERTY) in the AudioFileFormat object.
