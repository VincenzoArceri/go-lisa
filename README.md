# GoLiSA
[![GitHub license](https://img.shields.io/github/license/UniVE-SSV/go-lisa)](https://github.com/UniVE-SSV/go-lisa/blob/master/LICENSE)
![GitHub Workflow Status](https://img.shields.io/github/workflow/status/UniVE-SSV/go-lisa/Gradle%20Build%20(master%20branch))
![GitHub last commit](https://img.shields.io/github/last-commit/UniVE-SSV/go-lisa)
![GitHub issues](https://img.shields.io/github/issues-raw/UniVE-SSV/go-lisa)

GoLiSA: A Parametric Static Analyzer for Go(SAC 2023)

## How to use GoLiSA
The main class is [GoLiSA](go-lisa/src/main/java/it/unive/golisa/GoLiSA.java) and it expects four paramaters:
- `-i path`: the Go file to be analyzed
- `-o path`: the output directory
- `-box, -oct, -poly`: the numerical analysis
- `-ucci`: the taint analysis with the untrusted cross-contract invocation semantic checker
- `over-under-flow-check`: the underflow semantic checker

### Example of command line

`-i sm.go -o output -box -over-under-flow-check`

## How to build the project ##
GoLiSA comes as a Gradle 6.0 project. For development with Eclipse, please install the [Gradle IDE Pack](https://marketplace.eclipse.org/content/gradle-ide-pack) plugin from the Eclipse marketplace, and make sure to import the project into the workspace as a Gradle project.
