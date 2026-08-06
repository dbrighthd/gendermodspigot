package dbrighthd.wildfiregendermodplugin.wildfire.setup;

/**
 * @author winnpixie
 */
public record GeneralOptions(GenderIdentities genderIdentity,
        boolean hurtSounds,
        float voicePitch,
        boolean showInArmor) {
    /**
     * Builder for constructing {@link GeneralOptions} instances.
     * <p>
     * Default values:
     * <ul>
     * <li>{@code genderIdentity} = {@link GenderIdentities#MALE}</li>
     * <li>{@code hurtSounds} = {@code true}</li>
     * <li>{@code voicePitch} = {@code 1.0f}</li>
     * <li>{@code showInArmor} = {@code true}</li>
     * </ul>
     */
    public static class Builder {
        private GenderIdentities genderIdentity = GenderIdentities.MALE;
        private boolean hurtSounds = true;
        private float voicePitch = 1.0f;
        private boolean showInArmor = true;

        public Builder setGenderIdentity(GenderIdentities genderIdentity) {
            this.genderIdentity = genderIdentity;
            return this;
        }

        public Builder setHurtSounds(boolean hurtSounds) {
            this.hurtSounds = hurtSounds;
            return this;
        }

        public Builder setVoicePitch(float voicePitch) {
            this.voicePitch = voicePitch;
            return this;
        }

        public Builder setShowInArmor(boolean showInArmor) {
            this.showInArmor = showInArmor;
            return this;
        }

        public GeneralOptions create() {
            return new GeneralOptions(genderIdentity, hurtSounds, voicePitch, showInArmor);
        }
    }
}
