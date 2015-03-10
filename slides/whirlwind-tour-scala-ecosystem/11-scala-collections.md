# %red%Scala %blue%Collections

```scala
for {
   person <- people
   Address(city, _,_,_) <- person.addresses
   if city == "San Francisco"
} yield city
```