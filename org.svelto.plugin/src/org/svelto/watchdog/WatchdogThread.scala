package org.svelto.watchdog

import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import org.eclipse.core.runtime.Platform
import java.io.File
import java.io.BufferedWriter
import java.io.FileWriter
import scala.collection.JavaConverters
import org.svelto.plugin.SveltoPlugin
import org.eclipse.core.runtime.Status
import org.eclipse.core.runtime.IStatus
import java.io.PrintWriter
import java.lang.management.ManagementFactory
import java.lang.management.ThreadInfo
import java.util.Calendar
import org.svelto.plugin.SveltoPreferences
import java.io.Writer

/** This thread tries to acquire a semaphore that is released inside the UI thread.
 *  If the acquire times out, it's likely that the UI thread is busy and didn't get to
 *  execute the `release()` posted by `UIWorker`.
 *
 *  On timeout it saves a thread dump.
 */
class WatchdogThread extends Thread("Svelto Watchdog") {
  // maximum one permit
  private val semaphore = new Semaphore(1)

  def outDir: String =
    SveltoPlugin().getPreferenceStore().getString(SveltoPreferences.OUTPUT_DIR)

  def maxPause: Int =
    SveltoPlugin().getPreferenceStore().getInt(SveltoPreferences.MAX_PAUSE)

  def ignoreSwtInternal: Boolean =
    SveltoPlugin().getPreferenceStore().getBoolean(SveltoPreferences.IGNORE_SWT)

  override def run() {
    Thread.sleep(10000) // enough to get the UI up and running (too many false positives on startup otherwise)

    startWorker()

    while (true) {
      Thread.sleep(SveltoPlugin.SLEEP_TIME)
      try {
        if (!SveltoPlugin.stopped && !semaphore.tryAcquire(1, maxPause, TimeUnit.MILLISECONDS))
          dumpThreads()
      } catch {
        case e: InterruptedException =>
        case e: Exception            => SveltoPlugin.log(IStatus.ERROR, "Uncaught exception", e)
      }
    }
  }

  private def dumpThreads() {
    val start = System.currentTimeMillis()
    appendToFile(new File(outDir + File.separatorChar + "threadDumps.txt")) { writer =>

      val info = getThreadInfoMX
      if (shouldReport(info)) {
        SveltoPlugin.log(IStatus.WARNING, "UI thread blocked, dumping threads to " + outDir)
        val (mainThread, rest) = info.toSeq.partition(_.getThreadName().startsWith("main"))
        val infoString = (mainThread ++ rest).map(stringifyThreadInfo).mkString("\n")

        // wait until the UI thread is responsive again
        semaphore.tryAcquire(1, 10, TimeUnit.SECONDS)
        val millis = System.currentTimeMillis() - start + maxPause

        val dump = """
================================================================================
[%tc] UI Thread blocked for %,3d milliseconds. Thread dump follows.
================================================================================
%s
""".format(Calendar.getInstance.getTime, millis, infoString)

        writer.write(dump)
      } else
        println("skipped thread dump inside swt.internal package")
    }
  }

  private def appendToFile[T](file: File)(f: Writer => T) = {
    var writer: Writer = null
    try {
      writer = new FileWriter(new File(outDir + File.separatorChar + "threadDumps.txt"), true)
      f(writer)
    } finally {
      if (writer ne null) writer.close()
    }
  }

  private def startWorker() {
    val t = new UIWorker(semaphore)
    t.start()
  }

  private def getThreadInfoSE: String = {
    val buf = new StringBuilder

    import JavaConverters._
    for ((thread, frames) <- Thread.getAllStackTraces().asScala) {
      buf.append(thread.getName() + "\n")
      buf.append(frames.mkString("\n\t"))
      buf.append("\n\n")
    }

    buf.toString
  }

  private def getThreadInfoMX = {
    ManagementFactory.getThreadMXBean().dumpAllThreads(true, true)
  }

  /** Don't report hangs in SWT internal code */
  private def shouldReport(tis: Array[ThreadInfo]): Boolean =
    (!ignoreSwtInternal ||
      !tis.find(_.getThreadName().startsWith("main")).exists(_.getStackTrace().take(2).exists(_.getClassName().startsWith("org.eclipse.swt.internal."))))

  private def stringifyThreadInfo(info: ThreadInfo): String = {
    import info._

    val sb = new StringBuilder("\"" + getThreadName() + "\"" + " Id=" + getThreadId() + " " + getThreadState())

    if (getLockName() != null) sb.append(" on " + getLockName())
    if (getLockOwnerName() != null) sb.append(" owned by \"" + getLockOwnerName() + "\" Id=" + getLockOwnerId())
    if (isSuspended()) sb.append(" (suspended)")
    if (isInNative()) sb.append(" (in native)")

    sb.append('\n')
    for ((ste, i) <- info.getStackTrace().zipWithIndex) {
      sb.append("\tat " + ste.toString())
      sb.append('\n')
      if (i == 0 && getLockInfo() != null) {
        getThreadState match {
          case Thread.State.BLOCKED =>
            sb.append("\t-  blocked on " + getLockInfo())
            sb.append('\n')

          case Thread.State.WAITING =>
            sb.append("\t-  waiting on " + getLockInfo())
            sb.append('\n')

          case Thread.State.TIMED_WAITING =>
            sb.append("\t-  waiting on " + getLockInfo())
            sb.append('\n')

          case _ =>
        }
      }

      for (mi <- getLockedMonitors) {
        if (mi.getLockedStackDepth() == i) {
          sb.append("\t-  locked " + mi)
          sb.append('\n')
        }
      }
    }

    val locks = getLockedSynchronizers()
    if (locks.length > 0) {
      sb.append("\n\tNumber of locked synchronizers = " + locks.length)
      sb.append('\n')
      for (li <- locks) {
        sb.append("\t- " + li)
        sb.append('\n')
      }
    }
    sb.append('\n')
    return sb.toString()
  }
}
