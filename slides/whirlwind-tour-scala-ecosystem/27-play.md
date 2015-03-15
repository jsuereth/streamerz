# Play Framework

```
# Home page
GET   /   controllers.Application.index
```

```scala
package controllers
object Application {
  def index = Action { implicit req =>
    Ok(views.html.index())
  }
}
```

```
@()(implicit req: RequestHeader)
@main {
  <h1>Hello, World</h1>
}
```