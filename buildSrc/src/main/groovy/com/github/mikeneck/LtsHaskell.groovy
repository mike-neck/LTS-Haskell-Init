/*
 * Copyright 2015 Shinya Mochida
 *
 * Licensed under the Apache License,Version2.0(the"License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,software
 * Distributed under the License is distributed on an"AS IS"BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.mikeneck

import org.gradle.api.Task
import org.gradle.api.tasks.Exec
import org.gradle.model.Model
import org.gradle.model.Mutate
import org.gradle.model.RuleSource
import org.gradle.model.Validate
import org.gradle.model.collection.CollectionBuilder
import org.jsoup.Jsoup

import java.nio.file.Files
import java.nio.file.Paths

import static java.util.stream.Collectors.toSet

class LtsHaskell extends RuleSource {

    static final String STACKAGE_URL = 'https://www.stackage.org/snapshots'

    static final String STACKAGE_SNAPSHOTS_URL = 'https://www.stackage.org/lts-'

    static final String CABAL_CONFIG = 'cabal.config'

    static final def CONFIG_URL = {String version ->
        "http://www.stackage.org/lts/${version}/${CABAL_CONFIG}"
    }

    static final String LTS_HASKELL = 'LTS Haskell'

    static final def TASK_NAME = {String task, String version ->
        def v = version
        "${task}${v}"
    }

    static final    def GROUP = {String latest ->
        "${LTS_HASKELL} (latest: ${latest})"
    }

    static final def WITH_INSTALL_DESCRIPTION = {String version ->
        "Download ${LTS_HASKELL} ${version} configuration and run cabal sandbox init, cabal install."
    }

    static final def NON_INSTALL_DESCRIPTION = {String version ->
        "Download ${LTS_HASKELL} ${version} configuration and run cabal sandbox init."
    }

    @Model
    void ltsHaskell(LtsHaskellConfiguration base) {
        // retrieving directory
        def home = System.getenv("HOME") == null ?
                System.getenv("HOMEPATH").replace('\\', '/') :
                System.getenv("HOME")
        base.dir = "${home}/.ltshs"
    }

    @Validate
    void checkDirectoryExists(LtsHaskellConfiguration base) {
        def path = Paths.get(base.dir)
        if(!Files.exists(path)) {
            throw new IllegalArgumentException($/Directory[${base.dir}] does not exist.
Please run command before run gradle tasks.
mkdir -p ${base.dir}
/$)
        }
    }

    @Mutate
    void createTask(CollectionBuilder<Task> tasks, LtsHaskellConfiguration base) {
        // modify directory name to end with /
        def dir = base.dir.endsWith('/') ?
                base.dir :
                "${base.dir}/"
        def path = Paths.get(dir)
        // filter for haskell lts haskell versions
        def list = Files.list(path)
                .filter{Files.isDirectory(it)}
                .map{it.toAbsolutePath()}
                .map{it.toString()}
                .map{it.replace(dir, '')}
                .collect(toSet())
        // retrieving recent lts haskell versions
        def doc = Jsoup.parse(new URL(STACKAGE_URL), 4000)
        def versions = doc.select('ul.snapshots li a').findAll {
            it.text().contains(LTS_HASKELL)
        }.collect {
            it.attr('href').replace(STACKAGE_SNAPSHOTS_URL, '')
        }
        // retrieving latest lts haskell version
        def latest = versions.sort(false, Comparator.reverseOrder())[0]
        // filtering haskell versions
        versions.findAll {
            !list.contains(it)
        }.each {String v ->
            // create directory
            def mkDir = TASK_NAME('mkdir', v)
            tasks.create(mkDir) {
                def newDir = "${dir}/${v}"
                doLast {
                    Files.createDirectories(Paths.get(newDir))
                }
            }
            // download config
            def download = TASK_NAME('download', v)
            tasks.create(download) {
                def conf = new File("${dir}/${v}/${CABAL_CONFIG}")
                dependsOn mkDir
                doLast {
                    conf.write(new URL(CONFIG_URL(v)).text)
                }
            }
            // cabal update
            def update = TASK_NAME('cabalUpdate', v)
            tasks.create(update, Exec) {
                dependsOn download
                workingDir "${dir}/${v}"
                commandLine 'cabal', 'update'
            }
            // cabal sandbox init
            def sandbox = TASK_NAME('cabalSandboxInit', v)
            tasks.create(sandbox, Exec) {
                dependsOn update
                workingDir "${dir}/${v}"
                commandLine 'cabal', 'sandbox', 'init'
            }
            // cabal install
            def cabal = TASK_NAME('ltsHaskellSetup', v)
            if (base.cabal.size() == 0) {
                tasks.create(cabal) {
                    dependsOn sandbox
                    group = GROUP(latest)
                    description = NON_INSTALL_DESCRIPTION(v)
                }
            } else {
                tasks.create(cabal, Exec) {
                    dependsOn sandbox
                    group = GROUP(latest)
                    description = WITH_INSTALL_DESCRIPTION(v)
                    workingDir "${dir}/${v}"
                    def commands = ['cabal', 'install'] + base.cabal.collect {it.install}
                    commandLine commands
                }
            }
        }
    }
}
