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
package tanvd.bayou.implementation


import tanvd.bayou.implementation.facade.SimpleSynthesisProgress
import java.io.File

internal object TestClient {
    private val _testDialogJava = """
import edu.rice.cs.caper.bayou.annotations.Evidence;
import java.util.List;

public class TestUtil {
    void write(String file) {
        Evidence.types("FileOutputStream");
        Evidence.types("Random");
    }
}
        """

    private val _testDialogAndroid = """
import edu.rice.cs.caper.bayou.annotations.Evidence;
import android.bluetooth.BluetoothSocket;

public class TestBluetooth {
    void readFromBluetooth(BluetoothSocket adapter) {
        Evidence.apicalls("getInputStream");
    }
}

        """

    private fun synthesise(code: String) {
//        val results: List<String> = BayouClient.getModel("android").synthesize(code, 100).toList()
        val results: List<String> = BayouClient.getConfigurableModel(File("C:\\Users\\TanVD\\Work\\Diploma\\bayou-integration\\bayou-implementation\\resources\\stdlib.json").readText()).synthesize(code, 100, SimpleSynthesisProgress()).toList()

        for (result in results) {
            println("\n---------- BEGIN PROGRAM  ----------")
            print(result)
        }
        print("\n") // don't have next console prompt start on final line of code output.
    }

    @JvmStatic
    fun main(args: Array<String>) {
        synthesise(_testDialogJava)
    }
}
