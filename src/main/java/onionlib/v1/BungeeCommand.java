package onionlib.v1;

import net.kyori.adventure.audience.Audience;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("unused")
public class BungeeCommand extends net.md_5.bungee.api.plugin.Command implements TabExecutor {

    private static final String UNSUPPORTED_LIB_ERROR = "Please install the KyoriAdventureLib from https://github.com/Necnion8/KyoriAdventureLib";

    private final Command command;
    private final @NotNull Command.AudienceResolver audienceResolver;

    /**
     * {@link Command} を Bungee コマンドに変換します
     * @param audienceResolver {@link Audience} の変換に使用するアダプタ
     */
    public BungeeCommand(Command command, @NotNull Command.AudienceResolver audienceResolver, @Nullable String permission, String... aliases) {
        super(command.getName(), permission, aliases);
        this.command = command;
        this.audienceResolver = audienceResolver;
    }

    /**
     * {@link Command} を Bungee コマンドに変換します
     * @param audienceResolver {@link Audience} の変換に使用するアダプタ
     */
    public BungeeCommand(Command command, @NotNull Command.AudienceResolver audienceResolver) {
        super(command.getName(), null);
        this.command = command;
        this.audienceResolver = audienceResolver;
    }

    public void register(Plugin owner) {
        owner.getProxy().getPluginManager().registerCommand(owner, this);
    }

    public void unregister(Plugin owner) {
        owner.getProxy().getPluginManager().unregisterCommand(this);
    }


    /**
     * {@link CommandSender}のコマンドLib用のクラスを作成します<br>
     * プレイヤーである場合は {@link BungeeCommand#createPlayerSender(ProxiedPlayer)} を使用します
     */
    public Command.Sender createSender(CommandSender sender) {
        if (sender instanceof ProxiedPlayer)
            return createPlayerSender((ProxiedPlayer) sender);
        return new BungeeSender(sender, audienceResolver.getAudience(sender));
    }

    /**
     * {@link ProxiedPlayer}のコマンドLib用のクラスを作成します
     */
    public Command.PlayerSender createPlayerSender(ProxiedPlayer player) {
        return new BungeePlayerSender(player, audienceResolver.getAudience(player));
    }


    @Override
    public void execute(CommandSender sender, String[] args) {
        command.processCommand(createSender(sender), args, null);
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return command.processCommandComplete(createSender(sender), args, null);
    }

    public static class BungeeSender implements Command.Sender {

        private final CommandSender sender;
        private final Audience audience;

        public BungeeSender(CommandSender sender, Audience audience) {
            this.sender = sender;
            this.audience = audience;
        }

        @Override
        public CommandSender getInstance() {
            return sender;
        }

        @Override
        public Audience getAudience() {
            return audience;
        }

        @Override
        public @Nullable Locale getLocale() {
            return null;
        }

        @Override
        public boolean hasPermission(String permission) {
            return sender.hasPermission(permission);
        }
    }

    public static class BungeePlayerSender extends BungeeSender implements Command.PlayerSender {

        private final ProxiedPlayer player;

        public BungeePlayerSender(ProxiedPlayer player, Audience audience) {
            super(player, audience);
            this.player = player;
        }

        @Override
        public ProxiedPlayer getInstance() {
            return player;
        }
    }

    public static Compat compat(Plugin plugin) {
        return new Compat(plugin);
    }

    /**
     * サーバーの Audience 互換性を解決してコマンドを登録するクラス
     */
    public static class Compat implements Command.AudienceResolver {

        private final Plugin plugin;
        private final Map<String, BungeeCommand> commands = new HashMap<>();
        private @Nullable Command.AudienceResolver audienceResolver;
        private @Nullable Object bungeeAudiences;

        public Compat(Plugin plugin) {
            this.plugin = plugin;
        }

        @SuppressWarnings("UnusedReturnValue")
        public BungeeCommand register(Command command) {
            BungeeCommand wrap = new BungeeCommand(command, this);
            wrap.register(plugin);
            commands.put(command.getName(), wrap);
            return wrap;
        }

        public Map<String, BungeeCommand> commands() {
            return commands;
        }

        public void init() {
            initAudiences();
        }

        public void close() {
            for (BungeeCommand command : commands.values()) {
                command.unregister(plugin);
            }
            commands.clear();
            closeAudiences();
        }

        protected void initAudiences() {
            if (audienceResolver != null)
                return;

            // try bungee audiences
            bungeeAudiences = null;
            try {
                bungeeAudiences = createBungeeAudiences(plugin);
                audienceResolver = createBungeeAudiencesResolver(bungeeAudiences);

            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(UNSUPPORTED_LIB_ERROR);
            }
        }

        protected void closeAudiences() {
            audienceResolver = null;
            if (bungeeAudiences != null) {
                try {
                    closeBungeeAudiences(bungeeAudiences);
                } catch (ReflectiveOperationException ignored) {
                } finally {
                    bungeeAudiences = null;
                }
            }
        }

        @Override
        public Audience getAudience(Object sender) {
            return Objects.requireNonNull(audienceResolver, "Audience resolver not initialized").getAudience(sender);
        }

    }

    /**
     * 外部ライブラリの BungeeAudience を使用して AudienceResolver を作成
     */
    public static Command.AudienceResolver createBungeeAudiencesResolver(Object bungeeAudiences) throws ReflectiveOperationException {
        Method senderMethod = Class.forName("net.kyori.adventure.platform.bungee.BungeeAudiences").getMethod("sender", CommandSender.class);
        return sender -> {
            try {
                //noinspection JavaReflectionInvocation
                return (Audience) senderMethod.invoke(bungeeAudiences, sender);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Failed to convert to audience", e);
            }
        };
    }

    public static Object createBungeeAudiences(Plugin plugin) throws ReflectiveOperationException {
        Class<?> bungeeAudiencesClass = Class.forName("net.kyori.adventure.platform.bungee.BungeeAudiences");
        Method createMethod = bungeeAudiencesClass.getMethod("create", Plugin.class);
        return createMethod.invoke(null, plugin);
    }

    public static void closeBungeeAudiences(Object bungeeAudiences) throws ReflectiveOperationException {
        bungeeAudiences.getClass().getMethod("close").invoke(bungeeAudiences);
    }

}