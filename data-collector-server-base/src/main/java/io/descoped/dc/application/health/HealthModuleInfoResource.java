package io.descoped.dc.application.health;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.descoped.dc.api.health.HealthRenderPriority;
import io.descoped.dc.api.health.HealthResource;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.Resource;
import io.github.classgraph.ScanResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.lang.module.ModuleDescriptor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

import static java.util.Optional.ofNullable;

/**
 * The modules of the boot layer in the health output, with their Maven coordinates.
 */
@HealthRenderPriority(priority = 20)
public class HealthModuleInfoResource implements HealthResource {

    private static final Logger LOG = LoggerFactory.getLogger(HealthModuleInfoResource.class);

    private final List<ModuleInfo> modules;

    /**
     * Creates the resource.
     */
    public HealthModuleInfoResource() {
        modules = scanModules("io.descoped");
    }

    @Override
    public Optional<Boolean> isUp() {
        return Optional.empty();
    }

    @Override
    public String name() {
        return "modules";
    }

    @Override
    public boolean isList() {
        return true;
    }

    @Override
    public boolean canRender(Map<String, Deque<String>> queryParams) {
        return true;
    }

    @Override
    public Object resource() {
        return modules;
    }

    private List<ModuleInfo> scanModules(String packageName) {
        List<Module> bootModules = ModuleLayer
                .boot()
                .modules()
                .stream()
                .sorted(Comparator.comparing(md -> md.getDescriptor().name()))
                .filter(md -> md.getDescriptor().name().startsWith(packageName))
                .toList();

        List<ModuleInfo> moduleInfoList = new ArrayList<>();
        for (Module module : bootModules) {
            ModuleInfo.Builder moduleInfoBuilder = new ModuleInfo.Builder();

            ModuleDescriptor moduleDescriptor = module.getDescriptor();
            moduleInfoBuilder.moduleName(moduleDescriptor.name());

            try (ScanResult scanResult = new ClassGraph()
                    .enableClassLoaders(module.getClassLoader())
                    .enableClasspath()
                    .ignoreParentClassLoaders()
                    .enableModuleLayers(module.getLayer())
                    .enableNonSystemModules()
                    .enableClassInfo()
                    .acceptPaths("META-INF/maven")
                    .scan()
            ) {
                for (Resource resource : scanResult.getResourcesWithLeafName("pom.properties").nonClassFilesOnly()) {
                    byte[] content = load(resource);
                    if (isModuleNameEqualToResourceModuleRefName(moduleDescriptor, resource)) {
                        buildMavenInfo(moduleInfoBuilder, content);
                    }
                }
            }

            moduleInfoList.add(moduleInfoBuilder.build());
        }

        return moduleInfoList;
    }

    private boolean isModuleNameEqualToResourceModuleRefName(ModuleDescriptor moduleDescriptor, Resource resource) {
        if (resource.getModuleReference() == null) {
            return false;
        }
        return moduleDescriptor.name().equals(resource.getModuleReference().descriptor().name());
    }

    private void buildMavenInfo(ModuleInfo.Builder moduleInfoBuilder, byte[] content) {
        try {
            Properties mavenProperties = new Properties();
            try (BufferedReader reader = new BufferedReader(new StringReader(new String(content)))) {
                mavenProperties.load(reader);
                moduleInfoBuilder
                        .groupId(mavenProperties.getProperty("groupId"))
                        .artifactId(mavenProperties.getProperty("artifactId"))
                        .version(mavenProperties.getProperty("version"));
            }
        } catch (IOException e) {
            LOG.warn("Problem reading version resource from classpath: ", e);
        }
    }

    /**
     * A module and its Maven coordinates.
     */
    @SuppressWarnings("WeakerAccess")
    public static class ModuleInfo {
        /**
         * The module's name.
         */
        @JsonProperty
        public final String moduleName;
        /**
         * The Maven group id.
         */
        @JsonProperty
        public final String groupId;
        /**
         * The Maven artifact id.
         */
        @JsonProperty
        public final String artifactId;
        /**
         * The version.
         */
        @JsonProperty
        public final String version;

        ModuleInfo(String moduleName, String groupId, String artifactId, String version) {
            this.moduleName = moduleName;
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ModuleInfo that = (ModuleInfo) o;
            return moduleName.equals(that.moduleName) &&
                    Objects.equals(groupId, that.groupId) &&
                    Objects.equals(artifactId, that.artifactId) &&
                    Objects.equals(version, that.version);
        }

        @Override
        public int hashCode() {
            return Objects.hash(moduleName, groupId, artifactId, version);
        }

        @Override
        public String toString() {
            return "ModuleInfo{" +
                    "moduleName='" + moduleName + '\'' +
                    ", groupId='" + groupId + '\'' +
                    ", artifactId='" + artifactId + '\'' +
                    ", version='" + version + '\'' +
                    '}';
        }

        static class Builder {
            private String moduleName;
            private String groupId;
            private String artifactId;
            private String version;

            Builder moduleName(String moduleName) {
                this.moduleName = moduleName;
                return this;
            }

            Builder groupId(String groupId) {
                this.groupId = groupId;
                return this;
            }

            Builder artifactId(String artifactId) {
                this.artifactId = artifactId;
                return this;
            }

            Builder version(String version) {
                this.version = version;
                return this;
            }

            public ModuleInfo build() {
                return new ModuleInfo(
                        moduleName,
                        ofNullable(groupId).orElse("unknown"),
                        ofNullable(artifactId).orElse("unknown"),
                        ofNullable(version).orElse("(DEV VERSION)")
                );
            }
        }
    }

    // the resource's content, the resource closed; as ClassGraph's deprecated forEachByteArray
    private static byte[] load(Resource resource) {
        try (resource) {
            return resource.load();
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not load resource " + resource, e);
        }
    }
}
