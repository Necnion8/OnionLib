# OnionLib
Minecraftプラグインの開発を少し楽にするかもしれないクラスたち (開発中)


## Configuration

### 実装

| file                                                                                                                                                                       | description          |
|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------|
| [[raw]](src%2Fmain%2Fjava%2Fonionlib%2Fv1%2FBukkitConfiguration.java?raw=1) [BukkitConfiguration.java](src%2Fmain%2Fjava%2Fonionlib%2Fv1%2FBukkitConfiguration.java)       | 設定クラス for Bukkit     |
| [[raw]](src%2Fmain%2Fjava%2Fonionlib%2Fv1%2FBungeeConfiguration.java?raw=1) [BungeeConfiguration.java](src%2Fmain%2Fjava%2Fonionlib%2Fv1%2FBungeeConfiguration.java)       | 設定クラス for BungeeCord |
| [[raw]](src%2Fmain%2Fjava%2Fonionlib%2Fv1%2FVelocityConfiguration.java?raw=1) [VelocityConfiguration.java](src%2Fmain%2Fjava%2Fonionlib%2Fv1%2FVelocityConfiguration.java) | 設定クラス for Velocity   |



## Command
### 主な機能
- ネスト可能なサブコマンド
- 引数パラメータ
- プレフィックス挿入
- シンプルコマンド一覧
- プラットフォームに依存しない

### 前提
#### 導入サーバー
メッセージ処理に必要な [KyoriPowered/adventure Library](https://github.com/KyoriPowered/adventure) がサーバー環境で利用できる必要があります。  
Paper 1.16.5 など、ネイティブで対応しているサーバーは [docs.advntr.dev](https://docs.advntr.dev/platform/native.html) で確認できます。

対応しない古いサーバーなどでは [Necnion8/KyoriAdventureLib](https://github.com/Necnion8/KyoriAdventureLib) をサーバーに導入することで対応します。

#### 依存関係
- [net.kyori:adventure-api](https://docs.advntr.dev/getting-started.html)
- 使用するサーバーの基本API (bukkit-api など)

### 実装

| file                                                                                                                                                     | description          |
|----------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------|
| [[raw]](src%2Fmain%2Fjava%2Fonionlib%2Fv1%2FCommand.java?raw=1) [Command.java](src%2Fmain%2Fjava%2Fonionlib%2Fv1%2FCommand.java)                         | 共通のコマンド実装クラス         |
| [[raw]](src%2Fmain%2Fjava%2Fonionlib%2Fv1%2FBukkitCommand.java?raw=1) [BukkitCommand.java](src%2Fmain%2Fjava%2Fonionlib%2Fv1%2FBukkitCommand.java)       | 登録クラス for Bukkit     |
| [[raw]](src%2Fmain%2Fjava%2Fonionlib%2Fv1%2FBungeeCommand.java?raw=1) [BungeeCommand.java](src%2Fmain%2Fjava%2Fonionlib%2Fv1%2FBungeeCommand.java)       | 登録クラス for BungeeCord |
| [[raw]](src%2Fmain%2Fjava%2Fonionlib%2Fv1%2FVelocityCommand.java?raw=1) [VelocityCommand.java](src%2Fmain%2Fjava%2Fonionlib%2Fv1%2FVelocityCommand.java) | 登録クラス for Velocity   |

#### コマンドの実装例
```java
public class TestCommand extends Command {
    public TestCommand() {
        super("test", "example.command");

        // set prefix
        messagePrefix(Component.text()
                .append(Component.text("[", NamedTextColor.GRAY))
                .append(Component.text("OnionLib", NamedTextColor.AQUA))
                .append(Component.text("] ", NamedTextColor.GRAY)), true);

        // /test help
        addHelpCommand().defaultCommand();
        // /test reload
        addChild("reload", this::reload);
        // /test sub <int/bool/str> [ ..]
        addChildApply("sub", c -> {
            // /test sub int (number)
            c.addChild("int", this::subInt)
                    .argumentInt(0, 10);  // min, max
            // /test sub bool <true/false>
            c.addChild("bool", this::subBool)
                    .argumentBool();
            // /test sub str [value]
            c.addChild("str", this::subString)
                    .argumentString();
        });
    }

    private void reload(Context ctx) {
        ctx.send(Component.text("Configuration reloaded!", NamedTextColor.GREEN));
    }

    private void subInt(Context ctx) {
        int value = ctx.get(IntArg.class);  // not optional
        ctx.send(Component.text("The value is " + value + "!", NamedTextColor.GOLD));
    }

    private void subBool(Context ctx) {
        boolean value = ctx.get(BoolArg.class);  // not optional
        ctx.send(Component.text("The value is " + value + "!", NamedTextColor.GOLD));
    }

    private void subString(Context ctx) {
        String value = ctx.getOptional(StringArg.class).orElse(null);  // optional
        if (value != null) {
            ctx.send(Component.text("The value is \"" + value + "\"!", NamedTextColor.GOLD));
        } else {
            ctx.send(Component.text("No value specified!", NamedTextColor.YELLOW));
        }
    }
}
```
サンプル: [SampleCommand.java](src%2Fmain%2Fjava%2Fonionlib%2Fsample%2FSampleCommand.java)

#### コマンドの登録 for Bukkit
<details>
<summary>サンプル表示</summary>

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

**より短く (Paper 1.16.5 以降のみ)**
```java
public class BukkitPluginMain extends JavaPlugin {
    @Override
    public void onEnable() {
        BukkitCommand.register(this, new TestCommand());
    }
}
```
</details>

#### コマンドの登録 for BungeeCord
<details>
<summary>サンプル表示</summary>

```java
public class BungeePluginMain extends Plugin {
    private final BungeeCommand.Compat compat = BungeeCommand.compat(this);

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
</details>

#### コマンドの登録 for Velocity
<details>
<summary>サンプル表示</summary>

```java
@Plugin(id = "examplecommand")
public final class VelocityPluginMain {
    private final ProxyServer proxy;

    @Inject
    public VelocityMain(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        VelocityCommand.register(proxy, new TestCommand(), metaBuilder -> {});
    }
}
```
</details>
