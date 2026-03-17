package me.nullicorn.serpentine.asset;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.validation.Validators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SerpentLayoutSequence implements SerpentLayoutNode {
    /**
     * ID of this type's codec in {@link SerpentLayoutNode#CODEC}.
     */
    public static final String ID = "Sequence";

    public static final BuilderCodec<SerpentLayoutSequence> CODEC = BuilderCodec.builder(SerpentLayoutSequence.class, SerpentLayoutSequence::new)
        .appendInherited(
            new KeyedCodec<>("Sequence", new ArrayCodec<>(SerpentLayoutNode.CODEC, SerpentLayoutNode[]::new), true),
            (o, s) -> {
                o.sequence.clear();
                o.sequence.addAll(Arrays.asList(s));
            },
            (o) -> o.sequence.toArray(new SerpentLayoutNode[0]),
            (o, parent) -> {
                o.sequence.clear();
                o.sequence.addAll(parent.sequence);
            }
        )
        .addValidator(Validators.nonNull())
        .addValidator(Validators.nonEmptyArray())
        .add()
        .build();

    private final List<SerpentLayoutNode> sequence = new ArrayList<>();

    private SerpentLayoutSequence() {
        // Only intended for codec.
    }

    @Override
    public List<SerpentLayoutBone> chooseBones() {
        return this.sequence.stream().flatMap(node -> node.chooseBones().stream()).toList();
    }
}
