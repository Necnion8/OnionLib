# OnionLib
Minecraftプラグインの開発を少し楽にするかもしれないクラスたち


### Configuration


### Command

#### for Bukkit
```java
public class BukkitPluginMain extends JavaPlugin {

    private final BukkitCommand.Compat compat = BukkitCommand.compat(this);

    @Override
    public void onEnable() {
        compat.init();
        compat.register(new TestCommand());
    }

    @Override
    public void onDisable() {
        compat.close();
    }
}
```
