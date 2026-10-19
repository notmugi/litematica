package fi.dy.masa.litematica.gui;

import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.Nullable;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.MaterialListSchematic;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.util.FileType;
import fi.dy.masa.litematica.util.WorldUtils;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiStringListSelection;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase.DirectoryEntry;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.StringUtils;

/** Shared file browser/conversion support, not a second load screen. */
public abstract class GuiSchematicLoad extends GuiSchematicBrowserBase implements ISelectionListener<DirectoryEntry>
{
    protected GuiSchematicLoad()
    {
        super(12, 24);
    }

    @Override
    public String getBrowserContext()
    {
        return "schematic_load";
    }

    @Override
    public java.io.File getDefaultDirectory()
    {
        return DataManager.getSchematicsBaseDirectory();
    }

    @Override
    public int getMaxInfoHeight()
    {
        return this.getBrowserHeight() + 10;
    }

    @Override
    public void initGui()
    {
        super.initGui();
        this.createButtons();
    }

    protected abstract void createButtons();

    protected abstract void onSchematicLoaded(LitematicaSchematic schematic, Path file);

    protected void loadSelectedSchematic()
    {
        this.readSelection(false);
    }

    protected void previewSelectedMaterials()
    {
        this.readSelection(true);
    }

    @Override
    public void onSelectionChange(@Nullable DirectoryEntry entry)
    {
        this.clearButtons();
        this.createButtons();
    }

    @Override
    protected ISelectionListener<DirectoryEntry> getSelectionListener()
    {
        return this;
    }

    private void readSelection(boolean materials)
    {
        DirectoryEntry entry = this.getListWidget().getLastSelectedEntry();
        if (entry == null)
        {
            this.addMessage(MessageType.ERROR, "litematica.error.schematic_load.no_schematic_selected");
            return;
        }
        Path file = entry.getFullPath().toPath();
        try
        {
            if (!Files.isRegularFile(file) || !Files.isReadable(file))
            {
                this.addMessage(MessageType.ERROR, "litematica.error.schematic_load.cant_read_file", file.getFileName());
                return;
            }
            this.setNextMessageType(MessageType.ERROR);
            FileType type = FileType.fromFile(file);
            LitematicaSchematic schematic;
            switch (type)
            {
                case LITEMATICA_SCHEMATIC -> schematic = LitematicaSchematic.createFromFile(entry.getDirectory(), entry.getName());
                case SCHEMATICA_SCHEMATIC -> schematic = WorldUtils.convertSchematicaSchematicToLitematicaSchematic(entry.getDirectory(), entry.getName(), false, this);
                case VANILLA_STRUCTURE -> schematic = WorldUtils.convertStructureToLitematicaSchematic(entry.getDirectory(), entry.getName());
                case SPONGE_SCHEMATIC -> schematic = WorldUtils.convertSpongeSchematicToLitematicaSchematic(entry.getDirectory(), entry.getName());
                default -> {
                    this.addMessage(MessageType.ERROR, "litematica.error.schematic_load.unsupported_type", file.getFileName());
                    return;
                }
            }
            if (schematic == null)
            {
                this.addMessage(MessageType.ERROR, "litematica.error.schematic_load.cant_read_file", file.getFileName());
                return;
            }
            if (materials)
            {
                if (GuiBase.isShiftDown())
                {
                    GuiStringListSelection gui = new GuiStringListSelection(schematic.getAreas().keySet(), regions -> {
                        this.showMaterials(new MaterialListSchematic(schematic, regions, true));
                        return true;
                    });
                    gui.setTitle(StringUtils.translate("litematica.gui.title.material_list.select_schematic_regions", schematic.getMetadata().getName()));
                    gui.setParent(this);
                    GuiBase.openGui(gui);
                }
                else
                {
                    this.showMaterials(new MaterialListSchematic(schematic, true));
                }
            }
            else
            {
                this.onSchematicLoaded(schematic, file);
            }
            if (type != FileType.LITEMATICA_SCHEMATIC)
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, 15000, "litematica.message.warn.schematic_load_non_litematica");
            }
        }
        catch (RuntimeException e)
        {
            Litematica.LOGGER.warn("Failed to read schematic '{}'", file, e);
            this.addMessage(MessageType.ERROR, "litematica.error.schematic_load.cant_read_file", file.getFileName());
        }
    }

    private void showMaterials(MaterialListSchematic materialList)
    {
        DataManager.setMaterialList(materialList);
        GuiMaterialList gui = new GuiMaterialList(materialList);
        gui.setParent(this);
        GuiBase.openGui(gui);
    }
}
