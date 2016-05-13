package com.github.winse.spark

import java.lang.management.ManagementFactory
import java.util.concurrent.TimeUnit
import javax.management.ObjectName

import scala.beans.BeanProperty

object HelloMetrics extends App {

  import com.codahale.metrics.MetricRegistry._
  import com.codahale.metrics._

  val metricReg = new MetricRegistry()

  consoleExample()

  def jmxExample(): Unit = {
    val sleep = metricReg.counter(name(getClass, "sleeps"));

    // JmxAttributeGauge 获取已有系统的 jmx 对象，然后输出给其他系统，如ganglia等
    //    registry.register(name(SessionStore.class, "cache-evictions"),
    //    new JmxAttributeGauge("net.sf.ehcache:type=Cache,scope=sessions,name=eviction-count", "Value"));

    val reporter = JmxReporter
      .forRegistry(metricReg)
      .inDomain("com.github.winse")
      .build()
    reporter.start()

    // open MBeans tool, then click app ` com.intellij.rt.execution.application.AppMain `
    import scala.sys.process._
    "cmd /C %JAVA_HOME%/bin/jvisualvm.exe ".!

    for (i <- 0 to 60) {
      Thread.sleep(1000)
      sleep.inc()
    }

    println("jmx change property success, exit.")

  }

  def consoleExample() {
    val reporter = ConsoleReporter
      .forRegistry(metricReg)
      .convertRatesTo(TimeUnit.SECONDS)
      .convertDurationsTo(TimeUnit.MILLISECONDS).build()
    reporter.start(1, TimeUnit.SECONDS)

    val requests = metricReg.meter("requests");
    requests.mark();

    Thread.sleep(3000)
  }

}

object RWMetric extends App {

  trait StatMBean {
    def getReady: Boolean

    def setReady(ready: Boolean)

    def getSleepTimes(): Int
  }

  // 不能用object！用object后类名称带$后缀！！
  class Stat extends StatMBean {
    @BeanProperty var ready: Boolean = false
    @BeanProperty var sleepTimes: Int = 0
  }

  def openTool: Unit = {
    import scala.sys.process._
    """cmd  /S /C start /MAX %JAVA_HOME%/bin/jvisualvm.exe || exit """.!
  }

  val mBeanServer = ManagementFactory.getPlatformMBeanServer
  val name = new ObjectName("com.github.winse:name=Stat")
  val stat = new Stat
  mBeanServer.registerMBean(stat, name)

  val tool = new Thread(openTool)
  tool.setDaemon(false)
  tool.start()

  while (!stat.ready) {
    Thread.sleep(1000)
    stat.sleepTimes += 1
  }

  println("jmx change property success, exit.")

}