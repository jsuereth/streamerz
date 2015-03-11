# Lift

```
<lift:surround with="default" at="content">
 <div data-lift="Template.show?template=_simple_template"></div>
</lift:surround>
```

```scala
object Template extends DispatchSnippet {
  def dispatch = {
    case "show" => show _
  }
  def show(in: NodeSeq) = {
    {
      val users = User.findAll
      users.length match {
        case 0 => "#hasRecords" #> Text("No records found")
        case _ =>
          "#head-one *" #> "Name" &
          "#head-two *" #> "Email" &
          ".row *" #> (users.map { u =>
            ".name *" #> u.firstName.is &
            ".email *" #> u.email.is
          })
      }
    }.apply(templateFromTemplateAttr openOr Text("Error processing template"))
  }
}
```