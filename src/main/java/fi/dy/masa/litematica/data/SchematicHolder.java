package fi.dy.masa.litematica.data;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.util.FileType;

public class SchematicHolder
{
    private static final SchematicHolder INSTANCE = new SchematicHolder();
    private final List<LitematicaSchematic> schematics = new ArrayList<>();

    public static SchematicHolder getInstance()
    {
        return INSTANCE;
    }

    public void clearLoadedSchematics()
    {
        this.schematics.clear();
    }

    @Nullable
    public LitematicaSchematic getOrLoad(File file)
    {
        for (LitematicaSchematic schematic : this.schematics)
        {
            if (file.equals(schematic.getFile()))
            {
                return schematic;
            }
        }

        FileType type = FileType.fromFile(file);
        LitematicaSchematic schematic = LitematicaSchematic.createFromFile(file.getParentFile(), file.getName(), type);

        if (schematic != null)
        {
            this.schematics.add(schematic);
        }

        return schematic;
    }

    public void addSchematic(LitematicaSchematic schematic, boolean allowDuplicates)
    {
        if (allowDuplicates || this.schematics.contains(schematic) == false)
        {
            if (allowDuplicates == false && schematic.getFile() != null)
            {
                for (LitematicaSchematic tmp : this.schematics)
                {
                    if (schematic.getFile().equals(tmp.getFile()))
                    {
                        return;
                    }
                }
            }

            this.schematics.add(schematic);
        }
    }

    /** User unload only; internal move cleanup and disconnect must remain synchronous. */
    public void requestUnload(LitematicaSchematic schematic, Runnable afterUnload)
    {
        Runnable unload = () -> { if (this.removeSchematic(schematic)) afterUnload.run(); };
        if (!schematic.getMetadata().wasModifiedSinceSaved())
        {
            unload.run();
            return;
        }
        fi.dy.masa.litematica.util.UxSafety.confirm(false, "litematica.gui.ux.safety.unload",
                () -> java.util.List.of(schematic.getMetadata(), schematic.getMetadata().getTimeModified(),
                        schematic.getMetadata().wasModifiedSinceSaved(),
                        DataManager.getSchematicPlacementManager().getAllPlacementsOfSchematic(schematic).stream()
                            .map(fi.dy.masa.litematica.util.UxSafety::placementState).toList()),
                () -> this.schematics.contains(schematic), unload, schematic.getMetadata().getName());
    }

    public boolean removeSchematic(LitematicaSchematic schematic)
    {
        if (this.schematics.remove(schematic))
        {
            DataManager.getSchematicPlacementManager().removeAllPlacementsOfSchematic(schematic);
            return true;
        }

        return false;
    }

    public Collection<LitematicaSchematic> getAllSchematics()
    {
        return this.schematics;
    }
}
