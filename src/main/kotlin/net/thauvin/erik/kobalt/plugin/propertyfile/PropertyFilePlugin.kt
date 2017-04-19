/*
 * PropertyFilePlugin.kt
 *
 * Copyright (c) 2017, Erik C. Thauvin (erik@thauvin.net)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 *   Neither the name of this project nor the names of its contributors may be
 *   used to endorse or promote products derived from this software without
 *   specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.thauvin.erik.kobalt.plugin.propertyfile

import com.beust.kobalt.Plugins
import com.beust.kobalt.TaskResult
import com.beust.kobalt.api.*
import com.beust.kobalt.api.annotation.Directive
import com.beust.kobalt.api.annotation.Task
import com.beust.kobalt.misc.error
import com.beust.kobalt.misc.warn
import com.google.inject.Inject
import com.google.inject.Singleton
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

@Singleton
class PropertyFilePlugin @Inject constructor(val configActor: ConfigActor<PropertyFileConfig>,
                                             val taskContributor: TaskContributor) :
        BasePlugin(), ITaskContributor, IConfigActor<PropertyFileConfig> by configActor {

    // ITaskContributor
    override fun tasksFor(project: Project, context: KobaltContext): List<DynamicTask> {
        return emptyList()
    }

    companion object {
        const val NAME: String = "PropertyFile"
    }

    override val name = NAME

    override fun apply(project: Project, context: KobaltContext) {
        super.apply(project, context)
        taskContributor.addVariantTasks(this, project, context, NAME, group = "other",
                runTask = { propertyFile(project) })
    }

    @Task(name = "propertyFile", description = "Edit a property file.")
    fun propertyFile(project: Project): TaskResult {
        configurationFor(project)?.let { config ->
            if (config.file.isBlank()) {
                error("Please specify a property file name.")
                return TaskResult(!config.failOnWarning)
            } else {
                // Load properties
                val p = Properties()
                Paths.get(config.file).let { path ->
                    if (path.toFile().exists()) {
                        Files.newInputStream(path).use {
                            p.load(it)
                        }
                    }
                }

                var success = true

                // Process entries
                config.entries.forEach { entry ->
                    if (entry.key.isBlank()) {
                        error("An entry key must be specified.")
                        success = false
                    } else {
                        with(entry) {
                            if (value == null && default == null && operation != Operations.DELETE) {
                                warn("An entry value or default must be specified: $key")
                                success = false
                            } else if (type == Types.STRING && (operation == Operations.SUBTRACT)) {
                                warn("Subtraction is not supported for String properties: $key")
                                success = false
                            } else if (operation == Operations.DELETE) {
                                p.remove(entry.key)
                            } else {
                                when (type) {
                                    Types.DATE -> success = Utils.processDate(p, entry)
                                    Types.INT -> success = Utils.processInt(p, entry)
                                    else -> success = Utils.processString(p, entry)
                                }
                            }
                        }
                    }

                    if (config.failOnWarning && !success) {
                        return TaskResult(success)
                    }
                }

                // Save properties
                FileOutputStream(config.file).use { output ->
                    p.store(output, config.comment)
                }
            }
        }

        return TaskResult()
    }
}

enum class Types {
    DATE, INT, STRING
}

enum class Operations {
    ADD, DELETE, SET, SUBTRACT
}

enum class Units {
    MILLISECOND, SECOND, MINUTE, HOUR, DAY, WEEK, MONTH, YEAR
}

data class Entry(
        var key: String = "",
        var value: String? = null,
        var default: String? = null,
        var type: Types = Types.STRING,
        var operation: Operations = Operations.SET,
        var pattern: String = "",
        var unit: Units = Units.DAY)

@Directive
class PropertyFileConfig {
    var file: String = ""
    var comment: String? = null
    var failOnWarning: Boolean = false
    val entries = arrayListOf<Entry>()

    @Suppress("unused")
    fun entry(
            key: String = "",
            value: String? = null,
            default: String? = null,
            type: Types = Types.STRING,
            operation: Operations = Operations.SET,
            pattern: String = "",
            unit: Units = Units.DAY) {
        entries.add(Entry(key, value, default, type, operation, pattern, unit))
    }
}

@Suppress("unused")
@Directive
fun Project.propertyFile(init: PropertyFileConfig.() -> Unit) {
    PropertyFileConfig().let { config ->
        config.init()
        (Plugins.findPlugin(PropertyFilePlugin.NAME) as PropertyFilePlugin).addConfiguration(this, config)
    }
}