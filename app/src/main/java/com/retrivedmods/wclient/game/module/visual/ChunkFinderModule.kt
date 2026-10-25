package com.retrivedmods.wclient.game.module.visual

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import com.retrivedmods.wclient.render.RenderOverlayView
import org.cloudburstmc.math.matrix.Matrix4f
import org.cloudburstmc.math.vector.Vector2f
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.packet.UpdateBlockPacket
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.cos
import kotlin.math.sin

/**
 * Experimental ChunkFinder. Bedrock UpdateBlockPacket only reports block changes;
 * it does not enumerate every block in already-loaded chunks.
 */
class ChunkFinderModule : Module("chunkfinder", ModuleCategory.Visual) {
    companion object {
        private var renderView: RenderOverlayView? = null
        fun setRenderView(view: RenderOverlayView) { renderView = view }
    }

    private data class FoundBlock(val x: Int, val y: Int, val z: Int)
    private val found = ConcurrentHashMap<String, FoundBlock>()
    private val maxTracked = 500
    private val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        textSize = 32f
        style = Paint.Style.FILL
        setShadowLayer(4f, 1f, 1f, Color.BLACK)
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled || !isSessionCreated) return
        val packet = interceptablePacket.packet
        if (packet is UpdateBlockPacket) {
            val blockName = session.blockMapping.getDefinition(packet.definition.runtimeId).toString()
            if (blockName == "minecraft:large_amethyst_bud" || blockName == "minecraft:amethyst_cluster") {
                val p = packet.blockPosition
                val key = "${p.x},${p.y},${p.z}"
                val wasNew = found.put(key, FoundBlock(p.x, p.y, p.z)) == null
                if (found.size > maxTracked) found.keys.firstOrNull()?.let { found.remove(it) }
                if (wasNew) session.displayClientMessage("§c[ChunkFinder] §fAmethyst: §e${p.x}, ${p.y}, ${p.z}")
                renderView?.postInvalidateOnAnimation()
            }
        }
    }

    override fun onDisabled() {
        found.clear()
        renderView?.invalidate()
        super.onDisabled()
    }

    override fun onDisconnect(reason: String) {
        found.clear()
        renderView?.invalidate()
    }

    fun render(canvas: Canvas) {
        if (!isEnabled || !isSessionCreated || found.isEmpty()) return
        val player = session.localPlayer
        val matrix = Matrix4f.createPerspective(110f, canvas.width.toFloat() / canvas.height.coerceAtLeast(1), 0.1f, 128f)
            .mul(Matrix4f.createTranslation(player.vec3Position)
                .mul(rotateY(-player.rotationYaw - 180f))
                .mul(rotateX(-player.rotationPitch))
                .invert())

        found.values.forEach { block ->
            val x = block.x.toFloat(); val y = block.y.toFloat(); val z = block.z.toFloat()
            val corners = listOf(
                Vector3f.from(x, y, z), Vector3f.from(x + 1, y, z),
                Vector3f.from(x + 1, y + 1, z), Vector3f.from(x, y + 1, z),
                Vector3f.from(x, y, z + 1), Vector3f.from(x + 1, y, z + 1),
                Vector3f.from(x + 1, y + 1, z + 1), Vector3f.from(x, y + 1, z + 1)
            )
            val screen = corners.map { worldToScreen(it, matrix, canvas.width, canvas.height) }
            if (screen.any { it == null }) return@forEach
            val pts = screen.filterNotNull()
            val edges = arrayOf(
                0 to 1, 1 to 2, 2 to 3, 3 to 0,
                4 to 5, 5 to 6, 6 to 7, 7 to 4,
                0 to 4, 1 to 5, 2 to 6, 3 to 7
            )
            edges.forEach { (a, b) -> canvas.drawLine(pts[a].x, pts[a].y, pts[b].x, pts[b].y, boxPaint) }
            val label = "${block.x}, ${block.y}, ${block.z}"
            canvas.drawText(label, pts.minOf { it.x }, pts.minOf { it.y } - 8f, textPaint)
        }
    }

    private fun worldToScreen(pos: Vector3f, m: Matrix4f, w: Int, h: Int): Vector2f? {
        val rw = m.get(3, 0) * pos.x + m.get(3, 1) * pos.y + m.get(3, 2) * pos.z + m.get(3, 3)
        if (rw <= 0.01f) return null
        val inv = 1f / rw
        val sx = w / 2f + (m.get(0, 0) * pos.x + m.get(0, 1) * pos.y + m.get(0, 2) * pos.z + m.get(0, 3)) * inv * w / 2f
        val sy = h / 2f - (m.get(1, 0) * pos.x + m.get(1, 1) * pos.y + m.get(1, 2) * pos.z + m.get(1, 3)) * inv * h / 2f
        return Vector2f.from(sx, sy)
    }

    private fun rotateX(a: Float): Matrix4f {
        val r = Math.toRadians(a.toDouble())
        return Matrix4f.from(1f,0f,0f,0f, 0f,cos(r).toFloat(),-sin(r).toFloat(),0f, 0f,sin(r).toFloat(),cos(r).toFloat(),0f, 0f,0f,0f,1f)
    }

    private fun rotateY(a: Float): Matrix4f {
        val r = Math.toRadians(a.toDouble())
        return Matrix4f.from(cos(r).toFloat(),0f,sin(r).toFloat(),0f, 0f,1f,0f,0f, -sin(r).toFloat(),0f,cos(r).toFloat(),0f, 0f,0f,0f,1f)
    }
}
