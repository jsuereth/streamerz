# Scalaz

```scala
(some(1) |@| some(2) |@| some(3))(_ + _ + _) === Some(6)
val list1: List[Option[Int]] = List(Some(1), Some(2), Some(3), Some(4))
assert(Traverse[List].sequence(list1) === Some(List(1,2,3,4)))
```

