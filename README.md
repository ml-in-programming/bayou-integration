# BSL Code Synthesizer
BSL Code Synthesizer project is fully JVM implementation of Bayesian Sketch Learning synthesizer based on paper of CaperGroup and their reference implementation of a such synthesizer --- Bayou.

BSL Code Synthesizer includes a plugin to IntelliJ IDEA with own DSL language which capable of synthesizing Java STDlib and Android SDK code right into IntelliJ IDEA

## Structure
BSL Code Synthesizer project contains of two main modules:
* bsl-implementation -- JVM implementation of BSL Code Synthesizer. It is configurable and supports two "metamodels" -- Java STDlib and Android SDK (it means, that we are providing two configurations right now, but it is definitely possible to configure any "metamodel" you need, if bsl-implementation has needed algorithms).
* bsl-plugin -- IntelliJ IDEA plugin which is a UI frontend to bsl-implementation. It provides DSL to write synthesizing queries and intention actions to call bsl-implementation.

## BSL plugin

To work with plugin you need to install it (from Plugins repository or just build it from scratch from this Github repository).

One the first run of synthesizing plugin will download all needed models into temporary location (depends on your OS). 

To start you synthesizing you need to create method (into body of which synthesized code will be inserted). Here create multiline comment "/* ... /*", hit alt+enter, select Inject language or reference and select here BSL language.

### DSL language
DSL language consists of header and number of evidence block.
In header (first line) you need to select type of synthesizer -- STDLIB or ANDROID.
One the next lines you need to add evidences:
* For STDLIB (API|TYPE):=VALUE
* For ANDROID (API|TYPE|CONTEXT):=VALUE

Language errors are annotated, so in case of problems just take a closer look at this annotations.

### Examples

Let's consider a few examples.

Delete file:

![Delete file synthesizing](https://s3-eu-west-1.amazonaws.com/public-resources.ml-labs.aws.intellij.net/bayou/gifs/delete_file_test.gif)

Read file: 

![Read file synthesizing](https://s3-eu-west-1.amazonaws.com/public-resources.ml-labs.aws.intellij.net/bayou/gifs/delete_file_test.gif)

Clear list:

![Clear list synthesizing](https://s3-eu-west-1.amazonaws.com/public-resources.ml-labs.aws.intellij.net/bayou/gifs/remove_list_test.gif)



