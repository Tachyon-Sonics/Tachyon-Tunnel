tritonus-tachyon
================

This is a fork of tritonus-all, version 0.3.7.2.
This fork has been created mainly for a commercial application, "Quantum Audio Time Stretch".
Most, if not all improvements in this fork concern the three PCM audio file formats: WAVE, AIFF and AU.

Eclipse is the recommended IDE for maintenance of this project.

tritonus-tachyon-full.jar contains almost everything, but you probably still need the native libraries (dll) to use some low-level stuff such as alsa or cdda. It has not really been tested. The only missing part is the jorbis support (see below).

tritonus-tachyon.jar only contains:
- what is needed by third-party audio file format providers (for mp3, ogg, flac, etc)
  - is has been tested and is compatible with the following library providers (and should work with any other provider that worked with tritonus-0.3.7.2):
    - jaad-0.8.4.jar (reading of Apple's *.m4a audio files)
    - jflac-codec-1.5.2.jar (reading of *.flac audio files)
    - mp3spi-1.9.5.4.jar (reading of *.mp3 audio files)
    - vorbisspi-1.0.3.3.jar (reading of *.ogg audio files)
- support for WAVE, AIFF and AU file formats; with the improvements that follows.

The .jar distribution files can be created from Eclipse using the .jardesc files.


Summary of changes compared to tritonus 0.3.7.2
===============================================

Global changes
--------------

- Minimum Java version is 8, although most of it should still compile with Java 6.
- Java 1.5 generics have been added whenever missing
- All warnings (according to Eclipse compiler's settings) fixed.
- Removed integrated jorbis support. Not only did that create a dependency on the jorbis library (the dependency is now gone), but ogg/vorbis audio file support can already be achieved using jorbis, vorbisspi and the basic tritonus-tachyon.jar.


Changes to all PCM audio file formats (WAVE, AIFF, AU)
------------------------------------------------------

- The new org.tritonus.share.sampled.file.RandomFileInputStream class can be used in place of FileInputStream, because it always supports mark() and reset(), even if the read limit is reached. This solves problems when loading .mp3 files with large embedded images (which would need to read past the mark limit with a FileInputStream). For this to work, it must NOT be wrapped in a BufferedInputStream.
- TAudioFileFormat has additional constructors with file size and frame length as "long" instead of "int", as well as additional getters returning these values as long. For large audio files with which any of these values is greater than 2^32, the "int" version returns -1 (AudioSystem.NOT_SPECIFIED), but the "long" version returns the actual value. The "long" version also returns the proper value if the "int" version is between 2^31 and 2^32 and becomes negative. If the length is really unknown, both the "int" and the "long" version return -1.
  - When using a method from javax.sound.sampled that returns an AudioFileFormat, you want to check if it is an instance of TAudioFileFormat, and benefit from the "long" values in that case.
  - When using a method from javax.sound.sampled that takes an AudioFileFormat as argument, you can use a TAudioFileFormat (it's a compatible subclass), and the "long" values will be properly used if the format is WAVE, AIFF or AU and is processed by tritonus-tachyon.
- Added support for floating-point encoding (both 32-bit, corresponding to Java's float, and 64-bit, corresponding to Java's double). The reported encoding value is org.tritonus.share.sampled.Encodings.PCM_IEEE_FLOAT. The value is equal (according to the equals() method) to javax.sound.sampled.AudioFormat.Encoding.PCM_FLOAT added in Java 7 (but only supported for the WAVE format by the JRE). For WAVE files, this corresponds to type 3 (float values between -1.0 and 1.0); other floating point types are NOT supported (such as float values between -32768.0 and 32767.0). With tritonus-tachyon, the floating point encoding is supported by all three PCM formats (WAVE, AIFF and AU) for both reading and writing.
- When writing to a FileOutputStream and with an initially unknown size, tritonus-tachyon can now also patch the header (like it does when writing to a File, thanks to FileOutputStream.getChannel()) and set it to the actual length. Unfortunately this only works if a bare FileOutputStream is passed. It does NOT work if the FileOutputStream is wrapped in a BufferedOutputStream. Passing a java.io.File is still the recommended way.


Changes to the AU format
------------------------

- It is now possible to read and write files of size bigger than 2^31. The value of -1 (unknown) is used, but the file is not cropped as it used to be. Not all software might be able to read them though, but most of them seem ok.
- When loading an AU file of unknown size (or of size greater than 2^31) with a FileInputStream or a RandomFileInputStream, the actual size is calculated from the file size and reported in the "long" values of a TAudioFileFormat (see above). This does not work with other InputStreams though. This also does NOT work with a FileInputStream or RandomFileInputStream wrapped in a BufferedInputStream.


Changes to the WAVE format
--------------------------

- WAVE files of more than 4GB are supported for both reading and writing, using the "RF64" extension (see https://tech.ebu.ch/docs/tech/tech3306v1_0.pdf)
  - When writing, the "RF64" extension is only used if the length is actually >= 4 GB.
  - This only works when writing to a java.io.File, to a FileOutputStream, or when the length is specified in advance using the TAudioFileFormat class. Writing to an OutputStream (that is not a FileOutputStream) and with an initially unknown length will not use the "RF64" extension.
  - A consequence is that when writing a WAVE file of initially unknown length (with File or FileOutputStream), a "JUNK" chunk is added, so that it can be patched into a "ds64" chunk in case then actual length happens to be over 4 GB. While any WAVE file reader should ignore unknown chunks (including the "JUNK" chunk that is in fact part of the specification), this might introduce incompatibilities, because the "JUNK" chunk remains if the file is less than 4 GB. If a known length is provided, and the length is less than 4GB, no "JUNK" chunk is written.
  - WAVE file in the "BW64" format should also read properly, but the extensions are ignored, and writing in this format is not possible.
- WAVE files with the WAVE_FORMAT_EXTENSIBLE format are now supported, as long as the subformat is one of the supported encoding for "regular" WAVE files: PCM_SIGNED, PCM_UNSIGNED (only 8-bit), PCM_IEEE_FLOAT, aLaw, uLaw or ADPCM.
  - two properties (as Integer) are transmitted: "channelMask" and "validBitsPerSample". Unlike the JRE version, no "channelOrder" property is created.
  - currently, tritonus-tachyon never writes WAVE files in the WAVE_FORMAT_EXTENSIBLE. Although Microsoft recommends using this format as soon as the number of channel is > 2 or the encoding is not PCM_SIGNED/PCM_UNSIGNED, my experience is that WAVE files without the WAVE_FORMAT_EXTENSIBLE are still better supported (and obviously already allow for > 2 channels or non-PCM encoding).
  
  
Changes to the AIFF format
--------------------------

- None specifically, except the support for floating-point encoding (see "Changes to all PCM formats" above)


Others
------

"Sort of" junit tests have been introduced, in the "ch.tachyon.tritonus.test" project.
- Only new stuff is tested (mostly the floating-point format support, and support for WAVE / AU files of more than 4 GB)
- Tests are based on ch.tachyon.tunnel.audio, that redirects to tritonus-tachyon (rather than testing tritonus-tachyon directly)
- Some tests are VERY slow (those testing audio files of more than 4 GB)
  - They use mocks so that they do not need to actually store 4 GB on the disk, but they still have to process 4 GB of audio, hence the slowness.
  - The RootSuite class contains all tests. The FastRootSuite class excludes the slow tests.
- Several tests also check that the produced audio files are error-free using the external "ffmpeg" command. ffmpeg must either be in the PATH, or its path must be specified using the -Dffmpeg.path=<path> JVM argument when running the tests. The fmmpeg checks can be disabled using -Dffmpeg.path=disabled.
