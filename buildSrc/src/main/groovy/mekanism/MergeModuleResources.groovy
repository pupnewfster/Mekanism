package mekanism

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.file.*
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.api.tasks.util.PatternFilterable

import javax.inject.Inject
import java.util.function.BinaryOperator

abstract class MergeModuleResources extends DefaultTask {

    private static Closure<PatternFilterable> atlasFilter = { PatternFilterable pf -> pf.include('**/assets/*/atlases/**/*.json') }
    private static Closure<PatternFilterable> tagFilter = { PatternFilterable pf -> pf.include('**/data/*/tags/**/*.json') }
    static Closure<PatternFilterable> serviceFilter = { PatternFilterable pf -> pf.include('**/META-INF/services/*') }

    @InputFiles
    abstract FileCollection resources
    @InputFiles
    abstract FileCollection annotationGenerated
    @Internal
    final DirectoryProperty generatedDir

    @OutputDirectory
    final Provider<Directory> generatedAssets
    @OutputDirectory
    final Provider<Directory> generatedData
    @OutputDirectory
    final Provider<Directory> generatedMetaInf
    @OutputFile
    final Provider<RegularFile> pathsToExclude

    MergeModuleResources() {
        dependsOn('classes', 'apiClasses', 'additionsClasses', 'defenseClasses', 'generatorsClasses', 'toolsClasses')
        mustRunAfter('clean')

        generatedDir = objectFactory.directoryProperty().convention(projectLayout.buildDirectory.dir('generated'))

        //TODO: Re-evaluate? Ideally we might actually want to track the output files so we know which ones actually got generated
        // instead of just the directory? Or maybe OutputDirectory does that for us
        generatedAssets = generatedDir.dir('assets')
        generatedData = generatedDir.dir('data')
        generatedMetaInf = generatedDir.dir('META-INF')

        pathsToExclude = generatedDir.file('pathsToExclude.txt')
    }

    @Inject
    protected abstract ObjectFactory getObjectFactory()

    @Inject
    protected abstract ProjectLayout getProjectLayout()

    @TaskAction
    protected void merge() {
        //TODO: Test this is no longer a problem with declaring it as an output??
        //Delete the data directory so that we don't accidentally leak bad old data into it

        def toExclude = [
                //This file doesn't exist until compile time
                'META-INF/services/mekanism.common.integration.computer.IComputerMethodRegistry'
        ]

        mergeBasic(toExclude, resources, 'META-INF/accesstransformer.cfg', (text, fileText) -> text + "\n" + fileText)
        mergeModsTOML(toExclude)

        mergeAtlases(toExclude)
        mergeTags(toExclude)
        mergeServices(toExclude, resources + annotationGenerated)

        String text = ""
        for (def path : toExclude) {
            text = text.isEmpty() ? path : text + '\n' + path
        }
        pathsToExclude.get().getAsFile().text = text
    }

    private void mergeModsTOML(List<String> toExclude) {
        mergeBasic(toExclude, resources, 'META-INF/neoforge.mods.toml', (text, fileText) -> {
            //Add all but the first four lines (which are duplicated between the files)
            String[] lines = fileText.split("\n")
            for (int i = 4; i < lines.length; i++) {
                text = text + "\n" + lines[i]
            }
            return text
        })
    }

    private void mergeBasic(List<String> toExclude, FileCollection files, String name, BinaryOperator<String> appender) {
        String text = ""
        for (def file : files.getAsFileTree().matching({ it.include(name) })) {
            text = text.isEmpty() ? file.getText() : appender.apply(text, file.getText())
        }
        writeOutputFile(toExclude, name, text)
    }

    private Map<String, List<String>> getReverseLookup(Closure<PatternFilterable> filter, FileCollection files) {
        def fileTree = objectFactory.fileTree()
        Map<String, List<String>> reverseLookup = [:]
        for (def srcDir : files) {
            int srcDirPathLength = srcDir.getPath().length()
            for (def file : fileTree.setDir(srcDir).matching(filter)) {
                //Add the sourceSet to the reverse lookup
                String path = file.getPath()
                String trimmedPath = path.substring(srcDirPathLength)
                if (!reverseLookup.containsKey(trimmedPath)) {
                    reverseLookup.put(trimmedPath, [])
                }
                reverseLookup.get(trimmedPath).add(path)
            }
        }
        return reverseLookup
    }

    private void mergeAtlases(List<String> toExclude) {
        BinaryOperator<Object> merger = (a, b) -> {
            a.sources += b.sources
            return a
        }
        //Go through the reverse atlas lookup and if there are multiple sourceSets that contain the same atlas
        // properly merge that atlas
        for (def entry : getReverseLookup(atlasFilter, resources).entrySet()) {
            mergeSimpleJson(toExclude, entry.key.substring(1), entry.value, merger)
        }
    }

    private void mergeTags(List<String> toExclude) {
        BinaryOperator<Object> merger = (a, b) -> {
            a.values += b.values
            return a
        }

        //Go through the reverse tag index and if there are multiple sourceSets that contain the same tag
        // properly merge that tag
        for (def entry : getReverseLookup(tagFilter, resources).entrySet()) {
            mergeSimpleJson(toExclude, entry.key.substring(1), entry.value, merger)
        }
    }

    private void mergeSimpleJson(List<String> toExclude, String outputPath, List<String> paths, BinaryOperator<Object> appender) {
        //println(outputPath + " appeared " + paths.size() + " times")
        if (paths.size() < 2) {
            //Skip any there is only a single element for
            return
        }
        Object outputAsJson = null

        for (def file : objectFactory.fileCollection().from(paths)) {
            Object json = new JsonSlurper().parse(file)
            if (outputAsJson == null) {
                outputAsJson = json
            } else {
                outputAsJson = appender.apply(outputAsJson, json)
            }
        }
        if (outputAsJson != null) {
            writeOutputFile(toExclude, outputPath, JsonOutput.toJson(outputAsJson))
        }
    }

    private void mergeServices(List<String> toExclude, FileCollection files) {
        for (def entry : getReverseLookup(serviceFilter, files).entrySet()) {
            mergeSimpleLines(toExclude, entry.key.substring(1), entry.value)
        }
    }

    private void mergeSimpleLines(List<String> toExclude, String outputPath, List<String> paths) {
        //println(outputPath + " appeared " + paths.size() + " times")
        if (paths.size() < 2) {
            //Skip any there is only a single element for
            return
        }
        String text = ""
        for (def file : objectFactory.fileCollection().from(paths)) {
            text = text.isEmpty() ? file.getText() : text + "\n" + file.getText()
        }
        writeOutputFile(toExclude, outputPath, text)
    }

    private void writeOutputFile(List<String> toExclude, String outputPath, String text) {
        toExclude.add(outputPath)
        File outputFile = generatedDir.file(outputPath).get().getAsFile()
        //Make all parent directories needed
        outputFile.getParentFile().mkdirs()
        outputFile.text = text
    }
}