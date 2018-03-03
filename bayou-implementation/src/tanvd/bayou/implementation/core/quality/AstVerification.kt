package tanvd.bayou.implementation.core.quality

import tanvd.bayou.implementation.core.evidence.Evidences
import tanvd.bayou.implementation.core.synthesizer.DSubTree

interface AstVerification<E: Evidences, T: DSubTree> {
    fun verify(tree: T, evidences: E): Boolean
}