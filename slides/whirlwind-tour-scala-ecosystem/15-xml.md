# Scala Xml

```scala
val title: String = "Hello, XHTML world!"
val content: String = title
val result =
   <html>
     <head><title>{title}</title></head>
     <body>
       {content}
     </body>
   </html>
```