package org.amnezia.awg.activity

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.amnezia.awg.Application
import org.amnezia.awg.R
import org.amnezia.awg.backend.GoBackend
import org.amnezia.awg.backend.Tunnel
import org.amnezia.awg.model.ObservableTunnel
import org.json.JSONObject

class DeepLinkActivity : ComponentActivity() {

    private var pendingTunnel: ObservableTunnel? = null

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val tunnel = pendingTunnel
        if (result.resultCode == RESULT_OK && tunnel != null) {
            enableTunnel(tunnel)
        } else {
            Toast.makeText(this, R.string.deeplink_vpn_permission_required, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val domain = intent?.data?.host
        if (domain.isNullOrBlank()) {
            finish()
            return
        }

        lifecycleScope.launch {
            try {
                val mapping = assets.open("tunnel_map.json").bufferedReader().use { it.readText() }
                val tunnelName = JSONObject(mapping).optString(domain, "")
                if (tunnelName.isBlank()) {
                    Toast.makeText(
                        this@DeepLinkActivity,
                        getString(R.string.deeplink_no_mapping, domain),
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                    return@launch
                }

                val tunnel = Application.getTunnelManager().getTunnels()[tunnelName]
                if (tunnel == null) {
                    Toast.makeText(
                        this@DeepLinkActivity,
                        getString(R.string.deeplink_tunnel_not_found, tunnelName),
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                    return@launch
                }

                val permissionIntent = GoBackend.VpnService.prepare(this@DeepLinkActivity)
                if (permissionIntent != null) {
                    pendingTunnel = tunnel
                    vpnPermissionLauncher.launch(permissionIntent)
                } else {
                    enableTunnel(tunnel)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to prepare tunnel", e)
                Toast.makeText(
                    this@DeepLinkActivity,
                    getString(R.string.deeplink_error, e.message ?: ""),
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }

    private fun enableTunnel(tunnel: ObservableTunnel) {
        lifecycleScope.launch {
            try {
                tunnel.setStateAsync(Tunnel.State.UP)
                Toast.makeText(
                    this@DeepLinkActivity,
                    getString(R.string.deeplink_tunnel_enabled, tunnel.getName()),
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to enable tunnel", e)
                Toast.makeText(
                    this@DeepLinkActivity,
                    getString(R.string.deeplink_error, e.message ?: ""),
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                finish()
            }
        }
    }

    companion object {
        private const val TAG = "AmneziaWG/DeepLink"
    }
}
