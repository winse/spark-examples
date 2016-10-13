package com.github.winse.spark.access

import org.apache.spark.sql.Row

case class Access(id: String, url: String, time: String) {

  def compute(): (String, Int) =
    try {
      var score = 0

      //.. business
      score += 1

      (id, score)
    } catch {
      case _: Throwable =>
        (id, 0)
    }

}

object Access {

  def apply(row: Row): Option[Access] =
    try {
      // time must be integers
      row(2).toString.toInt

      Some(new Access(row(0).toString, row(1).toString, row(2).toString))
    } catch {
      case _: Throwable =>
        println(row)
        None
    }

}