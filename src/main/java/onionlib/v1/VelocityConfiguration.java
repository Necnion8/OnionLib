package onionlib.v1;

import com.google.common.io.ByteStreams;
import org.slf4j.Logger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

@SuppressWarnings("unused")
public class VelocityConfiguration {
    protected final Logger log;
    protected final Path dataFolder;
    protected final String fileName;
    protected final String resourceFilePath;
    protected final Path path;
    protected final YamlConfigurationLoader loader;
    protected CommentedConfigurationNode config;

    public VelocityConfiguration(Logger logger, Path dataFolder, String fileName, String resourceFilePath) {
        this.log = logger;
        this.dataFolder = dataFolder;
        this.fileName = fileName;
        this.resourceFilePath = resourceFilePath;
        this.path = Paths.get(dataFolder.toString(), fileName);
        this.loader = YamlConfigurationLoader.builder().path(path).build();
        this.config = loader.createNode();
    }

    public VelocityConfiguration(Logger logger, Path dataFolder) {
        this(logger, dataFolder, "config.yml", "velocity-config.yml");
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
                config = loader.load();
            } else {
                config = loader.createNode();
            }

            return onLoaded();

        } catch (IOException e) {
            log.error("Unable to load file: {}: {}: {}", fileName, e.getClass().getSimpleName(), e.getLocalizedMessage());

        } catch (Exception e) {
            log.error("Unable to load file: {}", fileName, e);
        }
        return false;
    }

    public boolean save() {
        try {
            if (Files.notExists(dataFolder))
                Files.createDirectory(dataFolder);

            loader.save(config);
            return true;

        } catch (IOException e) {
            log.error("Unable to save file: {}: {}: {}", fileName, e.getClass().getSimpleName(), e.getLocalizedMessage());

        } catch (Exception e) {
            log.error("Unable to save file: {}", fileName, e);
        }
        return false;
    }

    protected boolean onLoaded() {
        return true;
    }

}
