@file:JvmSynthetic

package com.walletconnect.android.internal.common.explorer.data.network

import com.walletconnect.android.internal.common.explorer.data.network.model.DappListingsDTO
import com.walletconnect.android.internal.common.explorer.data.network.model.WalletListingDTO
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ExplorerService {

    @GET("v3/dapps")
    suspend fun getAllDapps(@Query("projectId") projectId: String): Response<DappListingsDTO>

    @GET("w3m/v1/getAndroidListings")
    suspend fun getAndroidWallets(
        @Query("projectId") projectId: String,
        @Query("chains") chains: String?
    ): Response<WalletListingDTO>
}