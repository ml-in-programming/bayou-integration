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

class Variable internal constructor(val name: String, val type: Type) {
    internal var refCount: Int = 0

    fun addRefCount() {
        refCount += 1
    }

    override fun equals(o: Any?): Boolean {
        if (o == null || o !is Variable)
            return false
        val v = o as Variable?
        return v!!.name == name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun toString(): String {
        return name + ":" + type
    }
}
