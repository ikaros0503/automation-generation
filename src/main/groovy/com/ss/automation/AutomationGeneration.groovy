package com.ss.automation

import groovy.json.JsonSlurper
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.GradleBuild

class AutomationGeneration implements Plugin<Project> {

    void apply(Project project) {

        def file = project.file('../build-config.json')
        if (!file.exists()) {
            println "Count not find config file"
            return
        }

        def configJson = new JsonSlurper().parseText(file.text)

        project.getTasks().create(name: 'generateApp', type: GradleBuild) {
            tasks = ['clean', 'assembleAutomation', 'bundleAutomation']
        }


        def configSigning = configJson.signingConfig

        project.android.signingConfigs {
            automation {
                storeFile project.file(configSigning.storeFile)
                storePassword configSigning.storePassword
                keyAlias = configSigning.keyAlias
                keyPassword configSigning.keyPassword
            }
        }

        def automationSigningConfig = project.android.signingConfigs.automation

        project.android.buildTypes {
            automation {

                /**
                 * Generate resource files
                 */
                def automationDir = project.file('./src/automation/res/values/')
                if (!automationDir.exists()) {
                    automationDir.mkdirs()
                }
                def valueGeneratedContent = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                        "<resources>\n"
                configJson.resourceValues.each { field ->
                    valueGeneratedContent += "<${field.type} name=\"${field.name}\">${field.value}</${field.type}>\n"
                }
                valueGeneratedContent += "</resources>"
                new File(automationDir, 'data.xml').text = valueGeneratedContent

                /**
                 * Change default config files
                 */
                configJson.defaultConfig.each { key, value ->
                    project.android.defaultConfig[key] = value
                }

                /**
                 * Add build config fields
                 */
                configJson.buildConfigFields.each { field ->
                    buildConfigField "${field.type}", "${field.name}", "${field.value}"
                }

                minifyEnabled true

                signingConfig automationSigningConfig

                matchingFallbacks = ['release']

                project.copy {
                    from '../resources/.'
                    into './src/automation/res'
                }
            }
        }
    }
}
