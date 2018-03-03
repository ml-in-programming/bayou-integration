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
package tanvd.bayou.implementation.model.configurable.synthesizer

/**
 * A wrapper for searching for a target type in the enumerator.
 * Also contains fields useful for making a decision when the search passes/fails.
 */
class SearchTarget
/**
 * Initializes the search target type
 *
 * @param type type to search for
 */
(
        /**
         * The type to search for -- the only required field for SearchTarget
         */
        /**
         * Gets the type for the search target
         *
         * @return current value
         */
        val type: Type) {

    /**
     * The parameter name (if any) for which the search is being conducted.
     * Used in order to create meaningful variable names if the search failed.
     */
    var paramName: String? = null

    /**
     * Denotes whether the enumerator should create a single use variable or not
     * if the search failed (e.g., API call arguments)
     */
    var singleUseVariable: Boolean = false

    /**
     * The name of the API call whose argument is being searched for.
     * Used for cost metric to order variables if the search passed.
     */
    /**
     * Gets the API call name whose argument is being searched for
     *
     * @return the API call name
     */
    var apiCallName: String? = null
        private set


    /**
     * Sets the API call name whose argument is being searched for
     *
     * @param s the API call name
     * @return this object for chaining
     */
    fun setAPICallName(s: String): SearchTarget {
        apiCallName = s
        return this
    }
}
