
import com.beust.kobalt.plugin.application.application
import com.beust.kobalt.plugin.packaging.assemble
import com.beust.kobalt.profile
import com.beust.kobalt.project
//import net.thauvin.erik.kobalt.plugin.propertyfile.*

val semver = "0.1.0"

val dev by profile()
val kobaltDependency = if (dev) "kobalt" else "kobalt-plugin-api"

val p = project {
    name = "kobalt-property-file"
    group = "net.thauvin.erik"
    artifactId = name
    version = semver

    dependencies {
        compile("com.beust:$kobaltDependency:")
    }

    dependenciesTest {
        //compile("org.testng:testng:6.11")
    }

    assemble {
        jar {
            fatJar = true
        }

        mavenJars {}
    }

    application {
        mainClass = "com.example.MainKt"
    }
}

val example = project(p) {
    directory = "example"
    name = "example"
    version = "0.1.0"

    assemble {
        jar {
        }
    }

    application {
        mainClass = "com.example.MainKt"
    }

//    propertyFile {
//        file = "version.properties"
//    }
}