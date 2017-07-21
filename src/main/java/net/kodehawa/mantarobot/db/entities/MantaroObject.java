package net.kodehawa.mantarobot.db.entities;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.db.ManagedObject;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.rethinkdb.RethinkDB.r;
import static net.kodehawa.mantarobot.data.MantaroData.conn;

@Getter
@ToString
@RequiredArgsConstructor
public class MantaroObject implements ManagedObject {
    public static final String DB_TABLE = "mantaro";
    public final List<String> blackListedGuilds;
    public final List<String> blackListedUsers;
    public final String id = "mantaro";
    private final Map<String, Long> tempBans;
    private Map<Long, Pair<String, Long>> mutes;

    public MantaroObject() {
        this.blackListedGuilds = new LinkedList<>();
        this.blackListedUsers = new LinkedList<>();
        this.tempBans = new HashMap<>();
        this.mutes = new HashMap<>();
    }

    @Override
    public void delete() {
        r.table(DB_TABLE).get(getId()).delete().run(conn());
        MantaroBot.getInstance().getStatsClient().increment("database_hits");
    }

    @Override
    public void save() {
        r.table(DB_TABLE).insert(this)
            .optArg("conflict", "replace")
            .run(conn());
        MantaroBot.getInstance().getStatsClient().increment("database_hits");
    }
}