import com.beust.kobalt.*
import com.beust.kobalt.plugin.packaging.*
import com.beust.kobalt.plugin.application.*
import com.beust.kobalt.plugin.kotlin.*
import net.thauvin.erik.kobalt.plugin.propertyfile.*

// ./kobaltw propertyFile

val bs = buildScript {
    //repos(file("K:/maven/repository"))
    plugins("net.thauvin.erik:kobalt-property-file:")
}

val p = project {
    name = "example"
    group = "com.example"
    artifactId = name
    version = "0.1"

    dependencies {
    }

    dependenciesTest {
        compile("org.testng:testng:6.11")
    }

    assemble {
        jar {
        }
    }

    application {
        mainClass = "com.example.MainKt"
    }

    propertyFile {
        // parameters
        file = "version.properties"
        comment = "##Generated file - do not modify!"

        //failOnWarning = true
        entry(key = "version.fail", value = "a", type = Types.INT)

        // Version properties with patch increment
        entry(key = "version.major", value = "1")
        entry(key = "version.minor", value = "0")
        entry(key = "version.patch", value = "1", default = "-1", type = Types.INT, operation = Operations.ADD)
        entry(key = "version.date", value = "now", type = Types.DATE)
        // ISO8601 date
        entry(key = "version.dateISO", value = "now", type = Types.DATE, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")

        // Set date to now, then add a month
        entry(key = "date.nextMonth", value = "now", type = Types.DATE)
        entry(key = "date.nextMonth", value = "0", type = Types.DATE, unit = Units.MONTH, operation = Operations.ADD)

        // Examples from: https://ant.apache.org/manual/Tasks/propertyfile.html
        entry(key = "akey", value = "avalue")
        entry(key = "adate", type = Types.DATE, value = "now")
        entry(key = "anint", type = Types.INT, default = "0", operation = Operations.ADD)
        entry(key = "formated.int", type = Types.INT, default = "0013", operation = Operations.ADD, pattern = "0000")
        entry(key = "formated.date", type = Types.DATE, value = "now", pattern = "DDD HH:mm")
        entry(key = "formated.date-1", type = Types.DATE, default = "now", pattern = "DDD",
                operation = Operations.SUBTRACT, value = "1")
        entry(key = "formated.tomorrow", type = Types.DATE, default = "now", pattern = "DDD",
                operation = Operations.ADD, value = "1")
        entry(key = "progress", default = "", operation = Operations.ADD, value = ".")
    }
}
