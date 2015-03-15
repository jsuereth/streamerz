# %green%Unfiltered


The unfiltered way:

```scala
val echo = unfiltered.filter.Planify {
  case Path(Seg(p :: Nil)) => ResponseString(p)
}
unfiltered.jetty.Server.anylocal.plan(echo).run()
```