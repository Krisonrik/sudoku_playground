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

    private val blocks: MutableMap<Pair<Int, Int>, Array<IntArray>> = mutableMapOf()
    private val grid: Array<IntArray> = Array(9) { IntArray(9) }

    private val _digitUpdate: MutableSharedFlow<PositionalDigitStatus> = MutableSharedFlow()
    val digitUpdate: Flow<PositionalDigitStatus> = _digitUpdate.asSharedFlow()

    fun fillAllValues() {
        viewModelScope.launch(coroutineDispatcher) {
            for (blockY in 0 until BLOCK_WIDTH) {
                for (blockX in 0 until BLOCK_WIDTH) {
                    generateBlock().also {
                        Timber.d("block: ${it.joinToString { it.joinToString() }}")
                        blocks[Pair(blockX, blockY)] = it
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
            updateGrid()
            printGrid()
        }
    }

    private fun printGrid() {
        Timber.d("Grid:")
        grid.forEach {
            Timber.d(it.joinToString(" ") { it.toString() })
        }
    }

    private fun updateGrid() {
        blocks.forEach {
            fillGridWithBlock(it.value, it.key.first, it.key.second)
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

    private fun blockToGrid(
        blockX: Int, blockY: Int, blockPositionX: Int, blockPositionY: Int
    ): Pair<Int, Int> {
        val gridX = blockX + BLOCK_WIDTH * blockPositionX
        val gridY = blockY + BLOCK_WIDTH * blockPositionY
        Timber.d("blockToGridIndex: blockX: $blockX, blockY: $blockY, blockPositionX: $blockPositionX, blockPositionY: $blockPositionY, gridX: $gridX, gridY: $gridY")
        return Pair(gridX, gridY)
    }

    private fun fillGridWithBlock(
        block: Array<IntArray>, blockPositionX: Int, blockPositionY: Int
    ) {
        for (blockY in 0 until BLOCK_WIDTH) {
            for (blockX in 0 until BLOCK_WIDTH) {
                val (gridX, gridY) = blockToGrid(blockX, blockY, blockPositionX, blockPositionY)
                grid[gridY][gridX] = block[blockY][blockX]
            }
        }
        Timber.d("fillGridWithBlock: blockPositionX: $blockPositionX, blockPositionY: $blockPositionY, block: ${block.joinToString { it.joinToString() }}")
    }
}

class PositionalDigitStatus(
    val mainBlockId: Int,
    val subIndex: Int,
    val digit: Int,
)
