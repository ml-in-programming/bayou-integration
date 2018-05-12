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
package tanvd.bayou.implementation.model.android.synthesizer


import java.util.*

class SynthesisException(val id: Int) : RuntimeException(toMessage[id]) {
    companion object {

        val CouldNotResolveBinding = 1000
        val EvidenceNotInBlock = 1001
        val EvidenceMixedWithCode = 1002
        val MoreThanOneHole = 1003
        val InvalidEvidenceType = 1004
        val CouldNotEditDocument = 1005
        val ClassNotFoundInLoader = 1006
        val TypeNotFoundDuringSearch = 1007
        val MethodOrConstructorNotFound = 1008
        val GenericTypeVariableMismatch = 1009
        val InvalidKindOfType = 1010
        val MalformedASTFromNN = 1011

        private val toMessage: Map<Int, String>

        init {
            val _toMessage = HashMap<Int, String>()
            _toMessage.put(CouldNotResolveBinding,
                    "Could not resolve binding. Ensure CLASSPATH is set correctly.")
            _toMessage.put(EvidenceNotInBlock,
                    "Evidence should be given in a block.")
            _toMessage.put(EvidenceMixedWithCode,
                    "Evidence calls should appear in a separate empty block.")
            _toMessage.put(MoreThanOneHole,
                    "More than one hole for synthesis not currently supported.")
            _toMessage.put(InvalidEvidenceType,
                    "Invalid evidence type given.")
            _toMessage.put(CouldNotEditDocument,
                    "Could not edit document for some reason.")
            _toMessage.put(ClassNotFoundInLoader,
                    "Class could not be found in class loader.")
            _toMessage.put(TypeNotFoundDuringSearch,
                    "Type could not be found during combinatorial search.")
            _toMessage.put(MethodOrConstructorNotFound,
                    "Method or constructor not found in class.")
            _toMessage.put(GenericTypeVariableMismatch,
                    "Generic type variable name mismatched.")
            _toMessage.put(InvalidKindOfType,
                    "Invalid kind of type.")
            _toMessage.put(MalformedASTFromNN,
                    "Malformed AST predicted by neural network.")
            toMessage = Collections.unmodifiableMap(_toMessage)
        }
    }
}
