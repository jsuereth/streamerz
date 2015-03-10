# Futures

```scala
def handleWebRequest(req: Request): Future[Response] =
  for {
     (user, data) <- getUserData(req) zip getQueryData(req)
     if user.isAuthenticated
  } yield renderResultsPage(user, data)
```