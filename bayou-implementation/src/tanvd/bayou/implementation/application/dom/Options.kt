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

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.apache.commons.cli.*
import org.apache.commons.io.FileUtils

import java.io.File
import java.io.IOException
import java.util.*

class Options @Throws(ParseException::class, IOException::class)
constructor(args: Array<String>) {

    internal var cmdLine: CommandLine
    internal var config: JsonObject

    val API_CLASSES: List<String>
    val API_PACKAGES: List<String>
    val API_MODULES: List<String>
    val KNOWN_CONSTANTS_BOOLEAN: Map<String, Boolean>
    val KNOWN_CONSTANTS_NUMBER: Map<String, Float>
    val KNOWN_CONSTANTS_STRING: Map<String, String>
    val NUM_UNROLLS: Int
    val MAX_SEQS: Int
    val MAX_SEQ_LENGTH: Int
    val JAVADOC_TYPE: String

    private fun addOptions(opts: org.apache.commons.cli.Options) {
        opts.addOption(Option.builder("f")
                .longOpt("input-file")
                .hasArg()
                .numberOfArgs(1)
                .required()
                .desc("input Java program")
                .build())

        opts.addOption(Option.builder("c")
                .longOpt("config-file")
                .hasArg()
                .numberOfArgs(1)
                .required()
                .desc("configuration JSON file")
                .build())

        opts.addOption(Option.builder("o")
                .longOpt("output-file")
                .hasArg()
                .numberOfArgs(1)
                .desc("output DSL AST to file")
                .build())
    }

    init {
        this.cmdLine = readCommandLine(args)
        this.config = readConfigFile(cmdLine.getOptionValue("config-file"))

        // API_CLASSES
        val classes = ArrayList<String>()
        if (this.config.has("api-classes"))
            for (e in this.config.getAsJsonArray("api-classes"))
                classes.add(e.asString)
        this.API_CLASSES = Collections.unmodifiableList(classes)

        // API_PACKAGES
        val packages = ArrayList<String>()
        if (this.config.has("api-packages"))
            for (e in this.config.getAsJsonArray("api-packages"))
                packages.add(e.asString)
        this.API_PACKAGES = Collections.unmodifiableList(packages)

        // API_MODULES
        val modules = ArrayList<String>()
        if (this.config.has("api-modules"))
            for (e in this.config.getAsJsonArray("api-modules"))
                modules.add(e.asString)
        this.API_MODULES = Collections.unmodifiableList(modules)

        // KNOWN_CONSTANTS_BOOLEAN
        val kb = HashMap<String, Boolean>()
        if (this.config.has("known-constants-boolean")) {
            val o = this.config.getAsJsonObject("known-constants-boolean")
            for ((key, value) in o.entrySet())
                kb.put(key, value.asBoolean)
        }
        this.KNOWN_CONSTANTS_BOOLEAN = Collections.unmodifiableMap(kb)

        // KNOWN_CONSTANTS_NUMBER
        val kn = HashMap<String, Float>()
        if (this.config.has("known-constants-number")) {
            val o = this.config.getAsJsonObject("known-constants-number")
            for ((key, value) in o.entrySet())
                kn.put(key, value.asFloat)
        }
        this.KNOWN_CONSTANTS_NUMBER = Collections.unmodifiableMap(kn)

        // KNOWN_CONSTANTS_STRING
        val ks = HashMap<String, String>()
        if (this.config.has("known-constants-string")) {
            val o = this.config.getAsJsonObject("known-constants-string")
            for ((key, value) in o.entrySet())
                ks.put(key, value.asString)
        }
        this.KNOWN_CONSTANTS_STRING = Collections.unmodifiableMap(ks)

        // NUM_UNROLLS
        if (this.config.has("num-unrolls"))
            this.NUM_UNROLLS = this.config.getAsJsonPrimitive("num-unrolls").asInt
        else
            this.NUM_UNROLLS = 1

        // MAX_SEQS
        if (this.config.has("max-seqs"))
            this.MAX_SEQS = this.config.getAsJsonPrimitive("max-seqs").asInt
        else
            this.MAX_SEQS = 10

        // MAX_SEQS
        if (this.config.has("max-seq-length"))
            this.MAX_SEQ_LENGTH = this.config.getAsJsonPrimitive("max-seq-length").asInt
        else
            this.MAX_SEQ_LENGTH = 10

        // Javadoc only
        if (this.config.has("javadoc-type")) {
            this.JAVADOC_TYPE = this.config.getAsJsonPrimitive("javadoc-type").asString
            if (!(this.JAVADOC_TYPE == "full" || this.JAVADOC_TYPE == "summary")) {
                throw IllegalArgumentException("javadoc-type must be \"full\" or \"summary\"")
            }
        } else
            this.JAVADOC_TYPE = "summary"
    }

    @Throws(ParseException::class)
    private fun readCommandLine(args: Array<String>): CommandLine {
        val parser = DefaultParser()
        val clopts = org.apache.commons.cli.Options()

        addOptions(clopts)

        try {
            return parser.parse(clopts, args)
        } catch (e: ParseException) {
            val help = HelpFormatter()
            help.printHelp("driver", HELP.replace('`', '\"'), clopts, "", true)
            throw e
        }

    }

    @Throws(IOException::class)
    private fun readConfigFile(file: String): JsonObject {
        val parser = JsonParser()
        val configFile = File(file)

        return parser.parse(FileUtils.readFileToString(configFile, "utf-8")).asJsonObject
    }

    companion object {

        internal var HELP = "Configuration options for driver:\n" +
                "{                                     |\n" +
                "  `api-classes`: [                    | Classes that driver should\n" +
                "      `java.io.BufferedReader`,       | extract data on. Must be fully\n" +
                "      `java.util.Iterator`            | qualified class names.\n" +
                "  ],                                  |\n" +
                "  `api-packages`: [                   | Packages that the driver\n" +
                "      `java.io`,                      | should extract data on.\n" +
                "      `java.net`                      |\n" +
                "  ],                                  |\n" +
                "  `api-modules`: [                    | Modules (for lack of a better\n" +
                "      `java`,                         | word) that driver should extract\n" +
                "      `javax`                         | data on.\n" +
                "  ],                                  |\n" +
                "  `num-unrolls`: 1,                   | Max unroll of loops in sequences\n" +
                "  `max-seqs`: 10,                     | Max num of sequences in sketches\n" +
                "  `javadoc-type`: `summary`           | `summary` (only first line),\n" +
                "                                      | `full` (everything)\n" +
                "}                                     |"
    }
}
