package com.jsuereth.video.swing


sealed trait UIControl
case object Play extends UIControl
case object Pause extends UIControl
case object Stop extends UIControl
