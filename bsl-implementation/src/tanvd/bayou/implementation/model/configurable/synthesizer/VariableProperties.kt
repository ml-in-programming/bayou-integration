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
 * Properties of a variable in the type system. Allows chaining method calls.
 */
class VariableProperties {

    /**
     * Denotes if the variable is a user-defined variable in the param/body of the method
     */
    var userVar: Boolean = false

    /**
     * Denotes if the variable should participate in joins (e.g., catch clause variables will not)
     */
    var join: Boolean = false

    /**
     * Denotes if the variable needs to be initialized to a default value using $init
     */
    var defaultInit: Boolean = false

    /**
     * Denotes if the variable is single use only (e.g., API call arguments that had to be synthesized)
     */
    var singleUse: Boolean = false

}
