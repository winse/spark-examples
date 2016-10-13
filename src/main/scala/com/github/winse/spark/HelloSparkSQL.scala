package com.github.winse.spark

import org.apache.spark.SparkContext
import org.apache.spark.sql.SQLContext

object HelloSparkSQL {
  // @see http://spark.apache.org/docs/latest/sql-programming-guide.html
  val sc = new SparkContext()
  val sqlContext = new SQLContext(sc)

  import sqlContext.implicits._

  case class Person(name: String, age: Int)

  val person = sc.textFile("peopel.txt").map(_.split(" ")).map(p => Person(p(0), p(1).trim.toInt))
  person.toDF().registerTempTable("person")

  val teenagers = sqlContext.sql("SELECT name, age FROM people WHERE age >= 13 AND age <= 19")
  teenagers.map(t => "Name: " + t(0)).collect().foreach(println)

}
