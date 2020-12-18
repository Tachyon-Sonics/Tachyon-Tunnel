This .jar file is a demo command-line utility to perform audio time stretching.
It is implemented as a plugin and a host, that both use the tachyon tunner plugin system.

The source code of the command line can be found in file TimeStretcher.java. It uses the plugin
implemented by GradientTimeStretch.java.

You need to install a Java Runtime Environment (JRE) version 6 or greater to run the command-line.


Usage:

  java -jar TimeStretcher.jar [-z] <input_file> <ratio> <output_file>


Examples:

  java-jar TimeStretcher.jar Test.wav 0.5 Result.wav
Makes the audio file "Test.wav" two times faster, and save the result in file "Result.wav"

  java-jar TimeStretcher.jar -z Test.wav 2.0 Result.wav
Makes the audio file "Test.wav" two times slower, use zero padding to increase quality (at the expense of speed) and save the result in file "Result.wav"

Notes:
- Only .wav, .aiff and .au audio file formats (with PCM encoding) are supported
- The output file is overwritten without confirmation if it exists
