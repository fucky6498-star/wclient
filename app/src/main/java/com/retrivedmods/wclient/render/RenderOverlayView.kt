package com.retrivedmods.wclient.render

import android.content.Context
import android.graphics.Canvas
import android.view.View
import com.retrivedmods.wclient.game.ModuleManager
import com.retrivedmods.wclient.game.module.visual.ESPModule
import com.retrivedmods.wclient.game.module.visual.ChunkFinderModule

class RenderOverlayView(context: Context) : View(context) {

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        invalidate() // Initial render
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        ModuleManager.modules
            .filterIsInstance<ChunkFinderModule>()
            .filter { it.isEnabled && it.isSessionCreated }
            .forEach { it.render(canvas) }

        ModuleManager.modules
            .filterIsInstance<ESPModule>()
            .filter { it.isEnabled && it.isSessionCreated } //isSessionCreated check
            .forEach { it.render(canvas) }

        if (ModuleManager.modules.any {
                (it is ESPModule || it is ChunkFinderModule) && it.isEnabled && it.isSessionCreated
            }) {
            postInvalidateOnAnimation()
        }
    }
}