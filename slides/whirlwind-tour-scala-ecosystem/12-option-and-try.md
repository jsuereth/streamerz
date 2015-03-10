# %red%Option %white%and %blue%Try

```scala
def findUser(...): Option[User] = ...
findUser(...) orElse User.Anonymous
```

%black%....... %red%vs. %black%....... %reset%

```scala
Try(numberString.toInt) match {
  case Success(number) => println("Yay!")
  case Failure(ex) => println("Boo!")
}
```