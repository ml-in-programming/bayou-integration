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
package tanvd.bayou.implementation.core.synthesizer


/**
 * A method for taking code decorated with evidence of non-present api calls and completing the code with
 * those api calls.
 */
interface ApiSynthesizer {
    /**
     * Takes the given code containing evidence an replaces the evidence with corresponding api calls.
     *
     * @param code the evidence containing code
     * @param maxProgramCount the maximum number of possible completions to return
     * @return copies of the given code with different possible replacements of the evidence with api calls
     * @throws SynthesiseException if an error occurs in trying to synthesize the code completions
     */
    @Throws(SynthesiseException::class)
    fun synthesise(code: String, maxProgramCount: Int): Iterable<String>

    /**
     * Takes the given code containing evidence an replaces the evidence with corresponding api calls.
     *
     * @param code the evidence containing code
     * @param maxProgramCount the maximum number of possible completions to return
     * @param sampleCount the number of samples to draw from the underlying model in persuit of performing synthesis
     * @return copies of the given code with different possible replacements of the evidence with api calls
     * @throws SynthesiseException if an error occurs in trying to synthesize the code completions
     */
    @Throws(SynthesiseException::class)
    fun synthesise(code: String, maxProgramCount: Int, sampleCount: Int): Iterable<String>
}
