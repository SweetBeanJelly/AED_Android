package sweetbeanjelly.project.takecareoftheheart

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.viewpager.widget.ViewPager

class MainDialog : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_dialog)

        val viewPager = findViewById<ViewPager>(R.id.pager)
        val viewCpr = DialogCprActivity()
        val viewAed = DialogAedActivity()
        viewPager.adapter = viewCpr
        val btnCpr = findViewById<Button>(R.id.btnCpr)
        val btnAed = findViewById<Button>(R.id.btnAed)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        btnBack.setOnClickListener {
            finish()
        }

        var (btnCprCheck, btnAedCheck) = true to false // true white , false gray

        btnCpr.setOnClickListener {
            btnCprCheck = true
            btnAedCheck = false
            if (btnCprCheck && !btnAedCheck) {
                btnCpr.setBackgroundColor(resources.getColor(R.color.white))
                btnAed.setBackgroundColor(resources.getColor(R.color.gray))
                viewPager.adapter = viewCpr
            }
        }
        btnAed.setOnClickListener {
            btnCprCheck = false
            btnAedCheck = true
            if (!btnCprCheck && btnAedCheck) {
                btnCpr.setBackgroundColor(resources.getColor(R.color.gray))
                btnAed.setBackgroundColor(resources.getColor(R.color.white))
                viewPager.adapter = viewAed
            }
        }
    }
}