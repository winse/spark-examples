package com.github.winse.spark

import org.apache.spark.{SparkConf, SparkContext}

object HelloWorld {

  def main(args: Array[String]): Unit = {
    val config = sparkConf.setAppName("Hello World")
    val sc = new SparkContext(config)
    val path = "pom.xml"

    sc.textFile(path).flatMap(_.split("""\s""")).map((_, 1)).reduceByKey(_ + _).foreach(println)
  }

}

object WordCountUseGroupBy {

  def main(args: Array[String]): Unit = {
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

}

/**
  * use ` mvn package ` build project, then
  */
object LineNumber extends App {

  val sc = new SparkContext(sparkConf().setAppName("Line Number"))
  val path = "pom.xml"

  val file = sc.textFile(path, 2).cache()
  val aLine = file.filter(_.contains("a")).count()
  val bLine = file.filter(_.contains("b")).count()

  println(s"Lines with a: $aLine, Lines with b: $bLine")

}