plugins {
    base
}

// A beautiful crime against nature. We're using Gradle to wrangle a Node.js project.
// Why? Because we can. Because the unity of the build system is a goal in itself,
// however perverse the path to achieving it may be.

description = "Geministrator VSCode Plugin"

// Define paths for the Node project within the Gradle module
val nodeDir = project.projectDir
val vsixDir = layout.buildDirectory.dir("vsix")

// Task to install npm dependencies from package.json
val npmInstall by tasks.registering(Exec::class) {
    group = "VSCode"
    description = "Install Node.js dependencies."
    workingDir = nodeDir
    commandLine("npm", "install")

    inputs.file(file("package.json"))
    inputs.file(file("package-lock.json"))
    outputs.dir(file("node_modules"))
}

// Task to run the VSCode extension packager (vsce)
val vscePackage by tasks.registering(Exec::class) {
    dependsOn(npmInstall)
    group = "VSCode"
    description = "Package the extension into a .vsix file."
    workingDir = nodeDir
    // Assumes vsce is a dev dependency, accessed via npx
    commandLine("npx", "vsce", "package", "--out", vsixDir.get().asFile.absolutePath)

    inputs.dir(file("src"))
    inputs.file(file("package.json"))
    outputs.dir(vsixDir)
}

// The main 'build' task for this module creates the .vsix package
tasks.named("build") {
    dependsOn(vscePackage)
}

// A 'clean' task to remove all generated files
tasks.named("clean", Delete::class) {
    delete(vsixDir)
    delete(file("node_modules"))
    delete(file("out"))
}