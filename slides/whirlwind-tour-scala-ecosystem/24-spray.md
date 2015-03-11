# Spray

```scala
class MyServiceActor extends Actor with HttpService {
 val myRoute =
    path("") {
      get {
        respondWithMediaType(`text/html`) {
          complete {
            <html>
              <body>
                <h1>Say hello to <i>spray-routing</i> on <i>spray-can</i>!</h1>
              </body>
            </html>
          }
        }
      }
    }
  def receive = runRoute(myRoute)
}
```