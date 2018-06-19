# BSL Code Synthesizer
BSL Code Synthesizer project is a JVM implementation of the Bayesian Sketch Learning synthesizer based on the CaperGroup's [paper](https://arxiv.org/abs/1703.05698) and [Bayou](https://github.com/capergroup/bayou), their reference implementation of a such synthesizer.

BSL Code Synthesizer is implemented as an IntelliJ IDEA plugin capable of synthesizing Java STDlib and Android SDK code from a specification in a custom DSL.

## Code Structure
BSL Code Synthesizer consists of two main modules:
* bsl-implementation -- a JVM implementation of a BSL Code Synthesizer. It is highly configurable and currently supports two "metamodels": Java STDlib and Android SDK. Any other metamodel can be defined if all appropriate algorithms are implemented within this library.
* bsl-plugin -- an IntelliJ IDEA plugin which acts as a UI frontend to the bsl-implementation library. It provides a DSL to define synthesizing queries and intention actions to call bsl-implementation.

## The IntelliJ IDEA Plugin

The plugin could be installed from the Plugins Repository or build from scratch from this Github repository.

On the first synthesis command the plugin will download all needed models into a temporary folder depending on your OS. 

To run the synthesis you need to create a Java method where the synthesized code will be placed. Create a multiline comment "/* ... /*" before this method, hit Alt+Enter, select `Inject language or reference` and select `Bayou language` from the drop-down list.

### The DSL Language
The DSL language consists of a header and one or more of evidence blocks. The header (the first line of your comment) defines synthesizer type: `STDLIB` or `ANDROID`. Evidence blocks could be one of the following: 
One the next lines you need to add evidences:
* `API:=<VALUE>` or `TYPE:=<VALUE>` for STDLIB synthesizer,
* `API:=<VALUE>`, `TYPE:=<VALUE>` or `CONTEXT:=<VALUE>` for ANDROID synthesizer.

Syntactic errors are annotated for this DSL, so in case of a error just take a closer look at appearing annotations.

### Examples

Let's consider a few examples.

Delete file:

![Delete file synthesizing](https://s3-eu-west-1.amazonaws.com/public-resources.ml-labs.aws.intellij.net/bayou/gifs/delete_file_test.gif)

Read file: 

![Read file synthesizing](https://s3-eu-west-1.amazonaws.com/public-resources.ml-labs.aws.intellij.net/bayou/gifs/read_file_test.gif)

Clear list:

![Clear list synthesizing](https://s3-eu-west-1.amazonaws.com/public-resources.ml-labs.aws.intellij.net/bayou/gifs/remove_list_test.gif)



