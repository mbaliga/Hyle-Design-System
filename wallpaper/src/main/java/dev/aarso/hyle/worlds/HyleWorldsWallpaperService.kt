package dev.aarso.hyle.worlds

import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder

private const val TAG = "HyleWorlds"

/**
 * "Hyle Worlds" live wallpaper — the procedural Brutalist worlds as the home
 * screen. Each [WorldEngine] owns a render thread with its own EGL/GLES2 context;
 * rendering pauses whenever the wallpaper isn't visible (calm + battery).
 */
class HyleWorldsWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = WorldEngine()

    inner class WorldEngine : Engine() {
        private var thread: RenderThread? = null

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            // We drive a slow camera ourselves; no touch handling needed.
            setTouchEventsEnabled(false)
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            thread = RenderThread(holder.surface, WorldConfig.load(applicationContext)).also { it.start() }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            thread?.onSize(width, height)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            // Pick up any settings changed while we were away.
            if (visible) thread?.setConfig(WorldConfig.load(applicationContext))
            thread?.setVisible(visible)
        }

        override fun onOffsetsChanged(
            xOffset: Float, yOffset: Float, xStep: Float, yStep: Float, xPixels: Int, yPixels: Int,
        ) {
            // Home-screen paging becomes a gentle parallax yaw.
            thread?.setYaw((xOffset - 0.5f) * 0.9f)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            thread?.shutdown()
            thread = null
        }

        override fun onDestroy() {
            thread?.shutdown()
            thread = null
            super.onDestroy()
        }
    }

    /** Owns the GL context + render loop for one engine surface. */
    private inner class RenderThread(
        private val surface: Surface,
        initialCfg: WorldConfig,
    ) : Thread("HyleWorldsRender") {
        private val lock = Object()
        @Volatile private var cfg: WorldConfig = initialCfg
        @Volatile private var running = true
        @Volatile private var visible = false
        @Volatile private var width = 0
        @Volatile private var height = 0
        @Volatile private var yaw = 0f

        fun setVisible(v: Boolean) = synchronized(lock) { visible = v; lock.notifyAll() }
        fun onSize(w: Int, h: Int) = synchronized(lock) { width = w; height = h; lock.notifyAll() }
        fun setYaw(y: Float) { yaw = y }
        fun setConfig(c: WorldConfig) { cfg = c }
        fun shutdown() = synchronized(lock) { running = false; visible = true; lock.notifyAll() }

        override fun run() {
            val egl: EglCore
            val renderer: WorldRenderer
            try {
                egl = EglCore(surface)
                egl.makeCurrent()
                renderer = WorldRenderer(applicationContext).also { it.setup() }
            } catch (t: Throwable) {
                Log.e(TAG, "GL init failed", t)
                return
            }

            val start = System.nanoTime()
            try {
                while (true) {
                    synchronized(lock) {
                        while (running && (!visible || width == 0 || height == 0)) lock.wait()
                    }
                    if (!running) break
                    val frameStart = System.nanoTime()
                    val timeSec = (frameStart - start) / 1_000_000_000f
                    renderer.draw(timeSec, width, height, cfg, yaw)
                    egl.swapBuffers()

                    val frameMs = 1000L / cfg.targetFps.coerceIn(15, 60)
                    val elapsed = (System.nanoTime() - frameStart) / 1_000_000L
                    val sleep = frameMs - elapsed
                    if (sleep > 0) sleep(sleep)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "render loop ended", t)
            } finally {
                renderer.release()
                egl.release()
            }
        }
    }
}
