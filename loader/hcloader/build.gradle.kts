import ink.bgp.hcloader.gradle.HcLoaderConfigEntry

plugins {
    alias(libs.plugins.hcloader)
}

configurations {
    shadowRuntime {
        extendsFrom(runtimeClasspath.get())
    }
}

dependencies {
    delegateRuntime(project(":"))

    implementation(project(":api"))
    implementation(project(":libs:shadow-callsite-nbt", configuration = "shadow"))

    implementation(libs.bundles.adventure)

    // minecraft
    compileOnly("com.destroystokyo.paper:paper-api:1.16.5-R0.1-SNAPSHOT") {
        exclude("com.google.code.gson", "gson") // we use our version
        exclude("it.unimi.dsi", "fastutil") // we use our version
        exclude("org.checkerframework", "checker-qual")
        exclude("org.jetbrains", "annotations")
    }
    compileOnly(libs.hcloader)
}

tasks.hcLoaderJar {
    dependsOn(tasks.jar)

    loaderPackage.set("")
    enableStaticInject.set(true)
    staticInjectClass.set("org.inksnow.ankh.loader.AnkhCoreLoaderPlugin")

    exclude("classpath.index")
    exclude("module-info.class", "META-INF/versions/*/module-info.class")

    loadConfig.add(
        HcLoaderConfigEntry.of(
            10,
            "org/inksnow/ankh/core/api/**",
            HcLoaderConfigEntry.HcLoaderLoadPolicy.PARENT_ONLY
        )
    )

    loadConfig.add(
        HcLoaderConfigEntry.of(
            10,
            "kotlin/**",
            HcLoaderConfigEntry.HcLoaderLoadPolicy.SELF_ONLY
        )
    )
}

tasks.assemble {
    dependsOn(tasks.hcLoaderJar)
}