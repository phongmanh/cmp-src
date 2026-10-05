package com.liam.cmp_src.di

import com.liam.cmp_src.core.database.getDatabaseBuilder
import com.liam.cmp_src.core.security.KeychainTokenCipher
import org.koin.core.module.Module
import org.koin.dsl.module

/** iOS's half of the graph: the Room file and the Keychain cipher. */
fun iosPlatformModule(): Module = module {
    roomPersistence(builder = { getDatabaseBuilder() }, cipher = { KeychainTokenCipher() })
}
