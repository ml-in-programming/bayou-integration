package tanvd.bayou.prototype

import com.intellij.codeInsight.completion.AllClassesGetter
import com.intellij.codeInsight.completion.PlainPrefixMatcher
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import tanvd.bayou.implementation.facade.SynthesisProgress
import tanvd.bayou.implementation.facade.SynthesisPhase
import tanvd.bayou.prototype.language.EvidencesInput
import tanvd.bayou.prototype.language.PsiClassesProcessor

class BayouRunnable(val project: Project, val title: String, val map: Map<String, EvidencesInput>) : Runnable {
    override fun run() {
        val indicator = ProgressManager.getInstance().progressIndicator
        runReadAction {
            AllClassesGetter.processJavaClasses(
                    PlainPrefixMatcher(""),
                    project,
                    GlobalSearchScope.projectScope(project),
                    PsiClassesProcessor(project, ProgressIndicatorWrapper(indicator), map)
            )
        }
    }
}

class ProgressIndicatorWrapper(val p: ProgressIndicator): SynthesisProgress {
    override var fraction: Double = 0.0
        set(value) {
            field = value
            p.fraction = value
        }
    override var phase: SynthesisPhase = SynthesisPhase.IDLE
        set(value) {
            field = value
            p.text = when (value) {
                SynthesisPhase.IDLE -> "Writing down"
                SynthesisPhase.Started -> "Started"
                SynthesisPhase.Parsing -> "Parsing Evidences"
                SynthesisPhase.Embedding -> "Wrangling Evidences"
                SynthesisPhase.SketchGeneration -> "Generating Sketches"
                SynthesisPhase.Concretization -> "Concretizating Sketches"
                SynthesisPhase.Finished -> "Done"
            }
        }

}
