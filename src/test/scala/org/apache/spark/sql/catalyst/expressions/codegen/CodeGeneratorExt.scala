package org.apache.spark.sql.catalyst.expressions.codegen

import java.io.ByteArrayInputStream
import java.util.{Map => JavaMap}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

import com.google.common.cache.{CacheBuilder, CacheLoader}
import org.codehaus.janino.{ByteArrayClassLoader, ClassBodyEvaluator, SimpleCompiler}
import org.codehaus.janino.util.ClassFile
import scala.language.existentials

import org.apache.spark.SparkEnv
import org.apache.spark.internal.Logging
import org.apache.spark.metrics.source.CodegenMetrics
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.expressions._
import org.apache.spark.sql.catalyst.util.{ArrayData, MapData}
import org.apache.spark.sql.types._
import org.apache.spark.unsafe.Platform
import org.apache.spark.unsafe.types._
import org.apache.spark.util.{ParentClassLoader, Utils}
import org.scalatest.run
import java.io.File


object CodeGeneratorExt extends Logging {

  /**
   * Compile the Java source code into a Java class, using Janino.
   */
  private[this] def doCompile(code: CodeAndComment): GeneratedClass = {
    val evaluator = new ClassBodyEvaluator()

    // A special classloader used to wrap the actual parent classloader of
    // [[org.codehaus.janino.ClassBodyEvaluator]] (see CodeGenerator.doCompile). This classloader
    // does not throw a ClassNotFoundException with a cause set (i.e. exception.getCause returns
    // a null). This classloader is needed because janino will throw the exception directly if
    // the parent classloader throws a ClassNotFoundException with cause set instead of trying to
    // find other possible classes (see org.codehaus.janinoClassLoaderIClassLoader's
    // findIClass method). Please also see https://issues.apache.org/jira/browse/SPARK-15622 and
    // https://issues.apache.org/jira/browse/SPARK-11636.
    val parentClassLoader = new ParentClassLoader(Utils.getContextOrSparkClassLoader)
    evaluator.setParentClassLoader(parentClassLoader)
    // Cannot be under package codegen, or fail with java.lang.InstantiationException
    
    // TODO 修改1
//    evaluator.setClassName("org.apache.spark.sql.catalyst.expressions.GeneratedClass")

    evaluator.setDefaultImports(Array(
      classOf[Platform].getName,
      classOf[InternalRow].getName,
      classOf[UnsafeRow].getName,
      classOf[UTF8String].getName,
      classOf[Decimal].getName,
      classOf[CalendarInterval].getName,
      classOf[ArrayData].getName,
      classOf[UnsafeArrayData].getName,
      classOf[MapData].getName,
      classOf[UnsafeMapData].getName,
      classOf[MutableRow].getName,
      classOf[Expression].getName
    ))
    evaluator.setExtendedClass(classOf[GeneratedClass])

    lazy val formatted = CodeFormatter.format(code)

    logDebug({
      // Only add extra debugging info to byte code when we are going to print the source code.
      // TODO 修改3
//      evaluator.setDebuggingInformation(true, true, false)
      evaluator.setDebuggingInformation(true, true, true)
      s"\n$formatted"
    })

    try {
      // TODO 修改2
//      evaluator.cook(generated.java", code.body)
      evaluator.cook(code.body)
      recordCompilationStats(evaluator)
    } catch {
      case e: Exception =>
        val msg = s"failed to compile: $e\n$formatted"
        logError(msg, e)
        throw new Exception(msg, e)
    }
    evaluator.getClazz().newInstance().asInstanceOf[GeneratedClass]
  }

  /**
   * Records the generated class and method bytecode sizes by inspecting janino private fields.
   */
  private def recordCompilationStats(evaluator: ClassBodyEvaluator): Unit = {
    // First retrieve the generated classes.
    val classes = {
      val resultField = classOf[SimpleCompiler].getDeclaredField("result")
      resultField.setAccessible(true)
      val loader = resultField.get(evaluator).asInstanceOf[ByteArrayClassLoader]
      val classesField = loader.getClass.getDeclaredField("classes")
      classesField.setAccessible(true)
      classesField.get(loader).asInstanceOf[JavaMap[String, Array[Byte]]].asScala
    }

    // Then walk the classes to get at the method bytecode.
    val codeAttr = Utils.classForName("org.codehaus.janino.util.ClassFile$CodeAttribute")
    val codeAttrField = codeAttr.getDeclaredField("code")
    codeAttrField.setAccessible(true)
    classes.foreach { case (_, classBytes) =>
      CodegenMetrics.METRIC_GENERATED_CLASS_BYTECODE_SIZE.update(classBytes.length)
      val cf = new ClassFile(new ByteArrayInputStream(classBytes))
      cf.methodInfos.asScala.foreach { method =>
        method.getAttributes().foreach { a =>
          if (a.getClass.getName == codeAttr.getName) {
            CodegenMetrics.METRIC_GENERATED_METHOD_BYTECODE_SIZE.update(
              codeAttrField.get(a).asInstanceOf[Array[Byte]].length)
          }
        }
      }
    }
  }


  def main(args: Array[String]) {
    // clean
	  new File("target/generated-test-sources").listFiles().filter(_.isFile()).foreach(_.delete)

	  sys.props("org.codehaus.janino.source_debugging.enable") = "true"
    sys.props("org.codehaus.janino.source_debugging.dir") = "target/generated-test-sources"

    val run = doCompile(new CodeAndComment("""
      
 public Object generate(Object[] references) {
   return new GeneratedIterator();
 }
 
 final class GeneratedIterator implements Runnable {
   public void run(){
     System.out.println("Hello");
     String s = "World";
     System.out.println(s);
   } 
 }
 
      """,
      Map()))
      .generate(new Array[Any](0)).asInstanceOf[Runnable]

    run.run()

  }

}