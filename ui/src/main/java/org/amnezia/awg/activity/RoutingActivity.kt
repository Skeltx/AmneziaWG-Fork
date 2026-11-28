package org.amnezia.awg.activity

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.amnezia.awg.Application
import org.amnezia.awg.R
import org.amnezia.awg.databinding.ActivityRoutingBinding
import org.amnezia.awg.databinding.DialogRoutingRuleBinding
import org.amnezia.awg.databinding.RoutingRuleItemBinding
import org.amnezia.awg.engine.RoutingRule
import org.amnezia.awg.engine.RoutingRuleType
import org.amnezia.awg.engine.RoutingRulesStore
import org.amnezia.awg.engine.RoutingSettings
import org.amnezia.awg.engine.SingBoxBackend
import org.amnezia.awg.fragment.AppListDialogFragment

/**
 * Global split-tunnelling editor: app/domain/IP -> tunnel, direct or block.
 */
class RoutingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRoutingBinding
    private val store by lazy { RoutingRulesStore(this) }
    private var settings = RoutingSettings()
    private val adapter = RuleAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoutingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.routing)

        binding.rules.layoutManager = LinearLayoutManager(this)
        binding.rules.adapter = adapter
        binding.addRule.setOnClickListener { chooseTunnelsThen { showRuleDialog(it) } }
        binding.changeDefault.setOnClickListener { chooseTunnelsThen { showDefaultDialog(it) } }
        load()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun load() {
        settings = store.load()
        adapter.submit(settings.rules)
        binding.defaultOutbound.text = outboundLabel(settings.defaultOutbound)
        binding.empty.visibility = if (settings.rules.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun chooseTunnelsThen(block: (List<String>) -> Unit) {
        lifecycleScope.launch {
            val names = runCatching {
                Application.getTunnelManager().getTunnels().map { it.name }
            }.getOrDefault(emptyList())
            block(names)
        }
    }

    private fun showRuleDialog(tunnels: List<String>) {
        val dialog = DialogRoutingRuleBinding.inflate(layoutInflater)
        var type = RoutingRuleType.APP
        var outbound = "direct"

        dialog.typeGroup.check(R.id.type_app)
        dialog.typeGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            type = when (checkedId) {
                R.id.type_domain -> RoutingRuleType.DOMAIN
                R.id.type_ip -> RoutingRuleType.IP
                else -> RoutingRuleType.APP
            }
            dialog.pickApp.visibility =
                if (type == RoutingRuleType.APP) View.VISIBLE else View.GONE
        }

        val labels = mutableListOf(
            getString(R.string.routing_direct),
            getString(R.string.routing_block),
        )
        val values = mutableListOf("direct", "block")
        for (name in tunnels) {
            labels += name
            values += name
        }
        val options = ArrayAdapter(this, android.R.layout.simple_list_item_1, labels)
        dialog.outbound.setAdapter(options)
        dialog.outbound.setText(labels.first(), false)
        dialog.outbound.setOnItemClickListener { _, _, position, _ ->
            outbound = values.getOrElse(position) { "direct" }
        }

        dialog.pickApp.setOnClickListener {
            supportFragmentManager.setFragmentResultListener(
                AppListDialogFragment.REQUEST_SELECTION,
                this,
            ) { _, bundle ->
                val selected = bundle.getStringArray(AppListDialogFragment.KEY_SELECTED_APPS)
                    ?.firstOrNull()
                if (!selected.isNullOrBlank()) {
                    dialog.target.setText(selected)
                }
            }
            AppListDialogFragment.newInstance(ArrayList(), false)
                .show(supportFragmentManager, "routing-app-picker")
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.routing_add_rule)
            .setView(dialog.root)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val target = dialog.target.text?.toString()?.trim().orEmpty()
                if (target.isEmpty()) {
                    toast(getString(R.string.routing_invalid_target))
                    return@setPositiveButton
                }
                settings = settings.copy(
                    rules = settings.rules + RoutingRule(
                        type = type,
                        target = target,
                        outbound = outbound,
                    ),
                )
                store.save(settings)
                load()
                reloadEngine()
            }
            .show()
    }

    private fun showDefaultDialog(tunnels: List<String>) {
        val labels = mutableListOf(getString(R.string.routing_direct))
        val values = mutableListOf("direct")
        for (name in tunnels) {
            labels += name
            values += name
        }
        val checked = values.indexOf(settings.defaultOutbound).coerceAtLeast(0)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.routing_default)
            .setSingleChoiceItems(labels.toTypedArray(), checked) { dialog, which ->
                settings = settings.copy(defaultOutbound = values[which])
                store.save(settings)
                load()
                reloadEngine()
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun deleteRule(rule: RoutingRule) {
        settings = settings.copy(rules = settings.rules.filterNot { it.id == rule.id })
        store.save(settings)
        load()
        reloadEngine()
    }

    private fun reloadEngine() {
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                val backend = Application.getBackend()
                (backend as? SingBoxBackend)?.reloadRouting()
            }
        }
    }

    private fun outboundLabel(outbound: String): String = when (outbound) {
        "direct" -> getString(R.string.routing_direct)
        "block" -> getString(R.string.routing_block)
        else -> outbound
    }

    private fun toast(text: String) {
        android.widget.Toast.makeText(this, text, android.widget.Toast.LENGTH_SHORT).show()
    }

    private inner class RuleAdapter : RecyclerView.Adapter<RuleHolder>() {
        private var items: List<RoutingRule> = emptyList()

        fun submit(newItems: List<RoutingRule>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): RuleHolder =
            RuleHolder(RoutingRuleItemBinding.inflate(layoutInflater, parent, false))

        override fun onBindViewHolder(holder: RuleHolder, position: Int) =
            holder.bind(items[position])

        override fun getItemCount(): Int = items.size
    }

    private inner class RuleHolder(
        private val item: RoutingRuleItemBinding,
    ) : RecyclerView.ViewHolder(item.root) {
        fun bind(rule: RoutingRule) {
            item.title.text = when (rule.type) {
                RoutingRuleType.APP -> getString(R.string.routing_app)
                RoutingRuleType.DOMAIN -> getString(R.string.routing_domain)
                RoutingRuleType.IP -> getString(R.string.routing_ip)
            }
            item.subtitle.text = rule.target
            item.outbound.text = outboundLabel(rule.outbound)
            item.delete.setOnClickListener { deleteRule(rule) }
        }
    }
}
