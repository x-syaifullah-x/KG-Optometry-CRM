package com.lizpostudio.kgoptometrycrm.search.recycle_bin.details

import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.lizpostudio.kgoptometrycrm.R
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientEntity
import com.lizpostudio.kgoptometrycrm.databinding.ActivityDetailsBinding
import com.lizpostudio.kgoptometrycrm.search.recycle_bin.details.forms.*
import id.xxx.module.view.binding.ktx.viewBinding

class DetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NAME_PATIENT = "EXTRA_NAME_SECTION"
    }

    private val binding by viewBinding(ActivityDetailsBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)

        binding.backButton
            .setOnClickListener { onBackPressed() }

        if (savedInstanceState == null) {
//            val patient = intent.getSerializableExtra(EXTRA_NAME_PATIENT) as PatientEntity
            val intent = intent
            val patient =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getSerializableExtra(
                        EXTRA_NAME_PATIENT,
                        PatientEntity::class.java
                    )
                } else {
                    @Suppress("DEPRECATION")
                    intent.getSerializableExtra(EXTRA_NAME_PATIENT) as? PatientEntity
                }

            val fragment = when (patient?.sectionName) {
                getString(R.string.info_form_caption) -> InfoFragment()
                getString(R.string.follow_up_form_caption) -> FollowUpFragment()
                getString(R.string.memo_form_caption) -> MemoFragment()
                getString(R.string.current_rx_caption) -> CurrentRxFragment()
                getString(R.string.refraction_caption) -> RefractionFragment()
                getString(R.string.ocular_health_caption) -> OcularHealthFragment()
                getString(R.string.supplementary_test_caption) -> SupplementaryFragment()
                getString(R.string.contact_lens_exam_caption) -> ContactLensFragment()
                getString(R.string.orthox_caption) -> OrthokFragment()
                getString(R.string.cash_order) -> CashOrderFragment()
                getString(R.string.sales_order_caption) -> SalesOrderFragment()
                getString(R.string.final_prescription_caption) -> SalesOrderFragment()
                else -> throw NotImplementedError()
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, fragment)
                .commit()
        }
    }
}