package me.nullicorn.serpentine.asset;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.math.range.IntRange;
import com.hypixel.hytale.math.util.MathUtil;

import java.util.ArrayList;
import java.util.List;

public final class SerpentLayoutRepeater implements SerpentLayoutNode {
    /**
     * ID of this type's codec in {@link SerpentLayoutNode#CODEC}.
     */
    public static final String ID = "Repeat";

    public static final BuilderCodec<SerpentLayoutRepeater> CODEC = BuilderCodec.builder(SerpentLayoutRepeater.class, SerpentLayoutRepeater::new)
        .appendInherited(
            new KeyedCodec<>("Count", IntRange.CODEC, true),
            (o, s) -> o.count = s,
            (o) -> o.count,
            (o, parent) -> o.count = parent.count
        )
        .documentation("The number of times to repeat the node, selected from a random range, both ends inclusive. To repeat a constant number of times, set both to the same value")
        .addValidator(Validators.nonNull())
        .add()
        .appendInherited(
            new KeyedCodec<>("Node", SerpentLayoutNode.CODEC, true),
            (o, s) -> o.node = s,
            (o) -> o.node,
            (o, parent) -> o.node = parent.node
        )
        .documentation("The node to repeat")
        .addValidator(Validators.nonNull())
        .add()
        .build();

    private IntRange count;
    private SerpentLayoutNode node;

    private SerpentLayoutRepeater() {
        // Only intended for codec.
    }

    @Override
    public List<SerpentLayoutBone> chooseBones() {
        final List<SerpentLayoutBone> bones = new ArrayList<>();
        final int count = MathUtil.randomInt(this.count.getInclusiveMin(), this.count.getInclusiveMax() + 1);
        for (int i = 0; i < count; i++) {
            bones.addAll(this.node.chooseBones());
        }
        return bones;
    }
}
