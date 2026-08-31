package dev.fauza.tdeh.model;

/**
 * A {@code BlockDisplay} part. The block is held as its vanilla string form, for example
 * {@code minecraft:oak_log[axis=y]}; parsing it into real {@code BlockData} needs a server and so
 * happens in the render layer.
 */
public final class BlockPart extends HologramPart {

    private String blockData;

    public BlockPart(String blockData) {
        super(DisplayType.BLOCK);
        blockData(blockData);
    }

    public String blockData() {
        return blockData;
    }

    public void blockData(String blockData) {
        if (blockData == null || blockData.isBlank()) {
            throw new IllegalArgumentException("Block data must not be blank");
        }
        this.blockData = blockData.trim();
    }
}
