package org.svelto.watchdog

import org.eclipse.swt.widgets.Display
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Semaphore
import org.svelto.plugin.SveltoPlugin

/** A thread that continuously posts an `asyncExec` work chunk that will be executed by
 *  the UI thread. The work chunk simply closes down a latch
 */
class UIWorker(semaphore: Semaphore) extends Thread("Svelto worker") {
  val SLEEP_TIME = 100 // ms

  override def run() {
    while (!SveltoPlugin.stopped) {
      try {
        syncExec {
          semaphore.drainPermits()
          semaphore.release()
        }

        Thread.sleep(SLEEP_TIME)
      } catch {
        case e: InterruptedException =>
          println("Interrupted")
      }
    }
  }

  private def syncExec(f: => Unit) {
    Display.getDefault syncExec new Runnable {
      override def run() { f }
    }
  }

}