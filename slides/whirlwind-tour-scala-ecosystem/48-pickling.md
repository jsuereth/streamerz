# Scala Pickling

```scala
case class Person(name: String, age: Int)
val pkl = Person("foo", 20).pickle
val person = pkl.unpickle[Person]
```