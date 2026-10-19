package fi.dy.masa.litematica.gui;

import javax.annotation.Nullable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.GuiTextFieldInteger;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.gui.interfaces.ITextFieldListener;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.PositionUtils.CoordinateType;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.tool.ToolMode;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.litematica.gui.widgets.WidgetListPlacementSubRegions;
import fi.dy.masa.litematica.gui.widgets.WidgetPlacementSubRegion;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.util.PositionUtils;

public class GuiPlacementConfiguration  extends GuiListBase<SubRegionPlacement, WidgetPlacementSubRegion, WidgetListPlacementSubRegions>
                                        implements ISelectionListener<SubRegionPlacement>
{
    public final SchematicPlacement placement;
    private final PlacementUiContext context;
    private boolean advanced;
    private final java.util.List<UxButton> actions = new java.util.ArrayList<>();
    private final java.util.List<GuiTextFieldGeneric> fields = new java.util.ArrayList<>();
    private int focused = -1;
    public ButtonGeneric buttonResetPlacement;
    public GuiTextFieldGeneric textFieldRename;

    public GuiPlacementConfiguration(SchematicPlacement placement)
    {
        super(10, 82);
        this.placement = placement;
        this.context = new PlacementUiContext(placement);
        this.useTitleHierarchy = false;
        this.title = StringUtils.translate("litematica.gui.placement_editor.title");
    }

    @Override
    protected int getBrowserWidth()
    {
        return this.width - 150;
    }

    @Override
    protected int getBrowserHeight()
    {
        return this.height - 112;
    }

    @Override
    public void initGui()
    {
        this.actions.clear();
        this.fields.clear();
        this.focused = -1;
        this.buttonResetPlacement = null;
        super.initGui();
        if (!this.advanced)
        {
            this.createBasicPage();
            return;
        }

        int scaledWidth = GuiUtils.getScaledWindowWidth();
        int width = Math.min(300, scaledWidth - 230);
        int x = 12;
        int y = 22;

        this.textFieldRename = new GuiTextFieldGeneric(x, y + 2, width, 16, this.textRenderer);
        this.textFieldRename.setMaxLength(256);
        this.textFieldRename.setText(this.placement.getName());
        this.addTextField(this.textFieldRename, null);
        this.fields.add(this.textFieldRename);
        this.createButton(x + width + 4, y, -1, ButtonListener.Type.RENAME_PLACEMENT);

        String label = StringUtils.translate("litematica.gui.label.schematic_placement.sub_regions", this.placement.getSubRegionCount());
        this.addLabel(x + 2, y + 26, -1, 20, 0xFFFFFFFF, label);

        x = scaledWidth - 154;
        x -= this.createButton(x, y + 38, -1, ButtonListener.Type.TOGGLE_ALL_REGIONS_OFF) + 2;
        this.createButton(x, y + 38, -1, ButtonListener.Type.TOGGLE_ALL_REGIONS_ON);

        width = 120;
        x = this.width - width - 10;

        this.createButtonOnOff(x, y, width - 22, this.placement.isEnabled(), ButtonListener.Type.TOGGLE_ENABLED);
        this.createButton(x + width - 20, y, 20, ButtonListener.Type.TOGGLE_RENDERING);
        y += 21;

        this.createButtonOnOff(x, y, width - 22, this.placement.isLocked(), ButtonListener.Type.TOGGLE_LOCKED);
        this.createButton(x + width - 20, y + 2, 20, ButtonListener.Type.TOGGLE_ENCLOSING_BOX);
        y += 21;

        this.createButtonOnOff(x, y, width, this.placement.ignoreEntities(), ButtonListener.Type.TOGGLE_ENTITIES);
        y += 21;
        this.createButton(x, y, width, ButtonListener.Type.RESET_SUB_REGIONS);

        this.pageButtons(10, this.height - 24, Math.min(300, this.width - 20));

        this.updateElements();
    }

    @Override
    protected WidgetListPlacementSubRegions getListWidget()
    {
        return this.advanced ? super.getListWidget() : null;
    }

    private static String text(String key, Object... args)
    {
        return StringUtils.translate("litematica.gui.quick_place." + key, args);
    }

    private UxButton action(int x, int y, int width, String label, Runnable action)
    {
        return this.addButton(new UxButton(x, y, width, 20, label, label), (button, mouse) -> action.run());
    }

    private void pageButtons(int x, int y, int width)
    {
        int half = (width - 4) / 2;
        this.action(x, y, half, StringUtils.translate("litematica.gui.placement_editor." + (this.advanced ? "basic" : "advanced")), () -> {
            this.advanced = !this.advanced;
            this.initGui();
        });
        this.action(x + half + 4, y, half, text("done"), () -> openGui(this.getParent()));
    }

    private void createBasicPage()
    {
        int width = Math.min(420, this.width - 24);
        int x = (this.width - width) / 2;
        int half = (width - 4) / 2;
        this.addLabel(x, 24, width, 12, 0xFFFFFFFF, this.textRenderer.trimToWidth(this.placement.getName(), width));
        this.addLabel(x, 38, width, 12, 0xFFFFFFFF, text(this.previewHidden() ? "preview_hidden" : "ghost_only"));
        for (CoordinateType axis : CoordinateType.values())
        {
            int y = 54 + axis.ordinal() * 22;
            this.createCoordinateInput(x, y, half - 68, axis);
            this.action(x + half + 4, y, (half - 4) / 2, axis.name() + " −", () -> this.move(axis, -1))
                .setEnabled(!this.placement.isLocked() && !this.placement.isCoordinateLocked(axis));
            this.action(x + half + 8 + (half - 4) / 2, y, (half - 4) / 2, axis.name() + " +", () -> this.move(axis, 1))
                .setEnabled(!this.placement.isLocked() && !this.placement.isCoordinateLocked(axis));
        }
        this.createButton(x, 120, half, ButtonListener.Type.ROTATE);
        this.createButton(x + half + 4, 120, half, ButtonListener.Type.MIRROR);
        this.createButton(x, 142, half, ButtonListener.Type.MOVE_TO_PLAYER);
        this.createButtonOnOff(x + half + 4, 142, half, this.placement.isLocked(), ButtonListener.Type.TOGGLE_LOCKED);
        this.createButton(x, 164, half, ButtonListener.Type.OPEN_MATERIAL_LIST_GUI);
        this.createButton(x + half + 4, 164, half, ButtonListener.Type.OPEN_VERIFIER_GUI);
        int third = (width - 8) / 3;
        this.action(x, 186, third, text("paste"), () -> {
            if (this.context.valid(this))
                DataManager.getSchematicPlacementManager().pastePlacementToWorld(this.placement, false, true, this.mc);
        }).setEnabled(this.mc.player != null && EntityUtils.isCreativeMode(this.mc.player));
        this.action(x + third + 4, 186, third, text("tool_help"), this::useTool);
        this.action(x + 2 * (third + 4), 186, third, text("restore_preview"), this::restorePreview);
        this.pageButtons(x, 212, width);
    }

    @Override
    public void addGuiMessage(MessageType type, int duration, String key, Object... args)
    {
        super.addGuiMessage(type, duration, key, args);
        if (type == MessageType.ERROR || type == MessageType.WARNING)
            InfoUtils.showInGameMessage(type, (long) duration, key, args);
    }

    @Override
    public void setString(String message)
    {
        this.addMessage(MessageType.ERROR, message);
    }

    private boolean previewHidden()
    {
        return !Configs.Visuals.ENABLE_RENDERING.getBooleanValue() ||
               !Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.getBooleanValue() ||
               !Configs.Visuals.ENABLE_SCHEMATIC_BLOCKS.getBooleanValue();
    }

    private void restorePreview()
    {
        if (!this.context.valid(this)) return;
        Configs.Visuals.ENABLE_RENDERING.setBooleanValue(true);
        Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.setBooleanValue(true);
        Configs.Visuals.ENABLE_SCHEMATIC_BLOCKS.setBooleanValue(true);
        this.initGui();
    }

    private void useTool()
    {
        if (!this.context.valid(this)) return;
        if (this.placement.isLocked())
        {
            this.addMessage(MessageType.ERROR, "litematica.message.placement.cant_modify_is_locked");
            return;
        }
        if (DataManager.getSchematicProjectsManager().hasProjectOpen())
        {
            this.addMessage(MessageType.ERROR, "litematica.gui.quick_place.project_tool");
            return;
        }
        if (!Configs.Visuals.ENABLE_RENDERING.getBooleanValue())
        {
            this.addMessage(MessageType.ERROR, "litematica.gui.quick_place.render_tool");
            return;
        }
        DataManager.setToolMode(ToolMode.SCHEMATIC_PLACEMENT);
        Configs.Generic.TOOL_ITEM_ENABLED.setBooleanValue(true);
        this.placement.setSelectedSubRegionName(null);
        DataManager.getSchematicPlacementManager().setSelectedSchematicPlacement(this.placement);
        InfoUtils.showGuiAndInGameMessage(MessageType.INFO, 15000, "litematica.gui.quick_place.tool_instructions",
                (DataManager.getInstance().hasToolItemComponents()
                    ? DataManager.getInstance().getToolItemComponents() : DataManager.getToolItem()).getName().getString(),
                Hotkeys.TOOL_PLACE_CORNER_2.getKeybind().getKeysDisplayString());
    }

    private void move(CoordinateType axis, int amount)
    {
        if (isShiftDown()) amount *= 8;
        if (isAltDown()) amount *= 4;
        BlockPos old = this.placement.getOrigin();
        this.moveOrigin(switch (axis)
        {
            case X -> old.add(amount, 0, 0);
            case Y -> old.add(0, amount, 0);
            case Z -> old.add(0, 0, amount);
        });
        this.initGui();
    }

    private void moveOrigin(BlockPos pos)
    {
        if (!this.context.valid(this)) return;
        // Validate the effective position, not coordinates that are locked and won't move.
        BlockPos old = this.placement.getOrigin();
        pos = new BlockPos(this.placement.isCoordinateLocked(CoordinateType.X) ? old.getX() : pos.getX(),
                           this.placement.isCoordinateLocked(CoordinateType.Y) ? old.getY() : pos.getY(),
                           this.placement.isCoordinateLocked(CoordinateType.Z) ? old.getZ() : pos.getZ());
        if (this.mc.world.isOutOfHeightLimit(pos) || !this.mc.world.getWorldBorder().contains(pos))
        {
            this.addMessage(MessageType.ERROR, "litematica.gui.quick_place.outside_world");
            return;
        }
        this.setNextMessageType(MessageType.ERROR);
        this.placement.setOrigin(pos, this);
    }

    @Override
    public <T extends ButtonBase> T addButton(T button, IButtonActionListener listener)
    {
        if (button instanceof UxButton action) this.actions.add(action);
        return super.addButton(button, listener);
    }

    @Override
    public boolean onMouseClicked(int mouseX, int mouseY, int mouseButton)
    {
        this.focused = -1;
        this.actions.forEach(action -> action.setKeyboardFocused(false));
        // Also guards the legacy region-list buttons, without changing their shared widget.

        if (this.advanced && mouseY >= 82 && mouseY < this.height - 30 &&
            mouseX < this.width - 140 && !this.context.valid(this)) return true;
        return super.onMouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean onKeyTyped(int keyCode, int scanCode, int modifiers)
    {
        int count = this.fields.size();
        int fieldFocus = -1;
        for (int i = 0; i < count; ++i)
            if (this.fields.get(i).isFocused()) fieldFocus = i;
        if (keyCode == 258)
        {
            int current = fieldFocus >= 0 ? fieldFocus : this.focused;
            int step = GuiBase.isShiftDown() ? -1 : 1;
            int total = count + this.actions.size();
            this.focused = current < 0 ? (step > 0 ? 0 : total - 1) : Math.floorMod(current + step, total);
            for (int i = 0; i < count; ++i) this.fields.get(i).setFocused(i == this.focused);
            for (int i = 0; i < this.actions.size(); ++i) this.actions.get(i).setKeyboardFocused(i + count == this.focused);
            this.mc.getNarratorManager().narrate(this.focused < count ? (this.advanced ? StringUtils.translate("litematica.gui.button.rename")
                : text("coordinate_tip", CoordinateType.values()[this.focused].name()))
                : String.join(". ", this.actions.get(this.focused - count).getHoverStrings()));
            return true;
        }
        if (fieldFocus >= 0) return super.onKeyTyped(keyCode, scanCode, modifiers);
        if ((keyCode == 257 || keyCode == 335 || keyCode == 32) && this.focused >= count)
        {
            this.actions.get(this.focused - count).activate();
            return true;
        }
        if (this.advanced && keyCode != 256 && !this.context.valid(this)) return true;
        if (this.focused >= count && keyCode != 256) return true;
        return super.onKeyTyped(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean onCharTyped(char charIn, int modifiers)
    {
        if (this.focused >= this.fields.size()) return true;
        return super.onCharTyped(charIn, modifiers);
    }

    protected void createCoordinateInput(int x, int y, int width, CoordinateType type)
    {
        String label = type.name() + ":";
        this.addLabel(x, y, width, 20, 0xFFFFFFFF, label);
        int offset = this.getStringWidth(label) + 4;

        BlockPos pos = this.placement.getOrigin();
        String text = switch (type)
        {
	        case X -> String.valueOf(pos.getX());
	        case Y -> String.valueOf(pos.getY());
	        case Z -> String.valueOf(pos.getZ());
        };

	    GuiTextFieldInteger textField = new GuiTextFieldInteger(x + offset, y + 2, width, 14, this.textRenderer);
        textField.setText(text);
        TextFieldListener listener = new TextFieldListener(type, this.placement, this);
        textField.setEditable(!this.placement.isLocked() && !this.placement.isCoordinateLocked(type));
        this.addWidget(new fi.dy.masa.malilib.gui.widgets.WidgetHoverInfo(textField.getX(), textField.getY(), textField.getWidth(), textField.getHeight(), text("coordinate_tip", type.name())));
        this.addTextField(textField, listener);
        this.fields.add(textField);

        String labelKey = this.placement.isCoordinateLocked(type) ? "unlock_axis" : "lock_axis";
        this.action(x + offset + width + 4, y, 48,
                StringUtils.translate("litematica.gui.placement_editor." + labelKey, type.name()), () -> {
                    if (!this.context.valid(this)) return;
                    this.placement.setCoordinateLocked(type, !this.placement.isCoordinateLocked(type));
                    this.initGui();
                });
    }

    public int createButtonOnOff(int x, int y, int width, boolean isCurrentlyOn, ButtonListener.Type type)
    {
        String label = type.getDisplayName(StringUtils.translate("litematica.message.value." + (isCurrentlyOn ? "on" : "off")));
        ButtonGeneric button = new UxButton(x, y, width, 20, label, label);
        String hoverString = type.getHoverText();

        if (hoverString != null)
        {
            button.setHoverStrings(hoverString);
        }

        this.addButton(button, new ButtonListener(type, this.placement, this));

        return button.getWidth();
    }

    public int createButton(int x, int y, int width, ButtonListener.Type type)
    {
        ButtonListener listener = new ButtonListener(type, this.placement, this);
        String label = "";

        switch (type)
        {
            case TOGGLE_ENCLOSING_BOX:
            {
                Icons icon = this.placement.shouldRenderEnclosingBox() ? Icons.ENCLOSING_BOX_ENABLED : Icons.ENCLOSING_BOX_DISABLED;
                boolean enabled = this.placement.shouldRenderEnclosingBox();
                String str = (enabled ? TXT_GREEN : TXT_RED) + StringUtils.translate("litematica.message.value." + (enabled ? "on" : "off")) + TXT_RST;
                String hover = StringUtils.translate("litematica.gui.button.schematic_placement.hover.enclosing_box", str);

                this.addButton(new ButtonGeneric(x, y, icon, hover), listener);

                return icon.getWidth();
            }

            case TOGGLE_RENDERING:
            {
                boolean enabled = this.placement.isRenderingEnabled();
                String pre = enabled ? TXT_GREEN : TXT_RED;
                label = pre + type.getDisplayName() + TXT_RST;
                String str = pre + StringUtils.translate("litematica.message.value." + (enabled ? "on" : "off")) + TXT_RST;
                String hover = StringUtils.translate("litematica.gui.button.schematic_placement.hover.rendering", str);

                this.addButton(new ButtonGeneric(x, y, width, 20, label, hover), listener);
                return width;
            }

            case ROTATE:
            {
                String value = PositionUtils.getRotationNameShort(this.placement.getRotation());
                label = type.getDisplayName(value);
                break;
            }

            case MIRROR:
            {
                String value = PositionUtils.getMirrorName(this.placement.getMirror());
                label = type.getDisplayName(value);
                break;
            }

            case NUDGE_COORD_X:
            case NUDGE_COORD_Y:
            case NUDGE_COORD_Z:
            {
                String hover = StringUtils.translate("litematica.gui.button.hover.plus_minus_tip");
                ButtonGeneric button = new ButtonGeneric(x, y, Icons.BUTTON_PLUS_MINUS_16, hover);
                this.addButton(button, listener);
                return width;
            }

            default:
                label = type.getDisplayName();
        }

        if (width == -1)
        {
            width = this.getStringWidth(label) + 10;
        }

        // These are right-aligned
        if (type == ButtonListener.Type.TOGGLE_ALL_REGIONS_OFF || type == ButtonListener.Type.TOGGLE_ALL_REGIONS_ON)
        {
            x -= width;
        }

        ButtonGeneric button = new UxButton(x, y, width, 20, label, label);

        if (type == ButtonListener.Type.ROTATE || type == ButtonListener.Type.MIRROR || type == ButtonListener.Type.MOVE_TO_PLAYER)
            button.setEnabled(!this.placement.isLocked());
        this.addButton(button, listener);

        if (type == ButtonListener.Type.RESET_SUB_REGIONS)
        {
            this.buttonResetPlacement = button;
        }

        return width;
    }

    protected void updateElements()
    {
        if (this.buttonResetPlacement == null) return;
        String label = StringUtils.translate("litematica.gui.button.schematic_placement.reset_sub_region_placements");
        boolean enabled = true;

        if (this.placement.isRegionPlacementModified())
        {
            label = TXT_GOLD + label + TXT_RST;
        }
        else
        {
            enabled = false;
        }

        this.buttonResetPlacement.setDisplayString(label);
        this.buttonResetPlacement.setEnabled(enabled && !this.placement.isLocked());
    }

    public SchematicPlacement getSchematicPlacement()
    {
        return this.placement;
    }

    @Override
    protected ISelectionListener<SubRegionPlacement> getSelectionListener()
    {
        return this;
    }

    @Override
    public void onSelectionChange(SubRegionPlacement entry)
    {
        if (!this.context.valid(this)) return;
        this.placement.setSelectedSubRegionName(entry != null && entry.getName().equals(this.placement.getSelectedSubRegionName()) == false ? entry.getName() : null);
    }

    @Override
    protected WidgetListPlacementSubRegions createListWidget(int listX, int listY)
    {
        return new WidgetListPlacementSubRegions(listX, listY, this.getBrowserWidth(), this.getBrowserHeight(), this);
    }

    public record ButtonListener(Type type, SchematicPlacement placement, GuiPlacementConfiguration parent)
            implements IButtonActionListener
    {
        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton)
        {
            MinecraftClient mc = MinecraftClient.getInstance();

            if (!this.parent.context.valid(this.parent))
            {
                return;
            }

            int amount = mouseButton == 1 ? -1 : 1;

            if (GuiBase.isShiftDown())
            {
                amount *= 8;
            }
            if (GuiBase.isAltDown())
            {
                amount *= 4;
            }

            BlockPos oldOrigin = this.placement.getOrigin();
            this.parent.setNextMessageType(MessageType.ERROR);

            switch (this.type)
            {
                case RENAME_PLACEMENT:
                    this.placement.setName(this.parent.textFieldRename.getText());
                    break;

                case ROTATE:
                {
                    boolean reverse = mouseButton == 1;
                    BlockRotation rotation = PositionUtils.cycleRotation(this.placement.getRotation(), reverse);
                    this.placement.setRotation(rotation, this.parent);
                    break;
                }

                case MIRROR:
                {
                    boolean reverse = mouseButton == 1;
                    BlockMirror mirror = PositionUtils.cycleMirror(this.placement.getMirror(), reverse);
                    this.placement.setMirror(mirror, this.parent);
                    break;
                }

                case MOVE_TO_PLAYER:
                {
                    BlockPos pos = BlockPos.ofFloored(mc.player.getPos());
                    this.parent.moveOrigin(pos);
                    break;
                }

                case NUDGE_COORD_X:
                    this.parent.moveOrigin(oldOrigin.add(amount, 0, 0));
                    break;

                case NUDGE_COORD_Y:
                    this.parent.moveOrigin(oldOrigin.add(0, amount, 0));
                    break;

                case NUDGE_COORD_Z:
                    this.parent.moveOrigin(oldOrigin.add(0, 0, amount));
                    break;

                case TOGGLE_ENABLED:
                    this.placement.toggleEnabled();
                    break;

                case TOGGLE_RENDERING:
                    this.placement.setRenderSchematic(!this.placement.isRenderingEnabled());
                    break;

                case TOGGLE_LOCKED:
                    this.placement.toggleLocked();
                    break;

                case TOGGLE_ENTITIES:
                    this.placement.toggleIgnoreEntities(this.parent);
                    break;

                case TOGGLE_ENCLOSING_BOX:
                    this.placement.toggleRenderEnclosingBox();
                    break;

                case RESET_SUB_REGIONS:
                    this.placement.resetAllSubRegionsToSchematicValues(this.parent);
                    break;

                case TOGGLE_ALL_REGIONS_ON:
                case TOGGLE_ALL_REGIONS_OFF:
                {
                    boolean state = this.type == Type.TOGGLE_ALL_REGIONS_ON;
                    this.placement.setSubRegionsEnabledState(state, this.parent.getListWidget().getCurrentEntries(), this.parent);
                    break;
                }

                case OPEN_VERIFIER_GUI:
                {
                    GuiSchematicVerifier gui = new GuiSchematicVerifier(this.placement);
                    gui.setParent(this.parent);
                    GuiBase.openGui(gui);
                    return;
                }

                case OPEN_MATERIAL_LIST_GUI:
                {
                    MaterialListBase materialList = this.placement.getMaterialList();
                    materialList.reCreateMaterialList();
                    GuiMaterialList gui = new GuiMaterialList(materialList);
                    DataManager.setMaterialList(materialList); // Remember the last opened material list for the hotkey to (re-) open it
                    gui.setParent(this.parent);
                    GuiBase.openGui(gui);
                    return;
                }
            }

            this.parent.initGui(); // Re-create buttons/text fields
        }

        public enum Type
        {
            RENAME_PLACEMENT        ("litematica.gui.button.rename"),
            ROTATE                  ("litematica.gui.button.rotation_value"),
            MIRROR                  ("litematica.gui.button.mirror_value"),
            MOVE_TO_PLAYER          ("litematica.gui.button.move_to_player"),
            NUDGE_COORD_X           (""),
            NUDGE_COORD_Y           (""),
            NUDGE_COORD_Z           (""),
            TOGGLE_ENABLED          ("litematica.gui.button.schematic_placements.placement_enabled"),
            TOGGLE_RENDERING        ("litematica.gui.button.schematic_placement.abbr.rendering"),
            TOGGLE_LOCKED           ("litematica.gui.button.schematic_placements.locked", "litematica.gui.button.schematic_placement.hover.lock"),
            TOGGLE_ENTITIES         ("litematica.gui.button.schematic_placement.ignore_entities"),
            TOGGLE_ENCLOSING_BOX    (""),
            TOGGLE_ALL_REGIONS_ON   ("litematica.gui.button.schematic_placement.toggle_all_on"),
            TOGGLE_ALL_REGIONS_OFF  ("litematica.gui.button.schematic_placement.toggle_all_off"),
            RESET_SUB_REGIONS       ("litematica.gui.button.schematic_placement.reset_sub_region_placements"),
            OPEN_VERIFIER_GUI       ("litematica.gui.button.schematic_verifier"),
            OPEN_MATERIAL_LIST_GUI  ("litematica.gui.button.material_list");

            private final String translationKey;
            @Nullable
            private final String hoverText;

            Type(String translationKey)
            {
                this(translationKey, null);
            }

            Type(String translationKey, @Nullable String hoverText)
            {
                this.translationKey = translationKey;
                this.hoverText = hoverText;
            }

            public String getTranslationKey()
            {
                return this.translationKey;
            }

            public String getDisplayName(Object... args)
            {
                return StringUtils.translate(this.translationKey, args);
            }

            @Nullable
            public String getHoverText()
            {
                return this.hoverText != null ? StringUtils.translate(this.hoverText) : null;
            }
        }
    }

    public record TextFieldListener(CoordinateType type, SchematicPlacement placement, GuiPlacementConfiguration parent)
            implements ITextFieldListener<GuiTextFieldInteger>
    {
        @Override
        public boolean onTextChange(GuiTextFieldInteger textField)
        {
            try
            {
                int value = Integer.parseInt(textField.getText());
                BlockPos posOld = this.placement.getOrigin();
                this.parent.setNextMessageType(MessageType.ERROR);

                switch (this.type)
                {
                    case X:
                        this.parent.moveOrigin(new BlockPos(value, posOld.getY(), posOld.getZ()));
                        break;
                    case Y:
                        this.parent.moveOrigin(new BlockPos(posOld.getX(), value, posOld.getZ()));
                        break;
                    case Z:
                        this.parent.moveOrigin(new BlockPos(posOld.getX(), posOld.getY(), value));
                        break;
                }

                this.parent.updateElements();
            }
            catch (NumberFormatException ignored)
            {
            }

            return false;
        }
    }

}
