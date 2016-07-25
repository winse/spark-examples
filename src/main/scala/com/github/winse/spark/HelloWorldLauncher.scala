package com.github.winse.spark

import org.apache.hadoop.io.IOUtils
import org.apache.spark.Logging
import org.apache.spark.launcher.{SparkAppHandle, SparkLauncher}

import scala.collection.JavaConversions._
import scala.concurrent.Lock

trait SparkLauncherCreator {

  def create(): SparkLauncher = {

    // 通过launcher启动必须编译!!
    // 加参数也没用，SPARK_HOME 下面一定要 spark-assembly*hadoop*.jar
    // 就算修改脚本绕过去了，还是会缺少 scala-library.jar
    //
    // package:
    //   E:\git\spark\assembly>mvn package
    val launcher = new SparkLauncher(Map("SPARK_TESTING" -> "1"))

    launcher
      // 用于查找运行脚本位置
      .setSparkHome("""E:\git\spark""")
      .setVerbose(true)
      .setMainClass(s"${getClass.getPackage.getName}.HelloWorld")
      .setMaster("local[2]")
      // or use env SPARK_DIST_CLASSPATH instead.
      .setConf("spark.driver.extraClassPath", "target/classes")
      .setConf("spark.driver.extraJavaOptions", """-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005""")
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

    // 由于cmd编码是GBK的，在console窗口打印的中文会乱码!!
    new Thread(Try(p.getInputStream)(IOUtils.copyBytes(_, System.out, 4096))).start()
    new Thread(Try(p.getErrorStream)(IOUtils.copyBytes(_, System.err, 4096))).start()

    sys.exit(p.waitFor())
  }
}


/**
  * startApplication 比 launch 高级了很多(有服务端的支持)。
  *
  * 在 launch 提交任务的基础上，启动 LauncherServer 通过socket与Driver建立连接获取程序的状态
  * (具体以后看到spark-core再研究)，同时通知注册的listener。
  */
object HelloWorldLauncherWithServer extends SparkLauncherCreator with Logging {
  val lock = new Lock
  lock.available = false

  object Status extends SparkAppHandle.Listener {

    private def info(handle: SparkAppHandle): Unit = {
      logInfo(s"${handle.getAppId} : ${handle.getState}")
    }

    override def infoChanged(handle: SparkAppHandle): Unit = {
      info(handle)
    }

    override def stateChanged(handle: SparkAppHandle): Unit = {
      info(handle)

      if (handle.getState.isFinal) {
        lock.release()
      }
    }

  }

  def main(args: Array[String]) {
    // 与Driver端 org.apache.spark.launcher.LauncherBackend 有交互
    // + yarn.Client.$
    // + scheduler.cluster.SparkDeploySchedulerBackend
    // + scheduler.local.LocalBackend
    val handle = create().startApplication(Status)

    logInfo("start application and now running...")

    lock.acquire()
  }

}
