package com.example.sudoku_playground

import androidx.compose.ui.viewinterop.NoOpUpdate
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
private const val BLOCK_SIZE = BLOCK_WIDTH * BLOCK_WIDTH
private const val GRID_WIDTH = 9

class SudokuCreationViewModel(private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO) :
    ViewModel() {

    private val blocks: MutableMap<Pair<Int, Int>, Array<IntArray>> = mutableMapOf()
    private val grid: Array<IntArray> = Array(GRID_WIDTH) { IntArray(GRID_WIDTH) }
    private val mask: Array<IntArray> = Array(GRID_WIDTH) { IntArray(GRID_WIDTH) }

    private val _digitUpdate: MutableSharedFlow<PositionalDigitStatus> = MutableSharedFlow()
    val digitUpdate: Flow<PositionalDigitStatus> = _digitUpdate.asSharedFlow()

    fun fillAllValues() {
        viewModelScope.launch(coroutineDispatcher) {
            initializeBlocksWithRandomBlocks()
            displayBlocks()
            updateGrid()
            scanGrid()
            displayBlocks()
        }
    }

    private suspend fun iterateThroughBlocks(handler: suspend (blockX: Int, blockY: Int) -> Unit) {
        for (blockY in 0 until BLOCK_WIDTH) {
            for (blockX in 0 until BLOCK_WIDTH) {
                handler(blockX, blockY)
            }
        }
    }

    private suspend fun displayBlocks() {
        iterateThroughBlocks { blockX, blockY ->
            blocks[Pair(blockX, blockY)]?.let {
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
            } ?: Timber.d("Block not found: $blockX, $blockY")
        }
    }

    private suspend fun initializeBlocksWithRandomBlocks() {
        iterateThroughBlocks { blockX, blockY ->
            generateBlock().let {
                Timber.d("block: ${it.joinToString { it.joinToString() }}")
                blocks[Pair(blockX, blockY)] = it
            }
        }
    }

    private fun findPositions(list: List<Int>, value: Int): MutableList<Int> {
        val positions = mutableListOf<Int>()
        list.forEachIndexed { index, i ->
            if (i == value) {
                positions.add(index)
            }
        }
        return positions
    }

    private fun findDuplicates(list: List<Int>): Map<Int, MutableList<Int>> {
        val duplicates: MutableMap<Int, MutableList<Int>> = mutableMapOf()
        list.groupingBy { it }.eachCount().filter { it.value > 1 }.forEach {
            val positions = findPositions(list, it.key)
            duplicates[it.key] = positions
        }
        return duplicates
    }

    private fun findMissingNumbers(list: List<Int>): List<Int> {
        val missingNumbers = mutableListOf<Int>()
        for (i in 1..BLOCK_SIZE) {
            if (!list.contains(i)) {
                missingNumbers.add(i)
            }
        }
        return missingNumbers
    }

    private fun scanGrid() {
        for (i in 0 until GRID_WIDTH) {
            if (i == 0) {
                lockBlock(0, 0)
            }

            solveRow(i)
            solveColumn(i)

            printGrid()
            printMask()
        }
    }

    private fun solveRow(i: Int) {
        while (true) {
            val row = grabRow(i)
            Timber.d("Row $i: [${row.joinToString()}]")
            findDuplicates(row).takeIf { it.isNotEmpty() }?.let { duplicates ->
                findMissingNumbers(row).takeIf { it.isNotEmpty() }?.forEach { missingNumber ->
                    duplicates.firstNotNullOf { duplicateEntry ->
                        val potentialPositions = duplicateEntry.value
                        potentialPositions.forEach {
                            val x = it
                            val localBlock = gridToBlock(x, i)
                            val potentialSwapPosition = findValuePositionInBlock(
                                missingNumber, localBlock.blockX, localBlock.blockY
                            ).let {
                                val (xInBlock, yInBlock) = it
                                blockToGrid(
                                    xInBlock, yInBlock, localBlock.blockX, localBlock.blockY
                                )
                            }

                            isMovable(x, i).takeIf{it.not()}?.let{
                                Timber.d("$x $i is not movable")
                            }
                            isMovable(potentialSwapPosition.first, potentialSwapPosition.second).takeIf{it.not()}?.let{
                                Timber.d("${potentialSwapPosition.first} ${potentialSwapPosition.second} is not movable")
                            }

                            if (isMovable(x, i) && isMovable(
                                    potentialSwapPosition.first, potentialSwapPosition.second
                                )
                            ) {
                                Timber.d("Row: $i has missing number: $missingNumber, can be moved to ${duplicateEntry.key} at $x")
                                swapValues(
                                    x,
                                    i,
                                    potentialSwapPosition.first,
                                    potentialSwapPosition.second
                                )
                                mask[i][x] = 1
                                return@let
                            } else {
                                Timber.d("Row: $i has missing number: $missingNumber, cannot be moved to ${duplicateEntry.key} at $x")
                                Timber.d("")
                            }
                        }
                    }
                }
            } ?: break
        }
        lockRow(i)
    }

    private fun solveColumn(i: Int) {
        while (true) {
            val column = grabColumn(i)
            Timber.d("Column $i: [${column.joinToString()}]")
            findDuplicates(column).takeIf { it.isNotEmpty() }?.let { duplicates ->
                findMissingNumbers(column).takeIf { it.isNotEmpty() }?.forEach { missingNumber ->
                    duplicates.firstNotNullOf { duplicateEntry ->
                        val potentialPositions = duplicateEntry.value
                        potentialPositions.forEach {
                            val y = it
                            val localBlock = gridToBlock(i, y)
                            val potentialSwapPosition = findValuePositionInBlock(
                                missingNumber, localBlock.blockX, localBlock.blockY
                            ).let {
                                val (xInBlock, yInBlock) = it
                                blockToGrid(
                                    xInBlock, yInBlock, localBlock.blockX, localBlock.blockY
                                )
                            }

                            isMovable(i,y).takeIf { it.not() }?.let{
                                Timber.d("$i $y is not movable")
                            }
                            isMovable(potentialSwapPosition.first,potentialSwapPosition.second).takeIf { it.not() }?.let{
                                Timber.d("${potentialSwapPosition.first} ${potentialSwapPosition.second} is not movable")
                            }

                            if (isMovable(i, y) && isMovable(
                                    potentialSwapPosition.first, potentialSwapPosition.second
                                )
                            ) {
                                Timber.d("Column: $i has missing number: $missingNumber, can be moved to ${duplicateEntry.key} at $y")
                                swapValues(
                                    i, y, potentialSwapPosition.first, potentialSwapPosition.second
                                )
                                mask[y][i] = 1
                                return@let
                            } else {
                                Timber.d("Column: $i has missing number: $missingNumber, cannot be moved to ${duplicateEntry.key} at $y")
                                Timber.d("")
                            }
                        }
                    }
                }
            } ?: break
        }
        lockColumn(i)
    }

    private fun printMask() {
        Timber.d("Mask:")
        mask.forEach {
            Timber.d(it.joinToString(" ") { it.toString() })
        }
    }

    private fun lockRow(row: Int) {
        Timber.d("lockRow: $row")
        for (i in 0 until GRID_WIDTH) {
            mask[row][i] = 1
        }
    }

    private fun lockColumn(column: Int) {
        Timber.d("lockColumn: $column")
        for (i in 0 until GRID_WIDTH) {
            mask[i][column] = 1
        }
    }

    private fun lockBlock(blockX: Int, blockY: Int) {
        Timber.d("lockBlock: $blockX, $blockY")
        for (i in 0 until BLOCK_WIDTH) {
            for (j in 0 until BLOCK_WIDTH) {
                mask[blockY * BLOCK_WIDTH + i][blockX * BLOCK_WIDTH + j] = 1
            }
        }
    }

    private fun swapValues(fromX: Int, fromY: Int, toX: Int, toY: Int) {
        Timber.d("swapValues: fromX: $fromX, fromY: $fromY, toX: $toX, toY: $toY")
        val fromValue = grid[fromY][fromX]
        val toValue = grid[toY][toX]
        grid[fromY][fromX] = toValue
        grid[toY][toX] = fromValue

        val fromBlock = gridToBlock(fromX, fromY)
        val toBlock = gridToBlock(toX, toY)
        blocks[Pair(fromBlock.blockX, fromBlock.blockY)]?.let { block ->
            block[fromBlock.yInBlock][fromBlock.xInBlock] = toValue
        }
        blocks[Pair(toBlock.blockX, toBlock.blockY)]?.let { block ->
            block[toBlock.yInBlock][toBlock.xInBlock] = fromValue
        }
    }


    private fun findValuePositionInBlock(
        missingNumber: Int, blockX: Int, blockY: Int
    ): Pair<Int, Int> {
        blocks[Pair(blockX, blockY)]?.let { block ->
            for (y in 0 until BLOCK_WIDTH) {
                for (x in 0 until BLOCK_WIDTH) {
                    if (block[y][x] == missingNumber) {
                        return Pair(x, y)
                    }
                }
            }
        } ?: throw IllegalStateException("Block not found")
        return Pair(0, 0)
    }

    private fun gridToBlock(x: Int, y: Int): PositionByBlock {
        val xInBlock = x % BLOCK_WIDTH
        val yInBlock = y % BLOCK_WIDTH
        val blockX = x / BLOCK_WIDTH
        val blockY = y / BLOCK_WIDTH
        return PositionByBlock(xInBlock, yInBlock, blockX, blockY)
    }

    private fun isMovable(x: Int, y: Int): Boolean {
        return mask[y][x] == 0
    }

    private fun grabRow(x: Int): List<Int> {
        return grid[x].toList()
    }

    private fun grabColumn(x: Int): List<Int> {
        return grid.map { it[x] }
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
        val block: Array<IntArray> = Array(BLOCK_WIDTH) { IntArray(BLOCK_WIDTH) }
        // generate a set of 9 numbers from 1 to 9
        val numbers = (1..BLOCK_SIZE).toMutableList()
        for (i in 0 until BLOCK_WIDTH) {
            for (j in 0 until BLOCK_WIDTH) {
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
        xInBlock: Int, yInBlock: Int, blockX: Int, blockY: Int
    ): Pair<Int, Int> {
        val mainBlockId = blockY * BLOCK_WIDTH + blockX + 1
        val subIndex = xInBlock + BLOCK_WIDTH * yInBlock + 1
        return Pair(mainBlockId, subIndex)
    }

    private fun blockToGrid(
        xInBlock: Int, yInBlock: Int, blockX: Int, blockY: Int
    ): Pair<Int, Int> {
        val gridX = xInBlock + BLOCK_WIDTH * blockX
        val gridY = yInBlock + BLOCK_WIDTH * blockY
        return Pair(gridX, gridY)
    }

    private fun fillGridWithBlock(
        block: Array<IntArray>, blockX: Int, blockY: Int
    ) {
        for (yInBlock in 0 until BLOCK_WIDTH) {
            for (xInBlock in 0 until BLOCK_WIDTH) {
                val (gridX, gridY) = blockToGrid(xInBlock, yInBlock, blockX, blockY)
                grid[gridY][gridX] = block[yInBlock][xInBlock]
            }
        }
    }
}

class PositionByBlock(
    val xInBlock: Int, val yInBlock: Int, val blockX: Int, val blockY: Int
)

class PositionalDigitStatus(
    val mainBlockId: Int,
    val subIndex: Int,
    val digit: Int,
)
