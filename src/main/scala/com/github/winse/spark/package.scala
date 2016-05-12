package com.github.winse

import com.typesafe.config.ConfigFactory
import org.apache.spark.{Logging, SparkConf}

import scala.collection.JavaConversions._

package object spark extends Logging {

  implicit def funToRunnable(f: => Unit) = new Runnable() {
    def run() = f
  }

  def Try[T <: java.io.Closeable](resource: T)(f: T => Unit) {
    try {
      f(resource)
    } finally {
      resource.close()
    }
  }

  def sparkConf(): SparkConf = {
    logInfo("now, create spark config...")

    val conf = new SparkConf()
    conf.getOption("spark.master") match {
      case None => conf.setMaster("local")
      case _ =>
    }

    conf
  }

}
