/*
Copyright 2017 Rice University

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package tanvd.bayou.implementation.model.configurable.synthesizer.dsl

import tanvd.bayou.implementation.model.configurable.synthesizer.Synthesizable

abstract class DASTNode : Synthesizable {
    class TooManySequencesException : Exception()
    class TooLongSequenceException : Exception()

    @Throws(TooManySequencesException::class, TooLongSequenceException::class)
    abstract fun updateSequences(soFar: MutableList<Sequence>, max: Int, max_length: Int)

    abstract fun numStatements(): Int
    abstract fun numLoops(): Int
    abstract fun numBranches(): Int
    abstract fun numExcepts(): Int

    abstract fun bagOfAPICalls(): Set<DAPICall>

    abstract fun exceptionsThrown(): Set<Class<*>>

    abstract fun exceptionsThrown(eliminatedVars: Set<String>): Set<Class<*>>

    abstract override fun equals(o: Any?): Boolean

    abstract override fun hashCode(): Int

    abstract override fun toString(): String
}
