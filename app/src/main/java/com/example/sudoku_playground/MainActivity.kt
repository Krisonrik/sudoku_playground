package com.example.sudoku_playground

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewbinding.ViewBinding
import com.example.sudoku_playground.databinding.ActivityMainBinding
import timber.log.Timber
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        enableEdgeToEdge()
        ActivityMainBinding.inflate(layoutInflater).let { binding ->
            setContentView(binding.root)
            fillAllValues(binding)
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun fillAllValues(binding: ViewBinding) {
        binding.apply {
            for (i in 0 until 9) {
                for (j in 0 until 9) {
                    val resId = resources.getIdentifier("number_${i+1}_${j+1}", "id", packageName)
                    Timber.d("resId: $resId, i: $i, j: $j")
                    findViewById<TextView>(resId)?.let { textView ->
                        val randomValue = Random.nextInt(1, 10).toString()
                        textView.text = randomValue
                        textView.setOnClickListener {
                            textView.text = Random.nextInt(1, 10).toString()
                        }
                    }
                }
            }

        }

    }
}