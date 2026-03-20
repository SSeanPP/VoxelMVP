package guiHandler;

import bufferManager.SceneBufferManager;
import imgui.ImGui;
import main.GameState;
import main.Settings;

public class F3Menu {

    private static final int BUFFER_SIZE = 256; // shared dynamic buffer
    private final byte[] textBuffer = new byte[BUFFER_SIZE];

    private static int fps;
    private static int frames;
    private static long fpsTimer;

    private final Runtime rt = Runtime.getRuntime();

    public F3Menu() {
        fpsTimer = System.currentTimeMillis();
    }

    public void run(GameState state, int totalIndices, int totalVertices) {
        if (!state.getF3state()) return;

        // --- Compute used memory ---
        long usedMB = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);

        // --- Meshing window ---
        ImGui.setNextWindowPos(10, 10);
        ImGui.begin("Meshing");

        ImGui.text(textBuffer, writeAscii(textBuffer, 0, "Meshing Progress"));

        ImGui.separator();

        // --- VBO ---
        float vboFill = 1.0f - (float) SceneBufferManager.vboFreeBytes / Settings.VBO_SIZE_BYTES;
        int vboUsedMB = (int)((Settings.VBO_SIZE_BYTES - SceneBufferManager.vboFreeBytes) / (1024*1024));

        int offset = 0;
        offset = writeAscii(textBuffer, offset, "VBO: ");
        offset += writeInt(textBuffer, offset, vboUsedMB);
        offset = writeAscii(textBuffer, offset, " / ");
        offset += writeInt(textBuffer, offset, Settings.VBO_SIZE_BYTES / (1024*1024));
        offset = writeAscii(textBuffer, offset, " MiB");

        ImGui.progressBar(textBuffer, offset, vboFill);

        offset = 0;
        offset = writeAscii(textBuffer, offset, "Free regions: ");
        offset += writeInt(textBuffer, offset, SceneBufferManager.vboFreeRegions);
        ImGui.text(textBuffer, offset);

        ImGui.separator();

        // --- EBO ---
        float eboFill = 1.0f - (float) SceneBufferManager.eboFreeBytes / Settings.EBO_SIZE_BYTES;
        int eboUsedMB = (int)((Settings.EBO_SIZE_BYTES - SceneBufferManager.eboFreeBytes) / (1024*1024));

        offset = 0;
        offset = writeAscii(textBuffer, offset, "EBO: ");
        offset += writeInt(textBuffer, offset, eboUsedMB);
        offset = writeAscii(textBuffer, offset, " / ");
        offset += writeInt(textBuffer, offset, Settings.EBO_SIZE_BYTES / (1024*1024));
        offset = writeAscii(textBuffer, offset, " MiB");

        ImGui.progressBar(textBuffer, offset, eboFill);

        offset = 0;
        offset = writeAscii(textBuffer, offset, "Free regions: ");
        offset += writeInt(textBuffer, offset, SceneBufferManager.eboFreeRegions);
        ImGui.text(textBuffer, offset);

        ImGui.end(); // end Meshing window

        // --- General Data window ---
        ImGui.setNextWindowPos(210, 10);
        ImGui.begin("General Data");

        offset = 0;
        offset = writeAscii(textBuffer, offset, "Used MB: ");
        offset += writeInt(textBuffer, offset, (int)usedMB);
        ImGui.text(textBuffer, offset);

        // FPS
        frames++;
        long now = System.currentTimeMillis();
        if (now - fpsTimer >= 1000) {
            fps = frames;
            frames = 0;
            fpsTimer += 1000;
        }

        offset = 0;
        offset = writeAscii(textBuffer, offset, "FPS: ");
        offset += writeInt(textBuffer, offset, fps);
        ImGui.text(textBuffer, offset);

        // Vertices / Indices
        offset = 0;
        offset = writeAscii(textBuffer, offset, "Vertices: ");
        offset += writeInt(textBuffer, offset, totalVertices);
        offset = writeAscii(textBuffer, offset, " Indices: ");
        offset += writeInt(textBuffer, offset, totalIndices);
        ImGui.text(textBuffer, offset);

        ImGui.end(); // end General Data
    }

    // --- Helpers for GC-free number/string writing ---
    private int writeAscii(byte[] buf, int offset, String s) {
        for (int i = 0; i < s.length(); i++) {
            buf[offset++] = (byte)s.charAt(i);
        }
        return offset;
    }

    private int writeInt(byte[] buf, int offset, long l) {
        if (l == 0) {
            buf[offset++] = '0';
            return 1;
        }
        int start = offset;
        long temp = l;
        int digits = 0;
        while (temp > 0) { temp /= 10; digits++; }
        int pos = offset + digits - 1;
        temp = l;
        while (temp > 0) {
            buf[pos--] = (byte)('0' + temp % 10);
            temp /= 10;
        }
        return digits;
    }
}