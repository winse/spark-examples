package com.github.winse.spark

import java.lang.management.ManagementFactory
import javax.management.ObjectName

import org.apache.log4j.{Level, Logger}
import org.apache.spark.{SparkConf, SparkContext}

import scala.beans.BeanProperty

object HelloWorld {
  val config = sparkConf()
  val path = "pom.xml"

  def main(args: Array[String]): Unit = {
    val sc = new SparkContext(config.setAppName("Hello World"))

    sc.textFile(path).flatMap(_.split("""\s""")).map((_, 1)).reduceByKey(_ + _).foreach(println)
  }
}

object HelloWorldStandalone {

  Logger.getLogger("org.apache.spark").setLevel(Level.WARN);
  Logger.getLogger("org.apache.spark.scheduler").setLevel(Level.DEBUG);
  Logger.getLogger("org.apache.spark.SparkContext").setLevel(Level.DEBUG);

  //  import scala.sys.process._
  //  "cmd /C mvn clean package -DskipTests".!

  val config = sparkConf()
    .setMaster("spark://localhost:7077")
    .setJars(Array("target/spark-examples-0.1.jar"))
    .set("spark.eventLog.enabled", "true")
    .set("spark.eventLog.dir", "file:///R:/spark-eventlogs")
    .set("spark.logLineage", "true")

  val path = "E:/winsegit/spark-examples/pom.xml"

  def main(args: Array[String]): Unit = {
    val sc = new SparkContext(config.setAppName("Hello World"))

    sc.textFile(path).flatMap(_.split("""\s""")).map((_, 1)).reduceByKey(_ + _).foreach(println)
  }

}

object WordCountUseGroupBy extends App {

  val sc = new SparkContext(sparkConf().setAppName("Word Count"))
  val path = "pom.xml"

  val res = sc.textFile(path)
    .flatMap(_.split("\\s"))
    .map((_, 1))
    .groupByKey()
    .map(pair => (pair._1, pair._2.sum))
    .collect

  println(scala.runtime.ScalaRunTime.stringOf(res))

}

object LineNumber extends App {

  val sc = new SparkContext(sparkConf().setAppName("Line Number"))
  val path = "pom.xml"

  val file = sc.textFile(path, 2).cache()
  val aLine = file.filter(_.contains("a")).count()
  val bLine = file.filter(_.contains("b")).count()

  println(s"Lines with a: $aLine, Lines with b: $bLine")

}