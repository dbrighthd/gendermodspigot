package dbrighthd.wildfiregendermodplugin.wildfire.setup;

import java.util.EnumMap;
import java.util.Map;

public class UVLayout {
    private final Map<UVDirection, UVQuad> quads;

    public UVLayout() {
        this.quads = new EnumMap<>(UVDirection.class);
    }

    public UVLayout(Map<UVDirection, UVQuad> quads) {
        this.quads = new EnumMap<>(quads);
    }

    public Map<UVDirection, UVQuad> getQuads() {
        return quads;
    }

    public void setQuad(UVDirection direction, UVQuad quad) {
        quads.put(direction, quad);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        UVLayout uvLayout = (UVLayout) o;
        return java.util.Objects.equals(quads, uvLayout.quads);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hashCode(quads);
    }

    public static UVLayout defaultSkinLeft() {
        return new UVLayout(Map.of(
                UVDirection.EAST, new UVQuad(24, 21, 27, 26),
                UVDirection.WEST, new UVQuad(16, 21, 20, 26),
                UVDirection.DOWN, new UVQuad(20, 17, 24, 21),
                UVDirection.UP, new UVQuad(20, 25, 24, 27),
                UVDirection.NORTH, new UVQuad(20, 21, 24, 26)));
    }

    public static UVLayout defaultSkinRight() {
        return new UVLayout(Map.of(
                UVDirection.EAST, new UVQuad(28, 21, 32, 26),
                UVDirection.WEST, new UVQuad(21, 21, 24, 26),
                UVDirection.DOWN, new UVQuad(24, 17, 28, 21),
                UVDirection.UP, new UVQuad(24, 25, 28, 27),
                UVDirection.NORTH, new UVQuad(24, 21, 28, 26)));
    }

    public static UVLayout defaultOverlayLeft() {
        return new UVLayout(Map.of(
                UVDirection.EAST, new UVQuad(0, 0, 0, 0),
                UVDirection.WEST, new UVQuad(17, 37, 20, 42),
                UVDirection.DOWN, new UVQuad(20, 34, 24, 37),
                UVDirection.UP, new UVQuad(20, 42, 24, 45),
                UVDirection.NORTH, new UVQuad(20, 37, 24, 42)));
    }

    public static UVLayout defaultOverlayRight() {
        return new UVLayout(Map.of(
                UVDirection.EAST, new UVQuad(28, 37, 31, 42),
                UVDirection.WEST, new UVQuad(0, 0, 0, 0),
                UVDirection.DOWN, new UVQuad(24, 34, 28, 37),
                UVDirection.UP, new UVQuad(24, 42, 28, 45),
                UVDirection.NORTH, new UVQuad(24, 37, 28, 42)));
    }
}
