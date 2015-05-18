LTS-Haskell-Init
===

LTS-Haskell-Init is a gradle tasks for set up LTS Haskell template.

Requirements
===

* Java 1.8
* cabal

Usage
===

* clone this repository or download zip.
* modify `build.gradle` as you like
* `./gradlew tasks` shows tasks named `ltsHaskellSetup{version}` which set up LTS Haskell template.
* The list of versions is retrieved from stackage.org.
* select LTS Haskell version and run its task.

```
$ ./gradlew tasks
------------------------------------------------------------
All tasks runnable from root project
------------------------------------------------------------

LTS Haskell (latest: 2.9) tasks
-------------------------------
cabalSandboxInit2.2 - Download LTS Haskell 2.2 configuration and run init command(cabal sandbox init).
cabalSandboxInit2.3 - Download LTS Haskell 2.3 configuration and run init command(cabal sandbox init).
cabalSandboxInit2.4 - Download LTS Haskell 2.4 configuration and run init command(cabal sandbox init).
cabalSandboxInit2.5 - Download LTS Haskell 2.5 configuration and run init command(cabal sandbox init).
cabalSandboxInit2.7 - Download LTS Haskell 2.7 configuration and run init command(cabal sandbox init).
cabalSandboxInit2.8 - Download LTS Haskell 2.7 configuration and run init command(cabal sandbox init).
cabalSandboxInit2.9 - Download LTS Haskell 2.7 configuration and run init command(cabal sandbox init).

$ ./gradlew cabalSandboxInit2.9
:mkdir2.8
:download2.8
:cabalSandboxInit2.8
Writing a default package environment file to
/Users/mike/.ltshs/2.8/cabal.sandbox.config
Creating a new sandbox at /Users/mike/.ltshs/2.8/.cabal-sandbox
:cabalSetup2.8
Resolving dependencies...
Notice: installing into a sandbox located at
/Users/mike/.ltshs/2.8/.cabal-sandbox
Configuring ansi-terminal-0.6.2.1...
...
Building ghc-mod-5.2.1.2...
Installed ghc-mod-5.2.1.2

BUILD SUCCESSFUL

Total time: 9 mins 8.758 secs
```

Configuration
===

You can configure several settings.

property|meaning
:--|:--
`model.ltsHaskell.dir`|The directory where LTS Haskell is set up(default is `$HOME/.ltshs`). The directory must be existing.
`model.ltsHaskell.cabal.create.install`|A cabal package you need. This parameter can be configured multiple times.

example

```groovy
model {
    ltsHaskell {
        // cabal sandbox init will be executed on /path/to/lts/haskell/version
        dir = '/path/to/lts/haskell'
        // happy and ghc-mod will be installed to the directory.
        cabal.create {
            install = 'happy'
        }
        cabal.create {
            install = 'ghc-mod'
        }
    }
}
```
