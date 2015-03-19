# Reactive Collections

The building blocks of FRP.

```scala
val slideControl = frp.events.collect {
  case Space() => NextSlide()
  case LeftKey() => PreviousSlide()
  case RightKey() => NextSlide()
  case UpKey() => FirstSlide()
  case DownKey() => LastSlide()
}
val latestEventLabel =
  frp.events.signal(KeyPress(0)).map(_.toString)
```