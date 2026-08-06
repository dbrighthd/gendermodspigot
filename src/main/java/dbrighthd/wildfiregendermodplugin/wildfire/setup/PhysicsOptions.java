package dbrighthd.wildfiregendermodplugin.wildfire.setup;

/**
 * @author winnpixie
 */
public record PhysicsOptions(boolean breastPhysics,
        boolean armorPhysics,
        float buoyancy,
        float floppiness) {
    /**
     * Builder for constructing {@link PhysicsOptions} instances.
     * <p>
     * Default values:
     * <ul>
     * <li>{@code breastPhysics} = {@code true}</li>
     * <li>{@code armorPhysics} = {@code false}</li>
     * <li>{@code buoyancy} = {@code 0.333f}</li>
     * <li>{@code floppiness} = {@code 0.75f}</li>
     * </ul>
     */
    public static class Builder {
        private boolean breastPhysics = true;
        private boolean armorPhysics = false;
        private float buoyancy = 0.333f;
        private float floppiness = 0.75f;

        public Builder setBreastPhysics(boolean breastPhysics) {
            this.breastPhysics = breastPhysics;
            return this;
        }

        public Builder setArmorPhysics(boolean armorPhysics) {
            this.armorPhysics = armorPhysics;
            return this;
        }

        public Builder setBuoyancy(float buoyancy) {
            this.buoyancy = buoyancy;
            return this;
        }

        public Builder setFloppiness(float floppiness) {
            this.floppiness = floppiness;
            return this;
        }

        public PhysicsOptions create() {
            return new PhysicsOptions(breastPhysics, armorPhysics, buoyancy, floppiness);
        }
    }
}
