# Scalacheck

```scala
object StringSpecification extends Properties("String") {
  property("startsWith") = forAll { (a: String, b: String) =>
    (a+b).startsWith(a)
  }
}
```