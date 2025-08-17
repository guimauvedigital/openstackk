package digital.guimauve.openstackk.generate.extensions

import org.gradle.api.provider.Property

interface GeneratorExtension {

    val service: Property<String>

}
