# Tachyon-Tunnel

This project is:
- An audio plugin system written in Java
- A few side projects

> [!WARNING]  
> This project is no longer actively maintained. I still sometimes fix bugs and update the Java and libraries version because I'm using it in various applications available at the [Tachyon Sonics Home Page](http://www.tachyon-sonics.com).

All projects are Eclipse-based.


## The plugin system

Existing plugin systems (CLAP, VST, LV2, etc.) require the host and the plugins to agree on a common interface:

Host --> | Plugin/host interface | <-- Plugins

The idea of the Tachyon-Tunnel plugin system is that the host and the plugins all choose whatever interface is best for them, and the Tachyon-Tunnel's engine does the conversion on the fly:

Host --> | Host interface X --> Tachyon Tunnel's Engine <-- Plugin interface Y | <-- Plugin

For example, the host may choose to use:
- A "pull" interface: give an output buffer of audio samples to produce and provide a callback to read from the audio input
- Stereo audio
- A block size of 512 sample frames

Whereas the plugin may choose to:
- Use an "inplace" interface: process audio in-place in multiple buffers
- Use mono audio
- Use a block size of 200 sample frames
- Report its latency

The Tachyon-Tunnel engine will automatically satisfy both the plugin and the host:
- Convert in-place processing to "pull" processing
- Use two instances of the plugin to process each channels
- Convert the block sizes automatically
- Compensate the plugin's reported latency on the fly so that the host see a zero-latency plugin

Apart from channels and block size, the following interfaces are available for both hosts and plugins:
- In-place processing (interface `ISimpleEffect`) - using a buffer for both input and output
- Pull processing (interface `IPullEffect`) - using a buffer for output and a callback for input
- Push processing (interface `IPushEffect`) - using a buffer for input and a callback for output

The engine can convert any plugin interface to either a pull or a push interface, so if a host uses the pull or the push interface it can use any plugin. If the host uses the in-place interface it can only use plugins using the in-place interface because the engine cannot convert the push or pull interface to in-place. It does however convert channels and block sizes.

The push and pull interfaces allow for time varying effects like time-stretching or resampling.

Interfaces for each approach exists separately for the plugins and for the host. In general, the host interfaces default to multi-channel, and have a special "mono" version, and it's the other way around for plugins.

In case it wasn't clear yet, the plugin system is 100% written in Java. It is supposed to be used by hosts and plugins written in Java. It is *not* supposed to work for hosts or plugins written in C or any other language.

There is a demo host, class `SimpleHost` in `ch.tachyon.tunnel.demo.host`; and some demo plugins in project `ch.tachyon.tunnel.demo.plugin`. To use them:

- Create a .jar of the demo plugins from Eclipse, using `Demo-Plugin.jardesc`. This will create `Demo-PLugins.jar`
- Start `SimpleHost` as a Java application
- It opens a basic GUI. In "Plugin Bundle", choose the .jar file created in the first step
- It should show three demo plugins: "FFT Equalizer", "Resample" and "Time Stretching"
- Choose input and output audio files, and click "Transform..."
- A second window opens with the selected plugin's parameters. Choose them and click "Ok" to process.

Limitations:
- The plugin's GUI is rendered by the Tachyon Tunnel's engine, not by the plugin or by the host. The plugin can provide hints in how to display each parameter using Java annotations
- The GUI is always rendered in a separate dialog
- A "random access" interface was started but never really implemented
- Probably many many other things... this project was stopped as soon as I had what I needed for other projects that I was more motivated to do... As such this project is no longer active.


## Side projects

### tritonus-tachyon

This is a fork of [tritonus](https://www.tritonus.org/) with the following main changes and improvements:

- Better support for reading random access files
- Support for AU files of more than 2 GB
- Support for WAVE files of more than 4GB using the "RF64" extension
- Support for PCM 32-bit float encoding in AU, WAVE and AIFF formats

For more details, see `src/README.txt` in the `tritonus-tachyon` subproject.

There are basic JUnit tests in `ch.tachyon.tritonus.test`. They must be launched with `-Dffmpeg.path=<path to ffmpeg>`, because they use `ffmpeg` to check the results.


### OPUS SPI

Project `ch.tachyon.opus-spi` is a JavaSound provider for the OPUS audio format, for both reading and writing.

It is based on:
- https://github.com/Gagravarr/VorbisJava (container format)
- https://github.com/lostromb/concentus (packet decoding)

