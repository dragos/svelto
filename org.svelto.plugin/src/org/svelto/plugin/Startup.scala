package org.svelto.plugin

import org.eclipse.ui.IStartup
import org.svelto.watchdog.WatchdogThread
import org.eclipse.core.runtime.IStatus

class Startup extends IStartup {

  override def earlyStartup() {
    SveltoPlugin.log(IStatus.OK, "Svelto plugin started")
    (new WatchdogThread).start()
  }
}