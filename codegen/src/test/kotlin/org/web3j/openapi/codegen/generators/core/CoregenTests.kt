/*
 * Copyright 2020 Web3 Labs Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.web3j.openapi.codegen.generators.core

import assertk.assertThat
import assertk.assertions.isSuccess
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.io.TempDir
import org.web3j.openapi.codegen.config.GeneratorConfiguration
import org.web3j.openapi.codegen.coregen.CoreGenerator
import org.web3j.openapi.codegen.coregen.subgenerators.CoreApiGenerator
import org.web3j.openapi.codegen.utils.GeneratorUtils
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Paths

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class CoregenTests {

    @TempDir
    lateinit var tempFolder: File

    private val contractsFolder = Paths.get(
        "src",
        "test",
        "resources",
        "contracts").toFile()

    private val contractsConfiguration = GeneratorUtils.loadContractConfigurations(
        listOf(contractsFolder), listOf(contractsFolder)
    )
    init {
        if (contractsConfiguration.isEmpty()) throw FileNotFoundException("No test contracts found!")
    }

    @Test
    @Order(1)
    fun `Core lifecycle and resources test`() {
        contractsConfiguration.forEach { contractConfiguration ->
            assertThat {
                CoreApiGenerator(
                    "com.test",
                    tempFolder.canonicalPath,
                    contractConfiguration.contractDetails,
                    true
                ).generate()
            }.isSuccess()
        }
    }

    @Test
    @Order(2)
    fun `Core module test`() {
        assertThat {
            CoreGenerator(
                GeneratorConfiguration(
                    "testApp",
                    "com.test",
                    tempFolder.canonicalPath,
                    contractsConfiguration,
                    20,
                    "test",
                    "0.1.0-SNAPSHOT"
                )
            ).generate()
        }.isSuccess()
    }
}
