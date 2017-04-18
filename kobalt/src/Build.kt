
import com.beust.kobalt.plugin.packaging.assemble
import com.beust.kobalt.profile
import com.beust.kobalt.project

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
}