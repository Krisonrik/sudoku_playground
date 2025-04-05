package com.example.sudoku_playground

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber

private const val BLOCK_WIDTH = 3

class SudokuCreationViewModel(private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO) :
    ViewModel() {

    private val _digitUpdate: MutableSharedFlow<PositionalDigitStatus> = MutableSharedFlow()
    val digitUpdate: Flow<PositionalDigitStatus> = _digitUpdate.asSharedFlow()

    fun fillAllValues() {
        viewModelScope.launch(coroutineDispatcher) {
            for (blockY in 0 until BLOCK_WIDTH) {
                for (blockX in 0 until BLOCK_WIDTH) {
                    generateBlock().also {
                        Timber.d("block: ${it.joinToString { it.joinToString() }}")
                        for (innerY in 0 until BLOCK_WIDTH) {
                            for (innerX in 0 until BLOCK_WIDTH) {
                                val (mainBlockId, subIndex) = blockToId(
                                    innerX, innerY, blockX, blockY
                                )

                                _digitUpdate.emit(
                                    PositionalDigitStatus(
                                        mainBlockId, subIndex, it[innerY][innerX]
                                    )
                                )
                            }
                        }
                    }
                }
            }
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
        blockX: Int, blockY: Int, blockPositionX: Int, blockPositionY: Int
    ): Pair<Int, Int> {
        val mainBlockId = blockPositionY * BLOCK_WIDTH + blockPositionX + 1
        val subIndex = blockX + BLOCK_WIDTH * blockY + 1
        Timber.d("blockToGridIndex: blockX: $blockX, blockY: $blockY, blockPositionX: $blockPositionX, blockPositionY: $blockPositionY, mainBlockId: $mainBlockId, subIndex: $subIndex")
        return Pair(mainBlockId, subIndex)
    }

}

class PositionalDigitStatus(
    val mainBlockId: Int,
    val subIndex: Int,
    val digit: Int,
)
