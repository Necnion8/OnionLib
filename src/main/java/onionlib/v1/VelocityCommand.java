package onionlib.v1;

import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.audience.Audience;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class VelocityCommand implements SimpleCommand {

    private final Command command;
    private final @NotNull Command.AudienceResolver audienceResolver;
    private final CommandMeta meta;
    private final @Nullable String permission;

    /**
     * {@link Command} を Velocity コマンドに変換します
     * @param audienceResolver {@link Audience} の変換に使用するアダプタ
     */
    public VelocityCommand(Command command, @NotNull Command.AudienceResolver audienceResolver, CommandMeta meta, @Nullable String permission) {
        this.command = command;
        this.audienceResolver = audienceResolver;
        this.meta = meta;
        this.permission = permission;
    }

    public void register(ProxyServer proxy) {
        proxy.getCommandManager().register(meta, this);
    }

    public void unregister(ProxyServer proxy) {
        proxy.getCommandManager().unregister(meta);
    }

    public CommandMeta getCommandMeta() {
        return meta;
    }

    /**
     * {@link Command} を Velocity コマンドに変換します
     */
    @SuppressWarnings("UnusedReturnValue")
    public static VelocityCommand wrap(CommandMeta meta, Command command, @Nullable String permission) {
        return new VelocityCommand(command, createNativeAudience(), meta, permission);
    }

    /**
     * {@link Command} を Velocity コマンドに変換します
     */
    @SuppressWarnings("UnusedReturnValue")
    public static VelocityCommand register(ProxyServer proxy, Command command, @Nullable String permission, Consumer<CommandMeta.Builder> builder) {
        CommandMeta.Builder metaBuilder = proxy.getCommandManager().metaBuilder(command.getName());
        builder.accept(metaBuilder);
        VelocityCommand c = new VelocityCommand(command, createNativeAudience(), metaBuilder.build(), permission);
        c.register(proxy);
        return c;
    }

    /**
     * {@link Command} を Velocity コマンドに変換します
     */
    @SuppressWarnings("UnusedReturnValue")
    public static VelocityCommand register(ProxyServer proxy, Command command, Consumer<CommandMeta.Builder> builder) {
        return register(proxy, command, null, builder);
    }


    /**
     * {@link CommandSource}のコマンドLib用のクラスを作成します<br>
     * プレイヤーである場合は {@link VelocityCommand#createPlayerSender(Player)} を使用します
     */
    public Command.Sender createSender(CommandSource sender) {
        if (sender instanceof Player)
            return createPlayerSender((Player) sender);
        return new VelocitySender(sender, audienceResolver.getAudience(sender));
    }

    /**
     * {@link Player}のコマンドLib用のクラスを作成します
     */
    public Command.PlayerSender createPlayerSender(Player player) {
        return new VelocityPlayerSender(player, audienceResolver.getAudience(player));
    }


    @Override
    public void execute(Invocation invocation) {
        command.processCommand(createSender(invocation.source()), invocation.arguments(),invocation.alias());
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return command.processCommandComplete(createSender(invocation.source()), invocation.arguments(), invocation.alias());
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return permission == null || invocation.source().hasPermission(permission);
    }


    public static class VelocitySender implements Command.Sender {

        private final CommandSource sender;
        private final Audience audience;

        public VelocitySender(CommandSource sender, Audience audience) {
            this.sender = sender;
            this.audience = audience;
        }

        @Override
        public CommandSource getInstance() {
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

    public static class VelocityPlayerSender extends VelocitySender implements Command.PlayerSender {

        private final Player player;

        public VelocityPlayerSender(Player player, Audience audience) {
            super(player, audience);
            this.player = player;
        }

        @Override
        public Player getInstance() {
            return player;
        }
    }

    /**
     * サーバーネイティブの Audience を使用して AudienceResolver を作成
     */
    public static Command.AudienceResolver createNativeAudience() {
        return sender -> (Audience) sender;
    }

}