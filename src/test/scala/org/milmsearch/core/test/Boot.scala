package org.milmsearch.core.test

import scala.collection.mutable.ListBuffer

import net.liftweb.common.Logger
import net.liftweb.db.DefaultConnectionIdentifier
import net.liftweb.db.StandardDBVendor
import net.liftweb.mapper.DB
import net.liftweb.util.Props

/**
 * テスト開始前(終了後)に実行する処理を書く
 */
object Boot extends Logger {
  
  /**
   * テスト終了後に実行したい処理のリスト
   */
  private val cleanupHooks: ListBuffer[() => Unit] = ListBuffer()

  /**
   * テスト開始前に実行する処理
   */
  def setup() {
    try { initializeDBCon() } catch { case e => error(e); throw e }
  }
  
  /**
   * データベース接続を初期化する
   */
  private def initializeDBCon() {
    lazy val defaultDriver = "org.h2.Driver"
    lazy val defaultUrl = "jdbc:h2:mem:milmsearch_test;DB_CLOSE_DELAY=-1"
      
    val vendor = new StandardDBVendor(
      Props.get("db.driver") openOr defaultDriver,
      Props.get("db.url") openOr defaultUrl,
      Props.get("db.user"),
      Props.get("db.password")
    )
    
    DB.defineConnectionManager(DefaultConnectionIdentifier, vendor)
    
    cleanupHooks += vendor.closeAllConnections_!
  }
  
  /**
   * テスト終了後に実行する処理
   */
  def cleanup() {
    cleanupHooks foreach { f =>
      try { f() } catch { case e => error(e) }
    }
  }
  
}
