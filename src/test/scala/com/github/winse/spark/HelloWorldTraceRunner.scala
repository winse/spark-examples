package com.github.winse.spark

object HelloWorldTraceRunner extends App {

  import scala.sys.process._

  val clazz = HelloWorldLauncherWithServer.getClass
  val applicationName = clazz.getSimpleName.replace("""\$$""", "")
  //  println(clazz.getProtectionDomain.getCodeSource.getLocation)

  val cmd = s"""cmd /C set PATH=c:/cygwin/bin;%PATH% && jps | grep ${applicationName}| gawk "{print $$1}" """
  println(cmd)

  val pid = cmd.!!
  println(pid)

  if(pid == ""){
    stderr.print(s"not found application of $applicationName, exit now.")
    sys.exit(1)
  }

  sys.props("com.sun.btrace.debug") = "true"
  sys.props("com.sun.btrace.dumpClasses") = "false"
  sys.props("com.sun.btrace.unsafe") = "false"
  sys.props("com.sun.btrace.probeDescPath") = "."

  com.sun.btrace.client.Main.main(Array(s"$pid", "src/test/java/com/github/winse/spark/HelloWorldTrace.java"))

}
