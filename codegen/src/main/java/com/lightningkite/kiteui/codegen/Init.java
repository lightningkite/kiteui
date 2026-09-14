package com.lightningkite.kiteui.codegen;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generates the vendored Kotlin Toolchain plugin shim into a consumer project.
 *
 * <p>Written in Java on purpose: it is a bootstrap tool run as {@code java -cp codegen.jar ...} before
 * the consumer has resolved any dependencies, so it must run with nothing but the JDK on the
 * classpath. Kotlin code here would drag in kotlin-stdlib, which the consumer would then have to
 * fetch separately just to install the shim.
 *
 * <p>The shim templates live in this jar, so the shim can never be older than the library it delegates to.
 */
public final class Init {

    public static void main(String[] args) throws IOException {
        Map<String, String> opts = parseArgs(args);
        String version = require(opts, "version");
        Path project = Paths.get(opts.getOrDefault("project", ".")).toAbsolutePath().normalize();
        Path into = project.resolve(opts.getOrDefault("into", "kiteui-plugin")).normalize();
        List<String> modulePaths = Arrays.asList(require(opts, "module").split(","));
        String packageName = require(opts, "package");
        String mavenRepo = opts.get("maven-repo");
        Set<String> groups = new LinkedHashSet<>(Arrays.asList(opts.getOrDefault("only", "resources,routes").split(",")));
        String applyTemplate = opts.get("apply-template");

        List<Path> modules = new ArrayList<>();
        Set<String> platforms = new LinkedHashSet<>();
        for (String modulePath : modulePaths) {
            Path module = project.resolve(modulePath.trim()).normalize();
            Path moduleYaml = module.resolve("module.yaml");
            if (!Files.isRegularFile(moduleYaml)) {
                throw new IllegalArgumentException("No module.yaml found at " + moduleYaml);
            }
            modules.add(module);
            platforms.addAll(platformsOf(moduleYaml));
        }

        // The resources generator emits `expect object Resources` for common and one `actual` per
        // platform. Which platforms those are is not a choice: it is whatever the consuming modules
        // declare. Leaving it to --only means a module with an iOS or Android target silently gets an
        // `expect` with no matching `actual`, which fails at compile time rather than here.
        if (groups.contains("resources")) groups.addAll(resourcePlatformGroups(platforms));

        String pluginName = into.getFileName().toString();
        String intoRel = project.relativize(into).toString();

        Map<String, String> subs = new HashMap<>();
        subs.put("@VERSION@", version);
        subs.put("@PACKAGE@", packageName);

        // Only the iOS group needs this; it is the Xcode project directory holding Info.plist and
        // Assets.xcassets, which the generator writes into rather than authoring from scratch.
        subs.put("@IOS_PROJECT@", opts.getOrDefault("ios-project", "${module.rootDir}/ios"));
        // The plugin module resolves the codegen jar itself, so it needs the repository that hosts it
        // whenever that is not one of the toolchain's defaults. Pointing at a module template the
        // consumer already owns is preferred: the shim then carries a stable reference rather than a
        // copy of configuration that lives somewhere else.
        subs.put("@REPO_CONFIG@", repoConfig(applyTemplate, mavenRepo));

        List<String> changes = new ArrayList<>();
        Files.createDirectories(into.resolve("src"));
        emit(into.resolve("module.yaml"), "module.yaml.template", subs, groups, changes, project);
        emit(into.resolve("plugin.yaml"), "plugin.yaml.template", subs, groups, changes, project);
        emit(into.resolve("src/shim.kt"), "shim.kt.template", subs, groups, changes, project);

        patchProjectYaml(project.resolve("project.yaml"), intoRel, modulePaths.get(0).trim(), changes, project);
        for (Path module : modules) patchModuleYaml(module.resolve("module.yaml"), pluginName, changes, project);

        if (changes.isEmpty()) {
            System.out.println("kiteui shim already up to date (codegen " + version + ")");
        } else {
            System.out.println("kiteui shim installed (codegen " + version + "):");
            for (String c : changes) System.out.println("  " + c);
        }
    }

    /**
     * Reads the `platforms:` list out of a module.yaml.
     *
     * A deliberately small reader rather than a YAML parser: this runs from a bare JDK before the
     * consumer has resolved a single dependency, so it cannot pull one in. It only needs the inline
     * `platforms: [ a, b ]` form the Toolchain writes.
     */
    private static Set<String> platformsOf(Path moduleYaml) throws IOException {
        Set<String> found = new LinkedHashSet<>();
        for (String line : Files.readAllLines(moduleYaml)) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("platforms:")) continue;
            String list = trimmed.substring("platforms:".length()).trim();
            if (!list.startsWith("[") || !list.endsWith("]")) continue;
            for (String p : list.substring(1, list.length() - 1).split(",")) {
                if (!p.trim().isEmpty()) found.add(p.trim());
            }
        }
        // A module.yaml with no platforms list is single-platform (`product: jvm/lib` and friends).
        if (found.isEmpty()) found.add("jvm");
        return found;
    }

    /** Maps declared platforms onto the resource groups that supply their `actual` declarations. */
    private static Set<String> resourcePlatformGroups(Set<String> platforms) {
        Set<String> groups = new LinkedHashSet<>();
        Set<String> unsupported = new LinkedHashSet<>();
        for (String platform : platforms) {
            if (platform.equals("jvm")) groups.add("resources-jvm");
            else if (platform.equals("android")) groups.add("resources-android");
            else if (platform.equals("js")) groups.add("resources-js");
            else if (platform.startsWith("ios")) groups.add("resources-ios");
            else unsupported.add(platform);
        }
        if (!unsupported.isEmpty()) {
            // Better to say so than to emit an `expect` whose `actual` never arrives.
            System.out.println("WARNING: no kiteui resource generator for " + unsupported
                    + "; those targets will have no `actual object Resources` and will not compile"
                    + " while resource generation is enabled.");
        }
        return groups;
    }

    /** Either a pointer at a consumer-owned module template, or an inline repository declaration. */
    private static String repoConfig(String applyTemplate, String mavenRepo) {
        if (applyTemplate != null) return "\napply:\n  - //" + applyTemplate + "\n";
        if (mavenRepo != null) return "\nrepositories:\n  - url: " + mavenRepo + "\n";
        return "";
    }

    private static void emit(Path target, String template, Map<String, String> subs, Set<String> groups, List<String> changes, Path project) throws IOException {
        String text = selectGroups(readTemplate(template), groups);
        for (Map.Entry<String, String> e : subs.entrySet()) text = text.replace(e.getKey(), e.getValue());
        writeIfChanged(target, text, changes, project);
    }

    /**
     * Keeps only the `#<group:name>` / `//<group:name>` blocks the caller asked for, and strips the
     * markers. A module that only needs route generation should not carry task wiring for resources.
     */
    private static String selectGroups(String text, Set<String> groups) {
        StringBuilder result = new StringBuilder();
        boolean keep = true;
        for (String line : text.split("\n", -1)) {
            String marker = line.trim();
            if (marker.startsWith("#<group:") || marker.startsWith("//<group:")) {
                keep = groups.contains(marker.substring(marker.indexOf(':') + 1, marker.indexOf('>')));
                continue;
            }
            if (marker.equals("#</group>") || marker.equals("//</group>")) {
                keep = true;
                continue;
            }
            if (keep) result.append(line).append('\n');
        }
        // split with -1 keeps a trailing empty field; drop the extra newline it adds back.
        return result.length() > 0 ? result.substring(0, result.length() - 1) : "";
    }

    private static String readTemplate(String name) throws IOException {
        try (InputStream in = Init.class.getResourceAsStream("/kiteui-shim/" + name)) {
            if (in == null) throw new IllegalStateException("Template /kiteui-shim/" + name + " missing from codegen jar");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void writeIfChanged(Path target, String text, List<String> changes, Path project) throws IOException {
        boolean existed = Files.exists(target);
        if (existed && Files.readString(target).equals(text)) return;
        Files.createDirectories(target.getParent());
        Files.writeString(target, text);
        changes.add((existed ? "updated " : "created ") + project.relativize(target));
    }

    /** Adds the shim module to `modules:` and registers it under `plugins:` in project.yaml. */
    private static void patchProjectYaml(Path file, String intoRel, String modulePath, List<String> changes, Path project) throws IOException {
        List<String> lines = Files.exists(file)
                ? new ArrayList<>(Files.readAllLines(file))
                : new ArrayList<>(List.of("modules:", "  - " + modulePath));
        boolean changed = false;
        changed |= ensureListEntry(lines, "modules:", intoRel);
        changed |= ensureListEntry(lines, "plugins:", "./" + intoRel);
        if (changed) {
            Files.write(file, lines);
            changes.add("patched " + project.relativize(file));
        }
    }

    /** Enables the plugin on the consuming module. */
    private static void patchModuleYaml(Path file, String pluginName, List<String> changes, Path project) throws IOException {
        List<String> lines = new ArrayList<>(Files.readAllLines(file));
        String entry = "  " + pluginName + ": enabled";
        int header = indexOfTopLevel(lines, "plugins:");
        if (header < 0) {
            if (!lines.isEmpty() && !lines.get(lines.size() - 1).isBlank()) lines.add("");
            lines.add("plugins:");
            lines.add(entry);
        } else {
            for (int i = header + 1; i < lines.size() && isBlockChild(lines.get(i)); i++) {
                if (lines.get(i).trim().startsWith(pluginName + ":")) return;
            }
            lines.add(endOfBlock(lines, header), entry);
        }
        Files.write(file, lines);
        changes.add("patched " + project.relativize(file));
    }

    private static boolean ensureListEntry(List<String> lines, String header, String value) {
        String entry = "  - " + value;
        int at = indexOfTopLevel(lines, header);
        if (at < 0) {
            if (!lines.isEmpty() && !lines.get(lines.size() - 1).isBlank()) lines.add("");
            lines.add(header);
            lines.add(entry);
            return true;
        }
        int insertAt = endOfBlock(lines, at);
        for (int i = at + 1; i < lines.size() && isBlockChild(lines.get(i)); i++) {
            String trimmed = lines.get(i).trim();
            if (trimmed.equals("- " + value)) return false;
            // The toolchain asks for these lists to be sorted, so keep them that way.
            if (insertAt > i && trimmed.startsWith("- ") && trimmed.substring(2).compareTo(value) > 0) insertAt = i;
        }
        lines.add(insertAt, entry);
        return true;
    }

    private static int indexOfTopLevel(List<String> lines, String header) {
        for (int i = 0; i < lines.size(); i++) if (lines.get(i).equals(header)) return i;
        return -1;
    }

    /** A line belongs to the block if it is indented or blank; a new top-level key ends the block. */
    private static boolean isBlockChild(String line) {
        return line.isBlank() || line.startsWith(" ") || line.startsWith("\t") || line.startsWith("#");
    }

    private static int endOfBlock(List<String> lines, int header) {
        int end = header + 1;
        for (int i = header + 1; i < lines.size() && isBlockChild(lines.get(i)); i++) {
            if (!lines.get(i).isBlank()) end = i + 1;
        }
        return end;
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> opts = new HashMap<>();
        int i = 0;
        if (i < args.length && !args[i].startsWith("--")) {
            if (!args[i].equals("init")) throw new IllegalArgumentException("Unknown command: " + args[i]);
            i++;
        }
        for (; i < args.length; i++) {
            if (!args[i].startsWith("--")) throw new IllegalArgumentException("Unexpected argument: " + args[i]);
            if (i + 1 >= args.length) throw new IllegalArgumentException("Missing value for " + args[i]);
            opts.put(args[i].substring(2), args[++i]);
        }
        return opts;
    }

    private static String require(Map<String, String> opts, String key) {
        String v = opts.get(key);
        if (v == null) throw new IllegalArgumentException("Missing required option --" + key);
        return v;
    }

    private Init() {}
}
