plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

kotlin {
    dependencies {
        implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.10")
    }
}

gradlePlugin {
    plugins {
        create("core-generate") {
            id = "digital.guimauve.openstackk.generate"
            implementationClass = "digital.guimauve.openstackk.generate.CoreGeneratePlugin"
        }
    }
}
