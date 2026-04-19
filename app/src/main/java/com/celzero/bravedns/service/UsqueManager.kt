object UsqueManager {
    const val SOCKS_HOST = "127.0.0.1"
    const val SOCKS_PORT = 40000

    private var process: Process? = null
    private const val BINARY_NAME = "usque-rs-arm32"

    fun isRegistered(ctx: Context): Boolean {
        // check if a warp registration file exists from a prior run
        return ctx.getFileStreamPath("warp_reg.json").exists()
    }

    suspend fun registerWithWarp(ctx: Context): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val bin = copyBinary(ctx)
            val proc = ProcessBuilder(bin.absolutePath, "register")
                .redirectErrorStream(true)
                .start()
            val ok = proc.waitFor(30, TimeUnit.SECONDS) && proc.exitValue() == 0
            ok
        } catch (e: Exception) {
            Logger.e("USQUE", "register failed: ${e.message}", e)
            false
        }
    }

    suspend fun startSocksProxy(ctx: Context): Boolean = withContext(Dispatchers.IO) {
        stopSocksProxy()
        return@withContext try {
            val bin = copyBinary(ctx)
            process = ProcessBuilder(
                bin.absolutePath, "socks5",
                "--host", SOCKS_HOST,
                "--port", SOCKS_PORT.toString()
            )
                .redirectErrorStream(true)
                .start()
            Thread.sleep(800) // let it bind
            process?.isAlive == true
        } catch (e: Exception) {
            Logger.e("USQUE", "start failed: ${e.message}", e)
            false
        }
    }

    fun stopSocksProxy() {
        process?.destroy()
        process = null
    }

    private fun copyBinary(ctx: Context): File {
        val out = File(ctx.filesDir, BINARY_NAME)
        if (!out.exists()) {
            ctx.assets.open(BINARY_NAME).use { it.copyTo(out.outputStream()) }
            out.setExecutable(true)
        }
        return out
    }
}
