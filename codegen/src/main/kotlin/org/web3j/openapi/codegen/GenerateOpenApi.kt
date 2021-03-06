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
package org.web3j.openapi.codegen

import org.gradle.tooling.GradleConnectionException
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ResultHandler
import org.web3j.openapi.codegen.config.GeneratorConfiguration
import org.web3j.openapi.codegen.coregen.CoreGenerator
import org.web3j.openapi.codegen.gradlegen.GradleResourceCopy.copyProjectResources
import org.web3j.openapi.codegen.servergen.ServerGenerator
import org.web3j.openapi.codegen.utils.TemplateUtils.generateFromTemplate
import org.web3j.openapi.codegen.utils.TemplateUtils.mustacheTemplate
import org.web3j.openapi.codegen.web3jCodegenStuff.SolidityFunctionWrapperGenerator
import java.io.File
import java.nio.file.Paths

class GenerateOpenApi(
    private val configuration: GeneratorConfiguration
) {

    private val SWAGGERUI_GENERATION_TASK = "completeSwaggerUiGeneration"

    fun generate() {
        println("Generating Web3j-OpenAPI project ... Files written to ${configuration.outputDir}")
        if (configuration.withGradleResources) generateGradleResources()
        generateCore()
        generateServer()
        if (configuration.withWrappers) generateWrappers()
        if (configuration.withSwaggerUi) generateSwaggerUI()
    }

    @Deprecated("renaming the function name", ReplaceWith("generate"))
    fun generateAll() = generate()

    fun generateServer() {
        ServerGenerator(configuration).generate()
    }

    fun generateCore() {
        CoreGenerator(configuration).generate()
    }

    fun generateGradleResources() {
        copyProjectResources(File(configuration.outputDir).apply { mkdirs() })
        generateFromTemplate(
            outputDir = configuration.outputDir,
            context = mapOf("rootProjectName" to configuration.rootProjectName),
            template = mustacheTemplate("settings.gradle.mustache"),
            name = "settings.gradle"
        )
    }

    fun generateWrappers() {
        configuration.contracts.forEach {
            SolidityFunctionWrapperGenerator(
                abiFile = it.abiFile,
                binFile = it.binFile,
                contractName = it.abiFile.name.removeSuffix(".abi"),
                basePackageName = "${configuration.packageName}.wrappers",
                destinationDir = File(if (configuration.withGradleResources)
                    Paths.get(
                        configuration.outputDir,
                        "server",
                        "src",
                        "main",
                        "java"
                    ).toString()
                else
                    Paths.get(
                        configuration.outputDir
                    ).toString()
                ),
                useJavaPrimitiveTypes = true,
                useJavaNativeTypes = true,
                addressLength = configuration.addressLength
            ).generate()
        }
    }

    fun generateSwaggerUI() {
        println("Generating SwaggerUI ...")
        GradleConnector.newConnector()
            .useBuildDistribution()
            .forProjectDirectory(File(configuration.outputDir))
            .connect()
            .apply {
                newBuild()
                    .forTasks(SWAGGERUI_GENERATION_TASK)
                    .run(object : ResultHandler<Void> {
                        override fun onFailure(failure: GradleConnectionException) {
                            throw failure
                        }

                        override fun onComplete(result: Void?) {
                            print(" Done.\n")
                        }
                    })
                close()
            }
    }
}
