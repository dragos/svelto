package org.svelto.plugin

import org.eclipse.jface.preference.FieldEditorPreferencePage
import org.eclipse.jface.preference.IntegerFieldEditor
import org.eclipse.jface.preference.DirectoryFieldEditor
import org.eclipse.ui.IWorkbench
import org.eclipse.ui.IWorkbenchPreferencePage
import org.eclipse.swt.widgets._
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.ui.dialogs.PreferencesUtil
import org.eclipse.swt.SWT
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.layout.GridData
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer
import org.eclipse.core.runtime.Platform

class SveltoPreferences extends FieldEditorPreferencePage(FieldEditorPreferencePage.GRID) with IWorkbenchPreferencePage {

  override def createFieldEditors() {
    addField(new IntegerFieldEditor(SveltoPreferences.MAX_PAUSE, "&Maximum pause &(ms):", getFieldEditorParent(), 4))
    addField(new DirectoryFieldEditor(SveltoPreferences.OUTPUT_DIR, "&Output directory:", getFieldEditorParent()))
  }

  override def createContents(parent: Composite): Control = {
    val control = super.createContents(parent).asInstanceOf[Composite]
    val gridData = new GridData()

    gridData.horizontalSpan = 2
    val link = new Link(control, SWT.NONE)

    link.setLayoutData(gridData)
    link.setText("""You can disbale Svelto on the <a href="org.eclipse.ui.preferencePages.Startup">Workbench/Startup</a> preference page.""")
    link.addSelectionListener { e: SelectionEvent =>
      PreferencesUtil.createPreferenceDialogOn(parent.getShell, e.text, null, null)
    }

    control
  }

  implicit def fnToSelectionAdapter(p: SelectionEvent => Any): SelectionAdapter =
    new SelectionAdapter() {
      override def widgetSelected(e: SelectionEvent) { p(e) }
    }

  override def init(bench: IWorkbench) {
    setPreferenceStore(SveltoPlugin().getPreferenceStore())
    setDescription("Svelto can automatically save Java thread dumps when the UI thread is unresponsive")
  }
}

object SveltoPreferences {
  final val baseId = "org.svelto.plugin."
  final val MAX_PAUSE = baseId + "maxPause"
  final val OUTPUT_DIR = baseId + "outputDir"
}

class SveltoPreferencesInitializer extends AbstractPreferenceInitializer {
  import SveltoPreferences._

  override def initializeDefaultPreferences() {
    val store = SveltoPlugin().getPreferenceStore
    store.setDefault(MAX_PAUSE, 500)
    store.setDefault(OUTPUT_DIR, Platform.getInstanceLocation().getURL().toURI().getPath())
  }
}
