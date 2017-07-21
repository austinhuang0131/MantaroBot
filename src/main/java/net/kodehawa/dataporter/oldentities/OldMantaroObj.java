package net.kodehawa.dataporter.oldentities;

import lombok.Data;
import net.kodehawa.mantarobot.db.ManagedObject;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;

import static com.rethinkdb.RethinkDB.r;
import static net.kodehawa.mantarobot.data.MantaroData.conn;

@Data
public class OldMantaroObj implements ManagedObject {
    public static final String DB_TABLE = "mantaro";
    public final String id = "mantaro";
    public List<String> blackListedGuilds = null;
    public List<String> blackListedUsers = null;
    public List<String> patreonUsers = null;
    private Map<String, Long> tempBans = null;
    private Map<Long, Pair<String, Long>> mutes = null;

    @Override
    public void delete() {
        r.table(DB_TABLE).get(getId()).delete().runNoReply(conn());
    }

    @Override
    public void save() {
        r.table(DB_TABLE).insert(this)
                .optArg("conflict", "replace")
                .runNoReply(conn());
    }
}
