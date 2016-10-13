package com.github.winse

import org.codehaus.commons.compiler.IScriptEvaluator
import org.codehaus.janino.ScriptEvaluator

object janinoHello {
  def main(args: Array[String]) {
    sys.props("org.codehaus.janino.source_debugging.enable") = "true"
    sys.props("org.codehaus.janino.source_debugging.dir") = "target/generated-test-sources"

    val se: IScriptEvaluator = new ScriptEvaluator()
    val run = se.createFastEvaluator(
      """
    System.out.println("Hello");
    String s = "World";
    System.out.println(s);
      """,
      classOf[Runnable],
      new Array[String](0)).asInstanceOf[Runnable]

    run.run()
  }
}