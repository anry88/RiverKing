package util

import de.mkammerer.argon2.Argon2Factory

object PasswordHasher {
    private const val ITERATIONS = 3
    private const val MEMORY_KB = 65_536
    private const val PARALLELISM = 1

    fun hash(password: String): String {
        val chars = password.toCharArray()
        val argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id)
        return try {
            argon2.hash(ITERATIONS, MEMORY_KB, PARALLELISM, chars)
        } finally {
            argon2.wipeArray(chars)
        }
    }

    fun verify(password: String, hash: String): Boolean {
        val chars = password.toCharArray()
        val argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id)
        return try {
            argon2.verify(hash, chars)
        } finally {
            argon2.wipeArray(chars)
        }
    }
}
