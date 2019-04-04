package com.digitalascent.gradle.project;

import com.jfrog.bintray.gradle.BintrayExtension;
import nebula.plugin.resolutionrules.NebulaResolutionRulesExtension;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.tasks.TaskCollection;
import org.gradle.api.tasks.compile.CompileOptions;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.external.javadoc.JavadocMemberLevel;
import org.gradle.external.javadoc.StandardJavadocDocletOptions;
import pl.allegro.tech.build.axion.release.domain.VersionConfig;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class ProjectPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
//        project.getPluginManager().apply(ErrorPronePlugin.class);
        project.getPluginManager().apply(nebula.plugin.responsible.NebulaResponsiblePlugin.class);
        project.getPluginManager().apply(nebula.plugin.bintray.BintrayPlugin.class);
        project.getPluginManager().apply(nebula.plugin.resolutionrules.ResolutionRulesPlugin.class);
        project.getPluginManager().apply(nebula.plugin.publishing.maven.license.MavenApacheLicensePlugin.class);
        project.getPluginManager().apply(pl.allegro.tech.build.axion.release.ReleasePlugin.class);

        registerPublishArtifactsTask(project);
//        configureNebulaResolutionRules(project);
        configureJavaCompile(project);
        configureJavaDoc(project);
        configureBintray(project);
        configureAxiom(project);
    }

    private void configureJavaDoc(Project project) {
        final Action<Javadoc> action = task -> {

            task.setDescription("Generates project-level javadoc for use in -javadoc jar");
            StandardJavadocDocletOptions options = (StandardJavadocDocletOptions) task.getOptions();

            options.setMemberLevel(JavadocMemberLevel.PROTECTED);
            options.setAuthor(false);
            options.setHeader(project.getName());

            options.links("http://docs.oracle.com/javase/8/docs/api/","http://docs.oracle.com/javaee/7/api/");

            options.addStringOption("Xdoclint:none", "-quiet");

            // Suppress warnings due to cross-module @see and @link references.
            project.getLogging().captureStandardError(LogLevel.INFO);
            project.getLogging().captureStandardOutput(LogLevel.INFO);

        };

        final TaskCollection<Javadoc> javadocTasks = project.getTasks().withType(Javadoc.class);
        javadocTasks.all(action);
    }

    private void configureAxiom(Project project) {
        VersionConfig versionConfig = project.getExtensions().getByType(VersionConfig.class);

        versionConfig.setLocalOnly(true);
        versionConfig.getTag().setPrefix("release");
        versionConfig.getTag().setVersionSeparator("/");
    }

    private void configureBintray(Project project) {
        project.afterEvaluate(o -> {
            BintrayExtension bintray = project.getExtensions().getByType(BintrayExtension.class);

            BintrayExtension.PackageConfig packageConfig = bintray.getPkg();

            packageConfig.setUserOrg("");
            packageConfig.setLabels("");
            packageConfig.setPublicDownloadNumbers(false);
            packageConfig.setRepo(project.getGroup().toString());
            packageConfig.getVersion().setName(project.getVersion().toString());
            packageConfig.getVersion().setVcsTag(project.getVersion().toString());
            packageConfig.setLicenses("Apache-2.0");

            project.getLogger().lifecycle("Bintray configuration updated; repo={},version={}", packageConfig.getRepo(), packageConfig.getVersion().getName());
        });
    }

    private void configureJavaCompile(Project project) {
        final Action<JavaCompile> action = task -> {

            CompileOptions options = task.getOptions();
            options.setEncoding("UTF-8");
            project.getLogger().lifecycle("Java compile task {} - encoding set to UTF-8", task.getName());

            Set<String> lintOpts = new TreeSet<>();
            lintOpts.add("serial");
            lintOpts.add("varargs");
            lintOpts.add("cast");
            lintOpts.add("-classfile");
            lintOpts.add("dep-ann");
            lintOpts.add("divzero");
            lintOpts.add("empty");
            lintOpts.add("finally");
            lintOpts.add("overrides");
            lintOpts.add("path");
            lintOpts.add("processing");
            lintOpts.add("static");
            lintOpts.add("try");
            lintOpts.add("fallthrough");
            lintOpts.add("rawtypes");
            lintOpts.add("deprecation");
            lintOpts.add("unchecked");
            lintOpts.add("-options");

            List<String> compilerArgs = lintOpts.stream().map(opt -> "-Xlint:" + opt).collect(Collectors.toList());
          //  compilerArgs.add("-Werror");

            project.getLogger().lifecycle("Java compile task {} - added compiler args {}", task.getName(), compilerArgs);

            options.getCompilerArgs().addAll(compilerArgs);
        };

        project.afterEvaluate(project1 -> {
            final TaskCollection<JavaCompile> javaCompileTasks = project1.getTasks().withType(JavaCompile.class);
            javaCompileTasks.all(action);
        });


    }

    private void registerPublishArtifactsTask(Project project) {

        Action<DefaultTask> configureAction = task -> {

            task.dependsOn("release", "bintrayUpload", "artifactoryPublish");
            task.setGroup("publishing");

            Task bintrayUploadTask = project.getTasksByName("bintrayUpload", false).stream().findFirst().orElseThrow(() -> new IllegalStateException("Unable to find bintrayUpload task"));
            Task artifactoryPublishTask = project.getTasksByName("artifactoryPublish", false).stream().findFirst().orElseThrow(() -> new IllegalStateException("Unable to find artifactoryPublish task"));

            artifactoryPublishTask.setEnabled(false);
            bintrayUploadTask.setEnabled(false);

            // TODO - can these come from gradle-info-plugin?
            String tag = System.getenv("TRAVIS_TAG");
            String branch = System.getenv("TRAVIS_BRANCH");
            String pullRequest = System.getenv("TRAVIS_PULL_REQUEST");

            if ((tag != null && !tag.isEmpty()) || ("master".equals(branch) && "false".equals(pullRequest))) {
                if (tag != null && !tag.isEmpty()) {
                    bintrayUploadTask.setEnabled(true);
                } else {
                    artifactoryPublishTask.setEnabled(true);
                }
            }
            project.getLogger().lifecycle("Publish artifacts task configured; tag={}, branch={}, pullRequest={},bintrayUploadTask.enabled={},artifactoryPublishTask={}",
                    tag, branch, pullRequest, bintrayUploadTask.getEnabled(), artifactoryPublishTask.getEnabled());
        };
        project.getTasks().create("publishArtifacts", DefaultTask.class, configureAction);
    }

    private void configureNebulaResolutionRules(Project project) {
        Configuration resolutionRulesConfiguration = project.getRootProject().getConfigurations().getAt("resolutionRules");
        resolutionRulesConfiguration.getDependencies().add(project.getDependencies().create("com.netflix.nebula:gradle-resolution-rules:0.58.0"));

        NebulaResolutionRulesExtension resolutionRulesExtension = (NebulaResolutionRulesExtension) project.getExtensions().getByName("nebulaResolutionRules");
        resolutionRulesExtension.getOptional().add("slf4j-bridge");
        project.afterEvaluate((o) -> {
            project.getLogger().lifecycle("Optional resolution rules: {}", resolutionRulesExtension.getOptional());
            resolutionRulesConfiguration.getDependencies().forEach(dependency -> project.getLogger().lifecycle("Resolution rules dependency: {}", dependency));
        });
    }
}