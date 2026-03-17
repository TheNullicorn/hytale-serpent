package me.nullicorn.serpentine.asset;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;

import javax.annotation.Nullable;
import java.util.List;

public final class SerpentLayoutBone implements SerpentLayoutNode {
    /**
     * ID of this type's codec in {@link SerpentLayoutNode#CODEC}.
     */
    public static final String ID = "Bone";
    public static final BuilderCodec<SerpentLayoutBone> CODEC = BuilderCodec.builder(SerpentLayoutBone.class, SerpentLayoutBone::new)
        .appendInherited(
            new KeyedCodec<>("BoneConfig", Codec.STRING, true),
            (o, s) -> o.boneConfigId = s,
            (o) -> o.boneConfigId,
            (o, parent) -> o.boneConfigId = parent.boneConfigId
        )
        .addValidator(Validators.nonNull())
        .addValidator(SerpentBoneConfig.VALIDATOR_CACHE.getValidator())
        .add()
        .appendInherited(
            new KeyedCodec<>("Scale", Codec.DOUBLE, false),
            (o, s) -> o.scale = s,
            (o) -> o.scale,
            (o, parent) -> o.scale = parent.scale
        )
        .documentation("Scales the bone's model and hitbox")
        .addValidator(Validators.nonNull())
        .addValidator(Validators.greaterThan(0.0))
        .add()
        .afterDecode(it -> {
            if (it.boneConfigId != null) {
                it.boneConfig = SerpentBoneConfig.getAssetMap().getAsset(it.boneConfigId);
            }
        })
        .build();

    // Serialized fields.
    private String boneConfigId;
    private double scale = 1.0;

    // Non-serialized fields.
    @Nullable
    private SerpentBoneConfig boneConfig;

    public SerpentLayoutBone(final String boneConfigId, final double scale) {
        validateScale(scale);
        this.boneConfigId = boneConfigId;
        this.boneConfig = SerpentBoneConfig.getAssetMap().getAsset(boneConfigId);
        this.scale = scale;
    }

    public SerpentLayoutBone(final SerpentBoneConfig boneConfig, final double scale) {
        validateScale(scale);
        this.boneConfigId = boneConfig.getId();
        this.boneConfig = boneConfig;
        this.scale = scale;
    }

    private SerpentLayoutBone() {
        // Only intended for codec.
    }

    @Nullable
    public SerpentBoneConfig boneConfig() {
        return this.boneConfig;
    }

    public double scale() {
        return this.scale;
    }

    @Override
    public List<SerpentLayoutBone> chooseBones() {
        return List.of(this);
    }

    private static void validateScale(final double scale) {
        if (scale <= 0.0 || !Double.isFinite(scale)) {
            throw new IllegalArgumentException("scale must be finite and greater than zero");
        }
    }
}
