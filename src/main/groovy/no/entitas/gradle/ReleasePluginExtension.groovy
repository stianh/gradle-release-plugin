/*
 * Copyright 2011- the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package no.entitas.gradle

/**
 * Extension object for the release plugin with options that the user can set
 *
 * @author Stig Kleppe-Jorgensen, 2012.01.12
 */
class ReleasePluginExtension {
    def boolean failOnSnapshotDependencies = true
    def versionStrategy = { currentVersion -> new BigDecimal(currentVersion).add(BigDecimal.ONE).toPlainString() }
    def startVersion = { currentBranch -> "1" }
}
