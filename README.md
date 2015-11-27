# Streamerz

A playground of video processing examples in Akka streams and Scala (with some Kafka).

It's always been said that the best way to learn a topic through meme engineering.
This set of libraries enables all the fun you never wanted to have.



## Examples

1.  Rendering your webcam to the terminal in Ascii art

    ```
    $ sbt "examples/runMain examples.AsciiWebcam"
    ```

2.  Render video (and audio) to the terminal in Ascii Art

    ```
    $ sbt "examples/runMain examples.AsciiVideo"
    ```

3. Create a swing video player that can play/pause an mp4

    ```
    $ sbt "examples/runMain examples.VideoPlayer"
    ```

4. Run a web server that streams your webcam to web browsers in Ascii art

    ```
    $ brew install kafka 
    $ brew install zookeper
    ```

    ```
    $ zkServer start
    $ kafka-server-start.sh /usr/local/etc/kafka/server.properties &
    $ sbt "examples/runMain examples.asciiweb.feed.AsciiImageProducer" &
    $ sbt "examples/runMain examples.asciiweb.ws.Main" &
    ```
 
  You can now check your webcam/drone feed as an Ascii-art animation on `http://localhost:8080`

## License

Apache 2 for all libraries EXCEPT:

- ffmpeg library is LGPL v3 (Xuggler)
- example package is LGPL v3 (viral Xuggler)
