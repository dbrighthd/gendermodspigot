package dbrighthd.wildfiregendermodplugin.networking.wildfire;

import dbrighthd.wildfiregendermodplugin.networking.minecraft.CraftInputStream;
import dbrighthd.wildfiregendermodplugin.networking.minecraft.CraftOutputStream;
import dbrighthd.wildfiregendermodplugin.wildfire.ModUser;
import dbrighthd.wildfiregendermodplugin.wildfire.setup.*;

import java.io.IOException;
import java.util.UUID;

/**
 * @author winnpixie
 */
public class ModSyncPacketV3 implements ModSyncPacket {
    @Override
    public int getVersion() {
        return 3;
    }

    @Override
    public String getModRange() {
        return "3.1.0 - 4.0.0";
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
        physicsBuilder.setBreastPhysics(input.readBoolean());
        generalBuilder.setShowInArmor(input.readBoolean());
        physicsBuilder.setBuoyancy(input.readFloat());
        physicsBuilder.setFloppiness(input.readFloat());
        breastBuilder.setXOffset(input.readFloat());
        breastBuilder.setYOffset(input.readFloat());
        breastBuilder.setZOffset(input.readFloat());
        breastBuilder.setUniBoob(input.readBoolean());
        breastBuilder.setCleavage(input.readFloat());

        return new ModUser(userId, new ModConfiguration(
                generalBuilder.create(),
                physicsBuilder.create(),
                breastBuilder.create(),
                UVLayouts.defaultLayouts()));
    }

    @Override
    public void write(ModUser user, CraftOutputStream output) throws IOException {
        ModConfiguration configuration = user.configuration();
        GeneralOptions general = configuration.generalOptions();
        PhysicsOptions physics = configuration.physicsOptions();
        BreastOptions breast = configuration.breastOptions();

        output.writeUUID(user.userId());
        output.writeEnum(general.genderIdentity());
        output.writeFloat(breast.bustSize());
        output.writeBoolean(general.hurtSounds());
        output.writeBoolean(physics.breastPhysics());
        output.writeBoolean(general.showInArmor());
        output.writeFloat(physics.buoyancy());
        output.writeFloat(physics.floppiness());
        output.writeFloat(breast.xOffset());
        output.writeFloat(breast.yOffset());
        output.writeFloat(breast.zOffset());
        output.writeBoolean(breast.uniBoob());
        output.writeFloat(breast.cleavage());
    }
}
