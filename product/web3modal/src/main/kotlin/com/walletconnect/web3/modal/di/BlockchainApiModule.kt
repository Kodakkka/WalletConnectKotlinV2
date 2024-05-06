package com.walletconnect.web3.modal.di

import com.walletconnect.android.internal.common.di.AndroidCommonDITags
import com.walletconnect.web3.modal.data.BlockchainRepository
import com.walletconnect.web3.modal.data.network.BlockchainService
import com.walletconnect.web3.modal.domain.usecase.GetIdentityUseCase
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

internal fun blockchainApiModule() = module {

    single(named(Web3ModalDITags.BLOCKCHAIN_RETROFIT)) {
        Retrofit.Builder()
            .baseUrl("https://rpc.walletconnect.org/v1/")
            .client(get(named(AndroidCommonDITags.OK_HTTP)))
            .addConverterFactory(MoshiConverterFactory.create(get(named(AndroidCommonDITags.MOSHI))))
            .build()
    }

    single {
        get<Retrofit>(named(Web3ModalDITags.BLOCKCHAIN_RETROFIT)).create(BlockchainService::class.java)
    }

    single {
        BlockchainRepository(blockchainService = get(), projectId = get())
    }

    single { GetIdentityUseCase(blockchainRepository = get()) }
}
