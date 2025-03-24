package onionlib.v1;

import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class BungeeConfiguration {
    protected final Logger log;
    protected final Path dataFolder;
    protected final String fileName;
    protected final String resourceFilePath;
    protected final Path path;
    protected Configuration config = new Configuration();
    protected final ConfigurationProvider provider = ConfigurationProvider.getProvider(YamlConfiguration.class);

    public BungeeConfiguration(Logger logger, Path dataFolder, String fileName, String resourceFilePath) {
        this.log = logger;
        this.dataFolder = dataFolder;
        this.fileName = fileName;
        this.resourceFilePath = resourceFilePath;
        this.path = Paths.get(dataFolder.toString(), fileName);
    }

    public BungeeConfiguration(Logger logger, Path dataFolder) {
        this(logger, dataFolder, "config.yml", "bungee-config.yml");
    }

    public BungeeConfiguration(Plugin plugin, String fileName, String resourceFilePath) {
        this(plugin.getLogger(), plugin.getDataFolder().toPath(), fileName, resourceFilePath);
    }

    public BungeeConfiguration(Plugin plugin) {
        this(plugin.getLogger(), plugin.getDataFolder().toPath());
    }

    protected void generateNewFile(Path path) throws IOException {
        generateResourceFile(path);
    }

    protected void generateResourceFile(Path path) throws IOException {
        try (InputStream is = Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(resourceFilePath),
                "No resource file in plugin: " + resourceFilePath);
             OutputStream os = Files.newOutputStream(path)) {
            ByteStreams.copy(is, os);
        }
    }

    public boolean load() {
        try {
            if (Files.notExists(dataFolder))
                Files.createDirectory(dataFolder);

            if (Files.notExists(path))
                generateNewFile(path);

            if (Files.exists(path)) {
                try (InputStreamReader stream = new InputStreamReader(Files.newInputStream(path), Charsets.UTF_8)) {
                    config = provider.load(stream);
                }
            } else {
                config = new Configuration();
            }

            return onLoaded();

        } catch (IOException e) {
            log.severe("Unable to load file: " + fileName + ": " + e.getClass().getSimpleName() + ": " + e.getLocalizedMessage());

        } catch (Exception e) {
            log.log(Level.SEVERE, "Unable to load file: " + fileName, e);
        }
        return false;
    }

    public boolean save() {
        try {
            if (Files.notExists(dataFolder))
                Files.createDirectory(dataFolder);

            try (OutputStreamWriter stream = new OutputStreamWriter(Files.newOutputStream(path), Charsets.UTF_8)) {
                provider.save(config, stream);
                return true;
            }

        } catch (IOException e) {
            log.severe("Unable to save file: " + fileName + ": " + e.getClass().getSimpleName() + ": " + e.getLocalizedMessage());

        } catch (Exception e) {
            log.log(Level.SEVERE, "Unable to save file: " + fileName, e);
        }
        return false;
    }

    protected boolean onLoaded() {
        return true;
    }


    protected static List<Configuration> getConfigList(Configuration parent, String key) {
        List<?> list = parent.getList(key);
        return list != null ? list.stream()
                .filter(obj -> obj instanceof Map)
                .map(obj -> createMemoryConfigurationFromMap((Map<?, ?>) obj))
                .collect(Collectors.toList()) : Collections.emptyList();
    }

    private static void putMapToMemoryConfiguration(Configuration configuration, Map<?, ?> map) {
        map.forEach((k, v) -> {
            if (v instanceof Map) {
                configuration.set((String) k, createMemoryConfigurationFromMap((Map<?, ?>) v));
            } else {
                configuration.set((String) k, v);
            }
        });
    }

    private static Configuration createMemoryConfigurationFromMap(Map<?, ?> map) {
        Configuration nest = new Configuration();
        putMapToMemoryConfiguration(nest, map);
        return nest;
    }

}
