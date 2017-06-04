/*
 * PropertyFileUtils.kt
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

import com.beust.kobalt.misc.warn
import java.text.DecimalFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class PropertyFileUtils private constructor() {
    companion object {
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

        fun processDate(p: Properties, entry: Entry): Boolean {
            var success = true
            val cal = Calendar.getInstance()
            val value = PropertyFileUtils.currentValue(p.getProperty(entry.key), entry.value, entry.default, entry.operation)

            val fmt = SimpleDateFormat(if (entry.pattern.isBlank()) "yyyy-MM-dd HH:mm" else entry.pattern)

            if (value.equals("now", true) || value.isBlank()) {
                cal.time = Date()
            } else {
                try {
                    cal.time = fmt.parse(value)
                } catch (pe: ParseException) {
                    warn("Date parse exception for: ${entry.key} --> ${pe.message}", pe)
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
                    warn("Non-integer value for: ${entry.key} --> ${nfe.message}", nfe)
                    success = false
                }

                cal.add(calendarFields.getOrDefault(entry.unit, Calendar.DATE), offset)
            }

            p.setProperty(entry.key, fmt.format(cal.time))

            return success
        }

        fun processInt(p: Properties, entry: Entry): Boolean {
            var success = true
            var intValue: Int
            try {
                val fmt = DecimalFormat(entry.pattern)
                val value = PropertyFileUtils.currentValue(p.getProperty(entry.key), entry.value, entry.default, entry.operation)

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
                warn("Number format exception for: ${entry.key} --> ${nfe.message}", nfe)
                success = false
            } catch (pe: ParseException) {
                warn("Number parsing exception for: ${entry.key} --> ${pe.message}", pe)
                success = false
            }

            return success
        }

        fun processString(p: Properties, entry: Entry): Boolean {
            val value = PropertyFileUtils.currentValue(p.getProperty(entry.key), entry.value, entry.default, entry.operation)

            if (entry.operation == Operations.SET) {
                p.setProperty(entry.key, value)
            } else if (entry.operation == Operations.ADD) {
                if (entry.value != null) {
                    p.setProperty(entry.key, "$value${entry.value}")
                }
            }

            return true
        }

        fun currentValue(value: String?, newValue: String?, default: String?, operation: Operations): String {
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
}