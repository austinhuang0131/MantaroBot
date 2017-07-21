package net.kodehawa.mantarobot.db.entities.helpers;

import lombok.Data;

@Data
public class ExtraUserData {
    private String birthday;
    private String timezone;
    private int reminderN;
    private long lockedUntil = 0;
}
