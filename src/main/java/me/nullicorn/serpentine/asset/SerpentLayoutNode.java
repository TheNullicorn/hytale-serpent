package me.nullicorn.serpentine.asset;

import com.hypixel.hytale.codec.lookup.BuilderCodecMapCodec;

import java.util.List;

public interface SerpentLayoutNode {
    BuilderCodecMapCodec<SerpentLayoutNode> CODEC = new BuilderCodecMapCodec<>("Type");

    List<SerpentLayoutBone> chooseBones();
}
