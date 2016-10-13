package com.github.winse.spark.access

import java.io.File

import org.apache.spark.sql.SQLContext
import org.apache.spark.{ SparkConf, SparkContext }

object AccessAnalyser {

  def main(args: Array[String]): Unit = {

    // conf

    // clean
    new File("target/generated-sources").listFiles().filter(_.isFile()).foreach(_.delete)

    sys.props("org.codehaus.janino.source_debugging.enable") = "true"
    sys.props("org.codehaus.janino.source_debugging.dir") = "target/generated-sources"

    val input = "r:/match10.dat"
    val output = "r:/output"
    def delete(f: File): Unit = {
      if (f.isDirectory) f.listFiles().foreach(delete)
      f.delete()
    }
    delete(new File(output))

    // program

    val conf = new SparkConf().setAppName("DPI Analyser").setMaster("local[10]")
    // fix windows path.
    conf.set( /*SQLConf.WAREHOUSE_PATH*/ "spark.sql.warehouse.dir", "spark-warehouse")

    val sc = new SparkContext(conf)
    val sqlContext = new SQLContext(sc)

    import sqlContext.implicits._
    import org.apache.spark.sql.functions._

    // e.g.
    //  '0000950e537','http://szextshort.weixin.qq.com/mmtls/2226a6e3','100325'
    val df = sqlContext.read
      .format("com.databricks.spark.csv")
      .option("header", "false") // Use first line of all files as header
      .option("quote", "'")
      .option("escape", "'")
      .option("delimiter", ",")
      .load(input)

    df
            .flatMap(Access(_))
//      .map(Access(_)).filter((t: Option[Access]) => !t.isEmpty).map(_.get) // sparksql不合适用Option
      .map(_.compute)
      //      .explain(true)
      .toDF("id", "score")
      .groupBy("id").agg(sum("score") as "score")
      .sort("score", "id")
      .repartition(1)
      .write.format("com.databricks.spark.csv").save(output)

    sc.stop()
  }

}
