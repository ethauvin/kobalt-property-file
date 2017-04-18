import com.beust.kobalt.*
import com.beust.kobalt.plugin.packaging.*
import com.beust.kobalt.plugin.application.*
import com.beust.kobalt.plugin.kotlin.*
import net.thauvin.erik.kobalt.plugin.propertyfile.*

val bs = buildScript {
    plugins(file("../libs/kobalt-property-file-0.1.0.jar"))
}

val p = project {
    name = "example"
    group = "com.example"
    artifactId = name
    version = "0.1"

    dependencies {
//        compile("com.beust:jcommander:1.68")
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
        file = "version.properties"
    }
}
