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
import java.text.*
import java.util.*

@Singleton
class PropertyFilePlugin @Inject constructor(val configActor: ConfigActor<PropertyFileConfig>,
                                             val taskContributor: TaskContributor) :
        BasePlugin(), ITaskContributor, IConfigActor<PropertyFileConfig> by configActor {
    private val calendarFields = mapOf(
            Units.MILLISECOND to Calendar.MILLISECOND,
            Units.SECOND to Calendar.SECOND,
            Units.MINUTE to Calendar.MINUTE,
            Units.HOUR to Calendar.HOUR_OF_DAY,
            Units.DAY to Calendar.DATE,
            Units.WEEK to Calendar.WEEK_OF_YEAR,
            Units.MONTH to Calendar.MONTH,
            Units.YEAR to Calendar.YEAR
    )

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
                                    Types.DATE -> success = processDate(p, entry)
                                    Types.INT -> success = processInt(p, entry)
                                    else -> success = processString(p, entry)
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

    private fun processDate(p: Properties, entry: Entry): Boolean {
        var success = true
        val cal = Calendar.getInstance()
        val value = currentValue(p.getProperty(entry.key), entry.value, entry.default, entry.operation)

        val fmt = SimpleDateFormat(if (entry.pattern.isBlank()) "yyyy-MM-dd HH:mm" else entry.pattern)

        if (value.equals("now", true) || value.isBlank()) {
            cal.time = Date()
        } else {
            try {
                cal.time = fmt.parse(value)
            } catch (pe: ParseException) {
                warn("Date parse exception for: ${entry.key}", pe)
                success = false
            }
        }

        if (entry.operation != Operations.SET) {
            var offset = 0

            try {
                offset = entry.value!!.toInt()
                if (entry.operation == Operations.SUBTRACT) {
                    offset *= -1
                }
            } catch (nfe: NumberFormatException) {
                warn("Non-integer value for: ${entry.key}")
                success = false
            }

            cal.add(calendarFields.getOrDefault(entry.unit, Calendar.DATE), offset)
        }

        p.setProperty(entry.key, fmt.format(cal.time))

        return success
    }

    private fun processInt(p: Properties, entry: Entry): Boolean {
        var success = true
        var intValue: Int
        try {
            val fmt = DecimalFormat(entry.pattern)
            val value = currentValue(p.getProperty(entry.key), entry.value, entry.default, entry.operation)

            intValue = fmt.parse(if (value.isBlank()) "0" else value).toInt()

            if (entry.operation != Operations.SET) {
                var opValue = 1
                if (entry.value != null) {
                    opValue = fmt.parse(entry.value).toInt()
                }
                if (entry.operation == Operations.ADD) {
                    intValue += opValue
                } else if (entry.operation == Operations.SUBTRACT) {
                    intValue -= opValue
                }
            }

            p.setProperty(entry.key, fmt.format(intValue))
        } catch (nfe: NumberFormatException) {
            warn("Number format exception for: ${entry.key}", nfe)
            success = false
        } catch (pe: ParseException) {
            warn("Number parsing exception for: ${entry.key}", pe)
            success = false
        }

        return success
    }

    private fun processString(p: Properties, entry: Entry): Boolean {
        val value = currentValue(p.getProperty(entry.key), entry.value, entry.default, entry.operation)

        if (entry.operation == Operations.SET) {
            p.setProperty(entry.key, value)
        } else if (entry.operation == Operations.ADD) {
            if (entry.value != null) {
                p.setProperty(entry.key, "$value${entry.value}")
            }
        }

        return true
    }

    private fun currentValue(value: String?, newValue: String?, default: String?, operation: Operations): String {
        var result: String? = null

        if (operation == Operations.SET) {
            if (newValue != null && default == null) {
                result = newValue
            }
            if (default != null) {
                if (newValue == null && value != null) {
                    result = value
                }

                if (newValue == null && value == null) {
                    result = default
                }

                if (newValue != null && value != null) {
                    result = newValue
                }

                if (newValue != null && value == null) {
                    result = default
                }
            }
        } else {
            result = value ?: default
        }

        if (result == null) {
            result = ""
        }

        return result
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
        if (key.isNotEmpty()) entries.add(Entry(key, value, default, type, operation, pattern, unit))
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