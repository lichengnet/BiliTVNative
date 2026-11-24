package com.kirin.bilitv.core.image

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

@Suppress("DEPRECATION")
internal fun imageNetworkDiagnosticState(context: Context): String = runCatching {
  val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    ?: return@runCatching "networkState=unavailable"
  val networks = manager.allNetworks
  val internetCount = networks.count { network ->
    manager.getNetworkCapabilities(network)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
  }
  "networks=${networks.size} internet=$internetCount legacyConnected=${manager.activeNetworkInfo?.isConnected}"
}.getOrElse { "networkState=unavailable cause=${it.javaClass.simpleName}" }
