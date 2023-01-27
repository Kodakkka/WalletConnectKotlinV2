@file:JvmSynthetic

package com.walletconnect.push.wallet.di

import com.walletconnect.android.di.AndroidCoreDITags
import com.walletconnect.android.di.sdkBaseStorageModule
import com.walletconnect.android.internal.common.di.DBNames
import com.walletconnect.android.internal.common.di.deleteDBs
import com.walletconnect.push.PushDatabase
import com.walletconnect.push.common.storage.data.SubscriptionStorageRepository
import com.walletconnect.push.common.storage.data.dao.Subscriptions
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.dsl.module

@JvmSynthetic
internal fun pushStorageModule(storageSuffix: String) = module {
    fun Scope.createPushDB() = PushDatabase(
        get(),
        SubscriptionsAdapter = Subscriptions.Adapter(metadata_iconsAdapter = get(named(AndroidCoreDITags.COLUMN_ADAPTER_LIST)))
    )

    includes(sdkBaseStorageModule(PushDatabase.Schema, storageSuffix))

    single {
        try {
            createPushDB().also {
                it.subscriptionsQueries.getAllSubscriptions().executeAsOneOrNull()
            }
        } catch (e: Exception) {
            deleteDBs(DBNames.getSdkDBName(storageSuffix))
            createPushDB()
        }
    }

    single { get<PushDatabase>().subscriptionsQueries }

    single { SubscriptionStorageRepository(get()) }
}