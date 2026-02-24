package me.nullicorn.hytale.serpent.asset;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetKeyValidator;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.validation.ValidatorCache;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.math.range.IntRange;
import com.hypixel.hytale.math.range.IntRangeBoundValidator;

public final class SerpentConfig implements JsonAssetWithMap<String, DefaultAssetMap<String, SerpentConfig>> {
    public static final String PATH = "NPC/SerpentConfig";

    public static final AssetBuilderCodec<String, SerpentConfig> CODEC = AssetBuilderCodec.builder(
            SerpentConfig.class,
            SerpentConfig::new,
            Codec.STRING,
            (asset, s) -> asset.id = s,
            (asset) -> asset.id,
            (asset, data) -> asset.extraData = data,
            (asset) -> asset.extraData
        )
        .appendInherited(
            new KeyedCodec<>("Head", SerpentSegmentConfig.CHILD_ASSET_CODEC, true),
            (serpentConfig, s) -> serpentConfig.headAssetId = s,
            (serpentConfig) -> serpentConfig.headAssetId,
            (serpentConfig, parent) -> serpentConfig.headAssetId = parent.headAssetId
        )
        .addValidator(Validators.nonNull())
        .addValidator(SerpentSegmentConfig.VALIDATOR_CACHE.getValidator())
        .add()
        .appendInherited(
            new KeyedCodec<>("Body", SerpentSegmentConfig.CHILD_ASSET_CODEC, true),
            (serpentConfig, s) -> serpentConfig.bodyAssetId = s,
            (serpentConfig) -> serpentConfig.bodyAssetId,
            (serpentConfig, parent) -> serpentConfig.bodyAssetId = parent.bodyAssetId
        )
        .addValidator(Validators.nonNull())
        .addValidator(SerpentSegmentConfig.VALIDATOR_CACHE.getValidator())
        .add()
        .appendInherited(
            new KeyedCodec<>("Tail", SerpentSegmentConfig.CHILD_ASSET_CODEC, true),
            (serpentConfig, s) -> serpentConfig.tailAssetId = s,
            (serpentConfig) -> serpentConfig.tailAssetId,
            (serpentConfig, parent) -> serpentConfig.tailAssetId = parent.tailAssetId
        )
        .addValidator(Validators.nonNull())
        .addValidator(SerpentSegmentConfig.VALIDATOR_CACHE.getValidator())
        .add()
        .appendInherited(
            new KeyedCodec<>("BodySegmentCount", IntRange.CODEC, true),
            (serpentConfig, s) -> serpentConfig.bodySegmentCount = s,
            (serpentConfig) -> serpentConfig.bodySegmentCount,
            (serpentConfig, parent) -> serpentConfig.bodySegmentCount = parent.bodySegmentCount
        )
        .addValidator(Validators.nonNull())
        .addValidator(IntRangeBoundValidator.lowerBound(0, null, true))
        .add()
        .appendInherited(
            new KeyedCodec<>("DefaultHardDampingSpeed", Codec.DOUBLE, true),
            (serpentConfig, s) -> serpentConfig.defaultHardDampingSpeed = s,
            (serpentConfig) -> serpentConfig.defaultHardDampingSpeed,
            (serpentConfig, parent) -> serpentConfig.defaultHardDampingSpeed = parent.defaultHardDampingSpeed
        )
        .documentation("Joints moving faster than this will lose that additional speed before the next tick. **Note:** Joints can exceed this if they must in order to stay attached")
        .addValidator(Validators.nonNull())
        .addValidator(Validators.min(0.0d))
        .add()
        .appendInherited(
            new KeyedCodec<>("DefaultSoftDampingSpeed", Codec.DOUBLE, true),
            (serpentConfig, s) -> serpentConfig.defaultSoftDampingSpeed = s,
            (serpentConfig) -> serpentConfig.defaultSoftDampingSpeed,
            (serpentConfig, parent) -> serpentConfig.defaultSoftDampingSpeed = parent.defaultSoftDampingSpeed
        )
        .documentation("Joints moving faster than this are gradually slowed by multiplying their speed by **SoftDampingCoefficient**")
        .addValidator(Validators.nonNull())
        .addValidator(Validators.min(0.0d))
        .add()
        .appendInherited(
            new KeyedCodec<>("DefaultSoftDampingCoefficient", Codec.DOUBLE, true),
            (serpentConfig, s) -> serpentConfig.defaultSoftDampingCoefficient = s,
            (serpentConfig) -> serpentConfig.defaultSoftDampingCoefficient,
            (serpentConfig, parent) -> serpentConfig.defaultSoftDampingCoefficient = parent.defaultSoftDampingCoefficient
        )
        .documentation("Multiplied by a joint's speed if it exceeds **SoftDampingSpeed**")
        .addValidator(Validators.range(0.0d, 1.0d))
        .add()
        .appendInherited(
            new KeyedCodec<>("DefaultSoftAngleLimit", Codec.DOUBLE, true),
            (serpentConfig, s) -> serpentConfig.defaultSoftAngleLimit = s,
            (serpentConfig) -> serpentConfig.defaultSoftAngleLimit,
            (serpentConfig, parent) -> serpentConfig.defaultSoftAngleLimit = parent.defaultSoftAngleLimit
        )
        .addValidator(Validators.nonNull())
        .addValidator(Validators.range(0.0, 180.0))
        .add()
        .afterDecode(serpentConfig -> {
            serpentConfig.head = SerpentSegmentConfig.getAssetMap().getAsset(serpentConfig.headAssetId);
            serpentConfig.body = SerpentSegmentConfig.getAssetMap().getAsset(serpentConfig.bodyAssetId);
            serpentConfig.tail = SerpentSegmentConfig.getAssetMap().getAsset(serpentConfig.tailAssetId);
            assert serpentConfig.head != null;
            assert serpentConfig.body != null;
            assert serpentConfig.tail != null;
        })
        .build();

    public static final ValidatorCache<String> VALIDATOR_CACHE =
        new ValidatorCache<>(new AssetKeyValidator<>(SerpentConfig::getAssetStore));

    private static AssetStore<String, SerpentConfig, DefaultAssetMap<String, SerpentConfig>> ASSET_STORE;

    private String id;
    private AssetExtraInfo.Data extraData;
    private String headAssetId;
    private String bodyAssetId;
    private String tailAssetId;
    private IntRange bodySegmentCount;
    private double defaultHardDampingSpeed;
    private double defaultSoftDampingSpeed;
    private double defaultSoftDampingCoefficient;
    private double defaultSoftAngleLimit;

    private SerpentSegmentConfig head;
    private SerpentSegmentConfig body;
    private SerpentSegmentConfig tail;

    public static AssetStore<String, SerpentConfig, DefaultAssetMap<String, SerpentConfig>> getAssetStore() {
        if (ASSET_STORE == null) {
            ASSET_STORE = AssetRegistry.getAssetStore(SerpentConfig.class);
        }
        return ASSET_STORE;
    }

    public static DefaultAssetMap<String, SerpentConfig> getAssetMap() {
        return getAssetStore().getAssetMap();
    }

    @Override
    public String getId() {
        return this.id;
    }

    public SerpentSegmentConfig getHead() {
        return this.head;
    }

    public SerpentSegmentConfig getBody() {
        return this.body;
    }

    public SerpentSegmentConfig getTail() {
        return this.tail;
    }

    public IntRange getBodySegmentCount() {
        return this.bodySegmentCount;
    }

    public double getDefaultHardDampingSpeed() {
        return this.defaultHardDampingSpeed;
    }

    public double getDefaultSoftDampingSpeed() {
        return this.defaultSoftDampingSpeed;
    }

    public double getDefaultSoftDampingCoefficient() {
        return this.defaultSoftDampingCoefficient;
    }

    public double getDefaultSoftAngleLimit() {
        return this.defaultSoftAngleLimit;
    }
}
