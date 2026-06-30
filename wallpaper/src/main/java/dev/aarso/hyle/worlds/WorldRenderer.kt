package dev.aarso.hyle.worlds

import android.content.Context
import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Draws the Form-World SDF raymarcher with OpenGL ES 2.0. The fragment shader is
 * the engine's GLSL verbatim (res/raw/world_frag.glsl); we feed it the same
 * uniforms the web build's draw() loop does, sourced from [WorldConfig].
 *
 * Sprites are disabled (uSprCount = 0) so the marcher never hits the
 * dynamic uniform-array indexing path — keeping it portable and battery-light.
 */
class WorldRenderer(private val ctx: Context) {
    private var program = 0
    private val tri: FloatBuffer = ByteBuffer
        .allocateDirect(6 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        .apply { put(floatArrayOf(-1f, -1f, 3f, -1f, -1f, 3f)); position(0) }
    private val loc = HashMap<String, Int>()
    private val zeroSpr = FloatArray(8 * 4)
    private var seed = 11f

    fun setup() {
        val vert = readRaw(R.raw.world_vert)
        val frag = readRaw(R.raw.world_frag)
        val vs = compile(GLES20.GL_VERTEX_SHADER, vert)
        val fs = compile(GLES20.GL_FRAGMENT_SHADER, frag)
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vs)
        GLES20.glAttachShader(program, fs)
        GLES20.glBindAttribLocation(program, 0, "aPos")
        GLES20.glLinkProgram(program)
        val status = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0)
        check(status[0] != 0) { "link failed: " + GLES20.glGetProgramInfoLog(program) }
        GLES20.glDeleteShader(vs)
        GLES20.glDeleteShader(fs)
    }

    fun draw(timeSec: Float, width: Int, height: Int, cfg: WorldConfig, yaw: Float) {
        GLES20.glViewport(0, 0, width, height)
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(program)

        val p = cfg.palette
        f("uRes", width.toFloat(), height.toFloat())
        f("uTime", timeSec)
        f("uSeed", seed)
        f("uSpeed", cfg.speed * 2.2f)
        f("uGrain", 0.5f)
        f("uGlow", p.glow)
        f("uAim", 2.2f)
        f("uVoid", p.atmos)
        f("uPattern", 0f)
        f("uZoom", 0.5f)
        f("uCount", 5f)
        f("uYaw", yaw)
        f("uTilt", 0f)
        f("uInner", 0.3f)
        f("uTexType", p.texType.toFloat())
        f("uScene", cfg.scene.toFloat())
        f("uGrit", 1f)
        f("uCoarse", 0.5f)
        f("uGrainSc", 0.5f)
        f("uActive", if (cfg.active) 1f else 0f)
        f("uRecursive", if (cfg.recursive) 1f else 0f)
        f("uSprCount", 0f)
        f("uSprMode", 0f)
        f("uBreathe", 1f)
        f("uFlicker", 0f)
        f("uDrift", 1f)
        f("uPulse", 0f)
        v3("uMat", p.stone)
        v3("uFog", p.smog)
        v3("uLight", p.light)
        v4("uSpr", zeroSpr)
        v4("uSprD", zeroSpr)

        GLES20.glEnableVertexAttribArray(0)
        GLES20.glVertexAttribPointer(0, 2, GLES20.GL_FLOAT, false, 0, tri)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3)
        GLES20.glDisableVertexAttribArray(0)
    }

    fun release() {
        if (program != 0) GLES20.glDeleteProgram(program)
        program = 0
        loc.clear()
    }

    // --- uniform helpers (locations cached) ---
    private fun at(name: String) = loc.getOrPut(name) { GLES20.glGetUniformLocation(program, name) }
    private fun f(n: String, a: Float) = GLES20.glUniform1f(at(n), a)
    private fun f(n: String, a: Float, b: Float) = GLES20.glUniform2f(at(n), a, b)
    private fun v3(n: String, c: Rgb) = GLES20.glUniform3f(at(n), c[0], c[1], c[2])
    private fun v4(n: String, arr: FloatArray) = GLES20.glUniform4fv(at(n), arr.size / 4, arr, 0)

    private fun readRaw(id: Int): String =
        ctx.resources.openRawResource(id).bufferedReader().use { it.readText() }

    private fun compile(type: Int, src: String): Int {
        val s = GLES20.glCreateShader(type)
        GLES20.glShaderSource(s, src)
        GLES20.glCompileShader(s)
        val status = IntArray(1)
        GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, status, 0)
        check(status[0] != 0) {
            val log = GLES20.glGetShaderInfoLog(s)
            GLES20.glDeleteShader(s)
            "shader compile failed: $log"
        }
        return s
    }
}
