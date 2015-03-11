# Dispatch

```scala
import dispatch._, Defaults._
val svc = url("http://api.hostip.info/country.php")
val country = Http(svc OK as.String)
```