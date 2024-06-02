package mekanism

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.file.FileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.util.PatternFilterable

import java.util.function.BinaryOperator

@Deprecated(forRemoval = true)
class MergeJars {
    static Closure<PatternFilterable> atlasFilter = { PatternFilterable pf -> pf.include('**/assets/*/atlases/**/*.json') }
    static Closure<PatternFilterable> serviceFilter = { PatternFilterable pf -> pf.include('**/META-INF/services/*') }
    static Closure<PatternFilterable> tagFilter = { PatternFilterable pf -> pf.include('**/data/*/tags/**/*.json') }

    static List<String> getGeneralPathsToExclude(ObjectFactory objectFactory, FileCollection resources, FileCollection annotationGenerated) {
        List<String> toExclude = new ArrayList<>()
        toExclude.add('META-INF/neoforge.mods.toml')
        toExclude.add('META-INF/accesstransformer.cfg')
        //This file doesn't exist until compile time
        toExclude.add('META-INF/services/mekanism.common.integration.computer.IComputerMethodRegistry')
        addDuplicates(objectFactory, atlasFilter, resources, toExclude)
        addDuplicates(objectFactory, tagFilter, resources, toExclude)
        //Include things that might be generated as classes in addition to our normal resources
        addDuplicates(objectFactory, serviceFilter, resources + annotationGenerated, toExclude)
        return toExclude
    }

    private static addDuplicates(ObjectFactory objectFactory, Closure<PatternFilterable> filter, FileCollection files, List<String> toExclude) {
        getReverseLookup(objectFactory, filter, files).each { name, paths ->
            if (paths.size() > 1) {
                toExclude.add(name.substring(1))
            }
        }
    }

    //TODO - 1.20.5: Define these as part of a task and as an output to it?
    static void merge(ObjectFactory objectFactory, ProjectLayout projectLayout, FileCollection resources, FileCollection annotationGenerated) {
        //Generate folders, merge the access transformers and neoforge.mods.toml files
        //project.mkdir(projectLayout.buildDirectory.dir('generated/META-INF'))
        mergeBasic(projectLayout, resources, 'META-INF/accesstransformer.cfg', (text, fileText) -> text + "\n" + fileText)
        mergeModsTOML(projectLayout, resources)
        def assets = projectLayout.buildDirectory.dir('generated/assets').get().getAsFile()
        def data = projectLayout.buildDirectory.dir('generated/data').get().getAsFile()
        def services = projectLayout.buildDirectory.dir('generated/META-INF/services').get().getAsFile()
        //Delete the data directory so that we don't accidentally leak bad old data into it
        assets.deleteDir()
        data.deleteDir()
        services.deleteDir()
        //And then recreate the directory so we can put stuff in it
        assets.mkdir()
        data.mkdir()
        services.mkdirs()
        mergeAtlases(objectFactory, projectLayout, resources)
        mergeTags(objectFactory, projectLayout, resources)
        //Include things that might be generated as classes in addition to our normal resources
        mergeServices(objectFactory, projectLayout, resources + annotationGenerated)
    }

    private static void mergeModsTOML(ProjectLayout projectLayout, FileCollection files) {
        mergeBasic(projectLayout, files, 'META-INF/neoforge.mods.toml', (text, fileText) -> {
            //Add all but the first four lines (which are duplicated between the files)
            String[] lines = fileText.split("\n")
            for (int i = 4; i < lines.length; i++) {
                text = text + "\n" + lines[i]
            }
            return text
        })
    }

    private static void mergeBasic(ProjectLayout projectLayout, FileCollection files, String name, BinaryOperator<String> appender) {
        String text = ""
        files.getAsFileTree().matching { PatternFilterable pf ->
            pf.include(name)
        }.each { file ->
            text = text.isEmpty() ? file.getText() : appender.apply(text, file.getText())
        }
        writeOutputFile(projectLayout, '/' + name, text)
    }

    private static Map<String, List<String>> getReverseLookup(ObjectFactory objectFactory, Closure<PatternFilterable> filter, FileCollection files) {
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

    private static void mergeAtlases(ObjectFactory objectFactory, ProjectLayout projectLayout, FileCollection files) {
        Map<String, List<String>> reverseAtlasLookup = getReverseLookup(objectFactory, atlasFilter, files)
        //Go through the reverse atlas lookup and if there are multiple sourceSets that contain the same atlas
        // properly merge that atlas
        reverseAtlasLookup.each { atlas, atlasPaths ->
            mergeSimpleJson(objectFactory, projectLayout, atlas, atlasPaths, (a, b) -> {
                a.sources += b.sources
                return a
            })
        }
    }

    private static void mergeTags(ObjectFactory objectFactory, ProjectLayout projectLayout, FileCollection files) {
        Map<String, List<String>> reverseTags = getReverseLookup(objectFactory, tagFilter, files)
        //Go through the reverse tag index and if there are multiple sourceSets that contain the same tag
        // properly merge that tag
        reverseTags.each { tag, tagPaths ->
            mergeSimpleJson(objectFactory, projectLayout, tag, tagPaths, (a, b) -> {
                a.values += b.values
                return a
            })
        }
    }

    private static void mergeSimpleJson(ObjectFactory objectFactory, ProjectLayout projectLayout, String outputPath, List<String> paths, BinaryOperator<Object> appender) {
        //println(outputPath + " appeared " + paths.size() + " times")
        if (paths.size() < 2) {
            //Skip any there is only a single element for
            return
        }
        Object outputAsJson = null
        for (File file : objectFactory.fileCollection().from(paths)) {
            Object json = new JsonSlurper().parse(file)
            if (outputAsJson == null) {
                outputAsJson = json
            } else {
                outputAsJson = appender.apply(outputAsJson, json)
            }
        }
        if (outputAsJson != null) {
            writeOutputFile(projectLayout, outputPath, JsonOutput.toJson(outputAsJson))
        }
    }

    private static void mergeServices(ObjectFactory objectFactory, ProjectLayout projectLayout, FileCollection files) {
        Map<String, List<String>> reverseServices = getReverseLookup(objectFactory, serviceFilter, files)
        reverseServices.each { tag, tagPaths -> mergeSimpleLines(objectFactory, projectLayout, tag, tagPaths) }
    }

    private static void mergeSimpleLines(ObjectFactory objectFactory, ProjectLayout projectLayout, String outputPath, List<String> paths) {
        //println(outputPath + " appeared " + paths.size() + " times")
        if (paths.size() < 2) {
            //Skip any there is only a single element for
            return
        }
        String text = ""
        for (File file : objectFactory.fileCollection().from(paths)) {
            text = text.isEmpty() ? file.getText() : text + "\n" + file.getText()
        }
        writeOutputFile(projectLayout, outputPath, text)
    }

    private static void writeOutputFile(ProjectLayout projectLayout, String outputPath, String text) {
        File outputFile = projectLayout.buildDirectory.file('generated/' + outputPath).get().getAsFile()
        //Make all parent directories needed
        outputFile.getParentFile().mkdirs()
        outputFile.text = text
    }
}