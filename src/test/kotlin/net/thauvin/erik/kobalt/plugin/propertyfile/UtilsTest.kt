/*
 * UtilsTest.kt
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

import org.testng.Assert
import org.testng.annotations.Test
import java.text.SimpleDateFormat
import java.util.*

@Test
class UtilsTest {
    val p = Properties()

    @Test
    fun currentValueTest() {
        var prev : String?
        var value: String?
        var default: String? = null
        var operation = Operations.SET


        // If only value is specified, the property is set to it regardless of its previous value.
        prev = "previous"
        value = "value"
        Assert.assertEquals(Utils.currentValue(prev, value, default, operation), value,
                "currentValue($prev,$value,$default,$operation)")

        // If only default is specified and the property previously existed, it is unchanged.
        prev = "previous"
        value = null
        default = "default"
        Assert.assertEquals(Utils.currentValue(prev, value, default, operation), prev,
                "currentValue($prev,$value,$default,$operation)")

        // If only default is specified and the property did not exist, the property is set to default.
        prev = null
        value = null
        default = "default"
        Assert.assertEquals(Utils.currentValue(prev, value, default, operation), default,
                "currentValue($prev,$value,$default,$operation)")

        // If value and default are both specified and the property previously existed, the property is set to value.
        prev = "previous"
        value ="value"
        default = "default"
        Assert.assertEquals(Utils.currentValue(prev, value, default, operation), value,
                "currentValue($prev,$value,$default,$operation)")

        // If value and default are both specified and the property did not exist, the property is set to default.
        prev = null
        value = "value"
        default ="default"
        Assert.assertEquals(Utils.currentValue(prev, value, default, operation), default,
                "currentValue($prev,$value,$default,$operation)")

        // ADD
        operation = Operations.ADD

        prev = null
        value = "value"
        default = "default"
        Assert.assertEquals(Utils.currentValue(prev, value, default, operation), default,
                "currentValue($prev,$value,$default,$operation)")

        prev = "prev"
        value = "value"
        default = null
        Assert.assertEquals(Utils.currentValue(prev, value, default, operation), prev,
                "currentValue($prev,$value,$default,$operation)")

        prev = null
        value = "value"
        default = null
        Assert.assertEquals(Utils.currentValue(prev, value, default, operation), "",
                "currentValue($prev,$value,$default,$operation)")

        prev = null
        value = "value"
        default = "default"
        Assert.assertEquals(Utils.currentValue(prev, value, default, operation), default,
                "currentValue($prev,$value,$default,$operation)")

        prev = null
        value = null
        default = null
        Assert.assertEquals(Utils.currentValue(prev, value, default, operation), "",
                "currentValue($prev,$value,$default,$operation)")
    }

    @Test
    fun processStringTest() {
        val entry = Entry()

        entry.key = "version.major"
        entry.value = "1"

        Utils.processString(p, entry)
        Assert.assertEquals(entry.value, p.getProperty(entry.key), "processString(${entry.key}, ${entry.value})")

        entry.key = "version.minor"
        entry.value = "0"

        Utils.processString(p, entry)
        Assert.assertEquals(entry.value, p.getProperty(entry.key), "processString(${entry.key}, ${entry.value})")
    }

    @Test
    fun processIntTest() {
        val entry = Entry()
        entry.type = Types.INT

        entry.key = "version.patch"

        entry.value = "a"
        Assert.assertFalse(Utils.processInt(p, entry), "parsetInt(${entry.key}, a)")

        // ADD
        entry.operation = Operations.ADD

        entry.value = "1"
        entry.default = "-1"
        Utils.processInt(p, entry)
        Assert.assertEquals("0", p.getProperty(entry.key), "processInt(${entry.key}, 0)")

        entry.key = "anint"
        entry.value = null
        entry.default = "0"
        Utils.processInt(p, entry)
        Assert.assertEquals("1", p.getProperty(entry.key), "processInt(${entry.key}, 1)")
        Utils.processInt(p, entry)
        Assert.assertEquals("2", p.getProperty(entry.key), "processInt(${entry.key}, 2)")

        entry.key = "formated.int"
        entry.value = null
        entry.default = "0013"
        entry.pattern = "0000"
        Utils.processInt(p, entry)
        Assert.assertEquals("0014", p.getProperty(entry.key), "processInt(${entry.key}, 0014)")
        Utils.processInt(p, entry)
        Assert.assertEquals("0015", p.getProperty(entry.key), "processInt(${entry.key}, 0015)")

        entry.key = "formated.int"
        entry.value = "2"
        entry.default = "0013"
        entry.pattern = "0000"
        Utils.processInt(p, entry)
        Assert.assertEquals("0017", p.getProperty(entry.key), "processInt(${entry.key}, 0017)")

        // SUBTRACT
        entry.operation = Operations.SUBTRACT
        entry.value = null
        entry.default = "0013"
        entry.pattern = "0000"
        Utils.processInt(p, entry)
        Assert.assertEquals("0016", p.getProperty(entry.key), "processInt(${entry.key}, 0016)")

    }

    @Test
    fun processDateTest() {
        val entry = Entry()
        entry.type = Types.DATE
        entry.pattern = "D"
        entry.key = "adate"

        val day = SimpleDateFormat(entry.pattern).format(Date()).toInt()

        entry.value = "a"
        Assert.assertFalse(Utils.processDate(p, entry), "processDate(${entry.key}, a)")

        entry.value = "99"
        Utils.processDate(p, entry)
        Assert.assertEquals("99", p.getProperty(entry.key), "processDate(${entry.key}, 99)")

        entry.value = "now"
        Utils.processDate(p, entry)
        Assert.assertEquals("$day", p.getProperty(entry.key), "processDate(${entry.key}, now)")

        // ADD
        entry.operation = Operations.ADD

        entry.value = "1"
        Utils.processDate(p, entry)
        Assert.assertEquals("${day+1}", p.getProperty(entry.key), "processDate(${entry.key}, now+1)")

        entry.value = "2"
        Utils.processDate(p, entry)
        Assert.assertEquals("${day+3}", p.getProperty(entry.key), "processDate(${entry.key}, now+3)")

        // SUBTRACT
        entry.operation = Operations.SUBTRACT
        entry.value = "3"
        Utils.processDate(p, entry)
        Assert.assertEquals("$day", p.getProperty(entry.key), "processDate(${entry.key}, now-3)")

        entry.operation = Operations.SUBTRACT
        entry.value = "2"
        Utils.processDate(p, entry)
        Assert.assertEquals("${day-2}", p.getProperty(entry.key), "processDate(${entry.key}, now-2)")
    }
}