package com.digitalascent.gradle.project;

import com.digitalascent.gradle.errorprone.ErrorPronePlugin;
import nebula.plugin.resolutionrules.NebulaResolutionRulesExtension;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

public class ProjectPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(ErrorPronePlugin.class);
        project.getPluginManager().apply(nebula.plugin.responsible.NebulaResponsiblePlugin.class);
        project.getPluginManager().apply(nebula.plugin.bintray.BintrayPlugin.class);
        project.getPluginManager().apply(nebula.plugin.resolutionrules.ResolutionRulesPlugin.class);
        project.getPluginManager().apply(pl.allegro.tech.build.axion.release.ReleasePlugin.class);

        configureNebulaResolutionRules(project);

        // TODO - JavaCompile options (lint, encoding)
        // TODO - JavaDoc settings

        // TODO - publishArtifacts task

        // not sure about these...
        // TODO - bintray defaults
        // TODO - contacts
        // TODO - default group, contact info
    }

    private void configureNebulaResolutionRules(Project project) {
        Configuration resolutionRulesConfiguration = project.getRootProject().getConfigurations().getAt("resolutionRules");
        resolutionRulesConfiguration.getDependencies().add(project.getDependencies().create("com.netflix.nebula:gradle-resolution-rules:0.52.0"));

        NebulaResolutionRulesExtension resolutionRulesExtension = (NebulaResolutionRulesExtension) project.getExtensions().getByName("nebulaResolutionRules");
        resolutionRulesExtension.getOptional().add("slf4j-bridge");
    }
}