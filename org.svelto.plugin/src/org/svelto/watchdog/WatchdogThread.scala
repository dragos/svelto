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

class WatchdogThread extends Thread("Svelto Watchdog") {
  // maximum one permit
  private val semaphore = new Semaphore(1)

  def outDir: String =
    SveltoPlugin().getPreferenceStore().getString(SveltoPreferences.OUTPUT_DIR)

  def maxPause(): Int =
    SveltoPlugin().getPreferenceStore().getInt(SveltoPreferences.MAX_PAUSE)

  override def run() {
    Thread.sleep(10000) // enough to get the UI up and running (too many errors on startup otherwise)

    startWorker()

    while (true) {
      try {
        if (!semaphore.tryAcquire(1, maxPause, TimeUnit.MILLISECONDS))
          dumpThreads()
      } catch {
        case e: InterruptedException =>
        case e: Exception            => SveltoPlugin.log(IStatus.ERROR, "Uncaught exception", e)
      }
    }
  }

  private def dumpThreads() {
    SveltoPlugin.log(IStatus.WARNING, "UI thread blocked, dumping threads to " + outDir)
    val start = System.currentTimeMillis()
    val writer = new FileWriter(new File(outDir + "/threadDumps.txt"), true)

    val info = getThreadInfoMX

    // drain the queue and wait until the UI thread is responsive again
    semaphore.tryAcquire(1, 10, TimeUnit.SECONDS)
    val millis = System.currentTimeMillis() - start + maxPause

    val dump = """
================================================================================
[%tc] UI Thread blocked for %,3d milliseconds. Thread dump follows.
================================================================================
%s
""".format(Calendar.getInstance.getTime, millis, info)

    writer.write(dump)
    writer.flush()
    writer.close()
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

  private def getThreadInfoMX: String = {
    ManagementFactory.getThreadMXBean().dumpAllThreads(true, true).map(stringifyThreadInfo).mkString("\n")
  }

  private def stringifyThreadInfo(info: ThreadInfo): String = {
    import info._

    val sb = new StringBuilder("\"" + getThreadName() + "\"" + " Id=" + getThreadId() + " " + getThreadState());
    if (getLockName() != null) {
      sb.append(" on " + getLockName());
    }
    if (getLockOwnerName() != null) {
      sb.append(" owned by \"" + getLockOwnerName() + "\" Id=" + getLockOwnerId());
    }
    if (isSuspended()) {
      sb.append(" (suspended)");
    }
    if (isInNative()) {
      sb.append(" (in native)");
    }
    sb.append('\n');
    for ((ste, i) <- info.getStackTrace().zipWithIndex) {
      sb.append("\tat " + ste.toString());
      sb.append('\n');
      if (i == 0 && getLockInfo() != null) {
        getThreadState match {
          case Thread.State.BLOCKED =>
            sb.append("\t-  blocked on " + getLockInfo());
            sb.append('\n');

          case Thread.State.WAITING =>
            sb.append("\t-  waiting on " + getLockInfo());
            sb.append('\n');

          case Thread.State.TIMED_WAITING =>
            sb.append("\t-  waiting on " + getLockInfo());
            sb.append('\n');

          case _ =>
        }
      }

      for (mi <- getLockedMonitors) {
        if (mi.getLockedStackDepth() == i) {
          sb.append("\t-  locked " + mi);
          sb.append('\n');
        }
      }
    }

    val locks = getLockedSynchronizers();
    if (locks.length > 0) {
      sb.append("\n\tNumber of locked synchronizers = " + locks.length);
      sb.append('\n');
      for (li <- locks) {
        sb.append("\t- " + li);
        sb.append('\n');
      }
    }
    sb.append('\n');
    return sb.toString();
  }
}