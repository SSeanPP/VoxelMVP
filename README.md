# VoxelMVP

A high-performance voxel render engine built on Java 1.6 and LWJGL 2.9.3, demonstrating GPU-driven rendering techniques typically associated with modern C++ engines.

<img width="1916" height="1075" alt="VoxelMVP print" src="https://github.com/user-attachments/assets/fb0bbc7c-7f1a-465c-a33e-b85dcf414c39" />

## Features

- **GPU-driven rendering** via `glMultiDrawElementsIndirectCountARB` — the CPU submits one draw call per frame regardless of chunk count
- **Compute shader frustum culling** — a compute pass each frame culls chunks against the view frustum and writes only visible draw commands, using subgroup arithmetic to minimise atomic contention
- **Binary greedy meshing** — mesh threads compact geometry using binary plane operations, producing large merged quads that minimise vertex count
- **Persistent mapped buffers** — vertex and index data is written directly to GPU-mapped memory with no per-frame uploads
- **Toroidal chunk buffer** — slots never move in memory as the player moves; the world scrolls through a fixed SSBO
- **Priority mesh queue** — chunks are meshed nearest-first using a `PriorityBlockingQueue`, with physical core thread count to avoid hyperthreading overhead
- **Debug overlay** — real-time ImGui overlay showing FPS, GPU frame time, visible/meshed chunk counts, VBO/EBO utilisation, and mesh queue depth

## Tech Stack

| | |
|---|---|
| Language | Java 1.6 |
| Graphics | OpenGL 4.6, LWJGL 2.9.3 |
| GUI | ImGui (custom JNI binding) |
| Math | JOML |

## Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Main Thread   │     │ Game Eng Thread │     │   Mesh Threads  │
├─────────────────┤     ├─────────────────┤     ├─────────────────┤
│ reset countBuf  │     │ update player   │     │ generate chunk  │
│ dispatch compute│     │ evict chunks    │     │ greedy mesh     │
│ MDI draw        │     │ submit slots ───┼────►│ alloc VBO/EBO   │
│ read countBuf   │     │                 │     │ write to map ───┼──┐
│ ImGui overlay   │     │                 │     │                 │  │
└────────┬────────┘     └─────────────────┘     └─────────────────┘  │
         │                                                           │
         │                                        ┌──────────────────┘
         │                                        ▼
         │                             ┌─────────────────────┐
         │                             │  Persistent Buffer  │
         │                             ├─────────────────────┤
         │                             │ VBO (vertex data)   │
         │                             │ EBO (index data)    │
         │                             │ SlotSSBO (chunks)   │
         │                             └──────────┬──────────┘
         │                                        │
         ▼                                        ▼
┌─────────────────────────────────────────────────────────────┐
│                          GPU                                │
├─────────────────────────────────────────────────────────────┤
│         Draw Pass               │     Compute Shader        │
├─────────────────────────────────┼───────────────────────────┤
│ reads DrawCmds                  │ read SlotSSBO             │
│ adjusts for camera origin       │ frustum cull AABBs        │
│ calculates texture coordinates  │ write DrawCmds            │
│                                 │ atomicAdd drawCount       │
└─────────────────────────────────┴───────────────────────────┘
```

- **Render distance:** 65 chunks radius, 17 height (71,825 slots)
- **Vertex format:** 8 bytes/vertex (two packed uints — position, UV, face, texIndex)
- **Buffer allocator:** best-fit with coalescing free list

## Performance

Tested on NVIDIA GPU at 64 chunk render distance:

- GPU frame time: ~11.7ms (vertex/geometry bound)
- Compute cull dispatch: ~181μs
- Eliminated CPU/GPU driver bottleneck - future updates need to reduce triangle counts

## Building

Eclipse project. External dependencies:

- LWJGL 2.9.3 jars + natives
- ImGui JNI binding (see my github for how to build)
- JOML

Set your native library path and run `main.Main`.

## JVM Arguments
-XX:+UseConcMarkSweepGC
-XX:+UseParNewGC
-XX:+CMSParallelRemarkEnabled
-XX:CMSInitiatingOccupancyFraction=90
-XX:+UseCMSInitiatingOccupancyOnly
-XX:+CMSScavengeBeforeRemark
-XX:+UseCMSCompactAtFullCollection
-XX:CMSFullGCsBeforeCompaction=1
-Xms1024m -Xmx1024m
-server
-XX:CompileThreshold=1500

## Roadmap

- Hi-Z GPU occlusion culling (~50-70% triangle reduction)
- Baked 3D ambient occlusion
- LOD system
- Bindless sparse textures
