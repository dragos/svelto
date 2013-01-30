package org.svelto.watchdog

import org.eclipse.swt.widgets.Display
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Semaphore
import org.svelto.plugin.SveltoPlugin

/** A thread that continuously posts an `syncExec` work chunk that will be executed by
 *  the UI thread. The work chunk releases a semaphore that the Watchdog thread is waiting
 *  to acquire.
 *
 *  @see WatchdogThread
 */
class UIWorker(semaphore: Semaphore) extends Thread("Svelto worker") {

  override def run() {
    while (!SveltoPlugin.stopped) {
      try {
        syncExec {
          semaphore.drainPermits()
          semaphore.release()
        }

        Thread.sleep(SveltoPlugin.SLEEP_TIME)
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