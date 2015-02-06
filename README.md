# Streamerz

A playground of video processing examples in Akka streams and Scala.

It's always been said that the best way to learn a topic through meme engineering.
This set of libraries enables all the fun you never wanted to have.



## Examples

1.  Rendering your webcam to the terminal in Ascii art

    $ sbt "examples/runMain examples.AsciiWebcam"

2.  Render video (and audio) to the terminal in Ascii Art

    $ sbt "examples/runMain examples.AsciiVideo"

3. Create a swing video player that can play/pause an mp4

    $ sbt "examples/runMain examples.VideoPlayer"


## License

Apache 2 for all libraries EXCEPT:

- ffmpeg library is LGPL v3 (Xuggler)
- example package is LGPL v3 (viral Xuggler)
