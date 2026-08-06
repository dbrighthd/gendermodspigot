package dbrighthd.wildfiregendermodplugin.wildfire.setup;

public record UVLayouts(Layer skin, Layer overlay) {
    public record Layer(UVLayout left, UVLayout right) {
    }

    public static UVLayouts defaultLayouts() {
        return new UVLayouts(
                new Layer(UVLayout.defaultSkinLeft(), UVLayout.defaultSkinRight()),
                new Layer(UVLayout.defaultOverlayLeft(), UVLayout.defaultOverlayRight()));
    }
}
