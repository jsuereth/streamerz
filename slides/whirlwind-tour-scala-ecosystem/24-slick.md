# Slick

Map SQL to scala types.

```scala
val limit = 10.0
( for {
    c <- coffees
    if c.price < limit
  } yield c.name
).result
```
