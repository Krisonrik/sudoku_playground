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

const val BLOCK_WIDTH = 3

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


    private fun generateBlock(): Array<IntArray> {
        val block: Array<IntArray> = Array(3) { IntArray(3) }
        // generate a set of 9 numbers from 1 to 9
        val numbers = (1..9).toMutableList()
        for (i in 0 until 3) {
            for (j in 0 until 3) {
                // generate a random number from the set
                val randomValue = numbers.random()

                block[i][j] = randomValue
                // remove the number from the set to avoid duplicates
                numbers.remove(randomValue)
            }
        }
        return block
    }

    // 1 based index
    private fun blockToId(
        blockX: Int,
        blockY: Int,
        blockPositionX: Int,
        blockPositionY: Int
    ): Pair<Int, Int> {
        val mainBlockId = blockPositionY * BLOCK_WIDTH + blockPositionX + 1
        val subIndex = blockX + BLOCK_WIDTH * blockY + 1
        Timber.d("blockToGridIndex: blockX: $blockX, blockY: $blockY, blockPositionX: $blockPositionX, blockPositionY: $blockPositionY, mainBlockId: $mainBlockId, subIndex: $subIndex")
        return Pair(mainBlockId, subIndex)
    }

    private fun fillAllValues(binding: ViewBinding) {
        binding.apply {
            for (blockY in 0 until BLOCK_WIDTH) {
                for (blockX in 0 until BLOCK_WIDTH) {
                    generateBlock().also {
                        Timber.d("block: ${it.joinToString { it.joinToString() }}")
                        for (innerY in 0 until BLOCK_WIDTH) {
                            for (innerX in 0 until BLOCK_WIDTH) {
                                val (row, col) = blockToId(innerX, innerY, blockX, blockY)
                                val resId = resources.getIdentifier(
                                    "number_${row}_${col}",
                                    "id",
                                    packageName
                                )
                                Timber.d("resId: $resId, row: $row, col: $col")
                                findViewById<TextView>(resId).text = it[innerY][innerX].toString()
                            }
                        }
                    }
                }
            }
        }
    }
}