package net.teamfruit.easystructure;

import com.sk89q.worldedit.session.SessionKey;
import com.sk89q.worldedit.session.SessionOwner;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;

public class ESSessionManager {
    private final Map<UUID, SessionHolder> sessions = new HashMap<>();

    protected UUID getKey(SessionOwner owner) {
        return getKey(owner.getSessionKey());
    }

    protected UUID getKey(SessionKey key) {
        return key.getUniqueId();
    }

    @Nullable
    public synchronized ESSession getIfPresent(SessionOwner owner) {
        checkNotNull(owner);
        SessionHolder stored = sessions.get(getKey(owner));
        if (stored != null) {
            return stored.session;
        } else {
            return null;
        }
    }

    public synchronized ESSession get(SessionOwner owner) {
        checkNotNull(owner);

        ESSession session = getIfPresent(owner);
        SessionKey sessionKey = owner.getSessionKey();

        // No session exists yet -- create one
        if (session == null) {
            session = new ESSession();

            // Remember the session regardless of if it's currently active or not.
            // And have the SessionTracker FLUSH inactive sessions.
            sessions.put(getKey(owner), new SessionHolder(sessionKey, session));
        }

        return session;
    }

    private static final class SessionHolder {
        private final SessionKey key;
        private final ESSession session;
        //private long lastActive = System.currentTimeMillis();

        private SessionHolder(SessionKey key, ESSession session) {
            this.key = key;
            this.session = session;
        }
    }
}
