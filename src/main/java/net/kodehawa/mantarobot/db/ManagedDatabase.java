package net.kodehawa.mantarobot.db;

import br.com.brjdevs.java.snowflakes.Snowflakes;
import br.com.brjdevs.java.snowflakes.entities.Config;
import br.com.brjdevs.java.snowflakes.entities.Worker;
import com.rethinkdb.net.Connection;
import com.rethinkdb.net.Cursor;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.db.entities.*;

import java.util.List;

import static com.rethinkdb.RethinkDB.r;

public class ManagedDatabase {
    public static final Config MANTARO_FACTORY = Snowflakes.config(1495900000L, 2L, 2L, 12L);
    public static final Worker ID_WORKER = MANTARO_FACTORY.worker(0, 0), LOG_WORKER = MANTARO_FACTORY.worker(0, 2);

    private final Connection conn;
    private Guild guild;

    public ManagedDatabase(Connection conn) {
        this.conn = conn;
    }

    public CustomCommand getCustomCommand(String guildId, String name) {
        return r.table(CustomCommand.DB_TABLE).get(guildId + ":" + name).run(conn, CustomCommand.class);
    }

    public CustomCommand getCustomCommand(Guild guild, String name) {
        return getCustomCommand(guild.getId(), name);
    }

    public CustomCommand getCustomCommand(GuildData guild, String name) {
        return getCustomCommand(guild.getId(), name);
    }

    public CustomCommand getCustomCommand(GuildMessageReceivedEvent event, String cmd) {
        return getCustomCommand(event.getGuild(), cmd);
    }

    public List<CustomCommand> getCustomCommands() {
        Cursor<CustomCommand> c = r.table(CustomCommand.DB_TABLE).run(conn, CustomCommand.class);
        return c.toList();
    }

    public List<CustomCommand> getCustomCommands(String guildId) {
        String pattern = '^' + guildId + ':';
        Cursor<CustomCommand> c = r.table(CustomCommand.DB_TABLE).filter(quote -> quote.g("id").match(pattern)).run(conn, CustomCommand.class);
        return c.toList();
    }

    public List<CustomCommand> getCustomCommands(Guild guild) {
        return getCustomCommands(guild.getId());
    }

    public List<CustomCommand> getCustomCommandsByName(String name) {
        String pattern = ':' + name + '$';
        Cursor<CustomCommand> c = r.table(CustomCommand.DB_TABLE).filter(quote -> quote.g("id").match(pattern)).run(conn, CustomCommand.class);
        return c.toList();
    }

    public GuildData getGuild(String guildId) {
        GuildData guild = r.table(GuildData.DB_TABLE).get(guildId).run(conn, GuildData.class);
        return guild == null ? new GuildData(guildId) : guild;
    }

    public Marriage getMarriage(String user) {
        return r.table(Marriage.DB_TABLE)
            .getAll(user).optArg("index", "users")
            .nth(0).default_(((Object) null))
            .run(conn, Marriage.class);
    }

    public GuildData getGuild(Guild guild) {
        return getGuild(guild.getId());
    }

    public GuildData getGuild(Member member) {
        return getGuild(member.getGuild());
    }

    public GuildData getGuild(GuildMessageReceivedEvent event) {
        return getGuild(event.getGuild());
    }

    public MantaroObject getMantaroData() {
        MantaroObject obj = r.table(MantaroObject.DB_TABLE).get("mantaro").run(conn, MantaroObject.class);
        return obj == null ? new MantaroObject() : obj;
    }

    @Deprecated
    public UserData getPlayer(String userId) {
        return getUser(userId);
    }

    @Deprecated
    public UserData getPlayer(User user) {
        return getPlayer(user.getId());
    }

    @Deprecated
    public UserData getPlayer(Member member) {
        return getPlayer(member.getUser());
    }

    public List<PremiumKey> getPremiumKeys() {
        Cursor<PremiumKey> c = r.table(PremiumKey.DB_TABLE).run(conn, PremiumKey.class);
        return c.toList();
    }

    public List<QuotedMessage> getQuotes(String guildId) {
        String pattern = '^' + guildId + ':';
        Cursor<QuotedMessage> c = r.table(QuotedMessage.DB_TABLE).filter(quote -> quote.g("id").match(pattern)).run(conn, QuotedMessage.class);
        return c.toList();
    }

    public List<QuotedMessage> getQuotes(Guild guild) {
        return getQuotes(guild.getId());
    }

    public UserData getUser(String userId) {
        UserData user = r.table(UserData.DB_TABLE).get(userId).run(conn, UserData.class);
        return user == null ? new UserData(userId) : user;
    }

    public UserData getUser(User user) {
        return getUser(user.getId());
    }

    public UserData getUser(Member member) {
        return getUser(member.getUser());
    }
}
