package digital.guimauve.openstackk.generate

import digital.guimauve.openstackk.generate.extensions.GeneratorExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class CoreGeneratePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val fabrikt = project.configurations.create("fabrikt")
        project.dependencies.add("fabrikt", "com.cjbooms:fabrikt:24.0.0")

        val extension = project.extensions.create("openstackkGenerator", GeneratorExtension::class.java)

        project.afterEvaluate {
            val service = extension.service.orNull ?: error("You must configure openstackkGenerator.service")
            val serviceNoDash = service.replace("-", "")

            val workDir = project.layout.buildDirectory.dir("openapi/$service")
            val outputDir = project.layout.buildDirectory.dir("generated/fabrikt/$service")

            // Task: run openstack-codegenerator
            val generateSpec = project.tasks.register(
                "generate${serviceNoDash.capitalize()}Spec"
            ) {
                group = "openstack"
                description = "Generate OpenAPI spec for $service"

                val repoDir = rootProject.layout.buildDirectory.dir("codegenerator/src").get().asFile
                val venvDir = rootProject.layout.buildDirectory.dir("codegenerator/venv").get().asFile
                val serviceRepoDir = rootProject.layout.buildDirectory.dir("codegenerator/$service").get().asFile
                val specOutDir = workDir.get().asFile

                inputs.property("service", service)
                outputs.dir(workDir)

                doLast {
                    val isWin = System.getProperty("os.name").startsWith("Windows")
                    val python = if (isWin) "python" else "python3"

                    if (!repoDir.exists()) project.exec {
                        commandLine("git", "clone", "https://opendev.org/openstack/codegenerator", repoDir.absolutePath)
                    }
                    if (!serviceRepoDir.exists()) project.exec {
                        commandLine(
                            "git", "clone",
                            "https://opendev.org/openstack/${service.repositoryName}",
                            serviceRepoDir.absolutePath
                        )
                    }
                    if (!venvDir.exists()) project.exec {
                        commandLine(python, "-m", "venv", venvDir.absolutePath)
                    }

                    val pip =
                        if (isWin) venvDir.resolve("Scripts/pip").absolutePath
                        else venvDir.resolve("bin/pip").absolutePath
                    val codegenCmd =
                        if (isWin) venvDir.resolve("Scripts/openstack-codegenerator").absolutePath
                        else venvDir.resolve("bin/openstack-codegenerator").absolutePath

                    project.exec {
                        workingDir = repoDir
                        commandLine(pip, "install", "-e", ".")
                    }
                    project.exec {
                        workingDir = repoDir
                        commandLine(
                            pip, "install",
                            "-c", "https://releases.openstack.org/constraints/upper/master",
                            "-r", "../$service/requirements.txt"
                        )
                    }
                    project.exec {
                        workingDir = repoDir
                        commandLine(pip, "install", "-e", "../$service")
                    }

                    // 4. Run codegenerator
                    project.exec {
                        commandLine(
                            codegenCmd,
                            "--target", "openapi-spec",
                            "--service-type", service,
                            "--work-dir", specOutDir.absolutePath
                        )
                    }
                }
            }

            // Task: run Fabrikt
            val fabriktTask = project.tasks.register(
                "generate${serviceNoDash.capitalize()}Client"
            ) {
                group = "openstack"
                description = "Generate Kotlin client for $service"

                val apiDir = workDir.get().asFile
                val generationDir = outputDir.get().asFile.absolutePath
                val schemas = apiDir.walkTopDown()
                    .filter { it.isFile && it.extension in listOf("yaml", "yml") && "." !in it.nameWithoutExtension }
                    .toList()

                inputs.files(schemas)
                outputs.dir(generationDir)

                doLast {
                    schemas.forEach { file ->
                        val version = file.nameWithoutExtension
                        project.javaexec {
                            classpath = fabrikt
                            mainClass.set("com.cjbooms.fabrikt.cli.CodeGen")
                            args(
                                "--output-directory", generationDir,
                                "--base-package", "digital.guimauve.openstackk.$serviceNoDash.$version",
                                "--api-file", file.absolutePath,
                                "--targets", "http_models",
                                "--targets", "client",
                                //"--http-client-target", "ktor",
                                "--http-client-opts", "resilience4j",
                                //"--serialization-library", "kotlinx_serialization",
                                "--src-path", "src/commonMain/kotlin",
                                "--type-overrides", "datetime_as_instant",
                            )
                        }
                    }
                }
            }

            fabriktTask.configure {
                dependsOn(generateSpec)
            }

            /*
            // Hook into build
            project.tasks.withType<KotlinCompile> {
                dependsOn(fabriktTask)
            }

            // Add generated sources
            project.afterEvaluate {
                project.extensions.findByType(JavaPluginExtension::class.java)?.apply {
                    sourceSets.named("main").configure {
                        java.srcDir(outputDir)
                    }
                }
            }
            */
        }
    }

    private val String.repositoryName: String
        get() = when (this) {
            "baremetal" -> "ironic"
            "compute" -> "nova"
            "container-infrastructure" -> "magnum"
            "dns" -> "designate"
            "identity" -> "keystone"
            "image" -> "glance"
            "key-manager" -> "barbican"
            "network" -> "neutron"
            "load-balancer" -> "octavia"
            "placement" -> "placement"
            "shared-file-system" -> "manila"
            "volume" -> "cinder"
            else -> error("Unknown service: $this")
        }

}
