package net.teamfruit.easystructure;

import com.sk89q.worldedit.extent.clipboard.Clipboard;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ESSession {
    public List<UUID> entities = new ArrayList<>();
    public String currentSchematic;
    public Clipboard currentClipboard;
}
