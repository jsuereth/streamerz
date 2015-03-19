# Spark

```scala
val textFile = sc.textFile("README.md")
val wordCounts =
  textFile flatMap { line =>
    line.split(" ")
  } map { word =>
    (word, 1)
  } reduceByKey { (a, b) =>
    a + b
  }
```
