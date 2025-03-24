package onionlib.sample;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import onionlib.v1.Command;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class SampleCommand extends Command {

    // object defines

    public enum GuildColor {
        RED(NamedTextColor.RED),
        GREEN(NamedTextColor.GREEN),
        BLUE(NamedTextColor.BLUE);

        private final NamedTextColor color;

        GuildColor(NamedTextColor color) {
            this.color = color;
        }

        public NamedTextColor getColor() {
            return color;
        }
    }

    public static class Guild {

        private final String id;
        private String name;
        private GuildColor color;
        private boolean friendlyFire;

        public Guild(String id, String name, GuildColor color) {
            this.id = id;
            this.name = name;
            this.color = color;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public GuildColor getColor() {
            return color;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setColor(GuildColor color) {
            this.color = color;
        }

        public boolean isFriendlyFire() {
            return friendlyFire;
        }

        public void setFriendlyFire(boolean allow) {
            this.friendlyFire = allow;
        }
    }

    public static class GuildManager {
        private final List<Guild> guilds = new ArrayList<>();

        public void add(Guild guild) {
            guilds.add(guild);
        }

        public void remove(Guild guild) {
            guilds.remove(guild);
        }

        public void clear() {
            guilds.clear();
        }

        public @Nullable Guild getGuild(String id) {
            return guilds.stream()
                    .filter(g -> g.getId().equals(id))
                    .findFirst()
                    .orElse(null);
        }

        public List<Guild> getGuilds() {
            return Collections.unmodifiableList(guilds);
        }
    }


    private final GuildManager guilds = new GuildManager();

    private final EntriesArg<GuildColor> COLOR_ARG = createEntriesArgument(GuildColor.class);
    private final Argument<Guild> GUILD_ARG = new Argument<Guild>() {
        @Override
        public Guild execute(Context context, String input) {
            Guild guild = guilds.getGuild(input);
            if (guild == null)
                throw new UnknownGuildError(input);
            return guild;
        }

        @Override
        public Stream<String> completeEntries(Context context, String input) {
            return guilds.getGuilds().stream().map(Guild::getId);
        }
    };

    public static class UnknownGuildError extends CommandError {

        private final String guildName;

        public UnknownGuildError(String guildName) {
            super(null);
            this.guildName = guildName;
        }

        public String getGuildName() {
            return guildName;
        }
    }

    private void initSample() {
        guilds.clear();

        Guild guild = new Guild("onion", "The Onion", GuildColor.GREEN);
        guilds.add(guild);

        guild = new Guild("slime", "Slimy", GuildColor.RED);
        guild.setFriendlyFire(true);
        guilds.add(guild);
    }

    // command defines

    /**
     * /<command> reload
     * /<command> guild (id)
     * /<command> guild (id) info
     * /<command> guild (id) spawn
     * /<command> guild (id) setcolor (color)
     * /<command> guild (id) setfriendlyfire <allow/deny>
     * /<command> listguilds
     * /<command> addguild (id) [name]
     * /<command> removeguild (id)
     * /<command> section aaa
     * /<command> section bbb a
     * /<command> section bbb b
     * /<command> section ccc (a) (b) a
     * /<command> section ccc (a) (b) b
     */
    public SampleCommand() {
        super("test", null);
        messagePrefix(Component.text()
                .append(Component.text("[", NamedTextColor.GRAY))
                .append(Component.text("OnionLib", NamedTextColor.AQUA))
                .append(Component.text("] ", NamedTextColor.GRAY)), true);

        addChild("reload", this::reload);
        addChildApply("guild", c -> {
            c.argument(GUILD_ARG);
            c.addChild("info", this::guildInfo)
                    .defaultCommand();
            c.addChildPlayer("spawn", this::guildSpawn);  // player only
            c.addChild("setcolor", this::guildSetColor)
                    .argument(COLOR_ARG);
            c.addChild("setfriendlyfire", this::guildSetFriendlyFire)
                    .argumentBool();
            c.addHelpCommand();
        });
        addChild("listguilds", this::listGuilds);
        addChild("addguild", this::addGuild)
                .argumentString("id")
                .argumentString("name");
        addChild("removeguild", this::removeGuild)
                .argument(GUILD_ARG);

        initSample();
    }

    // implement executor

    private void reload(Context ctx) {
        guilds.clear();
        initSample();
        ctx.send(Component.text("Configuration reloaded!", NamedTextColor.GREEN));
    }

    private void guildInfo(Context ctx) {
        Guild guild = ctx.get(GUILD_ARG);

        ctx.send(Component.text()
                .append(Component.text("Guild Info || ", NamedTextColor.WHITE))
                .append(Component.text(guild.getName(), NamedTextColor.GOLD))
                .append(Component.text(" (id: " + guild.getId() + ")", NamedTextColor.GRAY))
                .appendNewline()
                .append(Component.text("Guild Color || ", NamedTextColor.WHITE))
                .append(Component.text(guild.getColor().name(), guild.getColor().getColor()))
                .appendNewline()
                .append(Component.text("Friendly Fire || ", NamedTextColor.WHITE))
                .append(Component.text(guild.isFriendlyFire() ? "On" : "Off", guild.isFriendlyFire() ? NamedTextColor.YELLOW : NamedTextColor.AQUA))
                .build());
    }

    private void guildSpawn(PlayerSender sender, Context ctx) {
        ctx.send(Component.text("Teleported your guild spawn"));
    }

    private void guildSetColor(Context ctx) {
        Guild guild = ctx.get(GUILD_ARG);
        GuildColor color = ctx.get(COLOR_ARG);

        guild.setColor(color);

        ctx.send(Component.text("Changed guild color to ").color(NamedTextColor.WHITE)
                .append(Component.text(color.name(), color.getColor())));
    }

    private void guildSetFriendlyFire(Context ctx) {
        Guild guild = ctx.get(GUILD_ARG);
        boolean friendlyFire = ctx.get(BoolArg.class);

        guild.setFriendlyFire(friendlyFire);

        ctx.send(Component.text("Changed friendly fire to ").color(NamedTextColor.WHITE)
                .append(Component.text(guild.isFriendlyFire() ? "On" : "Off", guild.isFriendlyFire() ? NamedTextColor.YELLOW : NamedTextColor.AQUA)));
    }

    private void listGuilds(Context ctx) {
        TextComponent.Builder b = Component.text()
                .content("Guild List").color(NamedTextColor.GOLD);

        for (Guild guild : guilds.getGuilds()) {
            b.appendNewline();
            b.append(Component.text(" - ", NamedTextColor.GRAY));
            b.append(Component.text(guild.getName(), NamedTextColor.WHITE));
        }

        ctx.send(b.build());
    }

    private void addGuild(Context ctx) {
        String id = ctx.get("id", StringArg.class);
        String name = ctx.getOptional("name", StringArg.class).orElse(id);

        if (guilds.getGuild(id) != null) {
            ctx.send(Component.text("Already exists guild id!").color(NamedTextColor.DARK_RED));
            return;
        }

        Guild guild = new Guild(id, name, GuildColor.BLUE);
        guilds.add(guild);

        ctx.send(Component.text("Created new guild!").color(NamedTextColor.GREEN));
    }

    private void removeGuild(Context ctx) {
        Guild guild = ctx.get(GUILD_ARG);

        guilds.remove(guild);

        ctx.send(Component.text("Removed " + guild.getId() + " guild!").color(NamedTextColor.RED));
    }

    // implement error handling

    @Override
    public boolean processError(Context context, Throwable error) {
        if (error instanceof UnknownGuildError) {
            if (context.isExecuted()) {
                String name = ((UnknownGuildError) error).getGuildName();
                context.send(Component.text("Unknown guild: " + name, NamedTextColor.RED));
            }
            return true;
        }
        return super.processError(context, error);
    }
}
