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
                return TaskResult()
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

                var result = TaskResult()

                // Process entries
                config.entries.forEach { entry ->
                    if (entry.key.isBlank()) {
                        error("An entry key must be specified.")
                        return TaskResult()
                    } else {
                        with(entry) {
                            if (value == null && default == null && operation != Operations.DELETE) {
                                warn("An entry value or default must be specified: $key")
                            } else if (type == Types.STRING && (operation == Operations.SUBTRACT)) {
                                warn("Subtraction is not supported for String properties: $key")
                            } else if (operation == Operations.DELETE) {
                                p.remove(entry.key)
                            } else {
                                when (type) {
                                    Types.DATE -> result = processDate(p, entry)
                                    Types.INT -> result = processInt(p, entry)
                                    else -> result = processString(p, entry)
                                }
                            }
                        }
                    }

                    // @TODO maybe just warn and keep on going?
                    if (!result.success) {
                        return result
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

    private fun processDate(p: Properties, entry: Entry): TaskResult {
        val cal = Calendar.getInstance()
        val value = currentValue(p.getProperty(entry.key), entry.value, entry.default, entry.operation)

        val fmt = SimpleDateFormat(if (entry.pattern.isBlank()) {
            "yyyy/MM/dd HH:mm"
        } else {
            entry.pattern
        })

        if (value.equals("now", true) || value.isBlank()) {
            cal.time = Date()
        } else {
            try {
                cal.time = fmt.parse(value)
            } catch (pe: ParseException) {
                warn("Date parse exception for: ${entry.key}", pe)
            }
        }

        if (entry.operation != Operations.SET) {
            var offset = 0

            try {
                offset = Integer.parseInt(value)
                if (entry.operation == Operations.SUBTRACT) {
                    offset *= -1
                }
            } catch (nfe: NumberFormatException) {
                warn("Non-integer value for: ${entry.key}")
            }

            cal.add(calendarFields.getOrDefault(entry.unit, Calendar.DATE), offset)
        }

        return TaskResult()
    }

    private fun processInt(p: Properties, entry: Entry): TaskResult {
        var intValue: Int
        try {
            val fmt = DecimalFormat(entry.pattern)
            val value = currentValue(p.getProperty(entry.key), entry.value, entry.default, entry.operation)

            intValue = fmt.parse(if (value.isBlank()) "0" else value).toInt()

            if (entry.operation == Operations.ADD) {
                intValue += 1
            } else if (entry.operation == Operations.SUBTRACT) {
                intValue -= 1
            }

            p.setProperty(entry.key, fmt.format(intValue))
        } catch (nfe: NumberFormatException) {
            warn("Number format exception for: ${entry.key}", nfe)
        } catch (pe: ParseException) {
            warn("Number parsing exception for: ${entry.key}", pe)
        }

        return TaskResult()
    }

    private fun processString(p: Properties, entry: Entry): TaskResult {
        val value = currentValue(p.getProperty(entry.key), entry.value, entry.default, entry.operation)

        if (entry.operation == Operations.SET) {
            p.setProperty(entry.key, value)
        } else if (entry.operation == Operations.ADD) {
            p.setProperty(entry.key, value + p.getProperty(entry.key, ""))
        }

        return TaskResult()
    }

    private fun currentValue(value: String?, newValue: String?, default: String?, operation: Enum<Operations>): String {
        var result: String? = null

        if (operation == Operations.SET) {
            if (newValue != null && default != null) {
                result = newValue
            }

            if (newValue != null && default != null && value != null) {
                result = value
            }

            if (newValue != null && default != null && value == null) {
                result = default
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
        var type: Enum<Types> = Types.STRING,
        var operation: Enum<Operations> = Operations.SET,
        var pattern: String = "",
        var unit: Units = Units.DAY)

@Directive
class PropertyFileConfig {
    var file: String = ""
    var comment: String = ""
    val entries = arrayListOf<Entry>()

    @Suppress("unused")
    fun entry(
            key: String = "",
            value: String? = null,
            default: String? = null,
            type: Enum<Types> = Types.STRING,
            operation: Enum<Operations> = Operations.SET,
            pattern: String = "",
            unit: Units = Units.DAY) {
        if (key.isNotEmpty()) entries.add(Entry(key, value, default, type, operation, pattern, unit))
    }
}

@Suppress("unused")
@Directive
fun Project.propertyfile(init: PropertyFileConfig.() -> Unit) {
    PropertyFileConfig().let { config ->
        config.init()
        (Plugins.findPlugin(PropertyFilePlugin.NAME) as PropertyFilePlugin).addConfiguration(this, config)
    }
}