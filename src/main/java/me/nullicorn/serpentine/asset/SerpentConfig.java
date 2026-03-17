package me.nullicorn.serpentine.asset;

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
import com.hypixel.hytale.server.core.command.system.arguments.types.AssetArgumentType;
import com.hypixel.hytale.server.core.command.system.arguments.types.SingleArgumentType;

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
            new KeyedCodec<>("Layout", SerpentLayoutNode.CODEC, true),
            (serpentConfig, s) -> serpentConfig.layout = s,
            (serpentConfig) -> serpentConfig.layout,
            (serpentConfig, parent) -> serpentConfig.layout = parent.layout
        )
        .addValidator(Validators.nonNull())
        .add()
        .build();

    public static final ValidatorCache<String> VALIDATOR_CACHE =
        new ValidatorCache<>(new AssetKeyValidator<>(SerpentConfig::getAssetStore));

    private static AssetStore<String, SerpentConfig, DefaultAssetMap<String, SerpentConfig>> ASSET_STORE;

    public static final SingleArgumentType<SerpentConfig> SINGLE_ARGUMENT_TYPE = new AssetArgumentType<>(
        "server.commands.parsing.argtype.asset.serpent-config.name", SerpentConfig.class, "server.commands.parsing.argtype.asset.serpent-config.usage"
    );

    private String id;
    private AssetExtraInfo.Data extraData;
    private SerpentLayoutNode layout;

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

    public SerpentLayoutNode layout() {
        return this.layout;
    }
}
