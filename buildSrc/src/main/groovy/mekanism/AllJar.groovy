package mekanism

import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.tasks.Jar

import javax.inject.Inject

abstract class AllJar extends Jar {

    @Input
    final MapProperty<String, String> versions
    @InputFile
    final RegularFileProperty pathsToExclude
    @InputFiles
    abstract FileCollection apiOutput
    @InputFiles
    abstract FileCollection mainOutput
    @InputFiles
    FileCollection secondaryModuleOutputs

    AllJar() {
        versions = objectFactory.mapProperty(String, String)
        pathsToExclude = objectFactory.fileProperty()
        secondaryModuleOutputs = objectFactory.fileCollection()
    }

    @Inject
    protected abstract ProjectLayout getProjectLayout()

    @Override
    @TaskAction
    protected void copy() {
        //copy all the files except for ones we are going to include from the merged
        from(apiOutput)

        List<String> baseExcludeData = pathsToExclude.get().getAsFile().text.split("\n")
        from(mainOutput, createExcludeClosure(baseExcludeData, 'crafttweaker_parameter_names.json'))
        from(secondaryModuleOutputs, createExcludeClosure(baseExcludeData, 'logo.png', 'pack.mcmeta'))

        //And finally copy over the generated files
        def generatedDir = projectLayout.buildDirectory.dir('generated')
        from(generatedDir) {
            include('META-INF/neoforge.mods.toml')
            //TODO: Validate versions isn't empty?
            expand(versions.get())
        }
        from(generatedDir) {
            //TODO: Should we have this more directly somehow use the output of the merge module resources task?
            include('META-INF/accesstransformer.cfg', 'META-INF/services/*', 'assets/**', 'data/**')
        }

        //TODO - 1.20.5: Do we need this above the finally bit above? Given technically we have no duplicate so it doesn't have to be after
        super.copy()
    }

    private static Closure<CopySpec> createExcludeClosure(List<String> baseExcludeData, String... extraExclusions) {
        List<String> toExcludeFromAll = new ArrayList<>(baseExcludeData)
        for (String extraExclusion : extraExclusions) {
            toExcludeFromAll.add(extraExclusion)
        }
        return { CopySpec c ->
            c.exclude(toExcludeFromAll)
        }
    }
}