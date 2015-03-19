# Rapture

```scala
import encodings.`UTF-8`
val src =
  uri"http://rapture.io/sample.json".slurp[Char]
import jsonBackends.jackson._
val json = Json.parse(src)
```