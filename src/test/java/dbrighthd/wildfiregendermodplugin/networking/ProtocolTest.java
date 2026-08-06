package dbrighthd.wildfiregendermodplugin.networking;

import dbrighthd.wildfiregendermodplugin.networking.minecraft.CraftInputStream;
import dbrighthd.wildfiregendermodplugin.networking.wildfire.ModSyncPacketV1;
import dbrighthd.wildfiregendermodplugin.networking.wildfire.ModSyncPacketV2;
import dbrighthd.wildfiregendermodplugin.networking.wildfire.ModSyncPacketV3;
import dbrighthd.wildfiregendermodplugin.networking.wildfire.ModSyncPacketV4;
import dbrighthd.wildfiregendermodplugin.networking.wildfire.ModSyncPacketV5;
import dbrighthd.wildfiregendermodplugin.wildfire.ModUser;
import dbrighthd.wildfiregendermodplugin.wildfire.UserManager;
import dbrighthd.wildfiregendermodplugin.wildfire.setup.GenderIdentities;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ProtocolTest {
        @Test
        public void testVarNumberRoundTripsAtNumericLimits() throws IOException {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                try (dbrighthd.wildfiregendermodplugin.networking.minecraft.CraftOutputStream output =
                                new dbrighthd.wildfiregendermodplugin.networking.minecraft.CraftOutputStream(bytes)) {
                        output.writeVarInt(Integer.MIN_VALUE);
                        output.writeVarInt(Integer.MAX_VALUE);
                        output.writeVarLong(Long.MIN_VALUE);
                        output.writeVarLong(Long.MAX_VALUE);
                }

                try (CraftInputStream input = CraftInputStream.ofBytes(bytes.toByteArray())) {
                        assertEquals(Integer.MIN_VALUE, input.readVarInt());
                        assertEquals(Integer.MAX_VALUE, input.readVarInt());
                        assertEquals(Long.MIN_VALUE, input.readVarLong());
                        assertEquals(Long.MAX_VALUE, input.readVarLong());
                }
        }

        @Test
        public void testVarNumbersRejectOverflowBits() {
                byte[] overflowingInt = {
                                (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, 0x10
                };
                byte[] overflowingLong = {
                                (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80,
                                (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, 0x02
                };

                assertThrows(IOException.class,
                                () -> CraftInputStream.ofBytes(overflowingInt).readVarInt());
                assertThrows(IOException.class,
                                () -> CraftInputStream.ofBytes(overflowingLong).readVarLong());
        }

        @Test
        public void testV5Parsing() throws IOException {
                // Sample V5 packet captured from research:
                // UUID: 00000000-0000-0000-0000-000000000000
                // Gender: FEMALE
                // Bust: 0.5f
                // HurtSounds: true
                // VoicePitch: 1.0f
                // Physics: (true, true, 1.0f, 1.0f)
                // Breasts: (0.0f, 0.0f, 0.0f, false, 0.0f)
                // UV Layouts: Empty layers (0 items in map)

                byte[] data = new byte[] {
                                // UUID (2 longs = 16 bytes)
                                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                                // Gender (FEMALE = index 0 in mod)
                                0x00,
                                // BustSize (0.5f)
                                0x3F, 0x00, 0x00, 0x00,
                                // HurtSounds (true)
                                0x01,
                                // VoicePitch (1.0f)
                                0x3F, (byte) 0x80, 0x00, 0x00,
                                // Physics (true, true, 1.0f, 1.0f)
                                0x01, 0x01,
                                0x3F, (byte) 0x80, 0x00, 0x00,
                                0x3F, (byte) 0x80, 0x00, 0x00,
                                // Breasts (0.0f, 0.0f, 0.0f, false, 0.0f)
                                0x00, 0x00, 0x00, 0x00,
                                0x00, 0x00, 0x00, 0x00,
                                0x00, 0x00, 0x00, 0x00,
                                0x00,
                                0x00, 0x00, 0x00, 0x00,
                                // UV Layouts (Skin: Left count=0, Right count=0; Overlay: Left count=0, Right
                                // count=0)
                                0x00, 0x00, 0x00, 0x00
                };

                ModSyncPacketV5 packet = new ModSyncPacketV5();
                try (CraftInputStream input = CraftInputStream.ofBytes(data)) {
                        ModUser user = packet.read(input);
                        assertNotNull(user);
                        assertEquals(new UUID(0, 0), user.userId());
                        assertEquals(GenderIdentities.FEMALE, user.configuration().generalOptions().genderIdentity());
                        assertEquals(0.5f, user.configuration().breastOptions().bustSize());
                        assertTrue(user.configuration().generalOptions().hurtSounds());
                        assertEquals(1.0f, user.configuration().generalOptions().voicePitch());
                        assertTrue(user.configuration().physicsOptions().breastPhysics());
                        assertTrue(user.configuration().generalOptions().showInArmor());
                        assertEquals(1.0f, user.configuration().physicsOptions().buoyancy());
                        assertEquals(1.0f, user.configuration().physicsOptions().floppiness());
                        assertNotNull(user.configuration().uvLayouts());
                }
        }

        @Test
        public void testV5RoundTrip() throws IOException {
                java.util.Map<dbrighthd.wildfiregendermodplugin.wildfire.setup.UVDirection, dbrighthd.wildfiregendermodplugin.wildfire.setup.UVQuad> quads = new java.util.EnumMap<>(
                                dbrighthd.wildfiregendermodplugin.wildfire.setup.UVDirection.class);
                quads.put(dbrighthd.wildfiregendermodplugin.wildfire.setup.UVDirection.NORTH,
                                new dbrighthd.wildfiregendermodplugin.wildfire.setup.UVQuad(1, 2, 3, 4));

                dbrighthd.wildfiregendermodplugin.wildfire.setup.UVLayout layout = new dbrighthd.wildfiregendermodplugin.wildfire.setup.UVLayout(
                                quads);
                dbrighthd.wildfiregendermodplugin.wildfire.setup.UVLayouts.Layer layer = new dbrighthd.wildfiregendermodplugin.wildfire.setup.UVLayouts.Layer(
                                layout, layout);
                dbrighthd.wildfiregendermodplugin.wildfire.setup.UVLayouts uvLayouts = new dbrighthd.wildfiregendermodplugin.wildfire.setup.UVLayouts(
                                layer, layer);

                dbrighthd.wildfiregendermodplugin.wildfire.setup.ModConfiguration config = new dbrighthd.wildfiregendermodplugin.wildfire.setup.ModConfiguration(
                                new dbrighthd.wildfiregendermodplugin.wildfire.setup.GeneralOptions(
                                                GenderIdentities.MALE, true, 1.2f,
                                                true),
                                new dbrighthd.wildfiregendermodplugin.wildfire.setup.PhysicsOptions(true, true, 0.8f,
                                                0.9f),
                                new dbrighthd.wildfiregendermodplugin.wildfire.setup.BreastOptions(0.7f, 0.1f, 0.2f,
                                                0.3f, true, 0.4f),
                                uvLayouts);

                UUID userId = UUID.randomUUID();
                ModUser user = new ModUser(userId, config);
                ModSyncPacketV5 packet = new ModSyncPacketV5();

                byte[] serialized;
                try (java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
                                dbrighthd.wildfiregendermodplugin.networking.minecraft.CraftOutputStream out = new dbrighthd.wildfiregendermodplugin.networking.minecraft.CraftOutputStream(
                                                bytes)) {
                        packet.write(user, out);
                        serialized = bytes.toByteArray();
                }

                ModUser deserialized;
                try (CraftInputStream in = CraftInputStream.ofBytes(serialized)) {
                        deserialized = packet.read(in);
                }

                assertEquals(userId, deserialized.userId());
                assertEquals(GenderIdentities.MALE, deserialized.configuration().generalOptions().genderIdentity());
                assertEquals(0.7f, deserialized.configuration().breastOptions().bustSize());
                assertEquals(0.8f, deserialized.configuration().physicsOptions().buoyancy());

                assertNotNull(deserialized.configuration().uvLayouts());
                dbrighthd.wildfiregendermodplugin.wildfire.setup.UVQuad quad = deserialized.configuration().uvLayouts()
                                .skin()
                                .left().getQuads()
                                .get(dbrighthd.wildfiregendermodplugin.wildfire.setup.UVDirection.NORTH);
                assertNotNull(quad);
                assertEquals(1, quad.x1());
                assertEquals(2, quad.y1());
                assertEquals(3, quad.x2());
                assertEquals(4, quad.y2());
        }

        @Test
        public void testUserManagerProtocolTracking() {
                UserManager userManager = new UserManager();
                UUID uuid = UUID.randomUUID();

                assertEquals(-1, userManager.getProtocolVersion(uuid));
                userManager.setProtocolVersion(uuid, 5);
                assertEquals(5, userManager.getProtocolVersion(uuid));
                userManager.removePlayer(uuid);
                assertEquals(-1, userManager.getProtocolVersion(uuid));
        }

        @Test
        public void testV4RoundTrip() throws IOException {
                dbrighthd.wildfiregendermodplugin.wildfire.setup.ModConfiguration config = new dbrighthd.wildfiregendermodplugin.wildfire.setup.ModConfiguration(
                                new dbrighthd.wildfiregendermodplugin.wildfire.setup.GeneralOptions(
                                                GenderIdentities.FEMALE, true, 1.0f,
                                                true),
                                new dbrighthd.wildfiregendermodplugin.wildfire.setup.PhysicsOptions(true, true, 1.0f,
                                                1.0f),
                                new dbrighthd.wildfiregendermodplugin.wildfire.setup.BreastOptions(0.5f, 0.0f, 0.0f,
                                                0.0f, false, 0.0f),
                                dbrighthd.wildfiregendermodplugin.wildfire.setup.UVLayouts.defaultLayouts());

                UUID userId = UUID.randomUUID();
                ModUser user = new ModUser(userId, config);
                ModSyncPacketV4 packet = new ModSyncPacketV4();

                byte[] serialized;
                try (java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
                                dbrighthd.wildfiregendermodplugin.networking.minecraft.CraftOutputStream out = new dbrighthd.wildfiregendermodplugin.networking.minecraft.CraftOutputStream(
                                                bytes)) {
                        packet.write(user, out);
                        serialized = bytes.toByteArray();
                }

                ModUser deserialized;
                try (CraftInputStream in = CraftInputStream.ofBytes(serialized)) {
                        deserialized = packet.read(in);
                }

                assertEquals(userId, deserialized.userId());
                assertEquals(GenderIdentities.FEMALE, deserialized.configuration().generalOptions().genderIdentity());
                // UV Layouts should now use defaults in V4
                assertNotNull(deserialized.configuration().uvLayouts());
                assertNotNull(deserialized.configuration().uvLayouts().skin());
                assertNotNull(deserialized.configuration().uvLayouts().skin().left());
                assertEquals(5, deserialized.configuration().uvLayouts().skin().left().getQuads().size());
        }

        @Test
        public void testDefaultProtocolDetection() {
                assertEquals(5, NetworkManager.detectDefaultProtocol("1.21.9-R0.1-SNAPSHOT"));
                assertEquals(5, NetworkManager.detectDefaultProtocol("1.22.0"));
                assertEquals(4, NetworkManager.detectDefaultProtocol("1.21.2"));
                assertEquals(4, NetworkManager.detectDefaultProtocol("1.21.5"));
                assertEquals(3, NetworkManager.detectDefaultProtocol("1.20.2"));
                assertEquals(3, NetworkManager.detectDefaultProtocol("1.20.4"));
                assertEquals(2, NetworkManager.detectDefaultProtocol("1.18.1"));
                assertEquals(2, NetworkManager.detectDefaultProtocol("1.19.4"));
                assertEquals(2, NetworkManager.detectDefaultProtocol("invalid"));
        }

        /**
         * Regression: Minecraft switched to a "YEAR.RELEASE" version scheme
         * (e.g. 26.1, 26.2) starting in 2026, dropping the "1." prefix entirely.
         * detectDefaultProtocol previously assumed every version string had the
         * legacy "1.MINOR.PATCH" shape, so a bare "26.1" was misparsed and fell
         * through to protocol 2 instead of protocol 5 (the sync packet format
         * hasn't changed since protocol 5 was introduced).
         */
        @Test
        public void testDefaultProtocolDetectionForYearReleaseScheme() {
                assertEquals(5, NetworkManager.detectDefaultProtocol("26.1-R0.1-SNAPSHOT"));
                assertEquals(5, NetworkManager.detectDefaultProtocol("26.2-R0.1-SNAPSHOT"));
                assertEquals(5, NetworkManager.detectDefaultProtocol("26.2"));
                assertEquals(5, NetworkManager.detectDefaultProtocol("26.1"));
                // Future year releases should also resolve to protocol 5 by default.
                assertEquals(5, NetworkManager.detectDefaultProtocol("27.4"));
        }

        @Test
        public void testIsImplementedProtocol() {
                assertFalse(NetworkManager.isImplementedProtocol(0));
                assertFalse(NetworkManager.isImplementedProtocol(1), "Protocol 1 is a stub and must not be usable");
                assertTrue(NetworkManager.isImplementedProtocol(2));
                assertTrue(NetworkManager.isImplementedProtocol(3));
                assertTrue(NetworkManager.isImplementedProtocol(4));
                assertTrue(NetworkManager.isImplementedProtocol(5));
                assertFalse(NetworkManager.isImplementedProtocol(6));
                assertFalse(NetworkManager.isImplementedProtocol(99));
        }

        @Test
        public void testV1ThrowsUnsupportedOperation() {
                ModSyncPacketV1 packet = new ModSyncPacketV1();
                assertThrows(UnsupportedOperationException.class, () -> packet.read(null));
                assertThrows(UnsupportedOperationException.class, () -> packet.write(null, null));
        }

        @Test
        public void testV4ModRangeMatchesReadmeFloor() {
                assertEquals("4.0.0 - 4.3.4", new ModSyncPacketV4().getModRange());
        }

        @Test
        public void testV2WriteRoundTrip() throws IOException {
                dbrighthd.wildfiregendermodplugin.wildfire.setup.ModConfiguration config =
                                new dbrighthd.wildfiregendermodplugin.wildfire.setup.ModConfiguration(
                                                new dbrighthd.wildfiregendermodplugin.wildfire.setup.GeneralOptions(
                                                                GenderIdentities.FEMALE, true, 1.0f, true),
                                                new dbrighthd.wildfiregendermodplugin.wildfire.setup.PhysicsOptions(true, true, 0.4f,
                                                                0.6f),
                                                new dbrighthd.wildfiregendermodplugin.wildfire.setup.BreastOptions(0.5f, 0.1f, 0.2f,
                                                                0.3f, true, 0.2f),
                                                dbrighthd.wildfiregendermodplugin.wildfire.setup.UVLayouts.defaultLayouts());

                UUID userId = UUID.randomUUID();
                ModUser user = new ModUser(userId, config);
                ModSyncPacketV2 packet = new ModSyncPacketV2();

                byte[] serialized;
                try (java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
                                dbrighthd.wildfiregendermodplugin.networking.minecraft.CraftOutputStream out = new dbrighthd.wildfiregendermodplugin.networking.minecraft.CraftOutputStream(
                                                bytes)) {
                        packet.write(user, out);
                        serialized = bytes.toByteArray();
                }

                ModUser deserialized;
                try (CraftInputStream in = CraftInputStream.ofBytes(serialized)) {
                        deserialized = packet.read(in);
                }

                assertEquals(userId, deserialized.userId());
                assertEquals(GenderIdentities.FEMALE, deserialized.configuration().generalOptions().genderIdentity());
                assertEquals(0.5f, deserialized.configuration().breastOptions().bustSize());
                assertEquals(0.4f, deserialized.configuration().physicsOptions().buoyancy());
                assertEquals(0.6f, deserialized.configuration().physicsOptions().floppiness());
                assertTrue(deserialized.configuration().physicsOptions().armorPhysics());
        }

        @Test
        public void testV3WriteRoundTrip() throws IOException {
                dbrighthd.wildfiregendermodplugin.wildfire.setup.ModConfiguration config =
                                new dbrighthd.wildfiregendermodplugin.wildfire.setup.ModConfiguration(
                                                new dbrighthd.wildfiregendermodplugin.wildfire.setup.GeneralOptions(
                                                                GenderIdentities.MALE, false, 1.0f, false),
                                                new dbrighthd.wildfiregendermodplugin.wildfire.setup.PhysicsOptions(false, false, 0.333f,
                                                                0.75f),
                                                new dbrighthd.wildfiregendermodplugin.wildfire.setup.BreastOptions(0.6f, 0.0f, 0.0f,
                                                                0.0f, true, 0.0f),
                                                dbrighthd.wildfiregendermodplugin.wildfire.setup.UVLayouts.defaultLayouts());

                UUID userId = UUID.randomUUID();
                ModUser user = new ModUser(userId, config);
                ModSyncPacketV3 packet = new ModSyncPacketV3();

                byte[] serialized;
                try (java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
                                dbrighthd.wildfiregendermodplugin.networking.minecraft.CraftOutputStream out = new dbrighthd.wildfiregendermodplugin.networking.minecraft.CraftOutputStream(
                                                bytes)) {
                        packet.write(user, out);
                        serialized = bytes.toByteArray();
                }

                ModUser deserialized;
                try (CraftInputStream in = CraftInputStream.ofBytes(serialized)) {
                        deserialized = packet.read(in);
                }

                assertEquals(userId, deserialized.userId());
                assertEquals(GenderIdentities.MALE, deserialized.configuration().generalOptions().genderIdentity());
                assertEquals(0.6f, deserialized.configuration().breastOptions().bustSize());
                assertFalse(deserialized.configuration().generalOptions().hurtSounds());
                assertFalse(deserialized.configuration().physicsOptions().breastPhysics());
        }

        /**
         * Regression: a V5 UV layout section with an out-of-range quad count
         * (more than the 5 known UVDirection values) is corrupt data, not a
         * valid layout. It must fall back to defaults rather than looping on
         * the bogus count or throwing an uncaught exception.
         */
        @Test
        public void testV5MalformedUvCountFallsBackToDefaults() throws IOException {
                java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
                try (dbrighthd.wildfiregendermodplugin.networking.minecraft.CraftOutputStream out =
                                new dbrighthd.wildfiregendermodplugin.networking.minecraft.CraftOutputStream(bytes)) {
                        // UUID
                        out.writeUUID(UUID.randomUUID());
                        // Gender, bust, hurtSounds, voicePitch
                        out.writeEnum(GenderIdentities.FEMALE);
                        out.writeFloat(0.5f);
                        out.writeBoolean(true);
                        out.writeFloat(1.0f);
                        // Physics: breastPhysics, showInArmor, buoyancy, floppiness
                        out.writeBoolean(true);
                        out.writeBoolean(true);
                        out.writeFloat(1.0f);
                        out.writeFloat(1.0f);
                        // Breasts: x, y, z, uniBoob, cleavage
                        out.writeFloat(0.0f);
                        out.writeFloat(0.0f);
                        out.writeFloat(0.0f);
                        out.writeBoolean(false);
                        out.writeFloat(0.0f);
                        // UV layouts: skin.left count is bogus (way more than 5 valid directions)
                        out.writeVarInt(1000);
                }

                ModSyncPacketV5 packet = new ModSyncPacketV5();
                ModUser user;
                try (CraftInputStream input = CraftInputStream.ofBytes(bytes.toByteArray())) {
                        user = packet.read(input);
                }

                assertNotNull(user, "read() must not throw for a malformed UV count");
                assertEquals(
                                dbrighthd.wildfiregendermodplugin.wildfire.setup.UVLayouts.defaultLayouts(),
                                user.configuration().uvLayouts(),
                                "Malformed UV count must fall back to default layouts");
        }

        @Test
        public void testLengthBasedDetection() {
                assertEquals(2, NetworkManager.detectProtocolFromLength(50));
                assertEquals(3, NetworkManager.detectProtocolFromLength(49));
                assertEquals(4, NetworkManager.detectProtocolFromLength(53));
                assertEquals(5, NetworkManager.detectProtocolFromLength(54));
                assertEquals(5, NetworkManager.detectProtocolFromLength(100));
                assertEquals(-1, NetworkManager.detectProtocolFromLength(36));
                assertEquals(-1, NetworkManager.detectProtocolFromLength(10));
        }

        @Test
        public void testLegacyToV5TranslationDefaults() throws IOException {
                // V2 packet bytes: UUID=0, Gender=MALE(0), Bust=0.5, HurtSounds=true,
                // Physics=true, Armor=true, ShowInArmor=true,
                // Bounce=0.333, Floppy=0.75, X=0, Y=0, Z=0, UniBoob=true, Cleavage=0
                // For V2 read, it skips some fields that were added later (like voicePitch and
                // UVs)
                byte[] v2Data = new byte[] {
                                // UUID
                                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                                // Gender (FEMALE = 0)
                                0x00,
                                // BustSize (0.6f)
                                0x3F, 0x19, (byte) 0x99, (byte) 0x9A,
                                // HurtSounds (true)
                                0x01,
                                // BreastPhysics (true)
                                0x01,
                                // ArmorPhysics (true)
                                0x01,
                                // ShowInArmor (true)
                                0x01,
                                // Buoyancy (0.333f ~ 0x3EAA7EFA)
                                0x3E, (byte) 0xAA, 0x7E, (byte) 0xFA,
                                // Floppiness (0.75f)
                                0x3F, 0x40, 0x00, 0x00,
                                // XOffset (0.0f)
                                0x00, 0x00, 0x00, 0x00,
                                // YOffset (0.0f)
                                0x00, 0x00, 0x00, 0x00,
                                // ZOffset (0.0f)
                                0x00, 0x00, 0x00, 0x00,
                                // UniBoob (true)
                                0x01,
                                // Cleavage (0.0f)
                                0x00, 0x00, 0x00, 0x00
                };

                dbrighthd.wildfiregendermodplugin.networking.wildfire.ModSyncPacketV2 packet = new dbrighthd.wildfiregendermodplugin.networking.wildfire.ModSyncPacketV2();
                ModUser user;
                try (CraftInputStream input = CraftInputStream.ofBytes(v2Data)) {
                        user = packet.read(input);
                }

                assertNotNull(user);
                // Verify V5-only defaults were applied during V2 read fallback
                assertEquals(1.0f, user.configuration().generalOptions().voicePitch(), 0.001f,
                                "Voice pitch should default to 1.0f");
                assertEquals(0.6f, user.configuration().breastOptions().bustSize(), 0.001f);
                assertEquals(0.333f, user.configuration().physicsOptions().buoyancy(), 0.001f);
                assertEquals(0.75f, user.configuration().physicsOptions().floppiness(), 0.001f);
                assertTrue(user.configuration().generalOptions().hurtSounds());
                assertTrue(user.configuration().generalOptions().showInArmor());
                assertTrue(user.configuration().physicsOptions().breastPhysics());
                assertTrue(user.configuration().physicsOptions().armorPhysics());
                assertTrue(user.configuration().breastOptions().uniBoob());

                // UV Layouts should be defaulted
                dbrighthd.wildfiregendermodplugin.wildfire.setup.UVLayouts defaultLayouts = dbrighthd.wildfiregendermodplugin.wildfire.setup.UVLayouts
                                .defaultLayouts();
                assertEquals(defaultLayouts, user.configuration().uvLayouts());
        }

        @Test
        public void testBuilderDefaults() {
                // GeneralOptions Defaults
                dbrighthd.wildfiregendermodplugin.wildfire.setup.GeneralOptions general = new dbrighthd.wildfiregendermodplugin.wildfire.setup.GeneralOptions.Builder()
                                .create();
                assertEquals(GenderIdentities.MALE, general.genderIdentity(), "Default gender should be MALE");
                assertTrue(general.hurtSounds(), "Default hurtSounds should be true");
                assertEquals(1.0f, general.voicePitch(), 0.001f, "Default voicePitch should be 1.0f");
                assertTrue(general.showInArmor(), "Default showInArmor should be true");

                // PhysicsOptions Defaults
                dbrighthd.wildfiregendermodplugin.wildfire.setup.PhysicsOptions physics = new dbrighthd.wildfiregendermodplugin.wildfire.setup.PhysicsOptions.Builder()
                                .create();
                assertTrue(physics.breastPhysics(), "Default breastPhysics should be true");
                assertFalse(physics.armorPhysics(), "Default armorPhysics should be false");
                assertEquals(0.333f, physics.buoyancy(), 0.001f, "Default buoyancy should be 0.333f");
                assertEquals(0.75f, physics.floppiness(), 0.001f, "Default floppiness should be 0.75f");

                // BreastOptions Defaults
                dbrighthd.wildfiregendermodplugin.wildfire.setup.BreastOptions breasts = new dbrighthd.wildfiregendermodplugin.wildfire.setup.BreastOptions.Builder()
                                .create();
                assertEquals(0.6f, breasts.bustSize(), 0.001f, "Default bustSize should be 0.6f");
                assertEquals(0.0f, breasts.xOffset(), 0.001f, "Default xOffset should be 0.0f");
                assertEquals(0.0f, breasts.yOffset(), 0.001f, "Default yOffset should be 0.0f");
                assertEquals(0.0f, breasts.zOffset(), 0.001f, "Default zOffset should be 0.0f");
                assertTrue(breasts.uniBoob(), "Default uniBoob should be true");
                assertEquals(0.0f, breasts.cleavage(), 0.001f, "Default cleavage should be 0.0f");

                // UVLayout Defaults
                dbrighthd.wildfiregendermodplugin.wildfire.setup.UVLayouts layouts = dbrighthd.wildfiregendermodplugin.wildfire.setup.UVLayouts
                                .defaultLayouts();
                assertEquals(dbrighthd.wildfiregendermodplugin.wildfire.setup.UVLayout.defaultSkinLeft(),
                                layouts.skin().left());
                assertEquals(dbrighthd.wildfiregendermodplugin.wildfire.setup.UVLayout.defaultSkinRight(),
                                layouts.skin().right());
                assertEquals(dbrighthd.wildfiregendermodplugin.wildfire.setup.UVLayout.defaultOverlayLeft(),
                                layouts.overlay().left());
                assertEquals(dbrighthd.wildfiregendermodplugin.wildfire.setup.UVLayout.defaultOverlayRight(),
                                layouts.overlay().right());
        }

        @Test
        public void testUuidPositionForProtocolDetection() throws IOException {
                // This test validates the assumption used by protocol detection:
                // UUID is always the first 16 bytes of the packet for all protocol versions.

                UUID testUuid = UUID.randomUUID();
                dbrighthd.wildfiregendermodplugin.wildfire.setup.ModConfiguration config = new dbrighthd.wildfiregendermodplugin.wildfire.setup.ModConfiguration(
                                new dbrighthd.wildfiregendermodplugin.wildfire.setup.GeneralOptions.Builder().create(),
                                new dbrighthd.wildfiregendermodplugin.wildfire.setup.PhysicsOptions.Builder().create(),
                                new dbrighthd.wildfiregendermodplugin.wildfire.setup.BreastOptions.Builder().create(),
                                dbrighthd.wildfiregendermodplugin.wildfire.setup.UVLayouts.defaultLayouts());
                ModUser user = new ModUser(testUuid, config);

                // Test V2
                byte[] v2Data = serializeWithPacket(user,
                                new dbrighthd.wildfiregendermodplugin.networking.wildfire.ModSyncPacketV2());
                assertEquals(testUuid, extractUuidFromBytes(v2Data), "V2: UUID should be at bytes 0-15");
                assertEquals(2, NetworkManager.detectProtocolFromLength(v2Data.length));

                // Test V3
                byte[] v3Data = serializeWithPacket(user,
                                new dbrighthd.wildfiregendermodplugin.networking.wildfire.ModSyncPacketV3());
                assertEquals(testUuid, extractUuidFromBytes(v3Data), "V3: UUID should be at bytes 0-15");
                assertEquals(3, NetworkManager.detectProtocolFromLength(v3Data.length));

                // Test V4
                byte[] v4Data = serializeWithPacket(user, new ModSyncPacketV4());
                assertEquals(testUuid, extractUuidFromBytes(v4Data), "V4: UUID should be at bytes 0-15");
                assertEquals(4, NetworkManager.detectProtocolFromLength(v4Data.length));

                // Test V5
                byte[] v5Data = serializeWithPacket(user, new ModSyncPacketV5());
                assertEquals(testUuid, extractUuidFromBytes(v5Data), "V5: UUID should be at bytes 0-15");
                assertEquals(5, NetworkManager.detectProtocolFromLength(v5Data.length));
        }

        private byte[] serializeWithPacket(ModUser user,
                        dbrighthd.wildfiregendermodplugin.networking.wildfire.ModSyncPacket packet)
                        throws IOException {
                try (java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
                                dbrighthd.wildfiregendermodplugin.networking.minecraft.CraftOutputStream out = new dbrighthd.wildfiregendermodplugin.networking.minecraft.CraftOutputStream(
                                                bytes)) {
                        packet.write(user, out);
                        return bytes.toByteArray();
                }
        }

        private UUID extractUuidFromBytes(byte[] data) throws IOException {
                try (CraftInputStream in = CraftInputStream.ofBytes(data)) {
                        return new UUID(in.readLong(), in.readLong());
                }
        }

        // -------------------------------------------------------------------------
        // Regression tests — based on real bugs discovered during Beta.1 support
        // -------------------------------------------------------------------------

        /**
         * Regression: Beta.1 client sends a truncated V5 packet (UV section cut short).
         * The server must not throw, must not assign null layouts, and must fall back
         * to defaults — not crash the deserialization pipeline.
         */
        @Test
        public void testV5TruncatedUvFallsBackToDefaults() throws IOException {
                // Full V5 header (53 bytes) + only 2 VarInt(0)s instead of 4.
                // Simulates a Beta.1 client whose UV section is smaller than the server expects.
                byte[] data = new byte[] {
                                // UUID
                                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                                // Gender (FEMALE)
                                0x00,
                                // BustSize (0.5f)
                                0x3F, 0x00, 0x00, 0x00,
                                // HurtSounds (true)
                                0x01,
                                // VoicePitch (1.0f)
                                0x3F, (byte) 0x80, 0x00, 0x00,
                                // Physics (true, true, 1.0f, 1.0f)
                                0x01, 0x01,
                                0x3F, (byte) 0x80, 0x00, 0x00,
                                0x3F, (byte) 0x80, 0x00, 0x00,
                                // Breasts (0.0f, 0.0f, 0.0f, false, 0.0f)
                                0x00, 0x00, 0x00, 0x00,
                                0x00, 0x00, 0x00, 0x00,
                                0x00, 0x00, 0x00, 0x00,
                                0x00,
                                0x00, 0x00, 0x00, 0x00,
                                // UV Layouts — TRUNCATED: only 2 VarInt(0)s, not the full 4
                                0x00, 0x00
                };

                ModSyncPacketV5 packet = new ModSyncPacketV5();
                ModUser user;
                try (CraftInputStream input = CraftInputStream.ofBytes(data)) {
                        user = packet.read(input);
                }

                assertNotNull(user, "read() must not return null for a truncated packet");
                assertNotNull(user.configuration().uvLayouts(),
                                "uvLayouts must not be null — should fall back to defaults");
                assertNotNull(user.configuration().uvLayouts().skin(),
                                "uvLayouts.skin() must not be null after fallback");
        }

        /**
         * Regression: After reading a truncated packet, write() must produce output
         * that can be successfully re-read. This is the idempotency contract.
         * If write() ever echoes raw bytes for the wrong recipient this fails.
         */
        @Test
        public void testV5WriteFromTruncatedReadIsRereadable() throws IOException {
                byte[] truncated = new byte[] {
                                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                                0x00,
                                0x3F, 0x00, 0x00, 0x00,
                                0x01,
                                0x3F, (byte) 0x80, 0x00, 0x00,
                                0x01, 0x01,
                                0x3F, (byte) 0x80, 0x00, 0x00,
                                0x3F, (byte) 0x80, 0x00, 0x00,
                                0x00, 0x00, 0x00, 0x00,
                                0x00, 0x00, 0x00, 0x00,
                                0x00, 0x00, 0x00, 0x00,
                                0x00,
                                0x00, 0x00, 0x00, 0x00,
                                // Truncated UV
                                0x00, 0x00
                };

                ModSyncPacketV5 packet = new ModSyncPacketV5();

                ModUser fromTruncated;
                try (CraftInputStream in = CraftInputStream.ofBytes(truncated)) {
                        fromTruncated = packet.read(in);
                }

                // Write then re-read — must not throw
                byte[] rewritten = serializeWithPacket(fromTruncated, packet);
                assertNotNull(rewritten);
                assertTrue(rewritten.length > 0, "Rewritten packet must not be empty");

                ModUser reread;
                try (CraftInputStream in = CraftInputStream.ofBytes(rewritten)) {
                        reread = packet.read(in);
                }
                assertNotNull(reread, "Re-read of rewritten packet must succeed");
                assertNotNull(reread.configuration().uvLayouts());
        }

        /**
         * Regression: write() output must be idempotent.
         * Reading then writing twice must produce identical bytes.
         * This catches any state leakage (e.g., rawUvBytes) that causes
         * the first and second write to differ.
         */
        @Test
        public void testV5WriteIsIdempotent() throws IOException {
                dbrighthd.wildfiregendermodplugin.wildfire.setup.ModConfiguration config =
                                new dbrighthd.wildfiregendermodplugin.wildfire.setup.ModConfiguration(
                                                new dbrighthd.wildfiregendermodplugin.wildfire.setup.GeneralOptions.Builder().create(),
                                                new dbrighthd.wildfiregendermodplugin.wildfire.setup.PhysicsOptions.Builder().create(),
                                                new dbrighthd.wildfiregendermodplugin.wildfire.setup.BreastOptions.Builder().create(),
                                                dbrighthd.wildfiregendermodplugin.wildfire.setup.UVLayouts.defaultLayouts());

                ModUser user = new ModUser(UUID.randomUUID(), config);
                ModSyncPacketV5 packet = new ModSyncPacketV5();

                byte[] first = serializeWithPacket(user, packet);

                // Read back, write again
                ModUser reread;
                try (CraftInputStream in = CraftInputStream.ofBytes(first)) {
                        reread = packet.read(in);
                }
                byte[] second = serializeWithPacket(reread, packet);

                assertEquals(first.length, second.length,
                                "Two successive writes of the same logical data must produce the same byte count");
                assertArrayEquals(first, second,
                                "Two successive writes of the same logical data must be byte-identical");
        }

        /**
         * Regression: Cross-player sync must never send Player A's raw byte snapshot
         * to Player B. Concretely: a ModUser deserialized from one packet size must
         * produce a packet readable by any other V5 client regardless of the source size.
         *
         * This is the exact regression that caused the second DecoderException
         * (rawUvBytes echoed verbatim across different-version clients).
         */
        @Test
        public void testV5CrossPlayerSyncNeverEchoesStaleRawBytes() throws IOException {
                // "Player A" — full modern packet with actual UV data (1 quad in each layout)
                java.util.Map<dbrighthd.wildfiregendermodplugin.wildfire.setup.UVDirection,
                                dbrighthd.wildfiregendermodplugin.wildfire.setup.UVQuad> quads =
                                new java.util.EnumMap<>(
                                                dbrighthd.wildfiregendermodplugin.wildfire.setup.UVDirection.class);
                quads.put(dbrighthd.wildfiregendermodplugin.wildfire.setup.UVDirection.NORTH,
                                new dbrighthd.wildfiregendermodplugin.wildfire.setup.UVQuad(1, 2, 3, 4));
                dbrighthd.wildfiregendermodplugin.wildfire.setup.UVLayout layout =
                                new dbrighthd.wildfiregendermodplugin.wildfire.setup.UVLayout(quads);
                dbrighthd.wildfiregendermodplugin.wildfire.setup.UVLayouts.Layer layer =
                                new dbrighthd.wildfiregendermodplugin.wildfire.setup.UVLayouts.Layer(layout, layout);
                dbrighthd.wildfiregendermodplugin.wildfire.setup.UVLayouts uvLayouts =
                                new dbrighthd.wildfiregendermodplugin.wildfire.setup.UVLayouts(layer, layer);

                ModUser playerA = new ModUser(UUID.randomUUID(),
                                new dbrighthd.wildfiregendermodplugin.wildfire.setup.ModConfiguration(
                                                new dbrighthd.wildfiregendermodplugin.wildfire.setup.GeneralOptions.Builder().create(),
                                                new dbrighthd.wildfiregendermodplugin.wildfire.setup.PhysicsOptions.Builder().create(),
                                                new dbrighthd.wildfiregendermodplugin.wildfire.setup.BreastOptions.Builder().create(),
                                                uvLayouts));

                ModSyncPacketV5 packet = new ModSyncPacketV5();

                // Serialize Player A, as if the server is syncing A's data to Player B
                byte[] sentToPlayerB = serializeWithPacket(playerA, packet);

                // Player B must be able to read this without any exception
                ModUser receivedByPlayerB;
                try (CraftInputStream in = CraftInputStream.ofBytes(sentToPlayerB)) {
                        receivedByPlayerB = packet.read(in);
                }

                assertNotNull(receivedByPlayerB);
                assertNotNull(receivedByPlayerB.configuration().uvLayouts());

                // The UV quad must survive the round-trip unaltered
                dbrighthd.wildfiregendermodplugin.wildfire.setup.UVQuad quad =
                                receivedByPlayerB.configuration().uvLayouts().skin().left().getQuads()
                                                .get(dbrighthd.wildfiregendermodplugin.wildfire.setup.UVDirection.NORTH);
                assertNotNull(quad, "UV quad must survive cross-player sync");
                assertEquals(1, quad.x1());
                assertEquals(2, quad.y1());
                assertEquals(3, quad.x2());
                assertEquals(4, quad.y2());
        }

        /**
         * Regression: ModUser must not have a rawUvBytes field.
         * If someone re-introduces it, this test fails at compile-time.
         * Ensures the proxy pattern cannot silently re-appear.
         */
        @Test
        public void testModUserHasNoRawUvBytesField() {
                // If rawUvBytes() accessor exists this won't compile — that's intentional.
                // We verify the record only has the two canonical accessors.
                ModUser user = new ModUser(UUID.randomUUID(),
                                new dbrighthd.wildfiregendermodplugin.wildfire.setup.ModConfiguration(
                                                new dbrighthd.wildfiregendermodplugin.wildfire.setup.GeneralOptions.Builder().create(),
                                                new dbrighthd.wildfiregendermodplugin.wildfire.setup.PhysicsOptions.Builder().create(),
                                                new dbrighthd.wildfiregendermodplugin.wildfire.setup.BreastOptions.Builder().create(),
                                                dbrighthd.wildfiregendermodplugin.wildfire.setup.UVLayouts.defaultLayouts()));

                // These two must compile and must be the only component accessors
                assertNotNull(user.userId());
                assertNotNull(user.configuration());

                // Verify via reflection that rawUvBytes is not present
                boolean hasRawUvBytes = false;
                for (java.lang.reflect.RecordComponent rc : ModUser.class.getRecordComponents()) {
                        if (rc.getName().equals("rawUvBytes")) {
                                hasRawUvBytes = true;
                                break;
                        }
                }
                assertFalse(hasRawUvBytes,
                                "ModUser must not contain a rawUvBytes field — the proxy pattern causes cross-client sync corruption");
        }
}
