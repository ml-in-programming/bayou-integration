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
package tanvd.bayou.implementation.application.dom

import org.eclipse.jdt.core.dom.Expression

import java.util.ArrayList

open class Refinement {
    companion object {

        fun getRefinements(e: Expression): List<Refinement> {
            val refinements = ArrayList<Refinement>()

            /* Add here any new refinement types. If the refinement requires a new kind
         * of return type apart from these, then add it to the DSL and make it
         * extend this class (Refinement). */
            refinements.add(RefinementBoolean(e))
            refinements.add(RefinementNumber(e))
            refinements.add(RefinementString(e))

            return refinements
        }
    }
}
