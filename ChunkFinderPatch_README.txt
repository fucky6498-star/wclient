WClient experimental ChunkFinder patch

Copy the contents of this ZIP into the root folder WClient-WClientv38.0, allowing overwrite.
Then build in Termux from the project root:
  ./gradlew --stop
  ./gradlew assembleDebug --no-daemon

Important limitation: this first prototype detects Large Amethyst Bud / Amethyst Cluster only when an UpdateBlockPacket for that block is received. Bedrock's chunk data packets are not decoded into a full local block map by this prototype, so it does NOT scan all already-loaded underground chunks yet. It is a starting integration, not a guaranteed full underground radar.
