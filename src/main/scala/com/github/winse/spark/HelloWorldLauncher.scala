package com.github.winse.spark

import org.apache.hadoop.io.IOUtils
import org.apache.spark.Logging
import org.apache.spark.launcher.{SparkAppHandle, SparkLauncher}

import scala.collection.JavaConversions._
import scala.concurrent.Lock

trait SparkLauncherCreator {

  def create(): SparkLauncher = {

    // 加参数也没用，windows 下面一定要 spark-assembly*hadoop*.jar
    // 就算修改脚本绕过去了，还是没有 scala-library.jar
    //
    // package:
    //   E:\git\spark\assembly>mvn package
    val launcher = new SparkLauncher(Map("SPARK_TESTING" -> "1", "SPARK_USER" -> "spark"))

    launcher
      // 用于查找运行脚本位置
      .setSparkHome("""E:\git\spark""")
      .setVerbose(true)
      .setMainClass(s"${getClass.getPackage.getName}.HelloWorld")
      .setMaster("local[2]")
      // or use env SPARK_DIST_CLASSPATH instead.
      .setConf("spark.driver.extraClassPath", "target/classes")
      .setConf("spark.driver.extraJavaOptions", """-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005""")
      .setConf("java.net.preferIPv4Stack", "true")
      // @see org.apache.spark.launcher.SparkSubmitCommandBuilder.specialClasses
      .setAppResource("spark-internal")

  }

}

/**
  * 这种方式没啥意思，搞到最后还是调用脚本去执行
  */
object HelloWorldLauncher extends SparkLauncherCreator {

  def main(args: Array[String]) {
    val p = create().launch()

    new Thread(Try(p.getInputStream)(IOUtils.copyBytes(_, System.out, 4096))).start()
    new Thread(Try(p.getErrorStream)(IOUtils.copyBytes(_, System.err, 4096))).start()

    sys.exit(p.waitFor())
  }
}


object HelloWorldLauncherWithServer extends SparkLauncherCreator with Logging {

  def main(args: Array[String]) {
    val lock = new Lock
    lock.available = false

    object Status extends SparkAppHandle.Listener {

      override def infoChanged(handle: SparkAppHandle): Unit = {
        logInfo(handle.toString)
      }

      override def stateChanged(handle: SparkAppHandle): Unit = {
        logInfo(handle.toString)

        if (handle.getState.isFinal) {
          lock.release()
        }
      }

    }

    val handle = create().startApplication(Status)

    logInfo(s"${handle.getAppId}: ${handle.getState}")

    lock.acquire()
  }

}
