import com.beust.kobalt.buildScript
import com.beust.kobalt.localMaven
import com.beust.kobalt.plugin.packaging.assemble
import com.beust.kobalt.plugin.publish.autoGitTag
import com.beust.kobalt.plugin.publish.bintray
import com.beust.kobalt.profile
import com.beust.kobalt.project
import net.thauvin.erik.kobalt.plugin.versioneye.versionEye
import org.apache.maven.model.Developer
import org.apache.maven.model.License
import org.apache.maven.model.Model
import org.apache.maven.model.Scm

val bs = buildScript {
    repos(localMaven())
    plugins("net.thauvin.erik:kobalt-versioneye:", "net.thauvin.erik:kobalt-maven-local:")
}

val dev by profile()
val kobaltDependency = if (dev) "kobalt" else "kobalt-plugin-api"

val p = project {
    name = "kobalt-property-file"
    group = "net.thauvin.erik"
    artifactId = name
    version = "0.9.1"

    pom = Model().apply {
        description = "PropertyFile plug-in for the Kobalt build system."
        url = "https://github.com/ethauvin/kobalt-property-file"
        licenses = listOf(License().apply {
            name = "BSD 3-Clause"
            url = "https://opensource.org/licenses/BSD-3-Clause"
        })
        scm = Scm().apply {
            url = "https://github.com/ethauvin/kobalt-property-file"
            connection = "https://github.com/ethauvin/kobalt-property-file.git"
            developerConnection = "git@github.com:ethauvin/kobalt-property-file.git"
        }
        developers = listOf(Developer().apply {
            id = "ethauvin"
            name = "Erik C. Thauvin"
            email = "erik@thauvin.net"
        })
    }

    dependencies {
        compileOnly("com.beust:$kobaltDependency:")
        compile("org.jetbrains.kotlin:kotlin-stdlib:1.1.2-4")
    }

    dependenciesTest {
        compile("org.testng:testng:6.11")
    }

    assemble {
        jar {
            fatJar = true
        }

        mavenJars {
            fatJar = true
        }
    }

    autoGitTag {
        enabled = true
        push = false
        message = "Version $version"
    }

    bintray {
        publish = true
        description = "Release version $version"
        vcsTag = version
    }

    versionEye {
        org = "Thauvin"
        team = "Owners"
    }
}