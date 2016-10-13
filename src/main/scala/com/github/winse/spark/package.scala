package com.github.winse

import org.apache.spark.SparkConf
import org.slf4j.LoggerFactory

package object spark {

  private val LOG = LoggerFactory.getLogger(getClass)

  def logInfo(f: => String) {
    if (LOG.isInfoEnabled)
      LOG.info(f)
  }

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
      case None => conf.setMaster("local[*]")
      case _ =>
    }

    // 添加配置。
    // 没网的情况下，找DNS然后报错要等很久！
    // @see org.apache.hadoop.security.UserGroupInformation#initialize
    // @see org.apache.hadoop.security.authentication.util.KerberosUtil#getDefaultRealm
    sys.props("java.security.krb5.kdc") = ""
    sys.props("java.security.krb5.realm") = ""

    conf
  }

}
