package dbrighthd.wildfiregendermodplugin.networking.wildfire;

import dbrighthd.wildfiregendermodplugin.networking.minecraft.CraftInputStream;
import dbrighthd.wildfiregendermodplugin.networking.minecraft.CraftOutputStream;
import dbrighthd.wildfiregendermodplugin.wildfire.ModUser;
import dbrighthd.wildfiregendermodplugin.wildfire.setup.*;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public class ModSyncPacketV5 implements ModSyncPacket {
    @Override
    public int getVersion() {
        return 5;
    }

    @Override
    public String getModRange() {
        return "5.0.0 - ?.?.?";
    }

    @Override
    public ModUser read(CraftInputStream input) throws IOException {
        GeneralOptions.Builder generalBuilder = new GeneralOptions.Builder();
        PhysicsOptions.Builder physicsBuilder = new PhysicsOptions.Builder();
        BreastOptions.Builder breastBuilder = new BreastOptions.Builder();

        UUID userId = input.readUUID();
        generalBuilder.setGenderIdentity(input.readEnum(GenderIdentities.class));
        breastBuilder.setBustSize(input.readFloat());
        generalBuilder.setHurtSounds(input.readBoolean());
        generalBuilder.setVoicePitch(input.readFloat());

        // Physics record
        physicsBuilder.setBreastPhysics(input.readBoolean());
        boolean showInArmor = input.readBoolean();
        generalBuilder.setShowInArmor(showInArmor);
        physicsBuilder.setArmorPhysics(showInArmor);
        physicsBuilder.setBuoyancy(input.readFloat()); // Now bounceMultiplier
        physicsBuilder.setFloppiness(input.readFloat()); // Now floppyMultiplier

        // Breasts record
        breastBuilder.setXOffset(input.readFloat());
        breastBuilder.setYOffset(input.readFloat());
        breastBuilder.setZOffset(input.readFloat());
        breastBuilder.setUniBoob(input.readBoolean());
        breastBuilder.setCleavage(input.readFloat());

        byte[] tailData = input.readAllBytes();
        UVLayouts uvLayouts = UVLayouts.defaultLayouts(); // We still populate default for configuration needs initially
        if (tailData.length > 0) {
            try (CraftInputStream probe = CraftInputStream.ofBytes(tailData)) {
                uvLayouts = readUVLayouts(probe);
            } catch (IOException ignored) {
                // Fallback to defaults if parsing fails or reaches EOF
                uvLayouts = UVLayouts.defaultLayouts();
            }
        }

        return new ModUser(userId, new ModConfiguration(
                generalBuilder.create(),
                physicsBuilder.create(),
                breastBuilder.create(),
                uvLayouts));
    }

    @Override
    public void write(ModUser user, CraftOutputStream output) throws IOException {
        ModConfiguration configuration = user.configuration();
        GeneralOptions general = configuration.generalOptions();
        PhysicsOptions physics = configuration.physicsOptions();
        BreastOptions breast = configuration.breastOptions();
        UVLayouts uvLayouts = configuration.uvLayouts();

        output.writeUUID(user.userId());
        output.writeEnum(general.genderIdentity());
        output.writeFloat(breast.bustSize());
        output.writeBoolean(general.hurtSounds());
        output.writeFloat(general.voicePitch());

        // Physics record
        output.writeBoolean(physics.breastPhysics());
        output.writeBoolean(general.showInArmor());
        output.writeFloat(physics.buoyancy());
        output.writeFloat(physics.floppiness());

        // Breasts record
        output.writeFloat(breast.xOffset());
        output.writeFloat(breast.yOffset());
        output.writeFloat(breast.zOffset());
        output.writeBoolean(breast.uniBoob());
        output.writeFloat(breast.cleavage());

        // UV Layouts
        writeUVLayouts(uvLayouts, output);
    }

    private UVLayouts readUVLayouts(CraftInputStream input) throws IOException {
        UVLayouts.Layer skin = readLayer(input);
        UVLayouts.Layer overlay = readLayer(input);
        return new UVLayouts(skin, overlay);
    }

    private UVLayouts.Layer readLayer(CraftInputStream input) throws IOException {
        UVLayout left = readUVLayout(input);
        UVLayout right = readUVLayout(input);
        return new UVLayouts.Layer(left, right);
    }

    private UVLayout readUVLayout(CraftInputStream input) throws IOException {
        int count = input.readVarInt();
        if (count < 0 || count > UVDirection.values().length) {
            // There are only UVDirection.values().length valid directions, so any
            // larger count is corrupt/truncated data — treat it as such rather than
            // looping on it.
            throw new IOException("Invalid UV layout quad count: " + count);
        }
        Map<UVDirection, UVQuad> quads = new EnumMap<>(UVDirection.class);
        for (int i = 0; i < count; i++) {
            UVDirection direction = input.readEnum(UVDirection.class);
            UVQuad quad = new UVQuad(
                    input.readVarInt(),
                    input.readVarInt(),
                    input.readVarInt(),
                    input.readVarInt());
            if (quads.put(direction, quad) != null)
                throw new IOException("Duplicate UV direction: " + direction);
        }
        return new UVLayout(quads);
    }

    private void writeUVLayouts(UVLayouts layouts, CraftOutputStream output) throws IOException {
        if (layouts == null) {
            // Write empty layouts if null
            writeLayer(null, output);
            writeLayer(null, output);
            return;
        }
        writeLayer(layouts.skin(), output);
        writeLayer(layouts.overlay(), output);
    }

    private void writeLayer(UVLayouts.Layer layer, CraftOutputStream output) throws IOException {
        if (layer == null) {
            writeUVLayout(null, output);
            writeUVLayout(null, output);
            return;
        }
        writeUVLayout(layer.left(), output);
        writeUVLayout(layer.right(), output);
    }

    private void writeUVLayout(UVLayout layout, CraftOutputStream output) throws IOException {
        if (layout == null || layout.getQuads() == null) {
            output.writeVarInt(0);
            return;
        }
        Map<UVDirection, UVQuad> quads = layout.getQuads();
        output.writeVarInt(quads.size());
        for (Map.Entry<UVDirection, UVQuad> entry : quads.entrySet()) {
            output.writeVarInt(entry.getKey().ordinal());
            UVQuad quad = entry.getValue();
            output.writeVarInt(quad.x1());
            output.writeVarInt(quad.y1());
            output.writeVarInt(quad.x2());
            output.writeVarInt(quad.y2());
        }
    }
}
