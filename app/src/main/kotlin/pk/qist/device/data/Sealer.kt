package pk.qist.device.data

/** Encrypts and decrypts the app's state file. An interface so tests on the JVM can use a plain stand-in. */
interface Sealer {
    fun seal(plain: ByteArray): ByteArray

    fun open(sealed: ByteArray): ByteArray
}
