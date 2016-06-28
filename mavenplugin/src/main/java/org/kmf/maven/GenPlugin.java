package org.kmf.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.kevoree.modeling.java2typescript.SourceTranslator;
import org.kmf.generator.Generator;

import java.io.*;
import java.util.Arrays;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, requiresDependencyResolution = ResolutionScope.COMPILE)
public class GenPlugin extends AbstractMojo {

    @Parameter(defaultValue = "${project.build.directory}/generated-sources/kmf")
    private File targetGen;

    @Parameter(defaultValue = "${project.build.directory}/generated-sources/kmfjs")
    private File targetGenJs;

    @Parameter(defaultValue = "${project.basedir}/src")
    private File src;

    @Parameter(defaultValue = "${project.artifactId}")
    private String name;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        //Generate Java
        Generator generator = new Generator();
        try {
            generator.deepScan(src);
        } catch (Exception e) {
            e.printStackTrace();
            throw new MojoExecutionException("Problem during the Scan step");
        }
        generator.generate(name, targetGen);
        project.addCompileSourceRoot(targetGen.getAbsolutePath());
        //Generate TS
        SourceTranslator transpiler = new SourceTranslator(Arrays.asList(targetGen.getAbsolutePath()), targetGenJs.getAbsolutePath(), project.getArtifactId());

        for (Artifact a : project.getArtifacts()) {
            File file = a.getFile();
            if (file != null) {
                if (file.isFile()) {
                    transpiler.addToClasspath(file.getAbsolutePath());
                }
            }
        }

        transpiler.process();
        transpiler.addModuleImport("mwg.d.ts");
        transpiler.generate();

        try {
            FileWriter mwgLib = new FileWriter(new File(targetGenJs, "mwg.d.ts"));
            InputStream mwgLibStream = this.getClass().getClassLoader().getResourceAsStream("mwg.d.ts");
            BufferedReader mwgReader = new BufferedReader(new InputStreamReader(mwgLibStream));
            String line = mwgReader.readLine();
            while (line != null) {
                mwgLib.write(line);
                mwgLib.write("\n");
                line = mwgReader.readLine();
            }
            mwgLib.flush();
            mwgLib.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
